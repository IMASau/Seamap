;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.futuresofseafood.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :as views]
            [imas-seamap.futuresofseafood.map.views :refer [map-component]]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] views/hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? (seq @(re-frame/subscribe [:ui/right-sidebar]))
        loading?           @(re-frame/subscribe [:app/loading?])]
    [:div#main-wrapper.seamap ;{:on-key-down handle-keydown :on-key-up handle-keyup}
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open") (when loading? " loading"))}
     [:div#content-wrapper
      [map-component]
      [views/plot-component]]

     ;; TODO: Update helper-overlay for new Seamap version (or remove?)
     [views/helper-overlay
      {:selector       ".SelectionListItem:first-child .layer-card .layer-header"
       :helperPosition "bottom"
       :helperText     "Toggle layer visibility, view info and metadata, show legend, adjust transparency, choose from download options (habitat data)"
       :padding        0}
      {:selector       ".leaflet-control-layers-toggle"
       :helperText     "Select from available basemaps"
       :helperPosition "left"}
      {:id "layer-search" :helperText "Freetext search for a specific layer by name or keywords"}
      {:id "settings-button" :helperText "Select from user-configurable settings"}
      {:id "print-control" :helperText "Export current map view as an image"}
      {:id "omnisearch-control" :helperText "Search all available layers in catalogue"}
      {:id "transect-control" :helperText "Draw a transect (habitat data) or take a measurement"}
      {:id "select-control" :helperText "Select a region for download (habitat data)"}
      {:id "share-control" :helperText "Create a shareable URL for current map view"}
      {:id "reset-control" :helperText "Reset the application back to its initial state"}
      {:id "shortcuts-control" :helperText "View keyboard shortcuts"}
      {:id "overlay-control" :helperText "You are here!"}
      {:selector       ".bp3-tab-panel.catalogue>.bp3-tabs>.bp3-tab-list"
       :helperText     "Filter layers by category or responsible organisation"
       :helperPosition "bottom"
       :padding        0}
      {:id "plot-footer"
       :helperText "Draw a transect to show a depth profile of habitat data"
       :helperPosition "top"}]
     [views/welcome-dialogue]
     [views/outage-message-dialogue]
     [views/settings-overlay]
     [views/info-card]
     [views/loading-display]
     [views/left-drawer]
     [views/right-drawer @(re-frame/subscribe [:ui/right-sidebar])]
     [views/layers-search-omnibar]
     [views/custom-leaflet-controls]
     [views/layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))
