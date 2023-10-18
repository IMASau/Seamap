# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from decimal import Decimal, getcontext
from io import BytesIO
import numbers
import os
import shapefile
import subprocess
import tempfile
import zipfile
import logging
from shapely.geometry import box

from catalogue.models import Layer, RegionReport, KeyedLayer, Pressure
from catalogue.serializers import RegionReportSerializer, LayerSerializer, PressureSerializer
from collections import defaultdict, namedtuple

from django.conf import settings
from django.contrib.gis.geos import GEOSGeometry
from django.db import connections, ProgrammingError
from django.db.models.functions import Coalesce
from django.http import FileResponse
from rest_framework.decorators import action, api_view, renderer_classes
from rest_framework.renderers import BaseRenderer, TemplateHTMLRenderer, JSONRenderer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.serializers import ValidationError


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
    select layer_name, habitat as name, geom from (
        {}
    ) polys
    order by priority asc;

SELECT segments.segment.STStartPoint().STX as 'start x',
       segments.segment.STStartPoint().STY as 'start y',
       segments.segment.STEndPoint().STX   as 'end x',
       segments.segment.STEndPoint().STY   as 'start y',
       segments.segment.STLength()         as 'length',
       segments.layer_name,
       segments.name
FROM(
    SELECT segment, layer_name, name
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

SQL_GET_AMP_BOUNDARIES = """
SELECT DISTINCT
  Network AS network,
  Park AS park,
  Zone_Category AS zone,
  IUCN_Category AS zone_iucn,
  Zone_ID AS zone_id
FROM VW_BOUNDARY_AMP;
"""

SQL_GET_IMCRA_BOUNDARIES = """
SELECT DISTINCT
  Provincial_Bioregion AS provincial_bioregion,
  Mesoscale_Bioregion AS mesoscale_bioregion
FROM VW_BOUNDARY_IMCRA;
"""

SQL_GET_MEOW_BOUNDARIES = """
SELECT DISTINCT
  Realm as realm,
  Province as province,
  Ecoregion AS ecoregion
FROM VW_BOUNDARY_MEOW;
"""

SQL_GET_AMP_BOUNDARY_AREA = "SELECT dbo.AMP_BOUNDARY_geom(%s, %s, %s, %s, %s).STArea() / 1000000"
SQL_GET_IMCRA_BOUNDARY_AREA = "SELECT dbo.IMCRA_BOUNDARY_geom(%s, %s).STArea() / 1000000"
SQL_GET_MEOW_BOUNDARY_AREA = "SELECT dbo.MEOW_BOUNDARY_geom(%s, %s, %s).STArea() / 1000000"

SQL_GEOM_BINARY_COL = ", geometry::UnionAggregate(geom).STAsBinary() as geom" # Only include if we need to because faster queries without geometry aggregation

SQL_GET_AMP_HABITAT_STATS = """
DECLARE @netname       NVARCHAR(254) = %s;
DECLARE @resname       NVARCHAR(254) = %s;
DECLARE @zonename      NVARCHAR(254) = %s;
DECLARE @zoneiucn      NVARCHAR(5)   = %s;
DECLARE @zone_id       NVARCHAR(10)  = %s;
DECLARE @boundary_area FLOAT         = %s;

SELECT
  boundary.habitat,
  SUM(boundary.area) / 1000000 AS area,
  100 * SUM(boundary.area) / (
    SELECT SUM(area)
    FROM BOUNDARY_AMP_HABITAT
    WHERE
      (Network = @netname OR @netname IS NULL) AND
      (Park = @resname OR @resname IS NULL) AND
      (Zone_Category = @zonename OR @zonename IS NULL) AND
      (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
      (Zone_ID = @zone_id OR @zone_id IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(boundary.area) / 1000000) / @boundary_area AS total_percentage,
  descriptor.colour AS color
  {}
FROM BOUNDARY_AMP_HABITAT AS boundary
JOIN catalogue_habitatdescriptor AS descriptor
ON boundary.habitat = descriptor.name
WHERE
  (Network = @netname OR @netname IS NULL) AND
  (Park = @resname OR @resname IS NULL) AND
  (Zone_Category = @zonename OR @zonename IS NULL) AND
  (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
  (Zone_ID = @zone_id OR @zone_id IS NULL)
GROUP BY boundary.habitat, descriptor.colour;
"""

SQL_GET_IMCRA_HABITAT_STATS = """
DECLARE @provincial_bioregion NVARCHAR(255) = %s;
DECLARE @mesoscale_bioregion  NVARCHAR(255) = %s;

SELECT
  habitat,
  SUM(area) / 1000000 AS area,
  100 * SUM(area) / (
    SELECT SUM(area)
    FROM BOUNDARY_IMCRA_HABITAT
    WHERE
      (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
      (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(area) / 1000000) / %s AS total_percentage
  {}
FROM BOUNDARY_IMCRA_HABITAT
WHERE
  (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
  (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
GROUP BY habitat;
"""

SQL_GET_MEOW_HABITAT_STATS = """
DECLARE @realm     NVARCHAR(255) = %s;
DECLARE @province  NVARCHAR(255) = %s;
DECLARE @ecoregion NVARCHAR(255) = %s;

SELECT
  habitat,
  SUM(area) / 1000000 AS area,
  100 * SUM(area) / (
    SELECT SUM(area)
    FROM BOUNDARY_MEOW_HABITAT
    WHERE
      (Realm = @realm OR @realm IS NULL) AND
      (Province = @province OR @province IS NULL) AND
      (Ecoregion = @ecoregion OR @ecoregion IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(area) / 1000000) / %s AS total_percentage
  {}
FROM BOUNDARY_MEOW_HABITAT
WHERE
  (Realm = @realm OR @realm IS NULL) AND
  (Province = @province OR @province IS NULL) AND
  (Ecoregion = @ecoregion OR @ecoregion IS NULL)
GROUP BY habitat;
"""

SQL_GET_AMP_BATHYMETRY_STATS = """
DECLARE @netname       NVARCHAR(254) = %s;
DECLARE @resname       NVARCHAR(254) = %s;
DECLARE @zonename      NVARCHAR(254) = %s;
DECLARE @zoneiucn      NVARCHAR(5)   = %s;
DECLARE @zone_id       NVARCHAR(10)  = %s;
DECLARE @boundary_area FLOAT         = %s;

SELECT
  boundary.bathymetry_resolution as resolution,
  boundary.bathymetry_rank as rank,
  SUM(boundary.area) / 1000000 AS area,
  100 * SUM(boundary.area) / (
    SELECT SUM(area)
    FROM BOUNDARY_AMP_BATHYMETRY
    WHERE
      (Network = @netname OR @netname IS NULL) AND
      (Park = @resname OR @resname IS NULL) AND
      (Zone_Category = @zonename OR @zonename IS NULL) AND
      (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
      (Zone_ID = @zone_id OR @zone_id IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(boundary.area) / 1000000) / @boundary_area AS total_percentage,
  descriptor.colour AS color
  {}
FROM BOUNDARY_AMP_BATHYMETRY AS boundary
JOIN catalogue_habitatdescriptor AS descriptor
ON boundary.bathymetry_resolution = descriptor.name
WHERE
  (Network = @netname OR @netname IS NULL) AND
  (Park = @resname OR @resname IS NULL) AND
  (Zone_Category = @zonename OR @zonename IS NULL) AND
  (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
  (Zone_ID = @zone_id OR @zone_id IS NULL)
GROUP BY boundary.bathymetry_resolution, boundary.bathymetry_rank, descriptor.colour
ORDER BY boundary.bathymetry_rank;
"""

SQL_GET_IMCRA_BATHYMETRY_STATS = """
DECLARE @provincial_bioregion NVARCHAR(255) = %s;
DECLARE @mesoscale_bioregion  NVARCHAR(255) = %s;

SELECT
  bathymetry_resolution as resolution,
  bathymetry_rank as rank,
  SUM(area) / 1000000 AS area,
  100 * SUM(area) / (
    SELECT SUM(area)
    FROM BOUNDARY_IMCRA_BATHYMETRY
    WHERE
      (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
      (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(area) / 1000000) / %s AS total_percentage
  {}
FROM BOUNDARY_IMCRA_BATHYMETRY
WHERE
  (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
  (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
GROUP BY bathymetry_resolution, bathymetry_rank;
"""

SQL_GET_MEOW_BATHYMETRY_STATS = """
DECLARE @realm     NVARCHAR(255) = %s;
DECLARE @province  NVARCHAR(255) = %s;
DECLARE @ecoregion NVARCHAR(255) = %s;

SELECT
  bathymetry_resolution as resolution,
  bathymetry_rank as rank,
  SUM(area) / 1000000 AS area,
  100 * SUM(area) / (
    SELECT SUM(area)
    FROM BOUNDARY_MEOW_BATHYMETRY
    WHERE
      (Realm = @realm OR @realm IS NULL) AND
      (Province = @province OR @province IS NULL) AND
      (Ecoregion = @ecoregion OR @ecoregion IS NULL)
  ) AS mapped_percentage,
  100 * (SUM(area) / 1000000) / %s AS total_percentage
  {}
FROM BOUNDARY_MEOW_BATHYMETRY
WHERE
  (Realm = @realm OR @realm IS NULL) AND
  (Province = @province OR @province IS NULL) AND
  (Ecoregion = @ecoregion OR @ecoregion IS NULL)
GROUP BY bathymetry_resolution, bathymetry_rank;
"""

SQL_GET_AMP_HABITAT_OBS_GLOBALARCHIVE = """
SET NOCOUNT ON;

DECLARE @netname  NVARCHAR(254) = %s;
DECLARE @resname  NVARCHAR(254) = %s;
DECLARE @zonename NVARCHAR(254) = %s;
DECLARE @zoneiucn NVARCHAR(5)   = %s;
DECLARE @zone_id  NVARCHAR(10)  = %s;

DECLARE @observations TABLE (
  campaign_name    NVARCHAR(MAX) NOT NULL, 
  deployment_id    NVARCHAR(MAX) NOT NULL, 
  date             DATE          NOT NULL,
  method           NVARCHAR(100) NULL,
  video_time       INT           NULL,
  video_annotation NVARCHAR(1)   NULL
);

INSERT INTO @observations
SELECT
  observation.CAMPAIGN_NAME AS campaign_name,
  observation.DEPLOYMENT_ID AS deployment_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.video_time,
  observation.data_open AS video_annotation
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE
  WHERE
    (Network = @netname OR @netname IS NULL) AND
    (Park = @resname OR @resname IS NULL) AND
    (Zone_Category = @zonename OR @zonename IS NULL) AND
    (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
    (Zone_ID = @zone_id OR @zone_id IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_GLOBALARCHIVE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_IMCRA_HABITAT_OBS_GLOBALARCHIVE = """
SET NOCOUNT ON;

DECLARE @provincial_bioregion NVARCHAR(255) = %s;
DECLARE @mesoscale_bioregion  NVARCHAR(255) = %s;

DECLARE @observations TABLE (
  campaign_name    NVARCHAR(MAX) NOT NULL, 
  deployment_id    NVARCHAR(MAX) NOT NULL, 
  date             DATE          NOT NULL,
  method           NVARCHAR(100) NULL,
  video_time       INT           NULL,
  video_annotation NVARCHAR(1)   NULL
);

INSERT INTO @observations
SELECT
  observation.CAMPAIGN_NAME AS campaign_name,
  observation.DEPLOYMENT_ID AS deployment_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.video_time,
  observation.data_open AS video_annotation
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_IMCRA_HABITAT_OBS_GLOBALARCHIVE
  WHERE
    (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_GLOBALARCHIVE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_MEOW_HABITAT_OBS_GLOBALARCHIVE = """
SET NOCOUNT ON;

DECLARE @realm     NVARCHAR(255) = %s;
DECLARE @province  NVARCHAR(255) = %s;
DECLARE @ecoregion NVARCHAR(255) = %s;

DECLARE @observations TABLE (
  campaign_name    NVARCHAR(MAX) NOT NULL, 
  deployment_id    NVARCHAR(MAX) NOT NULL, 
  date             DATE          NOT NULL,
  method           NVARCHAR(100) NULL,
  video_time       INT           NULL,
  video_annotation NVARCHAR(1)   NULL
);

INSERT INTO @observations
SELECT
  observation.CAMPAIGN_NAME AS campaign_name,
  observation.DEPLOYMENT_ID AS deployment_id,
  observation.DATE AS date,
  observation.METHOD AS method,
  observation.video_time,
  observation.data_open AS video_annotation
FROM (
  SELECT DISTINCT observation
  FROM BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE
  WHERE
    (Realm = @realm OR @realm IS NULL) AND
    (Province = @province OR @province IS NULL) AND
    (Ecoregion = @ecoregion OR @ecoregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_GLOBALARCHIVE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_AMP_HABITAT_OBS_SEDIMENT = """
SET NOCOUNT ON;

DECLARE @netname  NVARCHAR(254) = %s;
DECLARE @resname  NVARCHAR(254) = %s;
DECLARE @zonename NVARCHAR(254) = %s;
DECLARE @zoneiucn NVARCHAR(5)   = %s;
DECLARE @zone_id  NVARCHAR(10)  = %s;

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
    (Park = @resname OR @resname IS NULL) AND
    (Zone_Category = @zonename OR @zonename IS NULL) AND
    (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
    (Zone_ID = @zone_id OR @zone_id IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SEDIMENT AS observation
ON observation.SAMPLE_ID = boundary_observation.observation;
"""

SQL_GET_IMCRA_HABITAT_OBS_SEDIMENT = """
SET NOCOUNT ON;

DECLARE @provincial_bioregion NVARCHAR(255) = %s;
DECLARE @mesoscale_bioregion  NVARCHAR(255) = %s;

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
  FROM BOUNDARY_IMCRA_HABITAT_OBS_SEDIMENT
  WHERE
    (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SEDIMENT AS observation
ON observation.SAMPLE_ID = boundary_observation.observation;
"""

SQL_GET_MEOW_HABITAT_OBS_SEDIMENT = """
SET NOCOUNT ON;

DECLARE @realm     NVARCHAR(255) = %s;
DECLARE @province  NVARCHAR(255) = %s;
DECLARE @ecoregion NVARCHAR(255) = %s;

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
  FROM BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT
  WHERE
    (Realm = @realm OR @realm IS NULL) AND
    (Province = @province OR @province IS NULL) AND
    (Ecoregion = @ecoregion OR @ecoregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SEDIMENT AS observation
ON observation.SAMPLE_ID = boundary_observation.observation;
"""

SQL_GET_AMP_HABITAT_OBS_SQUIDLE = """
SET NOCOUNT ON;

DECLARE @netname  NVARCHAR(254) = %s;
DECLARE @resname  NVARCHAR(254) = %s;
DECLARE @zonename NVARCHAR(254) = %s;
DECLARE @zoneiucn NVARCHAR(5)   = %s;
DECLARE @zone_id  NVARCHAR(10)  = %s;

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
    (Park = @resname OR @resname IS NULL) AND
    (Zone_Category = @zonename OR @zonename IS NULL) AND
    (IUCN_Category = @zoneiucn OR @zoneiucn IS NULL) AND
    (Zone_ID = @zone_id OR @zone_id IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SQUIDLE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_IMCRA_HABITAT_OBS_SQUIDLE = """
SET NOCOUNT ON;

DECLARE @provincial_bioregion NVARCHAR(255) = %s;
DECLARE @mesoscale_bioregion  NVARCHAR(255) = %s;

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
  FROM BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE
  WHERE
    (Provincial_Bioregion = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    (Mesoscale_Bioregion = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SQUIDLE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_MEOW_HABITAT_OBS_SQUIDLE = """
SET NOCOUNT ON;

DECLARE @realm     NVARCHAR(255) = %s;
DECLARE @province  NVARCHAR(255) = %s;
DECLARE @ecoregion NVARCHAR(255) = %s;

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
  FROM BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE
  WHERE
    (Realm = @realm OR @realm IS NULL) AND
    (Province = @province OR @province IS NULL) AND
    (Ecoregion = @ecoregion OR @ecoregion IS NULL)
) AS boundary_observation
JOIN VW_HABITAT_OBS_SQUIDLE AS observation
ON observation.DEPLOYMENT_ID = boundary_observation.observation;
"""

SQL_GET_OBSERVATIONS = "SELECT * FROM @observations;"

SQL_GET_GLOBALARCHIVE_STATS = """
DECLARE @method NVARCHAR(MAX) = (
  STUFF(
    (
      SELECT
        ', ' + CONVERT(NVARCHAR(MAX), method)
      FROM (
        SELECT DISTINCT method
        FROM @observations AS T1
      ) AS methods
      FOR XML PATH('')
    ),
    1, 2, ''
  )
);

SELECT
  COUNT(DISTINCT deployment_id) AS deployments,
  COUNT(DISTINCT campaign_name) AS campaigns,
  MIN(date) AS start_date,
  MAX(date) AS end_date,
  @method AS method,
  SUM(video_time) / 60 AS video_time,
  SUM(CASE WHEN video_annotation='Y' THEN 1 END) AS video_annotations
FROM @observations AS T1;
"""

SQL_GET_SEDIMENT_STATS = """
DECLARE @method NVARCHAR(MAX) = (
  STUFF(
    (
      SELECT
        ', ' + CONVERT(NVARCHAR(MAX), method)
      FROM (
        SELECT DISTINCT method
        FROM @observations AS T1
      ) AS methods
      FOR XML PATH('')
    ),
    1, 2, ''
  )
);

SELECT
  COUNT(DISTINCT sample_id) AS samples,
  SUM(CASE WHEN analysed='YES' THEN 1 END) AS analysed,
  COUNT(DISTINCT survey) AS survey,
  MIN(date) AS start_date,
  MAX(date) AS end_date,
  @method AS method
FROM @observations AS T1;
"""

SQL_GET_SQUIDLE_STATS = """
DECLARE @method NVARCHAR(MAX) = (
  STUFF(
    (
      SELECT
        ', ' + CONVERT(NVARCHAR(MAX), method)
      FROM (
        SELECT DISTINCT method
        FROM @observations AS T1
      ) AS methods
      FOR XML PATH('')
    ),
    1, 2, ''
  )
);

SELECT
  COUNT(DISTINCT deployment_id) AS deployments,
  COUNT(DISTINCT campaign_name) AS campaigns,
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
FROM @observations AS T1;
"""

SQL_GET_NETWORK_SQUIDLE_URL = "SELECT NRandImage_URL FROM VW_IMAGERY_SQUIDLE_AMP_NETWORK WHERE NETWORK = %s;"
SQL_GET_PARK_SQUIDLE_URL = "SELECT NRandImage_URL FROM VW_IMAGERY_SQUIDLE_AMP_PARK WHERE PARK = %s;"

SQL_GET_DATA_IN_REGION = """
DECLARE @region GEOMETRY = GEOMETRY::STGeomFromText(%s, 4326);
SELECT DISTINCT layer_id FROM (
  SELECT * FROM [dbo].[layer_feature]
  WHERE @region.STIntersects(geom) = 1
) AS [T1];
"""

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
    layer_stmt = ('select layer_name, habitat, geom, {} as priority '
                  'from SeamapAus_Regions_VIEW '
                  'where lower(layer_name) = %s and geom.STIntersects(@line) = 1')
    layers_placeholder = '\nUNION ALL\n'.join( layer_stmt.format(i) for i,_ in enumerate(layers) )

    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_TRANSECT.format(layers_placeholder),
                       [linestring] + layers)
        while True:
            try:
                for row in cursor.fetchall():
                    [startx, starty, endx, endy, length, layer_name, name] = row
                    p1, p2 = (D(startx), D(starty)), (D(endx), D(endy))
                    segment = p1, p2, layer_name, name, length
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

    p1, p2, _, _, _ = start_segment
    if p1 != start_pt:
        p1, p2 = p2, p1
    start_distance = 0
    # p1 is the start point; it will always be the "known" point, and p2 is the next one to find:
    while True:
        _, _, layer_name, name, length = segments[p1][p2]
        end_distance = start_distance + length
        end_percentage = start_percentage + 100*length/float(distance)
        ordered_segments.append({'layer_name': layer_name,
                                 'name': name,
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


OGR2OGR_DB_TEMPLATE = "MSSQL:Driver={driver};Server={server},{port};Database={database};uid={user};pwd={password};"
OGR2OGR_SQL_TEMPLATE = "select {geom}.STIntersection(geometry::Point({x1},{y1},3112).STUnion(geometry::Point({x2},{y2},3112)).STEnvelope()) geom,{columns} from {table} where {geom}.STIntersects(geometry::Point({x1},{y1},3112).STUnion(geometry::Point({x2},{y2},3112)).STEnvelope()) = 1"

# Not a view function itself, but returns a Response.  Used as an
# optional performance gain in the subset view below.
def ogr2ogr_subset(table_name, geom_col, colnames, bounds):
    db = settings.DATABASES['default']
    connstr = OGR2OGR_DB_TEMPLATE.format(driver=settings.OGR2OGR_DRIVER or db['OPTIONS']['driver'],
                                         server=db['HOST'],
                                         port=db['PORT'] or '1433',
                                         database=db['NAME'],
                                         user=db['USER'],
                                         password=db['PASSWORD'])
    [x1,y1,x2,y2] = bounds
    querystr = OGR2OGR_SQL_TEMPLATE.format(geom=f"\"{geom_col}\"",
                                           table=table_name,
                                           columns=','.join([f"\"{c}\"" for c in colnames]),
                                           x1=x1,y1=y1,x2=x2,y2=y2)
    # Note, the contents will be cleaned up along with the directory:
    with tempfile.TemporaryDirectory(prefix=table_name) as tmpdir:
        subprocess.run([settings.OGR2OGR_PATH,
                        '--config', 'MSSQLSPATIAL_USE_GEOMETRY_COLUMNS', 'NO',
                        '-f', 'ESRI Shapefile',
                        f'{table_name}.shp',
                        connstr,
                        '-sql', querystr,
                        '-overwrite',
                        '-nlt', 'PROMOTE_TO_MULTI',
                        '-lco', 'SHPT=POLYGON',
                        '-a_srs', 'EPSG:3112'],
                       check=True, cwd=tmpdir)
        file_listing = os.listdir(tmpdir)
        zipfile_name = os.path.join(tmpdir, table_name)
        zf = zipfile.ZipFile(zipfile_name, 'w')
        for f in file_listing:
            zf.write(os.path.join(tmpdir, f), f)
        zf.close()
        return FileResponse(open(zipfile_name, 'rb'),
                            as_attachment=True,
                            filename=f'{table_name}.zip',
                            content_type='application/zip')


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
        columns = list(cursor.columns(table=table_name))

        if not columns:
            raise AssertionError(f"Table {table_name} does not exist; cannot generate subset shapefile")

        for col in columns:
            colname = col[NAME_IDX]
            typename = col[TYPE_IDX]
            if typename == 'geometry':
                geom_col = colname
            else:
                colnames.append(colname)

        # Optionally use ogr2ogr for performance gains:
        if settings.OGR2OGR_PATH:
            return ogr2ogr_subset(table_name, geom_col, colnames, parse_bounds(bounds_str))

        subset_sql = SQL_GET_SUBSET.format(geom_col, ','.join([f"\"{c}\"" for c in colnames]), table_name, f"\"{geom_col}\"")
        cursor.execute(subset_sql, parse_bounds(bounds_str))
        data = cursor.fetchall()
        field_metadata = cursor.description

    return Response({'data': data, 'file_name': table_name, 'fields': field_metadata},
                    content_type='application/zip',
                    headers={'Content-Disposition':
                             'attachment; filename="{}.zip"'.format(table_name)})

@action(detail=False)
@api_view()
def amp_boundaries(request):
    boundaries = []

    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_AMP_BOUNDARIES)
        columns = [col[0] for col in cursor.description]
        namedrow = namedtuple('Result', columns)
        results = [namedrow(*row) for row in cursor.fetchall()]

        boundaries = [row._asdict() for row in results]
    return Response(boundaries)

@action(detail=False)
@api_view()
def imcra_boundaries(request):
    boundaries = []

    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_IMCRA_BOUNDARIES)
        columns = [col[0] for col in cursor.description]
        namedrow = namedtuple('Result', columns)
        results = [namedrow(*row) for row in cursor.fetchall()]

        boundaries = [row._asdict() for row in results]
    return Response(boundaries)

@action(detail=False)
@api_view()
def meow_boundaries(request):
    boundaries = []

    with connections['transects'].cursor() as cursor:
        cursor.execute(SQL_GET_MEOW_BOUNDARIES)
        columns = [col[0] for col in cursor.description]
        namedrow = namedtuple('Result', columns)
        results = [namedrow(*row) for row in cursor.fetchall()]

        boundaries = [row._asdict() for row in results]
    return Response(boundaries)

@action(detail=False)
@api_view()
@renderer_classes((JSONRenderer, ShapefileRenderer))
def habitat_statistics(request):
    params = {k: v or None for k, v in request.query_params.items()}
    boundary_type        = params.get('boundary-type')
    network              = params.get('network')
    park                 = params.get('park')
    zone                 = params.get('zone')
    zone_iucn            = params.get('zone-iucn')
    zone_id              = params.get('zone-id')
    provincial_bioregion = params.get('provincial-bioregion')
    mesoscale_bioregion  = params.get('mesoscale-bioregion ')
    realm                = params.get('realm')
    province             = params.get('province')
    ecoregion            = params.get('ecoregion')
    is_download = request.accepted_renderer.format == 'raw'

    habitat_stats = []

    with connections['transects'].cursor() as cursor:
        if boundary_type == 'amp':
            cursor.execute(SQL_GET_AMP_BOUNDARY_AREA, [network, park, zone, zone_iucn, zone_id])
        elif boundary_type == 'imcra':
            cursor.execute(SQL_GET_IMCRA_BOUNDARY_AREA, [provincial_bioregion, mesoscale_bioregion])
        elif boundary_type == 'meow':
            cursor.execute(SQL_GET_MEOW_BOUNDARY_AREA, [realm, province, ecoregion])
        else:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        try:
            boundary_area = float(cursor.fetchone()[0])

            if boundary_type == 'amp':
                cursor.execute(SQL_GET_AMP_HABITAT_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [network, park, zone, zone_iucn, zone_id, boundary_area])
            elif boundary_type == 'imcra':
                cursor.execute(SQL_GET_IMCRA_HABITAT_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [provincial_bioregion, mesoscale_bioregion, boundary_area])
            elif boundary_type == 'meow':
                cursor.execute(SQL_GET_MEOW_HABITAT_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [realm, province, ecoregion, boundary_area])
            else:
                return Response(status=status.HTTP_400_BAD_REQUEST)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            if is_download:
                boundary_name = ''
                if boundary_type == 'amp':
                    boundary_name = ' - '.join([v for v in [network, park, zone, zone_iucn, zone_id] if v])
                elif boundary_type == 'imcra':
                    boundary_name = ' - '.join([v for v in [provincial_bioregion, mesoscale_bioregion] if v])
                elif boundary_type == 'meow':
                    boundary_name = ' - '.join([v for v in [realm, province, ecoregion] if v])
                else:
                    return Response(status=status.HTTP_400_BAD_REQUEST)
                return Response({'data': results,
                                 'fields': cursor.description,
                                 'file_name': boundary_name},
                                content_type='application/zip',
                                headers={'Content-Disposition': 'attachment; filename="{}.zip"'.format(boundary_name)})
            
            habitat_stats = [row._asdict() for row in results]

            mapped_area = float(sum(v['area'] for v in habitat_stats))
            mapped_percentage = 100 * mapped_area / boundary_area
            habitat_stats.append({'habitat': None, 'area': mapped_area, 'mapped_percentage': None, 'total_percentage': mapped_percentage})
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        else:
            return Response(habitat_stats)

@action(detail=False)
@api_view()
@renderer_classes((JSONRenderer, ShapefileRenderer))
def bathymetry_statistics(request):
    params = {k: v or None for k, v in request.query_params.items()}
    boundary_type        = params.get('boundary-type')
    network              = params.get('network')
    park                 = params.get('park')
    zone                 = params.get('zone')
    zone_iucn            = params.get('zone-iucn')
    zone_id              = params.get('zone-id')
    provincial_bioregion = params.get('provincial-bioregion')
    mesoscale_bioregion  = params.get('mesoscale-bioregion ')
    realm                = params.get('realm')
    province             = params.get('province')
    ecoregion            = params.get('ecoregion')
    is_download = request.accepted_renderer.format == 'raw'

    bathymetry_stats = []

    with connections['transects'].cursor() as cursor:
        if boundary_type == 'amp':
            cursor.execute(SQL_GET_AMP_BOUNDARY_AREA, [network, park, zone, zone_iucn, zone_id])
        elif boundary_type == 'imcra':
            cursor.execute(SQL_GET_IMCRA_BOUNDARY_AREA, [provincial_bioregion, mesoscale_bioregion])
        elif boundary_type == 'meow':
            cursor.execute(SQL_GET_MEOW_BOUNDARY_AREA, [realm, province, ecoregion])
        else:
            return Response(status=status.HTTP_400_BAD_REQUEST)
        
        try:
            boundary_area = float(cursor.fetchone()[0])

            if boundary_type == 'amp':
                cursor.execute(SQL_GET_AMP_BATHYMETRY_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [network, park, zone, zone_iucn, zone_id, boundary_area])
            elif boundary_type == 'imcra':
                cursor.execute(SQL_GET_IMCRA_BATHYMETRY_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [provincial_bioregion, mesoscale_bioregion, boundary_area])
            elif boundary_type == 'meow':
                cursor.execute(SQL_GET_MEOW_BATHYMETRY_STATS.format(SQL_GEOM_BINARY_COL if is_download else ''), [realm, province, ecoregion, boundary_area])
            else:
                return Response(status=status.HTTP_400_BAD_REQUEST)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            if is_download:
                boundary_name = ''
                if boundary_type == 'amp':
                    boundary_name = ' - '.join([v for v in [network, park, zone, zone_iucn, zone_id] if v])
                elif boundary_type == 'imcra':
                    boundary_name = ' - '.join([v for v in [provincial_bioregion, mesoscale_bioregion] if v])
                elif boundary_type == 'meow':
                    boundary_name = ' - '.join([v for v in [realm, province, ecoregion] if v])
                else:
                    return Response(status=status.HTTP_400_BAD_REQUEST)
                return Response({'data': results,
                                 'fields': cursor.description,
                                 'file_name': boundary_name},
                                content_type='application/zip',
                                headers={'Content-Disposition': 'attachment; filename="{}.zip"'.format(boundary_name)})

            bathymetry_stats = [row._asdict() for row in results]

            mapped_area = float(sum(v['area'] for v in bathymetry_stats))
            mapped_percentage = 100 * mapped_area / boundary_area
            bathymetry_stats.append({'resolution': None, 'rank': None, 'area': mapped_area, 'mapped_percentage': None, 'total_percentage': mapped_percentage})
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        else:
            return Response(bathymetry_stats)

@action(detail=False)
@api_view()
def habitat_observations(request):
    params = {k: v or None for k, v in request.query_params.items()}
    boundary_type        = params.get('boundary-type')
    network              = params.get('network')
    park                 = params.get('park')
    zone                 = params.get('zone')
    zone_iucn            = params.get('zone-iucn')
    zone_id              = params.get('zone-id')
    provincial_bioregion = params.get('provincial-bioregion')
    mesoscale_bioregion  = params.get('mesoscale-bioregion ')
    realm                = params.get('realm')
    province             = params.get('province')
    ecoregion            = params.get('ecoregion')

    global_archive = None
    sediment = None
    squidle = None

    with connections['transects'].cursor() as cursor:
        try:
            # Global Archives stats
            if boundary_type == 'amp':
                cursor.execute(SQL_GET_AMP_HABITAT_OBS_GLOBALARCHIVE + SQL_GET_GLOBALARCHIVE_STATS, [network, park, zone, zone_iucn, zone_id])
            elif boundary_type == 'imcra':
                cursor.execute(SQL_GET_IMCRA_HABITAT_OBS_GLOBALARCHIVE + SQL_GET_GLOBALARCHIVE_STATS, [provincial_bioregion, mesoscale_bioregion])
            elif boundary_type == 'meow':
                cursor.execute(SQL_GET_MEOW_HABITAT_OBS_GLOBALARCHIVE + SQL_GET_GLOBALARCHIVE_STATS, [realm, province, ecoregion])
            else:
                return Response(status=status.HTTP_400_BAD_REQUEST)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            result = namedrow(*cursor.fetchone())
            global_archive = result._asdict()

            # Marine Sediments stats
            if boundary_type == 'amp':
                cursor.execute(SQL_GET_AMP_HABITAT_OBS_SEDIMENT + SQL_GET_SEDIMENT_STATS, [network, park, zone, zone_iucn, zone_id])
            elif boundary_type == 'imcra':
                cursor.execute(SQL_GET_IMCRA_HABITAT_OBS_SEDIMENT + SQL_GET_SEDIMENT_STATS, [provincial_bioregion, mesoscale_bioregion])
            elif boundary_type == 'meow':
                cursor.execute(SQL_GET_MEOW_HABITAT_OBS_SEDIMENT + SQL_GET_SEDIMENT_STATS, [realm, province, ecoregion])
            else:
                return Response(status=status.HTTP_400_BAD_REQUEST)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            result = namedrow(*cursor.fetchone())
            sediment = result._asdict()

            # SQUIDLE observations
            if boundary_type == 'amp':
                cursor.execute(SQL_GET_AMP_HABITAT_OBS_SQUIDLE + SQL_GET_SQUIDLE_STATS, [network, park, zone, zone_iucn, zone_id])
            elif boundary_type == 'imcra':
                cursor.execute(SQL_GET_IMCRA_HABITAT_OBS_SQUIDLE + SQL_GET_SQUIDLE_STATS, [provincial_bioregion, mesoscale_bioregion])
            elif boundary_type == 'meow':
                cursor.execute(SQL_GET_MEOW_HABITAT_OBS_SQUIDLE + SQL_GET_SQUIDLE_STATS, [realm, province, ecoregion])
            else:
                return Response(status=status.HTTP_400_BAD_REQUEST)
            
            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            result = namedrow(*cursor.fetchone())
            squidle = result._asdict()
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        else:
            return Response({'global_archive': global_archive, 'sediment': sediment, 'squidle': squidle})

@action(detail=False)
@api_view()
def region_report_data(request):
    params = {k: v or None for k, v in request.query_params.items()}
    network = params.get('network')
    park    = params.get('park')

    rr = RegionReport.objects.get(network=network, park=park)
    data = RegionReportSerializer(rr).data

    data["parks"] = [{'park': v.park, 'slug': v.slug} for v in RegionReport.objects.filter(network=network) if v.park] if park == None else None
    
    network_region = RegionReport.objects.get(network=network, park=None)
    data["network"] = {'network': network_region.network, 'slug': network_region.slug}

    data["all_layers"] = [LayerSerializer(v.layer).data for v in KeyedLayer.objects.filter(keyword='data-report-minimap-panel1').order_by('-sort_key')]
    data["all_layers_boundary"] = LayerSerializer(KeyedLayer.objects.get(keyword='data-report-minimap-panel1-boundary').layer).data
    data["public_layers"] = [LayerSerializer(v.layer).data for v in KeyedLayer.objects.filter(keyword='data-report-minimap-panel2').order_by('-sort_key')]
    data["public_layers_boundary"] = LayerSerializer(KeyedLayer.objects.get(keyword='data-report-minimap-panel2-boundary').layer).data
    data["pressures"] = [PressureSerializer(v).data for v in Pressure.objects.filter(region_report=rr.id)]
    data["app_boundary_layer"] = LayerSerializer(KeyedLayer.objects.get(keyword=('amp-park' if park != None else 'amp-network')).layer).data

    with connections['transects'].cursor() as cursor:
        try:
            cursor.execute(SQL_GET_PARK_SQUIDLE_URL if park else SQL_GET_NETWORK_SQUIDLE_URL, [park or network])
            entry = cursor.fetchone()
            data["squidle_url"] = entry[0] if entry else None # some parks do not have URLs, so in that event we just provide a null URL
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            pass

    return Response(data)

@action(detail=False)
@api_view()
def data_in_region(request):
    params = {k: v or None for k, v in request.query_params.items()}
    try:
        select = box(
            float(params['east']),
            float(params['south']),
            float(params['west']),
            float(params['north'])
        )
    except Exception as e:
        return Response(status=status.HTTP_400_BAD_REQUEST)
    else:
        with connections['transects'].cursor() as cursor:
            cursor.execute(SQL_GET_DATA_IN_REGION, [select.wkt])
            layer_ids = [row[0] for row in cursor.fetchall()]
        return Response(layer_ids)
