(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.map.views :refer [map-component]]
            [goog.object :as gobj]
            [goog.dom :as dom]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def button (reagent/adapt-react-class js/Blueprint.Button))
(def collapse (reagent/adapt-react-class js/Blueprint.Collapse))

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (if drawing?
                               [:transect.draw/disable "Cancel Transect"]
                               [:transect.draw/enable  "Draw Transect"])]
    [button {:icon-name "edit"
             :class-name "pt-fill draw-transect"
             :on-click #(re-frame/dispatch [dispatch-key])
             :text label}]))

(defn layer-card [{:keys [name] :as layer-spec}]
  [:div.layer-wrapper
   [:div.pt-card.pt-elevation-1
    {:on-click #(re-frame/dispatch [:map/toggle-layer layer-spec])}
    [:span name]]])

(defn layer-group [{:keys [title expanded] :or {expanded false}} layers]
  (let [expanded-state (reagent/atom expanded)]
    (fn [props layers]
      [:div.layer-group
       [:h1 {:class (if @expanded-state "pt-icon-chevron-down" "pt-icon-chevron-right")
             :on-click #(swap! expanded-state not)}
        (str title " (" (count layers) ")")]
       [collapse {:is-open @expanded-state}
        (for [layer layers]
          ^{:key (:layer_name layer)}
          [layer-card layer])]])))

(defn app-controls []
  (let [{:keys [habitat bathymetry imagery third-party] :as groups} @(re-frame/subscribe [:map/layers])]
    [:div#sidebar
     [transect-toggle]
     [layer-group {:title "Habitat"    :expanded true } habitat]
     [layer-group {:title "Bathymetry" :expanded true } bathymetry]
     [layer-group {:title "Imagery"    :expanded false} imagery]
     [layer-group {:title "Other"      :expanded false} third-party]]))

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

