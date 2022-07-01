-- Contains the geometries for MEOW boundaries. Geometries can be selected by any
-- combination of realm, province, or ecoregion.

CREATE VIEW [dbo].[VW_BOUNDARY_MEOW] AS
SELECT
  [REALM] AS [Realm],
  [PROVINCE] AS [Province],
  [ECOREGION] AS [Ecoregion],
  [geom]
FROM [dbo].[SeamapAus_BOUNDARIES_MEOW3112_FULL];
