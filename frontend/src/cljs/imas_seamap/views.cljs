(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.views.map :refer [map-component]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])]
    (if drawing?
      [:button {:on-click #(re-frame/dispatch [:transect.draw/disable])}
       "Cancel Transect"]
      [:button {:on-click #(re-frame/dispatch [:transect.draw/enable])}
       "Draw Transect"])))

(defn app-controls []
  [:div#sidebar
   [transect-toggle]])

(defn plot-component []
  (let [show-plot (reagent/atom true)]
    (fn []
      [:footer {:on-click #(swap! show-plot not)}
       [css-transition-group {:transition-name "plot-height"
                              :transition-enter-timeout 300
                              :transition-leave-timeout 300}
        (if @show-plot
          [:div.plot-container])]])))

(defn layout-app []
  [:div#main-wrapper
   [app-controls]
   [:div#content-wrapper
    [map-component]
    [plot-component]]])
