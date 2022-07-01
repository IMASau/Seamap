-- When a habitat has been updated, use this stored procedure to update the
-- hardcoded values in the BoundaryHabitats table (can also be used to add a new
-- habitat to the table).

CREATE PROCEDURE UpdateHabitat
  @habitat NVARCHAR(30)
AS
BEGIN
  DELETE FROM [dbo].[BoundaryHabitats] WHERE [habitat] = @habitat;
  INSERT INTO [dbo].[BoundaryHabitats] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [habitat], [geom])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE [habitat].[CATEGORY] = @habitat;
END;
