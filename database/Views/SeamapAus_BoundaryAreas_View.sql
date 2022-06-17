CREATE VIEW [dbo].[SeamapAus_BoundaryAreas_View] AS
SELECT
  [NETNAME] AS [network],
  [RESNAME] AS [park],
  [ZONENAME] AS [zone],
  [ZONEIUCN] AS [zone_iucn],
  SUM([geom].STArea()) AS [area]
FROM [IMASSeamap].[dbo].[SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve]
GROUP BY [NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN];
