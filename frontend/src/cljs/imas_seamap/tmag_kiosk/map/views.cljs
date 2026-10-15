;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.map.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.map.utils :refer [map->bounds]]
            [imas-seamap.map.views :as map-views]
            [imas-seamap.interop.leaflet :as leaflet]
            ["/leaflet-scalefactor/leaflet.scalefactor"]
            ["esri-leaflet-renderers"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn map-component []
  (let [{:keys [center zoom bounds]} @(re-frame/subscribe [:map/props])
        feature-info                 @(re-frame/subscribe [:map.feature/info])]
    [leaflet/map-container
     (merge
      {:id                   "map"
       :crs                  leaflet/crs-epsg3857
       :preferCanvas         true
       :use-fly-to           false
       :center               center
       :zoom                 zoom
       :zoomControl          true
       :scaleFactor          true
       :minZoom              2
       :keyboard             false ; handled externally
       :close-popup-on-click false ; We'll handle that ourselves
       :ref                  #(when % (re-frame/dispatch [:map/update-leaflet-map %]))} ; obtain a reference to the leaflet map in re-frame state, so we can call leaflet map methods from anywhere in the app
      (when (seq bounds) {:bounds (map->bounds bounds)}))

     [map-views/basemap-layers]
     [map-views/catalogue-layers]

     [leaflet/scale-control]

     [map-views/popup feature-info]]))
