-- Pre-calculated geometries of habitats per IMCRA boundary. 

CREATE TABLE [dbo].[BOUNDARY_IMCRA_HABITAT] (
  [Provincial_Bioregion] NVARCHAR(255) NOT NULL,
  [Mesoscale_Bioregion]  NVARCHAR(255) NULL,
  [habitat]              NVARCHAR(30)  NOT NULL,
  [geom]                 GEOMETRY      NOT NULL,
  [area]                 FLOAT         NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT] ([Provincial_Bioregion], [Mesoscale_Bioregion], [habitat], [geom], [area])
SELECT
  [boundary].[Provincial_Bioregion],
  [boundary].[Mesoscale_Bioregion],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom],
  [habitat].[geom].STArea() AS [area]
FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];

-- Use:
-- DECLARE @provincial_bioregion NVARCHAR(255) = 'Cocos (Keeling) Island Province';
-- DECLARE @mesoscale_bioregion  NVARCHAR(255) = NULL;

-- SELECT [habitat], [area]
-- FROM [dbo].[BOUNDARY_IMCRA_HABITAT]
-- WHERE
--   ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
--   ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
-- GROUP BY [habitat];
