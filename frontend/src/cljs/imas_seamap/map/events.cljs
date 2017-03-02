(ns imas-seamap.map.events
  (:require [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))


(def ^:private test-layer-data
  [{:name "National" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NAT_CoastalWaterways_geomorphic" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "NSW" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NSW_ocean_ecosystems_2002" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "Tasmania" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "cite:SEAMAP_habitats_Geo" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "imagery" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "third-party" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}])

(defn process-layers [layers]
  (map #(-> %
            ;; TODO: convert the dates, bounding box, too
            (update :category keyword)
            (update :server_type keyword))
       layers))

(defn update-layers [db [_ layers]]
  (->> layers
       process-layers
       (assoc-in db [:map :layers])))

(defn toggle-layer [db [_ layer]]
  (update-in db [:map :active-layers]
             #(if (% layer)
                (disj % layer)
                (conj % layer))))

(defn map-view-updated [db [_ {:keys [zoom center bounds]}]]
  (update-in db [:map] assoc
             :zoom zoom
             :center center
             :bounds bounds))
