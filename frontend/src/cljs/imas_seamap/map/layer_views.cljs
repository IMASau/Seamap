(ns imas-seamap.map.layer-views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [with-params]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- layer-status-icons [{:keys [loading? errors?]}]
  (when (or loading? errors?)
   [:div.layer-status-icons
    (when loading? [b/spinner {:class "bp3-text-muted bp3-small"}])
    (when errors? [b/icon {:icon "warning-sign" :class "bp3-text-muted bp3-small"}])]))

(defn- layer-header-text [{{:keys [name] :as layer} :layer {:keys [expanded? active?]} :other-props}]
  [b/tooltip
   {:content (if expanded? "Hide details" "Show details")
    :disabled (not active?)
    :class "layer-header-text"}
   [b/clipped-text
    {:ellipsize true
     :on-click  #(re-frame/dispatch [:map.layer.legend/toggle layer])}
    name]])

(defn- layer-control [{:keys [tooltip icon on-click]}]
  [b/tooltip {:content tooltip}
   [b/icon
    {:icon    icon
     :class   "bp3-text-muted layer-control"
     :on-click on-click}]])

(defn- layer-controls [{:keys [layer] {:keys [visible? active?]} :other-props}]
  [:div.layer-controls

   [layer-control
    {:tooltip  "Layer info / Download data"
     :icon     "info-sign"
     :on-click #(re-frame/dispatch [:map.layer/show-info layer])}]

   [layer-control
    {:tooltip  "Zoom to layer"
     :icon     "zoom-to-fit"
     :on-click #(re-frame/dispatch [:map/pan-to-layer layer])}]

   (when active?
     [b/tooltip {:content (if visible? "Hide layer" "Show layer")}
      [b/icon
       {:icon     (if visible? "eye-on" "eye-off")
        :size     20
        :class    "bp3-text-muted layer-control"
        :on-click #(re-frame/dispatch [:map/toggle-layer-visibility layer])}]])

   (when active?
     [layer-control
      {:tooltip  "Deactivate layer"
       :icon     "remove"
       :on-click #(re-frame/dispatch [:map/toggle-layer layer])}])
   
   (when-not active?
     [b/tooltip {:content (if active? "Deactivate layer" "Activate layer")}
      [b/checkbox
       {:checked (boolean active?)
        :on-change #(re-frame/dispatch [:map/toggle-layer layer])}]])])

(defn- layer-header [{:keys [_layer] {:keys [active? visible?] :as other-props} :other-props :as layer-props}]
  [:div.layer-header
   (when (and active? visible?)
     [layer-status-icons other-props])
   [layer-header-text layer-props]
   [layer-controls layer-props]])

(defn- legend-display [{:keys [legend_url server_url layer_name style]}]
  ;; Allow a custom url via the legend_url field, else construct a GetLegendGraphic call:
  (let [legend-url (or legend_url
                       (with-params server_url
                         (merge
                          {:REQUEST "GetLegendGraphic"
                           :LAYER layer_name
                           :FORMAT "image/png"
                           :TRANSPARENT true
                           :SERVICE "WMS"
                           :VERSION "1.1.1"
                           :LEGEND_OPTIONS "forceLabels:on"}
                          (when style {:STYLE style}))))]
    [:div.legend-wrapper
     [:img {:src legend-url}]]))

(defn- layer-details [{:keys [layer] {:keys [opacity]} :other-props}]
  [:div.layer-details
   [b/slider
    {:label-renderer false :initial-value 0 :max 100 :value opacity
     :on-change #(re-frame/dispatch [:map.layer/opacity-changed layer %])}]
   [legend-display layer]])

(defn layer-content [{:keys [_layer] {:keys [active? expanded?]} :other-props :as layer-props}]
  [:div.layer-content
   {:class (when active? "active-layer")}
   [layer-header layer-props]
   [b/collapse {:is-open (and active? expanded?)}
    [layer-details layer-props]]])

(defn layer-card [{:keys [_layer _other-props] :as layer-props}]
  [b/card
   {:elevation 1
    :class     "layer-card"}
   [layer-content layer-props]])
