(ns imas-seamap.events
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            [goog.dom :as gdom]
            [imas-seamap.blueprint :as b]
            [imas-seamap.db :as db]
            [imas-seamap.map.utils :refer [applicable-layers bbox-intersects? habitat-layer?]]
            [oops.core :refer [gcall ocall]]
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
    {:when :seen-all-of? :events [:map/update-layers :map/update-groups :map/update-priorities :map/update-descriptors]
     :dispatch [:map/initialise-display]}
    {:when :seen? :events :ui/hide-loading
     :dispatch [:welcome-layer/open]
     :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn boot [{:keys [hash-state]} _]
  (let [initial-db (cond-> (update db/default-db :map merge hash-state)
                     hash-state (assoc-in [:map :logic :type] :map.layer-logic/manual))]
    {:db         initial-db
     :async-flow (boot-flow)}))

;;; Reset state.  Gets a bit messy because we can't just return
;;; default-db without throwing away ajax-loaded layer info, so we
;;; restore that manually first.
(defn re-boot [{:keys [habitat-colours habitat-titles] {:keys [layers priorities groups] :as map-state} :map :as db} _]
  (-> db/default-db
      (update :map merge {:layers     layers
                          :groups     groups
                          :priorities priorities})
      (merge {:habitat-colours habitat-colours
              :habitat-titles  habitat-titles})))

(defn loading-screen [db _]
  (assoc db :loading true))

(defn application-loaded [db _]
  (assoc db :loading false))

(defn loading-failed [db _]
  (assoc db :load-error-msg "Something went wrong!"))

(defn initialise-db [_ _]
  {:db db/default-db
   :dispatch [:db-initialised]})

(defn initialise-layers [{:keys [db]} _]
  (let [{:keys [layer-url group-url priority-url descriptor-url]} (:config db)]
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
                   :on-failure      [:ajax/default-err-handler]}]}))

(defn help-layer-toggle [db _]
  (update-in db [:display :help-overlay] not))

(defn help-layer-open [db _]
  (assoc-in db [:display :help-overlay] true))

(defn help-layer-close [db _]
  (assoc-in db [:display :help-overlay] false))

(defn welcome-layer-open [db _]
  (assoc-in db [:display :welcome-overlay] true))

(defn welcome-layer-close [db _]
  (assoc-in db [:display :welcome-overlay] false))

(defn- geojson->linestring [geojson]
  ;; No assertions or anything for now (could make the geojson spec more explicit, but this will be fine)
  (get-in geojson [:geometry :coordinates]))

(def ^:private *epsg-3112*
  (gcall "proj4"
         "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(defn- wgs48->epsg3112 [pt]
  (js->clj
   (ocall *epsg-3112* :forward (clj->js pt))))

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
  (let [db (assoc db :transect {:query geojson
                                :show? true
                                :habitat :loading
                                :bathymetry :loading})
        linestring (geojson->linestring geojson)]
    {:db db
     :dispatch-n [[:transect.plot/show] ; A bit redundant since we set the :show key above
                  [:transect.query/habitat linestring]
                  [:transect.query/bathymetry linestring]]}))

(defn transect-query-error [{:keys [db]} [_ type {:keys [last-error failure] :as response}]]
  (let [status-text (if (= failure :timeout)
                      (str "Remote server timed out querying " (name type))
                      (str "Error querying " (name type) ": " last-error))]
    {:db       (assoc-in db [:transect type] status-text)
     :dispatch [:info/show-message status-text b/*intent-danger*]}))

(defn transect-query-habitat [{:keys [db]} [_ linestring]]
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
                                                   (map wgs48->epsg3112)
                                                   coords->linestring)}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:transect.query.habitat/success]
                    :on-failure      [:transect.query/failure :habitat]}}
      ;; No layers to query; reset habitat to no-results:
      {:db (assoc-in db [:transect :habitat] [])})))

(defn transect-query-habitat-success [db [_ response]]
  (assoc-in db [:transect :habitat] response))

(defn transect-query-bathymetry [{:keys [db]} [_ linestring]]
  (if-let [{:keys [server_url layer_name] :as bathy-layer}
           (first (applicable-layers db :category :bathymetry))]
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
                  :on-success      [:transect.query.bathymetry/success]
                  :on-failure      [:transect.query/failure :bathymetry]}}
    ;; Otherwise, don't bother with ajax; immediately return no results
    {:db (assoc-in db [:transect :bathymetry] [])}))

(defn transect-query-bathymetry-success [db [_ response]]
  (let [zipped      (->> response xml/parse-str zip/xml-zip)
        data-points (zx/xml1-> zipped :transect :transectData)
        num-points  (zx/xml1->  data-points (zx/attr :numPoints) js/parseInt)
        values      (zx/xml-> data-points :dataPoint :value zx/text js/parseFloat)
        increment   (/ 100 num-points)]
    (assoc-in db [:transect :bathymetry]
              (vec (map-indexed (fn [i v] [(* i increment) (if (js/isNaN v) nil (- v))]) values)))))

(defn transect-drawing-start [db _]
  (-> db
      (assoc-in [:map :controls :transect] true)
      (assoc-in [:transect :query] nil)))

(defn transect-drawing-finish [db _]
  (assoc-in db [:map :controls :transect] false))

(defn transect-drawing-clear [db _]
  (update-in db [:transect] merge {:query      nil
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

(defn show-message [db [_ message intent]]
  (assoc-in db [:info :message]
            {:message message
             :intent (or intent b/*intent-warning*)
             :__cache-buster (js/Date.now)}))

;;; A bit of a misnomer really; it cleans up the db, but won't
;;; override the timeout if a message is still displayed.
(defn clear-message [db _]
  (assoc-in db [:info :message] nil))

(defn sidebar-open [db [_ tabid]]
  (-> db
      (assoc-in [:display :sidebar :selected] tabid)
      ;; Allow the left tab to close as well as open:
      (update-in [:display :sidebar :collapsed] not)))

(defn sidebar-close [db _]
  (assoc-in db [:display :sidebar :collapsed] true))

(defn sidebar-toggle [db _]
  (update-in db [:display :sidebar :collapsed] not))

(defn focus-search [_ _]
  (.focus (gdom/getElement "layer-search")))
