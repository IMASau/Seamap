-- When a bathymetry resolution has been updated, use this stored procedure to
-- update the hardcoded values in the BathymetryGeoms, UniqueBathymetryGeoms,
-- BOUNDARY_AMP_BATHYMETRY, BOUNDARY_IMCRA_BATHYMETRY, and BOUNDARY_MEOW_BATHYMETRY
-- tables.

CREATE PROCEDURE UpdateBathymetry
  @resolution VARCHAR(10)
AS
BEGIN
  -- Update BathymetryGeoms
  DELETE FROM [dbo].[BathymetryGeoms] WHERE [RESOLUTION] = @resolution;
  INSERT INTO [dbo].[BathymetryGeoms] ([RESOLUTION], [RANK], [geom])
  SELECT
    [RESOLUTION],
    [RANK],
    geometry::UnionAggregate([geom]) AS [geom]
  FROM [dbo].[VW_BATHYMETRY]
  WHERE [RESOLUTION] = @resolution
  GROUP BY [RESOLUTION], [RANK];

  -- Update UniqueBathymetryGeoms
  DELETE FROM [dbo].[UniqueBathymetryGeoms];
  INSERT INTO [dbo].[UniqueBathymetryGeoms] ([RESOLUTION], [RANK], [geom])
  SELECT
    [RESOLUTION],
    [RANK],
    COALESCE([geom].STDifference((
      SELECT geometry::UnionAggregate([b2].[geom])
      FROM [dbo].[BathymetryGeoms] AS [b2]
      WHERE [b2].[RANK] < [b1].[RANK]
    )), [geom]) AS [geom]
  FROM [dbo].[BathymetryGeoms] AS [b1];

  -- Update BOUNDARY_AMP_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_AMP_BATHYMETRY] WHERE [bathymetry_resolution] = @resolution;
  INSERT INTO [dbo].[BOUNDARY_AMP_BATHYMETRY] ([Network], [Park], [Zone_Category], [IUCN_Zone], [bathymetry_resolution], [bathymetry_rank], [geom], [area])
  SELECT
    [boundary].[Network],
    [boundary].[Park],
    [boundary].[Zone_Category],
    [boundary].[IUCN_Zone],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom],
    [bathymetry].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_AMP] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE [bathymetry].[RESOLUTION] = @resolution;

  -- Update BOUNDARY_IMCRA_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_IMCRA_BATHYMETRY] WHERE [bathymetry_resolution] = @resolution;
  INSERT INTO [dbo].[BOUNDARY_IMCRA_BATHYMETRY] ([Provincial_Bioregion], [Mesoscale_Bioregion], [bathymetry_resolution], [bathymetry_rank], [geom], [area])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom],
    [bathymetry].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE [bathymetry].[RESOLUTION] = @resolution;

  -- Update BOUNDARY_MEOW_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_MEOW_BATHYMETRY] WHERE [bathymetry_resolution] = @resolution;
  INSERT INTO [dbo].[BOUNDARY_MEOW_BATHYMETRY] ([Realm], [Province], [Ecoregion], [bathymetry_resolution], [bathymetry_rank], [geom], [area])
  SELECT
    [boundary].[Realm],
    [boundary].[Province],
    [boundary].[Ecoregion],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom],
    [bathymetry].[geom].STArea() AS [area]
  FROM [dbo].[VW_BOUNDARY_MEOW] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE [bathymetry].[RESOLUTION] = @resolution;
END;
