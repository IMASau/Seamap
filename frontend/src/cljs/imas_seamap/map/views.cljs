;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [copy-text handler-dispatch create-shadow-dom-element format-number] :include-macros true]
            [imas-seamap.map.utils :refer [bounds->geojson download-type->str map->bounds bounds->map]]
            [imas-seamap.interop.leaflet :as leaflet]
            [goog.string :as gstring]
            ["react-leaflet" :as ReactLeaflet]
            ["/leaflet-zoominfo/L.Control.Zoominfo"]
            ["/leaflet-scalefactor/leaflet.scalefactor"]
            ["esri-leaflet-renderers"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn point->latlng [[x y]] {:lat y :lng x})

(defn point-distance [[x1 y1 :as _p1] [x2 y2 :as _p2]]
  (let [xd (- x2 x1) yd (- y2 y1)]
    (js/Math.sqrt (+ (* xd xd) (* yd yd)))))

;;; Rather round-about logic: we want to attach a callback based on a
;;; specific layer, but because closure are created fresh they fail
;;; equality tests and thus the map re-renders, causing flickering.
;;; So, we take the raw event instead, pick out enough info to
;;; identify the layer, and then dispatch.
;;; We could actually rely on the event handler (:load-start, etc)
;;; looking up the layer itself, as a slight efficiency, but until
;;; it's necessary I prefer the API consistency of passing the layer
;;; to the dispatch.
(defn event->layer-tuple [e]
  (let [target (. e -target)]
    [(. target -_url)
     (.. target -options -layers)]))
(defn on-load-start [e]
  (let [lt (event->layer-tuple e)
        layer (-> @(re-frame/subscribe [:map.layers/lookup]) (get lt))]
    (re-frame/dispatch [:map.layer/load-start layer])))
(defn on-tile-load-start [e]
  (let [lt (event->layer-tuple e)
        layer (-> @(re-frame/subscribe [:map.layers/lookup]) (get lt))]
    (re-frame/dispatch [:map.layer/tile-load-start layer])))
(defn on-tile-error [e]
  (let [lt (event->layer-tuple e)
        layer (-> @(re-frame/subscribe [:map.layers/lookup]) (get lt))]
    (re-frame/dispatch [:map.layer/load-error layer])))
(defn on-load-end   [e]
  (let [lt (event->layer-tuple e)
        layer (-> @(re-frame/subscribe [:map.layers/lookup]) (get lt))]
    (re-frame/dispatch [:map.layer/load-finished layer])))

(defn download-component [{:keys [display-link link bbox download-type] :as _download-info}]
  (let [type-str (download-type->str download-type)]
    [b/dialogue {:is-open   display-link
                 :title     (str "Download " type-str)
                 :icon "import"
                 :on-close  (handler-dispatch [:ui.download/close-dialogue])}
     [:div.bp3-dialog-body
      [:p [:a {:href link :target "_blank"}
           "Click here to download"
           (when bbox " region")
           " as "
           type-str]]]
     [:div.bp3-dialog-footer
      [:div.bp3-dialog-footer-actions
       [b/button {:text     "Done"
                  :intent   b/INTENT-PRIMARY
                  :on-click (handler-dispatch [:ui.download/close-dialogue])}]]]]))

(defn share-control [_props]
  [leaflet/custom-control {:position "topleft" :container {:className "leaflet-bar"}}
   ;; The copy-text has to be here rather than in a handler, because
   ;; Firefox won't do execCommand('copy') outside of a "short-lived
   ;; event handler"
   [:a {:on-click #(do (copy-text js/location.href)
                       (re-frame/dispatch  [:create-save-state]))}
    [b/tooltip {:content "Create Shareable URL" :position b/RIGHT}
     [b/icon {:icon "share"}]]]])

(defn draw-transect-control []
  [leaflet/feature-group
   [leaflet/edit-control
    {:draw {:rectangle    false
            :circle       false
            :marker       false
            :circlemarker false
            :polygon      false
            :polyline     {:allowIntersection false
                           :metric            "metric"}}
     :ref  (fn [e]
             ;; Unfortunately, until we get react-leaflet-draw working with the latest version
             ;; of react-leaflet, we have to deal with missing our onMount event, and instead
             ;; have to make-do with waiting and then dispatching after 100ms.
             (js/setTimeout
              (fn []
                (try
                  (.. e -_toolbars -draw -_modes -polyline -handler enable)
                  (catch :default _ nil))
                (try
                  (.. e -_map (once "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable])))
                  (catch :default _ nil))
                (try
                  (.. e -_map (once "draw:created" #(re-frame/dispatch [:transect/query (-> % (.. -layer toGeoJSON) (js->clj :keywordize-keys true))])))
                  (catch :default _ nil)))
              100))}]])

(defn draw-region-control []
  [leaflet/feature-group
   [leaflet/edit-control
    {:draw {:rectangle    true
            :circle       false
            :marker       false
            :circlemarker false
            :polygon      false
            :polyline     false}
     :ref  (fn [e]
             ;; Unfortunately, until we get react-leaflet-draw working with the latest version
             ;; of react-leaflet, we have to deal with missing our onMount event, and instead
             ;; have to make-do with waiting and then dispatching after 100ms.
             (js/setTimeout
              (fn []
                (try
                  (.. e -_toolbars -draw -_modes -rectangle -handler enable)
                  (catch :default _ nil))
                (try
                  (.. e -_map (once "draw:drawstop" #(re-frame/dispatch [:map.layer.selection/disable])))
                  (catch :default _ nil))
                (try
                  (.. e -_map (once "draw:created" #(re-frame/dispatch [:map.layer.selection/finalise (-> % (.. -layer getBounds) bounds->map)])))
                  (catch :default _ nil)))
              100))}]])

(defn- control-block-child [{:keys [on-click tooltip icon]}]
  [b/tooltip {:content tooltip :position b/RIGHT}
   [:a {:on-click on-click}
    [b/icon {:icon icon :size 18}]]])

(defn- print-control []
  [:div.print-control
   [:a
    {:on-click #(-> "CurrentSize" js/document.getElementsByClassName first .click)
     :title    "Current Size"}
    [b/icon {:icon "media" :size 18}]]
   [:div.options
    [:a
     {:on-click #(-> "A4Landscape page" js/document.getElementsByClassName first .click)
      :title    "A4 Landscape"}
     [b/icon {:icon "document" :size 18 :style {:transform "rotate(-90deg)"}}]]
    [:a
     {:on-click #(-> "A4Portrait page" js/document.getElementsByClassName first .click)
      :title    "A4 Portrait"}
     [b/icon {:icon "document" :size 18}]]]])

(defn- transect-control [{:keys [drawing? query] :as _transect-info}]
  (let [[tooltip icon dispatch]
        (cond
          drawing? ["Cancel Measurement" "undo"   :transect.draw/disable]
          query    ["Clear Measurement"  "eraser" :transect.draw/clear]
          :else    ["Transect/Measure"   "edit"   :transect.draw/enable])]
    [control-block-child
     {:on-click #(re-frame/dispatch [dispatch])
      :tooltip  tooltip
      :icon     icon}]))

(defn- region-control [{:keys [selecting? region] :as _region-info}]
  (let [[tooltip icon dispatch]
        (cond
          selecting? ["Cancel Selecting" "undo"   :map.layer.selection/disable]
          region     ["Clear Selection"  "eraser" :map.layer.selection/clear]
          :else      ["Select Region"    "widget" :map.layer.selection/enable])]
    [control-block-child
     {:on-click #(re-frame/dispatch [dispatch])
      :tooltip  tooltip
      :icon     icon}]))

(defn- control-block [{:keys [transect-info region-info]}]
  [leaflet/custom-control {:position "topleft" :container {:className "leaflet-bar leaflet-control-block"}}
   
   [print-control]

   [control-block-child
    {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
     :tooltip  "Search all layers"
     :icon     "search"}]

   [transect-control transect-info]
   [region-control region-info]

   [control-block-child
    {:on-click #(re-frame/dispatch [:create-save-state])
     :tooltip  "Create Shareable URL"
     :icon     "share"}]])

(defn- element-dimensions [element]
  {:x (.-offsetWidth element) :y (.-offsetHeight element)})

(defn- popup-dimensions [element]
  (->
   (element-dimensions element)
   (update :x + (* 2 30))   ; add horizontal padding
   (update :y + (* 2 27)))) ; add vertical padding

(defn popup-contents [{:keys [status responses]}]
  (case status
    :feature-info/waiting        [b/non-ideal-state
                                  {:icon (r/as-element [b/spinner {:intent "success"}])}]
    
    :feature-info/none-queryable [b/non-ideal-state
                                  {:title       "Invalid Info"
                                   :description "Could not query the external data provider"
                                   :icon        "warning-sign"
                                   :ref         #(when % (re-frame/dispatch [:map/pan-to-popup {:x 305 :y 210}]))}] ; hardcoded popup contents size because we can't get the size of react elements
    
    :feature-info/error          [b/non-ideal-state
                                  {:title "Server Error"
                                   :icon  "error"
                                   :ref   #(when % (re-frame/dispatch [:map/pan-to-popup {:x 305 :y 210}]))}] ; hardcoded popup contents size because we can't get the size of react elements
    
    ;; Default; we have actual content:
    [:div
     {:ref
      (fn [element]
        (when element
          (set! (.-innerHTML element) nil)
          (doseq [{:keys [_body _style] :as response} responses]
            (.appendChild element (create-shadow-dom-element response)))
          (re-frame/dispatch [:map/pan-to-popup (popup-dimensions element)])))}]))

(defn popup [{:keys [has-info? responses location status show?] :as _feature-info}]
  (when (and show? has-info?)
    ;; Key forces creation of new node; otherwise it's closed but not reopened with new content:
    ^{:key (str location status)}
    [leaflet/popup
     {:position location
      :max-width "100%"
      :auto-pan false
      :class (when (= status :feature-info/waiting) "waiting")}

     ^{:key (str status responses)} [popup-contents {:status status :responses responses}]]))

(defn distance-tooltip [{:keys [distance] {:keys [x y]} :mouse-pos}]
  [:div.leaflet-draw-tooltip.distance-tooltip
   {:style {:visibility "inherit"
            :transform  (str "translate3d(" x "px, " y "px, 0px)")
            :z-index    700}}
   (if (> distance 1000)
     (str (format-number (/ distance 1000) 2) "km")
     (str (format-number distance 0) "m"))])

(defmulti layer-component (comp :layer_type :layer))

(defmethod layer-component :wms
  [{:keys [boundary-filter layer-opacities] {:keys [server_url layer_name style] :as layer} :layer}]
  [leaflet/wms-layer
   (merge
    {:url              server_url
     :layers           layer_name
     :eventHandlers
     {:loading       on-load-start
      :tileloadstart on-tile-load-start
      :tileerror     on-tile-error
      :load          on-load-end} ; sometimes results in tile query errors: https://github.com/PaulLeCam/react-leaflet/issues/626
     :transparent      true
     :opacity          (/ (layer-opacities layer) 100)
     :tiled            true
     :format           "image/png"}
    (when style {:styles style})
    (boundary-filter layer))])

(defmethod layer-component :tile
  [{:keys [layer-opacities] {:keys [server_url] :as layer} :layer}]
  [leaflet/tile-layer
   {:url              server_url
    :eventHandlers
    {:loading       on-load-start
     :tileloadstart on-tile-load-start
     :tileerror     on-tile-error
     :load          on-load-end} ; sometimes results in tile query errors: https://github.com/PaulLeCam/react-leaflet/issues/626
    :transparent      true
    :opacity          (/ (layer-opacities layer) 100)
    :tiled            true
    :format           "image/png"}])

(defmethod layer-component :feature
  [{{:keys [server_url] :as layer} :layer}]
  [leaflet/feature-layer
   {:url              server_url
    :eventHandlers
    {:loading       #(re-frame/dispatch [:map.layer/load-start layer])
     :tileloadstart #(re-frame/dispatch [:map.layer/tile-load-start layer])
     :tileerror     #(re-frame/dispatch [:map.layer/load-error layer])
     :load          #(re-frame/dispatch [:map.layer/load-finished layer])}}]) ; sometimes results in tile query errors: https://github.com/PaulLeCam/react-leaflet/issues/626

(defmethod layer-component :wms-non-tiled
  [{:keys [boundary-filter layer-opacities] {:keys [server_url layer_name style] :as layer} :layer}]
  [leaflet/non-tiled-layer
   (merge
    {:url              server_url
     :layers           layer_name
     :eventHandlers
     {:loading       #(re-frame/dispatch [:map.layer/load-start layer])
      :tileloadstart #(re-frame/dispatch [:map.layer/tile-load-start layer])
      :tileerror     #(re-frame/dispatch [:map.layer/load-error layer])
      :load          #(re-frame/dispatch [:map.layer/load-finished layer])}
     :transparent      true
     :opacity          (/ (layer-opacities layer) 100)
     :tiled            true
     :format           "image/png"}
    (when style {:styles style})
    (boundary-filter layer))])

(defmulti basemap-layer-component :layer_type)

(defmethod basemap-layer-component :tile
 [{:keys [server_url attribution]}]
 [leaflet/tile-layer {:url server_url :attribution attribution}])

(defmethod basemap-layer-component :vector
  [{:keys [server_url attribution]}]
  [leaflet/vector-tile-layer {:url server_url :attribution attribution}])

(defn map-component [& children]
  (let [{:keys [center zoom bounds]}                  @(re-frame/subscribe [:map/props])
        {:keys [layer-opacities visible-layers]}      @(re-frame/subscribe [:map/layers])
        {:keys [grouped-base-layers active-base-layer]} @(re-frame/subscribe [:map/base-layers])
        feature-info                                  @(re-frame/subscribe [:map.feature/info])
        {:keys [query mouse-loc distance] :as transect-info} @(re-frame/subscribe [:transect/info])
        {:keys [region] :as region-info}              @(re-frame/subscribe [:map.layer.selection/info])
        download-info                                 @(re-frame/subscribe [:download/info])
        boundary-filter                               @(re-frame/subscribe [:sok/boundary-layer-filter])
        mouse-pos                                     @(re-frame/subscribe [:ui/mouse-pos])
        {national-layer :displayed-layer}             @(re-frame/subscribe [:map/national-layer])
        {national-layer-opacity :opacity}             @(re-frame/subscribe [:map.national-layer/state])]
    (into
     [:div.map-wrapper
      [download-component download-info]
      [leaflet/map-container
       (merge
        {:id                   "map"
         :crs                  leaflet/crs-epsg3857
         :use-fly-to           false
         :center               center
         :zoom                 zoom
         :zoomControl          false
         :zoominfoControl      true
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
          [basemap-layer-component (first grouped-base-layers)]])

       ;; Basemap selection:
       [leaflet/layers-control {:position "topright" :auto-z-index false}
        (for [{:keys [id name] :as base-layer} grouped-base-layers]
          ^{:key id}
          [leaflet/layers-control-basemap {:name name :checked (= base-layer active-base-layer)}
           [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index 0}}
            [basemap-layer-component base-layer]]])]
       
       ;; Additional basemap layers
       (map-indexed
        (fn [i {:keys [id] :as base-layer}]
          ^{:key (str id (+ i 1))}
          [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index (+ i 1)}}
           [basemap-layer-component base-layer]])
        (:layers active-base-layer))
       
       ;; Catalogue layers
       (map-indexed
        (fn [i {:keys [id] :as layer}]
          ;; While it's not efficient, we give every layer it's own pane to simplify the
          ;; code.
          ;; Panes are given a name based on a uuid and time because if a pane is given the
          ;; same name as a previously existing pane leaflet complains about a new pane being
          ;; made with the same name as an existing pane (causing leaflet to no longer work).
          ^{:key (str id (boundary-filter layer) (+ i 1 (count (:layers active-base-layer))))}
          [leaflet/pane {:name (str (random-uuid) (.now js/Date)) :style {:z-index (+ i 1 (count (:layers active-base-layer)))}}
           [layer-component
            {:layer           layer
             :boundary-filter boundary-filter
             :layer-opacities
             (if (= layer national-layer)
               #(identity national-layer-opacity) layer-opacities)}]])
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
         [draw-transect-control])
       (when (:selecting? region-info)
         [draw-region-control]) 
       
       ;; This control needs to exist so we can trigger its functions programmatically in
       ;; the control-block element.
       [leaflet/print-control
        {:position   "topleft" :title "Export as PNG"
         :export-only true
         :size-modes ["Current", "A4Landscape", "A4Portrait"]}]

       [control-block {:transect-info transect-info :region-info region-info}]

       [leaflet/scale-control]

       [leaflet/coordinates-control
        {:decimals 2
         :labelTemplateLat "{y}"
         :labelTemplateLng "{x}"
         :useLatLngOrder   true
         :enableUserInput  false}]

       (when (and mouse-pos distance) [distance-tooltip {:mouse-pos mouse-pos :distance distance}])

       [popup feature-info]]]
     
     children)))
