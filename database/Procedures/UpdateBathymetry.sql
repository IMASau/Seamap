-- When a bathymetry category has been updated, use this stored procedure to update
-- the hardcoded values in the BathymetryGeoms, UniqueBathymetryGeoms, and
-- BoundaryBathymetries tables.

CREATE PROCEDURE UpdateBathymetry
  @category VARCHAR(10)
AS
BEGIN
  -- Update BathymetryGeoms
  DELETE FROM [dbo].[BathymetryGeoms] WHERE [CATEGORY] = @category;
  INSERT INTO [dbo].[BathymetryGeoms] ([CATEGORY], [RANK], [geom])
  SELECT
    [CATEGORY],
    [RANK],
    geometry::UnionAggregate([geom]) AS [geom]
  FROM [dbo].[VW_BATHYMETRY]
  WHERE [CATEGORY] = @category
  GROUP BY [CATEGORY], [RANK];

  -- Update UniqueBathymetryGeoms
  DELETE FROM [dbo].[UniqueBathymetryGeoms];
  INSERT INTO [dbo].[UniqueBathymetryGeoms] ([CATEGORY], [RANK], [geom])
  SELECT
    [CATEGORY],
    [RANK],
    COALESCE([geom].STDifference((
      SELECT geometry::UnionAggregate([b2].[geom])
      FROM [dbo].[BathymetryGeoms] AS [b2]
      WHERE [b2].[RANK] < [b1].[RANK]
    )), [geom]) AS [geom]
  FROM [dbo].[BathymetryGeoms] AS [b1];

  -- Update BoundaryBathymetries
  DELETE FROM [dbo].[BoundaryBathymetries] WHERE [bathymetry_category] = @category;
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
  WHERE [bathymetry].[CATEGORY] = @category;
END;
