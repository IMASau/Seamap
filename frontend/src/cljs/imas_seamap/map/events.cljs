;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [encode-state ids->layers]]
            [imas-seamap.map.utils :refer [applicable-layers layer-name bounds->str region-stats-habitat-layer wgs84->epsg3112 feature-info-html feature-info-json get-layers-info-format group-basemap-layers feature-info-none]]
            [ajax.core :as ajax]
            [imas-seamap.blueprint :as b]
            [reagent.core :as r]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


;;; Seamap is hosted under https, meaning the browser will block ajax
;;; (ie, getfeatureinfo) requests to plain http URLs.  Servers still
;;; using http need specil handling:
(defn- is-insecure? [url] (-> url string/lower-case (string/starts-with? "http:")))

(defn base-layer-changed [{:keys [db]} [_ layer-name]]
  (let [grouped-base-layers (-> db :map :grouped-base-layers)
        selected-base-layer (first (filter (comp #(= layer-name %) :name) grouped-base-layers))]
    (when selected-base-layer
      (let [db (assoc-in db [:map :active-base-layer] selected-base-layer)]
        {:db db
         :put-hash (encode-state db)}))))

(defn bounds-for-zoom
  "GetFeatureInfo requires the pixel coordinates and dimensions around a
  geographic point, to translate a click into a feature. The
  convenient option of using the map viewport for both, as provided by
  leaflet, can cause inaccuracies when zoomed out. So, we calculate a
  smaller region by using the viewport dimensions to approximate a
  narrower pixel region."
  [{:keys [lat lng] :as _point}
                     {:keys [x y] :as _map-size}
                     {:keys [north south east west] :as _map-bounds}
                     {:keys [width height] :as _img-size}]
  (let [x-scale (/ (Math/abs (- west east)) x)
        y-scale (/ (Math/abs (- north south)) y)
        img-x-bounds (* x-scale width)
        img-y-bounds (* y-scale height)]
    {:north (+ lat (/ img-y-bounds 2))
     :south (- lat (/ img-y-bounds 2))
     :east (+ lng (/ img-x-bounds 2))
     :west (- lng (/ img-x-bounds 2))}))

(defn get-feature-info-request
  [info-format request-id by-server img-size img-bounds point]
  (let [layers->str   #(->> % (map layer-name) reverse (string/join ","))]
   ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
    (for [[server-url active-layers] by-server
          :let [layers   (layers->str active-layers)]]
      (let [params {:REQUEST       "GetFeatureInfo"
                    :LAYERS        layers
                    :QUERY_LAYERS  layers
                    :WIDTH         (:width img-size)
                    :HEIGHT        (:height img-size)
                    :BBOX          (bounds->str img-bounds)
                    :FEATURE_COUNT 10
                    :STYLES        ""
                    :X             50
                    :Y             50
                    :TRANSPARENT   true
                    :CRS           "EPSG:4326"
                    :SRS           "EPSG:4326"
                    :FORMAT        "image/png"
                    :INFO_FORMAT   info-format
                    :SERVICE       "WMS"
                    :VERSION       "1.1.1"}]
        {:method          :get
         :uri             server-url
         :params          params
         :response-format (ajax/text-response-format)
         :on-success      [:map/got-featureinfo request-id point info-format]
         :on-failure      [:map/got-featureinfo-err request-id point]}))))

(defn get-feature-info [{:keys [db]} [_ {:keys [size bounds]} point]]
  (let [{:keys [hidden-layers active-layers]} (:map db)
        visible-layers (remove #((set hidden-layers) %) active-layers)
        secure-layers  (remove #(is-insecure? (:server_url %)) visible-layers)
        by-server      (group-by :server_url secure-layers)
        ;; Note, we don't use the entire viewport for the pixel bounds because of inaccuracies when zoomed out.
        img-size       {:width 101 :height 101}
        img-bounds     (bounds-for-zoom point size bounds img-size)
        ;; Note, top layer, last in the list, must be first in our search string:
        request-id     (gensym)
        had-insecure?  (some #(is-insecure? (:server_url %)) visible-layers)
        info-format    (get-layers-info-format secure-layers)
        db             (if had-insecure?
                         {:db (assoc db :feature {:status :feature-info/none-queryable :location point})} ;; This is the fall-through case for "layers are visible, but they're http so we can't query them":
                         {:db ;; Initialise marshalling-pen of data: how many in flight, and current best-priority response
                          (assoc
                           db
                           :feature-query
                           {:request-id        request-id
                            :response-remain   (count by-server)
                            :had-insecure?     had-insecure?
                            :responses         []}
                           :feature
                           {:status   :feature-info/waiting
                            :location point})})]
    (if had-insecure?
      db
      (if info-format
        (assoc db :http-xhrio (get-feature-info-request info-format request-id by-server img-size img-bounds point))
        (assoc db :dispatch [:map/got-featureinfo request-id point nil nil])))))

;; Unused - related to getting boundary and habitat region stats
#_(defn get-habitat-region-statistics [{:keys [db]} [_ _ point]]
  (let [boundary   (->> db :map :active-layers first :id)
        habitat    (region-stats-habitat-layer db)
        [x y]      (wgs84->epsg3112 ((juxt :lng :lat) point))
        request-id (gensym)]
    (when (and boundary habitat)
      {:http-xhrio {:method          :get
                    :uri             (get-in db [:config :region-stats-url])
                    :params          {:boundary boundary
                                      :habitat  (:id habitat)
                                      :x        x
                                      :y        y}
                    :response-format (ajax/text-response-format)
                    :on-success      [:map/got-featureinfo request-id point nil]
                    :on-failure      [:map/got-featureinfo-err request-id point]}
       :db         (assoc db :feature-query {:request-id        request-id
                                             :response-remain   1
                                             :responses         []}
                          :feature       {:status   :feature-info/waiting
                                          :location point})})))

(defn map-click-dispatcher
  "Jumping-off point for when we get a map-click event.  Normally we
  just want to issue a (or multiple) getFeatureInfo requests, but if
  we're in calculating-region-statistics mode we want to issue a
  different request, and it's cleaner to handle those separately."
  [{:keys [db] :as ctx} event-v]
  (let [{:keys [hidden-layers active-layers]} (:map db)]
    (cond
      (get-in db [:map :controls :ignore-click])
      {:dispatch [:map/toggle-ignore-click]}

      (:feature db) ; If we're clicking the map but there's a popup open, just close it
      {:dispatch [:map/popup-closed]}

      (not (or                                                         ; Only invoke if:
            (get-in db [:map :controls :transect])                     ; we aren't drawing a transect;
            (get-in db [:map :controls :download :selecting])          ; we aren't selecting a region; and
            (empty? (remove #((set hidden-layers) %) active-layers)))) ; there are visible layers
      (let [ctx (assoc-in ctx [:db :feature :status] :feature-info/waiting)]
        (get-feature-info ctx event-v)))))

(defn toggle-ignore-click [db _]
  (update-in db [:map :controls :ignore-click] not))

(defn responses-feature-info [db point]
  (letfn [(response-to-info ; Converts a response and info format into readable information for the feature info popup
            [response]
            (if-let [{:keys [response info-format]} response] ; If we have a response; then
              (case info-format                               ; process the response into readable information; else
                "text/html"        (feature-info-html response)
                "application/json" (feature-info-json response)
                feature-info-none)
              {:status :feature-info/empty}))                 ; use an empty info status

          (combine-feature-info ; Combines two feature infos into one
            [{info-1 :info status-1 :status} {info-2 :info status-2 :status}]
            (if (seq (str info-1 info-2))         ; If we have any info from either response; then
              {:info (str info-1 info-2)}         ; combine the info from the responses and use that; else
              {:status (or status-1 status-2)}))] ; just use the status from either response
    
    (let [responses     (get-in db [:feature-query :responses])
          feature-infos (map response-to-info responses)
          feature-info  (reduce combine-feature-info {} feature-infos)
          had-insecure? (get-in db [:feature-query :had-insecure?])]
      (merge
       {:location point :had-insecure? had-insecure?}
       feature-info))))

(defn got-feature-info [db [_ request-id point info-format response]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db ; Ignore late responses to old clicks
    (let [db (-> db
                 (update-in [:feature-query :response-remain] dec)
                 (update-in [:feature-query :responses] conj {:response response :info-format info-format}))]

      (if (zero? (get-in db [:feature-query :response-remain]))
        (assoc db :feature (responses-feature-info db point)) ;; If this is the last response expected, update the displayed feature
        db))))

(defn got-feature-info-error [db [_ request-id point _]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db ; Ignore late responses to old clicks
    (let [db (-> db
                 (update-in [:feature-query :response-remain] dec)
                 (update-in [:feature-query :responses] conj nil))]
      
      (if (zero? (get-in db [:feature-query :response-remain]))
        (assoc db :feature (responses-feature-info db point)) ;; If this is the last response expected, update the displayed feature
        db))))  

(defn destroy-popup [db _]
  (assoc db :feature nil))

(defn map-set-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :layers] filter-text))

(defn map-set-others-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :other-layers] filter-text))

(defn layer-set-opacity [{:keys [db]} [_ layer opacity]]
  (s/assert (s/int-in 0 100) opacity)
  (let [db (assoc-in db [:layer-state :opacity layer] opacity)]
    {:db       db
     :put-hash (encode-state db)}))

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

(defn init-layer-opacities [layers opacity-maps]
  (->> layers
       (reduce (fn [acc lyr]
                 (if-let [o (get opacity-maps (:id lyr))]
                   (conj acc [lyr o])
                   acc))
               {})))

(defn update-base-layer-groups [db [_ groups]]
  (let [layers (get-in db [:map :base-layers])
        layer-groups (group-basemap-layers layers groups)]
    (-> db
        (assoc-in [:map :base-layer-groups] groups)
        (assoc-in [:map :grouped-base-layers] layer-groups)
        (assoc-in [:map :active-base-layer] (first layer-groups)))))

(defn update-base-layers [db [_ layers]]
  (let [groups (get-in db [:map :base-layer-groups])
        layer-groups (group-basemap-layers layers groups)]
    (-> db
        (assoc-in [:map :base-layers] layers)
        (assoc-in [:map :grouped-base-layers] layer-groups)
        (assoc-in [:map :active-base-layer] (first layer-groups)))))

(defn update-layers [{:keys [legend-ids opacity-ids] :as db} [_ layers]]
  (let [layers (process-layers layers)]
    (-> db
        (assoc-in [:map :layers] layers)
        (assoc-in [:layer-state :legend-shown] (init-layer-legend-status layers legend-ids))
        (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids)))))

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

(defn layer-tile-started-loading [db [_ layer]]
  (update-in db [:layer-state :tile-count layer] inc)) ; note, (inc nil) => 1

(defn layer-loading-error [db [_ layer]]
  (update-in db [:layer-state :error-count layer] inc)) ; note, (inc nil) => 1

(defn layer-finished-loading [db [_ layer]]
  (update-in db [:layer-state :loading-state] assoc layer :map.layer/loaded))

(defn- toggle-layer-logic
  "Encompases all the special-case logic in toggling active layers.
  Returns the new active layers as a vector."
  [layer active-layers]
  (if ((set active-layers) layer)
    (filterv #(not= % layer) active-layers)
    (conj active-layers layer)))

(defn toggle-layer [{:keys [db]} [_ layer]]
  (let [db (update-in db [:map :active-layers] (partial toggle-layer-logic layer))
        db (update-in db [:map :hidden-layers] #(disj % layer))]
    {:db       db
     :put-hash (encode-state db)
     ;; If someone triggers this, we also switch to manual mode:
     :dispatch-n [[:map.layers.logic/manual]
                  [:map/popup-closed]]}))

(defn toggle-layer-visibility
  [{:keys [db]} [_ layer]]
  (let [hidden-layers (get-in db [:map :hidden-layers])
        hidden? (contains? hidden-layers layer)
        db (update-in db [:map :hidden-layers] #((if hidden? disj conj) % layer))]
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

(defn viewport-layers [{:keys [_west _south _east _north] :as bounds} layers]
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
  ;; re-set.  So we have this two step process.  Ditto :active-base /
  ;; :active-base-layer
  [{:keys [db]} _]
  (let [{:keys [active active-base _legend-ids logic]} (:map db)
        active-layers (if (= (:type logic) :map.layer-logic/manual)
                        (vec (ids->layers active (get-in db [:map :layers])))
                        (vec (applicable-layers db :category :habitat)))
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        active-base   (or active-base   ; If no base is set (eg no existing hash-state), use the first candidate
                          (first (get-in db [:map :grouped-base-layers])))
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base)
                          (assoc :initialised true))]
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
  ;; Race-condition warning: this is called when the map changes to
  ;; keep state synchronised, but the map can start generating events
  ;; before the rest of the app is ready... avoid this by flagging
  ;; initialised state:
  (when (:initialised db)
    {:db       (-> db
                   (update-in [:map] assoc
                              :zoom zoom
                              :center center
                              :bounds bounds)
                   update-active-layers)
     :put-hash (encode-state db)}))

(defn map-start-selecting [db _]
  (-> db
      (assoc-in [:display :left-drawer] false)
      (assoc-in [:map :controls :ignore-click] true)
      (assoc-in [:map :controls :download :selecting] true)))

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
