(ns imas-seamap.map.layer-views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [clojure.string :as string]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- layer-status-icons
  "Icons that appear to the left of the layer name to indicate the current state."
  [{:keys [loading? errors?]}]
  (when (or loading? errors?)
   [:div.layer-status-icons
    (when loading? [b/spinner {:class "bp3-text-muted bp3-small"}])
    (when errors? [b/icon {:icon "warning-sign" :class "bp3-text-muted bp3-small"}])]))

(defn- layer-header-text
  "Layer name, with some other fancy stuff on top. Clicking it will expand the
   layer's details."
  [{{:keys [name] :as layer} :layer {:keys [expanded? active?]} :layer-state}]
  [:div.layer-header-text
   {:on-click  #(re-frame/dispatch [:map.layer.legend/toggle layer])}
   [b/tooltip
   {:content (if expanded? "Hide details" "Show details")
    :disabled (not active?)}
   [b/clipped-text {:ellipsize true}
    name]]])

(defn- layer-control
  "Basic layer control. It's an icon with a tooltip that does something when
   clicked."
  [{:keys [tooltip icon on-click]}]
  [b/tooltip {:content tooltip}
   [b/icon
    {:icon    icon
     :class   "bp3-text-muted layer-control"
     :on-click on-click}]])

(defn- layer-card-controls
  "To the right of the layer name. Basic controls for the layer, like getting info
   and disabling the layer."
  [{:keys [layer] {:keys [visible?]} :layer-state}]
  [:div.layer-controls

   [layer-control
    {:tooltip  "Layer info / Download data"
     :icon     "info-sign"
     :on-click #(re-frame/dispatch [:map.layer/show-info layer])}]

   [layer-control
    {:tooltip  "Zoom to layer"
     :icon     "zoom-to-fit"
     :on-click #(re-frame/dispatch [:map/pan-to-layer layer])}]

   [b/tooltip {:content (if visible? "Hide layer" "Show layer")}
    [b/icon
     {:icon     (if visible? "eye-on" "eye-off")
      :size     20
      :class    "bp3-text-muted layer-control"
      :on-click #(re-frame/dispatch [:map/toggle-layer-visibility layer])}]]

   [layer-control
    {:tooltip  "Deactivate layer"
     :icon     "remove"
     :on-click #(re-frame/dispatch [:map/toggle-layer layer])}]])

(defn- layer-card-header
  "Top part of layer card. Always visible. Contains the layer status, name, and
   basic controls for the layer."
  [{:keys [_layer] {:keys [active? visible?] :as layer-state} :layer-state :as props}]
  [:div.layer-header
   (when (and active? visible?)
     [layer-status-icons layer-state])
   [layer-header-text props]
   [layer-card-controls props]])

(defn- vector-legend-entry [{:keys [label style] :as _entry}]
  [:div.vector-legend-entry
   [:div.key
    [:div {:style style}]]
   [b/clipped-text
    {:ellipsize true :class "label"}
    (string/replace label #"\\n" "\n")]])

(defn- legend [legend-info]
  (if (string? legend-info)
    [:img {:src legend-info}] ; if legend-info is a string, we treat it as a url to a legend graphic
    [:<>                      ; else we render the legend as a vector legend
     (map
      (fn [{:keys [label] :as entry}]
        ^{:key label}
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

(defn- layer-details
  "Layer details, including advanced opacity slider control and the layer's legend."
  [{:keys [layer] {:keys [opacity]} :layer-state}]
  [:div.layer-details
   [b/slider
    {:label-renderer false :initial-value 0 :max 100 :value opacity
     :on-change #(re-frame/dispatch [:map.layer/opacity-changed layer %])}]
   [legend-display layer]])

(defn- layer-card-content
  "Content of a layer card; includes both the header and the details that can be
   expanded and collapsed."
  [{:keys [_layer] {:keys [active? expanded?]} :layer-state :as props}]
  [:div.layer-content
   {:class (when active? "active-layer")}
   [layer-card-header props]
   [b/collapse {:is-open (and active? expanded?)}
    [layer-details props]]])

(defn layer-card
  "Wrapper of layer-card-content in a card for displaying in lists."
  [{:keys [_layer _layer-state] :as props}]
  [b/card
   {:elevation 1
    :class     "layer-card"}
   [layer-card-content props]])

(defn- layer-catalogue-controls
  "To the right of the layer name. Basic controls for the layer, like getting info
   and enabling/disabling the layer. Differs from layer-card-controls in what
   controls are displayed."
  [{:keys [layer] {:keys [active?]} :layer-state}]
  [:div.layer-controls

   [layer-control
    {:tooltip  "Layer info / Download data"
     :icon     "info-sign"
     :on-click #(re-frame/dispatch [:map.layer/show-info layer])}]

   [layer-control
    {:tooltip  "Zoom to layer"
     :icon     "zoom-to-fit"
     :on-click #(re-frame/dispatch [:map/pan-to-layer layer])}]

   [b/tooltip {:content (if active? "Deactivate layer" "Activate layer")}
    [b/checkbox
     {:checked (boolean active?)
      :on-change #(re-frame/dispatch [:map/toggle-layer layer])}]]])

(defn- layer-catalogue-header
  "Top part of layer catalogue element. Always visible. Contains the layer status,
   name, and basic controls for the layer. Differs from layer-card-header in what
   controls are displayed."
  [{:keys [_layer] {:keys [active? visible?] :as layer-state} :layer-state :as props}]
  [:div.layer-header
   (when (and active? visible?)
     [layer-status-icons layer-state])
   [layer-header-text props]
   [layer-catalogue-controls props]])

(defn layer-catalogue-content
  "Content of a layer catalogue element; includes both the header and the details
   that can be expanded and collapsed."
  [{:keys [layer] {:keys [active? expanded?]} :layer-state :as props}]
  [:div.layer-content
   {:on-mouse-over #(re-frame/dispatch [:map/update-preview-layer layer])
    :on-mouse-out  #(re-frame/dispatch [:map/update-preview-layer nil])
    :class         (when active? "active-layer")}
   [layer-catalogue-header props]
   [b/collapse {:is-open (and active? expanded?)}
    [layer-details props]]])


;; Main national layer

(defn- main-national-layer-time-filter
  "Time filter for main national layer, which filters what layers are displayed on
   the map."
  []
  (let [value     (reagent/atom 2000)   ; Graduate to event probably at some point to be able to filter displayed layer
        all-time? (reagent/atom false)] ; Graduate to event probably at some point to be able to filter displayed layer
    (fn []
      [components/form-group {:label "Time"}
       [b/checkbox
        {:label    "At all points in time"
         :checked  @all-time?
         :on-click #(swap! all-time? not)}]
       [b/slider
        {:min             2000
         :max             2022
         :label-step-size (- 2022 2000)
         :value           @value
         :step-size       1
         :on-change       #(reset! value %)
         :disabled        @all-time?}]])))

(defn- main-national-layer-details
  "Expanded details for main national layer. Differs from regular details by having
   a tabbed view, with a tab for the legend and a tab for filters. The filters
   alter how the main national layer is displayed on the map."
  [{:keys [_layer] {:keys [_opacity]} :layer-state}]
  (let [selected-tab (reagent/atom "legend") 
        alternate-view (reagent/atom nil)]   ; Graduate to event probably at some point to be able to filter displayed layer
    (fn [{:keys [layer] {:keys [opacity]} :layer-state}]
      [:div.layer-details
       [b/slider
        {:label-renderer false :initial-value 0 :max 100 :value opacity
         :on-change #(re-frame/dispatch [:map.layer/opacity-changed layer %])}]
       
       [b/tabs
        {:selected-tab-id @selected-tab
         :on-change       #(reset! selected-tab %)}
        
        [b/tab
         {:id    "legend"
          :title "Legend"
          :panel
          (reagent/as-element
           [:<>
            (when @alternate-view [:h2.bp3-heading @alternate-view])
            [legend-display layer]])}]
        
        [b/tab
         {:id    "filters"
          :title "Filters"
          :panel
          (reagent/as-element
           [:<>
            [components/form-group
             {:label "Alternate View"}
             [components/select
              {:value        @alternate-view        
               :options
               ["CBICS Classified" "Seagrass" "..." ".." "."]
               :onChange     #(reset! alternate-view %)
               :isSearchable true
               :isClearable  true
               :keyfns
               {:id   identity
                :text identity}}]]
            [main-national-layer-time-filter]])}]]])))

(defn- main-national-layer-card-content
  "Content of the main national layer card; includes both the header and the main
   national layer details that can be expanded and collapsed."
  [{:keys [_layer] {:keys [active? expanded?]} :layer-state :as props}]
  [:div.layer-content
   {:class (when active? "active-layer")}
   [layer-card-header props]
   [b/collapse {:is-open (and active? expanded?)}
    [main-national-layer-details props]]])

(defn main-national-layer-card
  "Wrapper of main-national-layer-card-content in a card for displaying in lists."
  [{:keys [_layer _layer-state] :as props}]
  [b/card
   {:elevation 1
    :class     "layer-card"}
   [main-national-layer-card-content props]])
