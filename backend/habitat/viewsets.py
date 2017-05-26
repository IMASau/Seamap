from collections import defaultdict
from decimal import Decimal, getcontext
from django.db import connections, ProgrammingError
from rest_framework import viewsets
from rest_framework.decorators import list_route
from rest_framework.response import Response
from rest_framework.serializers import ValidationError


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
) as segments;
"""


def D(number):
    "Return the (probably) string, quantized to an acceptable number of decimal places"
    return Decimal(number).quantize(Decimal('0.1'))


class HabitatViewSet(viewsets.ViewSet):

    # request as .../transect/?line= x1 y1,x2 y2, ...,xn yn&layers=layer1,layer2..
    @list_route()
    def transect(self, request):
        if 'line' not in request.query_params:
            raise ValidationError("Required parameter 'line' is missing")
        if 'layers' not in request.query_params:
            raise ValidationError("Required parameter 'layers' is missing")

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

        layers = request.query_params.get('layers').lower().split(',')
        layers_placeholder = ','.join(['%s'] * len(layers))

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
