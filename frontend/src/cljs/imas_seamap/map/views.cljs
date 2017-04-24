(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.utils :refer [select-values]]
            [oops.core :refer [ocall]]
            [debux.cs.core :refer-macros [dbg]]))

(def tile-layer    (r/adapt-react-class js/ReactLeaflet.TileLayer))
(def wms-layer     (r/adapt-react-class js/ReactLeaflet.WMSTileLayer))
(def geojson-layer (r/adapt-react-class js/ReactLeaflet.GeoJSON))
(def leaflet-map   (r/adapt-react-class js/ReactLeaflet.Map))
(def marker        (r/adapt-react-class js/ReactLeaflet.Marker))
(def popup         (r/adapt-react-class js/ReactLeaflet.Popup))
(def feature-group (r/adapt-react-class js/ReactLeaflet.FeatureGroup))
(def edit-control  (r/adapt-react-class js/ReactLeaflet.EditControl))
(def circle-marker (r/adapt-react-class js/ReactLeaflet.CircleMarker))

(defn bounds->map [bounds]
  {:north (.. bounds getNorth)
   :south (.. bounds getSouth)
   :east  (.. bounds getEast)
   :west  (.. bounds getWest)})

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

(defn leaflet-props [e]
  (let [m (.. e -target)]
    {:zoom (.. m getZoom)
     :center (-> m .getCenter latlng->vec)
     :bounds (-> m .getBounds bounds->map)}))

(def ^:private *category-ordering*
  (into {} (map vector [:bathymetry :habitat :imagery :third-party] (range))))

(defn sort-layers
  "Return layers in an order suitable for presentation (essentially,
  bathymetry at the bottom, third-party on top)"
  [layers]
  (let [comparator #(< (get *category-ordering* %1 99) (get *category-ordering* %2 99))]
    (sort-by :category comparator layers)))

(defn map-component []
  (let [{:keys [center zoom bounds controls active-layers]} @(re-frame/subscribe [:map/props])
        {:keys [drawing? query mouse-loc]} @(re-frame/subscribe [:transect/info])
        base-layer-bluemarble [wms-layer {:url "http://demo.opengeo.org/geoserver/ows?"
                                          :layers "nasa:bluemarble"
                                          :attribution "Made by Condense / Images by NASA"}]
        ;; NOTE: this would require  :crs js/L.CRS.EPSG4326 in the leaflet-map props to render properly:
        base-layer-bathy [wms-layer {:url "http://geoserver-static.aodn.org.au/geoserver/baselayers/wms?"
                                     :layers "baselayers:default_bathy"
                                     :transparent true :format "image/png"}]
        base-layer-osm [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]]
    [leaflet-map (merge
                  {:id "map" :use-fly-to true
                   :center center :zoom zoom
                   :on-zoomend #(re-frame/dispatch [:map/view-updated (leaflet-props %)])
                   :on-dragend #(re-frame/dispatch [:map/view-updated (leaflet-props %)])}
                  (when (seq bounds) {:bounds (map->bounds bounds)}))
     base-layer-osm
     (for [{:keys [server_url layer_name] :as layer} (sort-layers active-layers)]
       ^{:key (str server_url layer_name)}
       [wms-layer {:url server_url :layers layer_name
                   :transparent true :format "image/png"}])
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
        [edit-control {:draw {:rectangle false
                              :circle    false
                              :marker    false
                              :polygon   false
                              :polyline  {:allowIntersection false}}
                       :on-mounted (fn [e]
                                     (ocall e "_toolbars.draw._modes.polyline.handler.enable")
                                     (ocall e "_map.once" "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable])))
                       :on-created #(re-frame/dispatch [:transect/query (-> % .-layer .toGeoJSON (js->clj :keywordize-keys true))])}]])]))
