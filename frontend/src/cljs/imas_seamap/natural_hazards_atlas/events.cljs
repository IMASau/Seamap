;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.events
  (:require [ajax.core :as ajax]
            [imas-seamap.natural-hazards-atlas.db :as db]
            [imas-seamap.utils :refer [copy-text merge-in ids->layers first-where]]
            [imas-seamap.map.utils :as mutils :refer [init-layer-legend-status init-layer-opacities rich-layer->displayed-layer]]
            [imas-seamap.natural-hazards-atlas.utils :as nhatutils]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- boot-flow
  "Like imas-seamap.events/boot-flow, but we don't dispatch the events that load
   region reports and state of knowledge data since those aren't present in the
   Natural Hazards Atlas."
  []
  {:first-dispatch [:ui/show-loading "Loading Natural Hazards Atlas..."]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/join-keyed-layers
                                  :map/join-rich-layers
                                  :current-view/update-cmip-phases
                                  :current-view/update-models
                                  :current-view/update-scenarios
                                  :current-view/update-seasonal-datas]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch-n [[:welcome-layer/open]
                  [:display.outage-message/open true]]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-hash-state
    "Like imas-seamap.events/boot-flow-hash-state, but we don't dispatch the events
     that load region reports and state of knowledge data since those aren't present
     in the Natural Hazards Atlas."
  [hash-code]
  {:first-dispatch [:ui/show-loading "Loading Natural Hazards Atlas..."]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:load-hash-state hash-code]}
    {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/join-keyed-layers
                                  :map/join-rich-layers
                                  :current-view/update-cmip-phases
                                  :current-view/update-models
                                  :current-view/update-scenarios
                                  :current-view/update-seasonal-datas]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch-n [[:welcome-layer/open]
                  [:display.outage-message/open true]]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-save-state
  "Like imas-seamap.events/boot-flow-save-state, but we don't dispatch the events
   that load region reports and state of knowledge data since those aren't present
   in the Natural Hazards Atlas."
  [shortcode]
  {:first-dispatch [:ui/show-loading "Loading Natural Hazards Atlas..."]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:get-save-state shortcode [:load-hash-state]]}
    {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/join-keyed-layers
                                  :map/join-rich-layers
                                  :current-view/update-cmip-phases
                                  :current-view/update-models
                                  :current-view/update-scenarios
                                  :current-view/update-seasonal-datas]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch-n [[:welcome-layer/open]
                  [:display.outage-message/open true]]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn construct-urls [db _]
  (let [{:keys
         [site-configuration
          layer
          base-layer
          base-layer-group
          organisation
          classification
          region-stats
          descriptor
          save-state
          category
          keyed-layers
          rich-layers
          region-reports
          dynamic-pills
          layer-legend
          cql-filter-values
          dynamic-pill-region-control-values
          layer-previews
          story-maps
          cmip-phases
          scientific-models
          scenarios
          seasons]}
        (get-in db [:config :url-paths])
        {:keys [api-url-base media-url-base wordpress-url-base _img-url-base]} (get-in db [:config :url-base])]
    (assoc-in
     db [:config :urls]
     {:site-configuration-url      (str api-url-base site-configuration)
      :layer-url                   (str api-url-base layer)
      :base-layer-url              (str api-url-base base-layer)
      :base-layer-group-url        (str api-url-base base-layer-group)
      :organisation-url            (str api-url-base organisation)
      :classification-url          (str api-url-base classification)
      :region-stats-url            (str api-url-base region-stats)
      :descriptor-url              (str api-url-base descriptor)
      :save-state-url              (str api-url-base save-state)
      :category-url                (str api-url-base category)
      :keyed-layers-url            (str api-url-base keyed-layers)
      :rich-layers-url             (str api-url-base rich-layers)
      :region-reports-url          (str api-url-base region-reports)
      :dynamic-pills-url           (str api-url-base dynamic-pills)
      :layer-legend-url            (str api-url-base layer-legend)
      :cql-filter-values-url       (str api-url-base cql-filter-values)
      :dynamic-pill-region-control-values-url (str api-url-base dynamic-pill-region-control-values)
      :layer-previews-url          (str media-url-base layer-previews)
      :story-maps-url              (str wordpress-url-base story-maps)
      :cmip-phases-url             (str api-url-base cmip-phases)
      :scientific-models-url       (str api-url-base scientific-models)
      :scenarios-url               (str api-url-base scenarios)
      :seasons-url                 (str api-url-base seasons)})))

(defn boot
  "Identical to imas-seamap.events/boot, just triggers the private versions of the
   boot flow functions that don't load region reports and state of knowledge data."
  [{:keys [save-code hash-code] {:keys [seamap-app-state]} :local-storage/get} [_ api-url-base media-url-base wordpress-url-base img-url-base]]
  {:db         (assoc-in
                db/default-db [:config :url-base]
                {:api-url-base       api-url-base
                 :media-url-base     media-url-base
                 :wordpress-url-base wordpress-url-base
                 :img-url-base       img-url-base})
   :async-flow (cond ; Choose async boot flow based on what information we have for the DB:
                 (seq save-code)    (boot-flow-save-state save-code)    ; A shortform save-code that can be used to query for a hash-code
                 (seq hash-code)    (boot-flow-hash-state hash-code)    ; A hash-code that can be decoded into th DB's initial state
                 (seq seamap-app-state) (boot-flow-hash-state seamap-app-state) ; Same as hash-code, except that we use the one stored in local storage
                 :else              (boot-flow))})                      ; No information, so we start with an empty DB

(defn merge-state
  "Takes a hash-code and merges it into the current application state.
   
   Like imas-seamap.events/merge-state, but uses the NHAT version of parse-state,
   which doesn't include state of knowledge, but does include the current view
   (selections of model, scenario, seasonal data, and time period)."
  [{:keys [db]} [_ hash-code]]
  (let [parsed-state (-> (nhatutils/parse-state hash-code)
                         (dissoc :story-maps))
        parsed-state (-> parsed-state
                         (update
                          :display
                          dissoc
                          :left-drawer ; discard the left drawer open/closed state
                          :right-sidebars) ; discard the right sidebar state
                         (cond->
                          (not (get-in parsed-state [:display :left-drawer])) ; if the left drawer was closed, then discard the tab state
                           (update :display dissoc :left-drawer-tab)))
        db           (merge-in db parsed-state)
        {:keys [active active-base zoom center]} (:map db)
        startup-layers (get-in db [:map :keyed-layers :startup] [])

        active-layers (if active
                        (vec (ids->layers active (get-in db [:map :layers])))
                        startup-layers)
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base))

        {:keys [opacity-ids]} db
        layers        (get-in db [:map :layers])
        legends-shown (init-layer-legend-status layers active) ; get legends for all active layers - needed so legends can display in the hazard legends panel when the app loads
        legends-get   (concat
                       (map #(rich-layer->displayed-layer % db) legends-shown) ; displayed layers to get legends for
                       (filter identity (map #(mutils/rich-layer->side-by-side-views-selected-layer % db) legends-shown))) ; get legends for any side-by-side views
        db            (-> db
                          (assoc-in [:layer-state :legend-shown] legends-shown)
                          (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids)))

        feature-location      (get-in db [:feature :location])
        feature-leaflet-props (get-in db [:feature :leaflet-props])
        rich-layers (get-in db [:map :rich-layers :rich-layers])
        cql-get
        (->>
         active ; get CQL filters for all applicable active layers
         (mapv #(get-in db [:map :rich-layers :layer-lookup %]))
         (mapv (fn [id] (first-where #(= (:id %) id) rich-layers))))

        dynamic-pills (get-in db [:dynamic-pills :dynamic-pills])
        active-dynamic-pills (filter #(get-in db [:dynamic-pills :states (:id %) :active?]) dynamic-pills)]
    {:db         db
     :dispatch-n
     (concat
      [[:map/update-map-view {:zoom zoom :center center}]
       (when (and feature-location feature-leaflet-props)
         [:map/feature-info-dispatcher feature-leaflet-props feature-location])
       [:map/popup-closed]]
      (mapv #(vector :map.layer/get-legend %) (filter identity legends-get))
      (mapv #(vector :map.rich-layer/get-cql-filter-values %) (filter identity cql-get))
      (mapv #(vector :dynamic-pill.region-control/get-values %) active-dynamic-pills))}))

(defn re-boot
  "Identical to imas-seamap.events/re-boot, just triggers the private versions of
   the boot flow functions that don't load region reports and state of knowledge
   data."
 [{:keys [db]} _]
  (let [db (merge-in db/default-db (nhatutils/ajax-loaded-info db))
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        db (-> db
               (assoc-in [:map :active-layers] startup-layers)
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true))
        {:keys [zoom center bounds]} (:map db)]
    {:db         db
     :dispatch   [:map/update-map-view (if (seq startup-layers) {:bounds (:bounding_box (first startup-layers))} {:zoom zoom :center center :bounds bounds})]
     :local-storage/remove
     {:name :seamap-app-state}}))

(defn initialise-layers
  "Like imas-seamap.events/initialise-layers, but we don't dispatch requests for
   region reports or state of knowledge data."
  [{:keys [db]} _]
  (let [{:keys [site-configuration-url
                layer-url
                base-layer-url
                base-layer-group-url
                organisation-url
                classification-url
                descriptor-url
                category-url
                keyed-layers-url
                rich-layers-url
                dynamic-pills-url
                story-maps-url
                cmip-phases-url
                scientific-models-url
                scenarios-url
                seasons-url]} (get-in db [:config :urls])]
    {:db         db
     :http-xhrio [{:method          :get
                   :uri             site-configuration-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:update-site-configuration]
                   :on-failure      [:update-site-configuration/error-handler]}
                  {:method          :get
                   :uri             layer-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             base-layer-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-base-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             base-layer-group-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-base-layer-groups]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             descriptor-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-descriptors]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             classification-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-classifications]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             organisation-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-organisations]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             category-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-categories]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             keyed-layers-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-keyed-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             rich-layers-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-rich-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             dynamic-pills-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:update-dynamic-pills]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             story-maps-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sm/update-featured-maps]
                   :on-failure      [:sm/update-featured-maps []]}
                  {:method          :get
                   :uri             cmip-phases-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:current-view/update-cmip-phases]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             scientific-models-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:current-view/update-models]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             scenarios-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:current-view/update-scenarios]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             seasons-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:current-view/update-seasonal-datas]
                   :on-failure      [:ajax/default-err-handler]}]}))

(defn create-save-state
  "Like imas-seamap.events/create-save-state, but uses the NHAT version of
   encode-state, which doesn't include state of knowledge, but does include the
   current view (selections of model, scenario, seasonal data, and time period)."
  [{:keys [db]} _]
  (copy-text js/location.href)
  (let [save-state-url (get-in db [:config :urls :save-state-url])]
    {:http-xhrio [{:method          :post
                   :uri             save-state-url
                   :params          {:hashstate (nhatutils/encode-state db)}
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:create-save-state-success]
                   :on-failure      [:create-save-state-failure]}]}))

(defn maybe-autosave
  "Like imas-seamap.events/maybe-autosave, but uses the NHAT version of
   encode-state, which doesn't include state of knowledge, but does include the
   current view (selections of model, scenario, seasonal data, and time period)."
  [{{:keys [autosave?] :as db} :db} _]
  (when autosave?
    {:local-storage/set
     {:name  :seamap-app-state
      :value (nhatutils/encode-state db)}
     :put-hash   ""}))

(defn current-view-update-cmip-phases
  "From the REST API, update the CMIP phases the user can select in the current
   view.

   Update the selected CMIP phase to be the first in the list, if no selected phase
   exists."
  [{:keys [db]} [_ cmip-phases]]
  (let [selected-cmip-phase-id (get-in db [:current-view :selected-cmip-phase-id])]
    {:db (assoc-in db [:current-view :cmip-phases] cmip-phases)
     :dispatch (when-not selected-cmip-phase-id [:current-view/selected-cmip-phase (first cmip-phases)])}))

(defn current-view-update-models
  "From the REST API, update the scientific models the user can select in the
   current view.

   Update the selected model to be the first in the list, if no selected model
   exists."
  [{:keys [db]} [_ models]]
  (let [selected-model-id (get-in db [:current-view :selected-model-id])]
    {:db (assoc-in db [:current-view :models] models)
     :dispatch (when-not selected-model-id [:current-view/selected-model (first models)])}))

(defn current-view-update-scenarios
  "From the REST API, update the scenarios the user can select in the current view.

   Update the selected scenario to be the first in the list, if no selected
   scenario exists."
  [{:keys [db]} [_ scenarios]]
  (let [selected-scenario-id (get-in db [:current-view :selected-scenario-id])]
    {:db (assoc-in db [:current-view :scenarios] scenarios)
     :dispatch (when-not selected-scenario-id [:current-view/selected-scenario (first scenarios)])}))

(defn current-view-update-seasonal-datas
  "From the REST API, update the seasons the user can select in the current view.

   Update the selected season to be the first in the list, if no selected season
   exists."
  [{:keys [db]} [_ seasonal-datas]]
  (let [selected-seasonal-data-id (get-in db [:current-view :selected-seasonal-data-id])]
    {:db (assoc-in db [:current-view :seasonal-datas] seasonal-datas)
     :dispatch (when-not selected-seasonal-data-id [:current-view/selected-seasonal-data (first seasonal-datas)])}))

(defn current-view-selected-cmip-phase
  "CMIP (Coupled Model Intercomparison Project) phase that organizes models and
   scenarios for analyzing hazard data."
  [{:keys [db]} [_ {cmip-phase-id :id :as cmip-phase}]]
  (let [cmip-phases (get-in db [:current-view :cmip-phases])]
    (assert (some #{cmip-phase-id} (map :id cmip-phases)) (str "Selected CMIP phase " cmip-phase " is not a valid option"))
    {:db (assoc-in db [:current-view :selected-cmip-phase-id] cmip-phase-id)
     :dispatch [:maybe-autosave]}))

(defn current-view-selected-model
  "Scientific model to analyze the hazard data"
  [{:keys [db]} [_ {model-id :id :as model}]]
  (let [models (get-in db [:current-view :models])]
    (assert (some #{model-id} (map :id models)) (str "Selected model " model " is not a valid option"))
    {:db (assoc-in db [:current-view :selected-model-id] model-id)
     :dispatch [:maybe-autosave]}))

(defn current-view-selected-scenario
  "Scientific scenario to analyze the hazard data"
  [{:keys [db]} [_ {scenario-id :id :as scenario}]]
  (let [scenarios (get-in db [:current-view :scenarios])]
    (assert (some #{scenario-id} (map :id scenarios)) (str "Selected scenario " scenario " is not a valid option"))
    {:db (assoc-in db [:current-view :selected-scenario-id] scenario-id)
     :dispatch [:maybe-autosave]}))

(defn current-view-selected-seasonal-data
  "Season to view the hazard data under"
  [{:keys [db]} [_ {seasonal-data-id :id :as seasonal-data}]]
  (let [seasonal-datas (get-in db [:current-view :seasonal-datas])]
    (assert (some #{seasonal-data-id} (map :id seasonal-datas)) (str "Selected seasonal data " seasonal-data " is not a valid option"))
    {:db (assoc-in db [:current-view :selected-seasonal-data-id] seasonal-data-id)
     :dispatch [:maybe-autosave]}))

(defn current-view-selected-time-period
  "Time period to analyze the hazard data.

   If the currently selected time isn't avaliable in the new period, reset to the
   first available time in the range."
  [{:keys [db]} [_ {time-period-id :id :as time-period}]]
  (assert (some #{time-period-id} (map :id nhatutils/time-periods)) (str "Selected time period " time-period " is not a valid option"))
  (let [{:keys [start-year end-year]} (first-where  #(= (:id %) time-period-id) nhatutils/time-periods)
        current-time                  (get-in db [:display :current-time])
        available-times               (get-in db [:display :available-times])
        current-time-in-range?        (nhatutils/time-in-range? current-time start-year end-year)
        first-time-in-range           (first (filter #(nhatutils/time-in-range? % start-year end-year) available-times))]
    {:db (assoc-in db [:current-view :selected-time-period-id] time-period-id)
     :dispatch-n
     [(when-not current-time-in-range? [:map.time/current-time first-time-in-range])
      [:maybe-autosave]]}))

(defn current-view-time-step-forward
  "Move forward one time step in the hazard data"
  [{:keys [db]} _]
  (let [available-times (get-in db [:display :available-times])
        current-time    (get-in db [:display :current-time])
        current-index   (.indexOf available-times current-time)
        next-index      (mod (inc current-index) (count available-times))
        next-time       (nth available-times next-index)]
    (assert (seq available-times) "No available times to step through")
    (assert current-time "Current time is not set")
    {:dispatch [:map.time/current-time next-time]}))

(defn current-view-time-step-backward
  "Move backward one time step in the hazard data"
  [{:keys [db]} _]
  (let [available-times (get-in db [:display :available-times])
        current-time    (get-in db [:display :current-time])
        current-index   (.indexOf available-times current-time)
        prev-index      (mod (dec current-index) (count available-times))
        prev-time       (nth available-times prev-index)]
    (assert (seq available-times) "No available times to step through")
    (assert current-time "Current time is not set")
    {:dispatch [:map.time/current-time prev-time]}))

(defn feature-info-dispatcher
  "Takes a map click event, and dispatches :map/get-feature-info events for each
   visible layer.

   Overrides imas-seamap.map.events/feature-info-dispatcher by using
   `nhatutils/displayed-layers-under-point` instead of
   `mutils/displayed-layers-under-point`, which uses uses the NHAT
   `layer-displayed-layers-lookup` function that changes hazard layers server URLs.

   Args:
   - leaflet-props: Current Leaflet map state (zoom, size, center, bounds, etc)
   - point:         The lat lng and x y pixel coords of the clicked point"
  [{:keys [db]} [_ leaflet-props point]]
  (let [layers                        (get-in db [:map :layers])
        rich-layer-fn                 (mutils/rich-layer-fn db)
        hazard-layers                 (nhatutils/hazard-layers layers)
        selected-cmip-phase           (nhatutils/current-view-selected-cmip-phase db)
        selected-model                (nhatutils/current-view-selected-model db)
        selected-scenario             (nhatutils/current-view-selected-scenario db)
        selected-seasonal-data        (nhatutils/current-view-selected-seasonal-data db)
        hazard-layer-slug             (nhatutils/current-view-hazard-layer-slug selected-cmip-phase selected-model selected-scenario selected-seasonal-data)
        layer-displayed-layers-lookup (nhatutils/layer-displayed-layers-lookup layers rich-layer-fn hazard-layers hazard-layer-slug)
        
        visible-layers
        (nhatutils/displayed-layers-under-point (mutils/visible-layers (:map db)) layer-displayed-layers-lookup point db)
        secure-layers  (remove #(mutils/is-insecure? (:server_url %)) visible-layers)
        request-id     (gensym)

        ;; Requests used to be grouped by server URL, but has since been changed to be
        ;; per-layer (many reasons, but the  triggering factor was separating the CQL
        ;; filters per layer).
        ;; We now generate just one :map/get-feature-info event per layer.
        ;; :map/get-feature-info hasn't been updated to remove the multiple layers
        ;; parameter, but sending in a vector of a single layer works fine.
        requests       (map
                        (fn [{:keys [info_format_type] :as layer}]
                          [:map/get-feature-info info_format_type [layer] request-id leaflet-props point])
                        secure-layers)
        had-insecure?  (some #(mutils/is-insecure? (:server_url %)) visible-layers)
        db             (if had-insecure?
                         (assoc db :feature {:status :feature-info/none-queryable :location point :show? true}) ;; This is the fall-through case for "layers are visible, but they're http so we can't query them":
                         (assoc ;; Initialise marshalling-pen of data: how many in flight, and current best-priority response
                          db
                          :feature-query
                          {:request-id        request-id
                           :response-remain   (count requests)
                           :had-insecure?     had-insecure?
                           :responses         []}
                          :feature
                          {:status   :feature-info/waiting
                           :leaflet-props leaflet-props
                           :location point
                           :show?    false}))]
    (merge
     {:db db
      :dispatch-later {:ms 300 :dispatch [:map.feature/show request-id]}}
     (if (and (seq requests) (not had-insecure?))
       {:dispatch-n requests}
       {:dispatch   [:map/got-featureinfo request-id point nil nil []]}))))
