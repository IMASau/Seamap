;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.events
  (:require [ajax.core :as ajax]
            [imas-seamap.tas-marine-atlas.db :as db]
            [imas-seamap.utils :refer [copy-text merge-in ids->layers first-where]]
            [imas-seamap.map.utils :as mutils :refer [init-layer-legend-status init-layer-opacities rich-layer->displayed-layer]]
            [imas-seamap.tas-marine-atlas.utils :refer [encode-state parse-state ajax-loaded-info]]
            [imas-seamap.blueprint :as b]
            [reagent.core :as r]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- boot-flow []
  {:first-dispatch [:ui/show-loading]
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
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-hash-state [hash-code]
  {:first-dispatch [:ui/show-loading]
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
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-save-state [shortcode]
  {:first-dispatch [:ui/show-loading]
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
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
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
          dynamic-pills
          layer-legend
          layer-previews
          story-maps
          data-in-region
          cql-filter-values]}
        (get-in db [:config :url-paths])
        {:keys [api-url-base media-url-base wordpress-url-base _img-url-base]} (get-in db [:config :url-base])]
    (assoc-in
     db [:config :urls]
     {:site-configuration-url    (str api-url-base site-configuration)
      :layer-url                 (str api-url-base layer)
      :base-layer-url            (str api-url-base base-layer)
      :base-layer-group-url      (str api-url-base base-layer-group)
      :organisation-url          (str api-url-base organisation)
      :classification-url        (str api-url-base classification)
      :region-stats-url          (str api-url-base region-stats)
      :descriptor-url            (str api-url-base descriptor)
      :save-state-url            (str api-url-base save-state)
      :category-url              (str api-url-base category)
      :keyed-layers-url          (str api-url-base keyed-layers)
      :rich-layers-url           (str api-url-base rich-layers)
      :dynamic-pills-url         (str api-url-base dynamic-pills)
      :layer-legend-url          (str api-url-base layer-legend)
      :layer-previews-url        (str media-url-base layer-previews)
      :story-maps-url            (str wordpress-url-base story-maps)
      :data-in-region-url        (str api-url-base data-in-region)
      :cql-filter-values-url     (str api-url-base cql-filter-values)})))

(defn boot [{:keys [save-code hash-code] {:keys [seamap-app-state]} :local-storage/get} [_ api-url-base media-url-base wordpress-url-base img-url-base]]
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
  "Takes a hash-code and merges it into the current application state."
  [{:keys [db]} [_ hash-code]]
  (let [parsed-state (-> (parse-state hash-code)
                         (dissoc :story-maps))
        parsed-state (-> parsed-state
                         (update :display dissoc :left-drawer) ; discard the left drawer open/closed state
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

        {:keys [legend-ids opacity-ids]} db
        layers        (get-in db [:map :layers])
        legends-shown (init-layer-legend-status layers legend-ids)
        legends-get   (map #(rich-layer->displayed-layer % db) legends-shown)
        db            (-> db
                          (assoc-in [:layer-state :legend-shown] legends-shown)
                          (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids)))

        feature-location      (get-in db [:feature :location])
        feature-leaflet-props (get-in db [:feature :leaflet-props])
        rich-layers (get-in db [:map :rich-layers :rich-layers])
        cql-get
        (->>
         legend-ids
         (mapv #(get-in db [:map :rich-layers :layer-lookup %]))
         (mapv (fn [id] (first-where #(= (:id %) id) rich-layers))))

        dynamic-pills (get-in db [:dynamic-pills :dynamic-pills])
        active-dynamic-pills (filter #(get-in db [:dynamic-pills :states (:id %) :active?]) dynamic-pills)]
    {:db         db
     :dispatch-n (concat
                  []
                  (mapv #(vector :map.layer/get-legend %) legends-get)
                  (mapv #(vector :map.rich-layer/get-cql-filter-values %) cql-get)
                  (mapv #(vector :dynamic-pill.region-control/get-values %) active-dynamic-pills)
                  [:map/update-map-view {:zoom zoom :center center}]
                  (when (and feature-location feature-leaflet-props)
                    [:map/feature-info-dispatcher feature-leaflet-props feature-location])
                  [:map/popup-closed])}))

(defn re-boot [{:keys [db]} _]
  (let [db (merge-in db/default-db (ajax-loaded-info db))
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        db (-> db
               (assoc-in [:map :active-layers] startup-layers)
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true))
        {:keys [zoom center]} (:map db)]
    {:db         db
     :dispatch   [:map/update-map-view (if (seq startup-layers) {:bounds (:bounding_box (first startup-layers))} {:zoom zoom :center center})]
     :local-storage/remove
     {:name :seamap-app-state}}))

(defn loading-screen [db [_ msg]]
  (assoc db
         :loading true
         :loading-message (or msg "Loading Tasmania Marine Atlas...")))

(defn welcome-layer-close [db _]
  (assoc-in db [:display :welcome-overlay] false))

(defn initialise-layers [{:keys [db]} _]
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
                story-maps-url]} (get-in db [:config :urls])]
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
                   :on-failure      [:sm/update-featured-maps []]}]}))

(defn create-save-state [{:keys [db]} _]
  (copy-text js/location.href)
  (let [save-state-url (get-in db [:config :urls :save-state-url])]
    {:http-xhrio [{:method          :post
                   :uri             save-state-url
                   :params          {:hashstate (encode-state db)}
                   :format          (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:create-save-state-success]
                   :on-failure      [:create-save-state-failure]}]}))

(defn maybe-autosave [{{:keys [autosave?] :as db} :db} _]
  (when autosave?
    {:local-storage/set
     {:name  :seamap-app-state
      :value (encode-state db)}
     :put-hash   ""}))

(defn show-initial-layers
  "Figure out the highest priority layer, and display it"
  ;; Slight hack; note we use :active not :active-layers, because
  ;; during boot we may have loaded hash-state, but we can't hydrate
  ;; the id from the hash state into actual layers, until the layers
  ;; themselves are loaded... by which time the state will have been
  ;; re-set.  So we have this two step process.  Ditto :active-base /
  ;; :active-base-layer
  [{:keys [db]} _]
  (let [{:keys [active active-base layers]} (:map db)
        legend-ids    (:legend-ids db)
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        active-layers (if active
                        (vec (ids->layers active (get-in db [:map :layers])))
                        startup-layers)
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        active-base   (or active-base   ; If no base is set (eg no existing hash-state), use the first candidate
                          (first (get-in db [:map :grouped-base-layers])))
        story-maps    (get-in db [:story-maps :featured-maps])
        featured-map  (get-in db [:story-maps :featured-map])
        featured-map  (first-where #(= (% :id) featured-map) story-maps)
        legends-shown (init-layer-legend-status layers legend-ids)
        legends-get   (map #(rich-layer->displayed-layer % db) legends-shown)
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base)
                          (assoc-in [:story-maps :featured-map] featured-map)
                          (assoc :initialised true))

        feature-location      (get-in db [:feature :location])
        feature-leaflet-props (get-in db [:feature :leaflet-props])
        rich-layers (get-in db [:map :rich-layers :rich-layers])
        cql-get
        (->>
         legend-ids
         (mapv #(get-in db [:map :rich-layers :layer-lookup %]))
         (mapv (fn [id] (first-where #(= (:id %) id) rich-layers))))

        dynamic-pills (get-in db [:dynamic-pills :dynamic-pills])
        active-dynamic-pills (filter #(get-in db [:dynamic-pills :states (:id %) :active?]) dynamic-pills)]
    {:db         db
     :dispatch-n (concat
                  [[:ui/hide-loading]
                   (when (and feature-location feature-leaflet-props)
                     [:map/feature-info-dispatcher feature-leaflet-props feature-location])
                   [:maybe-autosave]]
                  (mapv #(vector :map.layer/get-legend %) legends-get)
                  (mapv #(vector :map.rich-layer/get-cql-filter-values %) cql-get)
                  (mapv #(vector :dynamic-pill.region-control/get-values %) active-dynamic-pills))}))

(defn data-in-region-open [{:keys [db]} [_ open?]]
  {:dispatch-n
   [(if open?
      [:ui.right-sidebar/bring-to-front {:id "data-in-region" :type :data-in-region}]
      [:ui.right-sidebar/remove {:id "data-in-region" :type :data-in-region}])
    [:maybe-autosave]]})

(defn map-start-selecting [db _]
  (-> db
      (assoc-in [:map :controls :ignore-click] true)
      (assoc-in [:map :controls :download :selecting] true)))

(defn map-clear-selection [{:keys [db]} _]
  {:db (update-in db [:map :controls :download] dissoc :bbox)
   :dispatch [:data-in-region/open false]})

(defn map-finalise-selection [{:keys [db]} [_ bbox]]
  {:db      (update-in
             db [:map :controls :download]
             merge {:selecting false :bbox bbox})
   :dispatch-n [[:data-in-region/open true]
                [:data-in-region/get bbox]]})

(defn get-data-in-region [{:keys [db]} [_ bbox]]
  (let [query-id           (gensym)
        data-in-region-url (get-in db [:config :urls :data-in-region-url])]
    {:db         (update db :data-in-region merge {:data :data-in-region/loading :query-id query-id})
     :http-xhrio {:method          :get
                  :uri             data-in-region-url
                  :params          bbox
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:data-in-region/got query-id]
                  :on-failure      [:ajax/default-err-handler]}}))

(defn got-data-in-region [db [_ query-id data]]
  (when (= (get-in db [:data-in-region :query-id]) query-id)
   (update db :data-in-region merge {:data data :query-id nil})))
