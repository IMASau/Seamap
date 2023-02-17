-- Contains the geometries for AMP boundaries. Geometries can be selected by any
-- combination of network, park, or zone.

CREATE VIEW [dbo].[VW_BOUNDARY_AMP] AS
SELECT
  [NETNAME] AS [Network],
  [RESNAME] AS [Park],
  [ZONENAME] AS [Zone_Category],
  [ZONEIUCN] AS [IUCN_Category],
  [POLYGONID] AS [Zone_ID],
  [geom]
FROM [dbo].[SeamapAus_BOUNDARIES_AMP2022]
WHERE [NETNAME] <> 'HIMI'
