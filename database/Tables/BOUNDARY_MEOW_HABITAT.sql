-- Pre-calculated geometries of habitats per MEOW boundary. 

CREATE TABLE [dbo].[BOUNDARY_MEOW_HABITAT] (
  [Realm]     NVARCHAR(255) NOT NULL,
  [Province]  NVARCHAR(255) NOT NULL,
  [Ecoregion] NVARCHAR(255) NOT NULL,
  [habitat]   NVARCHAR(30)  NOT NULL,
  [geom]      GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT] ([Realm], [Province], [Ecoregion], [habitat], [geom])
SELECT
  [boundary].[Realm],
  [boundary].[Province],
  [boundary].[Ecoregion],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom]
FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];

-- Use:
-- DECLARE @realm     NVARCHAR(255) = 'Temperate Southern Africa';
-- DECLARE @province  NVARCHAR(255) = NULL;
-- DECLARE @ecoregion NVARCHAR(255) = NULL;

-- SELECT [habitat], geometry::UnionAggregate([geom])
-- FROM [dbo].[BOUNDARY_MEOW_HABITAT]
-- WHERE
--   ([Realm] = @realm OR @realm IS NULL) AND
--   ([Province] = @province OR @province IS NULL) AND
--   ([Ecoregion] = @ecoregion OR @ecoregion IS NULL)
-- GROUP BY [habitat];
