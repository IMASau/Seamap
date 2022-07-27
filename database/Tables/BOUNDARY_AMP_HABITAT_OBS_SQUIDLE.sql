-- Pre-calculated SQUIDLE habitat observations per AMP boundary.

CREATE TABLE [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE] (
  [Network]       NVARCHAR(254) NOT NULL,
  [Park]          NVARCHAR(254) NOT NULL,
  [Zone_Category] NVARCHAR(254) NOT NULL,
  [IUCN_Category]     NVARCHAR(5)   NOT NULL,
  [observation]   NVARCHAR(480) NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE] ([Network], [Park], [Zone_Category], [IUCN_Category], [observation])
SELECT
  [boundary].[Network],
  [boundary].[Park],
  [boundary].[Zone_Category],
  [boundary].[IUCN_Category],
  [observation].[DEPLOYMENT_ID] AS [observation]
FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation];

-- Use:
-- DECLARE @network   NVARCHAR(254) = 'Coral Sea';
-- DECLARE @park      NVARCHAR(254) = NULL;
-- DECLARE @zone      NVARCHAR(254) = NULL;
-- DECLARE @zone_iucn NVARCHAR(5)   = NULL;

-- SELECT [observation]
-- FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE]
-- WHERE
--   ([Network] = @network OR @network IS NULL) AND
--   ([Park] = @park OR @park IS NULL) AND
--   ([Zone_Category] = @zone OR @zone IS NULL) AND
--   ([IUCN_Category] = @zone_iucn OR @zone_iucn IS NULL)
-- GROUP BY [observation];
