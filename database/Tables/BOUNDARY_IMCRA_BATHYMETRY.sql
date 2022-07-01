-- Pre-calculated geometries of bathymetries per IMCRA boundary.

CREATE TABLE [dbo].[BOUNDARY_IMCRA_BATHYMETRY] (
  [Provincial_Bioregion] NVARCHAR(255) NOT NULL,
  [Mesoscale_Bioregion]  NVARCHAR(255) NULL,
  [bathymetry_category]  VARCHAR(10)   NOT NULL,
  [bathymetry_rank]      INT           NOT NULL,
  [geom]                 GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_IMCRA_BATHYMETRY] ([Provincial_Bioregion], [Mesoscale_Bioregion], [bathymetry_category], [bathymetry_rank], [geom])
SELECT
  [boundary].[Provincial_Bioregion],
  [boundary].[Mesoscale_Bioregion],
  [bathymetry].[CATEGORY] AS [bathymetry_category],
  [bathymetry].[RANK] AS [bathymetry_rank],
  [bathymetry].[geom]
FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry];

-- Use:
-- DECLARE @provincial_bioregion NVARCHAR(255) = 'Cocos (Keeling) Island Province';
-- DECLARE @mesoscale_bioregion  NVARCHAR(255) = NULL;

-- SELECT [bathymetry_category], geometry::UnionAggregate([geom])
-- FROM [dbo].[BOUNDARY_IMCRA_BATHYMETRY]
-- WHERE
--   ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
--   ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
-- GROUP BY [bathymetry_category];
