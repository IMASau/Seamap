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

(defn- label-row
  "A control's numbered label, with an optional note on the right."
  [label note]
  [:div.cv-label-row
   [:h3.cv-label label]
   (when note [:span.cv-label-note note])])

(defn- active-layer-heading []
  (let [{:keys [name]} @(re-frame/subscribe [:map.layers/active-hazard-layer])]
    [:header.cv-heading
     [:h2 name]
     [:p.cv-note
      "Active hazard layer · "
      [:button.cv-link
       {:type     "button"
        :on-click #(re-frame/dispatch [:left-drawer/tab "catalogue"])}
       "change in catalogue"]]]))

(defn- year-span
  "\"2050–69\" style years for a window."
  [start-year]
  (let [end-year (nhatutils/window-end-year start-year)]
    (if (= (quot start-year 100) (quot end-year 100))
      (str start-year "–" (.padStart (str (mod end-year 100)) 2 "0"))
      (str start-year "–" end-year))))

(defn- when-select
  "One control for one question: when? Pick a named period, or drag the 20-year
   window along the track to any 20 years. Watch change sweeps the window across
   the century."
  []
  (let [drag  (reagent/atom nil) ; {:start year :offset years} while dragging
        track (atom nil)]
    (fn [{:keys [map-id]}]
      (let [stored    @(re-frame/subscribe [:current-view/window-start map-id])
            watching? @(re-frame/subscribe [:current-view.window/playing? map-id])
            start     (or (:start @drag) stored)
            end       (nhatutils/window-end-year start)
            {axis-start :start-year axis-end :end-year} nhatutils/timeline-axis
            [first-projected last-projected] nhatutils/projected-window-starts
            pct       #(* 100 (/ (- % axis-start) (- axis-end axis-start)))
            year-at   (fn [client-x]
                        (let [rect (.getBoundingClientRect @track)]
                          (+ axis-start (* (- axis-end axis-start) (/ (- client-x (.-left rect)) (.-width rect))))))
            choose    #(re-frame/dispatch [:current-view/window-start % map-id])
            end-drag  (fn [_]
                        (when-let [{:keys [start]} @drag]
                          (reset! drag nil)
                          (choose start)))]
        [:section#time-control.cv-section
         [label-row "1 · When"
          [b/tooltip {:content "Each map averages 20 years, which smooths out year-to-year swings."}
           [:span [b/icon {:icon "info-sign" :size 12}] " 20-year averages"]]]
         [:div.segmented {:role "group" :aria-label "Periods"}
          (for [{:keys [id name start-year]} nhatutils/when-presets]
            ^{:key id}
            [:button
             {:type         "button"
              :aria-pressed (= start-year start)
              :class        (when (= start-year start) "selected")
              :on-click     #(choose start-year)}
             [:span.segmented-name name]
             [:span.segmented-detail (year-span start-year)]])]
         [:div.when-track
          {:ref             #(reset! track %)
           :on-pointer-down (fn [e] ; click the track to centre the window there
                              (when (= (.-target e) (.-currentTarget e))
                                (choose (nhatutils/snap-window-start (js/Math.round (- (year-at (.-clientX e)) 10))))))}
          [:span.when-track-line]
          [:span.when-track-past {:style {:width (str (pct first-projected) "%")}}]
          [:div.when-window
           {:role            "slider"
            :tab-index       0
            :aria-label      "20-year window"
            :aria-valuemin   nhatutils/recent-window-start
            :aria-valuemax   last-projected
            :aria-valuenow   start
            :aria-valuetext  (str start " to " end)
            :class           (when @drag "dragging")
            :style           {:left  (str (pct start) "%")
                              :width (str (- (pct (+ start nhatutils/window-years)) (pct start)) "%")}
            :on-pointer-down (fn [e]
                               (.preventDefault e)
                               (.setPointerCapture (.-currentTarget e) (.-pointerId e))
                               (reset! drag {:start start :offset (- (year-at (.-clientX e)) start)}))
            :on-pointer-move (fn [e]
                               (when-let [{:keys [offset]} @drag]
                                 (swap! drag assoc :start
                                        (nhatutils/snap-window-start (js/Math.round (- (year-at (.-clientX e)) offset))))))
            :on-pointer-up     end-drag
            :on-pointer-cancel end-drag
            :on-key-down
            (fn [e]
              (let [historic? (nhatutils/historic-window? start)
                    next      (case (.-key e)
                                ("ArrowLeft" "ArrowDown") (if (= start first-projected) nhatutils/recent-window-start (dec start))
                                ("ArrowRight" "ArrowUp")  (if historic? first-projected (inc start))
                                "PageDown"                (- start 10)
                                "PageUp"                  (if historic? first-projected (+ start 10))
                                "Home"                    nhatutils/recent-window-start
                                "End"                     last-projected
                                nil)]
                (when next
                  (.preventDefault e)
                  (choose (nhatutils/snap-window-start next)))))}
           [:span.when-window-years (year-span start)]]]
         [:div.when-ticks {:aria-hidden true}
          (for [year [axis-start first-projected 2050 axis-end]]
            ^{:key year}
            [:span {:style {:left (str (pct year) "%")}} year])]
         [:div.when-footer
          [:span.cv-note "Drag the window to pick any 20 years"]
          [:button.watch-change
           {:type     "button"
            :class    (when watching? "playing")
            :on-click #(re-frame/dispatch (if watching?
                                            [:current-view.window/stop map-id]
                                            [:current-view.window/play map-id]))}
           [b/icon {:icon (if watching? "stop" "play") :size 12}]
           (if watching? "Stop" "Watch change")]]]))))

(defn- emissions-select
  "Lower or higher emissions, in plain words first with the SSP code underneath.
   Doesn't apply to the recent past."
  [{:keys [map-id]}]
  (let [is-historic? @(re-frame/subscribe [:current-view/is-historic? map-id])
        selected     @(re-frame/subscribe [:current-view/selected-scenario map-id])
        scenarios    @(re-frame/subscribe [:current-view/scenarios])]
    [:section.cv-section
     [label-row "2 · Emissions future" (when is-historic? "Not used for the recent past")]
     [:div.segmented {:role "group" :aria-label "Emissions future"}
      (for [{:keys [id name display_name] :as scenario} scenarios
            :let [{:keys [label code]} (get nhatutils/scenario-labels name)
                  selected? (and (not is-historic?) (= id (:id selected)))]]
        ^{:key id}
        [:button
         {:type         "button"
          :aria-pressed selected?
          :class        (when selected? "selected")
          :disabled     is-historic?
          :on-click     #(re-frame/dispatch [:current-view/selected-scenario scenario map-id])}
         [:span.segmented-name (or label display_name)]
         [:span.segmented-detail (or code name)]])]]))

(defn- season-select
  [{:keys [map-id]}]
  [:section.cv-section
   [label-row "3 · Season"]
   [components/select
    {:value    @(re-frame/subscribe [:current-view/selected-seasonal-data map-id])
     :options  @(re-frame/subscribe [:current-view/seasonal-datas])
     :onChange #(re-frame/dispatch [:current-view/selected-seasonal-data % map-id])
     :keyfns
     {:id   :id
      :text :display_name}}]])

(defn- advanced-settings
  "Expert settings with sensible defaults, folded away but showing what's in use."
  []
  (let [open? (reagent/atom false)]
    (fn [{:keys [map-id]}]
      (let [cmip-phase @(re-frame/subscribe [:current-view/selected-cmip-phase map-id])
            model      @(re-frame/subscribe [:current-view/selected-model map-id])]
        [:section.cv-section.advanced-settings
         [:button.advanced-settings-toggle
          {:type          "button"
           :aria-expanded @open?
           :on-click      #(swap! open? not)}
          "Advanced: model and dataset"
          [b/icon {:icon (if @open? "chevron-up" "chevron-down") :size 14}]]
         (if @open?
           [:div.advanced-settings-body
            [:h4.cv-sublabel "Model"]
            [components/select
             {:value    model
              :options  @(re-frame/subscribe [:current-view/models])
              :onChange #(re-frame/dispatch [:current-view/selected-model % map-id])
              :keyfns
              {:id   :id
               :text :display_name}}]
            [:h4.cv-sublabel "Dataset"]
            [components/select
             {:value    cmip-phase
              :options  @(re-frame/subscribe [:current-view/cmip-phases])
              :onChange #(re-frame/dispatch [:current-view/selected-cmip-phase % map-id])
              :keyfns
              {:id   :id
               :text :display_name}}]]
           [:p.cv-note (:display_name model) " · " (:display_name cmip-phase)])]))))

(defn- map-caption
  "What the map shows, in a line under the map. Makes the view defensible when
   shared."
  []
  (let [{:keys [layer period years emissions season model]}
        @(re-frame/subscribe [:current-view/caption])]
    (when layer
      [:div.map-caption.leaflet-control
       [:strong layer] " · " period ", average of " years
       (string/join (map #(str " · " %) (remove nil? [emissions season model])))])))

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
   [active-layer-heading]
   [when-select {:map-id map-id}]
   [:div#current-view-analysis
    [emissions-select {:map-id map-id}]
    [season-select {:map-id map-id}]
    [advanced-settings {:map-id map-id}]]])

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
      [:div.leaflet-bottom.leaflet-left.leaflet-touch.hazard-layer-legend-row
       [:div.hazard-layer-legend.leaflet-control [legend-display active-hazard-layer]]
       [map-caption]])))

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
       :helperText     "Choose the emissions scenario and season. Model choices are under Model settings"
       :helperPosition "bottom"}
      {:id             "time-control"
       :helperText     "Choose when: a named period, or drag the 20-year window"
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
