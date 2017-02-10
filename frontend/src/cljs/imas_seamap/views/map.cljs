(ns imas-seamap.views.map
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
  (let [{:keys [pos zoom markers controls layer-idx]} @(re-frame/subscribe [:map/props])
        {:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        wl [wms-layer {:url "http://demo.opengeo.org/geoserver/ows?"
                       :layers "nasa:bluemarble"
                       :attribution "Made by Condense / Images by NASA"}]
        tl [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                        :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]]
    [leaflet-map {:id "map" :center pos :zoom zoom}
     ;; Just hacking around, to test swapping layers in and out:
     (if (odd? layer-idx) wl tl)
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
                       :on-created #(re-frame/dispatch [:transect/query (-> % .-layer .toGeoJSON (js->clj :keywordize-keys true))])}]])
     (for [{:keys [pos title]} markers]
       ^{:key (str pos)}
       [marker {:position pos}
        [popup {:position pos}
         [:div.classname
          [:b title]
          [:p "Testing testing, " [:i "one two three..."]]]]])]))
