-- Takes the names for any network, park, zone, and zone (IUCN), and gets the
-- aggregate geometry of those intersecting boundaries.

CREATE FUNCTION [dbo].[boundary_geom] (@netname nvarchar(254), @resname nvarchar(254), @zonename nvarchar(254), @zoneiucn nvarchar(5))
RETURNS geometry
AS
BEGIN
  DECLARE @geom geometry;
  SET @geom =(
    SELECT geometry::UnionAggregate([geom])
    FROM [dbo].[VW_BOUNDARY_AMP]
    WHERE
      ([Network] = @netname OR @netname IS NULL) AND
      ([Park] = @resname OR @resname IS NULL) AND
      ([Zone_Category] = @zonename OR @zonename IS NULL) AND
      ([IUCN_Zone] = @zoneiucn OR @zoneiucn IS NULL)
  );
  RETURN(@geom);
END;
