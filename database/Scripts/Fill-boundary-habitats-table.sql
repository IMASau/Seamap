-- Pre-calculated geometries of habitats per boundary. 

CREATE TABLE [dbo].[BoundaryHabitats] (
  [NETNAME]  NVARCHAR(254) NOT NULL,
  [RESNAME]  NVARCHAR(254) NOT NULL,
  [ZONENAME] NVARCHAR(254) NOT NULL,
  [ZONEIUCN] NVARCHAR(5)   NOT NULL,
  [habitat]  NVARCHAR(30)  NOT NULL,
  [geom]     GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BoundaryHabitats] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [habitat], [geom])
SELECT
  [boundary].[Network],
  [boundary].[Park],
  [boundary].[Zone_Category],
  [boundary].[IUCN_Zone],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom]
FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];


-- Use:
-- DECLARE @network   NVARCHAR(254) = 'Coral Sea';
-- DECLARE @park      NVARCHAR(254) = NULL;
-- DECLARE @zone      NVARCHAR(254) = NULL;
-- DECLARE @zone_iucn NVARCHAR(5)   = NULL;

-- SELECT [habitat], geometry::UnionAggregate([geom])
-- FROM [dbo].[BoundaryHabitats]
-- WHERE
--   ([NETNAME] = @network OR @network IS NULL) AND
--   ([RESNAME] = @park OR @park IS NULL) AND
--   ([ZONENAME] = @zone OR @zone IS NULL) AND
--   ([ZONEIUCN] = @zone_iucn OR @zone_iucn IS NULL)
-- GROUP BY [habitat];
