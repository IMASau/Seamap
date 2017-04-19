(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [reagent.core :as reagent]))


(def button (reagent/adapt-react-class js/Blueprint.Button))

(def clipped-text (reagent/adapt-react-class js/Blueprint.Text))

(def collapse (reagent/adapt-react-class js/Blueprint.Collapse))

(def dialogue (reagent/adapt-react-class js/Blueprint.Dialog))

(def non-ideal-state (reagent/adapt-react-class js/Blueprint.NonIdealState))

(def overlay (reagent/adapt-react-class js/Blueprint.Overlay))

(def spinner (reagent/adapt-react-class js/Blueprint.Spinner))

(def tabs (reagent/adapt-react-class js/Blueprint.Tabs2))

(def tab (reagent/adapt-react-class js/Blueprint.Tab2))

(def tooltip (reagent/adapt-react-class js/Blueprint.Tooltip))

(def tree (reagent/adapt-react-class js/Blueprint.Tree))
