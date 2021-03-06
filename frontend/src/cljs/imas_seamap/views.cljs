;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.views
  (:require [clojure.set :refer [difference]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [RIGHT]]
            [imas-seamap.db :refer [img-url-base]]
            [imas-seamap.map.events :refer [process-layer]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer-macros [handler-fn handler-dispatch]]
            [goog.object :as gobj]
            [goog.dom :as dom]
            [oops.core :refer [oget ocall]]
            [debux.cs.core :refer [dbg]]))

(def css-transition-group
  (reagent/adapt-react-class (oget js/window "React.addons.CSSTransitionGroup")))

(def container-dimensions
  (reagent/adapt-react-class (oget js/window "React.ContainerDimensions")))

(def sidebar     (reagent/adapt-react-class (oget js/window "ReactSidebar.Sidebar")))
(def sidebar-tab (reagent/adapt-react-class (oget js/window "ReactSidebar.Tab")))

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
        wrapper-props (fn [posn {:keys [top left width height] :as elem-props}]
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
       (for [{:keys [top right bottom left width height
                     helperText helperPosition
                     textWidth]
                    :or {helperPosition "right"}
                    :as eprops} (selectors+elements element-selectors)
             :let [posn-cls (str "helper-layer-" helperPosition)]
             :when (and eprops
                        (pos? height)
                        (not (string/blank? helperText)))]
         ^{:key (hash eprops)}
         [:div.helper-layer-wrapper {:class-name posn-cls
                                     :style (wrapper-props helperPosition eprops)}
          [:div.helper-layer-tooltip {:class-name posn-cls
                                      :style (posn->offsets helperPosition width height)}
           [:div.helper-layer-tooltiptext {:style {:width (or textWidth *text-width*)}}
            helperText]]]))]))

(defn help-button []
  [:div.layer-controls.help-button
   [b/tooltip {:content "Show the quick-help guide"}
    [:span.control.pt-icon-large.pt-icon-help.pt-text-muted
     {:on-click #(re-frame/dispatch [:help-layer/open])}]]])

(defn legend-display [{:keys [server_url layer_name] :as layer-spec}]
  (let [legend-url (with-params server_url
                     {:REQUEST "GetLegendGraphic"
                      :LAYER layer_name
                      :FORMAT "image/png"
                      :TRANSPARENT true
                      :SERVICE "WMS"
                      :VERSION "1.1.1"})]
    [:div.legend-wrapper
     [:img {:src legend-url}]]))

(defn catalogue-header [{:keys [name] :as layer} {:keys [active? errors? loading? expanded?] :as layer-state}]
  [b/tooltip {:content (if expanded? "Click to hide legend" "Click to show legend")
              :class-name "header-text"
              :position RIGHT
              :is-disabled (not active?)}
   [:div.layer-wrapper (when active? {:class-name "layer-active"
                                      :on-click (handler-dispatch [:map.layer.legend/toggle layer])})
    [:div.header-text-wrapper (when (or loading? errors?) {:class "has-icons"})
     [:div (when (or loading? errors?) {:class "header-status-icons"})
      (when (and active? loading?) [b/spinner {:class-name "pt-small layer-spinner"}])
      (when (and active? errors?) [:span.layer-warning.pt-icon.pt-icon-small.pt-icon-warning-sign])]
     [b/clipped-text {:ellipsize true :class-name "header-text"}
      name]
     [b/collapse {:is-open (and active? expanded?)
                  :className "layer-legend"}
      [legend-display layer]]]]])

(defn catalogue-controls [layer {:keys [active? errors? loading?] :as layer-state}]
  [:div.catalogue-layer-controls (when active? {:class-name "layer-active"})
   [b/tooltip {:content "Layer info / Download data"}
    [:span.control.pt-icon-small.pt-icon-info-sign.pt-text-muted
     {:on-click (handler-dispatch [:map.layer/show-info layer])}]]
   [b/tooltip {:content "Zoom to layer"}
    [:span.control.pt-icon-standard.pt-icon-zoom-to-fit.pt-text-muted
     {:on-click (handler-dispatch [:map/pan-to-layer layer])}]]
   [b/tooltip {:content (if active? "Hide layer" "Show layer")}
    [:span.control.pt-icon-large.pt-text-muted
     {:class (if active? "pt-icon-eye-on" "pt-icon-eye-off")
      :on-click (handler-dispatch [:map/toggle-layer layer])}]]])

(defn- ->sort-by [sorting-info ordering-key]
  (let [name-key-mapping (get sorting-info ordering-key)]
    ;; Sort by key first, then name (ie, return vector of [key name])
    (comp #(vector (get name-key-mapping %) %) first)))

(defn layers->nodes
  "group-ordering is the category keys to order by, eg [:organisation :data_category]"
  [layers [ordering & ordering-remainder :as group-ordering] sorting-info expanded-states id-base
   {:keys [active-layers loading-fn expanded-fn error-fn] :as layer-props}]
  (for [[val layer-subset] (sort-by (->sort-by sorting-info ordering) (group-by ordering layers))
        ;; sorting-info maps category key -> label -> [sort-key,id].
        ;; We use the id for a stable node-id:
        :let [sorting-id (get-in sorting-info [ordering val 1])
              id-str (str id-base "|" sorting-id)]]
    {:id id-str
     :label val ; (Implicit assumption that the group-by value is a string)
     :isExpanded (get expanded-states id-str false)
     :childNodes (if (seq ordering-remainder)
                   (layers->nodes layer-subset (rest group-ordering) sorting-info expanded-states id-str layer-props)
                   (map-indexed
                    (fn [i layer]
                      (let [layer-state {:active?   (some #{layer} active-layers)
                                         :loading?  (loading-fn layer)
                                         :expanded? (expanded-fn layer)
                                         :errors?   (error-fn layer)}]
                        {:id (str id-str "-" i)
                         :className (when (:active? layer-state) "layer-active")
                        :label (reagent/as-element [catalogue-header layer layer-state])
                        ;; A hack, but if we just add the layer it gets
                        ;; warped in the js->clj conversion
                        ;; (specifically, values that were keywords become strings)
                        :do-layer-toggle #(re-frame/dispatch [:map/toggle-layer layer])
                        :secondaryLabel (reagent/as-component [catalogue-controls layer layer-state])}))
                    layer-subset))}))

(defn layer-catalogue-tree [layers ordering id layer-props]
  (let [expanded-states (re-frame/subscribe [:ui.catalogue/nodes])
        sorting-info (re-frame/subscribe [:sorting/info])
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-click (fn [node]
                   (let [{:keys [childNodes do-layer-toggle id] :as node} (js->clj node :keywordize-keys true)]
                     (when (seq childNodes )
                       ;; If we have children, toggle expanded state, else add to map
                       (re-frame/dispatch [:ui.catalogue/toggle-node id]))))]
    [:div.tab-body.layer-controls {:id id}
     [b/tree {:contents (layers->nodes layers ordering @sorting-info @expanded-states id layer-props)
              :onNodeCollapse on-close
              :onNodeExpand on-open
              :onNodeClick on-click}]]))

(defn layer-catalogue [layers layer-props]
  (let [selected-tab @(re-frame/subscribe [:ui.catalogue/tab])
        select-tab   #(re-frame/dispatch [:ui.catalogue/select-tab %1])]
    [:div.height-managed
     [b/tabs {:selected-tab-id selected-tab
              :on-change       select-tab
              :class-name      "group-scrollable height-managed"}
      [b/tab {:id    "org" :title "By Organisation"
              :panel (reagent/as-component
                      [layer-catalogue-tree layers [:organisation :data_classification] "org" layer-props])}]
      [b/tab {:id    "cat" :title "By Category"
              :panel (reagent/as-component
                      [layer-catalogue-tree layers [:data_classification] "cat" layer-props])}]]]))

(defn transect-toggle []
  (let [{:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (cond
                               drawing? [:transect.draw/disable "Cancel Transect"]
                               query    [:transect.draw/clear "Clear Transect"]
                               :else    [:transect.draw/enable  "Draw Transect"])]
    [:div#transect-btn-wrapper {:data-helper-text "Click to draw a transect"}
     [b/button {:id "transect-button"
                :icon-name "edit"
                :class-name "pt-fill draw-transect height-static"
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
                :icon-name  "widget"
                :class-name "pt-fill select-region height-static"
                :on-click   (handler-dispatch [dispatch-key])
                :text       label}]]))

(defn layer-logic-toggle []
  (let [{:keys [type trigger]} @(re-frame/subscribe [:map.layers/logic])
        user-triggered?        (= trigger :map.logic.trigger/user)
        [checked? label icon]  (if (= type :map.layer-logic/automatic)
                                 [true  " Automatic Layer Selection" [:i.fa.fa-magic]]
                                 [false " Choose Layers Manually"    [:span.pt-icon-standard.pt-icon-hand]])]
    [:div#logic-toggle.logic-toggle
     (merge {:data-helper-text "Automatic layer selection picks the best layers to display, or turn off to list all available layers and choose your own"
             :data-text-width "380px"}
            (when-not (or checked? user-triggered?)
              {:class-name "external-trigger"}))
     [b/switch {:checked   checked?
                :label     (reagent/as-element [:span icon label])
                :on-change (handler-dispatch [:map.layers.logic/toggle true])}]]))

(defn layer-search-filter []
  (let [filter-text (re-frame/subscribe [:map.layers/filter])]
    [:div.pt-input-group {:data-helper-text "Filter Layers"}
     [:span.pt-icon.pt-icon-search]
     [:input.pt-input.pt-round {:id          "layer-search"
                                :type        "search"
                                :placeholder "Search Layers..."
                                :value       @filter-text
                                :on-change   (handler-dispatch
                                               [:map.layers/filter (oget event :target :value)])}]]))


(defn layer-card [{:keys [name] :as layer-spec} {:keys [active? loading? errors? expanded?] :as other-props}]
  [:div.layer-wrapper ; {:on-click (handler-fn (when active? (swap! show-legend not)))}
   [:div.layer-card.pt-card.pt-elevation-1 {:class-name (when active? "layer-active pt-interactive")}
    [:div.header-row.height-static
     [catalogue-header layer-spec other-props]
     [catalogue-controls layer-spec other-props]]]])

(defn layer-group [{:keys [expanded] :or {:expanded false} :as props} layers active-layers loading-fn error-fn expanded-fn]
  (let [expanded (reagent/atom expanded)]
    (fn [{:keys [id title classes] :as props} layers active-layers loading-fn error-fn expanded-fn]
      [:div.layer-group.height-managed
       (merge {:class-name (str classes (if @expanded " expanded" " collapsed"))}
              (when id {:id id}))
       [:h1 {:on-click (handler-fn (swap! expanded not))}
        [:span.pt-icon-standard {:class (if @expanded "pt-icon-chevron-down" "pt-icon-chevron-right")}]
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
                              :expanded? (expanded-fn layer)}])]]])))

(defn settings-controls []
  [:div#settings
   [b/button {:id         "reset-button"
              :icon-name  "undo"
              :class-name "pt-fill"
              :on-click   (handler-dispatch [:re-boot])
              :text       "Reset Interface"}]])

(defn plot-component-animatable [{:keys [on-add on-remove]
                                  :or   {on-add identity on-remove identity}
                                  :as   props}
                                 child-component
                                 child-props]
  (reagent/create-class
   {:display-name           "plot-component-animatable"
    :component-will-unmount on-remove
    :component-did-mount    on-add
    :reagent-render
    (fn [props child-component child-props]
      [:div.plot-container
       [container-dimensions
        #(reagent/as-element [child-component
                              (merge child-props
                                     (js->clj % :keywordize-keys true))])]])}))

(defn plot-component []
  (let [show-plot (re-frame/subscribe [:transect.plot/show?])
        force-resize #(js/window.dispatchEvent (js/Event. "resize"))
        transect-results (re-frame/subscribe [:transect/results])]
    [:footer#plot-footer
     {:on-click (handler-dispatch [:transect.plot/toggle-visibility])
      :data-helper-text "This shows the habitat data along a bathymetry transect you can draw"
      :data-helper-position "top"}
     [:div.drag-handle [:span.pt-icon-large.pt-icon-drag-handle-horizontal]]
     [css-transition-group {:transition-name "plot-height"
                            :transition-enter-timeout 300
                            :transition-leave-timeout 300}
      (if @show-plot
        [plot-component-animatable {:on-add force-resize :on-remove force-resize}
         transect-display-component (assoc @transect-results
                                           :on-mousemove
                                           #(re-frame/dispatch [:transect.plot/mousemove %])
                                           :on-mouseout
                                           #(re-frame/dispatch [:transect.plot/mouseout]))])]]))

(defn show-messages []
  (let [info-message (re-frame/subscribe [:info/message])
        toaster      (ocall (oget js/window "Blueprint.Toaster") "create")]
    (fn []
      (let [{:keys [message] :as msg} @info-message
            msg (assoc msg :onDismiss #(re-frame/dispatch [:info/clear-message]))]
        (when message
          (ocall toaster "show" (clj->js msg))))
      nil)))


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
           :visual      "error"}]
         [b/non-ideal-state
          {:title  main-msg
           :visual (reagent/as-element [b/spinner {:intent "success"}])}])])))

(defn welcome-dialogue []
  (let [open? @(re-frame/subscribe [:welcome-layer/open?])]
    [b/dialogue {:title      "Welcome to Seamap Australia!"
                 :class-name "welcome-splash"
                 :is-open    open?
                 :on-close   #(re-frame/dispatch [:welcome-layer/close])}
     [:div#welcome-splash.pt-dialog-body
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
     [:div.pt-dialog-footer
      [:div.pt-dialog-footer-actions
       [b/button {:text       "Get Started!"
                  :intent     b/INTENT-PRIMARY
                  :auto-focus true
                  :on-click   (handler-dispatch [:welcome-layer/close])}]]]]))

(defn metadata-record [{:keys [license-name license-link license-img constraints other]
                        {:keys [category organisation name metadata_url]} :layer
                        :as layer-info}]
  [:div.metadata-record {:class-name (clojure.core/name category)}
   [:div.metadata-header.clearfix
    (when-let [logo (:logo @(re-frame/subscribe [:map/organisations organisation]))]
      [:img.metadata-img.org-logo {:class-name (string/replace logo #"\..+$" "")
                                   :src        (str img-url-base logo)}])
    [:h3 name]]
   [:h6.metadata-subheader "Citation Information:"]
   [:div
    [:p.citation  constraints]]
   (when (seq other)
     [:div
      [:h6.metadata-subheader "Usage:"]
      (map-indexed (fn [i o] (when o ^{:key i} [:p.other-constraints o])) other)])
   [:div.license-info.clearfix
    [:h6 "License Information:"]
    (when license-img [:img.license.metadata-img {:src license-img}])
    [:a {:href license-link :target "_blank"} license-name]]
   [:div.more-info
    [:a {:href metadata_url :target "_blank"} "Click here for the full metadata record."]]
   [:div
    [:p.download-instructions
     "Downloading implies acceptance of all citation and usage requirements."]]])

(defn- download-menu [{:keys [title disabled? layer bbox]}]
  [b/popover {:position           b/BOTTOM
              :is-disabled        disabled?
              :popover-class-name "pt-minimal"
              :content            (reagent/as-element
                                   [b/menu
                                    [b/menu-item {:text     "GeoTIFF"
                                                  :label    (reagent/as-element [b/icon {:icon-name "globe"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/geotiff])}]
                                    [b/menu-item {:text     "SHP File"
                                                  :label    (reagent/as-element [b/icon {:icon-name "polygon-filter"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/shp])}]
                                    [b/menu-item {:text     "CSV"
                                                  :label    (reagent/as-element [b/icon {:icon-name "th"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/csv])}]])}
   [b/button {:text            title
              :disabled        disabled?
              :right-icon-name "caret-down"}]])

(defn info-card []
  (let [layer-info       @(re-frame/subscribe [:map.layer/info])
        layer            (:layer layer-info)
        title            (or (get-in layer-info [:layer :name]) "Layer Information")
        {:keys [region]} @(re-frame/subscribe [:map.layer.selection/info])]
    [b/dialogue {:title    title
                 :is-open  (and layer-info (not (:hidden? layer-info)))
                 :on-close #(re-frame/dispatch [:map.layer/close-info])}
     [:div.pt-dialog-body
      (case layer-info
        :display.info/loading
        [b/non-ideal-state
         {:title  "Loading Metadata..."
          :visual (reagent/as-element [b/spinner {:intent "success"}])}]

        :display.info/error
        [b/non-ideal-state
         {:title       "Error"
          :description "Unable to load metadata record.  Please try again later."
          :visual      "error"}]

        [metadata-record layer-info])]
     [:div.pt-dialog-footer
      [:div.pt-dialog-footer-actions
       (when (#{:habitat :imagery} (:category layer))
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
                       [:span.pt-icon-standard {:class-name (str "pt-icon-" icon-name)}]]))

(defn layer-tab [layers active-layers loading-fn error-fn expanded-fn]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [selection-button]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-group {:expanded true :title "Layers"} layers active-layers loading-fn error-fn expanded-fn]
   [help-button]])

(defn thirdparty-layer-tab [layers active-layers loading-fn error-fn expanded-fn]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [selection-button]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-catalogue layers {:active-layers active-layers
                            :loading-fn    loading-fn
                            :error-fn      error-fn
                            :expanded-fn   expanded-fn}]
   [help-button]])

(defn management-layer-tab [boundaries habitat-layer active-layers loading-fn error-fn expanded-fn]
  [:div.sidebar-tab.height-managed
   [:div.boundary-layers.height-managed.group-scrollable
    [:h6 "Boundary Layers"]
    (for [layer boundaries]
      ^{:key (:layer_name layer)}
      [layer-card layer {:active?   (some #{layer} active-layers)
                         :loading?  (loading-fn layer)
                         :errors?   (error-fn layer)
                         :expanded? (expanded-fn layer)}])]
   [:label.pt-label.height-managed
    "Habitat layer for region statistics (only one active layer may be selected at a time):"
    [b/popover {:position           b/BOTTOM
                :class-name         "full-width"
                :popover-class-name "pt-minimal"
                :content            (reagent/as-element
                                     [b/menu
                                      (for [layer (filter #(= :habitat (:category %)) active-layers)]
                                        ^{:key (:layer_name layer)}
                                        [b/menu-item {:text     (:name layer)
                                                      :on-click #(re-frame/dispatch [:map.region-stats/select-habitat layer])}])])}
     [b/button {:text            (get habitat-layer :name "Select Habitat Layer for statistics...")
                :class-name      "pt-fill pt-text-overflow-ellipsis"
                :intent          (when-not habitat-layer b/INTENT-WARNING)
                :right-icon-name "caret-down"}]]]
   [:div.pt-callout.pt-icon-help.height-managed
    [:h5 "Hints"]
    [:p "Choose a management boundary and select a habitat layer for
    spatial summaries. Note that only visible habitat layers will be
    available for selection."]

    [:p "Click on a management boundary (on the map) to generate
    habitat statistics for that region, and to download the subsetted
    benthic habitat data."]]
   [help-button]])

(defn seamap-sidebar []
  (let [{:keys [collapsed selected] :as sidebar-state}                             @(re-frame/subscribe [:ui/sidebar])
        {:keys [groups active-layers loading-layers error-layers expanded-layers]} @(re-frame/subscribe [:map/layers])
        {:keys [habitat-layer]}                                                    @(re-frame/subscribe [:map/region-stats])
        {:keys [habitat boundaries bathymetry imagery third-party]}                groups]
    [sidebar {:id        "floating-sidebar"
              :selected  selected
              :collapsed collapsed
              :closeIcon (reagent/as-element [:span.pt-icon-standard.pt-icon-caret-left])
              :on-close  #(re-frame/dispatch [:ui.sidebar/close])
              :on-open   #(re-frame/dispatch [:ui.sidebar/open %])}
     [sidebar-tab {:header "Habitats"
                   :icon   (as-icon "home"
                                    (str "Habitat Layers (" (count habitat) ")"))
                   :id     "tab-habitat"}
      [layer-tab habitat active-layers loading-layers error-layers expanded-layers]]
     [sidebar-tab {:header "Bathymetry"
                   :icon   (as-icon "timeline-area-chart"
                                    (str "Bathymetry Layers (" (count bathymetry) ")"))
                   :id     "tab-bathy"}
      [layer-tab bathymetry active-layers loading-layers error-layers expanded-layers]]
     [sidebar-tab {:header "Imagery"
                   :icon   (as-icon "media"
                                    (str "Imagery Layers (" (count imagery) ")"))
                   :id     "tab-imagery"}
      [layer-tab imagery active-layers loading-layers error-layers expanded-layers]]
     [sidebar-tab {:header "Management Regions"
                   :icon   (as-icon "heatmap" "Management Region Layers")
                   :id     "tab-management"}
      [management-layer-tab boundaries habitat-layer active-layers loading-layers error-layers expanded-layers]]
     [sidebar-tab {:header "Third-Party"
                   :icon   (as-icon "more"
                                    (str "Third-Party Layers (" (count third-party) ")"))
                   :id     "tab-thirdparty"}
      [thirdparty-layer-tab third-party active-layers loading-layers error-layers expanded-layers]]
     [sidebar-tab {:header "Settings"
                   :anchor "bottom"
                   :icon   (reagent/as-element [:span.pt-icon-standard.pt-icon-cog])
                   :id     "tab-settings"}
      [settings-controls]]]))

(def hotkeys-combos
  (let [keydown-wrapper
        (fn [{:keys [label combo] :as m} keydown-v]
          (assoc m :global    true
                   :group     "Keyboard Shortcuts"
                   :onKeyDown #(re-frame/dispatch keydown-v)))]
    [b/hotkeys nil
     [b/hotkey (keydown-wrapper
                {:label "Zoom In"                :combo "plus"}
                [:map/zoom-in])]
     [b/hotkey (keydown-wrapper
                {:label "Zoom Out"               :combo "-"}
                [:map/zoom-out])]
     [b/hotkey (keydown-wrapper
                {:label "Pan Up"                 :combo "up"}
                [:map/pan-direction :up])]
     [b/hotkey (keydown-wrapper
                {:label "Pan Down"               :combo "down"}
                [:map/pan-direction :down])]
     [b/hotkey (keydown-wrapper
                {:label "Pan Left"               :combo "left"}
                [:map/pan-direction :left])]
     [b/hotkey (keydown-wrapper
                {:label "Pan Right"              :combo "right"}
                [:map/pan-direction :right])]
     [b/hotkey (keydown-wrapper
                {:label "Toggle Plot Panel"      :combo "p"}
                [:transect.plot/toggle-visibility])]
     [b/hotkey (keydown-wrapper
                {:label "Toggle Sidebar"         :combo "s"}
                [:ui.sidebar/toggle])]
     [b/hotkey (keydown-wrapper
                {:label "Start/Clear Transect"   :combo "t"}
                [:transect.draw/toggle])]
     [b/hotkey (keydown-wrapper
                {:label "Start/Clear Region"     :combo "r"}
                [:map.layer.selection/toggle])]
     [b/hotkey (keydown-wrapper
                {:label "Cancel"                 :combo "esc"}
                [:ui.drawing/cancel])]
     [b/hotkey (keydown-wrapper
                {:label "Toggle Layer Logic"     :combo "m"}
                [:map.layers.logic/toggle])]
     [b/hotkey (keydown-wrapper
                {:label "Start Searching Layers" :combo "/" :prevent-default true}
                [:ui.search/focus])]
     [b/hotkey (keydown-wrapper
                {:label "Reset"                  :combo "shift + r"}
                [:re-boot])]
     [b/hotkey (keydown-wrapper
                {:label "Copy Shareable URL"     :combo "c"}
                [:copy-share-url])]
     [b/hotkey (keydown-wrapper
                {:label "Show Help Overlay"      :combo "h"}
                [:help-layer/toggle])]]))

(def layout-app
  (b/hotkeys-target

   [:div#main-wrapper
    [:div#content-wrapper
     [map-component [seamap-sidebar]]
     [plot-component]]
    [helper-overlay
     :layer-search
     :logic-toggle
     :plot-footer
     {:selector ".group-scrollable"
      :helperText "Layers available in your current field of view (zoom out to see more)"}
     {:selector ".group-scrollable > .layer-wrapper:first-child"
      :helperPosition "bottom"
      :helperText "Toggle layer visibility, view more info, show legend, and download data"}
     {:selector ".sidebar-tabs ul:first-child"
      :helperText "Choose between habitat, bathymetry, and other layer types"}
     :transect-btn-wrapper
     :select-btn-wrapper
     {:selector ".sidebar-tabs ul:nth-child(2)" :helperText "Reset interface"}
     {:id "habitat-group"     :helperText "Layers showing sea-floor habitats"}
     {:id "bathy-group"       :helperText "Layers showing bathymetry data"}
     {:id "imagery-group"     :helperText "Layers showing photos collected"}
     {:id "third-party-group" :helperText "Layers from other providers (eg CSIRO)"}]
    [show-messages]
    [welcome-dialogue]
    [info-card]
    [loading-display]]

   hotkeys-combos))

