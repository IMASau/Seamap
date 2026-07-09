;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.utils
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [goog.crypt.base64 :as b64]
            [cognitect.transit :as t]
            [imas-seamap.utils :as utils :refer [select-keys* first-where]]
            [imas-seamap.map.utils :as map-utils]))

(defn encode-state
  "Returns a string suitable for storing in the URL's hash"
  [{:keys [story-maps] map-state :map :as db}]
  (let [pruned-map (-> (select-keys*
                        map-state
                        [[:rich-layers :states]
                         :center
                         :zoom
                         :active-layers
                         :active-base-layer
                         :viewport-only?
                         :bounds])
                       (rename-keys {:active-layers :active :active-base-layer :active-base})
                       (update :active (partial map :id))
                       (update :active-base :id))
        pruned-story-maps (-> (select-keys story-maps [:featured-map])
                              (update :featured-map :id))
        db         (-> db
                       (select-keys* [[:display :sidebar :selected]
                                      [:display :catalogue :main]
                                      [:display :left-drawer]
                                      [:display :left-drawer-tab]
                                      [:display :right-sidebars]
                                      [:display :split-layer-range-value]
                                      [:display :split-layer-container-x]
                                      [:display :current-time]
                                      [:filters :layers]
                                      :layer-state
                                      [:transect :show?]
                                      [:transect :query]
                                      [:feature :location]
                                      [:feature :leaflet-props]
                                      [:dynamic-pills :states]
                                      [:current-view :selected-cmip-phase-id]
                                      [:current-view :selected-model-id]
                                      [:current-view :selected-scenario-id]
                                      [:current-view :selected-seasonal-data-id]
                                      [:current-view :is-historic?]
                                      [:current-view :selected-time-period-id]
                                      :autosave?])
                       (assoc :map pruned-map)
                       (assoc :story-maps pruned-story-maps)
                       #_(update-in [:display :catalogue :expanded] #(into {} (filter second %))))
        legends    (->> db :layer-state :legend-shown (map :id))
        opacities  (->> db :layer-state :opacity (reduce (fn [acc [k v]] (if (= v 100) acc (conj acc [(:id k) v]))) {}))
        db*        (-> db
                       (dissoc :layer-state)
                       (assoc :legend-ids legends)
                       (assoc :opacity-ids opacities))]
    (b64/encodeString (t/write (t/writer :json) db*))))

(defn- filter-state
  "Given a state map, presumably from the hashed state, filter down to
  only expected/allowed paths to prevent injection attacks."
  [state]
  (select-keys* state
                [[:display :sidebar :selected]
                 [:display :catalogue :main]
                 [:display :left-drawer]
                 [:display :left-drawer-tab]
                 [:display :right-sidebars]
                 [:display :split-layer-range-value]
                 [:display :split-layer-container-x]
                 [:display :current-time]
                 [:filters :layers]
                 [:story-maps :featured-map]
                 [:transect :show?]
                 [:transect :query]
                 [:map :active]
                 [:map :active-base]
                 [:map :center]
                 [:map :zoom]
                 [:map :bounds]
                 [:map :viewport-only?]
                 [:map :rich-layers :states]
                 [:feature :location]
                 [:feature :leaflet-props]
                 [:dynamic-pills :states]
                 [:current-view :selected-cmip-phase-id]
                 [:current-view :selected-model-id]
                 [:current-view :selected-scenario-id]
                 [:current-view :selected-seasonal-data-id]
                 [:current-view :selected-time-period-id]
                 [:current-view :is-historic?]
                 :legend-ids
                 :opacity-ids
                 :autosave?
                 :config]))

(defn parse-state [hash-str]
  (try
    (let [decoded   (b64/decodeString hash-str)
          reader    (t/reader :json)]
      (->> decoded
           (t/read reader)
           filter-state))
    (catch js/Object e {})))

(defn ajax-loaded-info
  "Returns db of all the info retrieved via ajax"
  [db]
  (select-keys*
   db
   [[:map :layers]
    [:map :base-layers]
    [:map :base-layer-groups]
    [:map :grouped-base-layers]
    [:map :organisations]
    [:map :categories]
    [:map :keyed-layers]
    [:map :leaflet-map]
    [:map :legends]
    [:map :rich-layer-children]
    [:map :rich-layers :rich-layers]
    [:map :rich-layers :async-datas]
    [:map :rich-layers :layer-lookup]
    [:story-maps :featured-maps]
    [:dynamic-pills :dynamic-pills]
    [:dynamic-pills :async-datas]
    :habitat-colours
    :habitat-titles
    :sorting
    :config]))

(def time-periods
  [{:id "all"      :name "All"      :start-year nil  :end-year nil}
   {:id "short"    :name "Short"    :start-year 2020 :end-year 2039}
   {:id "medium"   :name "Medium"   :start-year 2050 :end-year 2069}
   {:id "long"     :name "Long"     :start-year 2080 :end-year 2099}])

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-cmip-phase
  "CMIP (Coupled Model Intercomparison Project) phase that organizes models and
   scenarios for analyzing hazard data."
  ([db] (current-view-selected-cmip-phase db nil))
  ([db map-id]
   (let [cmip-phases            (get-in db [:current-view :cmip-phases])
         selected-cmip-phase-id (utils/get-independent-map-state db map-id [:current-view :selected-cmip-phase-id])
         selected-cmip-phase    (first-where #(= (:id %) selected-cmip-phase-id) cmip-phases)]
     (when (and (seq cmip-phases) selected-cmip-phase-id)
       (assert selected-cmip-phase (str "Selected CMIP phase id " selected-cmip-phase-id " not found in CMIP phases list")))
     selected-cmip-phase)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-model
  "Scientific model to analyze the hazard data.

   If the value selected by the user isn't one of the models found in the current
   CMIP phase, then default to the first available model."
  ([db] (current-view-selected-model db nil))
  ([db map-id]
   (let [models              (get-in db [:current-view :models])
         selected-model-id   (utils/get-independent-map-state db map-id [:current-view :selected-model-id])
         selected-model      (first-where #(= (:id %) selected-model-id) models)]
     (when (and (seq models) selected-model-id)
       (assert selected-model (str "Selected model id " selected-model-id " not found in models list")))
     selected-model)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-scenario
  "Scenario to analyze the hazard data.

   If the value selected by the user isn't one of the scenarios found in the
   current scientific model, then default to the first available scenario."
  ([db] (current-view-selected-scenario db nil))
  ([db map-id]
   (let [scenarios               (get-in db [:current-view :scenarios])
         selected-scenario-id    (utils/get-independent-map-state db map-id [:current-view :selected-scenario-id])
         selected-scenario       (first-where #(= (:id %) selected-scenario-id) scenarios)]
     (when (and (seq scenarios) selected-scenario-id)
       (assert selected-scenario (str "Selected scenario id " selected-scenario-id " not found in scenarios list")))
     selected-scenario)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-seasonal-data
  "Seasonal data to analyze the hazard data"
  ([db] (current-view-selected-seasonal-data db nil))
  ([db map-id]
   (let [seasonal-datas               (get-in db [:current-view :seasonal-datas])
         selected-seasonal-data-id    (utils/get-independent-map-state db map-id [:current-view :selected-seasonal-data-id])
         selected-seasonal-data       (first-where #(= (:id %) selected-seasonal-data-id) seasonal-datas)]
     (when (and (seq seasonal-datas) selected-seasonal-data-id)
       (assert selected-seasonal-data (str "Selected seasonal data id " selected-seasonal-data-id " not found in seasonal datas list")))
     selected-seasonal-data)))

(defn current-view-is-historic?
  "Indicates whether the current view is for historic data."
  ([db] (current-view-is-historic? db nil))
  ([db map-id]
   (utils/get-independent-map-state db map-id [:current-view :is-historic?])))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-time-period
  "Time period to analyze the hazard data"
  ([db] (current-view-is-historic? db nil))
  ([db map-id]
   (let [selected-time-period-id (get-in db [:current-view :selected-time-period-id])
         is-historic?            (utils/get-independent-map-state db map-id [:current-view :is-historic?])
         selected-time-period-id (if is-historic? "all" selected-time-period-id)
         selected-time-period    (first-where #(= (:id %) selected-time-period-id) time-periods)]
     (assert selected-time-period (str "Selected time period id " selected-time-period-id " not found in time periods list"))
     selected-time-period)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn hazard-layers
  "List of currently available hazard layers, with metadata for display in the UI."
  [catalogue-layers]
  (filterv :hazardlayer catalogue-layers))


(defn- hazard-layer-dataset
  "Get the hazard layer dataset for the given hazard layer, CMIP phase, model, scenario, and seasonal data."
  [hazard-layer selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?]
  (let [datasets (get-in hazard-layer [:hazardlayer :datasets])]
    (first-where
     (fn [dataset]
       (and (= (:cmip_phase dataset) (:name selected-cmip-phase))
            (= (:scientific_model dataset) (:name selected-model))
            (= (:scenario dataset) (when-not is-historic? (:name selected-scenario))) ; historical data exclusive with scenario
            (= (:season dataset) (:name selected-seasonal-data))
            (= (:is_historical dataset) is-historic?)))
     datasets)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map.

   Overrides the `imas-seamap.map.subs/layer-displayed-layers-lookup` to grab the
   server URL and layer_name from whatever hazard layer dataset is selected by the
   current view."
  [layers rich-layer-fn hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?]
  (let [hazard-layers (set hazard-layers)]
    (->>
     (map-utils/layer-displayed-layers-lookup layers rich-layer-fn)
     (reduce-kv
      (fn [m layer displayed-layer]
        (if (hazard-layers displayed-layer)
          ; If the layer is a hazard layer, then override the server URL and layer_name with the values from the selected hazard layer dataset.
          (let [hazard-layer-dataset (hazard-layer-dataset displayed-layer selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?)
                displayed-layer
                (-> displayed-layer
                    (assoc-in [:server_url] (:server_url hazard-layer-dataset))
                    (assoc-in [:layer_name] (:layer_name hazard-layer-dataset))
                    (assoc-in [:hazardlayer :color_scale_range_min] (:color_scale_range_min hazard-layer-dataset))
                    (assoc-in [:hazardlayer :color_scale_range_max] (:color_scale_range_max hazard-layer-dataset)))]
            (assoc m layer displayed-layer))
          (assoc m layer displayed-layer)))
      {}))))

;; Proof-of-concept for having separate information in map B
(defn layer-displayed-layers-lookup-map-b
  [layers rich-layer-fn hazard-layers selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?]
  (let [hazard-layers (set hazard-layers)]
    (->>
     (map-utils/layer-displayed-layers-lookup layers rich-layer-fn)
     (reduce-kv
      (fn [m layer displayed-layer]
        (if (hazard-layers displayed-layer)
            ; If the layer is a hazard layer, then override the server URL and layer_name with the values from the selected hazard layer dataset.
          (let [hazard-layer-dataset (hazard-layer-dataset displayed-layer selected-cmip-phase selected-model selected-scenario selected-seasonal-data is-historic?)
                displayed-layer
                (-> displayed-layer
                    (assoc-in [:server_url] (:server_url hazard-layer-dataset))
                    (assoc-in [:layer_name] (:layer_name hazard-layer-dataset))
                    (assoc-in [:hazardlayer :color_scale_range_min] (:color_scale_range_min hazard-layer-dataset))
                    (assoc-in [:hazardlayer :color_scale_range_max] (:color_scale_range_max hazard-layer-dataset)))]
            (assoc m layer displayed-layer))
          (assoc m layer displayed-layer)))
      {}))))

; TODO: Refactor so that `db` isn't a necessary argument
(defn displayed-layers-under-point
  "From the list of visible layers on the map, get the layers displayed on the map
   under the current point.

   The current point can matter for things like split view layers."
  [visible-layers layer-displayed-layers-lookup point db]
  (map
   #(get layer-displayed-layers-lookup %)
   (map-utils/displayed-layers-under-point visible-layers point db)))

(defn time-in-range?
  "Is the given time (in ms) in the given range of years"
  [time start-year end-year]
  (let [start-ms (if start-year (js/Date.UTC start-year) ##-Inf)
        end-ms   (if end-year (js/Date.UTC end-year) ##Inf)]
    (and (>= time start-ms) (<= time end-ms))))
