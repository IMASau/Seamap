CREATE view [dbo].[SeamapAus_BOUNDARIES_IMCRA_PROVBIO] as
select pb_name AS REGION, geometry::UnionAggregate(geom) AS geom from [dbo].[SeamapAus_BOUNDARIES_imcra_FULL] group by pb_name;
GO


