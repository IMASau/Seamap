(ns imas-seamap.map.events
  (:require [re-frame.core :as re-frame]))


(def ^:private test-layer-data
  [{:name "National" :server_url "http://geoserver.imas.utas.edu.au/geoserver/seamap/wms?" :layer_name "seamap:SeamapAus_NAT_CoastalWaterways_geomorphic" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "NSW" :server_url "http://geoserver.imas.utas.edu.au/geoserver/seamap/wms?" :layer_name "seamap:SeamapAus_NSW_ocean_ecosystems_2002" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "Tasmania" :server_url "http://geoserver.imas.utas.edu.au/geoserver/seamap/wms?" :layer_name "cite:SEAMAP_habitats_Geo" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "imagery" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "third-party" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}])


(defn update-layers [db [_ layers]]
  (assoc-in db [:map :layers] layers))
