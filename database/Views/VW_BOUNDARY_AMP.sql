-- Contains the geometries for the smallest possible unit of AMP boundaries.
-- Geometries can be selected by any combination of network, park, or zone.

CREATE VIEW [dbo].[VW_BOUNDARY_AMP] AS
  SELECT
    [NETNAME] AS [Network],
    [RESNAME] AS [Park],
    [ZONENAME] AS [Zone_Category],
    [ZONEIUCN] AS [IUCN_Zone],
    [geom]
  FROM [dbo].[SeamapAus_BOUNDARIES_AMP2022];
