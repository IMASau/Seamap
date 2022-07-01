-- When a boundary has been updated, use this stored procedure to update the
-- hardcoded values in the BoundaryHabitats and BoundaryBathymetries tables (can
-- also be used to add a new boundary to the tables).

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
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE
    ([boundary].[Network] = @netname OR @netname IS NULL) AND
    ([boundary].[Park] = @resname OR @resname IS NULL) AND
    ([boundary].[Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([boundary].[IUCN_Zone] = @zoneiucn OR @zoneiucn IS NULL);

  -- Update BoundaryBathymetries
  DELETE FROM [dbo].[BoundaryBathymetries]
  WHERE
    ([NETNAME] = @netname OR @netname IS NULL) AND
    ([RESNAME] = @resname OR @resname IS NULL) AND
    ([ZONENAME] = @zonename OR @zonename IS NULL) AND
    ([ZONEIUCN] = @zoneiucn OR @zoneiucn IS NULL);
  
  INSERT INTO [dbo].[BoundaryBathymetries] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [bathymetry_category], [bathymetry_rank], [geom])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [bathymetry].[CATEGORY] AS [bathymetry_category],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE
    ([boundary].[Network] = @netname OR @netname IS NULL) AND
    ([boundary].[Park] = @resname OR @resname IS NULL) AND
    ([boundary].[Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([boundary].[IUCN_Zone] = @zoneiucn OR @zoneiucn IS NULL);
END;
