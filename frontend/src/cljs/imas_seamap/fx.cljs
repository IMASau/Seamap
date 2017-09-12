(ns imas-seamap.fx
  (:require [clojure.string :as string]
            [oops.core :refer [gget gset!]]
            [imas-seamap.utils :refer [parse-state]]
            [re-frame.core :as re-frame]))

(defn set-location-anchor [anchor]
  (gset! :location.hash anchor))


(re-frame/reg-fx :put-hash set-location-anchor)

(defn cofx-hash-state [cofx _]
  (let [hash-val (gget :location.hash)]
    (merge cofx
           (when-not (string/blank? hash-val)
             {:hash-state (parse-state (subs hash-val 1))}))))

(re-frame/reg-cofx :hash-state cofx-hash-state)
