(ns imas-seamap.map.layer-views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [clojure.string :as string]
            [imas-seamap.utils :refer [first-where round-to-nearest]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- layer-status-icons
  "Icons that appear to the left of the layer name to indicate the current state."
  [{:keys [loading? errors?]}]
  (when (or loading? errors?)
   [:div.layer-status-icons
    (when loading? [b/spinner {:class "bp3-small"}])
    (when errors? [b/icon {:icon "warning-sign" :size 16}])]))

(defn- layer-card-status-icons
  "Icons that appear to the left of the layer name to indicate the current state."
  [{:keys [loading? errors?]}]
  (when (or loading? errors?)
    [:div.layer-status-icons
     (when loading? [b/spinner {:class "bp3-small"}])
     (when errors? [b/icon {:icon "warning-sign" :size 18}])]))

(defn- cql-control-filter-text
  [{:keys [label value values controller-type]}]
  (case controller-type
    "dropdown"
    (when value (str "FILTER APPLIED: " label ": " value))
    "multi-dropdown"
    (when (seq value)
      (str
       "FILTER APPLIED: " label ": "
       (if (= (count value) 1)
         (first value)
         (apply str (interpose ", " value)))))
    "slider"
    (when (and value (not= value (apply max values)))
      (str "FILTER APPLIED: " label ": " value))))

(defn- layer-header-text
  "Layer name, with some other fancy stuff on top."
  [{{:keys [name tooltip]} :layer
    {{:keys [alternate-views-selected timeline-selected displayed-layer slider-label controls] :as rich-layer} :rich-layer} :layer-state}]
  (let [filters-text
        (remove
         nil?
         (conj
          (map cql-control-filter-text controls)
          (when alternate-views-selected
            (str "FILTER APPLIED: Alternate view: " (get-in alternate-views-selected [:layer :name])))
          (when timeline-selected
            (str "FILTER APPLIED: " slider-label ": " (get-in timeline-selected [:label])))))]
    [:div.layer-header-text
     [b/tooltip
      {:content
       (if (seq filters-text)
         (reagent/as-element
          [:div {:style {:max-width "320px"}}
           (apply str (interpose "\n" filters-text))
           [b/button
            {:icon     "cross"
             :minimal  true
             :on-click
             #(do
                (.stopPropagation %)
                (re-frame/dispatch [:map.rich-layer/reset-filters rich-layer]))}]])
         (reagent/as-element
          [:div {:style {:max-width "320px"}}
           tooltip]))
       :disabled (not (or alternate-views-selected timeline-selected (seq tooltip)))
       :hover-close-delay
       (if (seq filters-text) 1000 0)}
      [b/clipped-text {:ellipsize true} (or (:name displayed-layer) name)]]]))

(defn- layer-card-header-text
  "Layer name, with some other fancy stuff on top. Clicking it will expand the
   layer's details."
  [{{:keys [name tooltip]} :layer
    {:keys [expanded?]
     {:keys [alternate-views-selected timeline-selected displayed-layer slider-label controls] :as rich-layer} :rich-layer} :layer-state}]
  (let [filters-text
        (remove
         nil?
         (conj
          (map cql-control-filter-text controls)
          (when alternate-views-selected
            (str "FILTER APPLIED: Alternate view: " (get-in alternate-views-selected [:layer :name])))
          (when timeline-selected
            (str "FILTER APPLIED: " slider-label ": " (get-in timeline-selected [:label])))))]
    [:div.layer-header-text
     [b/tooltip
      {:content
       (cond
         (seq filters-text)
         (reagent/as-element
          [:div {:style {:max-width "320px"}}
           (apply str (interpose "\n" filters-text))
           [b/button
            {:icon     "cross"
             :minimal  true
             :on-click
             #(do
                (.stopPropagation %)
                (re-frame/dispatch [:map.rich-layer/reset-filters rich-layer]))}]])

         (seq tooltip)
         (reagent/as-element
          [:div {:style {:max-width "320px"}}
           tooltip])

         expanded? "Hide legend"
         :else     "Show legend")
       :hover-close-delay
       (if (seq filters-text) 1000 0)}
      [b/clipped-text {:ellipsize true} (or (:name displayed-layer) name)]]]))

(defn- layer-control
  "Basic layer control. It's an icon with a tooltip that does something when
   clicked."
  [{:keys [tooltip icon on-click]}]
  [b/tooltip {:content tooltip}
   [b/icon
    {:icon     icon
     :class    "layer-control"
     :size     16
     :on-click #(do
                  (.stopPropagation %)
                  (on-click))}]])

(defn- layer-card-control
  "Basic layer control. It's an icon with a tooltip that does something when
   clicked."
  [{:keys [tooltip icon on-click]}]
  [b/tooltip {:content tooltip}
   [b/icon
    {:icon     icon
     :class    "layer-control"
     :size     18
     :on-click #(do
                  (.stopPropagation %)
                  (on-click))}]])

(defn- layer-card-controls
  "To the right of the layer name. Basic controls for the layer, like getting info
   and disabling the layer."
  [{:keys [layer tma?] {:keys [visible?]} :layer-state}]
  [:div.layer-controls
   
   [layer-card-control
    {:tooltip  (if visible? "Hide layer" "Show layer")
     :icon     (if visible? "eye-on" "eye-off")
     :on-click #(re-frame/dispatch [:map/toggle-layer-visibility layer])}]
   
   [layer-card-control
    {:tooltip  (if tma? "Layer info" "Layer info / Download data")
     :icon     "info-sign"
     :on-click #(re-frame/dispatch [:map.layer/show-info layer])}]

   [layer-card-control
    {:tooltip  "Zoom to layer"
     :icon     "locate"
     :on-click #(re-frame/dispatch [:map/pan-to-layer layer])}]])

(defn- opacity-slider
  [{:keys [layer] {:keys [opacity]} :layer-state}]
  [:input
   {:type "range"
    :min 0 :max 100 :value opacity
    :on-click #(.stopPropagation %)
    :on-input #(re-frame/dispatch [:map.layer/opacity-changed layer (.. % -target -value)])}])

(defn- layer-card-header
  "Top part of layer card. Always visible. Contains the layer status, name, and
   basic controls for the layer."
  [{:keys [layer] {:keys [active? visible? _opacity] :as layer-state} :layer-state :as props}]
  [:div.layer-header
   [:div
    (when (and active? visible?)
      [layer-card-status-icons layer-state])
    [layer-card-header-text props]

    [layer-card-control
     {:tooltip  "Deactivate layer"
      :icon     "delete"
      :on-click #(re-frame/dispatch [:map/toggle-layer layer])}]]
   [:div
    [opacity-slider props]
    [layer-card-controls props]]])

(defn- vector-legend-entry [{:keys [label image style] :as _entry}]
  [:div.vector-legend-entry
   [:div.key
    (if image
      [:img {:src image}]
      [:div {:style style}])]
   [b/clipped-text
    {:ellipsize true :class "label"}
    ((fnil string/replace "") label #"\\n" "\n")]])


(defn- legend [legend-info]
  (if (string? legend-info)
    [:img {:src legend-info}] ; if legend-info is a string, we treat it as a url to a legend graphic
    [:<>                      ; else we render the legend as a vector legend
     (map-indexed
      (fn [i entry]
        ^{:key (str i)}
        [vector-legend-entry entry])
      legend-info)]))

(defn- legend-display [{:keys [legend_url] :as layer}]
  (let [{:keys [status info]} @(re-frame/subscribe [:map.layer/legend layer])]
    [:div.legend-wrapper
     (if legend_url 
       [legend legend_url] ; if we have a custom legend url, use that
       (case status        ; else use legend status to decide action

         :map.legend/loaded
         [legend info]

         :map.legend/loading
         [b/non-ideal-state
          {:icon  (reagent/as-element [b/spinner {:intent "success"}])}]

         :map.legend/unsupported-layer
         [b/non-ideal-state
          {:title       "Unsupported Layer"
           :description "This layer does not currently support legends."
           :icon        "warning-sign"}]

         :map.legend/error
         [b/non-ideal-state
          {:title       "Unexpected Error"
           :description "There was an issue in retrieving the legend."
           :icon        "error"}]

         :map.legend/none
         [b/non-ideal-state
          {:title       "No Data"
           :description "We are unable to display any legend data at this time."
           :icon        "info-sign"}]))]))

(defn- alternate-view-select
  [{:keys [layer]
    {{:keys [alternate-views alternate-views-selected] :as rich-layer} :rich-layer} :layer-state}]
  [components/form-group
   {:label    "Alternate View"}
   [:div
    {:on-click #(.stopPropagation %)}
    [components/select
     {:value        alternate-views-selected
      :options      alternate-views
      :onChange     #(re-frame/dispatch [:map.rich-layer/alternate-views-selected rich-layer %])
      :isSearchable true
      :isClearable  true
      :keyfns
      {:id   #(get-in % [:layer :id])
       :text #(get-in % [:layer :name])}}]]])

(defn- timeline-select
  [{:keys [layer]
    {{:keys [timeline timeline-selected timeline-disabled? slider-label displayed-layer] :as rich-layer} :rich-layer} :layer-state}]
  (let [timeline   (sort-by :value timeline)
        values     (map :value timeline)
        gaps      (:gaps
                   (reduce
                    (fn [{:keys [gaps prev]} val]
                      {:gaps (if prev
                               (conj gaps (- val prev))
                               gaps)
                       :prev val})
                    {:gaps [] :prev nil} values))
        
        labels-with-gaps
        (butlast
         (interleave
          (map :label timeline)
          (conj gaps nil)))]
    [components/form-group {:label slider-label}
     [:input
      {:type     "range"
       :min      (apply min values)
       :max      (apply max values)
       :step     0.01
       :value    (:value (or timeline-selected (first-where #(= (get-in % [:layer :id]) (:id (or displayed-layer layer))) timeline)))
       :on-click #(.stopPropagation %)
       :on-input (fn [e]
                   (let [value (-> e .-target .-value)
                         nearest-value (round-to-nearest value values)]
                     (re-frame/dispatch [:map.rich-layer/timeline-selected rich-layer (first-where #(= (:value %) nearest-value) timeline)])))
       :disabled timeline-disabled?}]
     [:div.time-range
      (map-indexed
       (fn [i v]
         [:div
          {:key   i
           :style (when (odd? i) {:flex v})}
          (when (even? i) v)])
       labels-with-gaps)]]))

(defmulti cql-control #(get-in % [:control :controller-type]))

(defmethod cql-control "dropdown"
  [{{:keys [label icon tooltip value values] :as control} :control {{:keys [rich-layer]} :layer-state} :props}]
  [components/form-group
   {:label
    [b/tooltip
     {:content tooltip}
     [:<>
      (when icon [b/icon {:icon icon}])
      label]]}
   [:div
    {:on-click #(.stopPropagation %)}
    [components/select
     {:value        value
      :options      values
      :onChange     #(re-frame/dispatch [:map.rich-layer/control-selected rich-layer control %])
      :isSearchable true
      :isClearable  true
      :keyfns
      {:id   identity
       :text str}}]]])

(defmethod cql-control "multi-dropdown"
  [{{:keys [label icon tooltip value values] :as control} :control {{:keys [rich-layer]} :layer-state} :props}]
  [components/form-group
   {:label
    [b/tooltip
     {:content tooltip}
     [:<>
      (when icon [b/icon {:icon icon}])
      label]]}
   [:div
    {:on-click #(.stopPropagation %)}
    [components/select
     {:value        value
      :options      values
      :onChange     #(re-frame/dispatch [:map.rich-layer/control-selected rich-layer control %])
      :isSearchable true
      :isClearable  true
      :isMulti      true
      :keyfns
      {:id   identity
       :text str}}]]])

(defmethod cql-control "slider"
  [{{:keys [label icon tooltip value values] :as control} :control {{:keys [rich-layer]} :layer-state} :props}]
  (let [gaps (:gaps
              (reduce
               (fn [{:keys [gaps prev]} val]
                 {:gaps (if prev
                          (conj gaps (- val prev))
                          gaps)
                  :prev val})
               {:gaps [] :prev nil} values))

        labels-with-gaps
        (butlast
         (interleave
          values
          (conj gaps nil)))]
    [components/form-group
     {:label
      [b/tooltip
       {:content tooltip}
       [:<>
        (when icon [b/icon {:icon icon}])
        label]]}
     [:input
      {:type     "range"
       :min      (apply min values)
       :max      (apply max values)
       :step     0.01
       :value    (or value 0)
       :on-click #(.stopPropagation %)
       :on-input
       (fn [e]
         (let [value (-> e .-target .-value)
               nearest-value (round-to-nearest value values)]
           (re-frame/dispatch [:map.rich-layer/control-selected rich-layer control nearest-value])))}]
     [:div.time-range
      (map-indexed
       (fn [i v]
         [:div
          {:key   i
           :style (when (odd? i) {:flex v})}
          (when (even? i) v)])
       labels-with-gaps)]]))

(defmethod cql-control :default
 [{{:keys [label]} :control {:keys []} :rich-layer}]
  [:div label])

(defn- layer-details
  "Layer details for layer card. Includes layer's legend, and tabs for selecting
   filters if the layer is a rich-layer."
  [{:keys [layer]
    {{:keys [tab displayed-layer alternate-views timeline controls tab-label icon cql-filter] :as rich-layer} :rich-layer} :layer-state
    :as props}]
  [:div.layer-details
   {:on-click #(.stopPropagation %)}
   (if rich-layer
     [b/tabs
      {:selected-tab-id tab
       :on-change       #(re-frame/dispatch [:map.rich-layer/tab rich-layer %])}

      [b/tab
       {:id    "legend"
        :title (reagent/as-element [:<> [b/icon {:icon "key"}] "Legend"])
        :panel
        (reagent/as-element
         [:div
          {:on-click #(re-frame/dispatch [:map.layer.legend/toggle layer])}
          (when displayed-layer [:h2 (:name displayed-layer)])
          [legend-display (or displayed-layer layer)]])}]

      [b/tab
       {:id    "filters"
        :title (reagent/as-element [:<> [b/icon {:icon icon}] tab-label])
        :panel
        (reagent/as-element
         [:div
          {:on-click #(re-frame/dispatch [:map.layer.legend/toggle layer])}
          (when (seq alternate-views) [alternate-view-select props])
          (when (seq timeline) [timeline-select props])
          (for [control controls]
            ^{:key (:label control)}
            [cql-control
             {:control control
              :props   props}])])}]]
     
     [legend-display layer])])

(defn- layer-card-content
  "Content of a layer card; includes both the header and the details that can be
   expanded and collapsed."
  [{{:keys [tooltip]} :layer
    {:keys [active? expanded?]
     {:keys [alternate-views-selected timeline-selected]} :rich-layer} :layer-state
    :as props}]
  [:div
   {:class (when (or (seq tooltip) alternate-views-selected timeline-selected) "has-tooltip")}
   [layer-card-header props]
   [b/collapse {:is-open (and active? expanded?)}
    [layer-details props]]])

(defn layer-card
  "Wrapper of layer-card-content in a card for displaying in lists."
  [{:keys [_layer-state layer _tma?] :as props}]
  [:div.layer-card
   {:on-click  #(re-frame/dispatch [:map.layer.legend/toggle layer])}
   [layer-card-content props]])

(defn- layer-catalogue-controls
  "To the right of the layer name. Basic controls for the layer, like getting info
   and enabling/disabling the layer. Differs from layer-card-controls in what
   controls are displayed."
  [{:keys [layer tma?]
    {{:keys [icon tooltip] :as rich-layer} :rich-layer} :layer-state}]
  [:div.layer-controls

   (when rich-layer
     [layer-control
      {:tooltip  tooltip
       :icon     icon
       :on-click #(re-frame/dispatch [:map.rich-layer/configure layer])}])

   [layer-control
    {:tooltip  (if tma? "Layer info" "Layer info / Download data")
     :icon     "info-sign"
     :on-click #(re-frame/dispatch [:map.layer/show-info layer])}]

   [layer-control
    {:tooltip  "Zoom to layer"
     :icon     "locate"
     :on-click #(re-frame/dispatch [:map/pan-to-layer layer])}]])

(defn layer-catalogue-header
  "Top part of layer catalogue element. Always visible. Contains the layer status,
   name, and basic controls for the layer. Differs from layer-card-header in what
   controls are displayed."
  [{:keys [layer _tma?] {:keys [active? visible?] :as layer-state} :layer-state :as props}]
  [:div.layer-header
   [b/tooltip {:content (if active? "Deactivate layer" "Activate layer")}
    [b/checkbox
     {:checked (boolean active?)
      :on-change #(re-frame/dispatch [:map/toggle-layer layer])}]]
   (when (and active? visible?)
     [layer-status-icons layer-state])
   [layer-header-text props]
   [layer-catalogue-controls props]])

(defn layer-catalogue-node
  [{{:keys [active-layers visible-layers loading-fn expanded-fn error-fn opacity-fn rich-layer-fn]} :layer-props
    {:keys [tooltip] :as layer} :layer
    :keys [id tma?]}]
  (let [active? (some #{layer} active-layers)
        {:keys [alternate-views-selected timeline-selected] :as rich-layer} (rich-layer-fn layer)
        layer-state
        {:active?    active?
         :visible?   (some #{layer} visible-layers)
         :loading?   (loading-fn layer)
         :expanded?  (expanded-fn layer)
         :errors?    (error-fn layer)
         :opacity    (opacity-fn layer)
         :rich-layer rich-layer}]
    {:id        id
     :className (str
                 "catalogue-layer-node"
                 (when active? " active-layer")
                 (when (or (seq tooltip) alternate-views-selected timeline-selected) " has-tooltip"))
     :nodeData  {:previewLayer layer}
     :label     (reagent/as-element [layer-catalogue-header {:layer layer :layer-state layer-state :tma? tma?}])}))
