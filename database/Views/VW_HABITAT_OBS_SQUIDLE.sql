-- Contains the geometry for each SQUIDLE habitat observation.

CREATE VIEW [dbo].[VW_HABITAT_OBS_SQUIDLE] AS
SELECT
  [CAMPAIGN_NAME] AS [CAMPAIGN_NAME],
  [name]+[KEY] AS [DEPLOYMENT_ID],
  CAST([date] AS DATE) AS [DATE],
  [PLATFORM_NAME] AS [METHOD],
  [media_count] AS [images],
  [total_annotation_count] AS [total_annotations],
  [public_annotation_count] as [public_annotations],
  [geom]
FROM  [TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS];


