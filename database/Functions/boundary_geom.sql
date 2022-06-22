CREATE FUNCTION boundary_geom (@network nvarchar(254), @park nvarchar(254), @zone nvarchar(254), @zone_iucn nvarchar(5))
RETURNS geometry
AS
BEGIN
  DECLARE @geom geometry;
  SET @geom =(
    SELECT geometry::UnionAggregate([geom])
    FROM [IMASSeamap].[dbo].[SeamapAus_BoundaryAreas_View]
    WHERE
      ([network] = @network OR @network IS NULL OR @network = '') AND
      ([park] = @park OR @park IS NULL OR @park = '') AND
      ([zone] = @zone OR @zone IS NULL OR @zone = '') AND
      ([zone_iucn] = @zone_iucn OR @zone_iucn IS NULL OR @zone_iucn = '')
  );
  RETURN(@geom);
END;
GO
