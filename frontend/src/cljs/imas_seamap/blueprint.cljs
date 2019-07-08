;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [oops.core :refer [gget]]
            [reagent.core :as reagent]))


(def button          (reagent/adapt-react-class (gget "Blueprint.Button")))

(def card            (reagent/adapt-react-class (gget "Blueprint.Card")))

(def clipped-text    (reagent/adapt-react-class (gget "Blueprint.Text")))

(def collapse        (reagent/adapt-react-class (gget "Blueprint.Collapse")))

(def dialogue        (reagent/adapt-react-class (gget "Blueprint.Dialog")))

(def icon            (reagent/adapt-react-class (gget "Blueprint.Icon")))

(def non-ideal-state (reagent/adapt-react-class (gget "Blueprint.NonIdealState")))

(def overlay         (reagent/adapt-react-class (gget "Blueprint.Overlay")))

(def popover         (reagent/adapt-react-class (gget "Blueprint.Popover")))

(def menu            (reagent/adapt-react-class (gget "Blueprint.Menu")))

(def menu-item       (reagent/adapt-react-class (gget "Blueprint.MenuItem")))

(def spinner         (reagent/adapt-react-class (gget "Blueprint.Spinner")))

(def switch          (reagent/adapt-react-class (gget "Blueprint.Switch")))

(def tabs            (reagent/adapt-react-class (gget "Blueprint.Tabs2")))

(def tab             (reagent/adapt-react-class (gget "Blueprint.Tab2")))

(def tooltip         (reagent/adapt-react-class (gget "Blueprint.Tooltip")))

(def tree            (reagent/adapt-react-class (gget "Blueprint.Tree")))

(def hotkeys         (reagent/adapt-react-class (gget "Blueprint.Hotkeys")))

(def hotkey          (reagent/adapt-react-class (gget "Blueprint.Hotkey")))

(defn hotkeys-target
  [view hotkeys]
  (let [c (js/React.createClass
           #js {:renderHotkeys (fn [_] (reagent/as-element hotkeys))
                :render        (fn [_] (reagent/as-element view))})]
    (js/Blueprint.HotkeysTarget c)
    (reagent/adapt-react-class c)))

;;; Intents:

(def *intent-none*    (gget "Blueprint.Intent.NONE"))
(def *intent-primary* (gget "Blueprint.Intent.PRIMARY"))
(def *intent-success* (gget "Blueprint.Intent.SUCCESS"))
(def *intent-warning* (gget "Blueprint.Intent.WARNING"))
(def *intent-danger*  (gget "Blueprint.Intent.DANGER"))

;;; Other constants:

(def *RIGHT*  (gget "Blueprint.Position.RIGHT"))
(def *BOTTOM* (gget "Blueprint.Position.BOTTOM"))
(def *LEFT*   (gget "Blueprint.Position.LEFT"))
(def *TOP*    (gget "Blueprint.Position.TOP"))

(def *elevation-one*   (gget "Blueprint.Elevation.ONE"))
(def *elevation-two*   (gget "Blueprint.Elevation.TWO"))
(def *elevation-three* (gget "Blueprint.Elevation.THREE"))
(def *elevation-four*  (gget "Blueprint.Elevation.FOUR"))
(def *elevation-five*  (gget "Blueprint.Elevation.FIVE"))
