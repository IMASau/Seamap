-- When a habitat has been updated, use this stored procedure to update the
-- hardcoded values in the BOUNDARY_AMP_HABITAT, BOUNDARY_IMCRA_HABITAT, and
-- BOUNDARY_MEOW_HABITAT tables (can also be used to add a new habitat to the
-- tables).

CREATE PROCEDURE UpdateHabitat
  @habitat NVARCHAR(30)
AS
BEGIN
  -- Update BOUNDARY_AMP_HABITAT
  DELETE FROM [dbo].[BOUNDARY_AMP_HABITAT] WHERE [habitat] = @habitat;
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
  WHERE [habitat].[CATEGORY] = @habitat;

  -- Update BOUNDARY_IMCRA_HABITAT
  DELETE FROM [dbo].[BOUNDARY_IMCRA_HABITAT] WHERE [habitat] = @habitat;
  INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT] ([Provincial_Bioregion], [Mesoscale_Bioregion], [habitat], [geom], [area])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom],
    [habitat].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE [habitat].[CATEGORY] = @habitat;

  -- Update BOUNDARY_MEOW_HABITAT
  DELETE FROM [dbo].[BOUNDARY_MEOW_HABITAT] WHERE [habitat] = @habitat;
  INSERT INTO [dbo].[BOUNDARY_MEOW_HABITAT] ([Realm], [Province], [Ecoregion], [habitat], [geom], [area])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom],
    [habitat].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE [habitat].[CATEGORY] = @habitat;
END;
