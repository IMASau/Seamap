-- Contains the geometry for each habitat.

CREATE VIEW [dbo].[VW_HABITAT] AS
SELECT
  [NAT_HAB_CL] AS [CATEGORY],
  geometry::UnionAggregate([geom]).MakeValid() AS [geom]
FROM [dbo].[FINALPRODUCT_SeamapAus]
GROUP BY [NAT_HAB_CL];
