(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.utils :refer [select-values]]
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

(defn ->point [p] (js/L.point (clj->js p)))

(defn point->latlng [[x y]] {:lat y :lng x})

(defn point-distance [p1 p2] (.distanceTo (->point p1) (->point p2)))

(defn scale-distance [[x1 y1] [x2 y2] pct]
  [(+ x1 (* pct (- x2 x1)))
   (+ y1 (* pct (- y2 y1)))])

(defn point-along-line
  "Given a series of coordinates representing a line, and a
  percentage (assumed to be between 0 and 100), return a point as
  two-element vector that occurs at that percentage distance along the
  line."
  [coords pct]
  (let [pairs (map vector coords (rest coords))
        seg-distances (loop [[[p1 p2 :as seg] & rest-pairs] pairs
                             acc-len 0
                             acc ()] ; accumlator has tuples of start-len, end-len, pair (segment)
                        (if-not p1
                          acc
                          (let [distance (point-distance p1 p2)
                                end-len (+ acc-len distance)]
                            (recur rest-pairs
                                   end-len
                                   ;; Note, we cons, so the last entry is first on the list:
                                   (cons [acc-len end-len seg] acc)))))
        [_ total-distance _] (first seg-distances)
        ->pctg #(/ % total-distance)
        pct (/ pct 100)
        pct-distances (map (fn [[d1 d2 seg]] [(->pctg d1) (->pctg d2) seg]) seg-distances)
        [lower upper [s1 s2]] (first (filter (fn [[p1 p2 s]] (<= p1 pct p2)) pct-distances))
        remainder-pct (+ lower (* (- pct lower) (- upper lower)))]
    (scale-distance s1 s2 remainder-pct)))

(defn bounds->map [bounds]
  {:north (.. bounds getNorth)
   :south (.. bounds getSouth)
   :east  (.. bounds getEast)
   :west  (.. bounds getWest)})

(defn map->bounds [{:keys [west south east north] :as bounds}]
  [[south west]
   [north east]])

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
       [geojson-layer {:data (clj->js query)}]
       (when mouse-loc
         [circle-marker {:center      mouse-loc
                         :radius      20
                         :fillColor   "#ff7800"
                         :color       "#000"
                         :opacity     1
                         :fillOpacity 1}]))
     (when drawing?
       [feature-group
        [edit-control {:draw {:rectangle false
                              :circle    false
                              :marker    false
                              :polygon   false
                              :polyline  {:allowIntersection false}}
                       :on-mounted (fn [e]
                                     (.. e -_toolbars -draw -_modes -polyline -handler enable)
                                     (.. e -_map  (once "draw:drawstop" #(re-frame/dispatch [:transect.draw/disable]))))
                       :on-created #(re-frame/dispatch [:transect/query (-> % .-layer .toGeoJSON (js->clj :keywordize-keys true))])}]])]))
