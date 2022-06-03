;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.components
  (:require [imas-seamap.interop.ui-controls :as ui-controls]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]))

(defn items-selection-list
  [{:keys [items disabled data-path is-reversed]}]
  (let [items (map (fn [{:keys [key content]}] {:key key :content (reagent/as-element content)}) items)]
   [ui-controls/ItemsSelectionList
    {:items (if is-reversed (reverse items) items)
     :disabled disabled
     :onReorder (fn [src-idx dst-idx] (re-frame/dispatch [::selection-list-reorder (if is-reversed (- (count items) src-idx 1) src-idx) (if is-reversed (- (count items) dst-idx 1) dst-idx) data-path]))}]))

(defn panel-stack
  [{:keys [panels on-close]}]
  (let [panels (map #(update % :content reagent/as-element) panels)]
    [ui-controls/PanelStack
     {:panels panels
      :onClose on-close}]))

(defn drawer
  [{:keys [title position size isOpen onClose hasBackdrop]} & children]
  (let [title (reagent/as-element title)
        children (reagent/as-element (into [:div] children))]
   [ui-controls/Drawer
    {:title       title
     :position    position
     :size        size
     :children    children
     :isOpen      isOpen
     :onClose     onClose
     :hasBackdrop hasBackdrop}]))

(defn floating-pill-button
  [{:keys [text icon on-click disabled]}]
  [b/button
   {:className "floating-pill-button"
    :text      text
    :icon
    (reagent/as-element
     [b/icon
      {:icon icon
       :icon-size 20}])
    :on-click  on-click
    :disabled  disabled}])

(defn omnibar
  [{:keys [placeholder, isOpen, onClose, items, onItemSelect]}]
  [ui-controls/Omnibar
   {:placeholder  placeholder
    :isOpen       isOpen
    :onClose      onClose
    :items        items
    :onItemSelect onItemSelect}])
