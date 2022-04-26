;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.components
  (:require [imas-seamap.interop.ui-controls :as ui-controls]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn items-selection-list
  [{:keys [items disabled data-path]}]
  (let [items (map (fn [{:keys [key content]}] {:key key :content (reagent/as-element content)}) items)]
   [ui-controls/ItemsSelectionList
    {:items items
     :disabled disabled
     :onReorder (fn [src-idx dst-idx] (re-frame/dispatch [::selection-list-reorder src-idx dst-idx data-path]))}]))