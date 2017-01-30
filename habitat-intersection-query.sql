--insert into Polygons (id, name, geom)
--values
-- (1, 'One', Geometry::STGeomFromText('POLYGON ((10 10,20 10,20 20,10 20,10 10))', 4326)),
-- (2, 'Two', Geometry::STGeomFromText('POLYGON ((20 10,30 10,30 20,20 20,20 10))', 4326)),
-- (3, 'Three', Geometry::STGeomFromText('POLYGON ((30 10,40 10,40 20,30 20,30 10))', 4326));


-- insert into Lines (id,geom)
-- values
-- (1, Geometry::STGeomFromText('LINESTRING (15 25,15 5)', 4326)),              -- one polygon, top to bottom
-- (2, Geometry::STGeomFromText('LINESTRING (5 15,45 15)', 4326)),              -- through all three polygons
-- (3, Geometry::STGeomFromText('LINESTRING (25 5,25 25,35 25,35 5)', 4326)),   -- up through one, across, down through another
-- (4, Geometry::STGeomFromText('LINESTRING (32 17,17 17)', 4326)),             -- right to left, from middle of one, through another, and to the middle of another
-- (5, Geometry::STGeomFromText('LINESTRING (45 12,38 12,38 18,45 18)', 4326)), -- outside into one, loop around and out again
-- (6, Geometry::STGeomFromText('LINESTRING (12 12,12 5,17 5,17 12)', 4326)),   -- inside-to-out-to-inside-again (same polygon)
-- (7, Geometry::STGeomFromText('LINESTRING (32 22,43 22)', 4326));             -- completely outside

-- select id, geom from polygons
-- union all
-- select id, geom from lines;

-- Given a geometry, return a table of component geometries all of whom have STNumGeometries() == 1:
IF OBJECT_ID('split_geom') IS NULL
  EXEC('create function split_geom(@geom geometry) returns @split_geom table (geom geometry) as begin return; end');
GO
ALTER FUNCTION split_geom (@geom geometry)
RETURNS @split_geom TABLE (geom geometry)
AS
BEGIN
  DECLARE
    @numgeoms int = @geom.STNumGeometries();
  WITH Nums AS (
    SELECT top (ISNULL(@numgeoms,0)) ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) AS number
    FROM master..spt_values t1 cross join master..spt_values t2 -- should provide plenty of size
  )
  --
  INSERT INTO @split_geom (geom)
  SELECT @geom.STGeometryN(n.number)
  FROM (select @geom g) geomtable inner join Nums n on n.number <= @numgeoms;
  RETURN;
END;
GO

declare @line     geometry = (select geom from Lines where id = 1);
declare @buffered geometry = @line.STBuffer(0.0000001);

with
-- first narrow to polygons we care about (those that intersect our line of interest):
valid_polygons as (
  select id, name, geom
  from Polygons
  where geom.STIntersects(@line) = 1)
,
total_extent as (
  select geometry::UnionAggregate(geom) as agg from valid_polygons
)
,
-- Polygons may not cover the entire length of the line; the difference gives us the bits outside our habitat data:
external_area as (
  --select @line.STEnvelope().STBuffer(1).STDifference(agg) as diff from total_extent
  select ISNULL(@buffered.STDifference(agg), @buffered) as diff from total_extent
)
,
-- Intermediate results are just all the intersections; we'll flatten them as the final step:
intermediate_results as (
  select id, name, geom.STIntersection(@line) as intersections
  from
    (select id, name, geom from valid_polygons
    union all
    select 0 as id, 'External' as name, diff as geom from external_area
    ) as results
)
--
select ir.id, ir.name, simplified.geom, simplified.geom.ToString()
from intermediate_results as ir
cross apply split_geom(ir.intersections) as simplified;


-- * We'll need to remove actual geometries from the output (eg, name, startx,starty,length,endx,endy), since we can't handle SQL Server geometries from python!
