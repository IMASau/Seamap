(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.utils :refer [select-values]]
            [imas-seamap.map.utils :refer [sort-layers]]
            [oops.core :refer [ocall oget]]
            [debux.cs.core :refer-macros [dbg]]))

(def tile-layer    (r/adapt-react-class (oget js/window "ReactLeaflet.TileLayer")))
(def wms-layer     (r/adapt-react-class (oget js/window "ReactLeaflet.WMSTileLayer")))
(def geojson-layer (r/adapt-react-class (oget js/window "ReactLeaflet.GeoJSON")))
(def leaflet-map   (r/adapt-react-class (oget js/window "ReactLeaflet.Map")))
(def marker        (r/adapt-react-class (oget js/window "ReactLeaflet.Marker")))
(def popup         (r/adapt-react-class (oget js/window "ReactLeaflet.Popup")))
(def feature-group (r/adapt-react-class (oget js/window "ReactLeaflet.FeatureGroup")))
(def edit-control  (r/adapt-react-class (oget js/window "ReactLeaflet.EditControl")))
(def circle-marker (r/adapt-react-class (oget js/window "ReactLeaflet.CircleMarker")))

(def sidebar       (r/adapt-react-class (oget js/window "ReactSidebar.Sidebar")))
(def sidebar-tab   (r/adapt-react-class (oget js/window "ReactSidebar.Tab")))

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
       (oget "containerPoint")
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

(defn map-component [sidebar-content]
  (let [{:keys [center zoom bounds controls active-layers]} @(re-frame/subscribe [:map/props])
        {:keys [has-info? info-body location] :as fi} @(re-frame/subscribe [:map.feature/info])
        {:keys [drawing? query mouse-loc]} @(re-frame/subscribe [:transect/info])
        layer-priorities @(re-frame/subscribe [:map.layers/priorities])
        logic-type @(re-frame/subscribe [:map.layers/logic])
        {:keys [collapsed selected] :as sidebar-state} @(re-frame/subscribe [:ui/sidebar])
        base-layer-osm [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]]
    [:div.map-wrapper
     [leaflet-map (merge
                   {:id            "map"
                    :use-fly-to    true
                    :center        center
                    :zoom          zoom
                    :on-zoomend    on-map-view-changed
                    :on-moveend    on-map-view-changed
                    :when-ready    on-map-view-changed
                    :on-click      on-map-clicked
                    :on-popupclose on-popup-closed}
                   (when (seq bounds) {:bounds (map->bounds bounds)}))

      base-layer-osm
      ;; We enforce the layer ordering by an incrementing z-index (the
      ;; order of this list is otherwise ignored, as the underlying
      ;; React -> Leaflet translation just does add/removeLayer, which
      ;; then orders in the map by update not by list):
      (map-indexed
       (fn [i {:keys [server_url layer_name] :as layer}]
         ^{:key (str server_url layer_name)}
         [wms-layer {:url          server_url :layers layer_name :z-index (inc i)
                     :on-loading   on-load-start
                     :on-tileerror on-tile-error
                     :on-load      on-load-end
                     :transparent  true       :format "image/png"}])
       (sort-layers active-layers layer-priorities logic-type))
      (when query
        [geojson-layer {:data (clj->js query)}])
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
                                     :polyline  {:allowIntersection false}}
                        :on-mounted (fn [e]
                                      (ocall e "_toolbars.draw._modes.polyline.handler.enable")
                                      (ocall e "_map.once" "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable])))
                        :on-created #(re-frame/dispatch [:transect/query (-> % (ocall "layer.toGeoJSON") (js->clj :keywordize-keys true))])}]])
      (when has-info?
        ;; Key forces creation of new node; otherwise it's closed but not reopened with new content:
        ^{:key (str location)}
        [popup {:position location :max-width 600}
         [:div {:dangerouslySetInnerHTML {:__html info-body}}]])]

     [sidebar {:id        "floating-sidebar"
               :selected  selected
               :collapsed collapsed
               :closeIcon (r/as-element [:span.pt-icon-standard.pt-icon-caret-left])
               :on-close  #(re-frame/dispatch [:ui.sidebar/close])
               :on-open   #(re-frame/dispatch [:ui.sidebar/open %])}
      [sidebar-tab {:header "Seamap Australia"
                    :icon   (r/as-element [:span.pt-icon-standard.pt-icon-menu])
                    :id     "tab-home"}
       sidebar-content]]]))
