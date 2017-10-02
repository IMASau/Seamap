(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [re-frame.core :as re-frame]
            [imas-seamap.utils :refer [encode-state names->active-layers]]
            [imas-seamap.map.utils :refer [applicable-layers bounds->str]]
            [debux.cs.core :refer-macros [dbg]]
            [ajax.core :as ajax]))


(defn get-feature-info [{:keys [db] :as context} [_ {:keys [size bounds] :as props} {:keys [x y] :as point}]]
  ;; Only invoke if we aren't drawing a transect (ie, different click):
  (when-not (get-in db [:map :controls :transect])
    (let [active-layers (->> db :map :active-layers (remove #(#{:bathymetry} (:category %))))
          by-server     (group-by :server_url active-layers)
          ;; Note, top layer, last in the list, must be first in our search string:
          layers->str   #(->> % (map :layer_name) reverse (string/join ","))
          request-id    (gensym)]
      ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
      {:http-xhrio (for [[server-url active-layers] by-server
                         :let [layers   (layers->str active-layers)
                               priority (->> active-layers
                                             (map :category)
                                             (map {:imagery     0
                                                   :habitat     1
                                                   :third-party 2})
                                             (apply min))]]
                     (let [params {:REQUEST      "GetFeatureInfo"
                                   :LAYERS       layers
                                   :QUERY_layers layers
                                   :WIDTH        (:x size)
                                   :HEIGHT       (:y size)
                                   :BBOX         (bounds->str bounds)
                                   :X            x
                                   :Y            y
                                   :I            x
                                   :J            y
                                   :SRS          "EPSG:4326"
                                   :CRS          "EPSG:4326"
                                   :FORMAT       "text/html"
                                   :INFO_format  "text/html"
                                   :SERVICE      "WMS"
                                   :VERSION      "1.3.0"}]
                       {:method          :get
                        :uri             server-url
                        :params          params
                        :response-format (ajax/text-response-format)
                        :on-success      [:map/got-featureinfo request-id priority point]
                        :on-failure      [:map/got-featureinfo-err request-id]}))
       :dispatch   [:map/popup-closed]
       ;; Initialise marshalling-pen of data: how many in flight, and current best-priority response
       :db         (assoc db :feature-query {:request-id        request-id
                                             :response-remain   (count by-server)
                                             :response-priority 99
                                             :candidate         nil})})))

(defn got-feature-info [db [_ request-id priority point response]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db                           ; Ignore late responses to old clicks

    (let [zipped           (->> response xml/parse-str zip/xml-zip)
          body             (zx/xml1-> zipped :html :body)
          ;; always decrement the counter:
          db'              (update-in db [:feature-query :response-remain] dec)
          {:keys [response-priority response-remain candidate]} (:feature-query db')
          higher-priority? (< priority response-priority)
          candidate'       {:location point
                            :info     (xml/emit-str (zip/node body))}]
      (if (-> body zx/text string/blank?)
        ;; empty response; otherwise ignore:
        db'
        (cond-> db'
          ;; If this response has a higher priority, update the response candidate
          higher-priority?
          (assoc-in [:feature-query :candidate] candidate')
          ;; If this is the last response expected, update the displayed feature
          (zero? response-remain)
          (assoc :feature (if higher-priority? candidate' candidate)))))))

(defn got-feature-info-error [db [_ request_id _]]
  (update-in db [:feature-query :response-remain] dec))

(defn destroy-popup [db _]
  (assoc db :feature nil))

(defn map-set-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :layers] filter-text))

(defn map-set-others-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :other-layers] filter-text))

(defn process-layer [layer]
  (-> layer
      ;; TODO: convert the dates, etc too
      (update :category    (comp keyword string/lower-case))
      (update :server_type (comp keyword string/lower-case))))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn update-layers [db [_ layers]]
  (->> layers
       process-layers
       (assoc-in db [:map :layers])))

(defn update-groups [db [_ groups]]
  (assoc-in db [:map :groups] groups))

(defn update-organisations [db [_ organisations]]
  (assoc-in db [:map :organisations] organisations))

(defn update-priorities [db [_ priorities]]
  (assoc-in db [:map :priorities] priorities))

(defn update-descriptors [db [_ descriptors]]
  (let [titles  (reduce (fn [acc {:keys [name title]}]  (assoc acc name title))  {} descriptors)
        colours (reduce (fn [acc {:keys [name colour]}] (assoc acc name colour)) {} descriptors)]
    (assoc db
           :habitat-titles  titles
           :habitat-colours colours)))

(defn layer-started-loading [db [_ layer]]
  (update-in db [:layer-state] assoc layer [:map.layer/loading false]))

(defn layer-loading-error [db [_ layer]]
  (update-in db [:layer-state] assoc-in [layer 1] true))

(defn layer-finished-loading [db [_ layer]]
  (update-in db [:layer-state] assoc-in [layer 0] :map.layer/loaded))

(defn toggle-layer [{:keys [db]} [_ layer]]
  (let [db (update-in db [:map :active-layers]
                      #(if (some #{layer} %)
                         (filterv (fn [l] (not= l layer)) %)
                         (if (= :bathymetry (:category layer))
                           [layer] ; if we turn on bathymetry, hide everything else (SM-58)
                           (conj % layer))))]
    {:db       db
     :put-hash (encode-state db)
     ;; If someone triggers this, we also switch to manual mode:
     :dispatch [:map.layers.logic/manual]}))

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [{:keys [db]} [_ {:keys [bounding_box] :as layer}]]
  (let [already-active? (some #{layer} (-> db :map :active-layers))]
    (merge
     {:db       (cond-> db
                  true                  (assoc-in [:map :bounds] bounding_box)
                  (not already-active?) (update-in [:map :active-layers] conj layer))
      :put-hash (encode-state db)}
     ;; If someone triggers this, we also switch to manual mode:
     (when-not already-active?
       {:dispatch [:map.layers.logic/manual]}))))

(defn map-zoom-in [db _]
  (update-in db [:map :zoom] inc))

(defn map-zoom-out [db _]
  (update-in db [:map :zoom] dec))

(defn map-pan-direction [db [_ direction]]
  (assert [(#{:left :right :up :down} direction)])
  (let [[x' y']                         [0.05 0.05]
        [horiz vert]                    (case direction
                                          :left  [x'     0]
                                          :right [(- x') 0]
                                          :up    [0      (- y')]
                                          :down  [0      y'])
        {:keys [north south east west]} (get-in db [:map :bounds])
        shift-centre                    (fn [[y x]]
                                          [(+ y (* vert  (- north south)))
                                           (+ x (* horiz (- east  west)))])]
    (update-in db [:map :center] shift-centre)))

(defn layer-visible? [{:keys [west south east north] :as bounds}
                      {:keys [bounding_box]          :as layer}]
  (not (or (> (:south bounding_box) north)
           (< (:north bounding_box) south)
           (> (:west  bounding_box) east)
           (< (:east  bounding_box) west))))

(defn visible-layers [{:keys [west south east north] :as bounds} layers]
  (filter (partial layer-visible? bounds) layers))

(defn update-active-layers
  "Utility to recalculate layers that are displayed when automatic
  layer-switching is in place.  When the viewport or zoom changes, we
  may need to switch out a layer for a coarser/finer resolution one.
  Only applies to habitat layers."
  [{{:keys [logic]} :map :as db}]
  ;; Basic idea:
  ;; * check that any habitat layer is currently displayed (ie, don't start with no habitats, then zoom in and suddenly display one!)
  ;; * filter out habitat layers from actives
  ;; * add back in those that are visible, and past the zoom cutoff
  ;; * assoc back onto the db
  (if (= (:type logic) :map.layer-logic/automatic)
    (assoc-in db [:map :active-layers]
              (vec (applicable-layers db :category :habitat)))
    db))

(defn show-initial-layers
  "Figure out the highest priority layer, and display it"
  ;; Slight hack; note we use :active not :active-layers, because
  ;; during boot we may have loaded hash-state, but we can't hydrate
  ;; the names from the hash state into actual layers, until the
  ;; layers themselves are loaded... by which time the state will have
  ;; been re-set.  So we have this two step process.
  [{:keys [db]} _]
  (let [{:keys [active logic]} (:map db)
        active-layers          (if (= (:type logic) :map.layer-logic/manual)
                                 (names->active-layers active (get-in db [:map :layers]))
                                 (vec (applicable-layers db :category :habitat)))
        db                     (assoc-in db [:map :active-layers] active-layers)]
    {:db       db
     :put-hash (encode-state db)
     :dispatch [:ui/hide-loading]}))

(defn map-layer-logic-manual [db [_ user-triggered]]
  (assoc-in db [:map :logic]
            {:type :map.layer-logic/manual
             :trigger (if user-triggered :map.logic.trigger/user :map.logic.trigger/automatic)}))

(defn map-layer-logic-automatic [db _]
  (assoc-in db [:map :logic] {:type :map.layer-logic/automatic :trigger :map.logic.trigger/automatic}))

(defn map-layer-logic-toggle [{:keys [db]} [_ user-triggered]]
  (let [type (if (= (get-in db [:map :logic :type])
                    :map.layer-logic/manual)
               :map.layer-logic/automatic
               :map.layer-logic/manual)
        trigger (if user-triggered :map.logic.trigger/user :map.logic.trigger/automatic)]
    (merge {:db (assoc-in db [:map :logic] {:type type :trigger trigger})
            :put-hash (encode-state db)}
           ;; Also reset displayed layers, if we're turning auto-mode back on:
           (when (= type :map.layer-logic/automatic)
             {:dispatch [:map/initialise-display]}))))

(defn map-view-updated [{:keys [db]} [_ {:keys [zoom center bounds]}]]
  {:db       (-> db
                 (update-in [:map] assoc
                            :zoom zoom
                            :center center
                            :bounds bounds)
                 update-active-layers)
   :put-hash (encode-state db)})
