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
