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

(defn panel-stack
  [{:keys [panels on-close]}]
  (let [panels (map #(update % :content reagent/as-element) panels)]
    [ui-controls/PanelStack
     {:panels panels
      :onClose on-close}]))

(defn seamap-drawer
  []
  (let [open? @(re-frame/subscribe [:seamap-drawer/open?])]
    [ui-controls/Drawer
     {:title
      (reagent/as-element
       [:div.seamap-drawer-header
        [:img
        {:src "img/Seamap2_V2_RGB.png"}]])
      :position "left"
      :size "460px"
      :children nil
      :isOpen  open?
      :onClose #(re-frame/dispatch [:seamap-drawer/close])}]))