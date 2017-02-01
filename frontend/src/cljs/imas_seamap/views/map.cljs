(ns imas-seamap.views.map
  (:require [cljsjs.react-leaflet]
            [reagent.core :as r]
            [re-frame.core :as re-frame]))


(def tile-layer  (r/adapt-react-class js/ReactLeaflet.TileLayer))
(def leaflet-map (r/adapt-react-class js/ReactLeaflet.Map))
(def marker      (r/adapt-react-class js/ReactLeaflet.Marker))
(def popup       (r/adapt-react-class js/ReactLeaflet.Popup))

(defn map-component []
  ;; pos/zoom would normally come from the state, of course:
  (let [map-props (re-frame/subscribe [:map/props])]
    #(let [{:keys [pos zoom markers]} @map-props]
       [leaflet-map {:id "map" :center pos :zoom zoom
                     ;; Event handlers must start with "on" and are hooked up automatically
                     ;; (see http://leafletjs.com/reference.html#map-events)
                     ;; Downside is the only info you get is the event type and target (probably the map):
                     :ondrag (fn [e] (println "dragging..." (-> (.-target e) .getCenter js->clj (select-keys ["lat" "lng"]))))}
        [tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                     :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}]
        (for [{:keys [pos]} markers]
          ^{:key (str pos)}
          [marker {:position pos}
           [popup {:position pos}
            [:div.classname
             [:b "Roar!"]
             [:p "Testing testing, " [:i "one two three..."]]]]])])))
