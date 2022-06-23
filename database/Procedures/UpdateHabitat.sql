CREATE PROCEDURE UpdateHabitat
  @habitat NVARCHAR(30)
AS
BEGIN
  DELETE FROM [dbo].[BoundaryHabitats] WHERE [habitat] = @habitat;
  INSERT INTO [dbo].[BoundaryHabitats] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [habitat], [geom])
  SELECT
    [boundary].[NETNAME],
    [boundary].[RESNAME],
    [boundary].[ZONENAME],
    [boundary].[ZONEIUCN],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom]
  FROM [dbo].[BoundaryGeoms_View] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE [habitat].[habitat] = @habitat;
END;
