-- Takes any geometry and finds out what habitat observations from
-- VW_HABITAT_OBS_SEDIMENT intersect with that geometry. Returns the intersecting
-- habitat observations as a table.

CREATE FUNCTION [dbo].[HABITAT_OBS_SEDIMENT_intersections] (@boundary GEOMETRY)
RETURNS TABLE
AS RETURN (
  SELECT
    [SURVEY],
    [SAMPLE_ID],
    [DATE],
    [METHOD],
    [ANALYSED]
  FROM [dbo].[VW_HABITAT_OBS_SEDIMENT]
  WHERE [geom].STIntersects(@boundary) = 1
);
