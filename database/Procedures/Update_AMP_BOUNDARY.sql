-- When an AMP boundary has been updated, use this stored procedure to update the
-- hardcoded values in the BOUNDARY_AMP_HABITAT and BOUNDARY_AMP_BATHYMETRY tables
-- (can also be used to add a new boundary to the tables).

CREATE PROCEDURE Update_AMP_BOUNDARY
  @netname  NVARCHAR(254),
  @resname  NVARCHAR(254),
  @zonename NVARCHAR(254),
  @zoneiucn NVARCHAR(5)
AS
BEGIN
  -- Update BOUNDARY_AMP_HABITAT
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Zone] = @zoneiucn OR @zoneiucn IS NULL);

  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT] ([Network], [Park], [Zone_Category], [IUCN_Zone], [habitat], [geom])
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

  -- Update BOUNDARY_AMP_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_AMP_BATHYMETRY]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Zone] = @zoneiucn OR @zoneiucn IS NULL);
  
  INSERT INTO [dbo].[BOUNDARY_AMP_BATHYMETRY] ([Network], [Park], [Zone_Category], [IUCN_Zone], [bathymetry_category], [bathymetry_rank], [geom])
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
