(ns imas-seamap.fx
  (:require [clojure.string :as string]
            [oops.core :refer [gget gset!]]
            [imas-seamap.utils :refer [encode-state]]
            [re-frame.core :as re-frame]))

(defn set-location-anchor [anchor]
  (gset! :location.hash anchor))


(re-frame/reg-fx :put-hash set-location-anchor)
