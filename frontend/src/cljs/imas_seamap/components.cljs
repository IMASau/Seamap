;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.components
  (:require [imas-seamap.interop.ui-controls :as ui-controls]
            [imas-seamap.utils :refer [first-where]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            ["vega-embed" :refer [embed]]))

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
  [{:keys [title position size isOpen onClose hasBackdrop className]} & children]
  (let [title (reagent/as-element title)
        children (reagent/as-element (into [:div.drawer-children] children))]
    [ui-controls/Drawer
     {:title       title
      :position    position
      :size        size
      :children    children
      :isOpen      isOpen
      :onClose     onClose
      :hasBackdrop hasBackdrop
      :className   className}]))

(defn floating-pill-button
  [{:keys [text icon on-click disabled]}]
  [:div
   {:class    (str "floating-pill-button" (when disabled " disabled"))
    :on-click (when-not disabled on-click)}
   [b/icon
    {:icon icon
     :icon-size 20}]
   text])

(defn omnibar
  [{:keys [placeholder isOpen onClose items onItemSelect keyfns]}]
  (letfn [(item->omnibar-item
            [item]
            (if-let [{:keys [id text keywords breadcrumbs]} keyfns]
              (merge
               {:id       (id item)
                :text     (text item)
                :keywords (keywords item)
                :item     item}
               (when breadcrumbs {:breadcrumbs (breadcrumbs item)}))
              item))]
    (let [items (map item->omnibar-item items)]
      [ui-controls/Omnibar
       {:placeholder  placeholder
        :isOpen       isOpen
        :onClose      onClose
        :items        items
        :onItemSelect (fn [id] (onItemSelect (:item (first-where #(= (:id %) id) items))))}])))

(defn select
  [{:keys [value options onChange keyfns]}]
  (letfn [(option->select-option
            [option]
            (if-let [{:keys [id text breadcrumbs]} keyfns]
              (merge
               {:id     (id option)
                :text   (text option)
                :option option}
               (when breadcrumbs {:breadcrumbs (breadcrumbs option)}))
              option))]
    (let [options (map option->select-option options)
          value   (:id (option->select-option value))]
      [ui-controls/Select
       {:value    value
        :options  options
        :onChange (fn [id] (onChange (:option (first-where #(= (:id %) id) options))))}])))

(defn form-group
  [{:keys [label]} & children]
  [:div.form-group
   [:div label]
   (into [:div] children)])

(defn donut-chart
  [{:keys [id values theta color legend-title]}]
  (let [spec {:description "A simple donut chart with embedded data."
              :width       "container"
              :data        {:values values}
              :mark        {:type "arc" :innerRadius 60}
              :encoding    {:theta {:field theta :type "quantitative"}
                            :color {:field color
                                    :type "nominal"
                                    :legend {:title (or legend-title color)}}}}]
    (embed (str "#" id) (clj->js spec) (clj->js {:actions false}))
    [:div.donut-chart
     {:id id}]))

(defn drawer-group
  [{:keys [heading icon collapsed? toggle-collapse]} & children]
  [:div
   {:class (str "drawer-group" (when collapsed? " collapsed") (when toggle-collapse " collapsible"))}
   [:h1
    (merge
     {:class (str "bp3-heading" (when icon (str " bp3-icon-" icon)))}
     (when toggle-collapse {:on-click toggle-collapse}))
    heading]
   (into [:div.drawer-group-content] children)])
