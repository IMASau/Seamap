-- Takes any geometry and finds out what bathymetry geometries from
-- UniqueBathymetryGeoms intersect with that geometry. Returns the intersecting
-- bathymetry categories, ranks, and corresponding intersecting geometries as a
-- table.

CREATE FUNCTION unique_bathymetry_intersections (@boundary GEOMETRY)
RETURNS TABLE
AS
RETURN
(
  SELECT
    [CATEGORY],
    [RANK],
    [geom].STIntersection(@boundary) AS [geom]
  FROM [dbo].[UniqueBathymetryGeoms]
  WHERE [geom].STIntersects(@boundary) = 1
);
