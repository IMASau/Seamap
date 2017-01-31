
IF OBJECT_ID('HabitatTableType') IS NULL

  CREATE TYPE HabitatTableType AS TABLE (
    name VARCHAR(max),
    geom GEOMETRY
  );

GO
