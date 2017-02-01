(ns imas-seamap.views.map
  (:require [cljsjs.react-leaflet]
            [reagent.core :as r]
            [re-frame.core :as re-frame]))


(def tile-layer  (r/adapt-react-class js/ReactLeaflet.TileLayer))
(def wms-layer   (r/adapt-react-class js/ReactLeaflet.WMSTileLayer))
(def leaflet-map (r/adapt-react-class js/ReactLeaflet.Map))
(def marker      (r/adapt-react-class js/ReactLeaflet.Marker))
(def popup       (r/adapt-react-class js/ReactLeaflet.Popup))

(defn map-component []
  ;; pos/zoom would normally come from the state, of course:
  (let [map-props (re-frame/subscribe [:map/props])]
    #(let [{:keys [pos zoom markers layer-idx]} @map-props
           wl [wms-layer {:url "http://demo.opengeo.org/geoserver/ows?"
                          :layers "nasa:bluemarble"
                          :attribution "Made by Condense / Images by NASA"}]
           tl [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                           :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]]
       [leaflet-map {:id "map" :center pos :zoom zoom}
        ;; Just hacking around, to test swapping layers in and out:
        (if (odd? layer-idx) wl tl)
        (for [{:keys [pos title]} markers]
          ^{:key (str pos)}
          [marker {:position pos}
           [popup {:position pos}
            [:div.classname
             [:b title]
             [:p "Testing testing, " [:i "one two three..."]]]]])])))
