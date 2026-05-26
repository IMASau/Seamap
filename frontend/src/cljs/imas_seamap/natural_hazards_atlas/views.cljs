;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.views
  (:require [goog.string.format]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.components :as components]
            [imas-seamap.natural-hazards-atlas.map.views :refer [map-component]]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.story-maps.views :refer [featured-maps]]
            [imas-seamap.views :as views]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn- welcome-dialogue []
  (let [dont-show-again? (reagent/atom false)]
   (fn []
     (let [open? @(re-frame/subscribe [:welcome-layer/open?])]
       [b/dialogue
        {:title
         (reagent/as-element
          [:<> "Welcome to the" [:br] "Natural Hazards Atlas"])
         :class    "welcome-splash"
         :is-open  open?
         :on-close #(re-frame/dispatch [:welcome-layer/close false])}
        [:div.bp3-dialog-body
         [:div.overview
          [:p "The Natural Hazards Atlas for Tasmania is an interactive platform developed by the University of Tasmania that brings together climate-driven natural hazards data, mapping and science communication to support disaster preparedness, resilience and informed decision-making across Tasmania."]
          [:p "Explore the interactive map and visit " [:a {:href "https://nathaz-dev.its.utas.edu.au/" :target "_blank"} "here"] " to learn more about the atlas tools and features."]]]
        [:div.bp3-dialog-footer
         [:div
          [:input
           {:type     "checkbox"
            :name     "dont-show-this-again"
            :on-click #(reset! dont-show-again? (.. % -target -checked))
            :checked  @dont-show-again?}]
          [:label {:for "dont-show-this-again"} "Don't show this again"]]
         [b/button
          {:text       "Get Started!"
           :intent     b/INTENT-PRIMARY
           :auto-focus true
           :on-click   #(re-frame/dispatch [:welcome-layer/close @dont-show-again?])}]]]))))

(defn- menu-button []
  (let [icon (if @(re-frame/subscribe [:left-drawer/open?]) "double-chevron-left" "double-chevron-right")]
    [views/leaflet-control-button
     {:on-click #(re-frame/dispatch [:left-drawer/toggle])
      :id       "menu-button"
      :icon     icon}]))

(defn- autosave-toggle-button []
  (let [[icon text] (if @(re-frame/subscribe [:autosave?])
                      ["floppy-disk" "Application autosave is currently enabled"]
                      ["disable" "Application autosave is currently disabled"])]
    [views/leaflet-control-button
     {:on-click #(re-frame/dispatch [:toggle-autosave])
      :tooltip  text
      :id       "autosave-button"
      :icon     icon}]))

(defn- custom-leaflet-controls
  "Changes from imas-seamap.views/custom-leaflet-controls:
   - removed region control
   - removed transect control
   - removed settings button
   - added autosave toggle"
  []
  [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch
   [menu-button]
   [autosave-toggle-button]
   [views/zoom-control]

   [views/control-block
    [views/print-control]

    [views/control-block-child
     {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
      :tooltip  "Search All Layers"
      :id       "omnisearch-control"
      :icon     "search"}]

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

(defn- floating-pills []
  (let [collapsed                      (:collapsed @(re-frame/subscribe [:ui/sidebar]))
        rich-layers-side-by-side-views @(re-frame/subscribe [:map/rich-layers-side-by-side-views])]
    [:div {:class (str "floating-pills" (when collapsed " collapsed"))}
     (for [{:keys [id] :as rich-layer} rich-layers-side-by-side-views]
       ^{:key (str id)}
       [views/side-by-side-views-pill rich-layer])]))

(defn- time-period-select
  "Select the time period of the hazard data to view on the map.
   This is a half-baked implementation, because we haven't nailed-down what time
   periods span what years, and all the currently available data is historic."
  []
  (let [{:keys [time-periods]} @(re-frame/subscribe [:current-view/time-periods])]
    [components/form-group
     {:label "Time Period"}
     [components/select
      {:value        @(re-frame/subscribe [:current-view/selected-time-period])
       :options      time-periods
       :onChange     #(re-frame/dispatch [:current-view/selected-time-period %])
       :keyfns
       {:id   :id
        :text :name}}]]))

(defn- timeline-media-controls
  "Media-style controls to play through the timeline of hazard data."
  []
  (let [{:keys [is-playing? is-loading? can-step-forward? can-step-backward?]} @(re-frame/subscribe [:current-view/timeline-media-controls])]
    [b/button-group
     [b/button
      {:icon "step-backward"
       :disabled (not can-step-backward?)
       :on-click #(re-frame/dispatch [:current-view.time/step-backward])}]
     [b/button
      {:icon (if is-playing? "pause" "play")
       :on-click
       (if is-playing?
         #(re-frame/dispatch [:map.time/pause])
         #(re-frame/dispatch [:map.time/play]))
       :active is-playing?}]
     [b/button
      {:icon "step-forward"
       :disabled (not can-step-forward?)
       :on-click #(re-frame/dispatch [:current-view.time/step-forward])}]
     [b/button
      {:icon "tick-circle"
       :loading is-loading?
       :disabled true}]]))

(defn- timeline-slider
  "Slider to select the date (year) of data to view."
  []
  (let [available-times @(re-frame/subscribe [:map.time/available-times])
        label-renderer #(.getFullYear (js/Date. %))
        label-values (conj (take-nth 10 available-times) (last available-times))
        value @(re-frame/subscribe [:map.time/current-time])]
    [components/snap-slider
     {:value     value
      :values    available-times
      :on-change #(re-frame/dispatch [:map.time/current-time %])
      :label-values label-values
      :label-renderer label-renderer}]))

(defn- timeline-select
  "Controls for selecting the time (year) of hazard data to view.
   Should only be shown if there are available times to select from."
  []
  [components/form-group
   {:label "Year"}
   [:<>
    [timeline-slider]
    [timeline-media-controls]]])

(defn- current-view
  "Layer configuration panel where model, scenario, and time parameters are
   selected. Each parameter affects the map appearance and projection data."
  []
  [:div.current-view
   [:div
    {:style {:display "flex" :gap "8px" :margin-bottom "8px"}}
    [:div {:style {:flex 1}}
     [components/form-group
      {:label "Model"}
      [components/select
       {:value        @(re-frame/subscribe [:current-view/selected-model])
        :options      @(re-frame/subscribe [:current-view/models])
        :onChange     #(re-frame/dispatch [:current-view/selected-model %])
        :keyfns
        {:id   :id
         :text :name}}]]]
    [:div {:style {:flex 1}}
     [components/form-group
      {:label "Scenario"}
      [components/select
       {:value        @(re-frame/subscribe [:current-view/selected-scenario])
        :options      @(re-frame/subscribe [:current-view/scenarios])
        :onChange     #(re-frame/dispatch [:current-view/selected-scenario %])
        :keyfns
        {:id   :id
         :text :name}}]]]]
   [components/form-group
    {:label "Seasonal Data"}
    [components/select
     {:value        @(re-frame/subscribe [:current-view/selected-seasonal-data])
      :options      @(re-frame/subscribe [:current-view/seasonal-datas])
      :onChange     #(re-frame/dispatch [:current-view/selected-seasonal-data %])
      :keyfns
      {:id   :id
       :text :name}}]]
   (when (and @(re-frame/subscribe [:map.time/current-time]) (seq @(re-frame/subscribe [:map.time/available-times])))
     [b/card
      [time-period-select]
      [timeline-select]])
   [:div {:style {:position "absolute" :top "200px" :right "16px"}}
    [b/tooltip
     {:content
      (reagent/as-element
       [b/non-ideal-state
        {:title       "Work In Progress"
         :description "Current View is a work in progress. Model, Scenario, and Seasonal Data parameters are non-functional."
         :icon        "info-sign"}])}
     [b/icon {:icon "info-sign"}]]]])

(defn- layer-catalogue [catid layer-props tma?]
  (let [selected-tab @(re-frame/subscribe [:ui.catalogue/tab catid])
        select-tab   #(re-frame/dispatch [:ui.catalogue/select-tab catid %1])
        open-all?    (>= (count @(re-frame/subscribe [:map.layers/filter])) 3)]
    [b/tabs {:selected-tab-id selected-tab
             :on-change       select-tab
             :render-active-tab-panel-only true} ; doing this re-renders ellipsized text on tab switch, fixing ISA-359
     [b/tab
      {:id    "hazards"
       :title "Hazards"
       :panel (reagent/as-element
               [views/layer-catalogue-tree catid @(re-frame/subscribe [:map.layers/filtered-hazard-layers]) [:category :data_classification] "hazards" layer-props open-all? tma?])}]
     [b/tab
      {:id    "supporting-layers"
       :title "Supporting Layers"
       :panel (reagent/as-element
               [views/layer-catalogue-tree catid @(re-frame/subscribe [:map.layers/filtered-supporting-layers]) [:data_classification] "supporting-layers" layer-props open-all? tma?])}]]))

(defn- left-drawer-catalogue [tma?]
  (let [{:keys [active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities rich-layer-fn]} @(re-frame/subscribe [:map/layers])]
    [:<>
     [views/layer-search-filter]
     [layer-catalogue :main
      {:active-layers  active-layers
       :visible-layers visible-layers
       :loading-fn     loading-layers
       :error-fn       error-layers
       :expanded-fn    expanded-layers
       :opacity-fn     layer-opacities
       :rich-layer-fn  rich-layer-fn}
      tma?]]))

(defn- left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [active-layers]} @(re-frame/subscribe [:map/layers])]
    [components/drawer
     {:title [:div [:img {:src "img/Climate Futures + NHAT logo – COLOUR.png"}]]
      :position    "left"
      :size        "368px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :className   "natural-hazards-atlas-drawer left-drawer"
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
        :panel (reagent/as-element [left-drawer-catalogue false])}]

      [b/tab
       {:id    "active-layers"
        :title (reagent/as-element
                [b/tooltip {:content "Currently active layers"}
                 [:<> "Active Layers"
                  (when (seq active-layers)
                    [:div.notification-bubble (count active-layers)])]])
        :panel (reagent/as-element [views/left-drawer-active-layers false])}]
      [:div {:style {:flex-basis "100%" :height 0}}] ; spacer to push the next tabs to a new line
      [b/tab
       {:id    "current-view"
        :title (reagent/as-element
                [b/tooltip {:content "Configure the map layers"} "Current View"])
        :panel (reagent/as-element [current-view])}]
      [b/tab
       {:id    "featured-maps"
        :title (reagent/as-element
                [b/tooltip {:content "Guided walkthrough of featured maps"} "Featured Maps"])
        :panel (reagent/as-element [featured-maps])}]]]))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] views/hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? (seq @(re-frame/subscribe [:ui/right-sidebar]))
        loading?           @(re-frame/subscribe [:app/loading?])]
    [:div#main-wrapper.natural-hazards-atlas
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
     [welcome-dialogue]
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
     [floating-pills]
     [views/layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))
