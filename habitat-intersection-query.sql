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


declare @line geometry = (select geom from Lines where id = 5);

with
-- first narrow to polygons we care about (those that intersect our line of interest):
valid_polygons as (
  select id, name, geom
  from Polygons
  where geom.STIntersects(@line) = 1),
total_extent as (
  select geometry::UnionAggregate(geom) as agg from valid_polygons
),
-- Polygons may not cover the entire length of the line; the difference gives us the bits outside our habitat data:
external_area as (
  --select @line.STEnvelope().STBuffer(1).STDifference(agg) as diff from total_extent
  select @line.STBuffer(0.0000001).STDifference(agg) as diff from total_extent
),
-- numbers table
Nums AS(SELECT top (ISNULL((select top 1 diff.STNumGeometries() from external_area), 0)) ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) AS number FROM master..spt_values t1 cross join master..spt_values t2),
--
-- external_area above might be a multipolygon; split it up by indexing each:
split_external_area as (
  select 0 as id, 'External' + cast(n.number as varchar) as name, diff.STGeometryN(n.number) as geom
  from (select top 1 diff from external_area) as ea
  inner join Nums n on n.number <= diff.STNumGeometries()
)
--
select id, name, geom.STIntersection(@line) as intersections, geom.STIntersection(@line).ToString() as wkt
from
  (select id, name, geom from valid_polygons
  union all
  select id, name, geom from split_external_area
  ) as results;


-- Current Known Bugs:
-- * Re-entrant transects (eg, line #6), in which case the output is a Mult-linestring and also needs to be split.
--   Probably just means we wrap the "splitting" up into a function (accepts a geometry, returns a table)
-- * No intersection at all, in which case it explodes (effectively, doing a "select top(null)" afaict)
