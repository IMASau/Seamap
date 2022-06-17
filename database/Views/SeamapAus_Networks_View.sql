CREATE VIEW [dbo].[SeamapAus_Networks_View] AS
SELECT
  [NETNAME] AS [name],
  [geom]
FROM [dbo].[SeamapAus_BOUNDARIES_AMP2018_NETWORKS]
