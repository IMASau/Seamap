from collections import defaultdict
from decimal import Decimal, getcontext
from django.db import connections, ProgrammingError
from rest_framework import viewsets
from rest_framework.decorators import list_route
from rest_framework.response import Response
from rest_framework.serializers import ValidationError
from habitat.models import Transect
from habitat.serializers import TransectSerializer


# SQL Template to invoke the habitat transect intersection procedure.
# There's an awkward situation of two types of parameters involved
# here; "%s" and "{}".  The former is used by ODBC for parameters
# (avoiding injection attacks, etc).  The latter is because we want to
# do a where-in clause against an unknown number of parameters, so for
# safety we construct the sequence "%s,%s,..." matching the number of
# user parameters, include that in the template by substituting for
# {}.
SQL_GET_TRANSECT = """
declare @line geometry = geometry::STGeomFromText(%s, 3112);
declare @tmphabitat as HabitatTableType;

insert into @tmphabitat
    select habitat as name, geom from SeamapAus_Regions_VIEW
    where lower(layer_name) in ({});

SELECT segments.segment.STStartPoint().STX as 'start x',
        segments.segment.STStartPoint().STY as 'start y',
        segments.segment.STEndPoint().STX as 'end x',
        segments.segment.STEndPoint().STY as 'start y',
        segments.segment.STLength() as 'length',
        segments.name
FROM(
    SELECT segment, name
    FROM path_intersections(@line, @tmphabitat)
    WHERE segment.STLength() > %s
) as segments;
"""


def D(number):
    "Return the (probably) string, quantized to an acceptable number of decimal places"
    return Decimal(number).quantize(Decimal('0.01'))


class HabitatViewSet(viewsets.ViewSet):

    # request as .../transect/?line= x1 y1,x2 y2, ...,xn yn&layers=layer1,layer2..
    @list_route()
    def transect(self, request):
        if 'line' not in request.query_params:
            raise ValidationError("Required parameter 'line' is missing")
        if 'layers' not in request.query_params:
            raise ValidationError("Required parameter 'layers' is missing")

        tolerance = 0.0001  # minimum length for non-zero line in sql query
        orderedModels = []
        distance = 0
        segments = defaultdict(dict)
        start_segment = None

        line = request.query_params.get('line')
        linestring = 'LINESTRING(' + line + ')'

        # Lines don't really have a direction, so we can't make assumptions
        # about how they will be returned
        start_pt = tuple(map(D, line.split(',', 1)[0].split(' ')))
        start_percentage = 0

        layers = request.query_params.get('layers').lower().split(',')
        layers_placeholder = ','.join(['%s'] * len(layers))

        with connections['transects'].cursor() as cursor:
            cursor.execute(SQL_GET_TRANSECT.format(layers_placeholder),
                           [linestring] + layers + [tolerance])
            while True:
                try:
                    for row in cursor.fetchall():
                        [startx, starty, endx, endy, length, name] = row
                        p1, p2 = (D(startx), D(starty)), (D(endx), D(endy))
                        segment = p1, p2, name, length
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
        # p1 is the start point; it will always be the "known" point, and p2 is the next one to find:
        while True:
            _, _, name, length = segments[p1][p2]
            end_percentage = start_percentage + 100*length/float(distance)
            orderedModels.append(Transect(name=name,
                                          start_percentage=start_percentage,
                                          end_percentage=end_percentage,
                                          startx=p1[0], starty=p1[1],
                                          endx=p2[0], endy=p2[1]))
            start_percentage = end_percentage
            del segments[p1][p2]
            if not segments[p1]: del segments[p1]
            del segments[p2][p1]
            if not segments[p2]: del segments[p2]

            if not segments:
                break
            p1, p2 = p2, segments[p2].keys()[0]

        serializer = TransectSerializer(orderedModels, many=True)
        return Response(serializer.data)
