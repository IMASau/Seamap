CREATE view [dbo].[SeamapAus_BOUNDARIES_MEOW_PROV] as
select province AS REGION, geometry::UnionAggregate(geom) AS geom from [dbo].[SeamapAus_BOUNDARIES_meow_FULL] group by province;

GO


