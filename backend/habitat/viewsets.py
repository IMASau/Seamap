from decimal import *
from django.db import connection, ProgrammingError
from rest_framework import viewsets
from rest_framework.decorators import list_route
from rest_framework.response import Response
from habitat.models import Transect
from habitat.serializers import TransectSerializer

SQL_GET_TRANSECT = """
declare @line geometry = %s;
declare @tmphabitat as HabitatTableType;

insert into @tmphabitat select name, geom from Polygons;

SELECT segments.segment.STStartPoint().STX as 'start x',
        segments.segment.STStartPoint().STY as 'start y',
        segments.segment.STEndPoint().STX as 'end x',
        segments.segment.STEndPoint().STY as 'start y',
        segments.name
FROM(
    SELECT segment, name
    FROM path_intersections(@line, @tmphabitat)
    WHERE segment.STLength() > %s
) as segments;
"""


def my_decimal(number):
    return Decimal(number) * 1


def list_to_coords(list):
    decimals = map(my_decimal, list)
    return zip(*[iter(decimals)]*2)


def coords_to_linsestring(coords):
    linestring = ','.join(' '.join(map(str, pair)) for pair in coords)
    return "LINESTRING(" + linestring + ")"


class HabitatViewSet(viewsets.ViewSet):

    # request as .../transect/?line= x1, y1, x2, y2, ..., xn, yn
    @list_route()
    def transect(self, request):
        tolerance = 0.0001  # minimum length for non-zero line in sql query
        starts = {}
        ends = {}
        orderedModels = []
        precision = 6       # significant figures for floating point comparison

        getcontext().prec = precision

        coords = list_to_coords(request.query_params.get('line').split(","))
        linestring = coords_to_linsestring(coords)

        with connection.cursor() as cursor:
            cursor.execute(SQL_GET_TRANSECT, [linestring, tolerance])
            while True:
                try:
                    for row in cursor.fetchall():
                        [startx, starty, endx, endy, name] = row
                        starts[(my_decimal(startx) * 1, my_decimal(starty) * 1)] = (endx, endy, name)
                        ends[(my_decimal(endx) * 1, my_decimal(endy) * 1)] = (startx, starty, name)
                    break
                except ProgrammingError:
                    if not cursor.nextset():
                        break

        # Lines don't really have a direction, so we can't make assumptions
        # about how they will be returned
        start = coords[0]

        for i in range(0, len(starts)):
            (startx, starty) = start
            if start in starts:
                (endx, endy, name) = starts[start]
            else:
                (endx, endy, name) = ends[start]
            model = Transect(name=name, startx=startx, starty=starty, endx=endx, endy=endy)
            orderedModels.append(model)
            start = (my_decimal(endx) * 1, my_decimal(endy) * 1)

        serializer = TransectSerializer(orderedModels, many=True)
        return Response(serializer.data)
