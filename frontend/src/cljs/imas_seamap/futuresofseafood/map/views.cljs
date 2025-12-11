;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.futuresofseafood.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [goog.string :as gstring]
            [imas-seamap.map.utils :refer [bounds->geojson map->bounds]]
            [imas-seamap.map.views :as map-views]
            [imas-seamap.interop.leaflet :as leaflet]
            ["react-leaflet" :as ReactLeaflet]
            ["/leaflet-scalefactor/leaflet.scalefactor"]
            ["esri-leaflet-renderers"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn map-component [& children]
  (let [{:keys [center zoom bounds]}                  @(re-frame/subscribe [:map/props])
        {:keys [layer-opacities visible-layers rich-layer-fn cql-filter-fn]} @(re-frame/subscribe [:map/layers])
        {:keys [grouped-base-layers active-base-layer]} @(re-frame/subscribe [:map/base-layers])
        feature-info                                  @(re-frame/subscribe [:map.feature/info])
        {:keys [query mouse-loc distance] :as transect-info} @(re-frame/subscribe [:transect/info])
        {:keys [region] :as region-info}              @(re-frame/subscribe [:map.layer.selection/info])
        download-info                                 @(re-frame/subscribe [:download/info])
        mouse-pos                                     @(re-frame/subscribe [:ui/mouse-pos])]
    (into
     [:div.map-wrapper
      [map-views/download-component download-info]
      [leaflet/map-container
       (merge
        {:id                   "map"
         :crs                  leaflet/crs-epsg3031
         :preferCanvas         true
         :use-fly-to           false
         :center               center
         :zoom                 zoom
         :zoomControl          true
         :scaleFactor          true
         :minZoom              2
         :keyboard             false ; handled externally
         :close-popup-on-click false} ; We'll handle that ourselves
        (when (seq bounds) {:bounds (map->bounds bounds)}))

       ;; Unfortunately, only map container children in react-leaflet v4 are able to
       ;; obtain a reference to the leaflet map through useMap. We make a dummy child here
       ;; to get around the issue and obtain the map.
       (r/create-element
        #(when-let [leaflet-map (ReactLeaflet/useMap)]
           (re-frame/dispatch [:map/update-leaflet-map leaflet-map])
           nil))

       ;; When the current active layer is a vector tile layer, display the default
       ;; basemap layer underneath, since vector tile layers don't support printing.
       (when (= (:layer_type active-base-layer) :vector)
         [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index -1}}
          [map-views/basemap-layer-component (first grouped-base-layers)]])

       ;; Basemap selection:
       [leaflet/layers-control {:position "topright" :auto-z-index false}
        (for [{:keys [id name] :as base-layer} grouped-base-layers]
          ^{:key id}
          [leaflet/layers-control-basemap {:name name :checked (= base-layer active-base-layer)}
           [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index 0}}
            [map-views/basemap-layer-component base-layer]]])]

       ;; Additional basemap layers
       (map-indexed
        (fn [i {:keys [id] :as base-layer}]
          ^{:key (str id (+ i 1))}
          [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index (+ i 1)}}
           [map-views/basemap-layer-component base-layer]])
        (:layers active-base-layer))

       ;; Catalogue layers
       (map-indexed
        (fn [i layer]
          (let [{:keys [id] :as displayed-layer} (or (:displayed-layer (rich-layer-fn layer)) layer)]
            ;; While it's not efficient, we give every layer it's own pane to simplify the
            ;; code.
            ;; Panes are given a name based on a uuid and time because if a pane is given the
            ;; same name as a previously existing pane leaflet complains about a new pane being
            ;; made with the same name as an existing pane (causing leaflet to no longer work).
            ^{:key (str id (+ i 1 (count (:layers active-base-layer))))}
            [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index (+ i 1 (count (:layers active-base-layer)))}}
             [map-views/layer-component
              {:layer           layer
               :displayed-layer displayed-layer
               :layer-opacities layer-opacities
               :cql-filter      (cql-filter-fn layer)}]]))
        visible-layers)

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

       ;; This control needs to exist so we can trigger its functions programmatically in
       ;; the control-block element.
       [leaflet/print-control
        {:position   "topleft" :title "Export as PNG"
         :export-only true
         :size-modes ["Current", "A4Landscape", "A4Portrait"]}]

       [leaflet/scale-control]

       [leaflet/coordinates-control
        {:decimals 2
         :labelTemplateLat "{y}"
         :labelTemplateLng "{x}"
         :useLatLngOrder   true
         :enableUserInput  false}]

       (when (and mouse-pos distance) [map-views/distance-tooltip {:mouse-pos mouse-pos :distance distance}])

       [map-views/popup feature-info]]]

     children)))
