-- Pre-calculated geometries of habitats per AMP boundary. 

CREATE TABLE [dbo].[BOUNDARY_AMP_HABITAT] (
  [Network]       NVARCHAR(254) NOT NULL,
  [Park]          NVARCHAR(254) NOT NULL,
  [Zone_Category] NVARCHAR(254) NOT NULL,
  [IUCN_Category]     NVARCHAR(5)   NOT NULL,
  [habitat]       NVARCHAR(30)  NOT NULL,
  [geom]          GEOMETRY      NOT NULL,
  [area]          FLOAT         NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT] ([Network], [Park], [Zone_Category], [IUCN_Category], [habitat], [geom], [area])
SELECT
  [boundary].[Network],
  [boundary].[Park],
  [boundary].[Zone_Category],
  [boundary].[IUCN_Category],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom],
  [habitat].[geom].STArea() AS [area]
FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];

-- Use:
-- DECLARE @network   NVARCHAR(254) = 'Coral Sea';
-- DECLARE @park      NVARCHAR(254) = NULL;
-- DECLARE @zone      NVARCHAR(254) = NULL;
-- DECLARE @zone_iucn NVARCHAR(5)   = NULL;

-- SELECT [habitat], [area]
-- FROM [dbo].[BOUNDARY_AMP_HABITAT]
-- WHERE
--   ([Network] = @network OR @network IS NULL) AND
--   ([Park] = @park OR @park IS NULL) AND
--   ([Zone_Category] = @zone OR @zone IS NULL) AND
--   ([IUCN_Category] = @zone_iucn OR @zone_iucn IS NULL)
-- GROUP BY [habitat];
