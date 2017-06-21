(ns imas-seamap.views
  (:require [clojure.set :refer [difference]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.map.events :refer [process-layer]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer-macros [handler-fn]]
            [goog.object :as gobj]
            [goog.dom :as dom]
            [oops.core :refer [oget ocall]]
            [debux.cs.core :refer [dbg]]))

(def css-transition-group
  (reagent/adapt-react-class (oget js/window "React.addons.CSSTransitionGroup")))

(def container-dimensions
  (reagent/adapt-react-class (oget js/window "React.ContainerDimensions")))

(defn with-params [url params]
  (let [u (goog/Uri. url)]
    (doseq [[k v] params]
      (.setParameterValue u (name k) v))
    (str u)))

(defn helper-overlay [& element-ids]
  (let [*line-height* 17.6 *padding* 10 *text-width* 200 ;; hard-code
        *vertical-bar* 50 *horiz-bar* 100
        elem-props #(when-let [elem (dom/getElement %)]
                      (merge
                       (-> elem .getBoundingClientRect js->clj)
                       (-> elem .-dataset js->clj)))
        posn->offsets (fn [posn width height]
                        (case posn
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
       (for [id element-ids
             :let [id (-> id name (string/replace #"^#" "")) ; allow "id", "#id", :id, :#id
                   {:keys [top right bottom left width height
                           helperText helperPosition]
                    :or {helperPosition "right"}
                    :as eprops} (elem-props id)
                   posn-cls (str "helper-layer-" helperPosition)]
             :when (not (string/blank? helperText))]
         ^{:key id}
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

(defn add-to-layer [layer]
  [b/tooltip {:content "Add to map"}
   [:span.pt-icon-standard.pt-icon-send-to-map.pt-text-muted.control
    {:on-click (handler-fn (re-frame/dispatch [:map/toggle-layer layer]))}]])

(defn layers->nodes
  "group-ordering is the category keys to order by, eg [:organisation :data_category]"
  [layers [ordering & ordering-remainder :as group-ordering] expanded-states id-base]
  (for [[val layer-subset] (group-by ordering layers)
        :let [id-str (str id-base val)]]
    {:id id-str
     :label val ; (Implicit assumption that the group-by value is a string)
     :isExpanded (get expanded-states id-str false)
     :childNodes (if (seq ordering-remainder)
                   (layers->nodes layer-subset (rest group-ordering) expanded-states id-str)
                   (map-indexed
                    (fn [i layer]
                      {:id (str id-str "-" i)
                       :label (:name layer)
                       ;; A hack, but if we just add the layer it gets
                       ;; warped in the js->clj conversion
                       ;; (specifically, values that were keywords become strings)
                       :do-layer-toggle #(re-frame/dispatch [:map/toggle-layer layer])
                       :secondaryLabel (reagent/as-component (add-to-layer layer))})
                    layer-subset))}))

(defn layer-catalogue-tree [layers ordering id]
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
    (fn [layers ordering id]
      [:div.tab-body.layer-controls {:id id}
       [b/tree {:contents (layers->nodes layers ordering @expanded-states id)
                :onNodeCollapse on-close
                :onNodeExpand on-open
                :onNodeDoubleClick on-dblclick}]])))

(defn layer-catalogue [layers]
  (let [filter-text (re-frame/subscribe [:map.layers/others-filter])]
    [:div.layer-catalogue.pt-dialog-body
     [:div.pt-input-group
      [:span.pt-icon.pt-icon-search]
      [:input.pt-input.pt-round {:type        "search"
                                 :placeholder "Search Layers..."
                                 :value       @filter-text
                                 :on-change   (handler-fn
                                               (re-frame/dispatch
                                                [:map.layers/others-filter (oget event :target :value)]))}]]
     [b/tabs
      [b/tab {:id    "org" :title "By Organisation"
              :panel (reagent/as-component
                      [layer-catalogue-tree layers [:organisation :data_classification] "org"])}]
      [b/tab {:id    "cat" :title "By Category"
              :panel (reagent/as-component
                      [layer-catalogue-tree layers [:data_classification :organisation] "cat"])}]]]))

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (if drawing?
                               [:transect.draw/disable "Cancel Transect"]
                               [:transect.draw/enable  "Draw Transect"])]
    [:div#transect-btn-wrapper {:data-helper-text "Click to draw a transect"}
     [b/button {:id "transect-button"
                :icon-name "edit"
                :class-name "pt-fill draw-transect height-static"
                :on-click (handler-fn (re-frame/dispatch [dispatch-key]))
                :text label}]]))

(defn layer-logic-toggle []
  (let [{:keys [type trigger]} @(re-frame/subscribe [:map.layers/logic])
        user-triggered?        (= trigger :map.logic.trigger/user)
        [checked? label icon]  (if (= type :map.layer-logic/automatic)
                                 [true  " Automatic Layers" [:i.fa.fa-magic]]
                                 [false " Choose Layers"    [:span.pt-icon-standard.pt-icon-hand]])]
    [:div.logic-toggle
     (when-not (or checked? user-triggered?)
       {:class-name "external-trigger"})
     [b/switch {:checked   checked?
                :label     (reagent/as-element [:span icon label])
                :on-change (handler-fn (re-frame/dispatch [:map.layers.logic/toggle true]))}]]))

(defn layer-search-filter []
  (let [filter-text (re-frame/subscribe [:map.layers/filter])]
    [:div.pt-input-group
     [:span.pt-icon.pt-icon-search]
     [:input.pt-input.pt-round {:type        "search"
                                :placeholder "Search Layers..."
                                :value       @filter-text
                                :on-change   (handler-fn
                                              (re-frame/dispatch
                                               [:map.layers/filter (oget event :target :value)]))}]]))

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

(def ^:private *RIGHT* (oget js/window "Blueprint.Position.RIGHT"))

(defn layer-card [layer-spec other-props]
  (let [show-legend (reagent/atom false)]
    (fn [{:keys [name] :as layer-spec} {:keys [active?] :as other-props}]
      [:div.layer-wrapper {:on-click (handler-fn (when active? (swap! show-legend not)))}
       [:div.layer-card.pt-card.pt-elevation-1 {:class-name (when active? "layer-active pt-interactive")}
        [:div.header-row.height-static
         [b/clipped-text {:ellipsize true :class-name "header-text"}
          [b/tooltip {:content (if @show-legend "Click to hide legend" "Click to show legend")
                      :position *RIGHT*
                      :isDisabled (not active?)}
           name]]
         [:div.layer-controls.pt-ui-text-large
          [b/tooltip {:content (if active? "Hide layer" "Show layer")
                      :position *RIGHT*}
           [:span.control.pt-text-muted.pt-icon-large
            {:class (if active? "pt-icon-eye-on" "pt-icon-eye-off")
             :on-click (handler-fn (re-frame/dispatch [:map/toggle-layer layer-spec]))}]]
          [b/tooltip {:content "Show entire layer"
                      :position *RIGHT*}
           [:span.control.pt-text-muted.pt-icon-large.pt-icon-zoom-to-fit
            {:on-click (handler-fn (re-frame/dispatch [:map/pan-to-layer layer-spec]))}]]]]
        [b/collapse {:is-open (and active? @show-legend)
                     :className "layer-legend"}
         [legend-display layer-spec]]]])))

(defn layer-group [{:keys [expanded] :or {:expanded false} :as props} layers active-layers]
  (let [expanded (reagent/atom expanded)]
    (fn [{:keys [title classes] :as props} layers active-layers]
      [:div.layer-group.height-managed
       {:class-name (str classes (if @expanded " expanded" " collapsed"))}
       [:h1.pt-icon-standard {:class (if @expanded "pt-icon-chevron-down" "pt-icon-chevron-right")
                              :on-click (handler-fn (swap! expanded not))}
        (str title " (" (count layers) ")")]
       [b/collapse {:is-open @expanded :className "height-managed"}
        (when-let [extra-component (:extra-component props)]
          extra-component)
        [:div.height-managed.group-scrollable
         (for [layer layers]
           ^{:key (:layer_name layer)}
           [layer-card layer {:active? (some #{layer} active-layers)}])]]])))

(defn third-party-layer-group [props layers active-layers]
  (let [show-dialogue? (reagent/atom false)]
    (fn [props layers active-layers]
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
         active-layers]))))

(defn app-controls []
  (let [{:keys [groups active-layers]} @(re-frame/subscribe [:map/layers])
        {:keys [habitat bathymetry imagery third-party]} groups]
    [:div#sidebar
     [transect-toggle]
     [layer-logic-toggle]
     [layer-search-filter]
     [layer-group {:title "Habitat"   :expanded true } habitat     active-layers]
     [layer-group {:title "Bathymetry":expanded true } bathymetry  active-layers]
     [layer-group {:title "Imagery"   :expanded false} imagery     active-layers]
     [third-party-layer-group
      {:title "Other"     :expanded false} third-party active-layers]
     [help-button]]))

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
     {:on-click (handler-fn (re-frame/dispatch [:transect.plot/toggle-visibility]))
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

(defn layout-app []
  [:div#main-wrapper
   [app-controls]
   [:div#content-wrapper
    [map-component]
    [plot-component]]
   ;; needs the ids of components to helper-annotate:
   [helper-overlay :plot-footer :transect-btn-wrapper]
   [show-messages]
   [loading-display]])

