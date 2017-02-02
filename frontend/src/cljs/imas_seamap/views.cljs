(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.views.map :refer [map-component]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn layout-app []
  [:div#main-wrapper
   [:div#sidebar]
   [:div#content-wrapper
    [map-component]
    [:footer]]])
