CREATE VIEW [dbo].[BoundaryGeoms_View] AS
SELECT
  [NETNAME],
  [RESNAME],
  [ZONENAME],
  [ZONEIUCN],
  geometry::UnionAggregate([geom]) AS [geom]
FROM [dbo].[SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve]
GROUP BY [NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN];
