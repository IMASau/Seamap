create view SeamapAus_Boundaries_VIEW as

select 'seamap:SeamapAus_BOUNDARIES_CMR2014' as boundary_layer, region, geom from SeamapAus_BOUNDARIES_CMR2014
union all
select 'seamap:SeamapAus_BOUNDARIES_MEOW_PROV' as boundary_layer, region, geom from SeamapAus_BOUNDARIES_MEOW_PROV
union all
select 'seamap:SeamapAus_BOUNDARIES_IMCRA_PROVBIO' as boundary_layer, region, geom from SeamapAus_BOUNDARIES_IMCRA_PROVBIO;

GO
