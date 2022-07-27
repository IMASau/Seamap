-- gets the stat counts for each network

CREATE VIEW [dbo].[VW_TIMELINE_COUNT_STATS_NETWORKS] AS
-- merge bathymetry
SELECT
  COALESCE(
    [sediment_imagery_video].[NETWORK],
    [bathymetry].[NETWORK]
  ) AS [NETWORK],
  COALESCE(
    [sediment_imagery_video].[YEAR],
    [bathymetry].[YEAR]
  ) AS [YEAR],
  COALESCE([sediment_imagery_video].[imagery_count], 0) AS [imagery_count],
  COALESCE([sediment_imagery_video].[video_count], 0) AS [video_count],
  COALESCE([sediment_imagery_video].[sediment_count], 0) AS [sediment_count],
  COALESCE([bathymetry].[COUNT], 0) AS [bathymetry_count]
FROM (
  -- merge sediment
  SELECT
    COALESCE(
      [imagery_video].[NETWORK],
      [sediment].[NETWORK]
    ) AS [NETWORK],
    COALESCE(
      [imagery_video].[YEAR],
      [sediment].[YEAR]
    ) AS [YEAR],
    [imagery_video].[imagery_count],
    [imagery_video].[video_count],
    [sediment].[COUNT] AS [sediment_count]
  FROM (
    -- merge imagery and video
    SELECT
      COALESCE(
        [imagery].[NETWORK],
        [video].[NETWORK]
      ) AS [NETWORK],
      COALESCE(
        [imagery].[YEAR],
        [video].[YEAR]
      ) AS [YEAR],
      [imagery].[COUNT] AS [imagery_count],
      [video].[COUNT] AS [video_count]
    FROM (
      -- group imagery data by network
      SELECT
        [NETWORK],
        [YEAR],
        SUM([COUNT]) AS [COUNT]
      FROM [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_SQUIDLE_CAMPAIGNS]
      GROUP BY [NETWORK], [YEAR]
    ) AS [imagery]
    FULL OUTER JOIN (
      -- group video data by network
      SELECT
        [NETWORK],
        [YEAR],
        SUM([COUNT]) AS [COUNT]
      FROM [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_GLOBALARCHIVE_CAMPAIGNS]
      GROUP BY [NETWORK], [YEAR]
    ) AS [video]
      ON
        [video].[NETWORK] = [imagery].[NETWORK] AND
        [video].[YEAR] = [imagery].[YEAR]
  ) AS [imagery_video]
  FULL OUTER JOIN (
      -- group sediment data by network
      SELECT
        [NETWORK],
        [YEAR],
        SUM([COUNT]) AS [COUNT]
      FROM [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_SEDIMENT_SURVEYS]
      GROUP BY [NETWORK], [YEAR]
    ) AS [sediment]
    ON
      [sediment].[NETWORK] = [imagery_video].[NETWORK] AND
      [sediment].[YEAR] = [imagery_video].[YEAR]
) AS [sediment_imagery_video]
FULL OUTER JOIN [dbo].[VW_TIMELINE_COUNT_BATHYMETRY_SURVEYS_NETWORKS] AS [bathymetry]
  ON
    [bathymetry].[NETWORK] = [sediment_imagery_video].[NETWORK] AND
    [bathymetry].[YEAR] = [sediment_imagery_video].[YEAR]
