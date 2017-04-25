(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [oops.core :refer [oget]]
            [reagent.core :as reagent]))


(def button          (reagent/adapt-react-class (oget js/window "Blueprint.Button")))

(def clipped-text    (reagent/adapt-react-class (oget js/window "Blueprint.Text")))

(def collapse        (reagent/adapt-react-class (oget js/window "Blueprint.Collapse")))

(def dialogue        (reagent/adapt-react-class (oget js/window "Blueprint.Dialog")))

(def non-ideal-state (reagent/adapt-react-class (oget js/window "Blueprint.NonIdealState")))

(def overlay         (reagent/adapt-react-class (oget js/window "Blueprint.Overlay")))

(def spinner         (reagent/adapt-react-class (oget js/window "Blueprint.Spinner")))

(def tabs            (reagent/adapt-react-class (oget js/window "Blueprint.Tabs2")))

(def tab             (reagent/adapt-react-class (oget js/window "Blueprint.Tab2")))

(def tooltip         (reagent/adapt-react-class (oget js/window "Blueprint.Tooltip")))

(def tree            (reagent/adapt-react-class (oget js/window "Blueprint.Tree")))
