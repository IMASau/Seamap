-- Calculate all the intersections along a path, together with an attribute of interest from the intersected polygon.
-- Assumption is that the input is a LINESTRING, and the intersected geometries are all polygons.

IF OBJECT_ID('path_intersections') IS NULL
  EXEC('create function path_intersections(@geom geometry) returns @split_geom table (geom geometry) as begin return; end');
GO

ALTER FUNCTION path_intersections(@transect geometry, @habitat HabitatTableType READONLY)
RETURNS @TransectSegments TABLE (
  name varchar(max),
  segment geometry
)
AS
BEGIN
  DECLARE @buffered geometry = @transect.STBuffer(0.0000001);

  WITH
  -- first narrow to polygons we care about (those that intersect our line of interest):
  valid_polygons AS (
    SELECT name, geom
    FROM @habitat
    WHERE geom.STIntersects(@transect) = 1)
  ,
  total_extent AS (
    SELECT geometry::UnionAggregate(geom) AS agg FROM valid_polygons
  )
  ,
  -- Polygons may not cover the entire length of the line; the difference gives us the bits outside our habitat data:
  external_area AS (
    --select @line.STEnvelope().STBuffer(1).STDifference(agg) as diff from total_extent
    SELECT ISNULL(@buffered.STDifference(agg), @buffered) AS diff FROM total_extent
  )
  ,
  -- Intermediate results are just all the intersections; we'll flatten them as the final step:
  intermediate_results AS (
    SELECT name, geom.STIntersection(@transect) AS intersections
    FROM
      (SELECT name, geom FROM valid_polygons
      UNION ALL
      SELECT 'External' AS name, diff AS geom FROM external_area
      ) AS results
  )

  -- Finally, flatten into the results table:
  INSERT INTO @TransectSegments
    SELECT ir.name, simplified.geom
    FROM intermediate_results AS ir
    CROSS APPLY split_geom(ir.intersections) AS simplified;
  RETURN;
END;
GO
