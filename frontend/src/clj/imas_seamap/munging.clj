(ns imas-seamap.munging
  "We need to automatically extract colours mapped against different
  SM_HAB_CLS values for the plot.  We do this by parsing the geoserver
  SLD (styled layer descriptor) files.  Designed to be used
  interactively from the REPL; not part of the client-application
  itself."
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [globber.glob :refer [glob]]))


(xml/alias-uri 'sld "http://www.opengis.net/sld")
(xml/alias-uri 'ogc "http://www.opengis.net/ogc")

(defn rule->info [rule]
  (let [legend      (zx/xml1-> rule ::sld/Title zx/text)
        literal     (zx/xml1-> rule ::ogc/Filter ::ogc/PropertyIsEqualTo ::ogc/Literal zx/text)
        wildcard    (zx/xml1-> rule ::ogc/Filter ::ogc/PropertyIsLike ::ogc/Literal zx/text)
        complex-and (zx/xml1-> rule ::ogc/Filter ::ogc/And)
        complex-or  (zx/xml1-> rule ::ogc/Filter ::ogc/Or)
        key         (or literal wildcard)
        colour      (zx/xml1-> rule ::sld/PolygonSymbolizer ::sld/Fill ::sld/CssParameter (zx/attr= :name "fill") zx/text)]
    (if (or complex-or complex-and)
      (println "****" legend " Complex expression")
      (when-not colour (println "****" legend " No colour (assumed background image)")))
    [key legend colour]))

(defn sld->rules [zipped-sld]
  (zx/xml-> zipped-sld
            ::sld/StyledLayerDescriptor
            ::sld/NamedLayer
            ::sld/UserStyle
            ::sld/FeatureTypeStyle
            ::sld/Rule))

(defn info->legend-map [habitat-classes [key legend _colour :as info]]
  (into {} (for [habitat-entry (glob key habitat-classes)]
             [habitat-entry legend])))

(defn info->colour-map [habitat-classes [key _legend colour :as info]]
  (into {} (for [habitat-entry (glob key habitat-classes)]
             [habitat-entry colour])))

(defn rules->mappings
  "Returns a pair of maps; the first from SM_HAB_CLS values to
  colours, and the second to legends."
  [rules habitat-classes]
  (let [rule-info      (map rule->info rules)
        colour-mapping (apply merge
                              (map (partial info->colour-map habitat-classes) rule-info))
        legend-mapping (apply merge
                              (map (partial info->legend-map habitat-classes) rule-info))]
    [colour-mapping legend-mapping]))

(def style-files-dir
  "/users/mark_2/projects/geoserver-config-imas/workspaces/seamap/styles/")

(def style-files
  ["SeamapAus_NAT_Aus_margin_geomorph.sld"
   "SeamapAus_NAT_CAMRIS_benthic_substrate.sld"
   "SeamapAus_NAT_CAMRIS_seagrass.sld"
   "SeamapAus_NAT_CoastalGeomorph_Smartline.sld"
   "SeamapAus_NAT_CoastalGeomorph_eco.sld"
   "SeamapAus_NAT_CoastalGeomorph_substrate.sld"
   "SeamapAus_NAT_CoastalGeomorph_surface_eco.sld"
   "SeamapAus_NAT_CoastalWaterways_geomorphic.sld"
   "SeamapAus_NSW_2002.sld"
   "SeamapAus_NSW_2013.sld"
   "SeamapAus_NSW_estuarine_inventory.sld"
   "SeamapAus_NSW_estuarine_macrophytes.sld"
   "SeamapAus_NSW_estuary_ecosystems.sld"
   "SeamapAus_NSW_ocean_ecosystems.sld"
   "SeamapAus_NT_Bynoe_mangrove.sld"
   "SeamapAus_NT_DarwinHarbour_mangrove.sld"
   "SeamapAus_NT_DarwinHarbour_seabed.sld"
   "SeamapAus_NT_EastMiddleArms_communities.sld"
   "SeamapAus_NT_EastMiddleArms_habitats.sld"
   "SeamapAus_NT_OceanicShoals.sld"
   "SeamapAus_NT_PetrelBasin.sld"
   "SeamapAus_NT_mangroves_100.sld"
   "SeamapAus_NT_mangroves_LudmillaCreek.sld"
   "SeamapAus_NT_seagrass.sld"
   "SeamapAus_QLD_EasternBanks_seagrassbiom.sld"
   "SeamapAus_QLD_EasternBanks_seagrasscov.sld"
   "SeamapAus_QLD_EasternBanks_seagrassspec.sld"
   "SeamapAus_QLD_GBRWHA_seagrass.sld"
   "SeamapAus_QLD_GBR_features.sld"
   "SeamapAus_QLD_GoldCoast_seagrass.sld"
   "SeamapAus_QLD_HeronReef_benthiccomm.sld"
   "SeamapAus_QLD_HeronReef_geomorph.sld"
   "SeamapAus_QLD_LowIsles_seagrass.sld"
   "SeamapAus_QLD_MoretonBay_broadscale.sld"
   "SeamapAus_QLD_MoretonBay_coral.sld"
   "SeamapAus_QLD_MoretonBay_seagrass2004.sld"
   "SeamapAus_QLD_MoretonBay_seagrass2011.sld"
   "SeamapAus_QLD_NWTorresStrait_seagrass.sld"
   "SeamapAus_QLD_PointLookout.sld"
   "SeamapAus_QLD_TorresStrait_seagrass_intsub.sld"
   "SeamapAus_QLD_coastal_wetlands.sld"
   "SeamapAus_QLD_reefs_shoals.sld"
   "SeamapAus_QLD_wetland.sld"
   "SeamapAus_SA.sld"
   "SeamapAus_TAS_SeamapTas.sld"
   "SeamapAus_WA_CockburnSound_mapping.sld"
   "SeamapAus_WA_CockburnSound_seagrass.sld"
   "SeamapAus_WA_DPAW.sld"
   "SeamapAus_WA_MOU74.sld"
   "SeamapAus_WA_MarineFutures_biota.sld"
   "SeamapAus_WA_MarineFutures_biota1.sld"
   "SeamapAus_WA_MarineFutures_biota_d.sld"
   "SeamapAus_WA_MarineFutures_reef.sld"
   "SeamapAus_WA_SeagrassSynthesis_labels.sld"
   "SeamapAus_WA_SeagrassSynthesis_pct.sld"
   "SeamapAus_WA_ecosystem_NWShelf.sld"])

(defn slurp-style-files [habitat-classes]
  (reduce
   (fn [[cls-colour cls-titles] filename]
     (println filename)
     (let [rules (-> filename slurp xml/parse-str zip/xml-zip sld->rules)
           [new-colours new-titles] (rules->mappings rules habitat-classes)]
       [(merge cls-colour new-colours)
        (merge cls-titles new-titles)]))
   [{} {}]
   (map #(str style-files-dir %) style-files)))

;;; Note, this silently over-writes overlapping values, and on initial
;;; investigation that happens a fair bit (100 classes with
;;; overlapping legends, 113 colours).  To investigate a bit I've
;;; changed the merge-s above to merge-with (comp flatten vector)
