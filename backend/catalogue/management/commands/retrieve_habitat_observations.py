
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

SQL_GET_AMP_HABITAT_OBS_GLOBALARCHIVE_STATS = """
SET NOCOUNT ON;

DECLARE @netname NVARCHAR(254) = %s;
DECLARE @resname NVARCHAR(254) = %s;

DECLARE @observations TABLE
(
  campaign_name NVARCHAR(MAX) NOT NULL, 
  deployment_id NVARCHAR(MAX) NOT NULL, 
  date          DATE          NOT NULL,
  method        NVARCHAR(100) NULL,
  video_time    INT           NULL
);

INSERT INTO @observations
SELECT
  observation.CAMPAIGN_NAME AS campaign_name,
  observation.DEPLOYMENT_ID AS deployment_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.video_time
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE
  WHERE
    (Network = @netname OR @netname IS NULL) AND
    (Park = @resname OR @resname IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_GLOBALARCHIVE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;

DECLARE @method NVARCHAR(MAX) = (
  SELECT
   STRING_AGG(
     CONVERT(NVARCHAR(MAX), method),
     ', '
   )
  FROM (
    SELECT DISTINCT method
    FROM @observations
  ) AS methods
);

SELECT
  COUNT(DISTINCT deployment_id) AS deployment_id,
  COUNT(DISTINCT campaign_name) AS campaign_name,
  MIN(date) AS start_date,
  MAX(date) AS end_date,
  @method AS method,
  SUM(video_time) / 60 AS video_time
FROM @observations;
"""

SQL_GET_AMP_HABITAT_OBS_SEDIMENT_STATS = """
SET NOCOUNT ON;

DECLARE @netname NVARCHAR(254) = %s;
DECLARE @resname NVARCHAR(254) = %s;

DECLARE @observations TABLE
(
  survey    NVARCHAR(MAX) NOT NULL, 
  sample_id NVARCHAR(MAX) NOT NULL, 
  date      DATE          NULL,
  method    NVARCHAR(MAX) NOT NULL,
  analysed  VARCHAR(3)    NOT NULL
);

INSERT INTO @observations
SELECT
  observation.SURVEY AS survey,
  observation.SAMPLE_ID AS sample_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.ANALYSED AS analysed
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_AMP_HABITAT_OBS_SEDIMENT
  WHERE
    (Network = @netname OR @netname IS NULL) AND
    (Park = @resname OR @resname IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SEDIMENT AS observation
ON observation.SAMPLE_ID = boundary_observation.observation;

DECLARE @method NVARCHAR(MAX) = (
  SELECT
   STRING_AGG(
     CONVERT(NVARCHAR(MAX), method),
     ', '
   )
  FROM (
    SELECT DISTINCT method
    FROM @observations
  ) AS methods
);

SELECT
  COUNT(DISTINCT sample_id) AS sample_id,
  SUM(CASE WHEN analysed='YES' THEN 1 END) AS analysed,
  COUNT(DISTINCT survey) AS survey,
  MIN(date) AS start_date,
  MAX(date) AS end_date,
  @method AS method
FROM @observations;
"""

SQL_GET_AMP_HABITAT_OBS_SQUIDLE_STATS = """
SET NOCOUNT ON;

DECLARE @netname NVARCHAR(254) = %s;
DECLARE @resname NVARCHAR(254) = %s;

DECLARE @observations TABLE
(
  campaign_name      NVARCHAR(254) NOT NULL,
  deployment_id      NVARCHAR(508) NOT NULL,
  date               DATE          NULL,
  method             NVARCHAR(254) NOT NULL,
  images             FLOAT         NOT NULL,
  total_annotations  FLOAT         NOT NULL,
  public_annotations FLOAT         NOT NULL
);

INSERT INTO @observations
SELECT
  observation.CAMPAIGN_NAME AS campaign_name,
  observation.DEPLOYMENT_ID AS deployment_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.images,
  observation.total_annotations,
  observation.public_annotations
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_AMP_HABITAT_OBS_SQUIDLE
  WHERE
    (Network = @netname OR @netname IS NULL) AND
    (Park = @resname OR @resname IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SQUIDLE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;

DECLARE @method NVARCHAR(MAX) = (
  SELECT
   STRING_AGG(
     CONVERT(NVARCHAR(MAX), method),
     ', '
   )
  FROM (
    SELECT DISTINCT method
    FROM @observations
  ) AS methods
);

SELECT
  COUNT(DISTINCT deployment_id) AS deployment_id,
  COUNT(DISTINCT campaign_name) AS campaign_name,
  MIN(date) AS start_date,
  MAX(date) AS end_date,
  @method AS method,
  CAST(
    SUM(images) AS INT
  ) AS images,
  CAST(
    SUM(total_annotations) AS INT
  ) AS total_annotations,
  CAST(
    SUM(public_annotations) AS INT
  ) AS public_annotations
FROM @observations;
"""

def generate_habitat_observations(cursor, network=None, park=None):

    filepath = f'habitat_observations/{network}/{park}.json' if park else f'habitat_observations/{network}.json'

    # Global Archives stats
    logging.info(f'Retrieving global archive statistics for {network}' + (f' - {park}' if park else ''))
    cursor.execute(SQL_GET_AMP_HABITAT_OBS_GLOBALARCHIVE_STATS, [network, park])

    columns = [col[0] for col in cursor.description]
    namedrow = namedtuple('Result', columns)
    result = namedrow(*cursor.fetchone())
    global_archive = result._asdict()
    global_archive['start_date'] = str(global_archive['start_date']) if global_archive['start_date'] else None
    global_archive['end_date'] = str(global_archive['end_date']) if global_archive['end_date'] else None

    # Marine Sediments stats
    logging.info(f'Retrieving marine sediments statistics for {network}' + (f' - {park}' if park else ''))
    cursor.execute(SQL_GET_AMP_HABITAT_OBS_SEDIMENT_STATS, [network, park])

    columns = [col[0] for col in cursor.description]
    namedrow = namedtuple('Result', columns)
    result = namedrow(*cursor.fetchone())
    sediment = result._asdict()
    sediment['start_date'] = str(sediment['start_date']) if sediment['start_date'] else None
    sediment['end_date'] = str(sediment['end_date']) if sediment['end_date'] else None

    # SQUIDLE observations
    logging.info(f'Retrieving squidle observations for {network}' + (f' - {park}' if park else ''))
    cursor.execute(SQL_GET_AMP_HABITAT_OBS_SQUIDLE_STATS, [network, park])

    columns = [col[0] for col in cursor.description]
    namedrow = namedtuple('Result', columns)
    result = namedrow(*cursor.fetchone())
    squidle = result._asdict()
    squidle['start_date'] = str(squidle['start_date']) if squidle['start_date'] else None
    squidle['end_date'] = str(squidle['end_date']) if squidle['end_date'] else None

    # saving
    logging.info(f'Saving habitat observations for {network}' + (f' - {park}' if park else ''))
    default_storage.delete(filepath)
    default_storage.save(filepath, ContentFile(json.dumps({'global_archive': global_archive, 'sediment': sediment, 'squidle': squidle})))


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

            # retrieve habitat observations
            for network in networks:
                generate_habitat_observations(cursor, network['network'])
            
            for park in parks:
                generate_habitat_observations(cursor, park['network'], park['park'])
            
