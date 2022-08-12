;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [copy-text select-values handler-dispatch create-shadow-dom-element] :include-macros true]
            [imas-seamap.map.utils :refer [bounds->geojson download-type->str]]
            [imas-seamap.interop.leaflet :as leaflet]
            [imas-seamap.components :as components]
            ["/leaflet-zoominfo/L.Control.Zoominfo"]
            ["/leaflet-scalefactor/leaflet.scalefactor"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn bounds->map [bounds]
  {:north (. bounds getNorth)
   :south (. bounds getSouth)
   :east  (. bounds getEast)
   :west  (. bounds getWest)})

(defn map->bounds [{:keys [west south east north] :as _bounds}]
  [[south west]
   [north east]])

(defn point->latlng [[x y]] {:lat y :lng x})

(defn point-distance [[x1 y1 :as _p1] [x2 y2 :as _p2]]
  (let [xd (- x2 x1) yd (- y2 y1)]
    (js/Math.sqrt (+ (* xd xd) (* yd yd)))))

(defn latlng->vec [ll]
  (-> ll
      js->clj
      (select-values ["lat" "lng"])))

(defn mouseevent->coords [e]
  (merge
   (-> e
       ;; Note need to round; fractional offsets (eg as in wordpress
       ;; navbar) cause fractional x/y which causes geoserver to
       ;; return errors in GetFeatureInfo
       (.. -containerPoint round)
       (js->clj :keywordize-keys true)
       (select-keys [:x :y]))
   (-> e
       (. -latlng)
       (js->clj :keywordize-keys true)
       (select-keys [:lat :lng]))))

(defn leaflet-props [e]
  (let [m (. e -target)]
    {:zoom   (. m getZoom)
     :size   (-> m (. getSize) (js->clj :keywordize-keys true) (select-keys [:x :y]))
     :center (-> m (. getCenter) latlng->vec)
     :bounds (-> m (. getBounds) bounds->map)}))

(defn on-map-clicked
  "Initial handler for map click events; intent is these only apply to image layers"
  [e]
  (re-frame/dispatch [:map/clicked (leaflet-props e) (mouseevent->coords e)]))

(defn on-popup-closed [_e]
  (re-frame/dispatch [:map/popup-closed]))

(defn on-map-view-changed [e]
  (re-frame/dispatch [:map/view-updated (leaflet-props e)]))

(defn on-base-layer-changed [e]
  ;; We only have easy access to the name, but we require that to be unique:
  (re-frame/dispatch [:map/base-layer-changed
                      (-> e (js->clj :keywordize-keys true) :name)]))

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
  [leaflet/custom-control {:position "topleft" :class "leaflet-bar"}
   ;; The copy-text has to be here rather than in a handler, because
   ;; Firefox won't do execCommand('copy') outside of a "short-lived
   ;; event handler"
   [:a {:on-click #(do (copy-text js/location.href)
                       (re-frame/dispatch  [:create-save-state]))}
    [b/tooltip {:content "Create Shareable URL" :position b/RIGHT}
     [b/icon {:icon "share"}]]]])

(defn omnisearch-control [_props]
  [leaflet/custom-control {:position "topleft" :class "leaflet-bar"}
   [:a {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])}
    [b/tooltip {:content "Search all layers" :position b/RIGHT}
     [b/icon {:icon "search"}]]]])

(defn transect-control [{:keys [drawing? query] :as _transect-info}]
  (let [[text icon dispatch] (cond
                               drawing? ["Cancel Transect" "undo"   :transect.draw/disable]
                               query    ["Clear Transect"  "eraser" :transect.draw/clear]
                               :else    ["Draw Transect"   "edit"   :transect.draw/enable])]
    [leaflet/custom-control {:position "topleft" :class "leaflet-bar"}
     [:a {:on-click #(re-frame/dispatch  [dispatch])}
      [b/tooltip {:content text :position b/RIGHT}
       [b/icon {:icon icon}]]]]))

(defn draw-transect-control []
  [leaflet/feature-group
   [leaflet/edit-control {:draw       {:rectangle    false
                                       :circle       false
                                       :marker       false
                                       :circlemarker false
                                       :polygon      false
                                       :polyline     {:allowIntersection false
                                                      :metric            "metric"}}
                          :on-mounted (fn [e]
                                        (.. e -_toolbars -draw -_modes -polyline -handler enable)
                                        (.. e -_map (once "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable]))))
                          :on-created #(re-frame/dispatch [:transect/query (-> % (.. -layer toGeoJSON) (js->clj :keywordize-keys true))])}]])

(defn region-control [{:keys [selecting? region] :as _region-info}]
  (let [[text icon dispatch] (cond
                               selecting? ["Cancel Selecting" "undo"   :map.layer.selection/disable]
                               region     ["Clear Selection"  "eraser" :map.layer.selection/clear]
                               :else      ["Select Region"    "widget" :map.layer.selection/enable])]
    [leaflet/custom-control {:position "topleft" :class "leaflet-bar"}
     [:a {:on-click #(re-frame/dispatch  [dispatch])}
      [b/tooltip {:content text :position b/RIGHT}
       [b/icon {:icon icon}]]]]))

(defn draw-region-control []
  [leaflet/feature-group
   [leaflet/edit-control {:draw       {:rectangle    true
                                       :circle       false
                                       :marker       false
                                       :circlemarker false
                                       :polygon      false
                                       :polyline     false}
                          :on-mounted (fn [e]
                                        (.. e -_toolbars -draw -_modes -rectangle -handler enable)
                                        (.. e -_map (once "draw:drawstop" #(re-frame/dispatch [:map.layer.selection/disable]))))
                          :on-created #(re-frame/dispatch [:map.layer.selection/finalise
                                                           (-> % (.. -layer getBounds) bounds->map)])}]])

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

(defn popup [{:keys [has-info? responses location status] :as _feature-info}]
  (when (and has-info? (not= status :feature-info/empty))
    
    ;; Key forces creation of new node; otherwise it's closed but not reopened with new content:
    ^{:key (str location)}
    [leaflet/popup
     {:position location
      :max-width "100%"
      :auto-pan false
      :class (when (= status :feature-info/waiting) "waiting")}

     ^{:key (str status responses)} [popup-contents {:status status :responses responses}]]))

(defn- add-raw-handler-once [js-obj event-name handler]
  (when-not (. js-obj listens event-name)
    (. js-obj on event-name handler)))

(defn map-component [& children]
  (let [{:keys [center zoom bounds]}                  @(re-frame/subscribe [:map/props])
        {:keys [layer-opacities visible-layers]}      @(re-frame/subscribe [:map/layers])
        {:keys [grouped-base-layers active-base-layer]} @(re-frame/subscribe [:map/base-layers])
        feature-info                                  @(re-frame/subscribe [:map.feature/info])
        {:keys [drawing? query mouse-loc] :as transect-info} @(re-frame/subscribe [:transect/info])
        {:keys [selecting? region] :as region-info}   @(re-frame/subscribe [:map.layer.selection/info])
        download-info                                 @(re-frame/subscribe [:download/info])
        boundary-filter                               @(re-frame/subscribe [:sok/boundary-layer-filter])
        loading?                                      @(re-frame/subscribe [:app/loading?])]
    (into
     [:div.map-wrapper
      [download-component download-info]
      [leaflet/leaflet-map
       (merge
        {:id                   "map"
         :class                "sidebar-map"
        ;; :crs                  leaflet/crs-epsg4326
         :crs                  leaflet/crs-epsg3857
         :use-fly-to           false ; Trial solution to ISA-171; doesn't actually appear to affect fly-to movement on the map, but does allow for minute movements between center points on the map
         :center               center
         :zoom                 zoom
         :zoomControl          false
         :zoominfoControl      true
         :scaleFactor          true
         :keyboard             false ; handled externally
         :on-zoomend           on-map-view-changed
         :on-moveend           on-map-view-changed
         :when-ready           on-map-view-changed
         :on-baselayerchange   on-base-layer-changed
         :double-click-zoom    false
         :ref                  (fn [map]
                                 (when map
                                   (add-raw-handler-once (. map -leafletElement) "easyPrint-start"
                                                         #(re-frame/dispatch [:ui/show-loading "Preparing Image..."]))
                                   (add-raw-handler-once (. map -leafletElement) "easyPrint-finished"
                                                         #(re-frame/dispatch [:ui/hide-loading]))))
         :on-click             on-map-clicked
         :close-popup-on-click false ; We'll handle that ourselves
         :on-popupclose        on-popup-closed}
        (when (seq bounds) {:bounds (map->bounds bounds)}))

      ;; Basemap selection:
       [leaflet/layers-control {:position "topright" :auto-z-index false}
        (for [{:keys [id name server_url attribution] :as base-layer} grouped-base-layers]
          ^{:key id}
          [leaflet/layers-control-basemap {:name name :checked (= base-layer active-base-layer)}
           [leaflet/tile-layer {:url server_url :attribution attribution}]])]

      ;; We enforce the layer ordering by an incrementing z-index (the
      ;; order of this list is otherwise ignored, as the underlying
      ;; React -> Leaflet translation just does add/removeLayer, which
      ;; then orders in the map by update not by list):
       (map-indexed
        (fn [i {:keys [server_url layer_name style id] :as layer}]
          ^{:key (str id (boundary-filter layer))}
          [leaflet/wms-layer
           (merge
            {:url              server_url
             :layers           layer_name
             :z-index          (+ 2 i) ; base layers is zindex 1, start content at 2
             :on-loading       on-load-start
             :on-tileloadstart on-tile-load-start
             :on-tileerror     on-tile-error
             :on-load          on-load-end
             :transparent      true
             :opacity          (/ (layer-opacities layer) 100)
             :tiled            true
             :format           "image/png"}
            (when style {:styles style})
            (boundary-filter layer))])
        (concat (:layers active-base-layer) visible-layers))
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

       ;; Top-left controls have a key that changes based on state for an important
       ;; reason: when new controls are added to the list they are added to the bottom of
       ;; the controls list.
       ;; New controls being added to the bottom is an issue in our case because we want
       ;; to be able to swap out the buttons that start drawing a transect/region with
       ;; the leaflet draw controls; when the controls are swapped out, they are added to
       ;; the bottom of the list rather than the location of the control we are replacing.
       ;; To get around this issue we give every control in the list a key that changes
       ;; with state, to force React Leaflet to recognise these as new controls and
       ;; rerender them all, preserving their order!
       ;; TL;DR: having controls show up in the correct order is a pain and this fixes
       ;; that.
       ^{:key (str "omnisearch-control" transect-info region-info)}
       [omnisearch-control]

       (if (:drawing? transect-info)
         ^{:key (str "transect-control" transect-info region-info)}
         [draw-transect-control]
         ^{:key (str "transect-control" transect-info region-info)}
         [transect-control transect-info])

       (if (:selecting? region-info)
         ^{:key (str "region-control" transect-info region-info)}
         [draw-region-control]
         ^{:key (str "region-control" transect-info region-info)}
         [region-control region-info])

       ^{:key (str "share-control" transect-info region-info)}
       [share-control]

       ^{:key (str "print-control" transect-info region-info)}
       [leaflet/print-control {:position   "topleft" :title "Export as PNG"
                               :export-only true
                               :size-modes ["Current", "A4Landscape", "A4Portrait"]}]

       [leaflet/scale-control]

       [leaflet/coordinates-control
        {:position "bottomright"
         :style nil}]

       [popup feature-info]]]
     
     children)))
