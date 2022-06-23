CREATE PROCEDURE UpdateBoundary
  @netname  NVARCHAR(254),
  @resname  NVARCHAR(254),
  @zonename NVARCHAR(254),
  @zoneiucn NVARCHAR(5)
AS
BEGIN
  -- Update BoundaryHabitats
  DELETE FROM [dbo].[BoundaryHabitats]
  WHERE
    ([NETNAME] = @netname OR @netname IS NULL) AND
    ([RESNAME] = @resname OR @resname IS NULL) AND
    ([ZONENAME] = @zonename OR @zonename IS NULL) AND
    ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL);

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
  WHERE
    ([NETNAME] = @netname OR @netname IS NULL) AND
    ([RESNAME] = @resname OR @resname IS NULL) AND
    ([ZONENAME] = @zonename OR @zonename IS NULL) AND
    ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL);

  -- Update BoundaryBathymetries
  DELETE FROM [dbo].[BoundaryBathymetries]
  WHERE
    ([NETNAME] = @netname OR @netname IS NULL) AND
    ([RESNAME] = @resname OR @resname IS NULL) AND
    ([ZONENAME] = @zonename OR @zonename IS NULL) AND
    ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL);
  
  INSERT INTO [dbo].[BoundaryBathymetries] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [bathymetry_category], [bathymetry_rank], [geom])
  SELECT
    [boundary].[NETNAME],
    [boundary].[RESNAME],
    [boundary].[ZONENAME],
    [boundary].[ZONEIUCN],
    [bathymetry].[CATEGORY] AS [bathymetry_category],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom]
  FROM [dbo].[BoundaryGeoms_View] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE
    ([NETNAME] = @netname OR @netname IS NULL) AND
    ([RESNAME] = @resname OR @resname IS NULL) AND
    ([ZONENAME] = @zonename OR @zonename IS NULL) AND
    ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL);
END;
