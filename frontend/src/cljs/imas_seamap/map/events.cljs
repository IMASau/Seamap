(ns imas-seamap.map.events
  (:require [re-frame.core :as re-frame]))


(defn update-layers [db [_ layers]]
  (assoc-in db [:map :layers] layers))
