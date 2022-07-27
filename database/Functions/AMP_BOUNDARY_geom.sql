-- Gets the aggregate geometry of the AMP boundaries defined by the intersection of
-- a specified network, park, and zone.

CREATE FUNCTION [dbo].[AMP_BOUNDARY_geom] (@network nvarchar(254), @park nvarchar(254), @zone_category nvarchar(254), @IUCN_Category nvarchar(5))
RETURNS geometry
AS
BEGIN
  DECLARE @geom geometry;
  SET @geom =(
    SELECT geometry::UnionAggregate([geom])
    FROM [dbo].[VW_BOUNDARY_AMP]
    WHERE
      ([Network] = @network OR @network IS NULL) AND
      ([Park] = @park OR @park IS NULL) AND
      ([Zone_Category] = @zone_category OR @zone_category IS NULL) AND
      ([IUCN_Category] = @IUCN_Category OR @IUCN_Category IS NULL)
  );
  RETURN(@geom);
END;
