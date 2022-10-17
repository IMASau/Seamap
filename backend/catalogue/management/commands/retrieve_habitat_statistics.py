
from collections import namedtuple
from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db import connections
import logging
import json

SQL_GET_AMP_NETWORKS = """
SELECT DISTINCT
  Network AS network
FROM VW_BOUNDARY_AMP;
"""

SQL_GET_AMP_PARKS = """
SELECT DISTINCT
  Network AS network,
  Park AS park
FROM VW_BOUNDARY_AMP;
"""

SQL_GET_AMP_BOUNDARY_AREA = "SELECT dbo.AMP_BOUNDARY_geom(%s, %s, NULL, NULL).STArea() / 1000000"

SQL_GET_AMP_HABITAT_STATS = """
DECLARE @netname       NVARCHAR(254) = %s;
DECLARE @resname       NVARCHAR(254) = %s;
DECLARE @boundary_area FLOAT         = %s;

SELECT
  boundary.habitat,
  boundary.area,
  boundary.mapped_percentage,
  boundary.total_percentage,
  descriptor.colour AS color
FROM (
  SELECT
    habitat,
    SUM(area) / 1000000 AS area,
    100 * SUM(area) / (
      SELECT SUM(area)
      FROM BOUNDARY_AMP_HABITAT
      WHERE
        (Network = @netname OR @netname IS NULL) AND
        (Park = @resname OR @resname IS NULL)
    ) AS mapped_percentage,
    100 * (SUM(area) / 1000000) / @boundary_area AS total_percentage
  FROM BOUNDARY_AMP_HABITAT
  WHERE
    (Network = @netname OR @netname IS NULL) AND
    (Park = @resname OR @resname IS NULL)
  GROUP BY habitat
) AS boundary
JOIN catalogue_habitatdescriptor AS descriptor
ON boundary.habitat = descriptor.name;
"""

def generate_habitat_statistics(cursor, network=None, park=None):
    logging.info(f'Retrieving habitat statistics for {network} - {park}' if park else f'Retrieving habitat statistics for {network}')

    filepath = f'habitat_statistics/{network}/{park}.json' if park else f'habitat_statistics/{network}.json'

    cursor.execute(SQL_GET_AMP_BOUNDARY_AREA, [network, park])

    boundary_area = float(cursor.fetchone()[0])
    cursor.execute(SQL_GET_AMP_HABITAT_STATS, [network, park, boundary_area])

    columns = [col[0] for col in cursor.description]
    namedrow = namedtuple('Result', columns)
    results = [namedrow(*row) for row in cursor.fetchall()]
    habitat_statistics = [row._asdict() for row in results]

    # saving
    logging.info(f'Saving habitat statistics for {network} - {park}' if park else f'Saving habitat statistics for {network}')
    default_storage.delete(filepath)
    default_storage.save(filepath, ContentFile(json.dumps(habitat_statistics)))


class Command(BaseCommand):
    def handle(self, *args, **options):

        with connections['default'].cursor() as cursor:

            # retrieve networks
            cursor.execute(SQL_GET_AMP_NETWORKS)
            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]
            networks = [row._asdict() for row in results]

            # retrieve parks
            cursor.execute(SQL_GET_AMP_PARKS)
            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]
            parks = [row._asdict() for row in results]

            # retrieve habitat statistics
            for network in networks:
                generate_habitat_statistics(cursor, network['network'])
            
            for park in parks:
                generate_habitat_statistics(cursor, park['network'], park['park'])
            
