(ns imas-seamap.worker
  (:require ["/esri-leaflet-worker/Parser" :refer [responseToFeatureCollection]]))


(defn init []
  (js/self.addEventListener "message"
    (fn [^js e]
      (js/postMessage (responseToFeatureCollection (.. e -data))))))
