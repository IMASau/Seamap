-- Calculate all the intersections along a path, together with an attribute of interest from the intersected polygon.
-- Assumption is that the input is a LINESTRING, and the intersected geometries are all polygons.
-- Any overlaps are clipped, with earlier polygons have priority over (clipping) later ones in @habitat.

CREATE FUNCTION path_intersections(@transect geometry, @habitat HabitatTableType READONLY)
RETURNS @TransectSegments TABLE (
  layer_name VARCHAR(max),
  name varchar(max),
  segment geometry
)
AS
BEGIN
  DECLARE @buffered geometry = @transect.STBuffer(0.01),
          @emptyshape geometry = geometry::STGeomFromText('GEOMETRYCOLLECTION EMPTY', 3112);

  WITH
  -- first narrow to polygons we care about (those that intersect our line of interest):
  -- UPDATE: we will do this refinement before invoking this procedure.  If using
  -- stand-alone, it might be convenient to reinstate this CTE, and replace all following
  -- instances of '@habitat' with 'valid_polygons':
  --valid_polygons AS (
  --  SELECT id, name, geom
  --  FROM @habitat
  --  WHERE geom.STIntersects(@transect) = 1)
  --,
  total_extent AS (
    SELECT geometry::UnionAggregate(geom) AS agg FROM @habitat
  )
  ,
  -- Polygons may not cover the entire length of the line; the difference gives us the bits outside our habitat data:
  external_area AS (
    SELECT ISNULL(@buffered.STDifference(agg), @buffered) AS diff FROM total_extent
  )
  ,
  -- We have to handle overlaps (both in the datasets, and when using multiple datasets)
  overlappers (id, layer_name, name, geom) AS (
    SELECT vl.id, vl.layer_name, vl.name, vr.geom
	FROM @habitat vl
	LEFT JOIN @habitat vr ON vl.geom.STIntersects(vr.geom) = 1 AND vl.id > vr.id
  )
  ,
  aggregated_overlappers (id, geom) AS (
    SELECT id, COALESCE(geometry::UnionAggregate(geom), geometry::STGeomFromText('POLYGON EMPTY', 3112))
	FROM overlappers
	GROUP BY id
  )
  ,
  valid_flattened_polygons (id, layer_name, name, geom) AS (
    SELECT p.id, p.layer_name, p.name, p.geom.STDifference(agg.geom)
	FROM @habitat p JOIN aggregated_overlappers agg
	ON p.id = agg.id
  )
  ,
  -- Intermediate results are just all the intersections; we'll flatten them as the final step:
  intermediate_results AS (
    SELECT layer_name, name, geom.STIntersection(@transect) AS intersections
    FROM
      (SELECT layer_name, name, geom FROM valid_flattened_polygons
      UNION ALL
      SELECT null AS layer_name, null AS name, diff AS geom FROM external_area
      ) AS results
  )

  -- Finally, flatten into the results table:
  INSERT INTO @TransectSegments
    SELECT layer.name AS layer_name, ir.name, simplified.geom
    FROM intermediate_results AS ir
    CROSS APPLY simplify_geoms(ir.intersections) AS simplified
    LEFT JOIN catalogue_layer AS layer ON layer.layer_name = ir.layer_name;
  RETURN;
END;
GO
