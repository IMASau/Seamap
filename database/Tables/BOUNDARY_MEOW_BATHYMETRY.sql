-- Pre-calculated geometries of bathymetries per MEOW boundary.

CREATE TABLE [dbo].[BOUNDARY_MEOW_BATHYMETRY] (
  [Realm]               NVARCHAR(255) NOT NULL,
  [Province]            NVARCHAR(255) NOT NULL,
  [Ecoregion]           NVARCHAR(255) NOT NULL,
  [bathymetry_category] VARCHAR(10)   NOT NULL,
  [bathymetry_rank]     INT           NOT NULL,
  [geom]                GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_MEOW_BATHYMETRY] ([Realm], [Province], [Ecoregion], [bathymetry_category], [bathymetry_rank], [geom])
SELECT
  [boundary].[Realm],
  [boundary].[Province],
  [boundary].[Ecoregion],
  [bathymetry].[CATEGORY] AS [bathymetry_category],
  [bathymetry].[RANK] AS [bathymetry_rank],
  [bathymetry].[geom]
FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry];

-- Use:
-- DECLARE @realm     NVARCHAR(255) = 'Temperate Southern Africa';
-- DECLARE @province  NVARCHAR(255) = NULL;
-- DECLARE @ecoregion NVARCHAR(255) = NULL;

-- SELECT [bathymetry_category], geometry::UnionAggregate([geom])
-- FROM [dbo].[BOUNDARY_MEOW_BATHYMETRY]
-- WHERE
--   ([Realm] = @realm OR @realm IS NULL) AND
--   ([Province] = @province OR @province IS NULL) AND
--   ([Ecoregion] = @ecoregion OR @ecoregion IS NULL)
-- GROUP BY [bathymetry_category];
