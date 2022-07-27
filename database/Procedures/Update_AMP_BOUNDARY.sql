-- When an AMP boundary has been updated, use this stored procedure to update the
-- hardcoded values in the BOUNDARY_AMP_HABITAT, BOUNDARY_AMP_BATHYMETRY,
-- BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE, BOUNDARY_AMP_HABITAT_OBS_SEDIMENT, and
-- BOUNDARY_AMP_HABITAT_OBS_SQUIDLE tables (can also be used to add a new boundary
-- to the tables).

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
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);

  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT] ([Network], [Park], [Zone_Category], [IUCN_Category], [habitat], [geom], [area])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Category],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom],
    [habitat].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE
    ([boundary].[Network] = @netname OR @netname IS NULL) AND
    ([boundary].[Park] = @resname OR @resname IS NULL) AND
    ([boundary].[Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([boundary].[IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);

  -- Update BOUNDARY_AMP_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_AMP_BATHYMETRY]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  
  INSERT INTO [dbo].[BOUNDARY_AMP_BATHYMETRY] ([Network], [Park], [Zone_Category], [IUCN_Category], [bathymetry_resolution], [bathymetry_rank], [geom], [area])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Category],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom],
    [bathymetry].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE
    ([boundary].[Network] = @netname OR @netname IS NULL) AND
    ([boundary].[Park] = @resname OR @resname IS NULL) AND
    ([boundary].[Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([boundary].[IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  
  -- Update BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE] ([Network], [Park], [Zone_Category], [IUCN_Category], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Category],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_GLOBALARCHIVE_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  
  -- Update BOUNDARY_AMP_HABITAT_OBS_SEDIMENT
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_SEDIMENT]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_SEDIMENT] ([Network], [Park], [Zone_Category], [IUCN_Category], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Category],
    [observation].[SAMPLE_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SEDIMENT_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);

  -- Update BOUNDARY_AMP_HABITAT_OBS_SQUIDLE
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE] ([Network], [Park], [Zone_Category], [IUCN_Category], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Category],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([Network] = @netname OR @netname IS NULL) AND
    ([Park] = @resname OR @resname IS NULL) AND
    ([Zone_Category] = @zonename OR @zonename IS NULL) AND
    ([IUCN_Category] = @zoneiucn OR @zoneiucn IS NULL);
END;
