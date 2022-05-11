;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.ui-controls
  (:require [reagent.core :as reagent]
            ["/SelectionList/SelectionList" :as SelectionList]
            ["/Drawer/Drawer" :as Drawer]))

(assert SelectionList/ItemsSelectionList)
(assert Drawer/Drawer)

(def ItemsSelectionList (reagent/adapt-react-class SelectionList/ItemsSelectionList))
(def Drawer             (reagent/adapt-react-class Drawer/Drawer))