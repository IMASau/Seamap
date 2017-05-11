(ns imas-seamap.events
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zip]
            [imas-seamap.blueprint :as b]
            [imas-seamap.db :as db]
            [oops.core :refer [gcall ocall]]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))

(defn not-yet-implemented
  "Register this handler against event symbols that don't have a
  handler yet"
  [db [sym & args :as event-v]]
  (js/console.warn "Warning: no handler for" sym "implemented yet")
  db)

;;; TODO: maybe pull in extra config inject as page config, but that
;;; may not be feasible with a wordpress host
(defn initialise-db [_ _] db/default-db)

(defn initialise-layers [{:keys [db]} _]
  (let [layer-url (get-in db [:config :catalogue-url])]
    {:db db
     :http-xhrio {:method :get
                  :uri layer-url
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:map/update-layers]
                  :on-failure [:ajax/default-err-handler]}}))

(defn help-layer-toggle [db _]
  (update-in db [:display :help-overlay] not))

(defn help-layer-open [db _]
  (assoc-in db [:display :help-overlay] true))

(defn help-layer-close [db _]
  (assoc-in db [:display :help-overlay] false))

(defn- geojson->linestring [geojson]
  ;; No assertions or anything for now (could make the geojson spec more explicit, but this will be fine)
  (let [coords (get-in geojson [:geometry :coordinates])]
    (->> coords
         (map (partial string/join " "))
         (string/join ","))))

(def ^:private *epsg-3112*
  (gcall "proj4"
         "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(defn- wgs48->epsg3112 [pt]
  (ocall *epsg-3112* :forward (clj->js pt)))

(defn- geojson->native-linestring [geojson]
  (let [coords (get-in geojson [:geometry :coordinates])]
    (->> coords
         (map wgs48->epsg3112)
         (map (partial string/join " "))
         (string/join ","))))

(defn transect-query [{:keys [db]} [_ geojson]]
  ;; Reset the transect before querying (and paranoia to avoid
  ;; unlikely race conditions; do this before dispatching)
  (let [db (assoc db :transect {:query geojson
                                :show? true
                                :habitat :loading
                                :bathymetry :loading})
        linestring (geojson->linestring geojson)
        native-linestring (geojson->native-linestring geojson)]
    {:db db
     :dispatch-n [[:transect.plot/show] ; A bit redundant since we set the :show key above
                  [:transect.query/habitat native-linestring]
                  [:transect.query/bathymetry linestring]]}))

(defn- transect-error-handler [type {:keys [status-text failure]}]
  (let [status-text (if (= failure :timeout)
                      (str "Remote server timed out querying " (name type))
                      status-text)]
   (re-frame/dispatch [:transect.query/failure type status-text])))

(defn transect-query-error [db [_ type text]]
  (assoc-in db [:transect type] text))

(defn transect-query-habitat [{:keys [db]} [_ linestring]]
  {:db db
   :http-xhrio {:method :get
                ;; FIXME: url and layers should be dynamically constructed
                :uri "http://localhost:8000/api/habitat/transect"
                :params {:layers "seamap:SeamapAus_NSW_ocean_ecosystems_2002"
                         :line linestring}
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [:transect.query.habitat/success]
                :on-failure [:transect.query/failure :habitat]}})

(defn transect-query-habitat-success [db [_ response]]
  (let [habitats (mapv (juxt :start_percentage :end_percentage :name) response)]
    (assoc-in db [:transect :habitat]
              habitats)))

(defn transect-query-bathymetry [{:keys [db]} [_ linestring]]
  (if-let [{:keys [server_url layer_name] :as bathy-layer}
           (get-in @(re-frame/subscribe [:map/layers]) [:groups :bathymetry 0])]
    {:db         db
     :http-xhrio {:method          :get
                  :uri             server_url
                  :params          {:REQUEST    "GetTransect"
                                    :LAYER      layer_name
                                    :LINESTRING linestring
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
