(ns imas-seamap.map.subs
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn map-props [db _] (:map db))

(defn map-layers [db _]
  (let [{:keys [layers active-layers]} (get-in db [:map])]
    {:groups        (group-by :category layers)
     :active-layers active-layers}))
