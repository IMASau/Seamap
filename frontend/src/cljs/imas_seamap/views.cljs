(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.map.views :refer [map-component]]
            [goog.object :as gobj]
            [goog.dom :as dom]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def button (reagent/adapt-react-class js/Blueprint.Button))
(def clipped-text (reagent/adapt-react-class js/Blueprint.Text))
(def collapse (reagent/adapt-react-class js/Blueprint.Collapse))
(def overlay (reagent/adapt-react-class js/Blueprint.Overlay))

(defn ->helper-props [& {:keys [text position]
                         :or   {position "right"}}]
  {:data-helper-text     text
   :data-helper-position position})

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
    [overlay {:is-open  open?
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

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (if drawing?
                               [:transect.draw/disable "Cancel Transect"]
                               [:transect.draw/enable  "Draw Transect"])]
    [button {:icon-name "edit"
             :class-name "pt-fill draw-transect"
             :on-click #(re-frame/dispatch [dispatch-key])
             :text label}]))

(defn layer-card [{:keys [name] :as layer-spec} {:keys [active?] :as other-props}]
  [:div.layer-wrapper
   [:div.pt-card.pt-elevation-1
    [:div.header-row
     [clipped-text {:ellipses true :class-name "header-text"} name]
     [:div.layer-controls.pt-ui-text-large
      [:span.control.pt-text-muted
       {:class (if active? "pt-icon-eye-on" "pt-icon-eye-off")
        :on-click #(re-frame/dispatch [:map/toggle-layer layer-spec])}]
      [:span.control.pt-text-muted.pt-icon-zoom-to-fit
       {:on-click #(re-frame/dispatch [:map/pan-to-layer layer-spec])}]]]]])

(defn layer-group [{:keys [expanded] :or {expanded false}} layers active-layers]
  (let [expanded-state (reagent/atom expanded)]
    (fn [{:keys [title] :as props} layers active-layers]
      [:div.layer-group
       [:h1 {:class (if @expanded-state "pt-icon-chevron-down" "pt-icon-chevron-right")
             :on-click #(swap! expanded-state not)}
        (str title " (" (count layers) ")")]
       [collapse {:is-open @expanded-state}
        (for [layer layers]
          ^{:key (:layer_name layer)}
          [layer-card layer {:active? (active-layers layer)}])]])))

(defn app-controls []
  (let [{:keys [groups active-layers]} @(re-frame/subscribe [:map/layers])
        {:keys [habitat bathymetry imagery third-party]} groups]
     [transect-toggle]
     [layer-group {:title "Habitat"    :expanded true } habitat     active-layers]
     [layer-group {:title "Bathymetry" :expanded true } bathymetry  active-layers]
     [layer-group {:title "Imagery"    :expanded false} imagery     active-layers]
     [layer-group {:title "Other"      :expanded false} third-party active-layers]]))

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
  (let [show-plot (reagent/atom true)
        force-resize #(js/window.dispatchEvent (js/Event. "resize"))]
    (fn []
      [:footer {:on-click #(swap! show-plot not)}
       [css-transition-group {:transition-name "plot-height"
                              :transition-enter-timeout 300
                              :transition-leave-timeout 300}
        (if @show-plot
          [plot-component-animatable {:on-add force-resize :on-remove force-resize}])]])))

(defn layout-app []
  [:div#main-wrapper
   [app-controls]
   [:div#content-wrapper
    [map-component]
    [plot-component]]])

