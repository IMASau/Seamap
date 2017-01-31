(ns imas-seamap.views.map
  (:require [cljsjs.react-leaflet]
            [reagent.core :as r]))


(defn- build
  [component props & children]
  (apply r/create-element
         component
         (clj->js props)
         children))

(def tile-layer  (partial build js/ReactLeaflet.TileLayer))
(def leaflet-map (partial build js/ReactLeaflet.Map))
(def marker      (partial build js/ReactLeaflet.Marker))
(def popup       (partial build js/ReactLeaflet.Popup))

#_(defn map-component []
  ;; pos/zoom would normally come from the state, of course:
  (let [{:keys [pos zoom]} {:pos [51.505 -0.09] :zoom 13}
        tl (tile-layer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                        :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"})
        ;; Note use of reagent as-element to render simple content
        p (popup {:position pos} (r/as-element [:div.classname
                                                [:b "Roar!"]
                                                [:p "Testing testing, " [:i "one two three..."]]]))
        mk (marker {:position pos} p)]
    (leaflet-map {:id "mapper" :center pos :zoom zoom
                  ;; Event handlers must start with "on" and are hooked up automatically
                  ;; (see http://leafletjs.com/reference.html#map-events)
                  ;; Downside is the only info you get is the event type and target (probably the map):
                  :ondrag #(println "dragging..." (-> (.-target %) .getCenter js->clj (select-keys ["lat" "lng"])))}
      tl
      mk)))
