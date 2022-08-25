;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.blueprint
  "A collection of components adapted from blueprintjs"
  (:require [reagent.core :as reagent]
            ["@blueprintjs/core" :as Blueprint :refer [Intent Position Elevation]]
            ["create-react-class" :as create-react-class]))


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

(def slider          (reagent/adapt-react-class Blueprint/Slider))

(def switch          (reagent/adapt-react-class Blueprint/Switch))

(def tabs            (reagent/adapt-react-class Blueprint/Tabs))

(def tab             (reagent/adapt-react-class Blueprint/Tab))

(def tooltip         (reagent/adapt-react-class Blueprint/Tooltip))

(def tree            (reagent/adapt-react-class Blueprint/Tree))

(def hotkeys-provider (reagent/adapt-react-class Blueprint/HotkeysProvider))

(def checkbox        (reagent/adapt-react-class Blueprint/Checkbox))

(def use-hotkeys
  "We need to be careful with the use of this; it needs a native-js
  array, but if we wrap
  this (use-hotkeys=#(Blueprint/useHotkeys (clj->js %)) for eg), the
  identity checks will cause continuous re-rendering, so make sure
  clj-js is called *once*:"
  Blueprint/useHotkeys)

;; (defonce toaster (Blueprint/Toaster.create))

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
