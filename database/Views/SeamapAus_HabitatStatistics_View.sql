CREATE VIEW [dbo].[SeamapAus_HabitatStatistics_View] AS
SELECT
  [NETNAME] AS [network],
  [RESNAME] AS [park],
  [ZONENAME] AS [zone],
  [ZONEIUCN] AS [zone_iucn],
  [habitat],
  SUM([area]) AS [area]
FROM [IMASSeamap].[dbo].[SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve]
JOIN [IMASSeamap].[dbo].[SeamapAus_Habitat_By_Region] ON
  [POLYGONID]=[region]
WHERE [habitat_layer_id]=95
GROUP BY [NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [habitat];
