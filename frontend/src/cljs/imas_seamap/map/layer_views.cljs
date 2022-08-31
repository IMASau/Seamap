(ns imas-seamap.map.layer-views
  (:require [imas-seamap.blueprint :as b]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- layer-header [{{:keys [name]} :layer}]
  [:div.layer-header name])

(defn- layer-content [{:keys [_layer _other-props] :as layer-props}]
  [:div.layer-content
   [layer-header layer-props]])

(defn layer-card [{:keys [_layer] {:keys [active?]} :other-props :as layer-props}]
  [b/card
   {:elevation 1
    :class (str "layer-card" (when active? " active-layer"))}
   [layer-content layer-props]])
