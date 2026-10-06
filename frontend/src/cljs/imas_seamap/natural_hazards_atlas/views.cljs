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

(defn- shown-as
  "What the layer for year is, in words: a single historical year, or the
   20-year average around a projected year."
  [year]
  (if (nhatutils/historic-year? year)
    (str "the single year " year)
    (str "the 20-year average for " (nhatutils/centre-and-window year))))

(defn- average-for
  "The status line's readout of the projected layer for year."
  [year]
  (str "20-year average for " (nhatutils/centre-and-window year) "."))

(defn- track-label
  "Short label for the handle while dragging."
  [year]
  (if (nhatutils/historic-year? year)
    (str year)
    (let [[first-year last-year] (nhatutils/year-window year)]
      (str first-year "–" last-year))))

(defn- period-buttons
  "The agreed periods, as presets: one tap moves the year to the period and
   lights the button until the year is moved another way."
  [{:keys [map-id preset]}]
  [:div.segmented {:role "group" :aria-label "Periods"}
   (for [{:keys [id name year]} nhatutils/when-presets]
     ^{:key id}
     [:button
      {:type         "button"
       :aria-pressed (= id (:id preset))
       :class        (when (= id (:id preset)) "selected")
       :on-click     #(re-frame/dispatch [:current-view/preset id map-id])}
      [:span.segmented-name name]
      [:span.segmented-detail year]])])

(defn- year-track
  "The century as a slider: drag or click to move the handle, or use the
   keyboard. For a projected year the handle carries the 20 years its layer
   averages; a historical year is a single point. A period's year lights its
   button."
  []
  (let [drag  (reagent/atom nil) ; centre year under the pointer while dragging
        track (atom nil)]
    (fn [{:keys [map-id year preset playing?]}]
      (let [{axis-start :start-year axis-end :end-year} nhatutils/timeline-axis
            [first-year last-year] nhatutils/year-range
            pct                    #(str (* 100 (/ (- % axis-start) (- axis-end axis-start))) "%")
            year-at                (fn [e]
                                     (let [rect (.getBoundingClientRect @track)]
                                       (nhatutils/snap-year
                                        (js/Math.floor (+ axis-start (* (- axis-end axis-start) (/ (- (.-clientX e) (.-left rect)) (.-width rect))))))))
            choose                 #(re-frame/dispatch [:current-view/year % map-id])
            shown                  (or @drag year)
            [window-start window-end] (nhatutils/year-window shown)
            end-drag               (fn [_]
                                     (when-let [y @drag]
                                       (reset! drag nil)
                                       (choose y)))]
        [:div.year-track
         [:div.year-track-rail
          {:ref               #(reset! track %)
           :on-pointer-down   (fn [e]
                                (.preventDefault e)
                                (.setPointerCapture (.-currentTarget e) (.-pointerId e))
                                (reset! drag (year-at e))
                                (.focus (.querySelector (.-currentTarget e) "[role=slider]")))
           :on-pointer-move   #(when @drag (reset! drag (year-at %)))
           :on-pointer-up     end-drag
           :on-pointer-cancel end-drag}
          [:span.year-track-line]
          (for [decade (range 2020 axis-end 10)] ; so the window reads as two decades
            ^{:key decade}
            [:span.year-track-decade {:style {:left (pct decade)}}])
          (when playing? ; how far Play has come
            [:span.year-track-played {:style {:width (pct (+ shown 0.5))}}])
          (when-not (nhatutils/historic-year? shown) ; historical layers are single years: just the point
            [:span.year-track-window
             {:class (when (and preset (not @drag)) "preset")
              :style {:left  (pct window-start)
                      :width (str "calc(" (pct (inc window-end)) " - " (pct window-start) ")")}}])
          [:span.year-track-handle
           {:role           "slider"
            :tab-index      0
            :aria-label     "Year"
            :aria-valuemin  first-year
            :aria-valuemax  last-year
            :aria-valuenow  shown
            :aria-valuetext (shown-as shown)
            :style          {:left (pct (+ shown 0.5))}
            :on-key-down    (fn [e]
                              (when-let [y (case (.-key e)
                                             ("ArrowLeft" "ArrowDown") (dec year)
                                             ("ArrowRight" "ArrowUp")  (inc year)
                                             "PageDown"                (- year 10)
                                             "PageUp"                  (+ year 10)
                                             "Home"                    first-year
                                             "End"                     last-year
                                             nil)]
                                (.preventDefault e)
                                (choose y)))}
           (when @drag [:span.year-track-bubble (track-label shown)])]]
         [:div.year-track-ticks {:aria-hidden true}
          (for [tick [axis-start 2050 axis-end]]
            ^{:key tick}
            [:span {:style {:left (pct tick)}} tick])]]))))

(defn- when-select
  "One question: when? Periods are presets on one year track: tap one and the
   handle jumps to it, or move it to any year. Projected layers are 20-year
   averages, so there the handle shows the window. Play sits on the track it
   moves. One line says what the map shows."
  [{:keys [map-id]}]
  (let [preset       @(re-frame/subscribe [:current-view/preset map-id])
        year         @(re-frame/subscribe [:current-view/year map-id])
        playing?     @(re-frame/subscribe [:current-view.play/playing? map-id])
        loading?     @(re-frame/subscribe [:map.time/is-loading? map-id])
        current-time @(re-frame/subscribe [:map.time/current-time map-id])
        shown-year   (when current-time (.getFullYear (js/Date. current-time)))]
    [:section#time-control.cv-section
     {:class (cond playing? "playing" preset "on-preset" :else "custom")}
     [label-row "1 · Timeframe"]
     [period-buttons {:map-id map-id :preset preset}]
     [:div.year-row
      [:button.play-button
       {:type       "button"
        :aria-label (if playing? "Stop" "Play through the years")
        :title      (if playing? "Stop" "Play through the years")
        :class      (when playing? "playing")
        :on-click   #(re-frame/dispatch (if playing?
                                          [:current-view.play/stop map-id]
                                          [:current-view.play/start map-id]))}
       (if (and playing? loading?)
         [b/spinner {:size 14}]
         [b/icon {:icon (if playing? "stop" "play") :size 14}])]
      [year-track {:map-id map-id :year year :preset preset :playing? playing?}]]
     [:p.when-shown {:aria-live "polite"}
      (cond
        playing? ; the year Play is on, without a "Loading" between every step
        [:<> [:span.when-chip "Playing"] " " (average-for year)]

        (or (not shown-year) ; nothing yet, or the other dataset's layer until the chosen one loads
            (not= (nhatutils/historic-year? shown-year) (nhatutils/historic-year? year)))
        (str "Loading " (or (:caption preset) (nhatutils/centre-and-window year)) "…")

        ; A named state (a period, Playing) gets a label; a year set on the track doesn't
        preset [:<> [:span.when-chip (:name preset)] " " (average-for shown-year)]
        :else  (average-for shown-year))]]))

(defn- emissions-select
  "Lower or higher emissions, in plain words first with the SSP code underneath,
   and one line on what the chosen pathway means."
  [{:keys [map-id]}]
  (let [selected  @(re-frame/subscribe [:current-view/selected-scenario map-id])
        scenarios @(re-frame/subscribe [:current-view/scenarios])]
    [:section.cv-section
     [label-row "2 · Emissions pathway"]
     [:div.segmented {:role "group" :aria-label "Emissions pathway"}
      (for [{:keys [id name display_name] :as scenario} scenarios
            :let [{:keys [label code]} (get nhatutils/scenario-labels name)
                  selected? (= id (:id selected))]]
        ^{:key id}
        [:button
         {:type         "button"
          :aria-pressed selected?
          :class        (when selected? "selected")
          :on-click     #(re-frame/dispatch [:current-view/selected-scenario scenario map-id])}
         [:span.segmented-name (or label display_name)]
         [:span.segmented-detail (or code name)]])]
     (when-let [{:keys [code description]} (get nhatutils/scenario-labels (:name selected))]
       [:p.cv-statement {:aria-live "polite"} [:span.when-chip code] " " description])]))

(defn- season-select
  [{:keys [map-id]}]
  [:section.cv-section
   [label-row "3 · Time of year"]
   [components/select
    {:value    @(re-frame/subscribe [:current-view/selected-seasonal-data map-id])
     :options  (nhatutils/sort-seasons @(re-frame/subscribe [:current-view/seasonal-datas]))
     :onChange #(re-frame/dispatch [:current-view/selected-seasonal-data % map-id])
     :keyfns
     {:id   :id
      :text nhatutils/season-label}}]])

(defn- advanced-settings
  "Expert settings with sensible defaults, folded away but showing what's in use."
  []
  (let [open? (reagent/atom false)]
    (fn [{:keys [map-id]}]
      (let [model @(re-frame/subscribe [:current-view/selected-model map-id])]
        [:section.cv-section.advanced-settings
         [:button.advanced-settings-toggle
          {:type          "button"
           :aria-expanded @open?
           :on-click      #(swap! open? not)}
          "Advanced"
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
               :text :display_name}}]]
           [:p.cv-note "Model: " (:display_name model)])]))))

(defn- map-caption
  "What the map shows, in a line under the map. Makes the view defensible when
   shared."
  []
  (let [{:keys [layer emissions season model] when-text :when}
        @(re-frame/subscribe [:current-view/caption])]
    (when layer
      [:div.map-caption.leaflet-control
       [:strong layer]
       (string/join (map #(str " · " %) (remove nil? [when-text emissions season model])))])))

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
       :helperText     "Choose when: tap a period, or move along the track to any year"
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
