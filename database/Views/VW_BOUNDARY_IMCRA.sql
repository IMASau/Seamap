-- Contains the geometries for the smallest possible unit of IMCRA boundaries.
-- Geometries can be selected by any combination of network, park, or zone.

CREATE VIEW [dbo].[VW_BOUNDARY_IMCRA] AS
SELECT
  [pb_name] AS [Provincial_Bioregion],
  (
    CASE
      WHEN meso_name = 'nil' THEN NULL
      ELSE meso_name
    END
  ) AS [Mesoscale_Bioregion],
  [geom]
FROM [SeamapAus_BOUNDARIES_IMCRA_FULL]
