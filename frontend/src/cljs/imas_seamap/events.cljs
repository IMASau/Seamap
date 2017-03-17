(ns imas-seamap.events
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [imas-seamap.db :as db]
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

(defn initialise-layers [db _]
  (let [layer-url (get-in db [:config :catalogue-url])]
    (re-frame/dispatch [:ajax layer-url
                        {:handler :map/update-layers}])
    db))

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

(defn transect-query [db [_ geojson]]
  ;; Reset the transect before querying (and paranoia to avoid
  ;; unlikely race conditions; do this before dispatching)
  (let [db (assoc db :transect {:query geojson
                                :show? true
                                :habitat :loading
                                :bathymetry :loading})
        linestring (geojson->linestring geojson)]
    (re-frame/dispatch [:transect.plot/show]) ; A bit redundant since we set the :show key above
    (re-frame/dispatch [:transect.query/habitat linestring])
    (re-frame/dispatch [:transect.query/bathymetry linestring])
    db))

(defn- transect-error-handler [type {:keys [status-text failure]}]
  (let [status-text (if (= failure :timeout)
                      (str "Remote server timed out querying " (name type))
                      status-text)]
   (re-frame/dispatch [:transect.query/failure type status-text])))

(defn transect-query-error [db [_ type text]]
  (assoc-in db [:transect type] text))

(defn transect-query-habitat [db [_ linestring]]
  ;; Mock for now, so it finishes
  (assoc-in db [:transect :habitat] []))

(defn transect-query-bathymetry [db [_ linestring]]
  (if-let [{:keys [server_url layer_name] :as bathy-layer} (get-in @(re-frame/subscribe [:map/layers]) [:groups :bathymetry 0])]
    (do
      (ajax/GET server_url
                {:params        {:REQUEST    "GetTransect"
                                 :LAYER      layer_name
                                 :LINESTRING linestring
                                 :CRS        "EPSG:4326"
                                 :format     "text/xml"
                                 :VERSION    "1.1.1"}
                 :handler       #(re-frame/dispatch [:transect.query.bathymetry/success %])
                 :error-handler (partial transect-error-handler :bathymetry)})
      db)
    ;; Otherwise, don't bother with ajax; immediately return no results
    (assoc-in db [:transect :bathymetry] [])))

(defn transect-query-bathymetry-success [db [_ response]]
  (assoc-in db [:transect :bathymetry] []))

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
