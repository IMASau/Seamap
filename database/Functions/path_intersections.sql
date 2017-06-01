-- Calculate all the intersections along a path, together with an attribute of interest from the intersected polygon.
-- Assumption is that the input is a LINESTRING, and the intersected geometries are all polygons.

CREATE FUNCTION path_intersections(@transect geometry, @habitat HabitatTableType READONLY)
RETURNS @TransectSegments TABLE (
  name varchar(max),
  segment geometry
)
AS
BEGIN
  DECLARE @buffered geometry = @transect.STBuffer(0.01),
          @emptyshape geometry = geometry::STGeomFromText('GEOMETRYCOLLECTION EMPTY', 3112);

  WITH
  -- first narrow to polygons we care about (those that intersect our line of interest):
  valid_polygons AS (
    SELECT id, name, geom
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
  -- We have to handle overlaps (both in the datasets, and when using multiple datasets)
  overlappers (id, name, geom) AS (
    SELECT vl.id, vl.name, vr.geom
	FROM valid_polygons vl
	LEFT JOIN valid_polygons vr ON vl.geom.STIntersects(vr.geom) = 1 AND vl.id > vr.id
  )
  ,
  aggregated_overlappers (id, geom) AS (
    SELECT id, COALESCE(geometry::UnionAggregate(geom), geometry::STGeomFromText('POLYGON EMPTY', 3112))
	FROM overlappers
	GROUP BY id
  )
  ,
  valid_flattened_polygons (id, name, geom) AS (
    SELECT p.id, p.name, p.geom.STDifference(agg.geom)
	FROM valid_polygons p JOIN aggregated_overlappers agg
	ON p.id = agg.id
  )
  ,
  -- Intermediate results are just all the intersections; we'll flatten them as the final step:
  intermediate_results AS (
    SELECT name, geom.STIntersection(@transect) AS intersections
    FROM
      (SELECT name, geom FROM valid_flattened_polygons
      UNION ALL
      SELECT null AS name, diff AS geom FROM external_area
      ) AS results
  )

  -- Finally, flatten into the results table:
  INSERT INTO @TransectSegments
    SELECT ir.name, simplified.geom
    FROM intermediate_results AS ir
    CROSS APPLY simplify_geoms(ir.intersections) AS simplified;
  RETURN;
END;
GO
