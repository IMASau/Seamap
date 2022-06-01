CREATE TYPE HabitatTableType AS TABLE (
  id int identity,
  layer_name VARCHAR(max),
  name VARCHAR(max),
  geom GEOMETRY
);
