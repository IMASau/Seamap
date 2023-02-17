-- Gets the aggregate geometry of the IMCRA boundaries defined by the intersection
-- of a specified provincial bioregion mesoscale bioregion.

CREATE FUNCTION [dbo].[IMCRA_BOUNDARY_geom] (@provincial_bioregion NVARCHAR(255), @mesoscale_bioregion NVARCHAR(255))
RETURNS geometry
AS
BEGIN
  DECLARE @geom GEOMETRY;
  SET @geom =(
    SELECT GEOMETRY::UnionAggregate([geom])
    FROM [dbo].[VW_BOUNDARY_IMCRA]
    WHERE
      ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
      ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
  );
  RETURN(@geom);
END;
