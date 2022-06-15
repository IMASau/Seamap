CREATE VIEW [dbo].[SeamapAus_Parks_View] AS
SELECT
  [RESNAME] AS [name],
  [NETNAME] AS [network],
  [geom]
FROM [dbo].[SeamapAus_BOUNDARIES_AMP2018_RESERVES]
