(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [oops.core :refer [gget]]
            [reagent.core :as reagent]))


(def button          (reagent/adapt-react-class (gget "Blueprint.Button")))

(def clipped-text    (reagent/adapt-react-class (gget "Blueprint.Text")))

(def collapse        (reagent/adapt-react-class (gget "Blueprint.Collapse")))

(def dialogue        (reagent/adapt-react-class (gget "Blueprint.Dialog")))

(def non-ideal-state (reagent/adapt-react-class (gget "Blueprint.NonIdealState")))

(def overlay         (reagent/adapt-react-class (gget "Blueprint.Overlay")))

(def spinner         (reagent/adapt-react-class (gget "Blueprint.Spinner")))

(def switch          (reagent/adapt-react-class (gget "Blueprint.Switch")))

(def tabs            (reagent/adapt-react-class (gget "Blueprint.Tabs2")))

(def tab             (reagent/adapt-react-class (gget "Blueprint.Tab2")))

(def tooltip         (reagent/adapt-react-class (gget "Blueprint.Tooltip")))

(def tree            (reagent/adapt-react-class (gget "Blueprint.Tree")))


;;; Intents:

(def *intent-none*    (gget "Blueprint.Intent.NONE"))
(def *intent-primary* (gget "Blueprint.Intent.PRIMARY"))
(def *intent-success* (gget "Blueprint.Intent.SUCCESS"))
(def *intent-warning* (gget "Blueprint.Intent.WARNING"))
(def *intent-danger*  (gget "Blueprint.Intent.DANGER"))
