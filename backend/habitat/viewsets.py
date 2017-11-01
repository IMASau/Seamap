from catalogue.models import Layer
from collections import defaultdict
from decimal import Decimal, getcontext
import zipfile
from django.contrib.gis.geos import GEOSGeometry
from django.db import connections, ProgrammingError
from django.db.models.functions import Coalesce
from rest_framework.decorators import api_view, list_route, renderer_classes
from rest_framework.renderers import BaseRenderer, TemplateHTMLRenderer
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.serializers import ValidationError
import shapefile
try:
    from StringIO import StringIO
except ImportError:
    from io import BytesIO as StringIO

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
select habitat, {} area / 1000000, 100 * area / %s as percentage
from SeamapAus_Habitat_By_Region
where region = %s
  and boundary_layer_id = %s
  and habitat_layer_id = %s;
"""

def D(number):
    "Return the (probably) string, quantized to an acceptable number of decimal places"
    return Decimal(number).quantize(Decimal('0.1'))


class ShapefileRenderer(BaseRenderer):
    media_type = 'application/zip'
    format = 'raw'

    def render(self, data, media_type=None, renderer_context=None):
        # Set up shapefile writer:
        sw = shapefile.Writer(shapeType=shapefile.POLYGON)
        sw.field("habitat", "C")
        sw.field("area", "N", decimal=30)
        sw.field("percentage", "N", decimal=30)

        for row in data:
            habitat,bgeom,area,pctg = row
            geom = GEOSGeometry(buffer(row['geom']))
            # For some reason MSSQL is giving me the occasional (2-point) LineString; filter those:
            geoms = (g for g in geom if g.geom_type == 'Polygon') if geom.num_geom > 1 else [geom]
            for g in geoms:
                sw.record(row['habitat'], row['area'], row['pctg'])
                sw.poly(parts=g.coords)
            # coords = geom.coords
            # pyshp doesn't natively handle multipolygons
            # yet, so if we have one of those just flatten
            # it out to parts ourselves:
            # if geom.num_geom > 1:
            #     # coords = [parts for poly in coords for parts in poly]
            #     coords = [part for g in geom for part in g.coords if g.geom_type == 'Polygon']

        shp = StringIO()
        shx = StringIO()
        dbf = StringIO()
        sw.saveShp(shp)
        sw.saveShx(shx)
        sw.saveDbf(dbf)

        zipstream = StringIO()
        with zipfile.ZipFile(zipstream, 'w') as responsezip:
            responsezip.writestr('regions.shp', shp.getvalue())
            responsezip.writestr('regions.shx', shx.getvalue())
            responsezip.writestr('regions.dbf', dbf.getvalue())
        return zipstream.getvalue()


# request as .../transect/?line= x1 y1,x2 y2, ...,xn yn&layers=layer1,layer2..
@list_route()
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
        p1, p2 = p2, segments[p2].keys()[0]

    return Response(ordered_segments)


# .../regions?boundary=boundarylayer&habitat=habitatlayer&x=longitude&y=latitude
# boundary is the boundary-layer name, eg seamap:SeamapAus_BOUNDARIES_CMR2014
# habitat is the habitat-layer name, eg seamap:FINALPRODUCT_SeamapAus
# x and y (lon + lat) are in espg3112
@list_route()
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

    if is_download:
        def to_dict(row):
            habitat,geom,area,pctg = row
            return {'habitat':habitat, 'geom':geom, 'area':area, 'pctg':pctg}
    else:
        def to_dict(row):
            habitat,area,pctg = row
            return {'habitat':habitat, 'area':area, 'pctg':pctg}

    # If we don't find the boundary layer it's probably shenanigans,
    # just let the default exception handling deal with it:
    boundary_layer = Layer.objects.annotate(layer=Coalesce('detail_layer', 'layer_name')).get(pk=boundary).layer

    results = []
    boundary_name = None
    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_IDENTIFY_REGION, [boundary_layer, x, y])
        boundary_info = cursor.fetchone()

        if boundary_info:
            boundary_name, boundary_area = boundary_info

            cursor.execute(SQL_GET_STATS.format('geom.STAsBinary(),' if is_download else ''),
                           [boundary_area, boundary_name, boundary, habitat])
            results = map(to_dict, cursor)

            if is_download:
                return Response(results, content_type='application/zip',
                                headers={'Content-Disposition': 'attachment; filename="regions.zip"'})

        # HTML only; add a derived row (doing it in SQL was getting complicated and slow):
        if results:
            area = boundary_area / 1000000 - float( sum(row['area'] or 0 for row in results) )
            pctg = 100 * area / (boundary_area / 1000000)
            results.append({'habitat': 'UNMAPPED', 'area': area, 'pctg': pctg})
        return Response({'data': results,
                         'boundary': boundary,
                         'boundary_name': boundary_name,
                         'habitat': habitat,
                         'url': reverse('habitat-regions', request=request),
                         'x': x,
                         'y': y},
                        template_name='habitat/regions.html')
