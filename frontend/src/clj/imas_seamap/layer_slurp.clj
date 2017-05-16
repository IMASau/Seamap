(ns imas-seamap.layer-slurp
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.string :as string]
            [clojure.zip :as zip])
  (:import  [org.osgeo.proj4j CoordinateTransformFactory CRSFactory ProjCoordinate]))

(def base-dir "/users/mark_2/projects/geoserver-config-imas/workspaces/seamap/IMASSeamapAU/")
(def layer-dirs
  ["SeamapAus_NAT_Aus_margin_geomorph_2006"
   "SeamapAus_NAT_CAMRIS_benthic_substrate"
   "SeamapAus_NAT_CAMRIS_seagrass"
   "SeamapAus_NAT_CoastalGeomorph_OffshoreRC100K"
   "SeamapAus_NAT_CoastalGeomorph_OzEstuariesRC100K"
   "SeamapAus_NAT_CoastalGeomorph_RC100K"
   "SeamapAus_NAT_CoastalGeomorph_RC25K"
   "SeamapAus_NAT_CoastalGeomorph_RC50K"
   "SeamapAus_NAT_CoastalGeomorph_Regolith25K"
   "SeamapAus_NAT_CoastalGeomorph_Smartline100K"
   "SeamapAus_NAT_CoastalGeomorph_Smartline100K_MAP"
   "SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC100K"
   "SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC250K"
   "SeamapAus_NAT_CoastalGeomorph_SurfaceGeoRC25K"
   "SeamapAus_NAT_CoastalWaterways_geomorphic"
   "SeamapAus_NSW_estuarine_inventory"
   "SeamapAus_NSW_estuarine_macrophytes"
   "SeamapAus_NSW_estuary_ecosystems_2002"
   "SeamapAus_NSW_marine_habitats_2002"
   "SeamapAus_NSW_marine_habitats_2013"
   "SeamapAus_NSW_ocean_ecosystems_2002"
   "SeamapAus_NT_BynoeHarbour_mangrove"
   "SeamapAus_NT_DarwinHarbour_mangrove"
   "SeamapAus_NT_DarwinHarbour_seabed_mapping"
   "SeamapAus_NT_DarwinHarbour_seabed_mapping_MAP"
   "SeamapAus_NT_EastMiddleArms_communities"
   "SeamapAus_NT_EastMiddleArms_habitats"
   "SeamapAus_NT_OceanicShoals_geomorphology"
   "SeamapAus_NT_PetrelBasin_geomorphology"
   "SeamapAus_NT_mangroves_100"
   "SeamapAus_NT_mangroves_LudmillaCreek"
   "SeamapAus_NT_seagrass"
   "SeamapAus_QLD_EasternBanks_seagrass_biomass"
   "SeamapAus_QLD_EasternBanks_seagrass_cover"
   "SeamapAus_QLD_EasternBanks_seagrass_species"
   "SeamapAus_QLD_GBRWHA_seagrass"
   "SeamapAus_QLD_GBR_features"
   "SeamapAus_QLD_GoldCoast_seagrass"
   "SeamapAus_QLD_HeronReef_benthiccomm"
   "SeamapAus_QLD_HeronReef_geomorph"
   "SeamapAus_QLD_LowIsles_seagrass"
   "SeamapAus_QLD_MoretonBay_broadscale"
   "SeamapAus_QLD_MoretonBay_coral"
   "SeamapAus_QLD_MoretonBay_seagrass_2004"
   "SeamapAus_QLD_MoretonBay_seagrass_2011"
   "SeamapAus_QLD_NWTorresStrait_seagrass"
   "SeamapAus_QLD_PointLookout_ecology"
   "SeamapAus_QLD_TorresStrait_seagrass_intertidal"
   "SeamapAus_QLD_TorresStrait_seagrass_subtidal"
   "SeamapAus_QLD_coastal_wetlands"
   "SeamapAus_QLD_reefs_shoals"
   "SeamapAus_QLD_wetland_v4"
   "SeamapAus_SA_state_benthic_habitats"
   "SeamapAus_TAS_SeamapTas"
   "SeamapAus_WA_CockburnSound_mapping"
   "SeamapAus_WA_CockburnSound_seagrass_1999"
   "SeamapAus_WA_DPAW_marine_habitats"
   "SeamapAus_WA_MOU74_AshmoreReef"
   "SeamapAus_WA_MOU74_ScottReef"
   "SeamapAus_WA_MarineFutures_biota"
   "SeamapAus_WA_MarineFutures_biota_Albrolhos"
   "SeamapAus_WA_MarineFutures_biota_BrokeInlet"
   "SeamapAus_WA_MarineFutures_biota_Capes"
   "SeamapAus_WA_MarineFutures_biota_GeographeBay"
   "SeamapAus_WA_MarineFutures_biota_JurienBay"
   "SeamapAus_WA_MarineFutures_biota_MiddleIsland"
   "SeamapAus_WA_MarineFutures_biota_MountGardner"
   "SeamapAus_WA_MarineFutures_biota_PointAnn"
   "SeamapAus_WA_MarineFutures_biota_Rottnest"
   "SeamapAus_WA_MarineFutures_reef"
   "SeamapAus_WA_MarineFutures_reef_Albrolhos"
   "SeamapAus_WA_MarineFutures_reef_BrokeInlet"
   "SeamapAus_WA_MarineFutures_reef_Capes"
   "SeamapAus_WA_MarineFutures_reef_GeographeBay"
   "SeamapAus_WA_MarineFutures_reef_JurienBay"
   "SeamapAus_WA_MarineFutures_reef_MiddleIsland"
   "SeamapAus_WA_MarineFutures_reef_MountGardner"
   "SeamapAus_WA_MarineFutures_reef_PointAnn"
   "SeamapAus_WA_MarineFutures_reef_Rottnest"
   "SeamapAus_WA_ecosystem_NWShelf"
   "SeamapAus_WA_seagrass_Beaufort_2009"
   "SeamapAus_WA_seagrass_HardyInlet_2008"
   "SeamapAus_WA_seagrass_IrwinInlet_2009"
   "SeamapAus_WA_seagrass_Leschenault_2009"
   "SeamapAus_WA_seagrass_OysterHarbour_1988"
   "SeamapAus_WA_seagrass_OysterHarbour_1996"
   "SeamapAus_WA_seagrass_PrincessRoyal_1996"
   "SeamapAus_WA_seagrass_PrincessRoyal_2006"
   "SeamapAus_WA_seagrass_StokesInlet_2009"
   "SeamapAus_WA_seagrass_SwanCanning_2011"
   "SeamapAus_WA_seagrass_Walpole_Nornalup_2009"
   "SeamapAus_WA_seagrass_WellsteadEstuary_2009"
   "SeamapAus_WA_seagrass_WilsonInlet_2007"
   "SeamapAus_WA_seagrass_WilsonInlet_2008"])

(def ctfactory (CoordinateTransformFactory.))
(def crsfactory (CRSFactory.))

(def wgs84 (.createFromParameters crsfactory "WGS84" "+proj=longlat +datum=WGS84 +units=degrees"))
(def epsg3112 (.createFromParameters crsfactory "EPSG:3112" "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(def trans (.createTransform ctfactory epsg3112 wgs84))

(defn coords->geo [x y]
  (let [psrc (ProjCoordinate. x y)
        ptgt (.transform trans psrc (ProjCoordinate.))]
    [(.x ptgt) (.y ptgt)]))

(defn ->geobbox [znbbox]
  (let [to-float #(Float/parseFloat %)
        minx     (zx/xml1-> znbbox :minx zx/text to-float)
        miny     (zx/xml1-> znbbox :miny zx/text to-float)
        maxx     (zx/xml1-> znbbox :maxx zx/text to-float)
        maxy     (zx/xml1-> znbbox :maxy zx/text to-float)
        ll       (coords->geo minx miny)
        ur       (coords->geo maxx maxy)]
    [[ll ur]]))

(defn extract-layer-defn [zipped-xml]
  (let [title                          (zx/xml1-> zipped-xml :featureType :title zx/text)
        name                           (zx/xml1-> zipped-xml :featureType :name zx/text)
        [[llx lly] [urx ury] :as bbox] (zx/xml1-> zipped-xml :featureType :nativeBoundingBox ->geobbox)]
    {:name                title
     :layer_name          (str "seamap:" name)
     :bounding_box        (str llx "," lly "," urx "," ury)
     :server_url          "http://geoserver.imas.utas.edu.au/geoserver/wms"
     :detail_resolution   (not (string/starts-with? name "SeamapAus_NAT"))
     :category            1 ; habitat
     :server_type         2 ; geoserver
     :data_classification nil
     :organisation        nil
     :metadata_url        "http://metadata.imas.utas.edu.au/"
     :description         "-"
     :date_start          nil
     :date_end            nil}))

(defn file->layer [filename]
  (-> filename
      slurp
      xml/parse-str
      zip/xml-zip
      extract-layer-defn))

;;; The global-variables top-level worker:
(defn layerfiles->json
  ([offset]
   (map-indexed
    (fn [i layername]
      (let [layer-defn (file->layer (str base-dir layername "/featuretype.xml"))]
        {:fields layer-defn
         :model  "catalogue.layer"
         :pk     (+ offset i)}))
    layer-dirs))
  ([] (layerfiles->json 10)))
