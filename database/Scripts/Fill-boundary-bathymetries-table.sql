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
  [boundary].[NETNAME],
  [boundary].[RESNAME],
  [boundary].[ZONENAME],
  [boundary].[ZONEIUCN],
  [bathymetry].[CATEGORY] AS [bathymetry_category],
  [bathymetry].[RANK] AS [bathymetry_rank],
  [bathymetry].[geom]
FROM [dbo].[BoundaryGeoms_View] AS [boundary]
CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry];
