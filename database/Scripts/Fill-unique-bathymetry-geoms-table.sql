CREATE TABLE [dbo].[UniqueBathymetryGeoms] (
  [CATEGORY] VARCHAR(10) NOT NULL,
  [RANK]     INT         NOT NULL,
  [geom]     GEOMETRY    NOT NULL
);

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
