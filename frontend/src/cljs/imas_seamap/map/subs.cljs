(ns imas-seamap.map.subs
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))


(defn map-layers [db _]
  (let [layer-list (get-in db [:map :layers])]
    (group-by :type layer-list)))
