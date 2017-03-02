(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))


(def ^:private test-layer-data
  [{:name "National" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NAT_CoastalWaterways_geomorphic" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "NSW" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NSW_ocean_ecosystems_2002" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   {:name "Tasmania" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "cite:SEAMAP_habitats_Geo" :category "habitat" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "geoserver" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "bathymetry" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "imagery" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}
   #_{:name "" :server_url "" :layer_name "" :category "third-party" :bounding_box "" :metadata_url "" :description "" :zoom_info "" :server_type "" :legend_url "" :date_start "" :date_end ""}])

(defn str->bounds [bounds-str]
  (as-> bounds-str bnds
      (string/split bnds ",")
      (map js/parseFloat bnds)
      (map vector [:west :south :east :north] bnds)
      (into {} bnds)))

(defn process-layer [layer]
  (-> layer
      ;; TODO: convert the dates, etc too
      (update :bounding_box str->bounds)
      (update :category keyword)
      (update :server_type keyword)))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn update-layers [db [_ layers]]
  (->> layers
       process-layers
       (assoc-in db [:map :layers])))

(defn toggle-layer [db [_ layer]]
  (update-in db [:map :active-layers]
             #(if (% layer)
                (disj % layer)
                (conj % layer))))

(defn visible-layers [{:keys [west south east north] :as bounds} layers]
  (filter
   (fn [{:keys [bounding_box]}]
     (not (or (> (:south bounding_box) north)
              (< (:north bounding_box) south)
              (> (:west  bounding_box) east)
              (< (:east  bounding_box) west))))
   layers))

(defn update-active-layers
  "Utility to recalculate layers that are displayed.  When the
  viewport or zoom changes, we may need to switch out a layer for a
  coarser/finer resolution one.  Only applies to habitat layers."
  [{:keys [map] :as db}]
  ;; Basic idea:
  ;; * check that any habitat layer is currently displayed (ie, don't start with no habitats, then zoom in and suddenly display one!)
  ;; * filter out habitat layers from actives
  ;; * add back in those that are visible, and past the zoom cutoff
  ;; * assoc back onto the db
  (js/console.warn "visible:" (visible-layers (:bounds map) (:layers map)))
  db)

(defn map-view-updated [db [_ {:keys [zoom center bounds]}]]
  (-> db
      (update-in [:map] assoc
                 :zoom zoom
                 :center center
                 :bounds bounds)
      update-active-layers))
