from decimal import *
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
declare @line geometry = %s;
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


def my_decimal(number):
    return Decimal(number) * 1

# FIXME: most of this isn't actually needed, since the coords come in
# in the format we expect already (ie, leave it as a string, don't
# need to reformat)
def list_to_coords(list):
    decimals = map(my_decimal, list)
    return zip(*[iter(decimals)]*2)


def coords_to_linsestring(coords):
    linestring = ','.join(' '.join(map(str, pair)) for pair in coords)
    return "LINESTRING(" + linestring + ")"


class HabitatViewSet(viewsets.ViewSet):

    # request as .../transect/?line= x1, y1, x2, y2, ..., xn, yn&layers=layer1,layer2..
    @list_route()
    def transect(self, request):
        if 'line' not in request.query_params:
            raise ValidationError("Required parameter 'line' is missing")
        if 'layers' not in request.query_params:
            raise ValidationError("Required parameter 'layers' is missing")

        tolerance = 0.0001  # minimum length for non-zero line in sql query
        starts = {}
        ends = {}
        orderedModels = []
        precision = 6       # significant figures for floating point comparison

        getcontext().prec = precision

        # coords = list_to_coords(request.query_params.get('line').split(","))
        # linestring = coords_to_linsestring(coords)
        linestring = "LINESTRING(" + request.query_params.get('line') + ")"

        layers = request.query_params.get('layers').lower().split(',')
        layers_placeholder = ','.join(['%s'] * len(layers))

        with connections['transects'].cursor() as cursor:
            cursor.execute(SQL_GET_TRANSECT.format(layers_placeholder),
                           [linestring] + layers + [tolerance])
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
