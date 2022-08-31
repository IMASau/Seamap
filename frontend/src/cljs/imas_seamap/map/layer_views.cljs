(ns imas-seamap.map.layer-views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- layer-status-icons [{:keys [loading? errors?]}]
  (when (or loading? errors?)
   [:div.layer-status-icons
    (when loading? [b/spinner {:class "bp3-text-muted bp3-small"}])
    (when errors? [b/icon {:icon "warning-sign" :class "bp3-text-muted bp3-small"}])]))

(defn- layer-header-text [{{:keys [name] :as layer} :layer {:keys [expanded? active?]} :other-props}]
  [b/tooltip
   {:content (if expanded? "Hide details" "Show details")
    :disabled (not active?)
    :class "layer-header-text"}
   [b/clipped-text
    {:ellipsize true
     :on-click  #(re-frame/dispatch [:map.layer.legend/toggle layer])}
    name]])

(defn- layer-header [{:keys [_layer] {:keys [active? visible?] :as other-props} :other-props :as layer-props}]
  [:div.layer-header
   (when (and active? visible?)
     [layer-status-icons other-props])
   [layer-header-text layer-props]])

(defn- layer-content [{:keys [_layer _other-props] :as layer-props}]
  [:div.layer-content
   [layer-header layer-props]])

(defn layer-card [{:keys [_layer] {:keys [active?]} :other-props :as layer-props}]
  [b/card
   {:elevation 1
    :class (str "layer-card" (when active? " active-layer"))}
   [layer-content layer-props]])
