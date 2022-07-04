-- Pre-calculated geometries of bathymetries per AMP boundary.

CREATE TABLE [dbo].[BOUNDARY_AMP_BATHYMETRY] (
  [Network]             NVARCHAR(254) NOT NULL,
  [Park]                NVARCHAR(254) NOT NULL,
  [Zone_Category]       NVARCHAR(254) NOT NULL,
  [IUCN_Zone]           NVARCHAR(5)   NOT NULL,
  [bathymetry_category] VARCHAR(10)   NOT NULL,
  [bathymetry_rank]     INT           NOT NULL,
  [geom]                GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_AMP_BATHYMETRY] ([Network], [Park], [Zone_Category], [IUCN_Zone], [bathymetry_category], [bathymetry_rank], [geom])
SELECT
  [boundary].[Network],
  [boundary].[Park],
  [boundary].[Zone_Category],
  [boundary].[IUCN_Zone],
  [bathymetry].[CATEGORY] AS [bathymetry_category],
  [bathymetry].[RANK] AS [bathymetry_rank],
  [bathymetry].[geom]
FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry];

-- Use:
-- DECLARE @network   NVARCHAR(254) = 'Coral Sea';
-- DECLARE @park      NVARCHAR(254) = NULL;
-- DECLARE @zone      NVARCHAR(254) = NULL;
-- DECLARE @zone_iucn NVARCHAR(5)   = NULL;

-- SELECT [bathymetry_category], geometry::UnionAggregate([geom])
-- FROM [dbo].[BOUNDARY_AMP_BATHYMETRY]
-- WHERE
--   ([Network] = @network OR @network IS NULL) AND
--   ([Park] = @park OR @park IS NULL) AND
--   ([Zone_Category] = @zone OR @zone IS NULL) AND
--   ([IUCN_Zone] = @zone_iucn OR @zone_iucn IS NULL)
-- GROUP BY [bathymetry_category];
