;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.subs
  (:require [clojure.string :as string]
            [imas-seamap.utils :refer [first-where]]
            [imas-seamap.natural-hazards-atlas.utils :as nhatutils]))

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

(defn current-view-selected-model
  "Scientific model to analyze the hazard data"
  [db _]
  (nhatutils/current-view-selected-model db))

(defn current-view-selected-scenario
  "Scenario to analyze the hazard data"
  [db _]
  (nhatutils/current-view-selected-scenario db))

(defn current-view-selected-seasonal-data
  "Seasonal data to analyze the hazard data"
  [db _]
  (nhatutils/current-view-selected-seasonal-data db))

(defn current-view-time-periods
  "List of time periods available to analyze the hazard data."
  [_db _]
  nhatutils/time-periods)

(defn current-view-selected-time-period
  "Time period to analyze the hazard data"
  [db _]
  (nhatutils/current-view-selected-time-period db))

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

(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map.

   Overrides the `imas-seamap.map.subs/layer-displayed-layers-lookup` to replace
   hazard layer server URLs with whatever scientific model, scenario, and season
   is selected.

   The format for hazard layer URLs is
   `<layer_name>_<model>_<scenario>_<season>.nc`, i.e.
   `variable_heatwave_amplitude_scenario_historical_format.nc` becomes
   `variable_heatwave_amplitude_scenario_historical_format_cmip6_ssp1_summer.nc`"
  [[{:keys [layers rich-layer-fn] :as _map-layers} hazard-layers selected-model selected-scenario selected-seasonal-data] _]
  (nhatutils/layer-displayed-layers-lookup layers rich-layer-fn hazard-layers selected-model selected-scenario selected-seasonal-data))

;; Proof-of-concept for having separate information in map B
(defn layer-displayed-layers-lookup-map-b
  [[{:keys [layers rich-layer-fn] :as _map-layers} hazard-layers selected-model selected-scenario selected-seasonal-data] _]
  (nhatutils/layer-displayed-layers-lookup-map-b layers rich-layer-fn hazard-layers selected-model selected-scenario selected-seasonal-data))

(defn time-available-times
  "The available times for the layers, driven by the timeDimension component.

   Availability of times is filtered by the range of the currently selected time
   period in current view."
  [db _]
  (let [all-available-times           (get-in db [:display :available-times])
        {:keys [start-year end-year]} (nhatutils/current-view-selected-time-period db)] ; Alternative is registering :current-view/selected-time-period as an input signal to this sub, but then we lose access to db for getting [:display :available-times], so another sub would be necessary.
    (filter #(nhatutils/time-in-range? % start-year end-year) all-available-times)))
