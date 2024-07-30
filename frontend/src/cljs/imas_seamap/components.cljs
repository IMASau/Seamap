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
  [{:keys [items disabled data-path is-reversed has-handle]}]
  (let [items (map (fn [{:keys [key content]}] {:key key :content (reagent/as-element content)}) items)]
    [ui-controls/ItemsSelectionList
     {:items (if is-reversed (reverse items) items)
      :disabled disabled
      :has-handle has-handle
      :onReorder (fn [src-idx dst-idx] (re-frame/dispatch [::selection-list-reorder (if is-reversed (- (count items) src-idx 1) src-idx) (if is-reversed (- (count items) dst-idx 1) dst-idx) data-path]))}]))

(defn panel-stack
  [{:keys [panels on-close showPanelHeader]}]
  (let [panels (map #(update % :content reagent/as-element) panels)]
    [ui-controls/PanelStack
     {:panels panels
      :onClose on-close
      :showPanelHeader showPanelHeader}]))

(defn drawer
  [{:keys [title position size isOpen onClose hasBackdrop isCloseButtonShown className]} & children]
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
      :isCloseButtonShown isCloseButtonShown
      :className   className}]))

(defn floating-pill-button
  [{:keys [text icon on-click disabled]}]
  [:div
   {:class    (str "floating-pill floating-pill-button" (when disabled " disabled"))
    :on-click (when-not disabled on-click)}
   [b/icon
    {:icon icon
     :size 16}]
   [:div.title text]])

(defn floating-pill-control-menu
  "This component renders a floating pill which the user can click on to display a
   pop-out menu that may contain a wide variety of content. Clicking on the pill
   again will collapse the menu. Menu contains a header with a button that can also
   be used for closing it.
   
   Props configure the component:
    - text: Text displayed on the floating pill.
    - icon: Icon displayed on the floating pill.
    - disabled? (optional): If true then the button is disabled and the user will be
      unable to interact with it.
    - expanded? (optional): For if the state of the component needs to be managed
      outside. Determines if the pop-out menu is currently visible.
    - on-open-click (optional): For if the state of the component needs to be
      managed outside. Event that fires when the pill is clicked that would
      normally toggle the visibility of the pop-out menu when using an unmanaged
      state.
    - on-close-click (optional): For if the state of the component needs to be
      managed outside. Event that fires when the cross in the pop-out menu is
      clicked that would normally close the pop-out menu when using an unmanaged
      state.
    - reset-click (optional): When this is set the caret iccon will be replaced
      with a cross, and clicking the cross will perform this action.
    - & children: the children rendered inside of the pop-out menu."
  [{:keys [_text _icon _disabled? _expanded? _on-open-click _on-close-click _reset-click _active? _tooltip _id] :as _props} & _children]
  (let [atom-expanded? (reagent/atom false)]
    (fn [{:keys [text icon disabled? expanded? on-open-click on-close-click reset-click active? tooltip id] :as props} & children]
      (let [expanded?       (if (contains? props :expanded?) expanded? @atom-expanded?)
            on-open-click   (or on-open-click #(reset! atom-expanded? true))
            on-close-click  (or on-close-click #(reset! atom-expanded? false))
            button          [:div
                             (merge
                              {:class    (str "floating-pill floating-pill-control-menu-button" (when disabled? " disabled") (when active? " active"))
                               :on-click (when-not disabled? (if expanded? on-close-click on-open-click))}
                              (when id {:id id}))
                             [b/icon
                              {:icon icon
                               :size 16}]
                             [b/clipped-text {:ellipsize true :class "title"} text]
                             (if reset-click
                               [b/button
                                {:icon "cross"
                                 :minimal true
                                 :on-click (fn [e]
                                             (.stopPropagation e) ; so we don't fire on-open-click or on-close-click
                                             (reset-click))}]
                               [b/icon
                                {:icon (if expanded? "caret-up" "caret-down")}])]]
        [:div
         {:class (str "floating-pill-control-menu" (when expanded? " expanded"))}
         (if tooltip
           [b/tooltip {:content tooltip} button]
           button)
         [:div.floating-pill-control-menu-content
          [:div.floating-pill-control-menu-content-header
           [:div
            [b/icon
             {:icon icon
              :size 16}]]
           [b/clipped-text {:ellipsize true :class "title"} text]
           [:div
            [b/button
             {:icon "cross"
              :minimal true
              :on-click on-close-click}]]]
          (into [:div] children)]]))))

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
  [{:keys [value options onChange isSearchable isClearable isDisabled isMulti keyfns]}]
  (letfn [(option->select-option
            [option]
            (if-let [{:keys [id text breadcrumbs is-disabled?]} keyfns]
              (merge
               {:id     (id option)
                :text   (text option)
                :option option}
               (when is-disabled? {:isDisabled (is-disabled? option)})
               (when breadcrumbs {:breadcrumbs (breadcrumbs option)}))
              option))]
    (let [options (map option->select-option options)
          value   (if isMulti
                    (mapv #(:id (option->select-option %)) value)
                    (:id (option->select-option value)))]
      [ui-controls/Select
       {:value        value
        :options      options
        :onChange     (fn [id]
                        (if isMulti
                          (onChange (mapv (fn [id] (:option (first-where #(= (:id %) id) options))) id))
                          (onChange (:option (first-where #(= (:id %) id) options)))))
        :isSearchable isSearchable
        :isClearable  isClearable
        :isDisabled   isDisabled
        :isMulti      isMulti}])))

(defn form-group
  [{:keys [label class]} & children]
  [:div.form-group
   (when class {:class class})
   [:div label]
   (into [:div] children)])

(defn donut-chart
  [{:keys [id values independent-var dependent-var sort-key color legend-title]}]
  (let [values (if sort-key (sort-by sort-key values) values)
        spec {:description "A simple donut chart with embedded data."
              :width       "container"
              :data        {:values values}
              :mark        {:type   "arc" :innerRadius 60}
              :encoding    {:theta  {:field dependent-var :type "quantitative"}
                            :color  (merge
                                     {:field  independent-var
                                      :type   "nominal"
                                      :legend {:title (or legend-title independent-var)}}
                                     (when sort-key {:sort {:field sort-key}})
                                     (when color
                                       {:sort  (map independent-var values)
                                        :scale {:range (map color values)}}))}}]
    (embed (str "#" id) (clj->js spec) (clj->js {:actions false}))
    [:div.donut-chart
     {:id id}]))

(defn drawer-group
  [{:keys [heading icon collapsed? toggle-collapse class]} & children]
  [:div
   {:class (str "drawer-group" (when collapsed? " collapsed") (when toggle-collapse " collapsible") (when class (str " " class)))}
   [:div.drawer-group-heading
    (when toggle-collapse {:on-click toggle-collapse})
    (when icon
      [b/icon
       {:icon icon
        :icon-size 20}])
    [:h1 heading]
    (when toggle-collapse
      [b/icon
       {:icon (if collapsed? "double-chevron-down" "double-chevron-up")
        :icon-size 20}])]
   (into [:div.drawer-group-content] children)])

(defn breadcrumbs
  [{:keys [content]}]
  (let [content (map #(vector :span %) content)
        content (interpose [b/icon {:icon "caret-right"}] content)]
    (into [:div.breadcrumbs] content)))

(defn custom-icon [{:keys [icon size]}]
  [:div.custom-icon
   {:style
    (merge
     {:mask-image (str "url(icons/" icon ".svg)")
      :-webkit-mask-image (str "url(icons/" icon ".svg)")}
     (when size {:height size :width size}))}])

;; Form-3 component necessary because we only want to add the event listener
;; once.
;; The event listener for triggering the dialog close needs to be used because
;; Blueprint's canEscapeKeyClose refuses to work - perhaps due to some friction
;; with ClojureScript?
(defn hotkeys-render-dialog
  [{{:keys [_hotkeys _isDialogOpen]} :state
    {:keys [handleDialogClose]} :context-actions}]
  (reagent/create-class
   {:component-did-mount
    (fn [_]
      (js/window.addEventListener
       "keydown"
       #(when (= (.-key %) "Escape") (handleDialogClose))))
    :reagent-render
    (fn
      [{{:keys [hotkeys isDialogOpen]} :state
        {:keys [handleDialogClose]} :context-actions}]
      [b/dialogue
       {:title "Keyboard Actions"
        :class "bp3-hotkey-dialog"
        :is-open isDialogOpen
        :on-close handleDialogClose}
       [:div.bp3-dialog-body
        [:div.bp3-hotkey-column
         (map-indexed
          (fn [i hotkey]
            ^{:key i}
            [b/hotkey hotkey])
          hotkeys)]]])}))
