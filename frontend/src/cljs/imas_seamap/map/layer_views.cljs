(ns imas-seamap.map.layer-views
  (:require [imas-seamap.blueprint :as b]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn layer-card [{:keys [_layer] {:keys [active?]} :other-props}]
  [b/card
   {:elevation 1
    :class (str "layer-card" (when active? " active-layer"))}])
