;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.seamap-antarctica.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.map.utils :refer [bounds->geojson map->bounds]]
            [imas-seamap.map.views :as map-views]
            [imas-seamap.interop.leaflet :as leaflet]
            ["esri-leaflet-renderers"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn map-component []
  (let [{:keys [center zoom bounds]}                @(re-frame/subscribe [:map/props])
        feature-info                                @(re-frame/subscribe [:map.feature/info])
        {:keys [query mouse-loc] :as transect-info} @(re-frame/subscribe [:transect/info])
        {:keys [region] :as region-info}            @(re-frame/subscribe [:map.layer.selection/info])]
    [leaflet/map-container
     (merge
      {:id                   "map"
       :crs                  leaflet/crs-epsg3031
       :preferCanvas         true
       :use-fly-to           false
       :center               center
       :zoom                 zoom
       :zoomControl          true
       :minZoom              1
       :maxZoom              15       ; ISA-657 (could support more; see imas-seamap.interop.leaflet/crs-epsg3031)
       :keyboard             false ; handled externally
       :close-popup-on-click false ; We'll handle that ourselves
       :ref                  #(when % (re-frame/dispatch [:map/update-leaflet-map %]))} ; obtain a reference to the leaflet map in re-frame state, so we can call leaflet map methods from anywhere in the app
      (when (seq bounds) {:bounds (map->bounds bounds)}))
    
     [map-views/basemap-layers]
     [map-views/catalogue-layers]
    
     (when query
       [leaflet/geojson-layer {:data (clj->js query)}])
     (when region
       [leaflet/geojson-layer {:data (clj->js (bounds->geojson region))}])
     (when (and query mouse-loc)
       [leaflet/circle-marker {:center      mouse-loc
                               :radius      3
                               :fillColor   "#3f8ffa"
                               :color       "#3f8ffa"
                               :opacity     1
                               :fillOpacity 1}])
    
     (when (:drawing? transect-info)
       [map-views/draw-transect-control])
     (when (:selecting? region-info)
       [map-views/draw-region-control])
    
     [leaflet/coordinates-control
      {:decimals 2
       :labelTemplateLat "{y}"
       :labelTemplateLng "{x}"
       :useLatLngOrder   true
       :enableUserInput  false}]
     [leaflet/scale-factor-control {:position "bottomright"}]
     [leaflet/scale-control {:position "bottomright"}]
    
     [map-views/distance-tooltip]
    
     [map-views/popup feature-info]]))
