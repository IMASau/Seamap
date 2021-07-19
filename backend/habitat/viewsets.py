# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from catalogue.models import Layer
from collections import defaultdict, namedtuple
from decimal import Decimal, getcontext
import numbers
import zipfile
from django.contrib.gis.geos import GEOSGeometry
from django.db import connections, ProgrammingError
from django.db.models.functions import Coalesce
from rest_framework.decorators import action, api_view, renderer_classes
from rest_framework.renderers import BaseRenderer, TemplateHTMLRenderer
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.serializers import ValidationError
import shapefile
from io import BytesIO

# SQL Template to invoke the habitat transect intersection procedure.
# There's an awkward situation of two types of parameters involved
# here; "%s" and "{}".  The former is used by ODBC for parameters
# (avoiding injection attacks, etc).  The latter is because we want to
# do a where-in clause against an unknown number of parameters, so for
# safety we construct the portion of the template refering to layers
# below, using a '%s' parameter for every layer, then include that in
# the template by substituting for {}.
SQL_GET_TRANSECT = """
declare @line geometry = geometry::STGeomFromText(%s, 3112);
declare @tmphabitat as HabitatTableType;

insert into @tmphabitat
    select habitat as name, geom from (
        {}
    ) polys
    order by priority asc;

SELECT segments.segment.STStartPoint().STX as 'start x',
       segments.segment.STStartPoint().STY as 'start y',
       segments.segment.STEndPoint().STX   as 'end x',
       segments.segment.STEndPoint().STY   as 'start y',
       segments.segment.STLength()         as 'length',
       segments.name
FROM(
    SELECT segment, name
    FROM path_intersections(@line, @tmphabitat)
) as segments;
"""


# Note hack; we only include geoms in the result sometimes, so there's
# a conditional fragement inclusion using {} before actual parameter
# preparation (%s)
SQL_IDENTIFY_REGION = """
select region, geom.STArea() from SeamapAus_Boundaries_View
where boundary_layer = %s
  and geom.STContains(geometry::Point(%s, %s, 3112)) = 1
"""

SQL_GET_STATS = """
select habitat, {} area / 1000000 as area, 100 * area / %s as percentage
from SeamapAus_Habitat_By_Region
where region = %s
  and boundary_layer_id = %s
  and habitat_layer_id = %s;
"""

# Another hacky two-step construction: we can pass the coordinates in
# as parameters, but need to string-splice in both the table name, and
# the list of of column names after introspection (technically we
# don't need to if we just assume the geometry column is always
# "geom", but let's not leave that hole open just to save a few
# minutes)
SQL_GET_SUBSET = """
declare @bbox geometry = geometry::Point(%s, %s, 3112).STUnion(geometry::Point(%s, %s, 3112)).STEnvelope();
select {}.STIntersection(@bbox).STAsBinary() geom, {} from {} where {}.STIntersects(@bbox) = 1;
"""

PRJ_3112 = """PROJCS["GDA94_Geoscience_Australia_Lambert",GEOGCS["GCS_GDA_1994",DATUM["D_GDA_1994",SPHEROID["GRS_1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]],PROJECTION["Lambert_Conformal_Conic"],PARAMETER["standard_parallel_1",-18],PARAMETER["standard_parallel_2",-36],PARAMETER["latitude_of_origin",0],PARAMETER["central_meridian",134],PARAMETER["false_easting",0],PARAMETER["false_northing",0],UNIT["Meter",1]]"""


def parse_bounds(bounds_str):
    # Note, we want points in x,y order but a boundary string is in y,x order:
    parts = bounds_str.split(',')[:4]  # There may be a trailing SRID URN we ignore for now
    [x0,y0,x1,y1] = list(map(float, parts))
    return [x0,y0,x1,y1]


def D(number):
    "Return the (probably) string, quantized to an acceptable number of decimal places"
    return Decimal(number).quantize(Decimal('0.1'))


class ShapefileRenderer(BaseRenderer):
    media_type = 'application/zip'
    format = 'raw'

    def render(self, data, media_type=None, renderer_context=None):
        # Set up shapefile writer:
        shp = BytesIO()
        shx = BytesIO()
        dbf = BytesIO()

        with shapefile.Writer(shp=shp, shx=shx, dbf=dbf, shapeType=shapefile.POLYGON) as sw:
            fields = data['fields']
            # Define shp-table column structure from field metadata:
            geom_idx = None
            for idx, field in enumerate(fields):
                fname,ftype = field[:2]
                fname = str(fname)  # it's unicode out of the box, with breaks pyshp / struct.pack
                if issubclass(ftype, str):
                    sw.field(str(fname), "C")
                elif issubclass(ftype, numbers.Number):
                    sw.field(str(fname), "N", decimal=30)
                else:
                    geom_idx = idx

            for row in data['data']:
                row = list(row)
                geom = row.pop(geom_idx)
                geom = GEOSGeometry(memoryview(geom))
                if geom.geom_type == 'Point':
                    sw.record(*row)
                    sw.point(*geom.coords)
                else:
                    # For some reason MSSQL is giving me the occasional (2-point) LineString; filter those:
                    geoms = (g for g in geom if g.geom_type == 'Polygon') if geom.num_geom > 1 else [geom]
                    for g in geoms:
                        sw.record(*row)
                        sw.poly(g.coords)
                # coords = geom.coords
                # pyshp doesn't natively handle multipolygons
                # yet, so if we have one of those just flatten
                # it out to parts ourselves:
                # if geom.num_geom > 1:
                #     # coords = [parts for poly in coords for parts in poly]
                #     coords = [part for g in geom for part in g.coords if g.geom_type == 'Polygon']

        filename = data['file_name']
        zipstream = BytesIO()
        with zipfile.ZipFile(zipstream, 'w') as responsezip:
            responsezip.writestr(filename + '.shp', shp.getvalue())
            responsezip.writestr(filename + '.shx', shx.getvalue())
            responsezip.writestr(filename + '.dbf', dbf.getvalue())
            responsezip.writestr(filename + '.prj', PRJ_3112)
        return zipstream.getvalue()


# request as .../transect/?line= x1 y1,x2 y2, ...,xn yn&layers=layer1,layer2..
@action(detail=False)
@api_view()
def transect(request):
    for required in ['line', 'layers']:
        if required not in request.query_params:
            raise ValidationError({"message": "Required parameter '{}' is missing".format(required)})

    ordered_segments = []
    distance = 0
    segments = defaultdict(dict)
    start_segment = None

    line = request.query_params.get('line')
    linestring = 'LINESTRING(' + line + ')'

    # Lines don't really have a direction, so we can't make assumptions
    # about how they will be returned
    start_pt = tuple(map(D, line.split(',', 1)[0].split(' ')))
    start_percentage = 0

    # To ensure polygons are inserted in the order of layer
    # ordering, we add a priority to sort by -- which means
    # generating a union-all statement by layer:
    layers = request.query_params.get('layers').lower().split(',')
    layer_stmt = ('select habitat, geom, {} as priority '
                  'from SeamapAus_Regions_VIEW '
                  'where lower(layer_name) = %s and geom.STIntersects(@line) = 1')
    layers_placeholder = '\nUNION ALL\n'.join( layer_stmt.format(i) for i,_ in enumerate(layers) )

    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_TRANSECT.format(layers_placeholder),
                       [linestring] + layers)
        while True:
            try:
                for row in cursor.fetchall():
                    [startx, starty, endx, endy, length, name] = row
                    p1, p2 = (D(startx), D(starty)), (D(endx), D(endy))
                    segment = p1, p2, name, length
                    if p1 == p2:
                        continue
                    distance += length
                    segments[p1][p2] = segment
                    segments[p2][p1] = segment
                    if p1 == start_pt or p2 == start_pt:
                        start_segment = segment
                if not cursor.nextset():
                    break
            except ProgrammingError:
                if not cursor.nextset():
                    break

    p1, p2, _, _ = start_segment
    if p1 != start_pt:
        p1, p2 = p2, p1
    start_distance = 0
    # p1 is the start point; it will always be the "known" point, and p2 is the next one to find:
    while True:
        _, _, name, length = segments[p1][p2]
        end_distance = start_distance + length
        end_percentage = start_percentage + 100*length/float(distance)
        ordered_segments.append({'name': name,
                                 'start_distance': start_distance,
                                 'end_distance': end_distance,
                                 'start_percentage': start_percentage,
                                 'end_percentage': end_percentage,
                                 'startx': p1[0],
                                 'starty': p1[1],
                                 'endx': p2[0],
                                 'endy': p2[1]})
        start_percentage = end_percentage
        start_distance = end_distance
        del segments[p1][p2]
        if not segments[p1]: del segments[p1]
        del segments[p2][p1]
        if not segments[p2]: del segments[p2]

        if not segments:
            break
        p1, p2 = p2, list(segments[p2].keys())[0]

    return Response(ordered_segments)


# .../regions?boundary=boundarylayer&habitat=habitatlayer&x=longitude&y=latitude
# boundary is the boundary-layer name, eg seamap:SeamapAus_BOUNDARIES_CMR2014
# habitat is the habitat-layer name, eg seamap:FINALPRODUCT_SeamapAus
# x and y (lon + lat) are in espg3112
@action(detail=False)
@api_view()
@renderer_classes((TemplateHTMLRenderer, ShapefileRenderer))
def regions(request):
    for required in ['boundary', 'habitat', 'x', 'y']:
        if required not in request.query_params:
            raise ValidationError({"message": "Required parameter '{}' is missing".format(required)})

    boundary = request.query_params.get('boundary')
    habitat  = request.query_params.get('habitat')
    x        = request.query_params.get('x')
    y        = request.query_params.get('y')

    # Performance optimisation (hack): it's expensive to pull
    # geometries over the wire for shapefile generation, but we don't
    # need them for popup display
    is_download = request.accepted_renderer.format == 'raw'

    # If we don't find the boundary layer it's probably shenanigans,
    # just let the default exception handling deal with it:
    boundary_layer = Layer.objects.annotate(layer=Coalesce('detail_layer', 'layer_name')).get(pk=boundary).layer

    results = []
    boundary_name = None
    downloadable = False
    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_IDENTIFY_REGION, [boundary_layer, x, y])
        boundary_info = cursor.fetchone()

        if boundary_info:
            boundary_name, boundary_area = boundary_info

            cursor.execute(SQL_GET_STATS.format('geom.STAsBinary() as geom,' if is_download else ''),
                           [boundary_area, boundary_name, boundary, habitat])

            # Convert plain list of tuples to list of dicts by zipping
            # with column names (emulates the raw pyodbc protocol):
            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', [col for col in columns])
            results = [namedrow(*row) for row in cursor.fetchall()]

            if is_download:
                return Response({'data': results,
                                 'fields': cursor.description,
                                 'file_name': boundary_name},
                                content_type='application/zip',
                                headers={'Content-Disposition': 'attachment; filename="{}.zip"'.format(boundary_name)})

            # HTML only; add a derived row (doing it in SQL was getting complicated and slow):
            downloadable = len(results)
            area = boundary_area / 1000000 - float( sum(row.area or 0 for row in results) )
            pctg = 100 * area / (boundary_area / 1000000)
            results.append({'habitat': 'UNMAPPED', 'area': area, 'percentage': pctg})
        return Response({'data': results,
                         'downloadable': downloadable,
                         'boundary': boundary,
                         'boundary_name': boundary_name,
                         'habitat': habitat,
                         'url': reverse('habitat-regions', request=request),
                         'x': x,
                         'y': y},
                        template_name='habitat/regions.html')


# .../subset?bounds=1,1,11,...&layer_id=layerid
# boundary is the boundary-layer name, eg seamap:SeamapAus_BOUNDARIES_CMR2014
# habitat is the habitat-layer name, eg seamap:FINALPRODUCT_SeamapAus
# x and y (lon + lat) are in espg3112
@action(detail=False)
@api_view()
@renderer_classes((ShapefileRenderer,))
def subset(request):
    for required in ['bounds', 'layer_id']:
        if required not in request.query_params:
            raise ValidationError({"message": "Required parameter '{}' is missing".format(required)})

    bounds_str = request.query_params.get('bounds')
    layer_id = request.query_params.get('layer_id')

    table_name = Layer.objects.get(pk=layer_id).table_name

    # ISA-68: cursor.columns() metadata is currently being returned as
    # tuples, instead of Row objects that can be accessed by name.
    # Avoid the drama, just use numeric indices:
    NAME_IDX = 3
    TYPE_IDX = 5

    geom_col = None
    colnames = []
    field_metadata = []
    with connections['transects'].cursor() as cursor:
        columns = cursor.columns(table=table_name)
        for row in cursor.fetchall():
            colname = row[NAME_IDX]
            typename = row[TYPENAME]
            if typename == 'geometry':
                geom_col = colname
            else:
                colnames.append(colname)

        subset_sql = SQL_GET_SUBSET.format(geom_col, ','.join(colnames), table_name, geom_col)
        cursor.execute(subset_sql, parse_bounds(bounds_str))
        data = cursor.fetchall()
        field_metadata = cursor.description

    return Response({'data': data, 'file_name': table_name, 'fields': field_metadata},
                    content_type='application/zip',
                    headers={'Content-Disposition':
                             'attachment; filename="{}.zip"'.format(table_name)})
