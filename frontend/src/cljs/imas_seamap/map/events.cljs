;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [encode-state ids->layers]]
            [imas-seamap.map.utils :refer [applicable-layers layer-name bounds->str region-stats-habitat-layer wgs84->epsg3112]]
            [ajax.core :as ajax]
            [imas-seamap.blueprint :as b]
            [reagent.core :as r]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


;;; Seamap is hosted under https, meaning the browser will block ajax
;;; (ie, getfeatureinfo) requests to plain http URLs.  Servers still
;;; using http need specil handling:
(defn- is-insecure? [url] (-> url string/lower-case (string/starts-with? "http:")))

(defn get-feature-info [{:keys [db] :as _context} [_ {:keys [size bounds] :as _props} {:keys [x y] :as point}]]
  (let [active-layers (->> db :map :active-layers (remove #(is-insecure? (:server_url %))) (remove #(#{:bathymetry} (:category %))))
        by-server     (group-by :server_url active-layers)
        ;; Note, top layer, last in the list, must be first in our search string:
        layers->str   #(->> % (map layer-name) reverse (string/join ","))
        request-id    (gensym)
        had-insecure? (->> db :map :active-layers (some #(is-insecure? (:server_url %))))]
    ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
    (cond
      (seq active-layers)
      {:http-xhrio (for [[server-url active-layers] by-server
                         :let [layers   (layers->str active-layers)
                               priority (->> active-layers
                                             (map :category)
                                             (map {:imagery     0
                                                   :habitat     1
                                                   :third-party 2})
                                             (apply min))]]
                     (let [params {:REQUEST       "GetFeatureInfo"
                                   :LAYERS        layers
                                   :QUERY_LAYERS  layers
                                   :WIDTH         (:x size)
                                   :HEIGHT        (:y size)
                                   :BBOX          (bounds->str bounds)
                                   :FEATURE_COUNT 5
                                   :STYLES        ""
                                   :X             x
                                   :Y             y
                                   :TRANSPARENT   true
                                   :CRS           "EPSG:4326"
                                   :SRS           "EPSG:4326"
                                   :FORMAT        "image/png"
                                   :INFO_FORMAT   "text/html"
                                   :SERVICE       "WMS"
                                   :VERSION       "1.1.1"}]
                       {:method          :get
                        :uri             server-url
                        :params          params
                        :response-format (ajax/text-response-format)
                        :on-success      [:map/got-featureinfo request-id priority point]
                        :on-failure      [:map/got-featureinfo-err request-id priority]}))
       ;; Initialise marshalling-pen of data: how many in flight, and current best-priority response
       :db         (assoc db :feature-query {:request-id        request-id
                                             :response-remain   (count by-server)
                                             :response-priority 99
                                             :had-insecure?     had-insecure?
                                             :candidate         nil}
                          :feature       {:status   :feature-info/waiting
                                          :location point})}

      ;; This is the fall-through case for "layers are visible, but
      ;; they're http so we can't query them":
      had-insecure?
      {:db (assoc db :feature {:status :feature-info/none-queryable :location point})})))

(defn get-habitat-region-statistics [{:keys [db] :as _ctx} [_ _props point]]
  (let [boundary   (->> db :map :active-layers (filter #(= :boundaries (:category %))) first :id)
        habitat    (region-stats-habitat-layer db)
        [x y]      (wgs84->epsg3112 ((juxt :lng :lat) point))
        request-id (gensym)
        priority   0]
    (when (and boundary habitat)
      {:http-xhrio {:method          :get
                    :uri             (get-in db [:config :region-stats-url])
                    :params          {:boundary boundary
                                      :habitat  (:id habitat)
                                      :x        x
                                      :y        y}
                    :response-format (ajax/text-response-format)
                    :on-success      [:map/got-featureinfo request-id priority point]
                    :on-failure      [:map/got-featureinfo-err request-id priority]}
       :db         (assoc db :feature-query {:request-id        request-id
                                             :response-remain   1
                                             :response-priority 99
                                             :candidate         nil}
                          :feature       {:status   :feature-info/waiting
                                          :location point})})))

(defn map-click-dispatcher
  "Jumping-off point for when we get a map-click event.  Normally we
  just want to issue a (or multiple) getFeatureInfo requests, but if
  we're in calculating-region-statistics mode we want to issue a
  different request, and it's cleaner to handle those separately."
  [{:keys [db] :as ctx} [_ _props _point :as event-v]]
  (cond (:feature db) ; If we're clicking the map but there's a popup open, just close it
        {:dispatch [:map/popup-closed]}

        ;; Only invoke if we aren't drawing a transect (ie, different click):
        (not (or (get-in db [:map :controls :transect])
                 (get-in db [:map :controls :download :selecting])
                 ;; (also ignore click if there's no active layers to click on)
                 (empty? (get-in db [:map :active-layers]))))
        (let [ctx (assoc-in ctx [:db :feature :status] :feature-info/waiting)]
          (if (= "tab-management" (get-in db [:display :sidebar :selected]))
            (get-habitat-region-statistics ctx event-v)
            (get-feature-info ctx event-v)))))

(defn got-feature-info [db [_ request-id priority point response]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db                           ; Ignore late responses to old clicks

    (let [parsed (.parseFromString (js/DOMParser.) response "text/html")
          body  (first (array-seq (.querySelectorAll parsed "body")))
          db' (update-in db [:feature-query :response-remain] dec)
          {:keys [response-priority response-remain candidate had-insecure?]} (:feature-query db')
          higher-priority? (< priority response-priority)
          candidate' (merge {:location point :had-insecure? had-insecure?}
                            (if (.-firstElementChild body)
                              {:info (.-innerHTML body)}
                              {:status :feature-info/empty}))]
      (cond-> db'
          ;; If this response has a higher priority, update the response candidate
        higher-priority?
        (assoc-in [:feature-query :candidate] candidate')
          ;; If this is the last response expected, update the displayed feature
        (zero? response-remain)
        (assoc :feature (if higher-priority? candidate' candidate))))))

(defn got-feature-info-error [db [_ _request_id priority _]]
  (let [db (update-in db [:feature-query :response-remain] dec)
        {:keys [response-priority response-remain]} (:feature-query db)
        higher-priority? (< priority response-priority)]
    (if (and (zero? response-remain) higher-priority?)
      (assoc-in db [:feature :status] :feature-info/error)
      db)))

(defn destroy-popup [db _]
  (assoc db :feature nil))

(defn map-set-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :layers] filter-text))

(defn map-set-others-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :other-layers] filter-text))

(defn layer-set-opacity [db [_ layer opacity]]
  (s/assert (s/int-in 0 100) opacity)
  (assoc-in db [:layer-state :opacity layer] opacity))

(defn process-layer [layer]
  (-> layer
      (update :category    (comp keyword string/lower-case))
      ;; If category is contours, turn it into bathymetry but with a contours attribute set:
      (as-> lyr (if (= (:category lyr) :contours) (assoc lyr :contours true) lyr))
      (update :category    #(if (= % :contours) :bathymetry %))
      (update :server_type (comp keyword string/lower-case))))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn init-layer-legend-status [layers legend-ids]
  (let [legends (set legend-ids)]
    (->> layers
         (filter (comp legends :id))
         set)))

(defn update-layers [{:keys [legend-ids] :as db} [_ layers]]
  (let [layers (process-layers layers)]
    (-> db
        (assoc-in [:map :layers] layers)
        (assoc-in [:layer-state :legend-shown] (init-layer-legend-status layers legend-ids)))))

(defn update-groups [db [_ groups]]
  (assoc-in db [:map :groups] groups))

(defn- ->sort-map [ms]
  ;; Associate a category of objects (categories, organisations) with
  ;; a tuple of its sort-key (user-assigned, to allow user-specified
  ;; ordering) and its id (which is used as a stable id)
  (reduce (fn [acc {:keys [id name sort_key]}] (assoc acc name [(or sort_key "zzzzz") id])) {} ms))

(defn update-organisations [db [_ organisations]]
  (-> db
      (assoc-in [:map :organisations] organisations)
      (assoc-in [:sorting :organisation] (->sort-map organisations))))

(defn update-classifications [db [_ classifications]]
  (assoc-in db [:sorting :data_classification] (->sort-map classifications)))

(defn update-priorities [db [_ priorities]]
  (assoc-in db [:map :priorities] priorities))

(defn update-descriptors [db [_ descriptors]]
  (let [titles  (reduce (fn [acc {:keys [name title]}]  (assoc acc name title))  {} descriptors)
        colours (reduce (fn [acc {:keys [name colour]}] (assoc acc name colour)) {} descriptors)]
    (assoc db
           :habitat-titles  titles
           :habitat-colours colours)))

(defn layer-started-loading [db [_ layer]]
  (update-in db [:layer-state :loading-state] assoc layer :map.layer/loading))

(defn layer-loading-error [db [_ layer]]
  (update-in db [:layer-state :seen-errors] conj layer))

(defn layer-finished-loading [db [_ layer]]
  (update-in db [:layer-state :loading-state] assoc layer :map.layer/loaded))

(defn- toggle-layer-logic
  "Encompases all the special-case logic in toggling active layers.
  Returns the new active layers as a vector."
  [layer active-layers]
  (if (some #{layer} active-layers)
    (filterv (fn [l] (not= l layer)) active-layers)
    (let [active-layers  (filterv #(or (:contours %)
                                       (not= :bathymetry (:category %)))
                                  active-layers)]
      (case (:category layer)
        ;; if we turn on bathymetry, hide everything else (SM-58) --
        ;; unless it's a contour layer, in which case we can keep it,
        ;; but note still filter out other bathy layers:
        :bathymetry (if (:contours layer)
                      (conj active-layers layer)
                      [layer])
        ;; Only show one boundary-layer at a time:
        :boundaries (conj
                     (filterv #(not= :boundaries (:category %))
                              active-layers)
                     layer)
        ;; Default:
        (conj active-layers layer)))))

(defn toggle-layer [{:keys [db]} [_ layer]]
  (let [db (update-in db [:map :active-layers] (partial toggle-layer-logic layer))]
    {:db       db
     :put-hash (encode-state db)
     ;; If someone triggers this, we also switch to manual mode:
     :dispatch-n [[:map.layers.logic/manual]
                  [:map/popup-closed]]}))

(defn toggle-legend-display [{:keys [db]} [_ layer]]
  (let [db (update-in db [:layer-state :legend-shown] #(if ((set %) layer) (disj % layer) (conj (set %) layer)))]
    {:db       db
     :put-hash (encode-state db)}))

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [{:keys [db]} [_ {:keys [bounding_box] :as layer}]]
  (let [already-active? (some #{layer} (-> db :map :active-layers))]
    (merge
     {:db       (cond-> db
                  true                  (assoc-in [:map :bounds] bounding_box)
                  (not already-active?) (update-in [:map :active-layers] (partial toggle-layer-logic layer)))
      :put-hash (encode-state db)}
     ;; If someone triggers this, we also switch to manual mode:
     (when-not already-active?
       {:dispatch [:map.layers.logic/manual]}))))

(defn region-stats-select-habitat [db [_ layer]]
  (assoc-in db [:region-stats :habitat-layer] layer))

(defn map-zoom-in [db _]
  (update-in db [:map :zoom] inc))

(defn map-zoom-out [db _]
  (update-in db [:map :zoom] dec))

(defn map-pan-direction [db [_ direction]]
  (assert [(#{:left :right :up :down} direction)])
  (let [[x' y']                         [0.05 0.05]
        [horiz vert]                    (case direction
                                          :left  [(- x') 0]
                                          :right [x'     0]
                                          :up    [0      y']
                                          :down  [0      (- y')])
        {:keys [north south east west]} (get-in db [:map :bounds])
        shift-centre                    (fn [[y x]]
                                          [(+ y (* vert  (- north south)))
                                           (+ x (* horiz (- east  west)))])]
    (update-in db [:map :center] shift-centre)))

(defn layer-visible? [{:keys [west south east north] :as _bounds}
                      {:keys [bounding_box]          :as _layer}]
  (not (or (> (:south bounding_box) north)
           (< (:north bounding_box) south)
           (> (:west  bounding_box) east)
           (< (:east  bounding_box) west))))

(defn visible-layers [{:keys [_west _south _east _north] :as bounds} layers]
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
  ;; the id from the hash state into actual layers, until the layers
  ;; themselves are loaded... by which time the state will have been
  ;; re-set.  So we have this two step process.
  [{:keys [db]} _]
  (let [{:keys [active _legend-ids logic]} (:map db)
        active-layers          (if (= (:type logic) :map.layer-logic/manual)
                                 (vec (ids->layers active (get-in db [:map :layers])))
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
             {:dispatch-n [[:map/initialise-display]
                           [:map/popup-closed]]}))))

(defn map-view-updated [{:keys [db]} [_ {:keys [zoom center bounds]}]]
  {:db       (-> db
                 (update-in [:map] assoc
                            :zoom zoom
                            :center center
                            :bounds bounds)
                 update-active-layers)
   :put-hash (encode-state db)})

(defn map-start-selecting [db _]
  (assoc-in db [:map :controls :download :selecting] true))

(defn map-cancel-selecting [db _]
  (assoc-in db [:map :controls :download :selecting] false))

(defn map-clear-selection [db _]
  (update-in db [:map :controls :download] dissoc :bbox))

(defn map-finalise-selection [{:keys [db]} [_ bbox]]
  {:db      (update-in db [:map :controls :download] merge {:selecting false
                                                            :bbox      bbox})
   :message [(r/as-element
              [:div "Open layer info ("
               [b/icon {:icon "info-sign" :icon-size 14}]
               ") to download selection"])
             b/INTENT-NONE]})

(defn map-toggle-selecting [{:keys [db]} _]
  {:dispatch
   (cond
     (get-in db [:map :controls :download :selecting]) [:map.layer.selection/disable]
     (get-in db [:map :controls :download :bbox])      [:map.layer.selection/clear]
     :default                                          [:map.layer.selection/enable])})
