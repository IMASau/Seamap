CREATE FUNCTION habitat_intersections (@boundary geometry)
RETURNS TABLE
AS
RETURN
(
  SELECT
    [CATEGORY] AS [habitat],
    [geom].STIntersection(@boundary) AS [geom]
  FROM [IMASSeamap].[dbo].[VW_Habitat]
  WHERE [geom].STIntersects(@boundary) = 1
);
GO
