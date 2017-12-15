SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		Mark Hepburn, Condense
-- Create date: 12/12/2017
-- Description:	Update the region-statistics precomputation table when a new boundary layer is added.  Assumes that the view SeamapAus_Boundaries_View has already been updated.
-- =============================================
CREATE PROCEDURE SeamapAus_Add_Boundary
	@boundary_layer_id int
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;

	-- Depending on how confident we felt, we could also make this
	-- idempotent by deleting everything with that habitat-layer id
	-- first... instead we will warn if it's already present.
    IF EXISTS(SELECT * FROM SeamapAus_Habitat_By_Region WHERE boundary_layer_id = @boundary_layer_id)
        RAISERROR('Data already exists for that boundary layer', -1, -1);

    INSERT INTO SeamapAus_Habitat_By_Region (boundary_layer_id, habitat_layer_id, habitat, region, boundary_area, area, geom)
    SELECT boundary_layer_id, habitat_layer_id, habitat, region, boundary_area, sum(region_area) AS area, geometry::UnionAggregate(region_geom) AS geom--, sum(percentage) as percentage
    FROM (
      SELECT boundary_layer_id, habitat_layer_id, region, boundary_area, habitat, region_geom, region_geom.STArea() AS region_area--, 100 * region_geom.STArea() / boundary_area as percentage
      FROM (
        SELECT bc.id AS boundary_layer_id, hc.id AS habitat_layer_id, h.habitat, b.region, b.geom.STIntersection(h.geom) AS region_geom, b.geom.STArea() AS boundary_area
        FROM SeamapAus_Boundaries_View b
        JOIN SeamapAus_Regions_VIEW h on b.geom.STIntersects(h.geom) = 1
        JOIN catalogue_layer bc ON b.boundary_layer = COALESCE(bc.detail_layer, bc.layer_name)
        JOIN catalogue_layer hc ON h.layer_name     = COALESCE(hc.detail_layer, hc.layer_name)
        WHERE bc.id = @boundary_layer_id
      ) sub1
    ) sub2
    GROUP BY boundary_layer_id, habitat_layer_id, habitat, region, boundary_area;

END
GO
