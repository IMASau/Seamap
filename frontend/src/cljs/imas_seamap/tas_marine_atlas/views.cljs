;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :refer [plot-component helper-overlay info-card loading-display settings-overlay left-drawer-catalogue left-drawer-active-layers menu-button settings-button layers-search-omnibar layer-preview hotkeys-combos control-block print-control control-block-child transect-control]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.components :as components]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn region-control []
  (let [{:keys [selecting? region]} @(re-frame/subscribe [:map.layer.selection/info])
        [tooltip icon dispatch]
        (cond
          selecting?            ["Cancel Selecting" "undo"   :map.layer.selection/disable]
          region                ["Clear Selection"  "eraser" :map.layer.selection/clear]
          :else                 ["Select Region"    "widget" :map.layer.selection/enable])]
    [control-block-child
     {:on-click  #(re-frame/dispatch [dispatch])
      :tooltip   tooltip
      :icon      icon
      :id        "select-control"}]))

(defn custom-leaflet-controls []
  [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch
   [menu-button]
   [settings-button]
   [control-block
    [print-control]

    [control-block-child
     {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
      :tooltip  "Search All Layers"
      :icon     "search"}]

    [transect-control]
    [region-control]

    [control-block-child
     {:on-click #(re-frame/dispatch [:create-save-state])
      :tooltip  "Create Shareable URL"
      :icon     "share"}]]

   [control-block
    [control-block-child
     {:on-click #(js/document.dispatchEvent (js/KeyboardEvent. "keydown" #js{:which 47 :keyCode 47 :shiftKey true :bubbles true})) ; https://github.com/palantir/blueprint/issues/1590
      :tooltip  "Show Keyboard Shortcuts"
      :icon     "key-command"}]

    [control-block-child
     {:on-click #(re-frame/dispatch [:help-layer/toggle])
      :tooltip  "Show Help Overlay"
      :icon     "help"}]]])

(defn- left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [active-layers]} @(re-frame/subscribe [:map/layers])]
    [components/drawer
     {:title       "Tasmania Marine Atlas"
      :position    "left"
      :size        "368px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :className   "left-drawer tas-marine-atlas-drawer"
      :hasBackdrop false}
     [b/tabs
      {:id              "left-drawer-tabs"
       :class           "left-drawer-tabs"
       :selected-tab-id tab
       :on-change       #(re-frame/dispatch [:left-drawer/tab %1])
       :render-active-tab-panel-only true} ; doing this re-renders ellipsized text on tab switch, fixing ISA-359

      [b/tab
       {:id    "catalogue"
        :class "catalogue"
        :title (reagent/as-element
                [b/tooltip {:content "All available map layers"} "Catalogue"])
        :panel (reagent/as-element [left-drawer-catalogue])}]

      [b/tab
       {:id    "active-layers"
        :title (reagent/as-element
                [b/tooltip {:content "Currently active layers"}
                 [:<> "Active Layers"
                  (when (seq active-layers)
                    [:div.notification-bubble (count active-layers)])]])
        :panel (reagent/as-element [left-drawer-active-layers])}]]]))

(defn data-in-region-drawer []
  [components/drawer
   {:title       "Data in Region"
    :position    "right"
    :size        "368px"
    :isOpen      @(re-frame/subscribe [:data-in-region/open?])
    :onClose     #(re-frame/dispatch [:data-in-region/open false])
    :hasBackdrop false
    :className   "data-in-region-drawer"}])

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
      {:selector   "*"
       :helperText "Layers available in your current field of view (zoom out to see more)"}
      {:selector       ".SelectionListItem:first-child .layer-card .layer-header"
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
     [settings-overlay]
     [loading-display]
     [left-drawer]
     [data-in-region-drawer]
     [layers-search-omnibar]
     [custom-leaflet-controls]
     [layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))

