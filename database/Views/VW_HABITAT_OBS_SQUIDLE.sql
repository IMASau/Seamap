-- Contains the geometry for each SQUIDLE habitat observation.

CREATE VIEW [dbo].[VW_HABITAT_OBS_SQUIDLE] AS
SELECT
  [CAMPAIGN.NAME] AS [CAMPAIGN_NAME],
  [name]+[KEY] AS [DEPLOYMENT_ID],
  CAST([created_at] AS DATE) AS [DATE],
  [PLATFORM.NAME] AS [METHOD],
  [no_media_items] AS [images],
  [no_annotations] AS [annotations],
  [geom]
FROM [TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS];
