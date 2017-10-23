CREATE view [dbo].[SeamapAus_Regions_VIEW] as
select 'seamap:seamapaus_nat_aus_margin_geomorph_2006'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_Aus_margin_geomorph_2006
union all
select 'seamap:seamapaus_nat_camris_benthic_substrate'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CAMRIS_benthic_substrate
union all
select 'seamap:seamapaus_nat_camris_seagrass'                   as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CAMRIS_seagrass
union all
select 'seamap:seamapaus_nat_coastalgeomorph_offshorerc100k'    as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_OffshoreRC100K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_ozestuariesrc100k' as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_OzEstuariesRC100K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_rc100k'            as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_RC100K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_rc25k'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_RC25K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_rc50k'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_RC50K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_regolith25k'       as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_Regolith25K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_smartline100k'     as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_Smartline100K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_surfacegeorc100k'  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC100K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_surfacegeorc250k'  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC250K
union all
select 'seamap:seamapaus_nat_coastalgeomorph_surfacegeorc25k'   as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC25K
union all
select 'seamap:seamapaus_nat_coastalwaterways_geomorphic'       as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NAT_CoastalWaterways_geomorphic
union all
select 'seamap:seamapaus_nsw_estuarine_inventory'               as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_estuarine_inventory
union all
select 'seamap:seamapaus_nsw_estuarine_macrophytes'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_estuarine_macrophytes
union all
select 'seamap:seamapaus_nsw_estuary_ecosystems_2002'           as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_estuary_ecosystems_2002
union all
select 'seamap:seamapaus_nsw_marine_habitats_2002'              as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_marine_habitats_2002
union all
select 'seamap:seamapaus_nsw_marine_habitats_2013'              as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_marine_habitats_2013
union all
select 'seamap:seamapaus_nsw_ocean_ecosystems_2002'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NSW_ocean_ecosystems_2002
union all
select 'seamap:seamapaus_nt_bynoeharbour_mangrove'              as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_BynoeHarbour_mangrove
union all
select 'seamap:seamapaus_nt_darwinharbour_mangrove'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_DarwinHarbour_mangrove
union all
select 'seamap:seamapaus_nt_darwinharbour_seabed_mapping'       as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_DarwinHarbour_seabed_mapping
union all
select 'seamap:seamapaus_nt_eastmiddlearms_communities'         as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_EastMiddleArms_communities
union all
select 'seamap:seamapaus_nt_eastmiddlearms_habitats'            as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_EastMiddleArms_habitats
union all
select 'seamap:seamapaus_nt_oceanicshoals_geomorphology'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_OceanicShoals_geomorphology
union all
select 'seamap:seamapaus_nt_petrelbasin_geomorphology'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_PetrelBasin_geomorphology
union all
select 'seamap:seamapaus_nt_mangroves_100'                      as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_mangroves_100
union all
select 'seamap:seamapaus_nt_mangroves_ludmillacreek'            as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_mangroves_LudmillaCreek
union all
select 'seamap:seamapaus_nt_seagrass'                           as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_NT_seagrass
union all
select 'seamap:seamapaus_qld_easternbanks_seagrass_species'     as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_EasternBanks_seagrass_species
union all
select 'seamap:seamapaus_qld_gbrwha_seagrass'                   as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_GBRWHA_seagrass
union all
select 'seamap:seamapaus_qld_gbr_features'                      as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_GBR_features
union all
select 'seamap:seamapaus_qld_goldcoast_seagrass'                as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_GoldCoast_seagrass
union all
select 'seamap:seamapaus_qld_heronreef_benthiccomm'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_HeronReef_benthiccomm
union all
select 'seamap:seamapaus_qld_heronreef_geomorph'                as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_HeronReef_geomorph
union all
select 'seamap:seamapaus_qld_lowisles_seagrass'                 as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_LowIsles_seagrass
union all
select 'seamap:seamapaus_qld_moretonbay_broadscale'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_MoretonBay_broadscale
union all
select 'seamap:seamapaus_qld_moretonbay_coral'                  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_MoretonBay_coral
union all
select 'seamap:seamapaus_qld_moretonbay_seagrass_2004'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_MoretonBay_seagrass_2004
union all
select 'seamap:seamapaus_qld_moretonbay_seagrass_2011'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_MoretonBay_seagrass_2011
union all
select 'seamap:seamapaus_qld_nwtorresstrait_seagrass'           as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_NWTorresStrait_seagrass
union all
select 'seamap:seamapaus_qld_pointlookout_ecology'              as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_PointLookout_ecology
union all
select 'seamap:seamapaus_qld_torresstrait_seagrass_intertidal'  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_TorresStrait_seagrass_intertidal
union all
select 'seamap:seamapaus_qld_torresstrait_seagrass_subtidal'    as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_TorresStrait_seagrass_subtidal
union all
select 'seamap:seamapaus_qld_coastal_wetlands'                  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_coastal_wetlands
union all
select 'seamap:seamapaus_qld_reefs_shoals'                      as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_reefs_shoals
union all
select 'seamap:seamapaus_qld_wetland_v4'                        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_QLD_wetland_v4
union all
select 'seamap:seamapaus_sa_state_benthic_habitats'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_SA_state_benthic_habitats
union all
select 'seamap:seamapaus_tas_seamaptas'                         as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_TAS_SeamapTas
union all
select 'seamap:seamapaus_vic_gippslandlakes_biotopes'            as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_VIC_GippslandLakes_biotopes
union all
select 'seamap:seamapaus_vic_opencoast_biotopes'                as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_VIC_OpenCoast_biotopes
union all
select 'seamap:seamapaus_vic_ppb_biotopes'                      as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_VIC_PPB_biotopes
union all
select 'seamap:seamapaus_vic_wpb_biotopes'                      as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_VIC_WPB_biotopes
union all
select 'seamap:seamapaus_wa_cockburnsound_mapping'              as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_CockburnSound_mapping
union all
select 'seamap:seamapaus_wa_cockburnsound_seagrass_1999'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_CockburnSound_seagrass_1999
union all
select 'seamap:seamapaus_wa_dpaw_marine_habitats'               as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_DPAW_marine_habitats
union all
select 'seamap:seamapaus_wa_mou74_scottreef'                    as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_MOU74_ScottReef
union all
select 'seamap:seamapaus_wa_mou74_ashmorereef'                  as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_MOU74_AshmoreReef
union all
select 'seamap:seamapaus_wa_marinefutures_biota'                as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_MarineFutures_biota
union all
select 'seamap:seamapaus_wa_marinefutures_reef'                 as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_MarineFutures_reef
union all
select 'seamap:seamapaus_wa_nwshelf_ecosystems'                 as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_NWShelf_ecosystems
union all
select 'seamap:seamapaus_wa_seagrass_beaufort_2009'             as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_Beaufort_2009
union all
select 'seamap:seamapaus_wa_seagrass_hardyinlet_2008'           as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_HardyInlet_2008
union all
select 'seamap:seamapaus_wa_seagrass_irwininlet_2009'           as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_IrwinInlet_2009
union all
select 'seamap:seamapaus_wa_seagrass_leschenault_2009'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_Leschenault_2009
union all
select 'seamap:seamapaus_wa_seagrass_oysterharbour_1988'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_OysterHarbour_1988
union all
select 'seamap:seamapaus_wa_seagrass_oysterharbour_1996'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_OysterHarbour_1996
union all
select 'seamap:seamapaus_wa_seagrass_oysterharbour_2006'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_OysterHarbour_2006
union all
select 'seamap:seamapaus_wa_seagrass_princessroyal_1996'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_PrincessRoyal_1996
union all
select 'seamap:seamapaus_wa_seagrass_princessroyal_2006'        as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_PrincessRoyal_2006
union all
select 'seamap:seamapaus_wa_seagrass_stokesinlet_2009'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_StokesInlet_2009
union all
select 'seamap:seamapaus_wa_seagrass_swancanning_2011'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_SwanCanning_2011
union all
select 'seamap:seamapaus_wa_seagrass_walpole_nornalup_2009'     as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_Walpole_Nornalup_2009
union all
select 'seamap:seamapaus_wa_seagrass_wellsteadestuary_2009'     as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_WellsteadEstuary_2009
union all
select 'seamap:seamapaus_wa_seagrass_wilsoninlet_2007'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_WilsonInlet_2007
union all
select 'seamap:seamapaus_wa_seagrass_wilsoninlet_2008'          as layer_name, SM_HAB_CLS as habitat, geom from SeamapAus_WA_seagrass_WilsonInlet_2008
union all
select 'seamap:finalproduct_seamapaus'							as layer_name, NAT_HAB_CL as habitat, geom from FINALPRODUCT_SeamapAus
;
