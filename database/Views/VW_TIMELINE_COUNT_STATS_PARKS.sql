-- gets the stat counts for each park (do not aggregate for network counts because
-- then some bathymetries would be counted twice)

CREATE VIEW [dbo].[VW_TIMELINE_COUNT_STATS_PARKS] AS
-- merge bathymetry
SELECT
  COALESCE(
    [sediment_imagery_video].[NETWORK],
    [bathymetry].[NETWORK]
  ) AS [NETWORK],
  COALESCE(
    [sediment_imagery_video].[PARK],
    [bathymetry].[PARK]
  ) AS [PARK],
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
      [imagery_video].[PARK],
      [sediment].[PARK]
    ) AS [PARK],
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
        [imagery].[PARK],
        [video].[PARK]
      ) AS [PARK],
      COALESCE(
        [imagery].[YEAR],
        [video].[YEAR]
      ) AS [YEAR],
      [imagery].[COUNT] AS [imagery_count],
      [video].[COUNT] AS [video_count]
    FROM [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_SQUIDLE_CAMPAIGNS] AS [imagery]
    FULL OUTER JOIN [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_GLOBALARCHIVE_CAMPAIGNS] AS [video]
      ON
        [video].[NETWORK] = [imagery].[NETWORK] AND
        [video].[PARK] = [imagery].[PARK] AND
        [video].[YEAR] = [imagery].[YEAR]
  ) AS [imagery_video]
  FULL OUTER JOIN [dbo].[VW_TIMELINE_COUNT_HABITAT_OBS_SEDIMENT_SURVEYS] AS [sediment]
    ON
      [sediment].[NETWORK] = [imagery_video].[NETWORK] AND
      [sediment].[PARK] = [imagery_video].[PARK] AND
      [sediment].[YEAR] = [imagery_video].[YEAR]
) AS [sediment_imagery_video]
FULL OUTER JOIN [dbo].[VW_TIMELINE_COUNT_BATHYMETRY_SURVEYS_PARKS] AS [bathymetry]
  ON
    [bathymetry].[NETWORK] = [sediment_imagery_video].[NETWORK] AND
    [bathymetry].[PARK] = [sediment_imagery_video].[PARK] AND
    [bathymetry].[YEAR] = [sediment_imagery_video].[YEAR]
