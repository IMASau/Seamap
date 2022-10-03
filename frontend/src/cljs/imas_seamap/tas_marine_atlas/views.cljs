;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :refer [plot-component helper-overlay info-card loading-display left-drawer-catalogue left-drawer-active-layers left-drawer-controls layers-search-omnibar layer-preview hotkeys-combos]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.story-maps.views :refer [featured-maps featured-map-drawer]]
            [imas-seamap.components :as components]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])]
    [components/drawer
     {:title       "Tasmania Marine Atlas"
      :position    "left"
      :size        "460px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :hasBackdrop false}
     [:div.sidebar-tab.height-managed
      [b/tabs
       {:id              "left-drawer-tabs"
        :class           "left-drawer-tabs"
        :selected-tab-id tab
        :on-change       #(re-frame/dispatch [:left-drawer/tab %1])}

       [b/tab
        {:id    "catalogue"
         :title "Catalogue"
         :panel (reagent/as-element [left-drawer-catalogue])}]

       [b/tab
        {:id    "active-layers"
         :title "Active Layers"
         :panel (reagent/as-element [left-drawer-active-layers])}]

       [b/tab
        {:id    "controls"
         :title "Controls"
         :panel (reagent/as-element [left-drawer-controls])}]

       [b/tab
        {:id    "featured-maps"
         :title "Featured Maps"
         :panel (reagent/as-element [featured-maps])}]]]]))

(defn- floating-pills []
  (let [collapsed (:collapsed @(re-frame/subscribe [:ui/sidebar]))]
    [:div
     {:class (str "floating-pills" (when collapsed " collapsed"))}

     [components/floating-pill-button
      {:icon     "menu"
       :on-click #(re-frame/dispatch [:left-drawer/toggle])}]]))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        _                  (use-hotkeys hot-keys) ; We don't need the results of this, just need to ensure it's called!
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? @(re-frame/subscribe [:sm.featured-map/open?])]
    [:div#main-wrapper
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open"))}
     [:div#content-wrapper
      [map-component]
      [plot-component]]
     
     ;; TODO: Separate helper overlay for TasMarineAtlas?
     [helper-overlay
      :layer-search
      :plot-footer
      {:selector   ".group-scrollable"
       :helperText "Layers available in your current field of view (zoom out to see more)"}
      {:selector       ".group-scrollable > .layer-wrapper:first-child"
       :helperPosition "bottom"
       :helperText     "Toggle layer visibility, view more info, show legend, and download data"}
      {:selector   ".sidebar-tabs ul:first-child"
       :helperText "Choose between habitat, bathymetry, and other layer types"}
      :transect-btn-wrapper
      :select-btn-wrapper
      {:selector ".sidebar-tabs ul:nth-child(2)" :helperText "Reset interface"}
      {:id "habitat-group" :helperText "Layers showing sea-floor habitats"}
      {:id "bathy-group" :helperText "Layers showing bathymetry data"}
      {:id "imagery-group" :helperText "Layers showing photos collected"}
      {:id "third-party-group" :helperText "Layers from other providers (eg CSIRO)"}]
     [info-card]
     [loading-display]
     [left-drawer]
     [featured-map-drawer]
     [layers-search-omnibar]
     [floating-pills]
     [layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))

