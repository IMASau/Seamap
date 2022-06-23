CREATE TABLE [dbo].[BoundaryHabitats] (
  [NETNAME]  NVARCHAR(254) NOT NULL,
  [RESNAME]  NVARCHAR(254) NOT NULL,
  [ZONENAME] NVARCHAR(254) NOT NULL,
  [ZONEIUCN] NVARCHAR(5)   NOT NULL,
  [habitat]  NVARCHAR(30)  NOT NULL,
  [geom]     GEOMETRY      NOT NULL
);

INSERT INTO [dbo].[BoundaryHabitats] ([NETNAME], [RESNAME], [ZONENAME], [ZONEIUCN], [habitat], [geom])
SELECT
  [boundary].[NETNAME],
  [boundary].[RESNAME],
  [boundary].[ZONENAME],
  [boundary].[ZONEIUCN],
  [habitat].[CATEGORY] AS [habitat],
  [habitat].[geom]
FROM [dbo].[BoundaryGeoms_View] AS [boundary]
CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat];
