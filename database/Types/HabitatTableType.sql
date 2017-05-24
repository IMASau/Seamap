
CREATE TYPE HabitatTableType AS TABLE (
  id int identity,
  name VARCHAR(max),
  geom GEOMETRY
);
