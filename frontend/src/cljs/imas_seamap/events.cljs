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
            [imas-seamap.utils :refer [copy-text encode-state geonetwork-force-xml merge-in]]
            [imas-seamap.map.utils :as mutils :refer [applicable-layers bbox-intersects? habitat-layer? download-link]]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))

(defn not-yet-implemented
  "Register this handler against event symbols that don't have a
  handler yet"
  [db [sym & args :as event-v]]
  (js/console.warn "Warning: no handler for" sym "implemented yet")
  db)

(defn boot-flow []
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-groups
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-priorities
                                  :map/update-descriptors]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading
     :dispatch [:welcome-layer/open]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn boot [{:keys [hash-state]} _]
  (let [initial-db (cond-> (merge-in db/default-db hash-state)
                     (seq hash-state) (assoc-in [:map :logic :type] :map.layer-logic/manual))]
    {:db         initial-db
     :async-flow (boot-flow)}))

;;; Reset state.  Gets a bit messy because we can't just return
;;; default-db without throwing away ajax-loaded layer info, so we
;;; restore that manually first.
(defn re-boot [{:keys [habitat-colours habitat-titles sorting]
                {:keys [layers organisations priorities groups] :as map-state} :map
                :as db} _]
  (-> db/default-db
      (update :map merge {:layers        layers
                          :organisations organisations
                          :groups        groups
                          :priorities    priorities})
      (merge {:habitat-colours habitat-colours
              :habitat-titles  habitat-titles
              :sorting         sorting})))

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

(defn initialise-layers [{:keys [db]} _]
  (let [{:keys [layer-url group-url organisation-url classification-url priority-url descriptor-url]} (:config db)]
    {:db         db
     :http-xhrio [{:method          :get
                   :uri             layer-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             group-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-groups]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             priority-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-priorities]
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
                   :on-failure      [:ajax/default-err-handler]}]}))

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
;;; we ignore success/failure of cookie setting; these are fired by default, so just ignore:
(re-frame/reg-event-db :cookie-set-no-on-success identity)
(re-frame/reg-event-db :cookie-set-no-on-failure identity)

(defn layer-show-info [{:keys [db]} [_ layer]]
  (if-not (#{:bathymetry :boundaries :third-party} (:category layer))
    {:db         (assoc-in db [:display :info-card] :display.info/loading)
     :http-xhrio {:method          :get
                  :uri             (-> layer :metadata_url geonetwork-force-xml)
                  :response-format (ajax/text-response-format)
                  :on-success      [:map.layer/update-metadata layer]
                  :on-failure      [:map.layer/metadata-error]}}
    ;; For third-party/boundary/bathymetry layers, just show the basic layer info from the catalogue:
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
                                    :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/MD_Metadata
                                    :xmlns.http%3A%2F%2Fwww.isotc211.org%2F2005%2Fgmd/identificationInfo
                                    :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/MD_DataIdentification
                                    :xmlns.http%3A%2F%2Fwww.isotc211.org%2F2005%2Fgmd/resourceConstraints
                                    :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/MD_Commons)
        license-link     (zx/xml1-> constraints :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/licenseLink zx/text)
        license-img      (zx/xml1-> constraints :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/imageLink zx/text)
        license-name     (zx/xml1-> constraints :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/licenseName zx/text)
        attr-constraints (zx/xml->  constraints :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/attributionConstraints zx/text)
        other            (zx/xml1-> constraints :xmlns.http%3A%2F%2Fschemas.aodn.org.au%2Fmcp-2.0/otherConstraints zx/text)]
    (assoc-in db [:display :info-card]
              {:layer        layer
               :license-name license-name
               :license-link license-link
               :license-img  license-img
               :constraints  (if (seq? attr-constraints) (first attr-constraints) attr-constraints)
               :other        (flatten [(when (seq? attr-constraints) (rest attr-constraints)) other])})))

(defn layer-receive-metadata-err [db [_ & err]]
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

(defn transect-query [{:keys [db]} [_ geojson]]
  ;; Reset the transect before querying (and paranoia to avoid
  ;; unlikely race conditions; do this before dispatching)
  (let [query-id (gensym)
        db (assoc db :transect {:query geojson
                                :query-id query-id
                                :show? true
                                :habitat :loading
                                :bathymetry :loading})
        linestring (geojson->linestring geojson)]
    {:db db
     :put-hash (encode-state db)
     :dispatch-n [[:transect.plot/show] ; A bit redundant since we set the :show key above
                  [:transect.query/habitat    query-id linestring]
                  [:transect.query/bathymetry query-id linestring]]}))

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

(defn transect-query-error [{:keys [db]} [_ type query-id {:keys [last-error failure response] :as http-response}]]
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
      {:db       (assoc-in db [:transect type] status-text)
       :dispatch [:info/show-message status-text b/INTENT-DANGER]})))

(defn transect-query-habitat [{:keys [db]} [_ query-id linestring]]
  (let [bbox           (geojson-linestring->bbox linestring)
        habitat-layers (->> db :map :active-layers (filter habitat-layer?))
        ;; Note, we reverse because the top layer is last, so we want
        ;; its features to be given priority in this search, so it
        ;; must be at the front of the list:
        layer-names    (->> habitat-layers (map :layer_name) reverse (string/join ","))]
    (if (seq habitat-layers)
      {:db         db
       :http-xhrio {:method          :get
                    :uri             (str db/api-url-base "habitat/transect/")
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
  (if-let [{:keys [server_url layer_name] :as bathy-layer}
           (first (applicable-layers db :category :bathymetry
                                        :server_type :ncwms))]
    {:db         db
     :http-xhrio {:method          :get
                  :uri             server_url
                  :params          {:REQUEST    "GetTransect"
                                    :LAYER      layer_name
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

(defn transect-drawing-start [db _]
  (-> db
      (assoc-in [:map :controls :transect] true)
      (assoc-in [:transect :query] nil)))

(defn transect-drawing-finish [db _]
  (assoc-in db [:map :controls :transect] false))

(defn transect-drawing-clear [db _]
  (update-in db [:transect] merge {:show?      false
                                   :query      nil
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
             merge {:link         (download-link layer bounds download-type)
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

(defn show-message [db [_ message intent-or-opts]]
  (let [msg (merge {:intent b/INTENT-WARNING}
                   (if (map? intent-or-opts) intent-or-opts {:intent intent-or-opts})
                   {:message        message
                    :__cache-buster (js/Date.now)})]
   (assoc-in db [:info :message] msg)))

;;; A bit of a misnomer really; it cleans up the db, but won't
;;; override the timeout if a message is still displayed.
(defn clear-message [db _]
  (assoc-in db [:info :message] nil))

(defn copy-share-url [{:keys [db]} _]
  (copy-text js/location.href)
  {:dispatch [:info/show-message "URL copied to clipboard!" {:intent   b/INTENT-SUCCESS
                                                         :iconName "clipboard"}]})

(defn catalogue-select-tab [{:keys [db]} [_ tabid]]
  {:db       (assoc-in db [:display :catalogue :tab] tabid)
   :put-hash (encode-state db)})

(defn catalogue-toggle-node [{:keys [db]} [_ nodeid]]
  (let [nodes         (get-in db [:display :catalogue :expanded])
        updated-nodes (if (contains? nodes nodeid) (disj nodes nodeid) (conj nodes nodeid))
        db            (assoc-in db [:display :catalogue :expanded] updated-nodes)]
    {:db       db
     :put-hash (encode-state db)}))

(defn sidebar-open [{:keys [db]} [_ tabid]]
  (let [{:keys [selected collapsed]} (get-in db [:display :sidebar])
        db (-> db
               (assoc-in [:display :sidebar :selected] tabid)
               ;; Allow the left tab to close as well as open, if clicking same icon:
               (assoc-in [:display :sidebar :collapsed] (and (= tabid selected) (not collapsed))))]
    {:db db
     :put-hash (encode-state db)}))

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
