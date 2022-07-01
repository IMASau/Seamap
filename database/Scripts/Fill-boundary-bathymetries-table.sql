-- Pre-calculated geometries of bathymetries per boundary.

CREATE TABLE [dbo].[BoundaryBathymetries] (
  [NETNAME]             NVARCHAR(254) NOT NULL,
  [RESNAME]             NVARCHAR(254) NOT NULL,
  [ZONENAME]            NVARCHAR(254) NOT NULL,
  [ZONEIUCN]            NVARCHAR(5)   NOT NULL,
  [bathymetry_category] VARCHAR(10)   NOT NULL,
  [bathymetry_rank]     INT           NOT NULL,
  [geom]                GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BoundaryBathymetries] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [bathymetry_category], [bathymetry_rank], [geom])
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
-- FROM [dbo].[BoundaryBathymetries]
-- WHERE
--   ([NETNAME] = @network OR @network IS NULL) AND
--   ([RESNAME] = @park OR @park IS NULL) AND
--   ([ZONENAME] = @zone OR @zone IS NULL) AND
--   ([ZONEIUCN] = @zone_iucn OR @zone_iucn IS NULL)
-- GROUP BY [bathymetry_category];
