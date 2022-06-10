;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.ui-controls
  (:require [reagent.core :as reagent]
            ["/SelectionList/SelectionList" :as SelectionList]
            ["/Drawer/Drawer" :as Drawer]
            ["/PanelStack/PanelStack" :as PanelStack]
            ["/Omnibar/Omnibar" :as Omnibar]
            ["/Select/Select" :as Select]))

(assert SelectionList/ItemsSelectionList)
(assert Drawer/Drawer)
(assert PanelStack/PanelStack)

(def ItemsSelectionList (reagent/adapt-react-class SelectionList/ItemsSelectionList))
(def Drawer             (reagent/adapt-react-class Drawer/Drawer))
(def PanelStack         (reagent/adapt-react-class PanelStack/PanelStack))
(def Omnibar            (reagent/adapt-react-class Omnibar/Omnibar))
(def Select             (reagent/adapt-react-class Select/Select))
