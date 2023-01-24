;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.events
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            [goog.dom :as gdom]
            [imas-seamap.blueprint :as b]
            [imas-seamap.db :as db]
            [imas-seamap.utils :refer [copy-text encode-state geonetwork-force-xml merge-in parse-state append-params-from-map ajax-loaded-info ids->layers]]
            [imas-seamap.map.utils :as mutils :refer [habitat-layer? download-link latlng-distance init-layer-legend-status init-layer-opacities visible-layers]]
            [re-frame.core :as re-frame]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn not-yet-implemented
  "Register this handler against event symbols that don't have a
  handler yet"
  [db [sym & _args :as _event-v]]
  (js/console.warn "Warning: no handler for" sym "implemented yet")
  db)

(defn boot-flow []
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls
     :dispatch-n [[:initialise-layers]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers]
     :dispatch [:map/join-keyed-layers]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/update-national-layer-timeline
                                  :map/update-region-reports
                                  :sok/update-amp-boundaries
                                  :sok/update-imcra-boundaries
                                  :sok/update-meow-boundaries]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch [:welcome-layer/open]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn boot-flow-hash-state [hash-code]
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:load-hash-state hash-code]}
    {:when :seen? :events :load-hash-state
     :dispatch-n [[:initialise-layers]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers]
     :dispatch [:map/join-keyed-layers]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/update-national-layer-timeline
                                  :map/update-region-reports
                                  :sok/update-amp-boundaries
                                  :sok/update-imcra-boundaries
                                  :sok/update-meow-boundaries]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch [:welcome-layer/open]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn boot-flow-save-state [shortcode]
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:get-save-state shortcode [:load-hash-state]]}
    {:when :seen? :events :load-hash-state
     :dispatch-n [[:initialise-layers]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers]
     :dispatch [:map/join-keyed-layers]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :map/update-national-layer-timeline
                                  :map/update-region-reports
                                  :sok/update-amp-boundaries
                                  :sok/update-imcra-boundaries
                                  :sok/update-meow-boundaries]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch [:welcome-layer/open]
     :halt? true}
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
          national-layer-timeline
          region-reports
          amp-boundaries
          imcra-boundaries
          meow-boundaries
          habitat-statistics
          bathymetry-statistics
          habitat-observations
          layer-previews
          story-maps
          region-report-pages]}
        (get-in db [:config :url-paths])
        {:keys [api-url-base media-url-base wordpress-url-base _img-url-base]} (get-in db [:config :url-base])]
    (assoc-in
     db [:config :urls]
     {:layer-url                   (str api-url-base layer)
      :base-layer-url              (str api-url-base base-layer)
      :base-layer-group-url        (str api-url-base base-layer-group)
      :organisation-url            (str api-url-base organisation)
      :classification-url          (str api-url-base classification)
      :region-stats-url            (str api-url-base region-stats)
      :descriptor-url              (str api-url-base descriptor)
      :save-state-url              (str api-url-base save-state)
      :category-url                (str api-url-base category)
      :keyed-layers-url            (str api-url-base keyed-layers)
      :national-layer-timeline-url (str api-url-base national-layer-timeline)
      :region-reports-url          (str api-url-base region-reports)
      :amp-boundaries-url          (str api-url-base amp-boundaries)
      :imcra-boundaries-url        (str api-url-base imcra-boundaries)
      :meow-boundaries-url         (str api-url-base meow-boundaries)
      :habitat-statistics-url      (str api-url-base habitat-statistics)
      :bathymetry-statistics-url   (str api-url-base bathymetry-statistics)
      :habitat-observations-url    (str api-url-base habitat-observations)
      :layer-previews-url          (str media-url-base layer-previews)
      :story-maps-url              (str wordpress-url-base story-maps)
      :region-report-pages-url     (str wordpress-url-base region-report-pages)})))

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
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        db (-> db
               (assoc-in [:map :active-layers] startup-layers)
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true))
        {:keys [zoom center]} (:map db)]
    {:db         db
     :dispatch   [:map/update-map-view (if (seq startup-layers) {:bounds (:bounding_box (first startup-layers))} {:zoom zoom :center center})]
     :cookie/set {:name  :cookie-state
                  :value nil}}))

(defn loading-screen [db [_ msg]]
  (assoc db
         :loading true
         :loading-message (or msg "Loading Seamap Layers...")))

(defn application-loaded [db _]
  (assoc db :loading false))

(defn loading-failed [db _]
  (assoc db :load-error-msg "Something went wrong!"))

(defn initialise-db [_ _]
  {:db db/default-db
   :dispatch [:db-initialised]})

(defn load-hash-state
  [{:keys [db]} [_ hash-code]]
  (let [db (merge-in db (parse-state hash-code))]
    (merge
     {:db db}
     (when (and hash-code (not= hash-code "null"))
       {:dispatch [:map/update-map-view (assoc (:map db) :instant? true)]}))))

(defn get-save-state
  "Uses a shortcode to retrieve a save-state. On success dispatches an event with
   the retrieved save-state."
  [{:keys [db]} [_ shortcode dispatch]]
  (let [save-state-url (get-in db [:config :urls :save-state-url])]
    {:db db
     :http-xhrio
     [{:method          :get
       :uri             (append-params-from-map save-state-url {:id shortcode})
       :response-format (ajax/json-response-format {:keywords? true})
       :on-success      [:get-save-state-success dispatch]
       :on-failure      [:ajax/default-err-handler]}]}))

(defn get-save-state-success [_ [_ dispatch save-state]]
  {:dispatch (conj dispatch (-> save-state first :hashstate))})

(defn initialise-layers [{:keys [db]} _]
  (let [{:keys [layer-url
                base-layer-url
                base-layer-group-url
                organisation-url
                classification-url
                descriptor-url
                category-url
                keyed-layers-url
                national-layer-timeline-url
                region-reports-url
                amp-boundaries-url
                imcra-boundaries-url
                meow-boundaries-url
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
                   :uri             national-layer-timeline-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-national-layer-timeline]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             region-reports-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-region-reports]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             amp-boundaries-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sok/update-amp-boundaries]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             imcra-boundaries-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sok/update-imcra-boundaries]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             meow-boundaries-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sok/update-meow-boundaries]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             story-maps-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:sm/update-featured-maps]
                   :on-failure      [:sm/update-featured-maps []]}]}))

(defn help-layer-toggle [db _]
  (update-in db [:display :help-overlay] not))

(defn help-layer-open [db _]
  (assoc-in db [:display :help-overlay] true))

(defn help-layer-close [db _]
  (assoc-in db [:display :help-overlay] false))

;;; Allow optional force-open arg to override already-seen cookie
(defn welcome-layer-open [{db :db cookies :cookie/get} [_ force-open]]
  (if-not (and (:seen-welcome cookies) (not force-open))
    {:db (assoc-in db [:display :welcome-overlay] true)}))

(defn welcome-layer-close [{:keys [db]} _]
  {:db         (assoc-in db [:display :welcome-overlay] false)
   :cookie/set {:name  :seen-welcome
                :value true}})

(defn layer-show-info [{:keys [db]} [_ {:keys [metadata_url] :as layer}]]
  (if (re-matches #"^https://metadata\.imas\.utas\.edu\.au/geonetwork/srv/eng/catalog.search#/metadata/[0-9a-f]{8}\-[0-9a-f]{4}\-4[0-9a-f]{3}\-[89ab][0-9a-f]{3}\-[0-9a-f]{12}$" metadata_url)
    {:db         (assoc-in db [:display :info-card] :display.info/loading)
     :http-xhrio {:method          :get
                  :uri             (-> layer :metadata_url geonetwork-force-xml)
                  :response-format (ajax/text-response-format)
                  :on-success      [:map.layer/update-metadata layer]
                  :on-failure      [:map.layer/metadata-error]}}
    {:db (assoc-in db [:display :info-card :layer] layer)}))

;; (xml/alias-uri 'mcp "http://schemas.aodn.org.au/mcp-2.0")
;; (xml/alias-uri 'gmd "http://www.isotc211.org/2005/gmd")
;; (xml/alias-uri 'gco "http://www.isotc211.org/2005/gco")
;;; There's some unfortunate verbosity here.  Ideally we'd alias-uri
;;; as above, but that doesn't work in clojurescript because it can't
;;; automatically create namespaces.  We can create the namespaces
;;; manually... except that the mcp namespace would wind up with a
;;; single file 0.cljs, which is invalid.  So we just do it the long
;;; way.
(defn layer-receive-metadata [db [_ layer raw-response]]
  (let [zipped           (-> raw-response xml/parse-str zip/xml-zip)
        constraints      (zx/xml1-> zipped
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmdb%2F2.0/MD_Metadata
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmdb%2F2.0/identificationInfo
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmri%2F1.0/MD_DataIdentification
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmri%2F1.0/resourceConstraints
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmco%2F1.0/MD_LegalConstraints)
        license-link     (zx/xml1-> constraints
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmco%2F1.0/reference
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/CI_Citation
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/onlineResource
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/CI_OnlineResource
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/linkage
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fgco%2F1.0/CharacterString
                                    zx/text)
        license-img      (zx/xml1-> constraints
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmco%2F1.0/graphic
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmcc%2F1.0/MD_BrowseGraphic
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmcc%2F1.0/linkage
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/CI_OnlineResource
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/linkage
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fgco%2F1.0/CharacterString
                                    zx/text)
        license-name     (zx/xml1-> constraints
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmco%2F1.0/reference
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/CI_Citation
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fcit%2F2.0/title
                                    :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fgco%2F1.0/CharacterString
                                    zx/text)
        other (zx/xml->  constraints
                         :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fmco%2F1.0/otherConstraints
                         :xmlns.http%3A%2F%2Fstandards.iso.org%2Fiso%2F19115%2F-3%2Fgco%2F1.0/CharacterString
                         zx/text)]
    (assoc-in db [:display :info-card]
              {:layer        layer
               :license-name license-name
               :license-link license-link
               :license-img  license-img
               :constraints  (if (seq? other) (first other) other)
               :other        (when (seq? other) (rest other))})))

(defn layer-receive-metadata-err [db [_ & _err]]
  (assoc-in db [:display :info-card] :display.info/error))

(defn layer-close-info [db _]
  (assoc-in db [:display :info-card] nil))

(defn- geojson->linestring [geojson]
  ;; No assertions or anything for now (could make the geojson spec more explicit, but this will be fine)
  (get-in geojson [:geometry :coordinates]))

(defn- coords->linestring
  "Turn a list of coords into a formatted linestring parameter, ie
  comma-separated pairs of space-separated coords."
  [coords]
  (->> coords
       (map (partial string/join " "))
       (string/join ",")))

(defn- geojson-linestring->bbox [coords]
  (let [[minx miny maxx maxy]
        (reduce
         (fn [[minx miny maxx maxy] [x y]]
           [(min minx x) (min miny y) (max maxx x) (max maxy y)])
         [js/Number.POSTIVE_INFINITY js/Number.POSTIVE_INFINITY js/Number.NEGATIVE_INFINITY js/Number.NEGATIVE_INFINITY]
         coords)]
    {:west  minx
     :south miny
     :east  maxx
     :north maxy}))

(defn- linestring->distance [linestring]
  (:distance
   (reduce
    (fn [{:keys [prev-latlng] :as acc} curr-latlng]
      (-> acc
          (update :distance + (latlng-distance prev-latlng curr-latlng))
          (assoc :prev-latlng curr-latlng)))
    {:distance 0 :prev-latlng (first linestring)}
    (rest linestring))))

(defn transect-query [{{db-map :map :as db} :db} [_ geojson]]
  ;; Reset the transect before querying (and paranoia to avoid
  ;; unlikely race conditions; do this before dispatching)
  (let [query-id (gensym)
        linestring (geojson->linestring geojson)
        db (assoc db :transect {:query geojson
                                :query-id query-id
                                :distance (linestring->distance linestring)
                                :habitat :loading
                                :bathymetry :loading})
        visible-layers (visible-layers db-map)
        habitat-layers (filter habitat-layer? visible-layers)]
    (merge
     {:db         db
      :dispatch-n (cond-> [[:maybe-autosave]]
                    (seq habitat-layers) ; only display transect plot if there's an active habitat layer
                    (conj [:transect.plot/show]
                          [:transect.query/habitat query-id linestring]
                          [:transect.query/bathymetry query-id linestring]))})))

(defn transect-maybe-query [{:keys [db]}]
  ;; When existing transect state is rehydrated it will have the
  ;; query, but that will need to be re-executed. Only do this if we
  ;; actually have a query of course:
  (when-let [query (get-in db [:transect :query])]
    {:dispatch [:transect/query query]}))

(defn transect-query-cancel [db]
  (let [clear-loading (fn [val] (if (= val :loading) [] val))]
    (-> db
        (update :transect dissoc :query-id)
        (update-in [:transect :habitat] clear-loading)
        (update-in [:transect :bathymetry] clear-loading))))

(defn transect-query-error [{:keys [db]} [_ type query-id {:keys [last-error failure response] :as _http-response}]]
  (when (= query-id
           (get-in db [:transect :query-id]))
    (let [status-text (cond
                        (= failure :timeout) (str "Remote server timed out querying " (name type))
                        ;; We'll guess it's a server time out due to complexity, here:
                        (= type :habitat) "Transect too complex.  Try using fewer layers or a shorter transect."
                        :default
                        (str "Error querying " (name type) ": " (or (:status-text response)
                                                                    (:debug-message  response)
                                                                    last-error)))]
      {:db      (assoc-in db [:transect type] status-text)
       :message [status-text b/INTENT-DANGER]})))

(defn transect-query-habitat [{{db-map :map :as db} :db} [_ query-id linestring]]
  (let [visible-layers (visible-layers db-map)
        habitat-layers (filter habitat-layer? visible-layers)
        ;; Note, we reverse because the top layer is last, so we want
        ;; its features to be given priority in this search, so it
        ;; must be at the front of the list:
        layer-names    (->> habitat-layers (map :layer_name) reverse (string/join ","))
        api-url-base   (get-in db [:config :url-base :api-url-base])]
    (if (seq habitat-layers)
      {:db         db
       :http-xhrio {:method          :get
                    :uri             (str api-url-base "habitat/transect/")
                    :params          {:layers layer-names
                                      :line   (->> linestring
                                                   (map mutils/wgs84->epsg3112)
                                                   coords->linestring)}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:transect.query.habitat/success query-id]
                    :on-failure      [:transect.query/failure :habitat query-id]}}
      ;; No layers to query; reset habitat to no-results:
      {:db (assoc-in db [:transect :habitat] [])})))

(defn transect-query-habitat-success [db [_ query-id response]]
  (cond-> db
    (= query-id (get-in db [:transect :query-id]))
    (assoc-in [:transect :habitat] response)))

(defn transect-query-bathymetry [{:keys [db]} [_ query-id linestring]]
  (if-let [{:keys [server_url layer_name] :as _bathy-layer}
           (first (get-in db [:map :keyed-layers :transect-bathy]))]
    {:db         db
     :http-xhrio {:method          :get
                  :uri             server_url
                  :params          {:REQUEST    "GetTransect"
                                    :LAYER      layer_name
                                    :SERVICE    "WMS"
                                    :LINESTRING (coords->linestring linestring)
                                    :CRS        "EPSG:4326"
                                    :format     "text/xml"
                                    :VERSION    "1.1.1"}
                  :response-format (ajax/text-response-format)
                  :on-success      [:transect.query.bathymetry/success query-id]
                  :on-failure      [:transect.query/failure :bathymetry query-id]}}
    ;; Otherwise, don't bother with ajax; immediately return no results
    {:db (assoc-in db [:transect :bathymetry] [])}))

(defn transect-query-bathymetry-success [db [_ query-id response]]
  (if (= query-id
         (get-in db [:transect :query-id]))
    (let [zipped      (->> response xml/parse-str zip/xml-zip)
          data-points (zx/xml1-> zipped :transect :transectData)
          num-points  (zx/xml1->  data-points (zx/attr :numPoints) js/parseInt)
          values      (zx/xml-> data-points :dataPoint :value zx/text js/parseFloat)
          increment   (/ 100 num-points)]
      (assoc-in db [:transect :bathymetry]
                (vec (map-indexed (fn [i v] [(* i increment) (if (js/isNaN v) nil (- v))]) values))))
    db))

(defn transect-drawing-start [{:keys [db]} _]
  {:db       (-> db
                 (assoc-in [:map :controls :transect] true)
                 (assoc-in [:transect :query] nil))
   :dispatch [:left-drawer/close]})

(defn transect-drawing-finish [db _]
  (assoc-in db [:map :controls :transect] false))

(defn transect-drawing-clear [db _]
  (update-in db [:transect] merge {:show?      false
                                   :query      nil
                                   :distance   nil
                                   :habitat    nil
                                   :bathymetry nil}))

(defn transect-drawing-toggle [{:keys [db]} _]
  {:dispatch
   (cond
     (get-in db [:transect :query])         [:transect.draw/clear]
     (get-in db [:map :controls :transect]) [:transect.draw/disable]
     :default                               [:transect.draw/enable])})

(defn transect-visibility-toggle [db _]
  (update-in db [:transect :show?] not))

(defn transect-visibility-show [db _]
  (assoc-in db [:transect :show?] true))

(defn transect-visibility-hide [db _]
  (assoc-in db [:transect :show?] false))

(defn transect-onmousemove [db [_ {:keys [percentage]}]]
  (assoc-in db [:transect :mouse-percentage] percentage))

(defn transect-onmouseout [db _]
  (assoc-in db [:transect :mouse-percentage] nil))

(defn download-show-link [db [_ layer bounds download-type]]
  (update-in db [:map :controls :download]
             merge {:link         (download-link layer bounds download-type (get-in db [:config :url-base :api-url-base]))
                    :layer        layer
                    :type         download-type
                    :bbox         bounds
                    :display-link true}))

(defn close-download-dialogue [db _]
  (assoc-in db [:map :controls :download :display-link] false))

(defn ajax [db [_ url {:keys [handler err-handler override-opts]
                       :or   {handler     :ajax/default-success-handler
                              err-handler :ajax/default-err-handler}
                       :as opts}]]
  (ajax/GET url
            (merge
             {:handler #(re-frame/dispatch [handler %])
              :error-handler #(re-frame/dispatch [err-handler %])
              :response-format :json :keywords? true}
             override-opts))
  db)

(defn default-success-handler [db [_ response]]
  (js/console.info "Received AJAX response" response)
  db)

(defn default-err-handler [db [_ response]]
  (js/console.info "AJAX error response" response)
  db)

(defn show-message [_ctx [_ message intent-or-opts]]
  (let [opts (merge {:intent b/INTENT-WARNING}
                   (if (map? intent-or-opts) intent-or-opts {:intent intent-or-opts}))]
   {:message [message opts]}))

(defn clear-message [db _]
  (. b/toaster clear)
  db)

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

(defn create-save-state-success
  [_ [_ response]]
  (let [url (str js/location.origin js/location.pathname "#" (:id response))]
    (copy-text url)
    {:message ["URL copied to clipboard!"
               {:intent b/INTENT-SUCCESS :icon "clipboard"}]}))

(defn create-save-state-failure
  [_ _]
  {:message ["Failed to generate URL!"
             {:intent b/INTENT-WARNING :icon "warning-sign"}]})

(defn catalogue-select-tab [{:keys [db]} [_ tabid]]
  (let [db (assoc-in db [:display :catalogue :tab] tabid)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn catalogue-toggle-node [{:keys [db]} [_ nodeid]]
  (let [nodes (get-in db [:display :catalogue :expanded])
        db    (update-in db [:display :catalogue :expanded] (if (nodes nodeid) disj conj) nodeid)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn catalogue-add-node [{:keys [db]} [_ nodeid]]
  (let [db (update-in db [:display :catalogue :expanded] conj nodeid)]
   {:db       db
    :dispatch [:maybe-autosave]}))

(defn catalogue-add-nodes-to-layer
  "Opens nodes in catalogue along path to specified layer"
  [{:keys [db]} [_ layer tab categories]]
  (let [sorting-info (:sorting db)
        node-ids   (reduce
                    (fn [node-ids category]
                      (let [sorting-id (get-in sorting-info [category (category layer) 1])
                            node-id (-> (last node-ids)
                                        (or tab)
                                        (str "|" sorting-id))]
                        (conj node-ids node-id)))
                    [] categories)]
    {:dispatch-n (map #(vec [:ui.catalogue/add-node %]) node-ids)}))

(defn sidebar-open [{:keys [db]} [_ tabid]]
  (let [{:keys [selected collapsed]} (get-in db [:display :sidebar])
        db (-> db
               (assoc-in [:display :sidebar :selected] tabid)
               ;; Allow the left tab to close as well as open, if clicking same icon:
               (assoc-in [:display :sidebar :collapsed] (and (= tabid selected) (not collapsed))))]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn sidebar-close [db _]
  (assoc-in db [:display :sidebar :collapsed] true))

(defn sidebar-toggle [db _]
  (update-in db [:display :sidebar :collapsed] not))

(defn focus-search [_ _]
  (.focus (gdom/getElement "layer-search")))

;;; Slightly hacky, in that we manually cancel everything that might
;;; be in flight. Convenient for the user though.
(defn global-drawing-cancel [db _]
  (-> db
      (assoc-in [:map :controls :transect] false)
      (assoc-in [:map :controls :download :selecting] false)))

(defn selection-list-reorder
  [{:keys [db]} [_ src-idx dst-idx data-path]]
  (let [data (get-in db data-path)
        element (get data src-idx)
        removed (into [] (concat (subvec data 0 src-idx) (subvec data (+ src-idx 1) (count data))))
        readded (into [] (concat (subvec removed 0 dst-idx) [element] (subvec removed dst-idx (count removed))))
        db (assoc-in db data-path readded)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn left-drawer-toggle [{:keys [db]} _]
  {:db       (update-in db [:display :left-drawer] not)
   :dispatch [:maybe-autosave]})

(defn left-drawer-open [{:keys [db]} _]
  {:db       (assoc-in db [:display :left-drawer] true)
   :dispatch [:maybe-autosave]})

(defn left-drawer-close [{:keys [db]} _]
  {:db       (assoc-in db [:display :left-drawer] false)
   :dispatch [:maybe-autosave]})

(defn left-drawer-tab [{:keys [db]} [_ tab]]
  (let [db (assoc-in db [:display :left-drawer-tab] tab)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn layers-search-omnibar-toggle [db _]
  (update-in db [:display :layers-search-omnibar] not))

(defn layers-search-omnibar-open [db _]
  (assoc-in db [:display :layers-search-omnibar] true))

(defn layers-search-omnibar-close [db _]
  (assoc-in db [:display :layers-search-omnibar] false))

(defn mouse-pos [db [_ mouse-pos]]
  (assoc-in db [:display :mouse-pos] mouse-pos))

(defn toggle-autosave [{:keys [db]} _]
  (let [db        (update db :autosave? not)
        autosave? (:autosave? db)]
    (merge
     {:db db}
     (if autosave?
       {:dispatch [:maybe-autosave]}
       {:cookie/set {:name  :cookie-state
                     :value nil}}))))

(defn maybe-autosave [{{:keys [autosave?] :as db} :db} _]
  (when autosave?
    {:cookie/set {:name  :cookie-state
                  :value (encode-state db)}
     :put-hash   ""}))

(defn settings-overlay [db [_ open?]]
  (assoc-in db [:display :settings-overlay] open?))
