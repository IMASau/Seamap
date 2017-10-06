CREATE view [dbo].[SeamapAus_BOUNDARIES_CMR2014] as
select mpa_name AS REGION, geometry::UnionAggregate(geom) AS geom from [dbo].[SeamapAus_BOUNDARIES_cmr2014_FULL] group by mpa_name;

GO


