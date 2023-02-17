-- Takes any geometry and finds out what habitats from VW_Habitat intersect with
-- that geometry. Returns the intersecting habitats and corresponding intersecting
-- geometries as a table.

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
