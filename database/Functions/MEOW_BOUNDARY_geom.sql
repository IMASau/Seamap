-- Gets the aggregate geometry of the MEOW boundaries defined by the intersection
-- of a specified realm, province, and/or ecoregion.

CREATE FUNCTION [dbo].[MEOW_BOUNDARY_geom] (@realm NVARCHAR(255), @province NVARCHAR(255), @ecoregion NVARCHAR(255))
RETURNS GEOMETRY
AS
BEGIN
  DECLARE @geom GEOMETRY;
  SET @geom =(
    SELECT GEOMETRY::UnionAggregate([geom])
    FROM [dbo].[VW_BOUNDARY_MEOW]
    WHERE
      ([Realm] = @realm OR @realm IS NULL) AND
      ([Province] = @province OR @province IS NULL) AND
      ([Ecoregion] = @ecoregion OR @ecoregion IS NULL)
  );
  RETURN(@geom);
END;
