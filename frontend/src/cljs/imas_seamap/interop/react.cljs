;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.react
  (:require [reagent.core :as reagent]
            ["react-transition-group" :refer [TransitionGroup CSSTransition]]
            ["react-sizeme" :refer [SizeMe]]
            ["react-leaflet-sidebarv2" :refer [Sidebar Tab]]))

(def css-transition-group
  ;; "The most straightforward way to migrate is to use <TransitionGroup> instead of <CSSTransitionGroup>:"
  (reagent/adapt-react-class TransitionGroup))

(def css-transition
  (reagent/adapt-react-class CSSTransition))

(def container-dimensions
  (reagent/adapt-react-class SizeMe))

(def sidebar     (reagent/adapt-react-class Sidebar))
(def sidebar-tab (reagent/adapt-react-class Tab))
