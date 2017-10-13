(ns imas-seamap.views
  (:require [clojure.set :refer [difference]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [*RIGHT*]]
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
                           helperText helperPosition]
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
           [:div.helper-layer-tooltiptext {:style {:width *text-width*}}
            helperText]]]))]))

(defn help-button []
  [:div.layer-controls.help-button
   [b/tooltip {:content "Show the quick-help guide"}
    [:span.control.pt-icon-large.pt-icon-help.pt-text-muted
     {:on-click #(re-frame/dispatch [:help-layer/open])}]]])

(defn catalogue-header [{:keys [name] :as layer} {:keys [active? errors? loading?] :as layer-state}]
  [:div.layer-wrapper (when active? {:class-name "layer-active"})
   [:div.header-text-wrapper (when (or loading? errors?) {:class "has-icons"})
    [:div (when (or loading? errors?) {:class "header-status-icons"})
     (when (and active? loading?) [b/spinner {:class-name "pt-small layer-spinner"}])
     (when (and active? errors?) [:span.layer-warning.pt-icon.pt-icon-small.pt-icon-warning-sign])]
    [b/clipped-text {:ellipsize true :class-name "header-text"}
     name]]])

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

(defn layers->nodes
  "group-ordering is the category keys to order by, eg [:organisation :data_category]"
  [layers [ordering & ordering-remainder :as group-ordering] expanded-states id-base
   {:keys [active-layers loading-fn error-fn] :as layer-props}]
  (for [[val layer-subset] (group-by ordering layers)
        :let [id-str (str id-base val)]]
    {:id id-str
     :label val ; (Implicit assumption that the group-by value is a string)
     :isExpanded (get expanded-states id-str false)
     :childNodes (if (seq ordering-remainder)
                   (layers->nodes layer-subset (rest group-ordering) expanded-states id-str layer-props)
                   (map-indexed
                    (fn [i layer]
                      (let [layer-state {:active?  (some #{layer} active-layers)
                                         :loading? (loading-fn layer)
                                         :errors?  (error-fn layer)}]
                       {:id (str id-str "-" i)
                        :label (reagent/as-element [catalogue-header layer layer-state])
                        ;; A hack, but if we just add the layer it gets
                        ;; warped in the js->clj conversion
                        ;; (specifically, values that were keywords become strings)
                        :do-layer-toggle #(re-frame/dispatch [:map/toggle-layer layer])
                        :secondaryLabel (reagent/as-component [catalogue-controls layer layer-state])}))
                    layer-subset))}))

(defn layer-catalogue-tree [layers ordering id layer-props]
  (let [expanded-states (reagent/atom {})
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (swap! expanded-states assoc (:id node) true)))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (swap! expanded-states assoc (:id node) false)))
        on-dblclick (fn [node]
                      (let [{:keys [childNodes do-layer-toggle id] :as node} (js->clj node :keywordize-keys true)]
                        (if (seq childNodes )
                          ;; If we have children, toggle expanded state, else add to map
                          (swap! expanded-states update id not)
                          (do-layer-toggle))))]
    (fn [layers ordering id layer-props]
      [:div.tab-body.layer-controls {:id id}
       [b/tree {:contents (layers->nodes layers ordering @expanded-states id layer-props)
                :onNodeCollapse on-close
                :onNodeExpand on-open
                :onNodeDoubleClick on-dblclick}]])))

(defn layer-catalogue [layers layer-props]
  [:div
   [b/tabs
    [b/tab {:id    "org" :title "By Organisation"
            :panel (reagent/as-component
                    [layer-catalogue-tree layers [:organisation :data_classification] "org" layer-props])}]
    [b/tab {:id    "cat" :title "By Category"
            :panel (reagent/as-component
                    [layer-catalogue-tree layers [:data_classification] "cat" layer-props])}]]])

(defn transect-toggle []
  (let [{:keys [drawing? query]} @(re-frame/subscribe [:transect/info])
        ;; Need to add a "Clear" button
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

(defn layer-logic-toggle []
  (let [{:keys [type trigger]} @(re-frame/subscribe [:map.layers/logic])
        user-triggered?        (= trigger :map.logic.trigger/user)
        [checked? label icon]  (if (= type :map.layer-logic/automatic)
                                 [true  " Automatic Layer Selection" [:i.fa.fa-magic]]
                                 [false " Choose Layers Manually"    [:span.pt-icon-standard.pt-icon-hand]])]
    [:div#logic-toggle.logic-toggle
     (merge {:data-helper-text "Automatic layer selection, or choose your own"}
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

(defn layer-card [layer-spec other-props]
  (let [show-legend (reagent/atom false)]
    (fn [{:keys [name] :as layer-spec} {:keys [active? loading? errors?] :as other-props}]
      [:div.layer-wrapper {:on-click (handler-fn (when active? (swap! show-legend not)))}
       [:div.layer-card.pt-card.pt-elevation-1 {:class-name (when active? "layer-active pt-interactive")}
        [:div.header-row.height-static
         [b/tooltip {:content (if @show-legend "Click to hide legend" "Click to show legend")
                     :class-name "header-text"
                     :position *RIGHT*
                     :is-disabled (not active?)}
          [catalogue-header layer-spec other-props]]
         [catalogue-controls layer-spec other-props]]
        [b/collapse {:is-open (and active? @show-legend)
                     :className "layer-legend"}
         [legend-display layer-spec]]]])))

(defn layer-group [{:keys [expanded] :or {:expanded false} :as props} layers active-layers loading-fn error-fn]
  (let [expanded (reagent/atom expanded)]
    (fn [{:keys [id title classes] :as props} layers active-layers loading-fn error-fn]
      [:div.layer-group.height-managed
       (merge {:class-name (str classes (if @expanded " expanded" " collapsed"))}
              (when id {:id id}))
       [:h1 {:on-click (handler-fn (swap! expanded not))}
        [:span.pt-icon-standard {:class (if @expanded "pt-icon-chevron-down" "pt-icon-chevron-right")}]
        (str title " (" (count layers) ")")]
       [b/collapse {:is-open @expanded :className "height-managed"}
        (when-let [extra-component (:extra-component props)]
          extra-component)
        [:div.height-managed.group-scrollable
         (for [layer layers]
           ^{:key (:layer_name layer)}
           [layer-card layer {:active?  (some #{layer} active-layers)
                              :loading? (loading-fn layer)
                              :errors?  (error-fn layer)}])]]])))

(defn third-party-layer-group [props layers active-layers loading-fn error-fn]
  (let [show-dialogue? (reagent/atom false)]
    (fn [props layers active-layers loading-fn error-fn]
      (let [catalogue [:div
                       [b/button  {:icon-name "pt-icon-add-to-artifact"
                                   :class-name "pt-fill catalogue-add"
                                   :on-click (handler-fn (swap! show-dialogue? not))
                                   :text "Catalogue"}]
                       [b/dialogue {:is-open @show-dialogue?
                                    :on-close #(reset! show-dialogue? false)
                                    :icon-name "pt-icon-add-to-artifact"
                                    :title "Add from catalogue"}
                        [layer-catalogue (seq (difference (set layers) (set active-layers)))]]]
            ;; Only display active (third-party) layers in this group:
            third-party-actives (filter #(= :third-party (:category %)) active-layers)]
        [layer-group (-> props
                         (assoc :extra-component catalogue)
                         (update :classes str (when (seq third-party-actives) " needs-extra")))
         third-party-actives
         active-layers
         loading-fn
         error-fn]))))

(defn app-controls []
  (let [{:keys [groups active-layers loading-layers error-layers]} @(re-frame/subscribe [:map/layers])
        {:keys [habitat bathymetry imagery third-party]} groups]
    [:div#sidebar
     [transect-toggle]
     [layer-logic-toggle]
     [layer-search-filter]
     [layer-group {:id "habitat-group" :title "Habitat"   :expanded true } habitat     active-layers loading-layers error-layers]
     [layer-group {:id "bathy-group"   :title "Bathymetry":expanded true } bathymetry  active-layers loading-layers error-layers]
     [layer-group {:id "imagery-group" :title "Imagery"   :expanded false} imagery     active-layers loading-layers error-layers]
     [third-party-layer-group
      {:id "third-party-group" :title "Other"     :expanded false} third-party active-layers loading-layers error-layers]
     [help-button]]))

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
      :data-helper-text "This shows the data along a transect you can draw"
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
      (let [{:keys [message intent] :or {intent b/*intent-warning*} :as msg} @info-message
            msg (assoc msg :onDismiss #(re-frame/dispatch [:info/clear-message]))]
        (when message
          (ocall toaster "show" (clj->js msg))))
      nil)))


(defn loading-display []
  (let [loading?  @(re-frame/subscribe [:app/loading?])
        error-msg @(re-frame/subscribe [:app/load-error-msg])]
    (when loading?
      [:div.loading-splash {:class (when error-msg "load-error")}
       (if error-msg
         [b/non-ideal-state
          {:title       error-msg
           :description "We were unable to load everything we need to get started.  Please try again later."
           :visual      "error"}]
         [b/non-ideal-state
          {:title  "Loading Seamap Layers..."
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
      Governments."]]]
     [:div.pt-dialog-footer
      [:div.pt-dialog-footer-actions
       [b/button {:text       "Get Started!"
                  :intent     b/*intent-primary*
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
     "Downloading implies acceptance of all citation and usage
     requirements.  You will first select your region of
     interest."]]])

(defn info-card []
  (let [layer-info @(re-frame/subscribe [:map.layer/info])
        layer      (:layer layer-info)
        title      (or (get-in layer-info [:layer :name]) "Layer Information")]
    [b/dialogue {:title    title
                 :is-open  layer-info
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
       (when (#{:habitat :bathymetry :imagery} (:category layer))
         [b/popover {:position           b/*BOTTOM*
                     :popover-class-name "pt-minimal"
                     :content            (reagent/as-element
                                          [b/menu
                                           [b/menu-item {:text     "GeoTIFF"
                                                         :label    (reagent/as-element [b/icon {:icon-name "globe"}])
                                                         :on-click (handler-dispatch [:map.layer/download-start
                                                                                      layer
                                                                                      :map.layer.download/geotiff])}]
                                           [b/menu-item {:text     "SHP File"
                                                         :label    (reagent/as-element [b/icon {:icon-name "polygon-filter"}])
                                                         :on-click (handler-dispatch [:map.layer/download-start
                                                                                      layer
                                                                                      :map.layer.download/shp])}]
                                           [b/menu-item {:text     "CSV"
                                                         :label    (reagent/as-element [b/icon {:icon-name "th"}])
                                                         :on-click (handler-dispatch [:map.layer/download-start
                                                                                      layer
                                                                                      :map.layer.download/csv])}]])}
          [b/button {:text            "Download As..."
                     :right-icon-name "caret-down"}]])
       [b/button {:text       "Close"
                  :auto-focus true
                  :intent     b/*intent-primary*
                  :on-click   (handler-dispatch [:map.layer/close-info])}]]]]))

(defn- as-icon [icon-name description]
  (reagent/as-element [b/tooltip {:content  description
                                  :position *RIGHT*}
                       [:span.pt-icon-standard {:class-name (str "pt-icon-" icon-name)}]]))

(defn layer-tab [layers active-layers loading-fn error-fn]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-group {:expanded true :title "Layers"} layers active-layers loading-fn error-fn]
   [help-button]])

(defn thirdparty-layer-tab [layers active-layers loading-fn error-fn]
  [:div.sidebar-tab.height-managed
   [transect-toggle]
   [layer-logic-toggle]
   [layer-search-filter]
   [layer-catalogue layers {:active-layers active-layers
                            :loading-fn    loading-fn
                            :error-fn      error-fn}]
   [help-button]])

(defn management-layer-tab [boundaries habitat-layer active-layers loading-fn error-fn]
  [:div.sidebar-tab.height-managed
   [:div.boundary-layers.height-managed.group-scrollable
    [:h6 "Boundary Layers"]
    (for [layer boundaries]
      ^{:key (:layer_name layer)}
      [layer-card layer {:active?  (some #{layer} active-layers)
                         :loading? (loading-fn layer)
                         :errors?  (error-fn layer)}])]
   [:label.pt-label.height-managed
    "Habitat layer for region statistics (only one active layer may be selected at a time):"
    [b/popover {:position           b/*BOTTOM*
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
                :intent (when-not habitat-layer b/*intent-warning*)
                :right-icon-name "caret-down"}]]]
   [help-button]])

(defn seamap-sidebar []
  (let [{:keys [collapsed selected] :as sidebar-state}              @(re-frame/subscribe [:ui/sidebar])
        {:keys [groups active-layers loading-layers error-layers]}  @(re-frame/subscribe [:map/layers])
        {:keys [habitat-layer]}                                     @(re-frame/subscribe [:map/region-stats])
        {:keys [habitat boundaries bathymetry imagery third-party]} groups]
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
      [layer-tab habitat active-layers loading-layers error-layers]]
     [sidebar-tab {:header "Bathymetry"
                   :icon   (as-icon "timeline-area-chart"
                                    (str "Bathymetry Layers (" (count bathymetry) ")"))
                   :id     "tab-bathy"}
      [layer-tab bathymetry active-layers loading-layers error-layers]]
     [sidebar-tab {:header "Imagery"
                   :icon   (as-icon "media"
                                    (str "Imagery Layers (" (count imagery) ")"))
                   :id     "tab-imagery"}
      [layer-tab imagery active-layers loading-layers error-layers]]
     [sidebar-tab {:header "Management Regions"
                   :icon   (as-icon "heatmap" "Management Region Layers")
                   :id     "tab-management"}
      [management-layer-tab boundaries habitat-layer active-layers loading-layers error-layers]]
     [sidebar-tab {:header "Third-Party"
                   :icon   (as-icon "more"
                                    (str "Third-Party Layers (miscellaneous data — " (count third-party) ")"))
                   :id     "tab-thirdparty"}
      [thirdparty-layer-tab third-party active-layers loading-layers error-layers]]
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
                {:label "Show Help Overlay"      :combo "h"}
                [:help-layer/toggle])]]))

(def layout-app
  (b/hotkeys-target

   [:div#main-wrapper
    [:div#content-wrapper
     [map-component [seamap-sidebar]]
     [plot-component]]
    ;; needs the ids of components to helper-annotate:
    [helper-overlay
     :layer-search
     :logic-toggle
     :plot-footer
     :transect-btn-wrapper
     {:id "habitat-group"     :helperText "Layers showing sea-floor habitats"}
     {:id "bathy-group"       :helperText "Layers showing bathymetry data"}
     {:id "imagery-group"     :helperText "Layers showing photos collected"}
     {:id "third-party-group" :helperText "Layers from other providers (eg CSIRO)"}]
    [show-messages]
    [welcome-dialogue]
    [info-card]
    [loading-display]]

   hotkeys-combos))

