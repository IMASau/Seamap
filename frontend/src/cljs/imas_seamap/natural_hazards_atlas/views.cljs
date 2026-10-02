;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.views
  (:require [clojure.string :as string]
            [goog.string.format]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.components :as components]
            [imas-seamap.map.layer-views :refer [legend-display]]
            [imas-seamap.natural-hazards-atlas.map.views :refer [map-component]]
            [imas-seamap.natural-hazards-atlas.utils :as nhatutils]
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

(defn- region-control []
  (let [{:keys [selecting? region]} @(re-frame/subscribe [:map.layer.selection/info])
        [tooltip icon dispatch]
        (cond
          selecting? ["Cancel Selecting"    "undo"   :map.layer.selection/disable]
          region     ["Clear Selection"     "eraser" :map.layer.selection/clear]
          :else      ["Select Habitat Data" "widget" :map.layer.selection/enable])]
    [views/control-block-child
     {:on-click  #(re-frame/dispatch [dispatch])
      :tooltip   tooltip
      :icon      icon
      :id        "select-control"}]))

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
    
    [region-control]

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

(defn- year-of
  "Year of a time in ms."
  [time]
  (when time (.getFullYear (js/Date. time))))

(defn- timeline-pct
  "Position of a year along the \"When\" timeline, as a CSS percentage."
  [year]
  (let [{:keys [start-year end-year]} nhatutils/timeline-axis]
    (str (* 100 (/ (- year start-year) (- end-year start-year))) "%")))

(defn- when-preset-buttons
  "The four \"When\" presets. Most users pick a period, not a year."
  [{:keys [map-id selected]}]
  [b/button-group
   {:fill  true
    :class "stacked-buttons"}
   (for [{:keys [id name historic? start-year end-year]} nhatutils/when-presets]
     ^{:key id}
     [b/button
      {:active   (= id selected)
       :on-click #(re-frame/dispatch [:current-view/when id map-id])}
      [:span.stacked-button-label name]
      [:span.stacked-button-detail
       (if historic?
         (str "to " end-year)
         (str start-year "–" (mod end-year 100)))]])])

(defn- when-timeline
  "Each preset's 20-year window drawn to scale on a timeline, so the averaging
   period is visible. The selected window is highlighted, and a marker shows the
   year the map is showing. Clicking a window selects that preset."
  [{:keys [map-id selected]}]
  (let [current-year (year-of @(re-frame/subscribe [:map.time/current-time map-id]))]
    [:div.when-timeline
     [:div.when-timeline-track
      (for [{:keys [id name start-year axis-start-year end-year]} nhatutils/when-presets
            :let [start-year (or start-year axis-start-year)]]
        ^{:key id}
        [:div.when-timeline-window
         {:class    (when (= id selected) "selected")
          :style    {:left  (timeline-pct start-year)
                     :right (str "calc(100% - " (timeline-pct (inc end-year)) ")")}
          :title    (str name " (" start-year "–" end-year ")")
          :on-click #(re-frame/dispatch [:current-view/when id map-id])}])
      (when current-year
        [:div.when-timeline-marker
         {:style {:left (timeline-pct (+ current-year 0.5))}}])]
     [:div.when-timeline-ticks
      (for [year [2000 2025 2050 2075 2100]]
        ^{:key year}
        [:span {:style {:left (timeline-pct year)}} year])]]))

(defn- watch-change
  "Play through the years of the selected period, one year at a time. Kept, as
   the project plan promises a time slider, but secondary to the presets."
  [{:keys [map-id]}]
  (let [{:keys [is-playing? is-loading? is-disabled? can-step-forward? can-step-backward?]}
        @(re-frame/subscribe [:current-view/timeline-media-controls map-id])
        current-year (year-of @(re-frame/subscribe [:map.time/current-time map-id]))
        has-years?   (seq @(re-frame/subscribe [:map.time/available-times map-id]))]
    [:div.watch-change
     [b/tooltip
      {:content  "Watch change is disabled while comparing maps"
       :disabled (not is-disabled?)}
      [b/button
       {:icon     (if is-playing? "pause" "play")
        :text     (if is-playing? "Pause" "Watch change")
        :small    true
        :active   is-playing?
        :disabled (or is-disabled? (not has-years?))
        :on-click (if is-playing?
                    #(re-frame/dispatch [:map.time/pause map-id])
                    #(re-frame/dispatch [:map.time/play map-id]))}]]
     [:div.watch-change-year
      [b/button
       {:icon     "chevron-left"
        :minimal  true
        :small    true
        :title    "Previous year"
        :disabled (or (not has-years?) (not can-step-backward?) is-disabled?)
        :on-click #(re-frame/dispatch [:current-view.time/step-backward map-id])}]
      [:span (cond current-year current-year has-years? "–" :else "Loading…")]
      [b/button
       {:icon     "chevron-right"
        :minimal  true
        :small    true
        :title    "Next year"
        :disabled (or (not has-years?) (not can-step-forward?) is-disabled?)
        :on-click #(re-frame/dispatch [:current-view.time/step-forward map-id])}]
      (when is-loading? [b/spinner {:size 14}])]]))

(defn- when-select
  "One control for one question: when? The presets, the timeline that shows their
   windows, and Watch change underneath."
  [{:keys [map-id]}]
  (let [selected @(re-frame/subscribe [:current-view/when map-id])]
    [b/card {:id "time-control"}
     [components/form-group
      {:label "When"}
      [:<>
       [when-preset-buttons {:map-id map-id :selected selected}]
       [when-timeline {:map-id map-id :selected selected}]
       [watch-change {:map-id map-id}]]]]))

(defn- emissions-select
  "Lower or higher emissions, in plain words first with the SSP code underneath.
   Doesn't apply to the baseline."
  [{:keys [map-id]}]
  (let [is-historic? @(re-frame/subscribe [:current-view/is-historic? map-id])
        selected     @(re-frame/subscribe [:current-view/selected-scenario map-id])
        scenarios    @(re-frame/subscribe [:current-view/scenarios])]
    [components/form-group
     {:label "Emissions"}
     [b/tooltip
      {:content  "Emissions scenarios apply to projections, not the baseline"
       :disabled (not is-historic?)
       :class    "bp3-fill"}
      [b/button-group
       {:fill  true
        :class "stacked-buttons"}
       (for [{:keys [id name display_name] :as scenario} scenarios
             :let [{:keys [label code]} (get nhatutils/scenario-labels name)]]
         ^{:key id}
         [b/button
          {:active   (and (not is-historic?) (= id (:id selected)))
           :disabled is-historic?
           :on-click #(re-frame/dispatch [:current-view/selected-scenario scenario map-id])}
          [:span.stacked-button-label (or label display_name)]
          [:span.stacked-button-detail (or code name)]])]]]))

(defn- season-select
  [{:keys [map-id]}]
  [components/form-group
   {:label "Season"}
   [components/select
    {:value    @(re-frame/subscribe [:current-view/selected-seasonal-data map-id])
     :options  @(re-frame/subscribe [:current-view/seasonal-datas])
     :onChange #(re-frame/dispatch [:current-view/selected-seasonal-data % map-id])
     :keyfns
     {:id   :id
      :text :display_name}}]])

(defn- advanced-settings
  "Expert settings with sensible defaults, collapsed and showing what's in use."
  []
  (let [open? (reagent/atom false)]
    (fn [{:keys [map-id]}]
      (let [cmip-phase @(re-frame/subscribe [:current-view/selected-cmip-phase map-id])
            model      @(re-frame/subscribe [:current-view/selected-model map-id])]
        [:div.advanced-settings
         [b/button
          {:minimal  true
           :small    true
           :icon     (if @open? "chevron-down" "chevron-right")
           :on-click #(swap! open? not)}
          "Advanced: "
          [:span.bp3-text-muted (:display_name model) ", " (:display_name cmip-phase)]]
         [b/collapse
          {:is-open @open?}
          [:div.advanced-settings-body
           [components/form-group {:label "Model"}
            [components/select
             {:value    model
              :options  @(re-frame/subscribe [:current-view/models])
              :onChange #(re-frame/dispatch [:current-view/selected-model % map-id])
              :keyfns
              {:id   :id
               :text :display_name}}]]
           [components/form-group {:label "CMIP"}
            [components/select
             {:value    cmip-phase
              :options  @(re-frame/subscribe [:current-view/cmip-phases])
              :onChange #(re-frame/dispatch [:current-view/selected-cmip-phase % map-id])
              :keyfns
              {:id   :id
               :text :display_name}}]]]]]))))

(defn- current-view-caption
  "What the map shows, in a sentence. Makes the view defensible when shared."
  [{:keys [map-id]}]
  (let [{:keys [layer year period years emissions season model cmip-phase]}
        @(re-frame/subscribe [:current-view/caption map-id])]
    (when (and layer year)
      [:p.current-view-caption
       "Map shows " [:b layer] " in " [:b year] ", from " period
       (when years (str " (" years ")"))
       (when emissions (str ", under " emissions))
       ". "
       [:span.bp3-text-muted
        (string/join ", " (remove nil? [season model cmip-phase])) "."]])))

(defn- side-by-side-toggle []
  [:div#side-by-side-toggle
   [b/checkbox
    {:checked   @(re-frame/subscribe [:ui.side-by-side/active?])
     :on-change #(re-frame/dispatch [:ui.side-by-side/active? (.. % -target -checked)])
     :label     "Compare Maps"}]])

(defn- current-view-map-controls
  "Set of controls to select time, emissions scenario, season, and model.
   Ordered by the question a non-expert asks: what will it be like, and how bad?

   Each parameter affects the map appearance and projection data."
  [{:keys [map-id]}]
  [:div
   {:class (str "current-view " (when map-id "dark"))}
   [when-select {:map-id map-id}]
   [:div#current-view-analysis
    [emissions-select {:map-id map-id}]
    [season-select {:map-id map-id}]
    [advanced-settings {:map-id map-id}]]
   [current-view-caption {:map-id map-id}]])

(defn- current-view
  "Control tab to select the current view of the hazard data.

   The current view is defined by the CMIP phase, model, scenario, seasonal data,
   and time period selected by the user.
   
   Side-by-side comparison can be enabled to compare two different current views."
  []
  (let [selected-tab (reagent/atom "map-1")]
    (fn []
      (let [active-hazard-layer @(re-frame/subscribe [:map.layers/active-hazard-layer])]
        (if active-hazard-layer ; only show controls when we have an active hazard layer, else show message to select a hazard layer
          [:<> (when @(re-frame/subscribe [:ui.side-by-side/active?])
                 [b/button-group {:fill true}
                  [b/button
                   {:text     "Map 1"
                    :on-click #(reset! selected-tab "map-1")
                    :active   (= @selected-tab "map-1")}]
                  [b/button
                   {:text     "Map 2"
                    :on-click #(reset! selected-tab "map-2")
                    :active   (= @selected-tab "map-2")}]])
           (if (and @(re-frame/subscribe [:ui.side-by-side/active?]) (= @selected-tab "map-2"))
             [current-view-map-controls {:map-id :map-2}] ; hardcoded second map ID
             [current-view-map-controls])
           [side-by-side-toggle]]
          [:div
           [b/non-ideal-state
            {:title       "No Data"
             :description "Select a hazard layer to configure hazard parameters."
             :icon        "info-sign"}]])))))

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
      [b/tab
       {:id    "current-view"
        :title (reagent/as-element
                [b/tooltip {:content "Configure the map layers"} "Current View"])
        :panel (reagent/as-element [current-view])}]]]))

(defn- hazard-layer-legend []
  (let [active-hazard-layer @(re-frame/subscribe [:map.layers/active-hazard-layer])]
    (when active-hazard-layer
      [:div.leaflet-bottom.leaflet-left.leaflet-touch
       [:div.hazard-layer-legend.leaflet-control [legend-display active-hazard-layer]]])))

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
       {:label "Start/Clear Region Select" :combo "r"}
       [:map.layer.selection/toggle])
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
        is-printing?       @(re-frame/subscribe [:map.print/is-printing?])
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? (seq @(re-frame/subscribe [:ui/right-sidebar]))
        loading?           @(re-frame/subscribe [:app/loading?])]
    [:div#main-wrapper.natural-hazards-atlas
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open") (when loading? " loading") (when is-printing? " map-printing"))}
     [:div#content-wrapper
      [map-component]
      [hazard-layer-legend]]

     ;; TODO: Update helper-overlay for new Seamap version (or remove?)
     [views/helper-overlay
      {:selector       ".SelectionListItem:first-child .layer-card .layer-header"
       :helperPosition "bottom"
       :helperText     "Toggle layer visibility, view info and metadata, show legend, adjust transparency, choose from download options (habitat data)"
       :padding        0}
      {:selector       ".leaflet-control-layers-toggle"
       :helperText     "Select from available basemaps"
       :helperPosition "left"}
      {:id "layer-search" :helperText "Freetext search for a specific layer by name or keywords" :helperPosition "top"}
      {:id "menu-button" :helperText (if catalogue-open? "Collapse menu sidebar" "Expand menu sidebar")}
      {:id "autosave-button" :helperText "Toggle autosave for the application"}
      {:id "zoom-in-control" :helperText "Zoom in"}
      {:id "zoom-out-control" :helperText "Zoom out"}
      {:id "print-control" :helperText "Export current map view as an image"}
      {:id "omnisearch-control" :helperText "Search all available layers in catalogue"}
      {:id "transect-control" :helperText "Draw a transect (habitat data) or take a measurement"}
      {:id "select-control" :helperText "Select a region for download (habitat data)"}
      {:id "share-control" :helperText "Create a shareable URL for current map view"}
      {:id "reset-control" :helperText "Reset the application back to its initial state"}
      {:id "shortcuts-control" :helperText "View keyboard shortcuts"}
      {:id "overlay-control" :helperText "You are here!"}
      {:selector       ".bp3-tab-panel.catalogue>.bp3-tabs>.bp3-tab-list"
       :helperText     "Filter by Hazard Layers or Supporting Layers"
       :helperPosition "bottom"
       :padding        0}
      {:id             "current-view-analysis"
       :helperText     "Choose the emissions scenario and season. Model settings are under Advanced"
       :helperPosition "bottom"}
      {:id             "time-control"
       :helperText     "Choose when: the historical baseline or a projected period"
       :helperPosition "bottom"}]
     [welcome-dialogue]
     [views/outage-message-dialogue]
     [views/settings-overlay]
     [views/info-card]
     [views/download-component]
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
