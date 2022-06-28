-- Takes the bathymetry geometries from BathymetryGeoms table and uses the
-- difference from all bathymetry geometries of higher rank (lower RANK values).

CREATE TABLE [dbo].[UniqueBathymetryGeoms] (
  [CATEGORY] VARCHAR(10) NOT NULL,
  [RANK]     INT         NOT NULL,
  [geom]     GEOMETRY    NOT NULL
);

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
