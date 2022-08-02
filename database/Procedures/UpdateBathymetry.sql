-- When a bathymetry resolution has been updated, use this stored procedure to
-- update the hardcoded values in the BathymetryGeoms, UniqueBathymetryGeoms,
-- BOUNDARY_AMP_BATHYMETRY, BOUNDARY_IMCRA_BATHYMETRY, and BOUNDARY_MEOW_BATHYMETRY
-- tables.

CREATE PROCEDURE UpdateBathymetry
  @resolution VARCHAR(10)
AS
BEGIN
  -- Update BathymetryGeoms
  DROP TABLE IF EXISTS ##T1;
  CREATE TABLE ##T1 (
    [geom]       GEOMETRY    NOT NULL,
    [Group]      INT         NOT NULL
  );

  DROP TABLE IF EXISTS ##T2;
  CREATE TABLE ##T2 (
    [geom]       GEOMETRY    NOT NULL,
    [Group]      INT         NOT NULL
  );

  DROP TABLE IF EXISTS ##T3;
  CREATE TABLE ##T3 (
    [geom]       GEOMETRY    NOT NULL,
    [Group]      INT         NOT NULL
  );

  DROP TABLE IF EXISTS ##Groups;
  CREATE TABLE ##Groups (
    [geom]       GEOMETRY    NOT NULL
  );
  
  INSERT INTO ##T1
  SELECT
    [geom],
    (
      ROW_NUMBER() OVER(
        ORDER BY [RESOLUTION], [RANK], [ID]
      ) - 1
    ) / 15
  FROM [dbo].[VW_BATHYMETRY]
  WHERE [RESOLUTION] = @resolution;
  
  INSERT INTO ##T2
  SELECT
    GEOMETRY::UnionAggregate([geom]),
    (
      ROW_NUMBER() OVER(
        ORDER BY [Group]
      ) - 1
    ) / 5
  FROM ##T1
  GROUP BY [Group];

  INSERT INTO ##T3
  SELECT
    GEOMETRY::UnionAggregate([geom]),
    (
      ROW_NUMBER() OVER(
        ORDER BY [Group]
      ) - 1
    ) / 5
  FROM ##T2
  GROUP BY [Group];

  INSERT INTO ##Groups
  SELECT GEOMETRY::UnionAggregate([geom])
  FROM ##T3
  GROUP BY [Group];

  DECLARE @geom GEOMETRY = (
    SELECT GEOMETRY::UnionAggregate([geom])
    FROM ##Groups
  );

  DECLARE @rank INT = (
    SELECT [RANK]
    FROM (
      SELECT TOP(1) *
      FROM [dbo].[VW_BATHYMETRY]
      WHERE [RESOLUTION] = @resolution
    ) AS [T1]
  );

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
