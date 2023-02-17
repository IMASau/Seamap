-- Squashes down the many bathymetry geometries (2645 at the time of writing!) from
-- VW_BATHYMETRY into aggregated geometries for each of the 6 categories and ranks.
-- Its purpose is to cut down on the number of geometry comparisons necessary for
-- other queries.

CREATE TABLE [dbo].[BathymetryGeoms] (
  [RESOLUTION] VARCHAR(10) NOT NULL,
  [RANK]     INT         NOT NULL,
  [geom]     GEOMETRY    NOT NULL
);

INSERT INTO [dbo].[BathymetryGeoms] ([RESOLUTION], [RANK], [geom])
SELECT
  [RESOLUTION],
  [RANK],
  geometry::UnionAggregate([geom]) AS [geom]
FROM [dbo].[VW_BATHYMETRY]
GROUP BY [RESOLUTION], [RANK];
