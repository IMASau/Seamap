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
                                      [:current-view :selected-preset-id]
                                      [:current-view :selected-year]
                                      (utils/independent-map-state-path :map-2 [:display :current-time])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-cmip-phase-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-model-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-scenario-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-seasonal-data-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :is-historic?])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-time-period-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-preset-id])
                                      (utils/independent-map-state-path :map-2 [:current-view :selected-year])
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
                 [:current-view :selected-preset-id]
                 [:current-view :selected-year]
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
                 (utils/independent-map-state-path :map-2 [:current-view :selected-preset-id])
                 (utils/independent-map-state-path :map-2 [:current-view :selected-year])
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
  "Each projected year's layer is a 20-year average centred on that year (Ben,
   2026-09-24 walkthrough): the layer for 2060 averages 2050-2069. Historical
   layers are single years."
  20)

(defn year-window
  "First and last year averaged in the projected layer for year."
  [year]
  [(- year (/ window-years 2)) (+ year (/ window-years 2) -1)])

(def when-presets
  "The agreed reporting periods. Most users pick one of these. Each is shown by
   one layer: a projected period by its centre year's 20-year average; Recent
   by a single historical year (which year is for the data lead to confirm)."
  [{:id "recent" :name "Recent" :caption "Recent climate" :year 2005 :span "1995–2014"}
   {:id "short"  :name "Short"  :caption "Short term"     :year 2030 :span "2020–39"}
   {:id "medium" :name "Medium" :caption "Medium term"    :year 2060 :span "2050–69"}
   {:id "long"   :name "Long"   :caption "Long term"      :year 2090 :span "2080–99"}])

(def timeline-axis
  "Years spanned by the \"When\" track."
  {:start-year 1950 :end-year 2100})

(def last-historic-year
  "The historical runs end here; projections start the year after."
  2014)

(def year-range
  "Years offered on the track. Assumes historical runs 1951-2014 and
   projections to 2100 (so 20-year averages centred up to 2090). Check against
   THREDDS."
  [1951 2090])

(def historic-gap
  "Years with no layer: after the historical runs end, and before the first
   projected 20-year average (centred 2025, so averaging 2015-2034). Not
   offered unless the data lead rules windows may mix the two."
  [(inc last-historic-year) (+ last-historic-year (/ window-years 2))])

(defn snap-year
  "Nearest centre year on offer: inside year-range and outside historic-gap."
  [year]
  (let [[first-year last-year] year-range
        [gap-start gap-end]    historic-gap
        year                   (-> year (max first-year) (min last-year))]
    (cond
      (< year gap-start)                      year
      (> year gap-end)                        year
      (< (- year gap-start) (- gap-end year)) (dec gap-start)
      :else                                   (inc gap-end))))

(defn historic-year?
  "Is the layer for year a single historical year (rather than a projected
   20-year average)?"
  [year]
  (<= year last-historic-year))

(defn window-preset
  "The preset centred on year, if any."
  [year]
  (first-where #(= (:year %) year) when-presets))

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

(declare current-view-year)

(defn current-view-is-historic?
  "Indicates whether the current view is for historic data: true for the one
   historical window."
  ([db] (current-view-is-historic? db nil))
  ([db map-id]
   (historic-year? (current-view-year db map-id))))

(defn- legacy-preset
  "The preset an older saved state was on (it stored a window or a time period
   and historic flag, not a year). Defaults to Medium."
  [db map-id]
  (let [get-state #(utils/get-independent-map-state db map-id [:current-view %])
        period-id (get-state :selected-time-period-id)]
    (or (some-> (get-state :window-start-year) (+ (/ window-years 2)) window-preset)
        (when (get-state :is-historic?) (first-where #(= (:id %) "recent") when-presets))
        (first-where #(= (:id %) period-id) when-presets)
        (first-where #(= (:id %) "medium") when-presets))))

(defn current-view-preset
  "The period (Recent, Short, Medium, Long) the user chose, or nil once they've
   moved to a year of their own."
  ([db] (current-view-preset db nil))
  ([db map-id]
   (if (utils/get-independent-map-state db map-id [:current-view :selected-year])
     (first-where #(= (:id %) (utils/get-independent-map-state db map-id [:current-view :selected-preset-id])) when-presets)
     (legacy-preset db map-id))))

(defn current-view-year
  "The year the user chose: directly, or as the start of a period."
  ([db] (current-view-year db nil))
  ([db map-id]
   (or (utils/get-independent-map-state db map-id [:current-view :selected-year])
       (:year (legacy-preset db map-id)))))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn current-view-selected-time-period
  "Time period to analyze the hazard data: the one layer for the chosen centre
   year, which averages the 20 years around it."
  ([db] (current-view-selected-time-period db nil))
  ([db map-id]
   (let [year   (current-view-year db map-id)
         preset (current-view-preset db map-id)]
     {:id         (or (:id preset) "year")
      :name       (or (:caption preset) (str "20 years centred on " year))
      :start-year year
      :end-year   year})))

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

   Availability of times is filtered to the selected window or year. If the
   dataset has none there, use the time nearest the start of the selection, so
   the map still shows something and the UI can say which year that is."
  [db map-id]
  (let [all-available-times           (utils/get-independent-map-state db map-id [:display :available-times])
        {:keys [start-year end-year]} (current-view-selected-time-period db map-id)
        in-range                      (filter #(time-in-range? % start-year end-year) all-available-times)
        year-of                       #(-> % js/Date. .getFullYear)]
    (cond
      (seq in-range)            in-range
      (seq all-available-times) [(apply min-key #(js/Math.abs (- (year-of %) start-year)) all-available-times)]
      :else                     [])))
