;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [select-values] :refer-macros [handler-dispatch]]
            [imas-seamap.map.utils :refer [sort-layers bounds->geojson download-type->str]]
            [oops.core :refer [ocall oget gget]]
            [debux.cs.core :refer-macros [dbg]]))

(def tile-layer    (r/adapt-react-class (gget "ReactLeaflet.TileLayer")))
(def wms-layer     (r/adapt-react-class (gget "ReactLeaflet.WMSTileLayer")))
(def geojson-layer (r/adapt-react-class (gget "ReactLeaflet.GeoJSON")))
(def leaflet-map   (r/adapt-react-class (gget "ReactLeaflet.Map")))
(def marker        (r/adapt-react-class (gget "ReactLeaflet.Marker")))
(def popup         (r/adapt-react-class (gget "ReactLeaflet.Popup")))
(def feature-group (r/adapt-react-class (gget "ReactLeaflet.FeatureGroup")))
(def edit-control  (r/adapt-react-class (gget "ReactLeaflet.EditControl")))
(def circle-marker (r/adapt-react-class (gget "ReactLeaflet.CircleMarker")))

(defn bounds->map [bounds]
  {:north (ocall bounds :getNorth)
   :south (ocall bounds :getSouth)
   :east  (ocall bounds :getEast)
   :west  (ocall bounds :getWest)})

(defn map->bounds [{:keys [west south east north] :as bounds}]
  [[south west]
   [north east]])

(defn point->latlng [[x y]] {:lat y :lng x})

(defn point-distance [[x1 y1 :as p1] [x2 y2 :as p2]]
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
       (ocall "containerPoint.round")
       (js->clj :keywordize-keys true)
       (select-keys [:x :y]))
   (-> e
       (oget "latlng")
       (js->clj :keywordize-keys true)
       (select-keys [:lat :lng]))))

(defn leaflet-props [e]
  (let [m (oget e :target)]
    {:zoom   (ocall m :getZoom)
     :size   (-> m (ocall :getSize) (js->clj :keywordize-keys true) (select-keys [:x :y]))
     :center (-> m (ocall :getCenter) latlng->vec)
     :bounds (-> m (ocall :getBounds) bounds->map)}))

(defn on-map-clicked
  "Initial handler for map click events; intent is these only apply to image layers"
  [e]
  (re-frame/dispatch [:map/clicked (leaflet-props e) (mouseevent->coords e)]))

(defn on-popup-closed [e]
  (re-frame/dispatch [:map/popup-closed]))

(defn on-map-view-changed [e]
  (re-frame/dispatch [:map/view-updated (leaflet-props e)]))

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
  (let [target (oget e :target)]
    [(oget target :_url)
     (oget target [:options :layers])]))
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

(defn download-component [{:keys [display-link link bbox download-type] :as download-info}]
  (let [type-str (download-type->str download-type)]
    [b/dialogue {:is-open   display-link
                 :title     (str "Download " type-str)
                 :icon-name "import"
                 :on-close  (handler-dispatch [:ui.download/close-dialogue])}
     [:div.pt-dialog-body
      [:p [:a {:href link :target "_blank"}
           "Click here to download"
           (when bbox " region")
           " as "
           type-str]]]
     [:div.pt-dialog-footer
      [:div.pt-dialog-footer-actions
       [b/button {:text     "Done"
                  :intent   b/*intent-primary*
                  :on-click (handler-dispatch [:ui.download/close-dialogue])}]]]]))

(defn popup-component [{:keys [status info-body]}]
  (case status
    :feature-info/waiting [b/non-ideal-state
                               {:visual (r/as-element [b/spinner {:intent "success"}])}]
    :feature-info/empty   [b/non-ideal-state
                           {:title  "No Results"
                            :visual "warning-sign"}]
    :feature-info/error   [b/non-ideal-state
                           {:title  "Server Error"
                            :visual "error"}]
    ;; Default; we have actual content:
    [:div {:dangerouslySetInnerHTML {:__html info-body}}]))

(defn map-component [sidebar]
  (let [{:keys [center zoom bounds controls active-layers]} @(re-frame/subscribe [:map/props])
        {:keys [has-info? info-body location] :as fi}       @(re-frame/subscribe [:map.feature/info])
        {:keys [drawing? query mouse-loc]}                  @(re-frame/subscribe [:transect/info])
        {:keys [selecting? region]}                         @(re-frame/subscribe [:map.layer.selection/info])
        download-info                                       @(re-frame/subscribe [:download/info])
        layer-priorities                                    @(re-frame/subscribe [:map.layers/priorities])
        layer-params                                        @(re-frame/subscribe [:map.layers/params])
        logic-type                                          @(re-frame/subscribe [:map.layers/logic])
        ;; base-layer-terrestris                               [wms-layer {:url "http://ows.terrestris.de/osm/service" :layers "OSM-WMS"}]
        base-layer-eoc                                      [tile-layer {:url (str "https://tiles.geoservice.dlr.de/service/wmts?"
                                                                                   "Service=WMTS&Request=GetTile&"
                                                                                   "Version=1.0.0&Format=image/png&"
                                                                                   "layer=eoc:basemap&tilematrixset=EPSG:4326&"
                                                                                   "TileMatrix=EPSG:4326:{z}&TileCol={x}&TileRow={y}")
                                                                         :attribution (str "Base Data &copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors "
                                                                                           "| Rendering &copy; <a href=\"http://www.dlr.de/eoc/\">DLR/EOC</a>")}]
        base-layer-eoc-overlay                              [tile-layer {:url (str "https://tiles.geoservice.dlr.de/service/wmts?"
                                                                                   "Service=WMTS&Request=GetTile&"
                                                                                   "Version=1.0.0&Format=image/png&"
                                                                                   "layer=eoc:baseoverlay&tilematrixset=EPSG:4326&"
                                                                                   "TileMatrix=EPSG:4326:{z}&TileCol={x}&TileRow={y}")}]
        ]
    [:div.map-wrapper
     sidebar
     [download-component download-info]

     [leaflet-map (merge
                   {:id                   "map"
                    :class-name           "sidebar-map"
                    :crs                  (gget "L.CRS.EPSG4326")
                    :use-fly-to           true
                    :center               center
                    :zoom                 zoom
                    :keyboard             false ; handled externally
                    :on-zoomend           on-map-view-changed
                    :on-moveend           on-map-view-changed
                    :when-ready           on-map-view-changed
                    :on-click             on-map-clicked
                    :close-popup-on-click false ; We'll handle that ourselves
                    :on-popupclose        on-popup-closed}
                   (when (seq bounds) {:bounds (map->bounds bounds)}))

      base-layer-eoc
      base-layer-eoc-overlay

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
           [wms-layer (merge
                       {:url          server_url
                        :layers       layer_name
                        :z-index      (inc i)
                        :on-loading   on-load-start
                        :on-tileerror on-tile-error
                        :on-load      on-load-end
                        :transparent  true
                        :opacity      1
                        :format       "image/png"}
                       extra-params)]))
       (sort-layers active-layers layer-priorities logic-type))
      (when query
        [geojson-layer {:data (clj->js query)}])
      (when region
        [geojson-layer {:data (clj->js (bounds->geojson region))}])
      (when (and query mouse-loc)
        [circle-marker {:center      mouse-loc
                        :radius      3
                        :fillColor   "#3f8ffa"
                        :color       "#3f8ffa"
                        :opacity     1
                        :fillOpacity 1}])
      (when drawing?
        [feature-group
         [edit-control {:draw       {:rectangle false
                                     :circle    false
                                     :marker    false
                                     :polygon   false
                                     :polyline  {:allowIntersection false
                                                 :metric            "metric"}}
                        :on-mounted (fn [e]
                                      (ocall e "_toolbars.draw._modes.polyline.handler.enable")
                                      (ocall e "_map.once" "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable])))
                        :on-created #(re-frame/dispatch [:transect/query (-> % (ocall "layer.toGeoJSON") (js->clj :keywordize-keys true))])}]])
      (when selecting?
        [feature-group
         [edit-control {:draw       {:rectangle true
                                     :circle    false
                                     :marker    false
                                     :polygon   false
                                     :polyline  false}
                        :on-mounted (fn [e]
                                      (ocall e "_toolbars.draw._modes.rectangle.handler.enable")
                                      (ocall e "_map.once" "draw:drawstop" #(re-frame/dispatch [:map.layer.selection/disable])))
                        :on-created #(re-frame/dispatch [:map.layer.selection/finalise
                                                         (-> % (ocall "layer.getBounds") bounds->map)])}]])
      (when has-info?
        ;; Key forces creation of new node; otherwise it's closed but not reopened with new content:
        ^{:key (str location)}
        [popup {:position location :max-width 600 :auto-pan false}
         ^{:key (or info-body (:status fi))}
         [popup-component fi]])]]))
