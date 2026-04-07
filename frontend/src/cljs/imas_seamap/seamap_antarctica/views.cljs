;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.seamap-antarctica.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.components :as components]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :as views]
            [imas-seamap.seamap-antarctica.map.views :refer [map-component]]
            [imas-seamap.story-maps.views :refer [featured-maps]]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn custom-leaflet-controls []
  "Differs from the custom-leaflet-controls component in imas-seamap.views by removing region-control"
  [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch
   [views/menu-button]
   [views/settings-button]
   [views/zoom-control]

   [views/control-block
    [views/print-control]

    [views/control-block-child
     {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
      :tooltip  "Search All Layers"
      :id       "omnisearch-control"
      :icon     "search"}]

    [views/transect-control]

    [views/control-block-child
     {:on-click #(re-frame/dispatch [:create-save-state])
      :tooltip  "Create Shareable URL"
      :id       "share-control"
      :icon     "share"}]

    [views/control-block-child
     {:on-click #(re-frame/dispatch [:re-boot])
      :tooltip  "Reset Interface"
      :id       "reset-control"
      :icon     "undo"}]]

   [views/control-block
    [views/control-block-child
     {:on-click #(js/document.dispatchEvent (js/KeyboardEvent. "keydown" #js{:which 47 :keyCode 47 :shiftKey true :bubbles true})) ; https://github.com/palantir/blueprint/issues/1590
      :tooltip  "Show Keyboard Shortcuts"
      :id       "shortcuts-control"
      :icon     "key-command"}]

    [views/control-block-child
     {:on-click #(re-frame/dispatch [:help-layer/toggle])
      :tooltip  "Show Help Overlay"
      :id       "overlay-control"
      :icon     "help"}]]])

(defn floating-pills []
  (let [collapsed                      (:collapsed @(re-frame/subscribe [:ui/sidebar]))
        rich-layers-side-by-side-views @(re-frame/subscribe [:map/rich-layers-side-by-side-views])]
    [:div {:class (str "floating-pills" (when collapsed " collapsed"))}
     (for [{:keys [id] :as rich-layer} rich-layers-side-by-side-views]
       ^{:key (str id)}
       [views/side-by-side-views-pill rich-layer])]))

(defn left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [active-layers]} @(re-frame/subscribe [:map/layers])]
    [components/drawer
     {:title
      [:<>
       [:div
        [:a {:href "https://seamapaustralia.org/"}
         [:img {:src "img/SeaMapAntarctica_Logo_RGB_1000px.png"}]]]
       [b/button
        {:icon     "double-chevron-left"
         :minimal  true
         :on-click #(re-frame/dispatch [:left-drawer/close])}]]
      :position    "left"
      :size        "368px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :className   "left-drawer seamap-drawer"
      :isCloseButtonShown false
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
        :panel (reagent/as-element [views/left-drawer-catalogue false])}]

      [b/tab
       {:id    "active-layers"
        :title (reagent/as-element
                [b/tooltip {:content "Currently active layers"}
                 [:<> "Active Layers"
                  (when (seq active-layers)
                    [:div.notification-bubble (count active-layers)])]])
        :panel (reagent/as-element [views/left-drawer-active-layers false])}]

      [b/tab
       {:id    "featured-maps"
        :title (reagent/as-element
                [b/tooltip {:content "Guided walkthrough of featured maps"} "Featured Maps"])
        :panel (reagent/as-element [featured-maps])}]]]))

(def hotkeys-combos
  (let [keydown-wrapper
        (fn [m keydown-v]
          (assoc m :global    true
                 :group "Keyboard Shortcuts"
                 :onKeyDown #(re-frame/dispatch keydown-v)))]
    ;; See note on `use-hotkeys' for rationale invoking `clj->js' here:
    (clj->js
     [(keydown-wrapper
       {:label "Zoom In"                :combo "plus"}
       [:map/zoom-in])
      (keydown-wrapper
       {:label "Zoom Out"               :combo "-"}
       [:map/zoom-out])
      (keydown-wrapper
       {:label "Pan Up"                 :combo "up"}
       [:map/pan-direction :up])
      (keydown-wrapper
       {:label "Pan Down"               :combo "down"}
       [:map/pan-direction :down])
      (keydown-wrapper
       {:label "Pan Left"               :combo "left"}
       [:map/pan-direction :left])
      (keydown-wrapper
       {:label "Pan Right"              :combo "right"}
       [:map/pan-direction :right])
      (keydown-wrapper
       {:label "Toggle Left Drawer"     :combo "a"}
       [:left-drawer/toggle])
      (keydown-wrapper
       {:label "Start/Clear Measurement"   :combo "t"}
       [:transect.draw/toggle])
      (keydown-wrapper
       {:label "Cancel"                 :combo "esc"}
       [:ui.drawing/cancel])
      (keydown-wrapper
       {:label "Layer Power Search"     :combo "s"}
       [:layers-search-omnibar/toggle])
      (keydown-wrapper
       {:label "Reset"                  :combo "shift + r"}
       [:re-boot])
      (keydown-wrapper
       {:label "Create Shareable URL"   :combo "c"}
       [:create-save-state])
      (keydown-wrapper
       {:label "Show Help Overlay"      :combo "h"}
       [:help-layer/toggle])])))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? (seq @(re-frame/subscribe [:ui/right-sidebar]))
        loading?           @(re-frame/subscribe [:app/loading?])]
    [:div#main-wrapper.seamap ;{:on-key-down handle-keydown :on-key-up handle-keyup}
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open") (when loading? " loading"))}
     [:div#content-wrapper
      [map-component]]

     ;; TODO: Update helper-overlay for new Seamap version (or remove?)
     [views/helper-overlay
      {:selector       ".SelectionListItem:first-child .layer-card .layer-header"
       :helperPosition "bottom"
       :helperText     "Toggle layer visibility, view info and metadata, show legend, adjust transparency, choose from download options (habitat data)"
       :padding        0}
      {:id "state-of-knowledge-pill"
       :helperText "Select a management region to view summaries of the current state of research knowledge for the area"}
      {:selector       ".leaflet-control-layers-toggle"
       :helperText     "Select from available basemaps"
       :helperPosition "left"}
      {:id "layer-search" :helperText "Freetext search for a specific layer by name or keywords"}
      {:id "settings-button" :helperText "Select from user-configurable settings"}
      {:id "print-control" :helperText "Export current map view as an image"}
      {:id "omnisearch-control" :helperText "Search all available layers in catalogue"}
      {:id "transect-control" :helperText "Take a measurement"}
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
     [left-drawer]
     [views/right-drawer @(re-frame/subscribe [:ui/right-sidebar])]
     [views/layers-search-omnibar]
     [custom-leaflet-controls]
     [:div.custom-leaflet-controls.leaflet-top.leaflet-right.leaflet-touch
      {:style {:font "12px/1.5 \"Helvetica Neue\", Arial, Helvetica, sans-serif"}} ; font style for Leaflet map-component - needs to be inherited into custom controls
      [views/layers-control]]
     ;; Definitely no state-of-knowledge, and probably no generic pills for now at least:
     [floating-pills]
     [views/layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))
