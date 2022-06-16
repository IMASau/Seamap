CREATE VIEW [dbo].[SeamapAus_HabitatStatistics_View] AS
SELECT
  [SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve].[NETNAME] AS [network],
  [SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve].[RESNAME] AS [park],
  [ZONENAME] AS [zone],
  [ZONEIUCN] AS [zone_iucn],
  [habitat],
  [area],
  [boundary_area],
  [POLYGONID] as [polygon_id]
FROM [IMASSeamap].[dbo].[SeamapAus_AMP2018_ZONES_UNIQUE_codepreserve]
JOIN [IMASSeamap].[dbo].[SeamapAus_Habitat_By_Region] ON
  [POLYGONID]=[region];
-- We don't limit ourselves to one habitat layer, because we need all the zone polygons so we can get a zone's total area
