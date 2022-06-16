CREATE VIEW [dbo].[SeamapAus_BoundaryAreas_View] AS
SELECT
  [network],
  [park],
  [zone],
  [zone_iucn],
  SUM([area]) AS [area]
FROM (
  SELECT
    [network],
    [park],
    [zone],
    [zone_iucn],
    MAX([boundary_area]) AS [area]
  FROM [IMASSeamap].[dbo].[SeamapAus_HabitatStatistics_View]
  GROUP BY [polygon_id], [network], [park], [zone], [zone_iucn]
) AS [BoundaryAreas]
GROUP BY [network], [park], [zone], [zone_iucn];
