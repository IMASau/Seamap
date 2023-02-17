-- Pre-calculated geometries of bathymetries per IMCRA boundary.

CREATE TABLE [dbo].[BOUNDARY_IMCRA_BATHYMETRY] (
  [Provincial_Bioregion]  NVARCHAR(255) NOT NULL,
  [Mesoscale_Bioregion]   NVARCHAR(255) NULL,
  [bathymetry_resolution] VARCHAR(10)   NOT NULL,
  [bathymetry_rank]       INT           NOT NULL,
  [geom]                  GEOMETRY      NOT NULL,
  [area]                  FLOAT         NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_IMCRA_BATHYMETRY] ([Provincial_Bioregion], [Mesoscale_Bioregion], [bathymetry_resolution], [bathymetry_rank], [geom], [area])
SELECT
  [boundary].[Provincial_Bioregion],
  [boundary].[Mesoscale_Bioregion],
  [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
  [bathymetry].[RANK] AS [bathymetry_rank],
  [bathymetry].[geom],
  [bathymetry].[geom].STArea() AS [area]
FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry];

-- Use:
-- DECLARE @provincial_bioregion NVARCHAR(255) = 'Cocos (Keeling) Island Province';
-- DECLARE @mesoscale_bioregion  NVARCHAR(255) = NULL;

-- SELECT [bathymetry_resolution], [area]
-- FROM [dbo].[BOUNDARY_IMCRA_BATHYMETRY]
-- WHERE
--   ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
--   ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
-- GROUP BY [bathymetry_resolution];
