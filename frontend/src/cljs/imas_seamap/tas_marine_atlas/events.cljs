;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.events
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            [goog.dom :as gdom]
            [imas-seamap.blueprint :as b]
            [imas-seamap.tas-marine-atlas.db :as db]
            [imas-seamap.utils :refer [copy-text geonetwork-force-xml merge-in append-params-from-map ids->layers]]
            [imas-seamap.map.utils :as mutils :refer [habitat-layer? download-link latlng-distance init-layer-legend-status init-layer-opacities]]
            [imas-seamap.tas-marine-atlas.utils :refer [encode-state parse-state ajax-loaded-info]]
            [re-frame.core :as re-frame]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- boot-flow []
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories]
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
                                  :map/update-base-layer-groups
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories]
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
                                  :map/update-base-layer-groups
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn construct-urls [db _]
  (let [{:keys
         [layer
          base-layer
          base-layer-group
          organisation
          classification
          region-stats
          descriptor
          save-state
          category
          keyed-layers
          layer-previews
          story-maps]}
        (get-in db [:config :url-paths])
        {:keys [api-url-base media-url-base wordpress-url-base _img-url-base]} (get-in db [:config :url-base])]
    (assoc-in
     db [:config :urls]
     {:layer-url                 (str api-url-base layer)
      :base-layer-url            (str api-url-base base-layer)
      :base-layer-group-url      (str api-url-base base-layer-group)
      :organisation-url          (str api-url-base organisation)
      :classification-url        (str api-url-base classification)
      :region-stats-url          (str api-url-base region-stats)
      :descriptor-url            (str api-url-base descriptor)
      :save-state-url            (str api-url-base save-state)
      :category-url              (str api-url-base category)
      :keyed-layers-url          (str api-url-base keyed-layers)
      :layer-previews-url        (str media-url-base layer-previews)
      :story-maps-url            (str wordpress-url-base story-maps)})))

(defn boot [{:keys [save-code hash-code] {:keys [cookie-state]} :cookie/get} [_ api-url-base media-url-base wordpress-url-base img-url-base]]
  {:db         (assoc-in
                db/default-db [:config :url-base]
                {:api-url-base       api-url-base
                 :media-url-base     media-url-base
                 :wordpress-url-base wordpress-url-base
                 :img-url-base       img-url-base})
   :async-flow (cond ; Choose async boot flow based on what information we have for the DB:
                 (seq save-code)    (boot-flow-save-state save-code)    ; A shortform save-code that can be used to query for a hash-code
                 (seq hash-code)    (boot-flow-hash-state hash-code)    ; A hash-code that can be decoded into th DB's initial state
                 (seq cookie-state) (boot-flow-hash-state cookie-state) ; Same as hash-code, except that we use the one stored in the cookies
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

        active-layers (if active
                        (vec (ids->layers active (get-in db [:map :layers])))
                        (get-in db [:map :keyed-layers :startup] []))
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base))

        {:keys [legend-ids opacity-ids]} db
        layers        (get-in db [:map :layers])
        db            (-> db
                          (assoc-in [:layer-state :legend-shown] (init-layer-legend-status layers legend-ids))
                          (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids)))]
    {:db         db
     :dispatch-n (conj
                  (mapv #(vector :map.layer/get-legend %) (init-layer-legend-status layers legend-ids))
                  [:map/update-map-view {:zoom zoom :center center}]
                  [:map/popup-closed])}))

(defn re-boot [{:keys [db]} _]
  (let [db (merge-in db/default-db (ajax-loaded-info db))
        db (-> db
               (assoc-in [:map :active-layers] (get-in db [:map :keyed-layers :startup] []))
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true))
        {:keys [zoom center]} (:map db)]
    {:db         db
     :dispatch   [:map/update-map-view {:zoom zoom :center center}]
     :cookie/set {:name  :cookie-state
                  :value nil}}))

(defn loading-screen [db [_ msg]]
  (assoc db
         :loading true
         :loading-message (or msg "Loading Tasmania Marine Atlas...")))

(defn load-hash-state
  [{:keys [db]} [_ hash-code]]
  (let [db (merge-in db (parse-state hash-code))]
    {:db db
     :dispatch [:map/update-map-view (assoc (:map db) :instant? true)]}))

(defn initialise-layers [{:keys [db]} _]
  (let [{:keys [layer-url
                base-layer-url
                base-layer-group-url
                organisation-url
                classification-url
                descriptor-url
                category-url
                keyed-layers-url
                story-maps-url]} (get-in db [:config :urls])]
    {:db         db
     :http-xhrio [{:method          :get
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
                   :uri             story-maps-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sm/update-featured-maps]
                   :on-failure      [:ajax/default-err-handler]}]}))

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
    {:cookie/set {:name  :cookie-state
                  :value (encode-state db)}
     :put-hash   ""}))
