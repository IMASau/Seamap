(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div "Hello from " @name])))
