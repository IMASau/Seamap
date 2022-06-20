;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.views
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [RIGHT use-hotkeys]]
            [imas-seamap.db :refer [img-url-base]]
            [imas-seamap.interop.react :refer [css-transition-group css-transition container-dimensions sidebar sidebar-tab use-memo]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer [handler-fn handler-dispatch first-where] :include-macros true]
            [imas-seamap.components :as components]
            [imas-seamap.map.utils :refer [layer-search-keywords]]
            [goog.string :as gstring]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn with-params [url params]
  (let [u (goog/Uri. url)]
    (doseq [[k v] params]
      (.setParameterValue u (name k) v))
    (str u)))

(defn- ->query-selector
  "Normalises selectors for helper-overlay into a form for use with
  document.querySelectorAll.  Arguments may be keywords, strings, or
  maps.  Maps must contain either an :id or :selector key.  Selectors
  without a # or . at the front are treated as ids."
  [selector]
  (let [normalise-selector-str #(if-not (#{"#" "."} (first %))
                                  (str "#" %)
                                  %)]
    (if (map? selector)
      (normalise-selector-str (name (or (:id selector) (:selector selector))))
      (normalise-selector-str (name selector)))))

(defn- elem-props [selector elem]
  (when elem
    (merge
     (-> elem .getBoundingClientRect js->clj)
     (-> elem .-dataset js->clj)
     (when (map? selector) selector))))

(defn- selectors+elements [selectors]
  (->> selectors
       (map
        (fn [selector]
          (map (partial elem-props selector)
               (array-seq (.querySelectorAll js/document (->query-selector selector))))))
       (apply concat)))

(defn helper-overlay [& element-selectors]
  (let [*line-height* 17.6 *padding* 10 *text-width* 200 ;; hard-code
        *vertical-bar* 50 *horiz-bar* 100
        posn->offsets (fn [posn width height]
                        (case (name posn)
                          "top"    {:bottom (+ height *vertical-bar* *padding*)
                                    :left (- (/ width 2) (/ *text-width* 2))}
                          "bottom" {:top (+ height *vertical-bar* *padding*)
                                    :left (- (/ width 2) (/ *text-width* 2))}
                          "left"   {:right (+ width *horiz-bar* *padding*)
                                    :top (- (/ height 2) (/ *line-height* 2))}
                          "right"  {:left (+ width *horiz-bar* *padding*)
                                    :top (- (/ height 2) (/ *line-height* 2))}))
        wrapper-props (fn [posn {:keys [top left width height] :as _elem-props}]
                        (let [vertical-padding   (when (#{"top" "bottom"} posn) *padding*)
                              horizontal-padding (when (#{"left" "right"} posn) *padding*)]
                          {:width  (+ width horizontal-padding) ; allow for css padding
                           :height (+ height vertical-padding)
                           :top    (if (= posn "top")  (- top *padding*)  top)
                           :left   (if (= posn "left") (- left *padding*) left)}))
        open? @(re-frame/subscribe [:help-layer/open?])]
    [b/overlay {:is-open  open?
                :on-close #(re-frame/dispatch [:help-layer/close])}
     (when open?
       (for [{:keys [width height
                     helperText helperPosition
                     textWidth]
                    :or {helperPosition "right"}
                    :as eprops} (selectors+elements element-selectors)
             :let [posn-cls (str "helper-layer-" helperPosition)]
             :when (and eprops
                        (pos? height)
                        (not (string/blank? helperText)))]
         ^{:key (hash eprops)}
         [:div.helper-layer-wrapper {:class posn-cls
                                     :style (wrapper-props helperPosition eprops)}
          [:div.helper-layer-tooltip {:class posn-cls
                                      :style (posn->offsets helperPosition width height)}
           [:div.helper-layer-tooltiptext {:style {:width (or textWidth *text-width*)}}
            helperText]]]))]))

(defn help-button []
  [:div.layer-controls.help-button
   [b/tooltip {:content "Show the quick-help guide"}
    [:span.control.bp3-icon-large.bp3-icon-help.bp3-text-muted
     {:on-click #(re-frame/dispatch [:help-layer/open])}]]])

(defn legend-display [{:keys [legend_url server_url layer_name]}]
  ;; Allow a custom url via the legend_url field, else construct a GetLegendGraphic call:
  (let [legend-url (or legend_url
                       (with-params server_url
                         {:REQUEST "GetLegendGraphic"
                          :LAYER layer_name
                          :FORMAT "image/png"
                          :TRANSPARENT true
                          :SERVICE "WMS"
                          :VERSION "1.1.1"
                          :LEGEND_OPTIONS "forceLabels:on"}))]
    [:div.legend-wrapper
     [:img {:src legend-url}]]))

(defn catalogue-legend
  [layer {:keys [active? _errors? _loading? expanded? opacity]}]
  [b/collapse {:is-open (and active? expanded?)
               :className "layer-legend"}
   [b/slider {:label-renderer false :initial-value 0 :max 100 :value opacity
              :on-change #(re-frame/dispatch [:map.layer/opacity-changed layer %])}]
   [legend-display layer]])

(defn catalogue-header [{:keys [name] :as layer} {:keys [active? errors? loading? expanded? _opacity] :as _layer-state}]
  [b/tooltip {:content (if expanded? "Click to hide legend" "Click to show legend")
              :class "header-text"
              :disabled (not active?)}
   [:div.layer-wrapper (when active? {:class "layer-active"})
    [:div.header-text-wrapper (when (or loading? errors?) {:class "has-icons"})
     [:div (when (or loading? errors?) {:class "header-status-icons"})
      (when (and active? loading?) [b/spinner {:className "bp3-small layer-spinner"}])
      (when (and active? errors?) [:span.layer-warning.bp3-icon.bp3-icon-small.bp3-icon-warning-sign])]
     [b/clipped-text {:ellipsize true :class "header-text"
                      :on-click (handler-dispatch [:map.layer.legend/toggle layer])}
      name]]]])

(defn catalogue-controls [layer {:keys [active? _errors? _loading?] :as _layer-state}]
  [:div.catalogue-layer-controls (when active? {:class "layer-active"})
   [b/tooltip {:content "Layer info / Download data"}
    [:span.control.bp3-icon-standard.bp3-icon-info-sign.bp3-text-muted
     {:on-click (handler-dispatch [:map.layer/show-info layer])}]]
   [b/tooltip {:content "Zoom to layer"}
    [:span.control.bp3-icon-standard.bp3-icon-zoom-to-fit.bp3-text-muted
     {:on-click (handler-dispatch [:map/pan-to-layer layer])}]]
   [b/tooltip {:content (if active? "Deactivate layer" "Activate layer")}
    [b/checkbox
     {:checked (boolean active?)
      :on-change (handler-dispatch [:map/toggle-layer layer])}]]])

(defn active-layer-catalogue-controls [layer {:keys [active? visible? _errors? _loading?] :as _layer-state}]
  [:div.catalogue-layer-controls (when active? {:class "layer-active"})
   [b/tooltip {:content "Layer info / Download data"}
    [:span.control.bp3-icon-standard.bp3-icon-info-sign.bp3-text-muted
     {:on-click (handler-dispatch [:map.layer/show-info layer])}]]
   [b/tooltip {:content "Zoom to layer"}
    [:span.control.bp3-icon-standard.bp3-icon-zoom-to-fit.bp3-text-muted
     {:on-click (handler-dispatch [:map/pan-to-layer layer])}]]
   [b/tooltip {:content (if active? "Hide layer" "Show layer")}
    [:span.control.bp3-icon-large.bp3-text-muted
     {:class (if visible? "bp3-icon-eye-on" "bp3-icon-eye-off")
      :on-click (handler-dispatch [:map/toggle-layer-visibility layer])}]]
   [b/tooltip {:content "Deactivate layer"}
    [:span.control.bp3-icon-standard.bp3-text-muted
     {:class "bp3-icon-remove"
      :on-click (handler-dispatch [:map/toggle-layer layer])}]]])

(defn- ->sort-by [sorting-info ordering-key]
  (let [name-key-mapping (get sorting-info ordering-key)]
    ;; Sort by key first, then name (ie, return vector of [key name])
    (comp #(vector (get name-key-mapping %) %) first)))

(defn layers->nodes
  "group-ordering is the category keys to order by, eg [:organisation :data_category]"
  [layers [ordering & ordering-remainder :as group-ordering] sorting-info expanded-states id-base
   {:keys [active-layers loading-fn expanded-fn error-fn opacity-fn] :as layer-props}]
  (for [[val layer-subset] (sort-by (->sort-by sorting-info ordering) (group-by ordering layers))
        ;; sorting-info maps category key -> label -> [sort-key,id].
        ;; We use the id for a stable node-id:
        :let [sorting-id (get-in sorting-info [ordering val 1] "nil")
              id-str (str id-base "|" sorting-id)]]
    {:id id-str
     :label (or val "Ungrouped") ; (Implicit assumption that the group-by value is a string)
     :isExpanded (get expanded-states id-str false)
     :childNodes (if (seq ordering-remainder)
                   (layers->nodes layer-subset (rest group-ordering) sorting-info expanded-states id-str layer-props)
                   (map-indexed
                    (fn [i layer]
                      (let [layer-state {:active?   (some #{layer} active-layers)
                                         :loading?  (loading-fn layer)
                                         :expanded? (expanded-fn layer)
                                         :errors?   (error-fn layer)
                                         :opacity  (opacity-fn layer)}]
                        {:id (str id-str "-" i)
                         :className (when (:active? layer-state) "layer-active")
                         :label (reagent/as-element
                                 [:div.layer-wrapper
                                  [:div.header-row.height-static
                                   [catalogue-header layer layer-state]
                                   [catalogue-controls layer layer-state]]
                                  [catalogue-legend layer layer-state]])
                        ;; A hack, but if we just add the layer it gets
                        ;; warped in the js->clj conversion
                        ;; (specifically, values that were keywords become strings)
                         ;; :do-layer-toggle #(re-frame/dispatch [:map/toggle-layer layer])
                         #_:secondaryLabel #_(reagent/as-element [catalogue-controls layer layer-state])}))
                    layer-subset))}))

(defn layer-catalogue-tree [_layers _ordering _id _layer-props group]
  (let [expanded-states (re-frame/subscribe [:ui.catalogue/nodes group])
        sorting-info (re-frame/subscribe [:sorting/info])
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (re-frame/dispatch [:ui.catalogue/toggle-node group (:id node)])))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (re-frame/dispatch [:ui.catalogue/toggle-node group (:id node)])))
        on-click (fn [node]
                   (let [{:keys [childNodes id]} (js->clj node :keywordize-keys true)]
                     (when (seq childNodes)
                       ;; If we have children, toggle expanded state, else add to map
                       (re-frame/dispatch [:ui.catalogue/toggle-node group id]))))]
    (fn [layers ordering id layer-props]
     [:div.tab-body.layer-controls {:id id}
      [b/tree {:contents (layers->nodes layers ordering @sorting-info @expanded-states id layer-props)
               :onNodeCollapse on-close
               :onNodeExpand on-open
               :onNodeClick on-click}]])))

(defn layer-catalogue [layers layer-props group]
  (let [selected-tab @(re-frame/subscribe [:ui.catalogue/tab group])
        select-tab   #(re-frame/dispatch [:ui.catalogue/select-tab group %1])]
    [:div.height-managed
     [b/tabs {:selected-tab-id selected-tab
              :on-change       select-tab
              :class      "group-scrollable height-managed"}
      [b/tab
       {:id    "cat"
        :title "By Category"
        :panel (reagent/as-element
                [layer-catalogue-tree layers [:data_classification] "cat" layer-props group])}]
      [b/tab
       {:id    "org"
        :title "By Organisation"
        :panel (reagent/as-element
                [layer-catalogue-tree layers [:organisation :data_classification] "org" layer-props group])}]]]))

(defn transect-toggle []
  (let [{:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (cond
                               drawing? [:transect.draw/disable "Cancel Transect"]
                               query    [:transect.draw/clear "Clear Transect"]
                               :else    [:transect.draw/enable  "Draw Transect"])]
    [:div#transect-btn-wrapper {:data-helper-text "Click to draw a transect"}
     [b/button {:id "transect-button"
                :icon "edit"
                :class "bp3-fill draw-transect height-static"
                :on-click (handler-dispatch [dispatch-key])
                :text label}]]))

(defn selection-button []
  (let [{:keys [selecting? region]} @(re-frame/subscribe [:map.layer.selection/info])
        [dispatch-key label]        (cond
                                      selecting? [:map.layer.selection/disable "Cancel Selecting"]
                                      region     [:map.layer.selection/clear   "Clear Selection"]
                                      :else      [:map.layer.selection/enable  "Select Region"])]
    [:div#select-btn-wrapper {:data-helper-text "Click to select a region"}
     [b/button {:id         "transect-button"
                :icon       "widget"
                :class "bp3-fill select-region height-static"
                :on-click   (handler-dispatch [dispatch-key])
                :text       label}]]))

(defn layer-logic-toggle []
  (let [{:keys [type trigger]} @(re-frame/subscribe [:map.layers/logic])
        user-triggered?        (= trigger :map.logic.trigger/user)
        [checked? label icon]  (if (= type :map.layer-logic/automatic)
                                 [true  " Automatic Layer Selection" [:i.fa.fa-magic]]
                                 [false " Choose Layers Manually"    [:span.bp3-icon-standard.bp3-icon-hand]])]
    [:div#logic-toggle.logic-toggle
     (merge {:data-helper-text "Automatic layer selection picks the best layers to display, or turn off to list all available layers and choose your own"
             :data-text-width "380px"}
            (when-not (or checked? user-triggered?)
              {:class "external-trigger"}))
     [b/switch {:checked   checked?
                :label     (reagent/as-element [:span icon label])
                :on-change (handler-dispatch [:map.layers.logic/toggle true])}]]))

(defn layer-logic-toggle-button []
  (let [{:keys [type]} @(re-frame/subscribe [:map.layers/logic])]
    [:div#logic-toggle
     {:data-helper-text "Automatic layer selection picks the best layers to display, or turn off to list all available layers and choose your own"
      :data-text-width "380px"}
     (if (= type :map.layer-logic/automatic)
       [b/button
        {:on-click  #(re-frame/dispatch [:map.layers.logic/toggle true])}
        [:span
         [:i.fa.fa-magic]
         "Automatic Layer Selection"]]
       [b/button
        {:icon "hand"
         :text "Choose Layers Manually"
         :on-click  #(re-frame/dispatch [:map.layers.logic/toggle true])}])]))

(defn layer-search-filter []
  (let [filter-text (re-frame/subscribe [:map.layers/filter])]
    [:div.bp3-input-group {:data-helper-text "Filter Layers"}
     [:span.bp3-icon.bp3-icon-search]
     [:input.bp3-input.bp3-round {:id          "layer-search"
                                :type        "search"
                                :placeholder "Search Layers..."
                                :value       @filter-text
                                :on-change   (handler-dispatch
                                               [:map.layers/filter (.. event -target -value)])}]]))


(defn layer-card [layer-spec {:keys [active? _loading? _errors? _expanded? _opacity-fn] :as other-props}]
  [:div.layer-wrapper.bp3-card.bp3-elevation-1
   {:class (when active? "layer-active bp3-interactive")}
   [:div.header-row.height-static
    [catalogue-header layer-spec other-props]
    [catalogue-controls layer-spec other-props]]
   [catalogue-legend layer-spec other-props]])

(defn active-layer-card [layer-spec {:keys [_active? _visible? _loading? _errors? _expanded? _opacity-fn] :as other-props}]
  [:div.layer-wrapper.bp3-card.bp3-elevation-1.layer-active.bp3-interactive
   [:div.header-row.height-static
    [catalogue-header layer-spec other-props]
    [active-layer-catalogue-controls layer-spec other-props]]
   [catalogue-legend layer-spec other-props]])

(defn layer-group [{:keys [expanded] :or {expanded false} :as _props} _layers _active-layers _loading-fn _error-fn _expanded-fn _opacity-fn]
  (let [expanded (reagent/atom expanded)]
    (fn [{:keys [id title classes] :as props} layers active-layers loading-fn error-fn expanded-fn opacity-fn]
      [:div.layer-group.height-managed
       (merge {:class (str classes (if @expanded " expanded" " collapsed"))}
              (when id {:id id}))
       [:h1.bp3-heading {:on-click (handler-fn (swap! expanded not))}
        [:span.bp3-icon-standard {:class (if @expanded "bp3-icon-chevron-down" "bp3-icon-chevron-right")}]
        (str title " (" (count layers) ")")]
       [b/collapse {:is-open               @expanded
                    :keep-children-mounted true
                    :className             "height-managed"}
        (when-let [extra-component (:extra-component props)]
          extra-component)
        [:div.height-managed.group-scrollable
         (for [layer layers]
           ^{:key (:layer_name layer)}
           [layer-card layer {:active?   (some #{layer} active-layers)
                              :loading?  (loading-fn layer)
                              :errors?   (error-fn layer)
                              :expanded? (expanded-fn layer)
                              :opacity   (opacity-fn layer)}])]]])))

(defn active-layer-selection-list
  [{:keys [layers visible-layers loading-fn error-fn expanded-fn opacity-fn]}]
  (let [layer-card-items
        (map
         (fn [layer]
           {:key (:layer_name layer)
            :content [active-layer-card layer
                      {:active?   true
                       :visible?  (some #{layer} visible-layers)
                       :loading?  (loading-fn layer)
                       :errors?   (error-fn layer)
                       :expanded? (expanded-fn layer)
                       :opacity   (opacity-fn layer)}]})
         layers)]
    [components/items-selection-list
     {:items       layer-card-items
      :disabled    false
      :data-path   [:map :active-layers]
      :is-reversed true}]))

(defn active-layer-group
  [layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
  [:div.active-layer-group.height-managed
   [:h1.bp3-heading
    (str "Layers (" (count layers) ")")]
   [:div.height-managed.group-scrollable
    [active-layer-selection-list
     {:layers         layers
      :visible-layers visible-layers
      :loading-fn     loading-fn
      :error-fn       error-fn
      :expanded-fn    expanded-fn
      :opacity-fn     opacity-fn}]]])

(defn settings-controls []
  [:div#settings
   [b/button {:id         "reset-button"
              :icon       "undo"
              :class "bp3-fill"
              :on-click   (handler-dispatch [:re-boot])
              :text       "Reset Interface"}]])

(defn plot-component-animatable [{:keys [on-add on-remove]
                                  :or   {on-add identity on-remove identity}
                                  :as   _props}
                                 _child-component
                                 _child-props]
  (reagent/create-class
   {:display-name           "plot-component-animatable"
    :component-will-unmount on-remove
    :component-did-mount    on-add
    :reagent-render
    (fn [_props child-component child-props]
      [:div.plot-container
       [container-dimensions {:monitor-height true
                              :no-placeholder true}
        #(reagent/as-element [child-component
                              (merge child-props
                                     (js->clj % :keywordize-keys true))])]])}))

(defn plot-component []
  (let [show-plot (re-frame/subscribe [:transect.plot/show?])
        force-resize #(js/window.dispatchEvent (js/Event. "resize"))
        transect-results (re-frame/subscribe [:transect/results])]
    (fn []
      [:footer#plot-footer
       {:on-click (handler-dispatch [:transect.plot/toggle-visibility])
        :data-helper-text "This shows the habitat data along a bathymetry transect you can draw"
        :data-helper-position "top"}
       [:div.drag-handle [:span.bp3-icon-large.bp3-icon-drag-handle-horizontal]]
       [css-transition-group
        (when @show-plot
          [css-transition {:class-names "plot-height"
                           :timeout    {:enter 300 :exit 300}}
           [plot-component-animatable {:on-add force-resize :on-remove force-resize}
            transect-display-component (assoc @transect-results
                                              :on-mousemove
                                              #(re-frame/dispatch [:transect.plot/mousemove %])
                                              :on-mouseout
                                              #(re-frame/dispatch [:transect.plot/mouseout]))]])]])))

(defn loading-display []
  (let [loading?  @(re-frame/subscribe [:app/loading?])
        main-msg  @(re-frame/subscribe [:app/load-normal-msg])
        error-msg @(re-frame/subscribe [:app/load-error-msg])]
    (when loading?
      [:div.loading-splash {:class (when error-msg "load-error")}
       (if error-msg
         [b/non-ideal-state
          {:title       error-msg
           :description "We were unable to load everything we need to get started.  Please try again later."
           :icon      "error"}]
         [b/non-ideal-state
          {:title  main-msg
           :icon (reagent/as-element [b/spinner {:intent "success"}])}])])))

(defn welcome-dialogue []
  (let [open? @(re-frame/subscribe [:welcome-layer/open?])]
    [b/dialogue {:title      "Welcome to Seamap Australia!"
                 :class "welcome-splash"
                 :is-open    open?
                 :on-close   #(re-frame/dispatch [:welcome-layer/close])}
     [:div#welcome-splash.bp3-dialog-body
      [:p "Seamap Australia is a nationally synthesised product of
      seafloor habitat data collected from various stakeholders around
      Australia. Source datasets were reclassified according to a
      newly-developed national marine benthic habitat classification
      scheme, and synthesised to produce a single standardised GIS
      data layer of Australian benthic marine habitats."]

      [:p [:i "Seamap Australia would not have been possible without the
      collaboration of its stakeholders, including (but not limited
      to): The University of Queensland, The University of Western
      Australia, The University of Tasmania, James Cook University,
      Griffith University, Deakin University, CSIRO, Geoscience
      Australia, Great Barrier Reef Marine Park Authority (GBRMPA),
      the National Environmental Science Program (NESP), and all State
      Governments."]]

      [:p "Please cite as Lucieer V, Walsh P, Flukes E, Butler C,Proctor R, Johnson C (2017). "
       [:i "Seamap Australia - a national seafloor habitat classification scheme."]
       " Institute for Marine and Antarctic Studies (IMAS), University of Tasmania (UTAS)."]]
     [:div.bp3-dialog-footer
      [:div.bp3-dialog-footer-actions
       [b/button {:text       "Get Started!"
                  :intent     b/INTENT-PRIMARY
                  :auto-focus true
                  :on-click   (handler-dispatch [:welcome-layer/close])}]]]]))

(defn metadata-record [_props]
  (let [expanded (reagent/atom false)]
    (fn  [{:keys [license-name license-link license-img constraints other]
           {:keys [category organisation name metadata_url server_url layer_name]} :layer
           :as _layer-info}]
      [:div.metadata-record {:class (clojure.core/name category)}
       [:div.metadata-header.clearfix.section
        (when-let [logo (:logo @(re-frame/subscribe [:map/organisations organisation]))]
          [:img.metadata-img.org-logo {:class (string/replace logo #"\..+$" "")
                                       :src        (str img-url-base logo)}])
        [:h3.bp3-heading name]]
       [:h6.bp3-heading.metadata-subheader "Citation Information:"]
       [:div.section
        [:p.citation  constraints]]
       (when (seq other)
         [:div.section
          [:h6.bp3-heading.metadata-subheader "Usage:"]
          (map-indexed (fn [i o] (when o ^{:key i} [:p.other-constraints o])) other)])
       [:h6.bp3-heading.clickable {:on-click (handler-fn (swap! expanded not))}
        [:span.bp3-icon-standard {:class (if @expanded "bp3-icon-chevron-down" "bp3-icon-chevron-right")}]
        "API Access"]
       [b/collapse {:is-open               @expanded
                    :keep-children-mounted true
                    :className             "height-managed"}
        [:p "You can access the data online at"]
        [:div.server-info.section
         [:span "WMS:"]
         [:span.server-url [:a {:href server_url} server_url]]
         [:span.server-layer layer_name]]]
       [:div.license-info.clearfix.section
        [:h6.bp3-heading "License Information:"]
        (when license-img [:img.license.metadata-img {:src license-img}])
        [:a {:href license-link :target "_blank"} license-name]]
       [:div.more-info.section
        [:a {:href metadata_url :target "_blank"} "Click here for the full metadata record."]]
       [:div.section
        [:p.download-instructions
         "Downloading implies acceptance of all citation and usage requirements."]]])))

(defn- download-menu [{:keys [title disabled? layer bbox]}]
  [b/popover {:position           b/BOTTOM
              :is-disabled        disabled?
              :popover-class-name "bp3-minimal"
              :content            (reagent/as-element
                                   [b/menu
                                    [b/menu-item {:text     "GeoTIFF"
                                                  :label    (reagent/as-element [b/icon {:icon "globe"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/geotiff])}]
                                    [b/menu-item {:text     "SHP File"
                                                  :label    (reagent/as-element [b/icon {:icon "polygon-filter"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/shp])}]
                                    [b/menu-item {:text     "CSV"
                                                  :label    (reagent/as-element [b/icon {:icon "th"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/csv])}]])}
   [b/button {:text       title
              :disabled   disabled?
              :right-icon "caret-down"}]])

(defn info-card []
  (let [layer-info                       @(re-frame/subscribe [:map.layer/info])
        {:keys [metadata_url] :as layer} (:layer layer-info)
        title                            (or (get-in layer-info [:layer :name]) "Layer Information")
        {:keys [region]}                 @(re-frame/subscribe [:map.layer.selection/info])]
    [b/dialogue {:title    title
                 :is-open  (and layer-info (not (:hidden? layer-info)))
                 :on-close #(re-frame/dispatch [:map.layer/close-info])}
     [:div.bp3-dialog-body
      (case layer-info
        :display.info/loading
        [b/non-ideal-state
         {:title "Loading Metadata..."
          :icon  (reagent/as-element [b/spinner {:intent "success"}])}]

        :display.info/error
        [b/non-ideal-state
         {:title       "Error"
          :description "Unable to load metadata record.  Please try again later."
          :icon        "error"}]

        [metadata-record layer-info])]
     [:div.bp3-dialog-footer
      [:div.bp3-dialog-footer-actions
       (when (and metadata_url (re-matches #"^https://metadata\.imas\.utas\.edu\.au/geonetwork/srv/eng/catalog.search#/metadata/[0-9a-f]{8}\-[0-9a-f]{4}\-4[0-9a-f]{3}\-[89ab][0-9a-f]{3}\-[0-9a-f]{12}$" metadata_url))
         [:div
          [download-menu {:title     "Download Selection..."
                          :layer     layer
                          :disabled? (nil? region)
                          :bbox      region}]
          [download-menu {:title "Download All..."
                          :layer layer}]])
       [b/button {:text       "Close"
                  :auto-focus true
                  :intent     b/INTENT-PRIMARY
                  :on-click   (handler-dispatch [:map.layer/close-info])}]]]]))

(defn- as-icon [icon-name description]
  (reagent/as-element [b/tooltip {:content  description
                                  :position RIGHT}
                       [:span.bp3-icon-standard {:class (str "bp3-icon-" icon-name)}]]))

;; Unused
#_(defn layer-tab [layers active-layers loading-fn error-fn expanded-fn opacity-fn]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [selection-button]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-group {:expanded true :title "Layers"} layers active-layers loading-fn error-fn expanded-fn opacity-fn]
   [help-button]])

;; Unused
#_(defn catalogue-layer-tab [layers active-layers loading-fn error-fn expanded-fn opacity-fn group]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [selection-button]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-catalogue layers
    {:active-layers active-layers
     :loading-fn    loading-fn
     :error-fn      error-fn
     :expanded-fn   expanded-fn
     :opacity-fn    opacity-fn}
    group]
   [help-button]])

;; Unused
#_(defn management-layer-tab [boundaries habitat-layer active-layers loading-fn error-fn expanded-fn opacity-fn]
  [:div.sidebar-tab.height-managed
   [:div.boundary-layers.height-managed.group-scrollable
    [:h6 "Boundary Layers"]
    (for [layer boundaries]
      ^{:key (:layer_name layer)}
      [layer-card layer {:active?   (some #{layer} active-layers)
                         :loading?  (loading-fn layer)
                         :errors?   (error-fn layer)
                         :expanded? (expanded-fn layer)
                         :opacity   (opacity-fn layer)}])]
   [:label.bp3-label.height-managed
    "Habitat layer for region statistics (only one active layer may be selected at a time):"
    [b/popover {:position           b/BOTTOM
                :class         "full-width"
                :popover-class-name "bp3-minimal"
                :content            (reagent/as-element
                                     [b/menu
                                      (for [layer (filter #(= :habitat (:category %)) active-layers)]
                                        ^{:key (:layer_name layer)}
                                        [b/menu-item {:text     (:name layer)
                                                      :on-click #(re-frame/dispatch [:map.region-stats/select-habitat layer])}])])}
     [b/button {:text       (get habitat-layer :name "Select Habitat Layer for statistics...")
                :class "bp3-fill bp3-text-overflow-ellipsis"
                :intent     (when-not habitat-layer b/INTENT-WARNING)
                :right-icon "caret-down"}]]]
   [:div.bp3-callout.bp3-icon-help.height-managed
    [:h5 "Hints"]
    [:p "Choose a management boundary and select a habitat layer for
    spatial summaries. Note that only visible habitat layers will be
    available for selection."]

    [:p "Click on a management boundary (on the map) to generate
    habitat statistics for that region, and to download the subsetted
    benthic habitat data."]]
   [help-button]])

(defn active-layers-tab
  [layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
  [:div.sidebar-tab.height-managed
   [active-layer-group layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
   [help-button]])

(defn catalogue-layers-button
  [{:keys [category]}]
  (let [title (:display_name category)]
    [b/button
     {:icon     "more"
      :text     title
      :on-click #(re-frame/dispatch [:drawer-panel-stack/open-catalogue-panel (:name category)])}]))

(defn base-panel
  []
  (let [categories @(re-frame/subscribe [:map/display-categories])]
    {:content
     [:div
      [:div.drawer-group
       [:h1.bp3-heading.bp3-icon-settings
        "Controls"]
       [transect-toggle]
       [selection-button]
       [layer-logic-toggle-button]]
      [:div.drawer-group
       [:h1.bp3-heading.bp3-icon-list-detail-view
        "Catalogue Layers"]
       (for [category categories]
         [catalogue-layers-button
          {:key      (:name category)
           :category category}])]
      [:div.drawer-group
       [:h1.bp3-heading.bp3-icon-cog
        "Settings"]
       [b/button
        {:icon     "undo"
         :text     "Reset Interface"
         :on-click   #(re-frame/dispatch [:re-boot])}]]]}))

;; Unused
#_(defn layer-panel
  [{:keys [title layers active-layers loading-layers error-layers expanded-layers layer-opacities]}]
  {:title title
   :content
   [:div.sidebar-tab.height-managed
    [layer-search-filter]
    [layer-group {:expanded true :title "Layers"} layers active-layers loading-layers error-layers expanded-layers layer-opacities]]})

;; Unused
#_(defn management-layers-panel
  [{:keys [layers habitat-layer active-layers loading-layers error-layers expanded-layers layer-opacities]}]
  {:title "Management Region Layers"
   :content
   [:div.sidebar-tab.height-managed
    [:div.boundary-layers.height-managed.group-scrollable.layer-group
     [:h1.bp3-heading "Boundary Layers"]
     (for [layer layers]
       ^{:key (:layer_name layer)}
       [layer-card layer {:active?   (some #{layer} active-layers)
                          :loading?  (loading-layers layer)
                          :errors?   (error-layers layer)
                          :expanded? (expanded-layers layer)
                          :opacity   (layer-opacities layer)}])]
    [:div
     [:label.bp3-label.height-managed
      "Habitat layer for region statistics (only one active layer may be selected at a time):"
      [b/popover {:position           b/BOTTOM
                  :class         "full-width"
                  :popover-class-name "bp3-minimal"
                  :content            (reagent/as-element
                                       [b/menu
                                        (for [layer (filter #(= :habitat (:category %)) active-layers)]
                                          ^{:key (:layer_name layer)}
                                          [b/menu-item {:text     (:name layer)
                                                        :on-click #(re-frame/dispatch [:map.region-stats/select-habitat layer])}])])}
       [b/button {:text       (get habitat-layer :name "Select Habitat Layer for statistics...")
                  :class "bp3-fill bp3-text-overflow-ellipsis"
                  :intent     (when-not habitat-layer b/INTENT-WARNING)
                  :right-icon "caret-down"}]]]
     [:div.bp3-callout.bp3-icon-help.height-managed
      [:h5 "Hints"]
      [:p "Choose a management boundary and select a habitat layer for
    spatial summaries. Note that only visible habitat layers will be
    available for selection."]

      [:p "Click on a management boundary (on the map) to generate
    habitat statistics for that region, and to download the subsetted
    benthic habitat data."]]]]})

(defn catalogue-layers-panel
  [{:keys [title layers active-layers loading-layers error-layers expanded-layers layer-opacities group]}]
  {:title   title
   :content
   [:div.sidebar-tab.height-managed
    [layer-search-filter]
    [layer-catalogue layers
     {:active-layers active-layers
      :loading-fn    loading-layers
      :error-fn      error-layers
      :expanded-fn   expanded-layers
      :opacity-fn    layer-opacities}
     group]]})

(defn drawer-panel-selection
  [panel map-layers]
  (let [{:keys [panel props]} panel
        {:keys [groups active-layers loading-layers error-layers expanded-layers layer-opacities]} map-layers]
    (case panel
      :drawer-panel/catalogue-layers
      (catalogue-layers-panel
       (merge
        props
        {:layers          ((:group props) groups)
         :active-layers   active-layers
         :loading-layers  loading-layers
         :error-layers    error-layers
         :expanded-layers expanded-layers
         :layer-opacities layer-opacities})))))

(defn drawer-panel-stack
  []
  (let [map-layers @(re-frame/subscribe [:map/layers])
        panels @(re-frame/subscribe [:drawer-panel-stack/panels])
        display-panels
        (map #(drawer-panel-selection % map-layers) panels)]
    [components/panel-stack
     {:panels (concat [(base-panel)] display-panels)
      :on-close #(re-frame/dispatch [:drawer-panel-stack/pop])}]))

(defn left-drawer
  []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])]
    [components/drawer
     {:title
      [:div.left-drawer-header
       [:img
        {:src "img/Seamap2_V2_RGB.png"}]]
      :position    "left"
      :size        "460px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :hasBackdrop false}
     [drawer-panel-stack]]))

(defn habitat-statistics-table
  [{:keys [habitat-statistics]}]
  [:table
   [:thead
    [:tr
     [:th "Habitat"]
     [:th "Area (km^2)"]
     [:th "Percentage (%)"]]]
   [:tbody
    (if (seq habitat-statistics)
     (for [{:keys [habitat area percentage]} habitat-statistics]
      [:tr
       {:key (or habitat "Unmapped")}
       [:td (or habitat "Unmapped")]
       [:td (gstring/format "%.2f" area)]
       [:td (gstring/format "%.2f" percentage)]])
      [:tr
       [:td
        {:colSpan 3}
        "No habitat information"]])]])

(defn boundary-selection
  []
  [:div.drawer-group
   [:h1.bp3-heading.bp3-icon-heatmap "Boundaries"]
   [components/form-group
    {:label "Network"}
    [components/select
     {:value    @(re-frame/subscribe [:map/active-network])
      :options  @(re-frame/subscribe [:map/networks])
      :onChange #(re-frame/dispatch [:map/update-active-network %])
      :keyfns
      {:id   :name
       :text :name}}]]
   [components/form-group
    {:label "Park"}
    [components/select
     {:value    @(re-frame/subscribe [:map/active-park])
      :options  @(re-frame/subscribe [:map/parks])
      :onChange #(re-frame/dispatch [:map/update-active-park %])
      :keyfns
      {:id          :name
       :text        :name
       :breadcrumbs (comp vector :network)}}]]
   [components/form-group
    {:label "Zone Category"}
    [components/select
     {:value    @(re-frame/subscribe [:map/active-zone])
      :options  @(re-frame/subscribe [:map/zones])
      :onChange #(re-frame/dispatch [:map/update-active-zone %])
      :keyfns
      {:id   :name
       :text :name}}]]
   [components/form-group
    {:label "IUCN Category (Zone)"}
    [components/select
     {:value    @(re-frame/subscribe [:map/active-zone-iucn])
      :options  @(re-frame/subscribe [:map/zones-iucn])
      :onChange #(re-frame/dispatch [:map/update-active-zone-iucn %])
      :keyfns
      {:id   :name
       :text :name}}]]])

(defn habitat-statistics
  []
  (let [selected-tab       (reagent/atom "breakdown")
        habitat-statistics @(re-frame/subscribe [:map/habitat-statistics])
        without-unmapped   (filter :habitat habitat-statistics)]
    (fn []
      [:div.drawer-group
       [:h1.bp3-heading.bp3-icon-home "Habitat Statistics"]
       [b/tabs
        {:selected-tab-id @selected-tab
         :on-change       #(reset! selected-tab %)}
        [b/tab
         {:id    "breakdown"
          :title "Breakdown"
          :panel
          (reagent/as-element
           [habitat-statistics-table
            {:habitat-statistics habitat-statistics}])}]
        [b/tab
         {:id    "chart"
          :title "Chart"
          :panel
          (reagent/as-element
           (if (seq without-unmapped)
             [components/donut-chart
              {:values       without-unmapped
               :theta        :area
               :color        :habitat
               :legend-title "Habitat"}]
             [:div "No habitat information"]))}]]])))

(defn right-drawer
  []
  [components/drawer
   {:title       "State of Knowledge"
    :position    "right"
    :size        "460px"
    :isOpen      @(re-frame/subscribe [:right-drawer/open?])
    :onClose     #(re-frame/dispatch [:right-drawer/close])
    :hasBackdrop false}
   [boundary-selection]
   [habitat-statistics]])

(defn active-layers-sidebar []
  (let [{:keys [collapsed selected] :as _sidebar-state}                            @(re-frame/subscribe [:ui/sidebar])
        {:keys [active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities]} @(re-frame/subscribe [:map/layers])]
    [sidebar {:id        "floating-sidebar"
              :selected  selected
              :collapsed collapsed
              :closeIcon (reagent/as-element [:span.bp3-icon-standard.bp3-icon-caret-left])
              :on-close  #(re-frame/dispatch [:ui.sidebar/close])
              :on-open   #(re-frame/dispatch [:ui.sidebar/open %])}
     [sidebar-tab {:header "Active Layers"
                   :icon   (as-icon "eye-open"
                                    (str "Active Layers (" (count active-layers) ")"))
                   :id     "tab-activelayers"}
      [active-layers-tab active-layers active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities]]]))

(defn floating-menu-bar []
  [:div.floating-menu-bar
   [:div.floating-menu-bar-drawer-button
    {:on-click #(re-frame/dispatch [:left-drawer/toggle])}
    [b/icon
     {:icon "menu"
      :icon-size 20}]]
   [:div.floating-menu-bar-search-button
    {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])}
    [b/icon
     {:icon "search"
      :icon-size 16}]
    "Search Layers..."]])

(defn floating-menu-active-layers
  [{:keys [active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities]}]
  [:div.floating-menu-active-layers
   [:div.header
    [b/icon
     {:icon "eye-open"
      :icon-size 18}]
    [:h1 (str "Active Layers (" (count active-layers) ")")]]
   [:div.content
    [active-layer-selection-list
     {:layers         active-layers
      :visible-layers visible-layers
      :loading-fn     loading-layers
      :error-fn       error-layers
      :expanded-fn    expanded-layers
      :opacity-fn     layer-opacities}]]])

(defn floating-menu []
  (let [{:keys [active-layers] :as map-layers} @(re-frame/subscribe [:map/layers])]
    [:div.floating-menu-positioning
     [:div.floating-menu
      [floating-menu-bar]
      (when (seq active-layers) [floating-menu-active-layers map-layers])]]))

(defn floating-pills
  []
  (let [collapsed (:collapsed @(re-frame/subscribe [:ui/sidebar]))]
    [:div
     {:class (str "floating-pills" (when collapsed " collapsed"))}
     [components/floating-pill-button
      {:text     "State of Knowledge"
       :icon     "add-column-right"
       :on-click #(re-frame/dispatch [:right-drawer/toggle])}]
     [components/floating-pill-button
      {:text     "Draw Transect"
       :icon     "edit"
       :on-click #(re-frame/dispatch [:transect.draw/toggle])}]
     [components/floating-pill-button
      {:text     "Select Region"
       :icon     "widget"
       :on-click #(re-frame/dispatch [:map.layer.selection/toggle])}]]))

(defn layers-search-omnibar
  []
  (let [categories @(re-frame/subscribe [:map/categories-map])
        open?                @(re-frame/subscribe [:layers-search-omnibar/open?])
        {:keys [all-layers]} @(re-frame/subscribe [:map/layers])]
    [components/omnibar
     {:placeholder  "Search Layers..."
      :isOpen       open?
      :onClose      #(re-frame/dispatch [:layers-search-omnibar/close])
      :items        all-layers
      :onItemSelect #(re-frame/dispatch [:map/add-layer-from-omnibar %])
      :keyfns
      {:id          :id
       :text        :name
       :breadcrumbs (fn [{:keys [category data_classification]}]
                      (map
                       #(or % "Ungrouped")
                       [(or
                         (:display_name (category categories))
                         (:name (category categories)))
                        data_classification]))
       :keywords    #(layer-search-keywords categories %)}}]))

(def hotkeys-combos
  (let [keydown-wrapper
        (fn [m keydown-v]
          (assoc m :global    true
                 :group     "Keyboard Shortcuts"
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
       {:label "Toggle Plot Panel"      :combo "p"}
       [:transect.plot/toggle-visibility])
      (keydown-wrapper
       {:label "Toggle Sidebar"         :combo "s"}
       [:ui.sidebar/toggle])
      (keydown-wrapper
       {:label "Toggle Left Drawer"     :combo "a"}
       [:left-drawer/toggle])
      (keydown-wrapper
       {:label "Toggle Right Drawer"    :combo "d"}
       [:right-drawer/toggle])
      (keydown-wrapper
       {:label "Toggle Layers Search"   :combo "shift + s"}
       [:layers-search-omnibar/toggle])
      (keydown-wrapper
       {:label "Start/Clear Transect"   :combo "t"}
       [:transect.draw/toggle])
      (keydown-wrapper
       {:label "Start/Clear Region"     :combo "r"}
       [:map.layer.selection/toggle])
      (keydown-wrapper
       {:label "Cancel"                 :combo "esc"}
       [:ui.drawing/cancel])
      (keydown-wrapper
       {:label "Toggle Layer Logic"     :combo "m"}
       [:map.layers.logic/toggle])
      (keydown-wrapper
       {:label "Start Searching Layers" :combo "/" :prevent-default true}
       [:ui.search/focus])
      (keydown-wrapper
       {:label "Reset"                  :combo "shift + r"}
       [:re-boot])
      (keydown-wrapper
       {:label "Copy Shareable URL"     :combo "c"}
       [:create-save-state])
      (keydown-wrapper
       {:label "Show Help Overlay"      :combo "h"}
       [:help-layer/toggle])])))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)]
    [:div#main-wrapper ;{:on-key-down handle-keydown :on-key-up handle-keyup}
     [:div#content-wrapper
      [map-component [floating-menu] [floating-pills]]
      [plot-component]]
     [helper-overlay
      :layer-search
      :logic-toggle
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
     [welcome-dialogue]
     [info-card]
     [loading-display]
     [left-drawer]
     [right-drawer]
     [layers-search-omnibar]]))

