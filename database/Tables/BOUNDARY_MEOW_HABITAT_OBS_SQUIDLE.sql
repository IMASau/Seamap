-- Pre-calculated SQUIDLE habitat observations per MEOW boundary.

CREATE TABLE [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE] (
  [Realm]       NVARCHAR(255) NOT NULL,
  [Province]    NVARCHAR(255) NOT NULL,
  [Ecoregion]   NVARCHAR(255) NOT NULL,
  [observation] NVARCHAR(480) NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE] ([Realm], [Province], [Ecoregion], [observation])
SELECT
  [boundary].[Realm],
  [boundary].[Province],
  [boundary].[Ecoregion],
  [observation].[DEPLOYMENT_ID] AS [observation]
FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation];

-- Use:
-- DECLARE @realm     NVARCHAR(255) = 'Temperate Southern Africa';
-- DECLARE @province  NVARCHAR(255) = NULL;
-- DECLARE @ecoregion NVARCHAR(255) = NULL;

-- SELECT [observation]
-- FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE]
-- WHERE
--   ([Realm] = @realm OR @realm IS NULL) AND
--   ([Province] = @province OR @province IS NULL) AND
--   ([Ecoregion] = @ecoregion OR @ecoregion IS NULL)
-- GROUP BY [observation];
