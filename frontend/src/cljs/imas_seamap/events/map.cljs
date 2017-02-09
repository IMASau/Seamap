(ns imas-seamap.events.map
  (:require [re-frame.core :as re-frame]))


(defn toggle-transect [db _]
  (update-in db [:map :controls :transect] not))

(defn update-layers [db [_ layers]]
  (assoc-in db [:map :layers] layers))
