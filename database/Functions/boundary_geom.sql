CREATE FUNCTION boundary_geom (@netname nvarchar(254), @resname nvarchar(254), @zonename nvarchar(254), @zoneiucn nvarchar(5))
RETURNS geometry
AS
BEGIN
  DECLARE @geom geometry;
  SET @geom =(
    SELECT geometry::UnionAggregate([geom])
    FROM [dbo].[BoundaryGeoms_View]
    WHERE
      ([NETNAME] = @netname OR @netname IS NULL OR @netname = '') AND
      ([RESNAME] = @resname OR @resname IS NULL OR @resname = '') AND
      ([ZONENAME] = @zonename OR @zonename IS NULL OR @zonename = '') AND
      ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL OR @zoneiucn = '')
  );
  RETURN(@geom);
END;
