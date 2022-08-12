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
            [imas-seamap.state-of-knowledge.views :refer [state-of-knowledge floating-state-of-knowledge-pill floating-boundaries-pill floating-zones-pill]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer [handler-fn handler-dispatch] :include-macros true]
            [imas-seamap.components :as components]
            [imas-seamap.map.utils :refer [layer-search-keywords]]
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

;; TODO: Update, replace?
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

;; TODO: Update, replace?
#_(defn help-button []
  [:div.layer-controls.help-button
   [b/tooltip {:content "Show the quick-help guide"}
    [:span.control.bp3-icon-large.bp3-icon-help.bp3-text-muted
     {:on-click #(re-frame/dispatch [:help-layer/open])}]]])

(defn legend-display [{:keys [legend_url server_url layer_name style]}]
  ;; Allow a custom url via the legend_url field, else construct a GetLegendGraphic call:
  (let [legend-url (or legend_url
                       (with-params server_url
                         (merge
                          {:REQUEST "GetLegendGraphic"
                           :LAYER layer_name
                           :FORMAT "image/png"
                           :TRANSPARENT true
                           :SERVICE "WMS"
                           :VERSION "1.1.1"
                           :LEGEND_OPTIONS "forceLabels:on"}
                          (when style {:STYLE style}))))]
    [:div.legend-wrapper
     [:img {:src legend-url}]]))

(defn catalogue-legend
  [layer {:keys [active? _errors? _loading? expanded? opacity]}]
  [b/collapse {:is-open (and active? expanded?)
               :className "layer-legend"}
   [b/slider {:label-renderer false :initial-value 0 :max 100 :value opacity
              :on-change #(re-frame/dispatch [:map.layer/opacity-changed layer %])}]
   [legend-display layer]])

(defn catalogue-header [{:keys [name] :as layer} {:keys [active? visible? errors? loading? expanded? _opacity] :as _layer-state}]
  [b/tooltip {:content (if expanded? "Click to hide legend" "Click to show legend")
              :class "header-text"
              :disabled (not active?)}
   [:div.layer-wrapper (when active? {:class "layer-active"})
    [:div.header-text-wrapper (when (or loading? errors?) {:class "has-icons"})
     (when (and active? visible? (or loading? errors?))
       [:div.header-status-icons
        (when loading? [b/spinner {:className "bp3-small layer-spinner"}])
        (when errors? [:span.layer-warning.bp3-icon.bp3-icon-small.bp3-icon-warning-sign])])
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
   [b/tooltip {:content (if visible? "Hide layer" "Show layer")}
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
   {:keys [active-layers visible-layers loading-fn expanded-fn error-fn opacity-fn] :as layer-props} open-all?]
  (for [[val layer-subset] (sort-by (->sort-by sorting-info ordering) (group-by ordering layers))
        ;; sorting-info maps category key -> label -> [sort-key,id].
        ;; We use the id for a stable node-id:
        :let [sorting-id (get-in sorting-info [ordering val 1])
              id-str (str id-base "|" sorting-id)
              display-name (get-in sorting-info [ordering val 2])]]
    {:id id-str
     :label (or display-name "Ungrouped")
     :isExpanded (or (get expanded-states id-str false) open-all?)
     :childNodes (if (seq ordering-remainder)
                   (layers->nodes layer-subset (rest group-ordering) sorting-info expanded-states id-str layer-props open-all?)
                   (map-indexed
                    (fn [i layer]
                      (let [layer-state {:active?   (some #{layer} active-layers)
                                         :visible?  (some #{layer} visible-layers)
                                         :loading?  (loading-fn layer)
                                         :expanded? (expanded-fn layer)
                                         :errors?   (error-fn layer)
                                         :opacity  (opacity-fn layer)}]
                        {:id (str id-str "-" i)
                         :className (str "layer-wrapper" (when (:active? layer-state) " layer-active"))
                         :label (reagent/as-element
                                 [:div
                                  {:on-mouse-over #(re-frame/dispatch [:map/update-preview-layer layer])
                                   :on-mouse-out #(re-frame/dispatch [:map/update-preview-layer nil])}
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

(defn layer-catalogue-tree [_layers _ordering _id _layer-props _open-all?]
  (let [expanded-states (re-frame/subscribe [:ui.catalogue/nodes])
        sorting-info (re-frame/subscribe [:sorting/info])
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-click (fn [node]
                   (let [{:keys [childNodes id]} (js->clj node :keywordize-keys true)]
                     (when (seq childNodes)
                       ;; If we have children, toggle expanded state, else add to map
                       (re-frame/dispatch [:ui.catalogue/toggle-node id]))))]
    (fn [layers ordering id layer-props open-all?]
     [:div.tab-body.layer-controls {:id id}
      [b/tree {:contents (layers->nodes layers ordering @sorting-info @expanded-states id layer-props open-all?)
               :onNodeCollapse on-close
               :onNodeExpand on-open
               :onNodeClick on-click}]])))

(defn layer-catalogue [layers layer-props]
  (let [selected-tab @(re-frame/subscribe [:ui.catalogue/tab])
        select-tab   #(re-frame/dispatch [:ui.catalogue/select-tab %1])
        open-all?    (>= (count @(re-frame/subscribe [:map.layers/filter])) 3)]
    [:div.height-managed
     [b/tabs {:selected-tab-id selected-tab
              :on-change       select-tab
              :class      "group-scrollable height-managed"}
      [b/tab
       {:id    "cat"
        :title "By Category"
        :panel (reagent/as-element
                [layer-catalogue-tree layers [:category :data_classification] "cat" layer-props open-all?])}]
      [b/tab
       {:id    "org"
        :title "By Organisation"
        :panel (reagent/as-element
                [layer-catalogue-tree layers [:category :organisation :data_classification] "org" layer-props open-all?])}]]]))

(defn transect-toggle []
  (let [{:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        [text icon dispatch] (cond
                               drawing? ["Cancel Measurement" "undo"   :transect.draw/disable]
                               query    ["Clear Measurement"  "eraser" :transect.draw/clear]
                               :else    ["Transect/Measure"   "edit"   :transect.draw/enable])]
    [:div#transect-btn-wrapper {:data-helper-text "Click to draw a transect"}
     [b/button {:icon     icon
                :class    "bp3-fill"
                :on-click (handler-dispatch [dispatch])
                :text     text}]]))

(defn selection-button []
  (let [{:keys [selecting? region]} @(re-frame/subscribe [:map.layer.selection/info])
        [dispatch-key label]        (cond
                                      selecting? [:map.layer.selection/disable "Cancel Selecting"]
                                      region     [:map.layer.selection/clear   "Clear Selection"]
                                      :else      [:map.layer.selection/enable  "Select Region"])]
    [:div#select-btn-wrapper {:data-helper-text "Click to select a region"}
     [b/button {:icon       "widget"
                :class "bp3-fill"
                :on-click   (handler-dispatch [dispatch-key])
                :text       label}]]))

(defn viewport-only-toggle []
  (let [[icon text] (if @(re-frame/subscribe [:map/viewport-only?])
                      ["globe" "All layers"]
                      ["map" "Viewport layers only"])]
    [b/button
     {:icon     icon
      :class    "bp3-fill"
      :on-click #(re-frame/dispatch [:map/toggle-viewport-only])
      :text     text}]))

;; TODO: Remove, unused
#_(defn layer-logic-toggle []
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

;; TODO: Remove, unused
#_(defn layer-logic-toggle-button []
  (let [{:keys [type]} @(re-frame/subscribe [:map.layers/logic])]
    [:div#logic-toggle
     {:data-helper-text "Automatic layer selection picks the best layers to display, or turn off to list all available layers and choose your own"
      :data-text-width "380px"}
     (if (= type :map.layer-logic/automatic)
       [b/button
        {:class "bp3-fill"
         :on-click  #(re-frame/dispatch [:map.layers.logic/toggle true])}
        [:span
         [:i.fa.fa-magic]
         "Automatic Layer Selection"]]
       [b/button
        {:icon "hand"
         :class "bp3-fill"
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


;; TODO: Remove, unused
#_(defn layer-card [layer-spec {:keys [active? _visible? _loading? _errors? _expanded? _opacity-fn] :as other-props}]
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

;; TODO: Remove, unused
#_(defn layer-group [{:keys [expanded] :or {expanded false} :as _props} _layers _active-layers _visible-layers _loading-fn _error-fn _expanded-fn _opacity-fn]
  (let [expanded (reagent/atom expanded)]
    (fn [{:keys [id title classes] :as props} layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
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
                              :visible?  (some #{layer} visible-layers)
                              :loading?  (loading-fn layer)
                              :errors?   (error-fn layer)
                              :expanded? (expanded-fn layer)
                              :opacity   (opacity-fn layer)}])]]])))

(defn active-layer-selection-list
  [{:keys [layers visible-layers loading-fn error-fn expanded-fn opacity-fn]}]
  (let [layer-card-items
        (map
         (fn [layer]
           {:key (str (:id layer))
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

;; TODO: Remove, unused
#_(defn active-layer-group
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

;; TODO: Remove, unused
#_(defn settings-controls []
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

;; TODO: Remove, unused
#_(defn- as-icon [icon-name description]
  (reagent/as-element [b/tooltip {:content  description
                                  :position RIGHT}
                       [:span.bp3-icon-standard {:class (str "bp3-icon-" icon-name)}]]))

;; TODO: Remove, unused
#_(defn active-layers-tab
  [layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
  [:div.sidebar-tab.height-managed
   [active-layer-group layers active-layers visible-layers loading-fn error-fn expanded-fn opacity-fn]
   [help-button]])

;; TODO: Remove, unused
#_(defn catalogue-layers-button
  [{:keys [category]}]
  (let [title (:display_name category)]
    [b/button
     {:icon     "more"
      :text     title
      :on-click #(re-frame/dispatch [:drawer-panel-stack/open-catalogue-panel (:name category)])}]))

;; TODO: Remove, unused
#_(defn base-panel []
  (let [categories @(re-frame/subscribe [:map/display-categories])]
    {:content
     [:div
      [components/drawer-group
       {:heading "Controls"
        :icon    "settings"}
       [transect-toggle]
       [selection-button]
       [layer-logic-toggle-button]]
      
      [components/drawer-group
       {:heading "Catalogue Layers"
        :icon    "list-detail-view"}
       (for [category categories]
         [catalogue-layers-button
          {:key      (:name category)
           :category category}])]
      [components/drawer-group
       {:heading "Settings"
        :icon    "cog"}
       [b/button
        {:icon     "undo"
         :text     "Reset Interface"
         :on-click   #(re-frame/dispatch [:re-boot])}]]]}))

;; TODO: Remove, unused
#_(defn catalogue-layers-panel
  [{:keys [title layers active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities]}]
  {:title   title
   :content
   [:div.sidebar-tab.height-managed
    [layer-search-filter]
    [layer-catalogue layers
     {:active-layers  active-layers
      :visible-layers visible-layers
      :loading-fn     loading-layers
      :error-fn       error-layers
      :expanded-fn    expanded-layers
      :opacity-fn     layer-opacities}]]})

;; TODO: Remove, unused
#_(defn drawer-panel-selection
  [panel map-layers]
  (let [{:keys [panel props]} panel
        {:keys [groups active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities]} map-layers]
    (case panel
      :drawer-panel/catalogue-layers
      (catalogue-layers-panel
       (merge
        props
        {:layers          ((:group props) groups)
         :active-layers   active-layers
         :visible-layers  visible-layers
         :loading-layers  loading-layers
         :error-layers    error-layers
         :expanded-layers expanded-layers
         :layer-opacities layer-opacities})))))
;; TODO: Remove, unused
#_(defn drawer-panel-stack
  []
  (let [map-layers @(re-frame/subscribe [:map/layers])
        panels @(re-frame/subscribe [:drawer-panel-stack/panels])
        display-panels
        (map #(drawer-panel-selection % map-layers) panels)]
    [components/panel-stack
     {:panels (concat [(base-panel)] display-panels)
      :on-close #(re-frame/dispatch [:drawer-panel-stack/pop])}]))

;; TODO: Remove, unused
#_(defn active-layers-sidebar []
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

;; TODO: Remove, unused
#_(defn floating-menu-bar []
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

;; TODO: Remove, unused
#_(defn floating-menu-active-layers
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

;; TODO: Remove, unused
#_(defn floating-menu []
  (let [{:keys [active-layers] :as map-layers} @(re-frame/subscribe [:map/layers])]
    [:div.floating-menu-positioning
     [:div.floating-menu
      [floating-menu-bar]
      (when (seq active-layers) [floating-menu-active-layers map-layers])]]))

;; TODO: Remove, unused
#_(defn floating-transect-pill
  [{:keys [drawing? query]}]
  (let [[text icon dispatch] (cond
                           drawing? ["Cancel Transect" "undo"   :transect.draw/disable]
                           query    ["Clear Transect"  "eraser" :transect.draw/clear]
                           :else    ["Draw Transect"   "edit"   :transect.draw/enable])]
    [components/floating-pill-button
     {:text     text
      :icon     icon
      :on-click #(re-frame/dispatch [dispatch])}]))

;; TODO: Remove, unused
#_(defn floating-region-pill
  [{:keys [selecting? region]}]
  (let [[text icon dispatch] (cond
                               selecting? ["Cancel Selecting" "undo"   :map.layer.selection/disable]
                               region     ["Clear Selection"  "eraser" :map.layer.selection/clear]
                               :else      ["Select Region"    "widget" :map.layer.selection/enable])]
    [components/floating-pill-button
     {:text     text
      :icon     icon
      :on-click #(re-frame/dispatch [dispatch])}]))

(defn floating-pills []
  (let [collapsed                (:collapsed @(re-frame/subscribe [:ui/sidebar]))
        state-of-knowledge-open? @(re-frame/subscribe [:sok/open?])
        amp-boundaries           @(re-frame/subscribe [:sok/valid-amp-boundaries])
        imcra-boundaries         @(re-frame/subscribe [:sok/valid-imcra-boundaries])
        meow-boundaries          @(re-frame/subscribe [:sok/valid-meow-boundaries])
        boundaries               @(re-frame/subscribe [:sok/boundaries])
        active-boundary          @(re-frame/subscribe [:sok/active-boundary])
        open-pill                @(re-frame/subscribe [:sok/open-pill])]
    [:div
     {:class (str "floating-pills" (when collapsed " collapsed"))}

     [components/floating-pill-button
      {:icon     "menu"
       :on-click #(re-frame/dispatch [:left-drawer/toggle])}]

     [floating-state-of-knowledge-pill
      {:expanded?       (= open-pill "state-of-knowledge")
       :boundaries      boundaries
       :active-boundary active-boundary}]
     
     (when (and state-of-knowledge-open? active-boundary)
       [floating-boundaries-pill
        (merge
         amp-boundaries
         imcra-boundaries
         meow-boundaries
         {:expanded?       (= open-pill "boundaries")
          :active-boundary active-boundary})])
     
     (when (and state-of-knowledge-open? (= (:id active-boundary) "amp"))
       [floating-zones-pill
        (merge
         amp-boundaries
         {:expanded? (= open-pill "zones")})])]))

(defn layers-search-omnibar []
  (let [categories @(re-frame/subscribe [:map/categories-map])
        open?                @(re-frame/subscribe [:layers-search-omnibar/open?])
        {:keys [filtered-layers]} @(re-frame/subscribe [:map/layers])]
    [components/omnibar
     {:placeholder  "Search Layers..."
      :isOpen       open?
      :onClose      #(re-frame/dispatch [:layers-search-omnibar/close])
      :items        filtered-layers
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

;; TODO: Remove, unused
#_(defn catalogue-layer-search-button []
  [:div.catalogue-layer-search-button
   {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])}
   [b/icon
    {:icon "search"
     :icon-size 16}]
   "Search Layers..."])

(defn left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [filtered-layers active-layers visible-layers viewport-layers loading-layers error-layers expanded-layers layer-opacities]} @(re-frame/subscribe [:map/layers])
        viewport-only? @(re-frame/subscribe [:map/viewport-only?])
        catalogue-layers (filterv #(or (not viewport-only?) ((set viewport-layers) %)) filtered-layers)]
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
     [:div.sidebar-tab.height-managed
      [b/tabs
       {:id              "left-drawer-tabs"
        :class           "left-drawer-tabs"
        :selected-tab-id tab
        :on-change       #(re-frame/dispatch [:left-drawer/tab %1])}

       [b/tab
        {:id    "catalogue"
         :title "Catalogue"
         :panel (reagent/as-element
                 [:div
                  [layer-search-filter]
                  [layer-catalogue catalogue-layers
                   {:active-layers  active-layers
                    :visible-layers visible-layers
                    :loading-fn     loading-layers
                    :error-fn       error-layers
                    :expanded-fn    expanded-layers
                    :opacity-fn     layer-opacities}]])}]

       [b/tab
        {:id    "active-layers"
         :title "Active Layers"
         :panel (reagent/as-element
                 [:div
                  [components/drawer-group
                   {:heading (str "Active Layers (" (count active-layers) ")")
                    :icon    "eye-open"}
                   [active-layer-selection-list
                    {:layers         active-layers
                     :visible-layers visible-layers
                     :loading-fn     loading-layers
                     :error-fn       error-layers
                     :expanded-fn    expanded-layers
                     :opacity-fn     layer-opacities}]]])}]
       
       [b/tab
        {:id    "controls"
         :title "Controls"
         :panel (reagent/as-element
                 [:div
                  [components/drawer-group
                   {:heading "Controls"
                    :icon    "settings"}
                   [transect-toggle]
                   [selection-button]]
                  
                  [components/drawer-group
                   {:heading "Settings"
                    :icon    "cog"}
                   [viewport-only-toggle]
                   [b/button
                    {:icon     "undo"
                     :text     "Reset Interface"
                     :on-click   #(re-frame/dispatch [:re-boot])}]]])}]]]]))

(defn layer-preview [_preview-layer-url]
  (let [previous-url (reagent/atom nil) ; keeps track of previous url for the purposes of tracking its changes
        error? (reagent/atom false)]    ; keeps track of if previous url had an error in displaying
    (fn [preview-layer-url]
      (when (not= preview-layer-url @previous-url) ; if the preview layer has changed, then:
        (reset! error? false)                      ; - reset errors we're keeping track of (we need to check if the new image has errors in displaying)
        (reset! previous-url preview-layer-url))   ; - set the "previous url" we're keeping track of to the new url
      (when preview-layer-url
        [:div.layer-preview
         (if @error?
           [:div "No layer preview available"]
           [:img
            {:src preview-layer-url
             :onError #(reset! error? true)}])])))) ; if there's an error in displaying the image, then we keep track of it so we can instead display an error message

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
       {:label "Start Searching Layers" :combo "/" :prevent-default true}
       [:ui.search/focus])
      (keydown-wrapper
       {:label "Reset"                  :combo "shift + r"}
       [:re-boot])
      (keydown-wrapper
       {:label "Create Shareable URL"     :combo "c"}
       [:create-save-state])
      (keydown-wrapper
       {:label "Show Help Overlay"      :combo "h"}
       [:help-layer/toggle])])))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)
        catalogue-open?          @(re-frame/subscribe [:left-drawer/open?])
        state-of-knowledge-open? @(re-frame/subscribe [:sok/open?])]
    [:div#main-wrapper ;{:on-key-down handle-keydown :on-key-up handle-keyup}
     {:class (str (when catalogue-open? " catalogue-open") (when state-of-knowledge-open? " state-of-knowledge-open"))}
     [:div#content-wrapper
      [map-component [floating-pills]]
      [plot-component]]
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
     [welcome-dialogue]
     [info-card]
     [loading-display]
     [left-drawer]
     [state-of-knowledge]
     [layers-search-omnibar]
     [layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))

