-- Pre-calculated geometries of habitats per AMP boundary. 

CREATE TABLE [dbo].[BOUNDARY_AMP_HABITAT] (
  [Network]       NVARCHAR(254) NOT NULL,
  [Park]          NVARCHAR(254) NOT NULL,
  [Zone_Category] NVARCHAR(254) NOT NULL,
  [IUCN_Zone]     NVARCHAR(5)   NOT NULL,
  [habitat]       NVARCHAR(30)  NOT NULL,
  [geom]          GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT] ([Network], [Park], [Zone_Category], [IUCN_Zone], [habitat], [geom])
SELECT
  [boundary].[Network],
  [boundary].[Park],
  [boundary].[Zone_Category],
  [boundary].[IUCN_Zone],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom]
FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];
