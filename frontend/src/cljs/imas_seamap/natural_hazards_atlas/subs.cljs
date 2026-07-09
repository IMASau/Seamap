;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.subs
  (:require
   [re-frame.core :as re-frame]
   [imas-seamap.natural-hazards-atlas.utils :as nhatutils]
   [imas-seamap.utils :as utils]))

(defn current-view-cmip-phases
  "List of CMIP (Coupled Model Intercomparison Project) phases available to
   organize models and scenarios for analyzing hazard data."
  [db _]
  (get-in db [:current-view :cmip-phases]))

(defn current-view-models
  "List of scientific models available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :models]))

(defn current-view-scenarios
  "List of scenarios available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :scenarios]))

(defn current-view-seasonal-datas
  "List of seasonal data available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :seasonal-datas]))

(defn current-view-selected-cmip-phase
  "CMIP (Coupled Model Intercomparison Project) phase that organizes models and
   scenarios for analyzing hazard data."
  [db [_ map-id]]
  (nhatutils/current-view-selected-cmip-phase db map-id))

(defn current-view-selected-model
  "Scientific model to analyze the hazard data.

   If the value selected by the user isn't one of the models found in the current
   CMIP phase, then default to the first available model."
  [db [_ map-id]]
  (nhatutils/current-view-selected-model db map-id))

(defn current-view-selected-scenario
  "Scenario to analyze the hazard data.

   If the value selected by the user isn't one of the scenarios found in the
   current scientific model, then default to the first available scenario."
  [db [_ map-id]]
  (nhatutils/current-view-selected-scenario db map-id))

(defn current-view-selected-seasonal-data
  "Seasonal data to analyze the hazard data"
  [db [_ map-id]]
  (nhatutils/current-view-selected-seasonal-data db map-id))

(defn current-view-is-historic?
  "Indicates whether the current view is for historic data."
  [db [_ map-id]]
  (nhatutils/current-view-is-historic? db map-id))

(defn current-view-time-periods
  "List of time periods available to analyze the hazard data."
  [_db _]
  nhatutils/time-periods)

(defn current-view-selected-time-period
  "Time period to analyze the hazard data"
  [db [_ map-id]]
  (nhatutils/current-view-selected-time-period db map-id))

(defn current-view-timeline-media-controls-signals
  "Signals function for current-view-timeline-media-controls.

   Signals function is a necessity, in order to pass through subscription args"
  [[_ map-id]]
  [(re-frame/subscribe [:map.time/current-time map-id])
   (re-frame/subscribe [:map.time/available-times map-id])
   (re-frame/subscribe [:map.time/is-playing? map-id])
   (re-frame/subscribe [:map.time/is-loading? map-id])])

(defn current-view-timeline-media-controls
  "State for the media-style controls to play through the timeline of hazard data."
  [[current-time available-times is-playing? is-loading?] _]
  {:is-playing?        is-playing?
   :is-loading?        (and is-loading? is-playing?)
   :can-step-forward?  (not= current-time (last available-times))
   :can-step-backward? (not= current-time (first available-times))})

(defn hazard-layers
  "List of currently available hazard layers, with metadata for display in the UI."
  [{:keys [catalogue-layers]} _]
  (filterv :hazardlayer catalogue-layers))

(defn supporting-layers
  "List of currently available supporting layers, with metadata for display in the
   UI."
  [{:keys [catalogue-layers]} _]
  (filterv (comp not :hazardlayer) catalogue-layers))

(defn filtered-hazard-layers
  "List of currently available hazard layers, filtered by the user's search in the
   layer catalogue."
  [[{:keys [filtered-layers]} hazard-layers] _]
  (filterv (set filtered-layers) hazard-layers))

(defn filtered-supporting-layers
  "List of currently available supporting layers, filtered by the user's search in
   the layer catalogue."
  [[{:keys [filtered-layers]} supporting-layers] _]
  (filterv (set filtered-layers) supporting-layers))

(defn layer-displayed-layers-lookup-signals
  "Signals function for layer-displayed-layers-lookup.

   Signals function is a necessity, in order to pass through subscription args"
  [[_ map-id]]
  [(re-frame/subscribe [:map/layers])
   (re-frame/subscribe [:map.layers/hazard-layers])
   (re-frame/subscribe [:current-view/selected-cmip-phase map-id])
   (re-frame/subscribe [:current-view/selected-model map-id])
   (re-frame/subscribe [:current-view/selected-scenario map-id])
   (re-frame/subscribe [:current-view/selected-seasonal-data map-id])
   (re-frame/subscribe [:current-view/is-historic? map-id])])

(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map.

   Overrides the `imas-seamap.map.subs/layer-displayed-layers-lookup` to insert the
   hazard layer slug from the current view into the hazard layer server URLs."
  [[{:keys [layers rich-layer-fn] :as _map-layers} hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?]]
  (nhatutils/layer-displayed-layers-lookup layers rich-layer-fn hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?))

(defn time-available-times
  "The available times for the layers, driven by the timeDimension component.

   Availability of times is filtered by the range of the currently selected time
   period in current view."
  [db [_ map-id]]
  (let [all-available-times           (utils/get-independent-map-state db map-id [:display :available-times])
        {:keys [start-year end-year]} (nhatutils/current-view-selected-time-period db map-id)] ; Alternative is registering :current-view/selected-time-period as an input signal to this sub, but then we lose access to db for getting [:display :available-times], so another sub would be necessary.
    (filter #(nhatutils/time-in-range? % start-year end-year) all-available-times)))
