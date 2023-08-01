;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :refer [helper-overlay info-card loading-display left-drawer-catalogue left-drawer-active-layers menu-button layer-catalogue layers-search-omnibar hotkeys-combos control-block print-control control-block-child transect-control autosave-application-state-toggle]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.map.layer-views :refer [layer-catalogue-header]]
            [imas-seamap.story-maps.views :refer [featured-maps featured-map-drawer]]
            [imas-seamap.components :as components]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn welcome-dialogue []
  (let [open? @(re-frame/subscribe [:welcome-layer/open?])]
    [b/dialogue
     {:title
      (reagent/as-element
       [:<> "Welcome to" [:br] "Tasmania's Marine Atlas."])
      :class    "welcome-splash"
      :is-open  open?
      :on-close #(re-frame/dispatch [:welcome-layer/close])}
     [:div.bp3-dialog-body
      [:div.overview
       "The Tasmania's Marine Atlas acknowledges the traditional
       custodians of the land upon which we live and work. We honour
       their enduring culture and knowledges as vital to the
       self-determination, wellbeing and resilience of their
       communities."]
      [b/button
       {:text       "Get Started!"
        :intent     b/INTENT-PRIMARY
        :auto-focus true
        :on-click   #(re-frame/dispatch [:welcome-layer/close])}]]]))

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

(defn zoom-control []
  [control-block
   [control-block-child
    {:on-click #(-> "leaflet-control-zoom-in" js/document.getElementsByClassName first .click)
     :icon     "plus"}]
   [control-block-child
    {:on-click #(-> "leaflet-control-zoom-out" js/document.getElementsByClassName first .click)
     :icon     "minus"}]])

(defn custom-leaflet-controls []
  [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch
   [menu-button]
   [zoom-control]
   [control-block
    [print-control]

    [control-block-child
     {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
      :tooltip  "Search All Layers"
      :id       "omnisearch-control"
      :icon     "search"}]

    [transect-control]
    [region-control]

    [control-block-child
     {:on-click #(re-frame/dispatch [:create-save-state])
      :tooltip  "Create Shareable URL"
      :id       "share-control"
      :icon     "share"}]
    
    [control-block-child
     {:on-click #(re-frame/dispatch [:re-boot])
      :tooltip  "Reset Interface"
      :id       "reset-control"
      :icon     "undo"}]]

   [control-block
    [control-block-child
     {:on-click #(js/document.dispatchEvent (js/KeyboardEvent. "keydown" #js{:which 47 :keyCode 47 :shiftKey true :bubbles true})) ; https://github.com/palantir/blueprint/issues/1590
      :tooltip  "Show Keyboard Shortcuts"
      :id       "shortcuts-control"
      :icon     "key-command"}]

    [control-block-child
     {:on-click #(re-frame/dispatch [:help-layer/toggle])
      :tooltip  "Show Help Overlay"
      :id       "overlay-control"
      :icon     "help"}]]])

(defn- left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [active-layers]} @(re-frame/subscribe [:map/layers])]
    [components/drawer
     {:title
      [:<>
       [:div
        [:a {:href "https://tasmaniamarineatlas.org/"}
         [:img {:src "img/TMA_Banner_size_website.png"}]]]]
      :position    "left"
      :size        "368px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :className   "left-drawer tas-marine-atlas-drawer"
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
        :panel (reagent/as-element [left-drawer-catalogue])}]

      [b/tab
       {:id    "active-layers"
        :title (reagent/as-element
                [b/tooltip {:content "Currently active layers"}
                 [:<> "Active Layers"
                  (when (seq active-layers)
                    [:div.notification-bubble (count active-layers)])]])
        :panel (reagent/as-element [left-drawer-active-layers])}]

      [b/tab
       {:id    "featured-maps"
        :title (reagent/as-element
                [b/tooltip {:content "Guided walkthrough of featured maps"} "Featured Maps"])
        :panel (reagent/as-element [featured-maps])}]]]))

(defn- data-in-region-layer [layer {:keys [active-layers visible-layers loading-fn expanded-fn error-fn opacity-fn] :as _layer-props}]
  (let [active? (some #{layer} active-layers)
        layer-state
        {:active?   active?
         :visible?  (some #{layer} visible-layers)
         :loading?  (loading-fn layer)
         :expanded? (expanded-fn layer)
         :errors?   (error-fn layer)
         :opacity   (opacity-fn layer)}]
    [:div.data-in-region-layer
     (when active? {:class "active-layer"})
     [layer-catalogue-header {:layer layer :layer-state layer-state}]]))

(defn data-in-region-drawer []
  (let [{:keys [filtered-layers active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities main-national-layer]} @(re-frame/subscribe [:map/layers])
        {:keys [status layers]} @(re-frame/subscribe [:data-in-region/data])
        ;; Filter out layers in region that have no category (ie, currently just placeholders)
        layers (filter (set filtered-layers) layers)
        layer-props
        {:active-layers  active-layers
         :visible-layers visible-layers
         :main-national-layer main-national-layer
         :loading-fn     loading-layers
         :error-fn       error-layers
         :expanded-fn    expanded-layers
         :opacity-fn     layer-opacities}]
    [components/drawer
     {:title       "Data in Region"
      :position    "right"
      :size        "368px"
      :isOpen      @(re-frame/subscribe [:data-in-region/open?])
      :onClose     #(re-frame/dispatch [:data-in-region/open false])
      :hasBackdrop false
      :className   "data-in-region-drawer"}
     (case status

       :data-in-region/loaded
       [layer-catalogue :region layers layer-props]
       #_[:<>
        (for [{:keys [id] :as layer} layers]
          ^{:key (str id)}
          [data-in-region-layer layer layer-props])]

       :data-in-region/loading
       [b/non-ideal-state
        {:icon  (reagent/as-element [b/spinner {:intent "success"}])}]

       :data-in-region/none
       [b/non-ideal-state
        {:title       "No Data"
         :description "We are unable to display any region data at this time."
         :icon        "info-sign"}])]))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        _                  (use-hotkeys hot-keys) ; We don't need the results of this, just need to ensure it's called!
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? @(re-frame/subscribe [:sm.featured-map/open?])]
    [:div#main-wrapper.tas-marine-atlas
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open"))}
     [:div#content-wrapper
      [map-component]]

     ;; TODO: Separate helper overlay for TasMarineAtlas?
     [helper-overlay
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
      {:id "print-control" :helperText "Export current map view as an image"}
      {:id "omnisearch-control" :helperText "Search all available layers in catalogue"}
      {:id "transect-control" :helperText "Draw a transect (habitat data) or take a measurement"}
      {:id "select-control" :helperText "Select a region"}
      {:id "share-control" :helperText "Create a shareable URL for current map view"}
      {:id "reset-control" :helperText "Reset the application back to its initial state"}
      {:id "shortcuts-control" :helperText "View keyboard shortcuts"}
      {:id "overlay-control" :helperText "You are here!"}
      {:selector       ".bp3-tab-panel.catalogue>.bp3-tabs>.bp3-tab-list"
       :helperText     "Filter layers by category or responsible organisation"
       :helperPosition "bottom"
       :padding        0}]
     [info-card]
     [loading-display]
     [left-drawer]
     [data-in-region-drawer]
     [featured-map-drawer]
     [layers-search-omnibar]
     [custom-leaflet-controls]
     [welcome-dialogue]]))

