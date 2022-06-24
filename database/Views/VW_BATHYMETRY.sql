-- Contains the many bathymetry geometries.

CREATE VIEW [dbo].[VW_BATHYMETRY] AS
SELECT
  [objectid],
  (
    SELECT RES_ACTUAL = CASE
      WHEN RES_ACTUAL <=2 THEN '2m or less'
      WHEN RES_ACTUAL >2 AND RES_ACTUAL <=10 THEN '3-10m'
      WHEN RES_ACTUAL >10 AND RES_ACTUAL <=20 THEN '11-20m'
      WHEN RES_ACTUAL >20 AND RES_ACTUAL <=50 THEN '21-50m'
      WHEN RES_ACTUAL >50 THEN '> 50m'
      WHEN objectid like '%dem%' THEN 'modelled'
      ELSE 'unknown'
    END
  ) AS [CATEGORY],
  (
    SELECT RES_ACTUAL = CASE
      WHEN RES_ACTUAL <=2 THEN 1
      WHEN RES_ACTUAL >2 AND RES_ACTUAL <=10 THEN 2
      WHEN RES_ACTUAL >10 AND RES_ACTUAL <=20 THEN 3
      WHEN RES_ACTUAL >20 AND RES_ACTUAL <=50 THEN 4
      WHEN RES_ACTUAL >50 THEN 5
      WHEN objectid like '%dem%' THEN 6
      ELSE 7
    END
  ) AS [RANK],
  [geom]
FROM [dbo].[Aus_bathymetry_ALL_SURVEYS]
