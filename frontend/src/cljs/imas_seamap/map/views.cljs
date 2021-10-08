;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [copy-text select-values handler-dispatch] :include-macros true]
            [imas-seamap.map.utils :refer [sort-layers bounds->geojson download-type->str]]
            [imas-seamap.interop.leaflet :as leaflet]
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
                       (re-frame/dispatch  [:copy-share-url]))}
    [b/tooltip {:content "Copy Shareable URL to clipboard" :position b/RIGHT}
     [b/icon {:icon "clipboard"}]]]])

(defn popup-component [{:keys [status info-body had-insecure?] :as _feature-popup}]
  (case status
    :feature-info/waiting        [b/non-ideal-state
                                  {:icon (r/as-element [b/spinner {:intent "success"}])}]
    :feature-info/empty          [b/non-ideal-state
                                  (merge
                                   {:title       "No Results"
                                    :description "Try clicking elsewhere or adding another layer"
                                    :icon        "warning-sign"}
                                   (when had-insecure? {:description "(Could not query all displayed external data layers)"}))]
    :feature-info/none-queryable [b/non-ideal-state
                                  {:title       "Invalid Info"
                                   :description "Could not query the external data provider"
                                   :icon        "warning-sign"}]
    :feature-info/error          [b/non-ideal-state
                                  {:title "Server Error"
                                   :icon  "error"}]
    ;; Default; we have actual content:
    [:div {:dangerouslySetInnerHTML {:__html info-body}}]))

(defn- add-raw-handler-once [js-obj event-name handler]
  (when-not (. js-obj listens event-name)
    (. js-obj on event-name handler)))

(defn map-component [sidebar]
  (let [{:keys [center zoom bounds active-layers]}    @(re-frame/subscribe [:map/props])
        {:keys [base-layers active-base-layer]}       @(re-frame/subscribe [:map/base-layers])
        {:keys [has-info? info-body location] :as fi} @(re-frame/subscribe [:map.feature/info])
        {:keys [drawing? query mouse-loc]}            @(re-frame/subscribe [:transect/info])
        {:keys [selecting? region]}                   @(re-frame/subscribe [:map.layer.selection/info])
        download-info                                 @(re-frame/subscribe [:download/info])
        layer-priorities                              @(re-frame/subscribe [:map.layers/priorities])
        layer-params                                  @(re-frame/subscribe [:map.layers/params])
        logic-type                                    @(re-frame/subscribe [:map.layers/logic])]
    [:div.map-wrapper
     sidebar
     [download-component download-info]

     [leaflet/leaflet-map
      (merge
       {:id                   "map"
        :class           "sidebar-map"
        ;; :crs                  leaflet/crs-epsg4326
        :crs                  leaflet/crs-epsg3857
        :use-fly-to           true
        :center               center
        :zoom                 zoom
        :keyboard             false ; handled externally
        :on-zoomend           on-map-view-changed
        :on-moveend           on-map-view-changed
        :when-ready           on-map-view-changed
        :on-baselayerchange   on-base-layer-changed
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
       (for [{:keys [name server_url attribution] :as base-layer} base-layers]
         ^{:key name}
         [leaflet/layers-control-basemap {:name name :checked (= base-layer active-base-layer)}
          [leaflet/tile-layer {:url server_url :attribution attribution}]])]

      ;; We enforce the layer ordering by an incrementing z-index (the
      ;; order of this list is otherwise ignored, as the underlying
      ;; React -> Leaflet translation just does add/removeLayer, which
      ;; then orders in the map by update not by list):
      (map-indexed
       (fn [i {:keys [server_url layer_name] :as layer}]
         (let [extra-params (layer-params layer)]
           ;; This extra key aspect (hash of extra params) shouldn't
           ;; be necessary anyway, and now it interferes with
           ;; download-selection (if we fade out other layers when
           ;; selecting, it triggers a reload of that layer, which
           ;; triggers draw:stop it seems)
           ^{:key (str server_url layer_name)}
           [leaflet/wms-layer (merge
                               {:url          server_url
                                :layers       layer_name
                                :z-index      (+ 2 i) ; base layers is zindex 1, start content at 2
                                :on-loading   on-load-start
                                :on-tileerror on-tile-error
                                :on-load      on-load-end
                                :transparent  true
                                :opacity      1
                                :tiled        true
                                :format       "image/png"}
                               extra-params)]))
       (sort-layers active-layers layer-priorities logic-type))
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
      (when drawing?
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
      (when selecting?
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

      [share-control]

      [leaflet/print-control {:position   "topleft" :title "Export as PNG"
                              :export-only true
                              :size-modes ["Current", "A4Landscape", "A4Portrait"]}]

      (when has-info?
        ;; Key forces creation of new node; otherwise it's closed but not reopened with new content:
        ^{:key (str location)}
        [leaflet/popup {:position location :max-width 600 :auto-pan false}
         ^{:key (or info-body (:status fi))}
         [popup-component fi]])]]))
