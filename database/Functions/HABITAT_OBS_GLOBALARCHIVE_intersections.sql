-- Takes any geometry and finds out what habitat observations from
-- VW_HABITAT_OBS_GLOBALARCHIVE intersect with that geometry. Returns the
-- intersecting habitat observations as a table.

CREATE FUNCTION [dbo].[HABITAT_OBS_GLOBALARCHIVE_intersections] (@boundary GEOMETRY)
RETURNS TABLE
AS RETURN (
  SELECT
    [CAMPAIGN_NAME],
    [DEPLOYMENT_ID],
    [DATE],
    [METHOD],
    [video_time]
  FROM [dbo].[VW_HABITAT_OBS_GLOBALARCHIVE]
  WHERE [geom].STIntersects(@boundary) = 1
);
