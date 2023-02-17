-- Contains the geometry for each Marine Sediments habitat observation.

CREATE VIEW [dbo].[VW_HABITAT_OBS_SEDIMENT] AS
SELECT
  [survey_name] AS [SURVEY],
  [fid] AS [SAMPLE_ID],
  (
    CASE
      WHEN [acquire_date] = '' THEN NULL
      ELSE CAST([acquire_date] AS DATE)
    END
  ) AS [DATE],
  [sample_type] AS [METHOD],
  (
    CASE
      WHEN [ANALYSIS_METHOD] = 'NA' THEN 'NO'
      ELSE 'YES'
    END
  ) AS [ANALYSED],
  [geom]
FROM [TRANSFORM_MARS];
