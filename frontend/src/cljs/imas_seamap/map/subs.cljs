(ns imas-seamap.map.subs
  (:require [imas-seamap.utils :refer [bbox-intersects?]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn map-props [db _] (:map db))

(defn map-layers [db _]
  (let [{:keys [layers active-layers bounds]} (get-in db [:map])
        visible-layers                        (filter #(bbox-intersects? bounds (:bounding_box %)) layers)]
    {:groups        (group-by :category visible-layers)
     :active-layers active-layers}))
