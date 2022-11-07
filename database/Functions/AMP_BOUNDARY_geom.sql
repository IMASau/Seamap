-- Gets the aggregate geometry of the AMP boundaries defined by the intersection of
-- a specified network, park, and zone.

CREATE FUNCTION [dbo].[AMP_BOUNDARY_geom] (
  @network       NVARCHAR(254),
  @park          NVARCHAR(254),
  @zone_category NVARCHAR(254),
  @IUCN_Category NVARCHAR(5),
  @zone_id       NVARCHAR(10)
)
RETURNS GEOMETRY
AS
BEGIN
  DECLARE @geom GEOMETRY;
  SET @geom =(
    SELECT GEOMETRY::UnionAggregate([geom])
    FROM [dbo].[VW_BOUNDARY_AMP]
    WHERE
      ([Network] = @network OR @network IS NULL) AND
      ([Park] = @park OR @park IS NULL) AND
      ([Zone_Category] = @zone_category OR @zone_category IS NULL) AND
      ([IUCN_Category] = @IUCN_Category OR @IUCN_Category IS NULL) AND
      ([Zone_ID] = @zone_id OR @zone_id IS NULL)
  );
  RETURN(@geom);
END;
