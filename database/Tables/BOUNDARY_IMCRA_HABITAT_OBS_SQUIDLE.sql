-- Pre-calculated SQUIDLE habitat observations per IMCRA boundary.

CREATE TABLE [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE] (
  [Provincial_Bioregion] NVARCHAR(255) NOT NULL,
  [Mesoscale_Bioregion]  NVARCHAR(255) NULL,
  [observation]          NVARCHAR(480) NOT NULL
);

INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE] ([Provincial_Bioregion], [Mesoscale_Bioregion], [observation])
  [boundary].[Provincial_Bioregion],
  [boundary].[Mesoscale_Bioregion],
  [observation].[DEPLOYMENT_ID] AS [observation]
FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
CROSS APPLY [dbo].HABITAT_OBS_SQUIDLE_intersections([boundary].[geom]) AS [observation];

-- Use:
-- DECLARE @provincial_bioregion NVARCHAR(255) = 'Cocos (Keeling) Island Province';
-- DECLARE @mesoscale_bioregion  NVARCHAR(255) = NULL;

-- SELECT [observation]
-- FROM [dbo].[BOUNDARY_IMCRA_HABITAT_OBS_SQUIDLE]
-- WHERE
--   ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
--   ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL)
-- GROUP BY [observation];
