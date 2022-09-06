-- When a bathymetry resolution has been updated, use this stored procedure to
-- update the hardcoded values in the BathymetryGeoms, UniqueBathymetryGeoms,
-- BOUNDARY_AMP_BATHYMETRY, BOUNDARY_IMCRA_BATHYMETRY, and BOUNDARY_MEOW_BATHYMETRY
-- tables.

-- The @batch optional parameter dictates where in the procedure we should
-- begin/continue (for the event in which the procedure fails partway through
-- execution). A @batch value of 0 indicates we start at the beginning, values 1-5
-- are for the different stages of geometry aggregation, a value of 6 is for
-- updating the BathymetryGeoms tables, and a value of 7 updates the hardcoded
-- bathymetry boundaries tables. Smaller @batch values execute larger @batch value
-- operations automatically (i.e. starting with @batch of 3 will execute 3 as well
-- as all the @batch=4 stuff, @batch=4 executes 4 and @batch=5 stuff, and so on).

CREATE PROCEDURE UpdateBathymetry
  @resolution VARCHAR(10),
  @batch      INT         = NULL
AS
BEGIN
  SET @batch = COALESCE(@batch, 0);

  -- Begin bathymetry geometry aggregation
  IF @batch = 0 BEGIN
    DELETE FROM [dbo].[GeometryAggregationTable];
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      [geom],
      (
        ROW_NUMBER() OVER(
          ORDER BY [RESOLUTION], [RANK], [ID]
        ) - 1
      ) / 10,
      0
    FROM [dbo].[VW_BATHYMETRY]
    WHERE [RESOLUTION] = @resolution;
  END;

  IF @batch <= 1
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      GEOMETRY::UnionAggregate([geom]),
      (
        ROW_NUMBER() OVER(
          ORDER BY [BatchGroup]
        ) - 1
      ) / 5,
      1
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 0
    GROUP BY [BatchGroup];

  IF @batch <= 2
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      GEOMETRY::UnionAggregate([geom]),
      (
        ROW_NUMBER() OVER(
          ORDER BY [BatchGroup]
        ) - 1
      ) / 3,
      2
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 1
    GROUP BY [BatchGroup];

  IF @batch <= 3
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      GEOMETRY::UnionAggregate([geom]),
      (
        ROW_NUMBER() OVER(
          ORDER BY [BatchGroup]
        ) - 1
      ) / 3,
      3
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 2
    GROUP BY [BatchGroup];

  IF @batch <= 4
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      GEOMETRY::UnionAggregate([geom]),
      0,
      4
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 3
    GROUP BY [BatchGroup];

  IF @batch <= 5
    INSERT INTO [dbo].[GeometryAggregationTable]
    SELECT
      GEOMETRY::UnionAggregate([geom]),
      0,
      5
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 4
    GROUP BY [BatchGroup];
  -- End bathymetry geometry aggregation

  DECLARE @geom GEOMETRY = (
    SELECT GEOMETRY::UnionAggregate([geom])
    FROM [dbo].[GeometryAggregationTable]
    WHERE [Batch] = 5
  );

    DECLARE @rank INT = (
    SELECT [RANK]
    FROM (
      SELECT TOP(1) *
      FROM [dbo].[VW_BATHYMETRY]
      WHERE [RESOLUTION] = @resolution
    ) AS [T1]
  );

  IF @batch <= 6 BEGIN
    -- Update BathymetryGeoms
    BEGIN TRANSACTION;
      DELETE FROM [dbo].[BathymetryGeoms] WHERE [RESOLUTION] = @resolution;
      INSERT INTO [dbo].[BathymetryGeoms] ([RESOLUTION], [RANK], [geom])
      VALUES (@resolution, @rank, @geom);
    COMMIT;

    -- Update UniqueBathymetryGeoms
    BEGIN TRANSACTION;
      DELETE FROM [dbo].[UniqueBathymetryGeoms] WHERE [RANK] >= @rank;
      INSERT INTO [dbo].[UniqueBathymetryGeoms] ([RESOLUTION], [RANK], [geom])
      SELECT
        [RESOLUTION],
        [RANK],
        COALESCE([geom].STDifference((
          SELECT geometry::UnionAggregate([b2].[geom])
          FROM [dbo].[BathymetryGeoms] AS [b2]
          WHERE [b2].[RANK] < [b1].[RANK]
        )), [geom]) AS [geom]
      FROM [dbo].[BathymetryGeoms] AS [b1]
      WHERE [RANK] >= @rank;
    COMMIT;
  END;

  IF @batch <= 7 BEGIN
    -- Update BOUNDARY_AMP_BATHYMETRY
    BEGIN TRANSACTION;
      DELETE FROM [dbo].[BOUNDARY_AMP_BATHYMETRY] WHERE [bathymetry_rank] >= @rank;
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
      WHERE [bathymetry].[RANK] >= @rank;
    COMMIT;

    -- Update BOUNDARY_IMCRA_BATHYMETRY
    BEGIN TRANSACTION;
      DELETE FROM [dbo].[BOUNDARY_IMCRA_BATHYMETRY] WHERE [bathymetry_rank] >= @rank;
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
      WHERE [bathymetry].[RANK] >= @rank;
    COMMIT;

    -- Update BOUNDARY_MEOW_BATHYMETRY
    BEGIN TRANSACTION;
      DELETE FROM [dbo].[BOUNDARY_MEOW_BATHYMETRY] WHERE [bathymetry_rank] >= @rank;
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
      WHERE [bathymetry].[RANK] >= @rank;
    COMMIT;
  END;
END;
