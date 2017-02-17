(ns imas-seamap.map.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]))


(def tile-layer    (r/adapt-react-class js/ReactLeaflet.TileLayer))
(def wms-layer     (r/adapt-react-class js/ReactLeaflet.WMSTileLayer))
(def geojson-layer (r/adapt-react-class js/ReactLeaflet.GeoJSON))
(def leaflet-map   (r/adapt-react-class js/ReactLeaflet.Map))
(def marker        (r/adapt-react-class js/ReactLeaflet.Marker))
(def popup         (r/adapt-react-class js/ReactLeaflet.Popup))
(def feature-group (r/adapt-react-class js/ReactLeaflet.FeatureGroup))
(def edit-control  (r/adapt-react-class js/ReactLeaflet.EditControl))

(defn map-component []
  (let [{:keys [pos zoom controls active-layers]} @(re-frame/subscribe [:map/props])
        {:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        base-layer-bluemarble [wms-layer {:url "http://demo.opengeo.org/geoserver/ows?"
                                          :layers "nasa:bluemarble"
                                          :attribution "Made by Condense / Images by NASA"}]
        base-layer-osm [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]]
    [leaflet-map {:id "map" :center pos :zoom zoom}
     base-layer-osm
     (for [{:keys [server_url layer_name] :as layer} active-layers]
       ^{:key (str server_url layer_name)}
       [wms-layer {:url server_url :layers layer_name
                   :transparent true :format "image/png"}])
     (when query
       [geojson-layer {:data (clj->js query)}])
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
