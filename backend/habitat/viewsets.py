from collections import defaultdict
from decimal import Decimal, getcontext
import zipfile
from django.contrib.gis.geos import GEOSGeometry
from django.db import connections, ProgrammingError
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


SQL_GET_REGIONS = """
declare @pt geometry = geometry::Point(%s, %s, 3112);

select region, habitat, geometry::UnionAggregate(geom).STAsBinary() as geom, sum(area)/1000000 as area, sum(percentage) as percentage
from (
  select b.region,
         b.boundary_area,
         r.habitat,
         b.geom.STIntersection(r.geom) as geom,
         b.geom.STIntersection(r.geom).STArea() as area,
         100 * b.geom.STIntersection(r.geom).STArea() / b.geom.STArea() as percentage
  from
    (select region, geom, geom.STArea() as boundary_area
     from seamapaus_boundaries_view
     where boundary_layer = %s
       and geom.STContains(@pt) = 1) b
  left join
    (select habitat, geom
     from seamapaus_regions_view
     where layer_name = %s) r
  on b.geom.STIntersects(r.geom) = 1
) sub
group by region, habitat;
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
        sw.field("region", "C")
        sw.field("habitat", "C")
        sw.field("area", "N", decimal=30)
        sw.field("percentage", "N", decimal=30)

        for row in data:
            region,habitat,bgeom,area,pctg = row
            sw.record(row['region'], row['habitat'], row['area'], row['pctg'])
            geom = GEOSGeometry(buffer(row['geom']))
            coords = geom.coords
            # pyshp doesn't natively handle multipolygons
            # yet, so if we have one of those just flatten
            # it out to parts ourselves:
            if geom.num_geom > 1:
                coords = [parts for poly in coords for parts in poly]
            sw.poly(parts=coords)

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

    def to_dict(row):
        region,habitat,geom,area,pctg = row
        return {'region':region, 'habitat':habitat, 'geom':geom, 'area':area, 'pctg':pctg}

    results = []
    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_REGIONS, [x, y, boundary, habitat])
        while True:
            try:
                results.extend((to_dict(row) for row in cursor.fetchall()))
                if not cursor.nextset():
                    break
            except ProgrammingError:
                if not cursor.nextset():
                    break

    if request.accepted_renderer.format == 'raw':
        return Response(results, content_type='application/zip',
                        headers={'Content-Disposition': 'attachment; filename="regions.zip"'})

    return Response({'data': results,
                     'boundary': boundary,
                     'habitat': habitat,
                     'url': reverse('habitat-regions', request=request),
                     'x': x,
                     'y': y},
                    template_name='habitat/regions.html')
