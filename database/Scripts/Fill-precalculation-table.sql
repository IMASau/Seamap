create table SeamapAus_Habitat_By_Region (
    boundary_layer_id int          foreign key references catalogue_layer(id) not null,
	region            char(50)                                                not null,
	habitat_layer_id  int          foreign key references catalogue_layer(id) not null,
	habitat           varchar(100)                                            not null,
	geom              geometry                                                not null,
	area              numeric                                                 not null,
	boundary_area     numeric                                                 not null
);
go

create clustered index idx_seamapaus_habitat_by_region on seamapaus_habitat_by_region(boundary_layer_id, habitat_layer_id, region);
go

declare @habid int;
select @habid = min(id) from catalogue_layer where category_id = 1;

-- output only:
declare @layername nvarchar(200);

-- This ran into problems (hadn't completed after more than a day!) with the Darwin seabed layer (2.5 million rows), so I used a multi-stage process for that one:
-- insert into a temp table, performing a geometry::UnionAggregate and grouping by habitat...
-- actually doing this in two stages; I had memory errors doing this in a single step, so I first grouped by habitat and id%10, and then into a second table just grouping by habitat.
-- then this single-layer table can be spliced into the generic query below for insertion (which still took 3.5 hours).

while @habid is not null
begin

select @layername = coalesce(detail_layer, layer_name) from catalogue_layer where id = @habid;
print 'Processing layer ' + @layername;

insert into SeamapAus_Habitat_By_Region (boundary_layer_id, habitat_layer_id, habitat, region, boundary_area, area, geom)
select boundary_layer_id, habitat_layer_id, habitat, region, boundary_area, sum(region_area) as area, geometry::UnionAggregate(region_geom) as geom--, sum(percentage) as percentage
from (
  select boundary_layer_id, habitat_layer_id, region, boundary_area, habitat, region_geom, region_geom.STArea() as region_area--, 100 * region_geom.STArea() / boundary_area as percentage
  from (
    select bc.id as boundary_layer_id, hc.id as habitat_layer_id, h.habitat, b.region, b.geom.STIntersection(h.geom) as region_geom, b.geom.STArea() as boundary_area
    from SeamapAus_Boundaries_View b
    join SeamapAus_Regions_VIEW h on b.geom.STIntersects(h.geom) = 1
    join catalogue_layer bc on b.boundary_layer = coalesce(bc.detail_layer, bc.layer_name)
    join catalogue_layer hc on h.layer_name     = coalesce(hc.detail_layer, hc.layer_name)
	where hc.id = @habid
  ) sub1
) sub2
group by boundary_layer_id, habitat_layer_id, habitat, region, boundary_area;

  print '...Done.';
  select @habid = min(id) from catalogue_layer where category_id = 1 and id > @habid and id in (38, 39, 40, 41, 43, 44);
end


--sp_spaceused 'Seamapaus_habitat_by_region'

