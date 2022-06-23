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
    [geom].STDifference(
      SELECT geometry::UnionAggregate([geom])
      FROM [dbo].[BathymetryGeoms] AS [b2]
      WHERE [b2].[RANK] < [b1].[RANK]
    ) AS [geom]
  FROM [dbo].[BathymetryGeoms] AS [b1];

  -- Update BoundaryBathymetries
  DELETE FROM [dbo].[BoundaryBathymetries] WHERE [bathymetry_category] = @category;
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
  WHERE [bathymetry].[CATEGORY] = @category;
END;
