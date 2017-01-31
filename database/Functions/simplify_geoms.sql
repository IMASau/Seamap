-- Given a geometry, return a table of component geometries all of whom have STNumGeometries() == 1:

CREATE FUNCTION simplify_geoms (@geom geometry)
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
