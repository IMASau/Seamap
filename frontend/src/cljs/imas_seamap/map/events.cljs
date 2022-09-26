;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [ids->layers first-where index-of append-query-params]]
            [imas-seamap.map.utils :refer [layer-name bounds->str wgs84->epsg3112 feature-info-response->display bounds->projected region-stats-habitat-layer sort-by-sort-key map->bounds leaflet-props mouseevent->coords init-layer-legend-status init-layer-opacities]]
            [ajax.core :as ajax]
            [imas-seamap.blueprint :as b]
            [reagent.core :as r]
            [imas-seamap.interop.leaflet :as leaflet]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


;;; Seamap is hosted under https, meaning the browser will block ajax
;;; (ie, getfeatureinfo) requests to plain http URLs.  Servers still
;;; using http need specil handling:
(defn- is-insecure? [url] (-> url string/lower-case (string/starts-with? "http:")))

(defn base-layer-changed [{:keys [db]} [_ layer-name]]
  (let [grouped-base-layers (-> db :map :grouped-base-layers)
        selected-base-layer (first-where (comp #(= layer-name %) :name) grouped-base-layers)]
    (when selected-base-layer
      (let [db (assoc-in db [:map :active-base-layer] selected-base-layer)]
        {:db       db
         :dispatch [:maybe-autosave]}))))

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
     :east  (+ lng (/ img-x-bounds 2))
     :west  (- lng (/ img-x-bounds 2))}))

(defmulti get-feature-info #(second %2))

(def feature-info-image-size
  {:width 101 :height 101})

(defmethod get-feature-info 1
  [_ [_ _info-format-type layers request-id {:keys [size bounds] :as _leaflet-props} point]]
  (let [bbox (->> (bounds-for-zoom point size bounds feature-info-image-size)
                  (bounds->projected wgs84->epsg3112)
                  (bounds->str 3112))
        layer-names (->> layers (map layer-name) reverse (string/join ","))]
    {:http-xhrio
     ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
     {:method          :get
      :uri             (-> layers first :server_url)
      :params
      {:REQUEST       "GetFeatureInfo"
       :LAYERS        layer-names
       :QUERY_LAYERS  layer-names
       :WIDTH         (:width feature-info-image-size)
       :HEIGHT        (:height feature-info-image-size)
       :BBOX          bbox
       :FEATURE_COUNT 1000
       :STYLES        ""
       :X             50
       :Y             50
       :TRANSPARENT   true
       :CRS           "EPSG:3112"
       :SRS           "EPSG:3112"
       :FORMAT        "image/png"
       :INFO_FORMAT   "text/html"
       :SERVICE       "WMS"
       :VERSION       "1.1.1"}
      :response-format (ajax/text-response-format)
      :on-success      [:map/got-featureinfo request-id point "text/html" layers]
      :on-failure      [:map/got-featureinfo-err request-id point]}}))

(defmethod get-feature-info 2
  [_ [_ _info-format-type layers request-id {:keys [size bounds] :as _leaflet-props} point]]
  (let [bbox (->> (bounds-for-zoom point size bounds feature-info-image-size)
                  (bounds->projected wgs84->epsg3112)
                  (bounds->str 3112))
        layer-names (->> layers (map layer-name) reverse (string/join ","))]
    {:http-xhrio
     ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
     {:method          :get
      :uri             (-> layers first :server_url)
      :params
      {:REQUEST       "GetFeatureInfo"
       :LAYERS        layer-names
       :QUERY_LAYERS  layer-names
       :WIDTH         (:width feature-info-image-size)
       :HEIGHT        (:height feature-info-image-size)
       :BBOX          bbox
       :FEATURE_COUNT 1000
       :STYLES        ""
       :X             50
       :Y             50
       :TRANSPARENT   true
       :CRS           "EPSG:3112"
       :SRS           "EPSG:3112"
       :FORMAT        "image/png"
       :INFO_FORMAT   "application/json"
       :SERVICE       "WMS"
       :VERSION       "1.1.1"}
      :response-format (ajax/json-response-format)
      :on-success      [:map/got-featureinfo request-id point "application/json" layers]
      :on-failure      [:map/got-featureinfo-err request-id point]}}))

(defmethod get-feature-info 4
  [_ [_ _info-format-type layers request-id _leaflet-props {:keys [lat lng] :as point}]]
  (let [query         (leaflet/esri-query {:url (-> layers first :server_url)})
        leaflet-point (leaflet/latlng. lat lng)]
    (.intersects query leaflet-point)
    (.run query (fn [error feature-collection _response]
                  (if error
                    (re-frame/dispatch [:map/got-featureinfo-err request-id point nil])
                    (re-frame/dispatch [:map/got-featureinfo request-id point "application/json" layers (js->clj feature-collection)]))))
    nil))

(defmethod get-feature-info :default
  [_ [_ _info-format-type layers request-id _leaflet-props point]]
  {:dispatch [:map/got-featureinfo request-id point nil nil layers]})

(defn feature-info-dispatcher [{:keys [db]} [_ leaflet-props point]]
  (let [{:keys [hidden-layers active-layers]} (:map db)
        visible-layers (remove #((set hidden-layers) %) active-layers)
        secure-layers  (remove #(is-insecure? (:server_url %)) visible-layers)
        per-request    (group-by (juxt :server_url :info_format_type) secure-layers)
        request-id     (gensym)
        requests       (reduce-kv
                        (fn [acc [_ info-format-type] layers]
                          (conj acc [:map/get-feature-info info-format-type layers request-id leaflet-props point]))
                        [] per-request)
        had-insecure?  (some #(is-insecure? (:server_url %)) visible-layers)
        db             (if had-insecure?
                         (assoc db :feature {:status :feature-info/none-queryable :location point :show? true}) ;; This is the fall-through case for "layers are visible, but they're http so we can't query them":
                         (assoc ;; Initialise marshalling-pen of data: how many in flight, and current best-priority response
                          db
                          :feature-query
                          {:request-id        request-id
                           :response-remain   (count requests)
                           :had-insecure?     had-insecure?
                           :responses         []}
                          :feature
                          {:status   :feature-info/waiting
                           :location point
                           :show?    false}))] 
    (merge
     {:db db
      :dispatch-later {:ms 300 :dispatch [:map.feature/show request-id]}}
     (if (and (seq requests) (not had-insecure?))
       {:dispatch-n requests}
       {:dispatch   [:map/got-featureinfo request-id point nil nil []]}))))

(defn get-habitat-region-statistics [{:keys [db]} [_ _ point]]
  (let [{:keys [hidden-layers active-layers]} (:map db)
        visible-layers (remove #((set hidden-layers) %) active-layers)
        boundary       (first-where #(= (:category %) :boundaries) visible-layers)
        habitat        (region-stats-habitat-layer db)
        [x y]          (wgs84->epsg3112 ((juxt :lng :lat) point))
        request-id     (gensym)]
    (when (and boundary habitat)
      {:http-xhrio {:method          :get
                    :uri             (get-in db [:config :region-stats-url])
                    :params          {:boundary (:id boundary)
                                      :habitat  (:id habitat)
                                      :x        x
                                      :y        y}
                    :response-format (ajax/text-response-format)
                    :on-success      [:map/got-featureinfo request-id point "text/html" []]
                    :on-failure      [:map/got-featureinfo-err request-id point]}
       :db         (assoc db :feature-query {:request-id        request-id
                                             :response-remain   1
                                             :responses         []}
                          :feature       {:status   :feature-info/waiting
                                          :location point
                                          :show?    false})})))

(defn show-popup [db [_ request-id]]
  (cond-> db
    (and (:feature db) (= (get-in db [:feature-query :request-id]) request-id))
    (assoc-in [:feature :show?] true)))

(defn map-click-dispatcher
  "Jumping-off point for when we get a map-click event.  Normally we
  just want to issue a (or multiple) getFeatureInfo requests, but if
  we're in calculating-region-statistics mode we want to issue a
  different request, and it's cleaner to handle those separately."
  [{:keys [db] :as ctx} event-v]
  (let [{:keys [hidden-layers active-layers]} (:map db)
        visible-layers (remove #((set hidden-layers) %) active-layers)]
    (cond
      (get-in db [:map :controls :ignore-click])
      {:dispatch [:map/toggle-ignore-click]}

      (:feature db) ; If we're clicking the map but there's a popup open, just close it
      {:dispatch [:map/popup-closed]}

      (and                                                     ; Only invoke if:
       (not (get-in db [:map :controls :transect]))            ; we aren't drawing a transect;
       (not (get-in db [:map :controls :download :selecting])) ; we aren't selecting a region; and
       (seq visible-layers))                                   ; there are visible layers
      (feature-info-dispatcher ctx event-v))))

(defn toggle-ignore-click [db _]
  (update-in db [:map :controls :ignore-click] not))

(defn responses-feature-info [db point]
  (let [responses     (->> (get-in db [:feature-query :responses])
                           (map feature-info-response->display)
                           (remove nil?)
                           vec)
        had-insecure? (get-in db [:feature-query :had-insecure?])]
    (when (seq responses)
      {:location point :had-insecure? had-insecure? :responses responses :show? true})))

(defn got-feature-info [db [_ request-id point info-format layers response]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db ; Ignore late responses to old clicks
    (let [db (-> db
                 (update-in [:feature-query :response-remain] dec)
                 (update-in [:feature-query :responses] conj {:response response :info-format info-format :layers layers}))]

      (if-not (pos? (get-in db [:feature-query :response-remain]))
        (assoc db :feature (responses-feature-info db point)) ;; If this is the last response expected, update the displayed feature
        db))))

(defn got-feature-info-error [db [_ request-id point _]]
  (if (not= request-id (get-in db [:feature-query :request-id]))
    db ; Ignore late responses to old clicks
    (let [db (-> db
                 (update-in [:feature-query :response-remain] dec)
                 (update-in [:feature-query :responses] conj nil))]
      
      (if-not (pos? (get-in db [:feature-query :response-remain]))
        (assoc db :feature (responses-feature-info db point)) ;; If this is the last response expected, update the displayed feature
        db))))  

(defn destroy-popup [{:keys [db]} _]
  {:db       (assoc db :feature nil)
   :put-hash ""})

(defn map-set-layer-filter [{:keys [db]} [_ filter-text]]
  (let [db (assoc-in db [:filters :layers] filter-text)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn map-set-others-layer-filter [db [_ filter-text]]
  (assoc-in db [:filters :other-layers] filter-text))

(defn layer-set-opacity [{:keys [db]} [_ layer opacity]]
  (s/assert (s/int-in 0 100) opacity)
  (let [db (assoc-in db [:layer-state :opacity layer] opacity)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn process-layer [layer]
  (-> layer
      (update :category    (comp keyword string/lower-case))
      (update :server_type (comp keyword string/lower-case))
      (update :layer_type  (comp keyword string/lower-case))))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn update-grouped-base-layers [{{layers :base-layers groups :base-layer-groups} :map :as db}]
  (if (and (seq layers) (seq groups))
   (let [grouped-layers (group-by :layer_group layers)
        groups (map
                (fn [{:keys [id] :as group}]
                  (let [layers (get grouped-layers id)
                        layers (sort-by-sort-key layers)]
                    (merge
                     (first layers)
                     group
                     {:id     (:id (first layers))
                      :layers (drop 1 layers)})))
                groups)
        ungrouped-layers (get grouped-layers nil)
        ungrouped-groups (map #(assoc % :layers []) ungrouped-layers)
        groups (concat groups ungrouped-groups)
        groups (filter :server_url groups)  ;; removes empty groups
        groups (sort-by-sort-key groups)]
    (-> db
        (assoc-in [:map :grouped-base-layers] (vec groups))
        (assoc-in [:map :active-base-layer] (first groups))))
    db))

(defn update-base-layer-groups [db [_ groups]]
  (let [db (assoc-in db [:map :base-layer-groups] groups)]
    (update-grouped-base-layers db)))

(defn update-base-layers [db [_ layers]]
  (let [layers (mapv #(update % :layer_type (comp keyword string/lower-case)) layers)
        db     (assoc-in db [:map :base-layers] layers)]
    (update-grouped-base-layers db)))

(defn- keyed-layers-join
  "Using layers and keyed-layers, replaces layer IDs in keyed-layers with layer
   objects. We only update keyed-layers if the db contains layers."
  [{{:keys [layers keyed-layers]} :map :as db}]
  (if (seq layers)
    (assoc-in
     db [:map :keyed-layers]
     (reduce-kv
      (fn [m k v] (assoc m k (vec (ids->layers v layers))))
      {} keyed-layers))
    db))

(defn update-layers [{:keys [db]} [_ layers]]
  (let [{:keys [legend-ids opacity-ids]} db
        layers (process-layers layers)
        db     (-> db
                   (assoc-in [:map :layers] layers)
                   (assoc-in [:layer-state :legend-shown] (init-layer-legend-status layers legend-ids))
                   (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids))
                   keyed-layers-join)]
    {:db         db
     :dispatch-n (mapv #(vector :map.layer/get-legend %) (init-layer-legend-status layers legend-ids))}))

(defn- ->sort-map [ms]
  ;; Associate a category of objects (categories, organisations) with
  ;; a tuple of its sort-key (user-assigned, to allow user-specified
  ;; ordering) and its id (which is used as a stable id)
  (reduce
   (fn [acc {:keys [id name sort_key display_name]}]
     (assoc acc name [(or sort_key "zzzzzzzzzz") id (or display_name name)]))
   {} ms))

(defn update-organisations [db [_ organisations]]
  (-> db
      (assoc-in [:map :organisations] organisations)
      (assoc-in [:sorting :organisation] (->sort-map organisations))))

(defn update-classifications [db [_ classifications]]
  (assoc-in db [:sorting :data_classification] (->sort-map classifications)))

(defn update-descriptors [db [_ descriptors]]
  (let [titles  (reduce (fn [acc {:keys [name title]}]  (assoc acc name title))  {} descriptors)
        colours (reduce (fn [acc {:keys [name colour]}] (assoc acc name colour)) {} descriptors)]
    (assoc db
           :habitat-titles  titles
           :habitat-colours colours)))

(defn update-categories [db [_ categories]]
  (let [categories           (mapv #(update % :name (comp keyword string/lower-case)) categories)
        categories           (sort-by-sort-key categories)]
    (-> db
        (assoc-in [:map :categories] categories)
        (assoc-in [:sorting :category] (->sort-map categories)))))

(defn update-keyed-layers [db [_ keyed-layers]]
  (let [keyed-layers (map #(update % :keyword (comp keyword string/lower-case)) keyed-layers) ; keywordize
        keyed-layers (group-by :keyword keyed-layers) ; to map
        keyed-layers (reduce-kv
                      (fn [m k v] (assoc m k (mapv :layer v)))
                      {} keyed-layers) ; remove junk, isolate layer IDs
        db           (assoc-in db [:map :keyed-layers] keyed-layers)]
    (keyed-layers-join db)))

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
  (let [{:keys [habitat bathymetry habitat-obs]} (get-in db [:map :keyed-layers])
        db (-> db
               (update-in [:map :active-layers] (partial toggle-layer-logic layer))
               (update-in [:map :hidden-layers] #(disj % layer))
               (cond->
                ((set habitat) layer)
                 (assoc-in [:state-of-knowledge :statistics :habitat :show-layers?] false)

                 ((set bathymetry) layer)
                 (assoc-in [:state-of-knowledge :statistics :bathymetry :show-layers?] false)

                 ((set habitat-obs) layer)
                 (assoc-in [:state-of-knowledge :statistics :habitat-observations :show-layers?] false)))]
    {:db         db
     :dispatch-n [[:map/popup-closed]
                  [:maybe-autosave]]}))

(defn toggle-layer-visibility
  [{:keys [db]} [_ layer]]
  (let [hidden-layers (get-in db [:map :hidden-layers])
        hidden? (contains? hidden-layers layer)
        db (update-in db [:map :hidden-layers] #((if hidden? disj conj) % layer))]
    {:db         db
     :dispatch-n [[:map/popup-closed]
                  [:maybe-autosave]]}))

(defn toggle-legend-display [{:keys [db]} [_ {:keys [id] :as layer}]]
  (let [db (update-in db [:layer-state :legend-shown] #(if ((set %) layer) (disj % layer) (conj (set %) layer)))]
    {:db         db
     :dispatch-n [[:maybe-autosave]
                  (when-not (get-in db [:map :legends id]) [:map.layer/get-legend layer])]})) ; Retrieve layer legend data for display if we don't already have it or aren't already retrieving it

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [{:keys [db]} [_ {:keys [bounding_box] :as layer}]]
  (let [already-active? (some #{layer} (-> db :map :active-layers))
        db              (cond-> db
                          (not already-active?) (update-in [:map :active-layers] (partial toggle-layer-logic layer)))]
    {:db         db
     :dispatch-n [[:map/update-map-view {:bounds bounding_box}]
                  [:maybe-autosave]]}))

(defn region-stats-select-habitat [db [_ layer]]
  (assoc-in db [:region-stats :habitat-layer] layer))

(defn map-zoom-in [{:keys [db]} _]
  {:dispatch [:map/update-map-view {:zoom (inc (get-in db [:map :zoom]))}]})

(defn map-zoom-out [{:keys [db]} _]
  {:dispatch [:map/update-map-view {:zoom (dec (get-in db [:map :zoom]))}]})

(defn map-pan-direction [{:keys [db]} [_ direction]]
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
    {:dispatch [:map/update-map-view {:center (shift-centre (get-in db [:map :center]))}]}))

(defn show-initial-layers
  "Figure out the highest priority layer, and display it"
  ;; Slight hack; note we use :active not :active-layers, because
  ;; during boot we may have loaded hash-state, but we can't hydrate
  ;; the id from the hash state into actual layers, until the layers
  ;; themselves are loaded... by which time the state will have been
  ;; re-set.  So we have this two step process.  Ditto :active-base /
  ;; :active-base-layer
  [{:keys [db]} _]
  (let [{:keys [active active-base _legend-ids]} (:map db)
        active-layers (if active
                        (vec (ids->layers active (get-in db [:map :layers])))
                        (get-in db [:map :keyed-layers :startup] []))
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        active-base   (or active-base   ; If no base is set (eg no existing hash-state), use the first candidate
                          (first (get-in db [:map :grouped-base-layers])))
        story-maps    (get-in db [:story-maps :featured-maps])
        featured-map  (get-in db [:story-maps :featured-map])
        featured-map  (first-where #(= (% :id) featured-map) story-maps)
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base)
                          (assoc-in [:story-maps :featured-map] featured-map)
                          (assoc :initialised true))]
    {:db         db
     :dispatch-n [[:ui/hide-loading]
                  [:maybe-autosave]]}))

(defn update-leaflet-map [db [_ leaflet-map]]
  (when (not= leaflet-map (get-in db [:map :leaflet-map]))
    (.on leaflet-map "zoomend"            #(re-frame/dispatch [:map/view-updated (leaflet-props %)]))
    (.on leaflet-map "moveend"            #(re-frame/dispatch [:map/view-updated (leaflet-props %)]))
    (.on leaflet-map "baselayerchange"    #(re-frame/dispatch [:map/base-layer-changed (.-name %)]))
    (.on leaflet-map "click"              #(re-frame/dispatch [:map/clicked (leaflet-props %) (mouseevent->coords %)]))
    (.on leaflet-map "popupclose"         #(when-not (-> % .-popup .-options .-className (= "waiting"))  ;; Only dispatch :map/popup-closed if we're not closing a waiting popup (fixes ISA-269, caused by switching out waiting popup with info popup triggers popup closed)
                                             (re-frame/dispatch [:map/popup-closed])))
    (.on leaflet-map "mousemove"          #(re-frame/dispatch [:ui/mouse-pos {:x (-> % .-containerPoint .-x) :y (-> % .-containerPoint .-y)}]))
    (.on leaflet-map "mouseout"           #(re-frame/dispatch [:ui/mouse-pos nil]))
    (.on leaflet-map "easyPrint-start"    #(re-frame/dispatch [:ui/show-loading "Preparing Image..."]))
    (.on leaflet-map "easyPrint-finished" #(re-frame/dispatch [:ui/hide-loading]))

    (assoc-in db [:map :leaflet-map] leaflet-map)))

(defn update-map-view [{{:keys [leaflet-map] old-zoom :zoom old-center :center} :map} [_ {:keys [zoom center bounds instant?]}]] 
  (when leaflet-map
    (if instant?
      (do
        (when (or zoom (seq center)) (.setView leaflet-map (clj->js (or center old-center)) (or zoom old-zoom)))
        (when (seq bounds) (.fitBounds leaflet-map (-> bounds map->bounds clj->js))))
      (do
        (when (or zoom (seq center)) (.flyTo leaflet-map (clj->js (if (seq center) center old-center)) (or zoom old-zoom)))
        (when (seq bounds) (.flyToBounds leaflet-map (-> bounds map->bounds clj->js))))))
  nil)

(defn map-view-updated [{:keys [db]} [_ {:keys [zoom size center bounds]}]]
  (let [db (update db :map assoc
                   :zoom   zoom
                   :size   size
                   :center center
                   :bounds bounds)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn map-start-selecting [{:keys [db]} _]
  {:db       (-> db
                 (assoc-in [:map :controls :ignore-click] true)
                 (assoc-in [:map :controls :download :selecting] true))
   :dispatch [:left-drawer/close]})

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

(defn add-layer
  "Adds a layer to the list of active layers.
   
   Params:
    - layer: Layer you wish to add to active layers
    - target-layer (optional): If supplied, the new layer will be added just beneath
      this layer in the active layers list."
  [{:keys [db]} [_ layer target-layer]]
  (let [active-layers (get-in db [:map :active-layers])
        db            (cond
                        
                        ((set active-layers) layer) ; if the layer is already active, do nothing
                        db

                        target-layer                ; else if a target layer was specified, insert the new layer beneath the target
                        (let [index         (first (index-of #(= % target-layer) active-layers))
                              below         (subvec active-layers 0 index)
                              above         (subvec active-layers index)
                              active-layers (vec (concat below [layer] above))]
                          (assoc-in db [:map :active-layers] active-layers))

                        :else                       ; else, add the layer to the end of the list
                        (update-in db [:map :active-layers] conj layer))]
    {:db         db
     :dispatch-n [[:map/popup-closed]
                  [:maybe-autosave]]}))

(defn remove-layer
  [{:keys [db]} [_ layer]]
  (let [layers (get-in db [:map :active-layers])
        layers (vec (remove #(= % layer) layers))
        {:keys [habitat bathymetry habitat-obs]} (get-in db [:map :keyed-layers])
        db     (->
                db
                (assoc-in [:map :active-layers] layers)
                (update-in [:map :hidden-layers] #(disj % layer))
                (cond->
                 ((set habitat) layer)
                  (assoc-in [:state-of-knowledge :statistics :habitat :show-layers?] false)

                  ((set bathymetry) layer)
                  (assoc-in [:state-of-knowledge :statistics :bathymetry :show-layers?] false)

                  ((set habitat-obs) layer)
                  (assoc-in [:state-of-knowledge :statistics :habitat-observations :show-layers?] false)))]
    {:db         db
     :dispatch-n [[:map/popup-closed]
                  [:maybe-autosave]]}))

(defn add-layer-from-omnibar
  [{:keys [db]} [_ layer]]
  {:db       (assoc-in db [:display :layers-search-omnibar] false)
   :dispatch-n [[:map/add-layer layer]
                [:left-drawer/open]
                [:ui.catalogue/select-tab "cat"]
                [:ui.catalogue/catalogue-add-nodes-to-layer layer "cat" [:category :data_classification]]
                [:map/pan-to-layer layer]]})

(defn update-preview-layer [db [_ preview-layer]]
  (assoc-in db [:map :preview-layer] preview-layer))


(defn toggle-viewport-only [{:keys [db]} _]
  (let [db (update-in db [:map :viewport-only?] not)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn pan-to-popup
  "Based on a known location and size of the popup, we can find out if it will be
   outside of the bounds of the map. Knowing this we can pan the map to fix this."
  [{{{{popup-x :x popup-y :y popup-lat :lat popup-lng :lng} :location} :feature
    {{map-width :x map-height :y} :size [map-lat map-lng] :center}
    :map} :db}
   [_ {popup-width :x popup-height :y :as _popup-dimensions}]]
  (let [map-x           (/ map-width 2)
        map-y           (/ map-height 2)
        overflow-top    (- popup-height popup-y) ; positive values mean we're over the bounds by that many pixels (which tells us how many pixels we'd need to move the map in the opposite direction to have everything in-bounds)
        overflow-bottom (- popup-y map-height)
        overflow-left   (- (/ popup-width 2) popup-x)
        overflow-right  (- popup-x (- map-width (/ popup-width 2)))
        x-to-lng        (/ (- popup-lng map-lng) (- popup-x map-x)) ; ratio of lng degrees per x pixel
        y-to-lat        (/ (- popup-lat map-lat) (- popup-y map-y)) ; ratio of lat degrees per y pixel
        map-lat         (cond-> map-lat
                          (pos? overflow-top) (- (* overflow-top y-to-lat))
                          (pos? overflow-bottom) (+ (* overflow-bottom y-to-lat)))
        map-lng         (cond-> map-lng
                          (pos? overflow-left) (- (* overflow-left x-to-lng))
                          (pos? overflow-right) (+ (* overflow-right x-to-lng)))]
    {:dispatch [:map/update-map-view {:center [map-lat map-lng]}]}))

(defmulti get-layer-legend #(get-in %2 [1 :layer_type]))

(defmethod get-layer-legend :wms
  [{:keys [db]} [_ {:keys [id server_url layer_name] :as layer}]]
  {:db         (assoc-in db [:map :legends id] :map.legend/loading)
   :http-xhrio {:method          :get
                :uri             server_url
                :params          {:REQUEST     "GetLegendGraphic"
                                  :LAYER       layer_name
                                  :TRANSPARENT true
                                  :SERVICE     "WMS"
                                  :VERSION     "1.1.1"
                                  :FORMAT      "application/json"}
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:map.layer/get-legend-success layer]
                :on-failure      [:map.layer/get-legend-error layer]}})

(defmethod get-layer-legend :feature
  [{:keys [db]} [_ {:keys [id server_url] :as layer}]]
  {:db         (assoc-in db [:map :legends id] :map.legend/loading)
   :http-xhrio {:method          :get
                :uri             server_url
                :params          {:f "json"}
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:map.layer/get-legend-success layer]
                :on-failure      [:map.layer/get-legend-error layer]}})

(defmethod get-layer-legend :default
  [{:keys [db]} [_ {:keys [id] :as _layer}]]
  {:db (assoc-in db [:map :legends id] :map.legend/unsupported-layer)})

(defmulti get-layer-legend-success #(get-in %2 [1 :layer_type]))

(defmulti wms-symbolizer->key #(-> % keys first))

(defmethod wms-symbolizer->key :Polygon
  [{{:keys [fill]} :Polygon :as _symbolizer}]
  {:background-color fill
   :height           "100%"
   :width            "100%"})

(defmethod wms-symbolizer->key :Point
  [{{:keys [graphics size]} :Point :as _symbolizer}]
  (let [{:keys [mark fill stroke stroke-width]} (first graphics)]
    (merge
     {:background-color fill
      :border           (str "solid " stroke-width "px " stroke)
      :width            (str size "px")
      :height           (str size "px")}
     (when (= mark "circle") {:border-radius "100%"}))))

(defmethod wms-symbolizer->key :default [] nil)

(defmethod get-layer-legend-success :wms
  [db [_ {:keys [id server_url layer_name] :as _layer} response]]
  (let [legend (if (-> response :Legend first :rules first :symbolizers first wms-symbolizer->key) ; Convert the symbolizer for the first key
                 (->> response :Legend first :rules                                                ; if it converts successfully, then we make a vector legend and convert to keys and labels
                      (mapv
                       (fn [{:keys [title filter symbolizers]}]
                         {:label  title
                          :filter filter
                          :style  (-> symbolizers first wms-symbolizer->key)})))
                 (append-query-params                                                              ; else we just use an image for the legend graphic
                  server_url
                  {:REQUEST     "GetLegendGraphic"
                   :LAYER       layer_name
                   :TRANSPARENT true
                   :SERVICE     "WMS"
                   :VERSION     "1.1.1"
                   :FORMAT      "image/png"}))]
    (assoc-in db [:map :legends id] legend)))

(defmethod get-layer-legend-success :feature
  [db [_ {:keys [id] :as _layer} response]]
  (letfn [(convert-color
            [[r g b a]]
            (str "rgba(" r "," g "," b "," a ")"))
          (convert-value-info
            [{:keys [label] :as value-info}]
            {:label   (or label name)
             :style
             {:background-color (-> value-info :symbol :color convert-color)
              :border           (str "solid 2px " (-> value-info :symbol :outline :color convert-color))
              :height           "100%"
              :width            "100%"}})]
    (let [render-info (get-in response [:drawingInfo :renderer])
          legend      (if (:uniqueValueInfos render-info)
                        (mapv convert-value-info (:uniqueValueInfos render-info))
                        (-> render-info convert-value-info vector))]
      (assoc-in db [:map :legends id] legend))))

(defn get-layer-legend-error [db [_ {:keys [id] :as _layer}]]
  (assoc-in db [:map :legends id] :map.legend/error))
