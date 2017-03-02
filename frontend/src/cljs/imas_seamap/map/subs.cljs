(ns imas-seamap.map.subs
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn map-props [db _] (:map db))

(defn map-layers [db _]
  (let [layer-list (get-in db [:map :layers])]
    (group-by :category layer-list)))
