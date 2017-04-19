(ns imas-seamap.views
  (:require [clojure.set :refer [difference]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.map.events :refer [process-layer]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [goog]
            [goog.object :as gobj]
            [goog.dom :as dom]
            [debux.cs.core :refer [dbg]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn ->helper-props [& {:keys [text position]
                         :or   {position "right"}}]
  {:data-helper-text     text
   :data-helper-position position})

(defn with-params [url params]
  (let [u (goog/Uri. url)]
    (doseq [[k v] params]
      (.setParameterValue u (name k) v))
    (str u)))

(defn helper-overlay [& element-ids]
  (let [elem-props (fn [id]
                     (let [elem (dom/getElement id)
                           rect (-> elem .getBoundingClientRect js->clj)
                           data (-> elem .-dataset js->clj)]
                       (merge rect data)))
        posn->offsets (fn [posn props]
                        (case posn
                          "top"    {:top -30}
                          "bottom" {:bottom -30}
                          "left"   {:left -176}
                          "right"  {:right -176}))
        open? @(re-frame/subscribe [:help-layer/open?])]
    [b/overlay {:is-open  open?
                :on-close #(re-frame/dispatch  [:help-layer/close])}
     (when open?
      (for [id element-ids
            :let [{:keys [top right bottom left width height
                          helperText helperPosition]
                   :as eprops} (elem-props id)
                  posn-cls (str "helper-layer-" helperPosition)]]
        ^{:key id}
        [:div.helper-layer-wrapper {:class-name posn-cls
                                    :style {:width width
                                            :height height
                                            :top top
                                            :left left}}
         [:div.helper-layer-tooltip {:class-name posn-cls} ; TODO: needs positioning-offsets depending on position attribute
          [:div.helper-layer-tooltiptext id helperText]]]))]))

;;; FIXME: Mocked-up for now:
(defn- dbg-callback [& args] (js/console.warn "args:" args))

(defn node-obj->layer
  "Convert a js object to a layer representation (keywords in the
  appropriate places, etc).  Necessary because as well as using
  js->clj we also keword-ise a few *values*, not just keys."
  [obj]
  (-> obj (js->clj :keywordize-keys true) :layer process-layer))

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
                       ;; the layer itself added to be accessible to event handlers; ignored by the tree component:
                       :layer layer})
                    layer-subset))}))

(defn layer-catalogue-tree [layers ordering id]
  (let [expanded-states (reagent/atom {})
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (swap! expanded-states assoc (:id node) true)))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (swap! expanded-states assoc (:id node) false)))]
    (fn [layers ordering id]
      [:div.tab-body {:id id}
       [b/tree {:contents (layers->nodes layers ordering @expanded-states id)
                :onNodeClick dbg-callback
                :onNodeCollapse on-close
                :onNodeExpand on-open}]])))

(defn layer-catalogue [layers]
  [:div.layer-catalogue.pt-dialog-body
   [b/tabs
    [b/tab {:id "org" :title "By Organisation"
            :panel (reagent/as-component
                    [layer-catalogue-tree layers [:organisation :data_classification] "org"])}]
    [b/tab {:id "cat" :title "By Category"
            :panel (reagent/as-component
                    [layer-catalogue-tree layers [:data_classification :organisation] "cat"])}]]])

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (if drawing?
                               [:transect.draw/disable "Cancel Transect"]
                               [:transect.draw/enable  "Draw Transect"])]
    [b/button {:icon-name "edit"
               :class-name "pt-fill draw-transect"
               :on-click #(re-frame/dispatch [dispatch-key])
               :text label}]))

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
    (fn [{:keys [name] :as layer-spec} {:keys [active?] :as other-props}]
      [:div.layer-wrapper {:on-click #(when active? (swap! show-legend not))}
       [:div.pt-card.pt-elevation-1 {:class-name (when active? "pt-interactive")}
        [:div.header-row
         [b/clipped-text {:ellipses true :class-name "header-text"} name]
         [:div.layer-controls.pt-ui-text-large
          [:span.control.pt-text-muted.pt-icon-large
           {:class (if active? "pt-icon-eye-on" "pt-icon-eye-off")
            :on-click #(re-frame/dispatch [:map/toggle-layer layer-spec])}]
          [:span.control.pt-text-muted.pt-icon-large.pt-icon-zoom-to-fit
           {:on-click #(re-frame/dispatch [:map/pan-to-layer layer-spec])}]]]
        [b/collapse {:is-open (and active? @show-legend)}
         [legend-display layer-spec]]]])))

(defn layer-group [{:keys [expanded] :or {expanded false}} layers active-layers]
  (let [expanded-state (reagent/atom expanded)]
    (fn [{:keys [title] :as props} layers active-layers]
      [:div.layer-group
       [:h1.pt-icon-standard {:class (if @expanded-state "pt-icon-chevron-down" "pt-icon-chevron-right")
             :on-click #(swap! expanded-state not)}
        (str title " (" (count layers) ")")]
       [b/collapse {:is-open @expanded-state}
        (when-let [extra-component (:extra-component props)]
          extra-component)
        (for [layer layers]
          ^{:key (:layer_name layer)}
          [layer-card layer {:active? (active-layers layer)}])]])))

(defn third-party-layer-group [props layers active-layers]
  (let [show-dialogue? (reagent/atom false)]
    (fn [props layers active-layers]
      (let [catalogue [:div
                       [b/button  {:icon-name "pt-icon-add-to-artifact"
                                   :class-name "pt-fill catalogue-add"
                                   :on-click #(swap! show-dialogue? not)
                                   :text "Catalogue"}]
                       [b/dialogue {:is-open @show-dialogue?
                                    :on-close #(reset! show-dialogue? false)
                                    :icon-name "pt-icon-add-to-artifact"
                                    :title "Add from catalogue"}
                        [layer-catalogue (seq (difference (set layers) (set active-layers)))]]]]
        [layer-group (assoc props :extra-component catalogue)
         ;; Only display active (third-party) layers in this group:
         (filter #(= :third-party (:category %)) active-layers)
         active-layers]))))

(defn app-controls []
  (let [{:keys [groups active-layers]} @(re-frame/subscribe [:map/layers])
        {:keys [habitat bathymetry imagery third-party]} groups]
    [:div#sidebar
     [transect-toggle]
     [layer-group {:title "Habitat"    :expanded true } habitat     active-layers]
     [layer-group {:title "Bathymetry" :expanded true } bathymetry  active-layers]
     [layer-group {:title "Imagery"    :expanded false} imagery     active-layers]
     [third-party-layer-group
                  {:title "Other"      :expanded false} third-party active-layers]]))

(def container-dimensions (reagent/adapt-react-class js/React.ContainerDimensions))

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
    [:footer {:on-click #(re-frame/dispatch [:transect.plot/toggle-visibility])}
     [:div.drag-handle [:span.pt-icon-large.pt-icon-drag-handle-horizontal]]
     [css-transition-group {:transition-name "plot-height"
                            :transition-enter-timeout 300
                            :transition-leave-timeout 300}
      (if @show-plot
        [plot-component-animatable {:on-add force-resize :on-remove force-resize}
         transect-display-component @transect-results])]]))

(defn layout-app []
  [:div#main-wrapper
   [app-controls]
   [:div#content-wrapper
    [map-component]
    [plot-component]]])

