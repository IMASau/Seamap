from rest_framework import viewsets
from rest_framework.response import Response
from rest_framework.decorators import list_route
from django.db import connection, ProgrammingError
from habitat.models import Transect
from habitat.serializers import TransectSerializer
from decimal import *





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



def list_to_coords(list):
    coords = []
    newlist = map(Decimal, list)
    newlist = map(lambda x: x * 1, newlist)
    coords = zip(*[iter(newlist)]*2)
    return coords

def coords_to_linsestring(coords):
    tmp = []
    for pair in coords:
        tmp.append(' '.join(map(str, pair)))
    linestring = ', '.join(tmp)
    return "LINESTRING(" + linestring + ")"

class HabitatViewSet(viewsets.ViewSet):

    # request as .../transect/?line= x1, y1, x2, y2, ..., xn, yn
    @list_route()
    def transect(self, request):
        tolerance = 0.0001  #minimum length for non-zero line in sql query
        starts = {}
        ends = {}
        orderedModels = []
        precision = 6       #significant figures for floating point comparison

        getcontext().prec = precision


        coords = list_to_coords(request.query_params.get('line').split(","))
        linestring = coords_to_linsestring(coords)

        with connection.cursor() as cursor:
            cursor.execute(SQL_GET_TRANSECT, [linestring, tolerance])
            while True:
                try:
                    for row in cursor.fetchall():
                        [startx,starty,endx, endy, name] = row
                        starts[(Decimal(startx) * 1, Decimal(starty) * 1)] = (endx, endy, name)
                        ends[(Decimal(endx) * 1, Decimal(endy) * 1)] = (startx, starty, name)
                    break
                except ProgrammingError:
                    if not cursor.nextset():
                        break


        # For some reason the start and end points are being swapped (presumably in path_intersections function)
        # If the issue is found, only the if part of this code is required.
        start = coords[0]

        if start in starts:
            for i in range(0, len(starts)):
                (startx, starty) = start
                (endx, endy, name) = starts[start]
                model = Transect(name = name, startx = startx, starty = starty, endx = endx, endy=endy)
                orderedModels.append(model)
                start = (Decimal(endx) * 1, Decimal(endy) * 1)
        elif start in ends:
            for i in range(0, len(ends)):
                (endx, endy) = start
                (startx, starty, name) = ends[start]
                model = Transect(name = name, startx = endx, starty = endy, endx = startx, endy=starty)
                orderedModels.append(model)
                start = (Decimal(startx) * 1, Decimal(starty) * 1)
        else:
            print "error"

        serializer = TransectSerializer(orderedModels, many=True)
        return Response(serializer.data)