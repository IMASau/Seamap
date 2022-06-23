CREATE FUNCTION [dbo].[habitat_intersections] (@boundary geometry)
RETURNS TABLE
AS
RETURN
(
  SELECT
    [CATEGORY],
    [geom].STIntersection(@boundary) AS [geom]
  FROM [dbo].[VW_HABITAT]
  WHERE [geom].STIntersects(@boundary) = 1
);
