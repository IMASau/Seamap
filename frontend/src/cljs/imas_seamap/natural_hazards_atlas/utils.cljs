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
                                      [:display :side-by-side :active?]
                                      [:display :side-by-side :split-ratio]
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
                                      [:current-view :window-start-year]
                                      (utils/independent-map-state-path :map-2 [:display :current-time])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-cmip-phase-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-model-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-scenario-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-seasonal-data-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :is-historic?])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-time-period-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :window-start-year])
                                      :autosave?])
                       (assoc :map pruned-map)
                       (assoc :story-maps pruned-story-maps)
                       #_(update-in [:display :catalogue :expanded] #(into {} (filter second %))))
        legends    (->> db :layer-state :legend-shown (map :id))
        opacities  (->> db :layer-state :opacity (reduce (fn [acc [k v]] (if (= v 100) acc (conj acc [(:id k) v]))) {}))
        
        db (assoc-in db [:display :load-time] (get-in db [:display :current-time]))
        db (utils/assoc-independent-map-state db :map-2 [:display :load-time] (utils/get-independent-map-state db :map-2 [:display :current-time]))
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
                 [:display :load-time]
                 [:display :side-by-side :active?]
                 [:display :side-by-side :split-ratio]
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
                 [:current-view :window-start-year]
                 [:current-view :is-historic?]
                 (utils/independent-map-state-path :map-2 [:display :current-time])
                 (utils/independent-map-state-path :map-2 [:display :load-time])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-cmip-phase-id])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-model-id])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-scenario-id])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-seasonal-data-id])
                 (utils/independent-map-state-path :map-2 [:current-view :is-historic?])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-time-period-id])
                 (utils/independent-map-state-path :map-2 [:current-view :window-start-year])
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
    [:current-view :cmip-phases]
    [:current-view :models]
    [:current-view :scenarios]
    [:current-view :seasonal-datas]
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

(def window-years
  "Every view averages a 20-year window."
  20)

(def recent-window-start
  "The one historical window. The historical runs end in 2014."
  1995)

(def projected-window-starts
  "Range of start years for projected windows: from the first projected year
   until the window reaches the end of the century."
  [2015 2080])

(def when-presets
  "Named windows. Most users pick one of these rather than dragging."
  [{:id "recent" :name "Recent" :caption "Recent climate" :start-year 1995}
   {:id "short"  :name "Short"  :caption "Short term"     :start-year 2020}
   {:id "medium" :name "Medium" :caption "Medium term"    :start-year 2050}
   {:id "long"   :name "Long"   :caption "Long term"      :start-year 2080}])

(def timeline-axis
  "Years spanned by the \"When\" track."
  {:start-year 1990 :end-year 2100})

(defn window-end-year [start-year] (+ start-year window-years -1))

(defn historic-window? [start-year] (= start-year recent-window-start))

(defn snap-window-start
  "Nearest allowed window start for a dragged-to year. A window is wholly
   historical or wholly projected, never straddling the join between them."
  [year]
  (let [[first-projected last-projected] projected-window-starts]
    (if (< year (/ (+ recent-window-start first-projected) 2))
      recent-window-start
      (-> year (max first-projected) (min last-projected)))))

(defn window-preset
  "The preset whose window starts at start-year, if any."
  [start-year]
  (first-where #(= (:start-year %) start-year) when-presets))

(def scenario-labels
  "Plain-English names for the emissions scenarios, keyed by scenario name. The
   SSP code is shown underneath for anyone who needs it."
  {"ssp126" {:label "Lower"  :code "SSP1-2.6" :description "Emissions fall steeply from today."}
   "ssp370" {:label "Higher" :code "SSP3-7.0" :description "Emissions keep rising through the century."}})

(def default-scenario-name
  "Scenario shown until the user picks one (agreed 24 Sep: higher emissions)."
  "ssp370")

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-cmip-phase
  "CMIP (Coupled Model Intercomparison Project) phase that organizes models and
   scenarios for analyzing hazard data."
  ([db] (current-view-selected-cmip-phase db nil))
  ([db map-id]
   (let [cmip-phases            (get-in db [:current-view :cmip-phases])
         selected-cmip-phase-id (utils/get-independent-map-state db map-id [:current-view :selected-cmip-phase-id])
         selected-cmip-phase    (first-where #(= (:id %) selected-cmip-phase-id) cmip-phases)]
     (if (and (seq cmip-phases) selected-cmip-phase-id)
       (do
         (assert selected-cmip-phase (str "Selected CMIP phase id " selected-cmip-phase-id " not found in CMIP phases list"))
         selected-cmip-phase)
       (first cmip-phases)))))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-model
  "Scientific model to analyze the hazard data."
  ([db] (current-view-selected-model db nil))
  ([db map-id]
   (let [models              (get-in db [:current-view :models])
         selected-model-id   (utils/get-independent-map-state db map-id [:current-view :selected-model-id])
         selected-model      (first-where #(= (:id %) selected-model-id) models)]
     (if (and (seq models) selected-model-id)
       (do
         (assert selected-model (str "Selected model id " selected-model-id " not found in models list"))
         selected-model)
       (first models)))))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-scenario
  "Scenario to analyze the hazard data."
  ([db] (current-view-selected-scenario db nil))
  ([db map-id]
   (let [scenarios               (get-in db [:current-view :scenarios])
         selected-scenario-id    (utils/get-independent-map-state db map-id [:current-view :selected-scenario-id])
         selected-scenario       (first-where #(= (:id %) selected-scenario-id) scenarios)]
     (if (and (seq scenarios) selected-scenario-id)
       (do
         (assert selected-scenario (str "Selected scenario id " selected-scenario-id " not found in scenarios list"))
         selected-scenario)
       (or (first-where #(= (:name %) default-scenario-name) scenarios)
           (first scenarios))))))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-seasonal-data
  "Seasonal data to analyze the hazard data"
  ([db] (current-view-selected-seasonal-data db nil))
  ([db map-id]
   (let [seasonal-datas               (get-in db [:current-view :seasonal-datas])
         selected-seasonal-data-id    (utils/get-independent-map-state db map-id [:current-view :selected-seasonal-data-id])
         selected-seasonal-data       (first-where #(= (:id %) selected-seasonal-data-id) seasonal-datas)]
     (if (and (seq seasonal-datas) selected-seasonal-data-id)
       (do
         (assert selected-seasonal-data (str "Selected seasonal data id " selected-seasonal-data-id " not found in seasonal datas list"))
         selected-seasonal-data)
       (first seasonal-datas)))))

(declare current-view-window-start)

(defn current-view-is-historic?
  "Indicates whether the current view is for historic data: true for the one
   historical window."
  ([db] (current-view-is-historic? db nil))
  ([db map-id]
   (historic-window? (current-view-window-start db map-id))))

(defn current-view-window-start
  "Start year of the selected 20-year window. Older saved states only have a
   time period and historic flag, so derive it from those."
  ([db] (current-view-window-start db nil))
  ([db map-id]
   (or (utils/get-independent-map-state db map-id [:current-view :window-start-year])
       (if (utils/get-independent-map-state db map-id [:current-view :is-historic?])
         recent-window-start
         (let [period-id (utils/get-independent-map-state db map-id [:current-view :selected-time-period-id])]
           (or (:start-year (first-where #(= (:id %) period-id) when-presets))
               (:start-year (first-where #(= (:id %) "medium") when-presets))))))))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-time-period
  "Time period to analyze the hazard data: the selected 20-year window."
  ([db] (current-view-selected-time-period db nil))
  ([db map-id]
   (let [start-year (current-view-window-start db map-id)
         preset     (window-preset start-year)]
     {:id         (or (:id preset) "custom")
      :name       (or (:caption preset) "Custom window")
      :start-year start-year
      :end-year   (window-end-year start-year)})))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn hazard-layers
  "List of currently available hazard layers, with metadata for display in the UI."
  [catalogue-layers]
  (filterv :hazardlayer catalogue-layers))


(defn hazard-layer-dataset
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
                    (assoc :server_url (:server_url hazard-layer-dataset))
                    (assoc :layer_name (:layer_name hazard-layer-dataset))
                    (assoc :style (str "default-scalar/" (get-in displayed-layer [:hazardlayer :color_palette])))
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
  (let [year (-> time js/Date. .getFullYear)]
    (and (>= year (or start-year ##-Inf)) (<= year (or end-year ##Inf)))))

(defn time-available-times
  "The available times for the layers, driven by the timeDimension component.

   Availability of times is filtered to the selected 20-year window. If the
   dataset has no years in the window, fall back to all of them so the map
   still shows something."
  [db map-id]
  (let [all-available-times           (utils/get-independent-map-state db map-id [:display :available-times])
        {:keys [start-year end-year]} (current-view-selected-time-period db map-id)
        in-window                     (filter #(time-in-range? % start-year end-year) all-available-times)]
    (if (seq in-window) in-window all-available-times)))
