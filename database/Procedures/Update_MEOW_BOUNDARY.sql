-- When a MEOW boundary has been updated, use this stored procedure to update the
-- hardcoded values in the BOUNDARY_MEOW_HABITAT, BOUNDARY_MEOW_BATHYMETRY,
-- BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE, BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT,
-- BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE tables (can also be used to add a new boundary
-- to the tables).

CREATE PROCEDURE Update_MEOW_BOUNDARY
  @realm     NVARCHAR(255),
  @province  NVARCHAR(255),
  @ecoregion NVARCHAR(255)
AS
BEGIN
  -- Update BOUNDARY_MEOW_HABITAT
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT]
  WHERE
    ([Realm] = @realm OR @realm IS NULL) AND
    ([Province] = @province OR @province IS NULL) AND
    ([Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT] ([Realm], [Province], [Ecoregion], [habitat], [geom])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE
    ([boundary].[Realm] = @realm OR @realm IS NULL) AND
    ([boundary].[Province] = @province OR @province IS NULL) AND
    ([boundary].[Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  -- Update BOUNDARY_MEOW_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_MEOW_BATHYMETRY]
  WHERE
    ([Realm] = @realm OR @realm IS NULL) AND
    ([Province] = @province OR @province IS NULL) AND
    ([Ecoregion] = @ecoregion OR @ecoregion IS NULL);
  
  INSERT INTO [dbo].[BOUNDARY_MEOW_BATHYMETRY] ([Realm], [Province], [Ecoregion], [bathymetry_resolution], [bathymetry_rank], [geom])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE
    ([boundary].[Realm] = @realm OR @realm IS NULL) AND
    ([boundary].[Province] = @province OR @province IS NULL) AND
    ([boundary].[Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  -- Update BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE]
  WHERE
    ([Realm] = @realm OR @realm IS NULL) AND
    ([Province] = @province OR @province IS NULL) AND
    ([Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_GLOBALARCHIVE] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_GLOBALARCHIVE_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([boundary].[Realm] = @realm OR @realm IS NULL) AND
    ([boundary].[Province] = @province OR @province IS NULL) AND
    ([boundary].[Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  -- Update BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT]
  WHERE
    ([Realm] = @realm OR @realm IS NULL) AND
    ([Province] = @province OR @province IS NULL) AND
    ([Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SEDIMENT] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[SAMPLE_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SEDIMENT_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([boundary].[Realm] = @realm OR @realm IS NULL) AND
    ([boundary].[Province] = @province OR @province IS NULL) AND
    ([boundary].[Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  -- Update BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE]
  WHERE
    ([Realm] = @realm OR @realm IS NULL) AND
    ([Province] = @province OR @province IS NULL) AND
    ([Ecoregion] = @ecoregion OR @ecoregion IS NULL);

  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT_OBS_SQUIDLE] ([Realm], [Province], [Ecoregion], [observation])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [observation].[DEPLOYMENT_ID] AS [observation]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation]
  WHERE
    ([boundary].[Realm] = @realm OR @realm IS NULL) AND
    ([boundary].[Province] = @province OR @province IS NULL) AND
    ([boundary].[Ecoregion] = @ecoregion OR @ecoregion IS NULL);
END;
