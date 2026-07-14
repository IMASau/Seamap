;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.map.utils :refer [bounds->geojson map->bounds] :as map-utils]
            [imas-seamap.map.views :as map-views]
            [imas-seamap.interop.leaflet :as leaflet]
            ["react-leaflet"]
            ["esri-leaflet-renderers"]))

(defn- divider
  "Vertical divider that can be dragged left and right to adjust the split ratio of
   the maps.

   Styling is based on the divider from the Leaflet Side-by-Side library (though
   otherwise has nothing to do with that library).

   Visible only when side-by-side mode is active. This is consistent with the
   behaviour of \"map B\" of the side-by-side view, which is done so we aren't
   removing and re-adding maps from the DOM."
  []
  (let [split-ratio @(re-frame/subscribe [:ui.side-by-side/split-ratio])
        side-by-side-active? @(re-frame/subscribe [:ui.side-by-side/active?])]
    [:div.leaflet-sbs
     {:style {:display (if side-by-side-active? "block" "none")}}
     [:div.leaflet-sbs-divider
      {:style {:left (str split-ratio "%")}}]
     [:input.leaflet-sbs-range
      {:type "range"
       :min 0
       :max 100
       :value split-ratio
       :step "any"
       :on-input #(re-frame/dispatch [:ui.side-by-side/split-ratio (js/parseFloat (.. % -target -value))])
       :style {:position "absolute" :left "-20px" :width "calc(100% + 40px)"}}]]))

(defn map-component []
  (let [map-a                (r/atom nil)
        map-b                (r/atom nil)
        side-by-side-active? (re-frame/subscribe [:ui.side-by-side/active?])
        split-ratio          (re-frame/subscribe [:ui.side-by-side/split-ratio])]
    (r/track!
     #(when (and @map-a @map-b)
        (if @side-by-side-active?
          (do (.sync @map-a @map-b) (.sync @map-b @map-a))
          (do (.unsync @map-a @map-b) (.unsync @map-b @map-a)))))
    (r/track!
     (fn []
       @split-ratio          ; re-render when split ratio changes
       @side-by-side-active? ; re-render when side-by-side mode is toggled
       (when @map-a (.invalidateSize @map-a))
       (when @map-b (.invalidateSize @map-b))))
    (fn []
      (let [{:keys [center zoom bounds]}                @(re-frame/subscribe [:map/props])
            feature-info-1                              @(re-frame/subscribe [:map.feature/info])
            feature-info-2                              @(re-frame/subscribe [:map.feature/info :map-2])
            {:keys [query mouse-loc] :as transect-info} @(re-frame/subscribe [:transect/info])
            {:keys [region] :as region-info}            @(re-frame/subscribe [:map.layer.selection/info])
            show-time-slider?                           @(re-frame/subscribe [:map.time/show-time-slider?])
            side-by-side-active?                        @side-by-side-active?
            split-ratio                                 @split-ratio]
        [:div
         {:style {:display "flex" :height "100vh"}}
         [divider]
         [:div {:style {:height "100%" :width (if side-by-side-active? (str split-ratio "%") "100%")}}
          [leaflet/map-container
           (merge
            {:style                {:height "100%"}
             :crs                  leaflet/crs-epsg3857
             :preferCanvas         true
             :use-fly-to           false
             :center               center
             :zoom                 zoom
             :zoomControl          true
             :minZoom              2
             :keyboard             false ; handled externally
             :close-popup-on-click false ; We'll handle that ourselves
             :ref                  #(do (reset! map-a %) (when % (re-frame/dispatch [:map/update-leaflet-map %])))} ; obtain a reference to the leaflet map in re-frame state, so we can call leaflet map methods from anywhere in the app
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

           ;; This control needs to exist so we can trigger its functions programmatically in
           ;; the control-block element.
           [leaflet/print-control
            {:position   "topleft" :title "Export as PNG"
             :export-only true
             :size-modes ["Current", "A4Landscape", "A4Portrait"]}]

           [leaflet/coordinates-control
            {:decimals 2
             :labelTemplateLat "{y}"
             :labelTemplateLng "{x}"
             :useLatLngOrder   true
             :enableUserInput  false}]
           [leaflet/scale-factor-control {:position "bottomright"}]
           [leaflet/scale-control {:position "bottomright"}]


           (when show-time-slider?
             [:f> leaflet/time-dimension-control
              {:time-dimension
               {:ref #(re-frame/dispatch [:map.time/time-dimension-ref %])
                :defaultTime @(re-frame/subscribe [:map.time/current-time])}
               :ref #(re-frame/dispatch [:map.time/time-dimension-control-ref %])
               :auto-play false
               :playerOptions
               {:buffer 10
                :transitionTime 500
                :startOver true}}])

           [map-views/distance-tooltip]

           [map-views/popup feature-info-1]]]

         [:div
          {:style {:height "100%" :width (str (- 100 split-ratio) "%") :display (if side-by-side-active? "block" "none")}}
          [leaflet/map-container
           {:style {:height "100%"}
            :ref
            (fn [leaflet-map]
              (when (and leaflet-map (not= leaflet-map @map-b))
                (js/console.log "LOG: Registering")
                (reset! map-b leaflet-map)
                (.on leaflet-map "click" #(re-frame/dispatch [:map/clicked (map-utils/leaflet-props %) (map-utils/mouseevent->coords %)]))))}
           [map-views/basemap-layers]
           [map-views/catalogue-layers {:map-id :map-2}]

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
           
           (when show-time-slider?
             [:f> leaflet/time-dimension-control
              {:time-dimension
               {:ref #(re-frame/dispatch [:map.time/time-dimension-ref % :map-2])        ; hardcoded second map ID
                :defaultTime @(re-frame/subscribe [:map.time/current-time :map-2])}      ; ditto
               :ref #(re-frame/dispatch [:map.time/time-dimension-control-ref % :map-2]) ; ditto
               :auto-play false
               :playerOptions
               {:buffer 10
                :transitionTime 500
                :startOver true}}])

           [map-views/popup feature-info-2]]]]))))
