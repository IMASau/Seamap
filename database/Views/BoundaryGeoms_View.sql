-- Contains the geometries for the smallest possible unit of boundaries. Geometries
-- can be selected by any combination of network, park, or zone.

CREATE VIEW [dbo].[BoundaryGeoms_View] AS
SELECT
  [NETNAME],
  [RESNAME],
  [ZONENAME],
  [ZONEIUCN],
  geometry::UnionAggregate([geom]) AS [geom]
FROM [dbo].[SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve]
GROUP BY [NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN];
