-- Contains the geometry for each Global Archive habitat observation.

CREATE VIEW [dbo].[VW_HABITAT_OBS_GLOBALARCHIVE] AS
SELECT
  [CAMPAIGNID] AS [CAMPAIGN_NAME],
  [CAMPAIGNID]+[DEPLOYMENTID] AS DEPLOYMENT_ID,
  CONVERT(
    DATE,
    (
      CASE
        WHEN LEN([DATE]) < 10
          THEN SUBSTRING([DATE], 3, 2) + '/0' + LEFT([DATE], 1) + RIGHT([DATE], 5)
        WHEN LEN([DATE]) = 10 AND SUBSTRING([DATE], 4, 2) > 12
          THEN SUBSTRING([DATE], 4, 2) + '/' + LEFT([DATE], 2) + RIGHT([DATE], 5)
        ELSE [DATE]
      END
    ),
    103
  ) AS [DATE],
  [DEPLOYMENT_METHOD] AS [METHOD],
  [deployment_mins] AS [video_time],
  [geom]
FROM [TRANSFORM_GLOBALARCHIVE_DEPLOYMENT_POINTS];
