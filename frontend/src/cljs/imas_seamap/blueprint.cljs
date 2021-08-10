;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [reagent.core :as reagent]
            ["@blueprintjs/core" :as Blueprint :refer [Intent Position Elevation]]))


(def button          (reagent/adapt-react-class Blueprint/Button))

(def card            (reagent/adapt-react-class Blueprint/Card))

(def clipped-text    (reagent/adapt-react-class Blueprint/Text))

(def collapse        (reagent/adapt-react-class Blueprint/Collapse))

(def dialogue        (reagent/adapt-react-class Blueprint/Dialog))

(def icon            (reagent/adapt-react-class Blueprint/Icon))

(def non-ideal-state (reagent/adapt-react-class Blueprint/NonIdealState))

(def overlay         (reagent/adapt-react-class Blueprint/Overlay))

(def popover         (reagent/adapt-react-class Blueprint/Popover))

(def menu            (reagent/adapt-react-class Blueprint/Menu))

(def menu-item       (reagent/adapt-react-class Blueprint/MenuItem))

(def spinner         (reagent/adapt-react-class Blueprint/Spinner))

(def switch          (reagent/adapt-react-class Blueprint/Switch))

(def tabs            (reagent/adapt-react-class Blueprint/Tabs2))

(def tab             (reagent/adapt-react-class Blueprint/Tab2))

(def tooltip         (reagent/adapt-react-class Blueprint/Tooltip))

(def tree            (reagent/adapt-react-class Blueprint/Tree))

(def hotkeys         (reagent/adapt-react-class Blueprint/Hotkeys))

(def hotkey          (reagent/adapt-react-class Blueprint/Hotkey))

(defn hotkeys-target
  [view hotkeys]
  (let [c (js/React.createClass
           #js {:renderHotkeys (fn [_] (reagent/as-element hotkeys))
                :render        (fn [_] (reagent/as-element view))})]
    (js/Blueprint.HotkeysTarget c)
    (reagent/adapt-react-class c)))

;;; Intents:

(def INTENT-NONE    (. Intent -NONE))
(def INTENT-PRIMARY (. Intent -PRIMARY))
(def INTENT-SUCCESS (. Intent -SUCCESS))
(def INTENT-WARNING (. Intent -WARNING))
(def INTENT-DANGER  (. Intent -DANGER))

;;; Other constants:

(def RIGHT  (. Position -RIGHT))
(def BOTTOM (. Position -BOTTOM))
(def LEFT   (. Position -LEFT))
(def TOP    (. Position -TOP))

(def ELEVATION-ONE   (. Elevation -ONE))
(def ELEVATION-TWO   (. Elevation -TWO))
(def ELEVATION-THREE (. Elevation -THREE))
(def ELEVATION-FOUR  (. Elevation -FOUR))
