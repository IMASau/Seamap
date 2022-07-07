-- When a habitat observation has been updated, use this stored procedure to update
-- the hardcoded values in the BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE,
-- BOUNDARY_AMP_HABITAT_OBS_SEDIMENT, BOUNDARY_AMP_HABITAT_OBS_SQUIDLE,
-- BOUNDARY_IMCRA_HABITAT_OBS_GLOBALARCHIVE, BOUNDARY_IMCRA_HABITAT_OBS_SEDIMENT,
-- BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE, BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE,
-- BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT, and BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE tables
-- (can also be used to add a new habitat observation to the tables).

CREATE PROCEDURE Update_HABITAT_OBS
  @observation NVARCHAR(MAX)
AS
BEGIN
  -- Update BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_GLOBALARCHIVE] ([Network], [Park], [Zone_Category], [IUCN_Zone], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_GLOBALARCHIVE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;

  -- Update BOUNDARY_AMP_HABITAT_OBS_SEDIMENT
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_SEDIMENT] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_SEDIMENT] ([Network], [Park], [Zone_Category], [IUCN_Zone], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [observation].[SAMPLE_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SEDIMENT_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[SAMPLE_ID] = @observation;

  -- Update BOUNDARY_AMP_HABITAT_OBS_SQUIDLE
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_AMP_HABITAT_OBS_SQUIDLE] ([Network], [Park], [Zone_Category], [IUCN_Zone], [observation])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;

  -- Update BOUNDARY_IMCRA_HABITAT_OBS_GLOBALARCHIVE
  DELETE FROM [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_GLOBALARCHIVE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_GLOBALARCHIVE] ([Provincial_Bioregion], [Mesoscale_Bioregion], [observation])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_GLOBALARCHIVE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;

  -- Update BOUNDARY_IMCRA_HABITAT_OBS_SEDIMENT
  DELETE FROM [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SEDIMENT] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SEDIMENT] ([Provincial_Bioregion], [Mesoscale_Bioregion], [observation])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [observation].[SAMPLE_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SEDIMENT_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[SAMPLE_ID] = @observation;

  -- Update BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE
  DELETE FROM [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE] ([Provincial_Bioregion], [Mesoscale_Bioregion], [observation])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;

  -- Update BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_GLOBALARCHIVE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;

  -- Update BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[SAMPLE_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SEDIMENT_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[SAMPLE_ID] = @observation;

  -- Update BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE] WHERE [observation] = @observation;
  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation]
  WHERE [observation].[DEPLOYMENT_ID] = @observation;
END;
