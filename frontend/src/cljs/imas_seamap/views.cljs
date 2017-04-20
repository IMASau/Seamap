(ns imas-seamap.views
  (:require [clojure.set :refer [difference]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.map.events :refer [process-layer]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer-macros [handler-fn]]
            [goog]
            [goog.object :as gobj]
            [goog.dom :as dom]
            [debux.cs.core :refer [dbg]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def container-dimensions
  (reagent/adapt-react-class js/React.ContainerDimensions))

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

(defn add-to-layer [layer]
  [b/tooltip {:content "Add to map"}
   [:span.pt-icon-standard.pt-icon-send-to-map
    {:on-click #(re-frame/dispatch [:map/toggle-layer layer])}]])

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
                       :secondaryLabel (reagent/as-component (add-to-layer layer))})
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
         [b/clipped-text {:ellipses true :class-name "header-text"}
          [b/tooltip {:content (if @show-legend "Click to hide legend" "Click to show legend")
                      :position js/Blueprint.Position.RIGHT
                      :isDisabled (not active?)}
           name]]
         [:div.layer-controls.pt-ui-text-large
          [b/tooltip {:content (if active? "Hide layer" "Show layer")
                      :position js/Blueprint.Position.RIGHT}
           [:span.control.pt-text-muted.pt-icon-large
            {:class (if active? "pt-icon-eye-on" "pt-icon-eye-off")
             :on-click (handler-fn (re-frame/dispatch [:map/toggle-layer layer-spec]))}]]
          [b/tooltip {:content "Show entire layer"
                      :position js/Blueprint.Position.RIGHT}
           [:span.control.pt-text-muted.pt-icon-large.pt-icon-zoom-to-fit
            {:on-click (handler-fn (re-frame/dispatch [:map/pan-to-layer layer-spec]))}]]]]
        [b/collapse {:is-open (and active? @show-legend)}
         [legend-display layer-spec]]]])))

(defn layer-group [{:keys [title expanded on-toggle max-height] :as props} layers active-layers]
  [:div.layer-group
   [:h1.pt-icon-standard {:class (if expanded "pt-icon-chevron-down" "pt-icon-chevron-right")
                          :on-click #(on-toggle)}
    (str title " (" (count layers) ")")]
   [b/collapse {:is-open expanded}
    [:div {:style {:max-height (str max-height "px")
                   :overflow-y "auto"}}
     (when-let [extra-component (:extra-component props)]
       extra-component)
     (for [layer layers]
       ^{:key (:layer_name layer)}
       [layer-card layer {:active? (active-layers layer)}])]]])

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

(defn -calc-group-heights [vertical-height expanded-states groups active-layers]
  (let [;; Need to special-case third-party; only calc height for those displayed:
        groups (update groups :third-party #(-> % set (difference (set active-layers)) seq))
        expanded-count (->> expanded-states vals (filter identity) count)
        group-height (/ (- vertical-height
                           35           ; button
                           (* 4 23)     ; 4 group headers
                           10)          ; magic (random padding)
                        (if (zero? expanded-count) 1 expanded-count))
        requirements (reduce-kv (fn [m k v]
                                  (let [cnt (count v)
                                        required-height (* cnt 67)
                                        relinquished-height (- group-height required-height)
                                        surplus? (pos? relinquished-height)]
                                    (assoc m k {:surplus? surplus?
                                                :required required-height
                                                :surplus-height (max 0 relinquished-height)})))
                                {} groups)
        available-surplus (->> requirements vals (map :surplus-height) (apply +))
        need-more-count (->> requirements vals (remove :surplus?) count)
        height-to-distribute (+ group-height (/ available-surplus need-more-count))]
    (reduce-kv (fn [m k v]
                 (let [group-requirements (k requirements)
                       height (if (:surplus? group-requirements)
                                (:required group-requirements)
                                height-to-distribute)]
                   (assoc m k height)))
               {} groups)))

(defn app-controls [props]
  (let [layer-sub (re-frame/subscribe [:map/layers])
        expanded-states (reagent/atom {:hab true
                                       :bat true
                                       :img false
                                       :oth false})
        callback (fn [k]
                   (fn [] (swap! expanded-states update k not)))]
    (fn [{:keys [height] :as props}]
      (let [{:keys [groups active-layers]} @layer-sub
            {:keys [habitat bathymetry imagery third-party]} groups
            {:keys [hab bat img oth] :as es} @expanded-states
            group-heights (-calc-group-heights height @expanded-states groups active-layers)
            h (:habitat group-heights 0)
            b (:bathymetry group-heights 0)
            i (:imagery group-heights 0)
            o (:third-party group-heights 0)]
        [:div#sidebar
         [transect-toggle]
         [layer-group {:title "Habitat"    :on-toggle (callback :hab) :expanded hab :max-height h} habitat     active-layers]
         [layer-group {:title "Bathymetry" :on-toggle (callback :bat) :expanded bat :max-height b} bathymetry  active-layers]
         [layer-group {:title "Imagery"    :on-toggle (callback :img) :expanded img :max-height i} imagery     active-layers]
         [third-party-layer-group
                      {:title "Other"      :on-toggle (callback :oth) :expanded oth :max-height o} third-party active-layers]]))))

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
   [container-dimensions
    #(reagent/as-element
      [app-controls (js->clj % :keywordize-keys true)])]
   [:div#content-wrapper
    [map-component]
    [plot-component]]])

