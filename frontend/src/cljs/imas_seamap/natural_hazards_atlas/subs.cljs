;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.subs
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]
   [imas-seamap.map.subs :as msubs]
   [imas-seamap.natural-hazards-atlas.utils :as nhatutils]
   [imas-seamap.utils :as utils :refer [first-where]]))

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

(defn current-view-preset
  "The period the user chose, or nil once they've moved to a year of their own."
  [db [_ map-id]]
  (nhatutils/current-view-preset db map-id))

(defn current-view-year
  "The year the user chose: directly, or as the start of a period."
  [db [_ map-id]]
  (nhatutils/current-view-year db map-id))

(defn current-view-playing?
  "Is Play moving the year along the track?"
  [db [_ map-id]]
  (boolean (utils/get-independent-map-state db map-id [:display :window-playing?])))

(defn current-view-caption-signals
  "Signals function for current-view-caption.

   Signals function is a necessity, in order to pass through subscription args"
  [[_ map-id]]
  [(re-frame/subscribe [:map.layers/active-hazard-layer])
   (re-frame/subscribe [:current-view/preset map-id])
   (re-frame/subscribe [:current-view/year map-id])
   (re-frame/subscribe [:map.time/current-time map-id])
   (re-frame/subscribe [:current-view/is-historic? map-id])
   (re-frame/subscribe [:current-view/selected-scenario map-id])
   (re-frame/subscribe [:current-view/selected-seasonal-data map-id])
   (re-frame/subscribe [:current-view/selected-model map-id])])

(defn current-view-caption
  "The parts of a plain-English description of what the map is showing. A
   projected layer is a 20-year average centred on its year; a historical
   layer is a single year."
  [[hazard-layer preset chosen-year current-time historic? scenario season model] _]
  (let [year (when current-time (.getFullYear (js/Date. current-time)))
        year (when (and year (= (nhatutils/historic-year? year) (nhatutils/historic-year? chosen-year)))
               year)] ; the other dataset's layer is still showing; don't describe it as the choice
    {:layer     (:name hazard-layer)
     :when      (let [shown (when year
                                  (if (nhatutils/historic-year? year)
                                    (str "year " year)
                                    (let [[first-year last-year] (nhatutils/year-window year)]
                                      (str "20-year average " first-year "–" last-year))))]
                  (some-> (string/join ", " (remove nil? [(:caption preset) shown]))
                          not-empty
                          (#(str (string/upper-case (subs % 0 1)) (subs % 1)))))
     :emissions (when-not historic?
                  (let [{:keys [label]} (get nhatutils/scenario-labels (:name scenario))]
                    (if label (str (string/lower-case label) " emissions") (:display_name scenario))))
     :season    (when-let [label (some-> season nhatutils/season-label)] ; "Summer (Dec–Feb)" -> "summer (Dec–Feb)"
                  (str (string/lower-case (subs label 0 1)) (subs label 1)))
     :model     (let [model-name (:display_name model)] ; "Ensemble median" reads as words; model codes don't
                  (if (some-> model-name (string/starts-with? "Ensemble")) (string/lower-case model-name) model-name))}))

(defn current-view-timeline-media-controls-signals
  "Signals function for current-view-timeline-media-controls.

   Signals function is a necessity, in order to pass through subscription args"
  [[_ map-id]]
  [(re-frame/subscribe [:map.time/current-time map-id])
   (re-frame/subscribe [:map.time/available-times map-id])
   (re-frame/subscribe [:map.time/is-playing? map-id])
   (re-frame/subscribe [:map.time/is-loading? map-id])
   (re-frame/subscribe [:ui.side-by-side/active?])])

(defn current-view-timeline-media-controls
  "State for the media-style controls to play through the timeline of hazard data."
  [[current-time available-times is-playing? is-loading? side-by-side-active?] _]
  {:is-playing?        is-playing?
   :is-loading?        is-loading?
   :is-disabled?       side-by-side-active?
   :can-step-forward?  (not= current-time (last available-times))
   :can-step-backward? (not= current-time (first available-times))})

(defn hazard-layers
  "List of currently available hazard layers, with metadata for display in the UI."
  [{:keys [catalogue-layers]} _]
  (filterv :hazardlayer catalogue-layers))

(defn hazard-layers-color-scale-range
  "Merged scale range for hazard layers across map 1 and 2."
  [[hazard-layers
    cmip-phase-1 model-1 scenario-1 seasonal-data-1 is-historic?-1
    cmip-phase-2 model-2 scenario-2 seasonal-data-2 is-historic?-2]
   [_ layer]]
  (letfn [(get-hazard-layer-min-max
           [layer]
           (let [{min-1 :color_scale_range_min max-1 :color_scale_range_max}
                 (nhatutils/hazard-layer-dataset layer cmip-phase-1 model-1 scenario-1 seasonal-data-1 is-historic?-1)
                 {min-2 :color_scale_range_min max-2 :color_scale_range_max}
                 (nhatutils/hazard-layer-dataset layer cmip-phase-2 model-2 scenario-2 seasonal-data-2 is-historic?-2)]
             {:color-scale-range-min (min min-1 min-2)
              :color-scale-range-max (max max-1 max-2)}))]
    (if layer
      (get-hazard-layer-min-max layer)
      (reduce
       (fn [m layer]
         (assoc m layer (get-hazard-layer-min-max layer)))
       {} hazard-layers))))

(defn hazard-layers-active-hazard-layer
  "There should only be one hazard layer active at a time, per spec. This is handy
   for retrieving that single layer for showing its legend on the map."
  [[{:keys [active-layers]} hazard-layers] _]
  (->> hazard-layers (filter (set active-layers)) first))

(defn hazard-layers-units
  "Units for hazard layers"
  [[hazard-layers] [_ layer]]
  (letfn [(get-hazard-layer-units [layer] (get-in layer [:hazardlayer :human_readable_units]))]
    (if layer
      (get-hazard-layer-units layer)
      (reduce
       (fn [m layer]
         (assoc m layer (get-hazard-layer-units layer)))
       {} hazard-layers))))

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
  [[{:keys [layers rich-layer-fn] :as _map-layers} hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?] _]
  (nhatutils/layer-displayed-layers-lookup layers rich-layer-fn hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?))

(defn time-available-times
  "The available times for the layers, driven by the timeDimension component.

   Availability of times is filtered by the range of the currently selected time
   period in current view."
  [db [_ map-id]]
  (nhatutils/time-available-times db map-id))

(defn layer-legend [db [_ {:keys [id] :as layer}]]
  (let [layer-legend (msubs/layer-legend db [_ layer])]
    (if (:hazardlayer layer)
      (assoc layer-legend :type :color-scale-bar)
      layer-legend)))
