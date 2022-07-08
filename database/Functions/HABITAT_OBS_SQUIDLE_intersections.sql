-- Takes any geometry and finds out what habitat observations from
-- VW_HABITAT_OBS_SQUIDLE intersect with that geometry. Returns the intersecting
-- habitat observations as a table.

CREATE FUNCTION [dbo].[HABITAT_OBS_SQUIDLE_intersections] (@boundary GEOMETRY)
RETURNS TABLE
AS RETURN (
  SELECT
    [CAMPAIGN_NAME],
    [DEPLOYMENT_ID],
    [DATE],
    [METHOD],
    [images],
    [total_annotations],
    [public_annotations]
  FROM [dbo].[VW_HABITAT_OBS_SQUIDLE]
  WHERE [geom].STIntersects(@boundary) = 1
);
