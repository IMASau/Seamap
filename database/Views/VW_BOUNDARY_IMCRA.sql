-- Contains the geometries for IMCRA boundaries. Geometries can be selected by any
-- combination of provincial and mesoscale bioregions.

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
FROM [SeamapAus_BOUNDARIES_IMCRA_FULL];
