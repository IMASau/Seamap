;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :as re-frame]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [ids->layers first-where index-of append-query-params round-to-nearest map-server-url? feature-server-url?]]
            [imas-seamap.map.utils :refer [layer-name bounds->str feature-info-response->display bounds->projected region-stats-habitat-layer sort-by-sort-key map->bounds leaflet-props mouseevent->coords init-layer-legend-status init-layer-opacities visible-layers has-visible-habitat-layers? enhance-rich-layer rich-layer->displayed-layer layer->rich-layer layer->cql-filter project-coords layer->dynamic-pills ->dynamic-pill]]
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

(def ^:const INFO-FORMAT-HTML 1)
(def ^:const INFO-FORMAT-JSON 2)
(def ^:const INFO-FORMAT-NONE 3)
(def ^:const INFO-FORMAT-FEATURE 4)
(def ^:const INFO-FORMAT-XML 5)
(def ^:const INFO-FORMAT-MAP-SERVER 6)

(defmulti get-feature-info #(second %2))

(def feature-info-image-size
  {:width 101 :height 101})

(defmethod get-feature-info INFO-FORMAT-HTML
  [{:keys [db]} [_ _info-format-type layers request-id {:keys [size bounds] :as _leaflet-props} point]]
  (let [bbox (->> (bounds-for-zoom point size bounds feature-info-image-size)
                  (bounds->projected #(project-coords % (-> layers first :crs)))
                  (bounds->str (-> layers first :crs)))
        layer-names (->> layers (map layer-name) reverse (string/join ","))
        cql-filters (->> layers (map #(layer->cql-filter % db)) (filter identity))
        cql-filter (apply str (interpose ";" cql-filters))
        cql-filter (when (seq cql-filter) cql-filter)]
    {:http-xhrio
     ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
     {:method          :get
      :uri             (-> layers first :server_url)
      :params
      (merge
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
        :CRS           (-> layers first :crs)
        :SRS           (-> layers first :crs)
        :FORMAT        "image/png"
        :INFO_FORMAT   "text/html"
        :SERVICE       "WMS"
        :VERSION       "1.1.1"}
       (when cql-filter {:CQL_FILTER cql-filter}))
      :response-format (ajax/text-response-format)
      :on-success      [:map/got-featureinfo request-id point "text/html" layers]
      :on-failure      [:map/got-featureinfo-err request-id point]}}))

(defmethod get-feature-info INFO-FORMAT-JSON
  [{:keys [db]} [_ _info-format-type layers request-id {:keys [size bounds] :as _leaflet-props} point]]
  (let [bbox (->> (bounds-for-zoom point size bounds feature-info-image-size)
                  (bounds->projected #(project-coords % (-> layers first :crs)))
                  (bounds->str (-> layers first :crs)))
        layer-names (->> layers (map layer-name) reverse (string/join ","))
        cql-filters (->> layers (map #(layer->cql-filter % db)) (filter identity))
        cql-filter (apply str (interpose ";" cql-filters))
        cql-filter (when (seq cql-filter) cql-filter)]
    {:http-xhrio
     ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
     {:method          :get
      :uri             (-> layers first :server_url)
      :params
      (merge
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
        :CRS           (-> layers first :crs)
        :SRS           (-> layers first :crs)
        :FORMAT        "image/png"
        :INFO_FORMAT   "application/json"
        :SERVICE       "WMS"
        :VERSION       "1.1.1"}
       (when cql-filter {:CQL_FILTER cql-filter}))
      :response-format (ajax/json-response-format)
      :on-success      [:map/got-featureinfo request-id point "application/json" layers]
      :on-failure      [:map/got-featureinfo-err request-id point]}}))

(defmethod get-feature-info INFO-FORMAT-FEATURE
  [_ [_ _info-format-type layers request-id _leaflet-props {:keys [lat lng] :as point}]]
  (let [query         (leaflet/esri-query {:url (-> layers first :server_url)})
        leaflet-point (leaflet/latlng. lat lng)]
    (.intersects query leaflet-point)
    (.run query (fn [error feature-collection _response]
                  (if error
                    (re-frame/dispatch [:map/got-featureinfo-err request-id point nil])
                    (re-frame/dispatch [:map/got-featureinfo request-id point "application/json" layers (js->clj feature-collection)]))))
    nil))

(defmethod get-feature-info INFO-FORMAT-XML
  [{:keys [db]} [_ _info-format-type layers request-id {:keys [size bounds] :as _leaflet-props} point]]
  (let [bbox (->> (bounds-for-zoom point size bounds feature-info-image-size)
                  (bounds->projected #(project-coords % (-> layers first :crs)))
                  (bounds->str (-> layers first :crs)))
        layer-names (->> layers (map layer-name) reverse (string/join ","))
        cql-filters (->> layers (map #(layer->cql-filter % db)) (filter identity))
        cql-filter (apply str (interpose ";" cql-filters))
        cql-filter (when (seq cql-filter) cql-filter)]
    {:http-xhrio
     ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
     {:method          :get
      :uri             (-> layers first :server_url)
      :params
      (merge
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
        :CRS           (-> layers first :crs)
        :SRS           (-> layers first :crs)
        :FORMAT        "image/png"
        :INFO_FORMAT   "text/xml"
        :SERVICE       "WMS"
        :VERSION       "1.1.1"}
       (when cql-filter {:CQL_FILTER cql-filter}))
      :response-format (ajax/text-response-format)
      :on-success      [:map/got-featureinfo request-id point "text/xml" layers]
      :on-failure      [:map/got-featureinfo-err request-id point]}}))

(defmethod get-feature-info INFO-FORMAT-MAP-SERVER
  [_ [_ info-format-type layers request-id leaflet-props point]]
  {:http-xhrio
   {:method :get
    :uri    (-> layers first :server_url)
    :params {:f "json"}
    :response-format (ajax/json-response-format {:keywords? true})
    :on-success      [:map/get-feature-info-map-server-step-2 info-format-type layers request-id leaflet-props point]
    :on-failure      [:map/got-featureinfo-err request-id point]}})

;; MapServer layers need to make an additional request to determine if they are a
;; group layer
(defn get-feature-info-map-server-step-2
  [{:keys [db]} [_ _info-format-type layers request-id _leaflet-props {:keys [lat lng] :as point} map-server-layer-data]]
  (let [layer-server-ids (concat [(:id map-server-layer-data)] (map :id (:subLayers map-server-layer-data)))
        url              (string/join "/" (butlast (string/split (-> layers first :server_url) "/")))
        leaflet-map      (get-in db [:map :leaflet-map])
        leaflet-point    (leaflet/latlng. lat lng)]
    (leaflet/dynamic-map-layer-query
     url
     leaflet-map
     leaflet-point
     (fn [error feature-collection _response]
       (if error
         (re-frame/dispatch [:map/got-featureinfo-err request-id point nil])
         (let [feature-collection
               (as-> (js->clj feature-collection) feature-collection
                 (assoc
                  feature-collection "features"
                  (filterv
                   #(and
                     (some #{(get % "layerId")} layer-server-ids)       ; check it's a layer we're querying for (not a different layer on the server)
                     (not= (get-in % ["properties" "Pixel Value"]) "NoData")) ; check the layer has associated data
                   (get feature-collection "features"))))]
           (re-frame/dispatch [:map/got-featureinfo request-id point "application/json" layers feature-collection])))))
    nil))

(defmethod get-feature-info :default
  [_ [_ _info-format-type layers request-id _leaflet-props point]]
  {:dispatch [:map/got-featureinfo request-id point nil nil layers]})

(defn feature-info-dispatcher [{:keys [db]} [_ leaflet-props point]]
  (let [visible-layers (map #(rich-layer->displayed-layer % db) (visible-layers (:map db)))
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
                           :leaflet-props leaflet-props
                           :location point
                           :show?    false}))] 
    (merge
     {:db db
      :dispatch-later {:ms 300 :dispatch [:map.feature/show request-id]}}
     (if (and (seq requests) (not had-insecure?))
       {:dispatch-n requests}
       {:dispatch   [:map/got-featureinfo request-id point nil nil []]}))))

(defn get-habitat-region-statistics [{:keys [db]} [_ _ point]]
  (let [visible-layers (visible-layers (:map db))
        boundary       (first-where #(= (:category %) :boundaries) visible-layers)
        habitat        (region-stats-habitat-layer db)
        [x y]          (project-coords ((juxt :lng :lat) point) "EPSG:3112")
        request-id     (gensym)]
    (when (and boundary habitat)
      {:http-xhrio {:method          :get
                    :uri             (get-in db [:config :urls :region-stats-url])
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
  [{:keys [db]} [_ leaflet-props point]]
  (let [visible-layers (visible-layers (:map db))]
    (cond
      (get-in db [:map :controls :ignore-click])
      {:dispatch [:map/toggle-ignore-click]}

      (:feature db) ; If we're clicking the map but there's a popup open, just close it
      {:dispatch [:map/popup-closed]}

      (and                                                     ; Only invoke if:
       (not (get-in db [:map :controls :transect]))            ; we aren't drawing a transect;
       (not (get-in db [:map :controls :download :selecting])) ; we aren't selecting a region; and
       (seq visible-layers))                                   ; there are visible layers
      {:dispatch [:map/feature-info-dispatcher leaflet-props point]})))

(defn toggle-ignore-click [db _]
  (update-in db [:map :controls :ignore-click] not))

(defn responses-feature-info [db point]
  (let [responses     (->> (get-in db [:feature-query :responses])
                           (map feature-info-response->display)
                           (remove nil?)
                           vec)
        had-insecure? (get-in db [:feature-query :had-insecure?])]
    (when (seq responses)
      {:location point :leaflet-props (get-in db [:feature :leaflet-props]) :had-insecure? had-insecure? :responses responses :show? true})))

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
        (assoc-in [:map :active-base-layer] (first groups)))))

(defn update-base-layer-groups [db [_ groups]]
  (assoc-in db [:map :base-layer-groups] groups))

(defn update-base-layers [db [_ layers]]
  (let [layers (mapv #(update % :layer_type (comp keyword string/lower-case)) layers)]
    (assoc-in db [:map :base-layers] layers)))

(defn join-keyed-layers
  "Using layers and keyed-layers, replaces layer IDs in keyed-layers with layer
   objects. We only update keyed-layers if the db contains layers."
  [{{:keys [layers keyed-layers]} :map :as db} _]
  (if (seq layers)
    (assoc-in
     db [:map :keyed-layers]
     (reduce-kv
      (fn [m k v] (assoc m k (vec (ids->layers v layers))))
      {} keyed-layers))
    db))

(defn join-rich-layers
  "Using layers and rich-layers, replaces layer IDs in rich-layer data with layer
   objects."
  [{{:keys [layers rich-layer-children]} :map :as db} _]
  (assoc-in
   db [:map :rich-layer-children]
   (reduce-kv
    (fn [m k v]
      (let [parents
            (set
             (map
              (fn [parent]
                (first-where
                 #(= (:id %) parent)
                 layers))
              v))
            child (first-where #(= (:id %) k) layers)]
        (assoc m child parents)))
    {} rich-layer-children)))

(defn update-layers [db [_ layers]]
  (let [{:keys [legend-ids opacity-ids]} db
        layers (process-layers layers)
        db     (-> db
                   (assoc-in [:map :layers] layers)
                   (assoc-in [:layer-state :legend-shown] (init-layer-legend-status layers legend-ids))
                   (assoc-in [:layer-state :opacity] (init-layer-opacities layers opacity-ids)))]
    db))

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
        categories           (vec (sort-by-sort-key categories))]
    (-> db
        (assoc-in [:map :categories] categories)
        (assoc-in [:sorting :category] (->sort-map categories)))))

(defn update-keyed-layers [db [_ keyed-layers]]
  (let [keyed-layers (->> keyed-layers
                          (map #(update % :keyword (comp keyword string/lower-case)))
                          sort-by-sort-key
                          (group-by :keyword)
                          (reduce-kv (fn [m k v] (assoc m k (mapv :layer v))) {}))]
    (assoc-in db [:map :keyed-layers] keyed-layers)))

(defn ->rich-layer-control
  [{:keys [controller_type default_value] :as rich-layer-control}]
  (let [default_value
        (if (= controller_type "multi-dropdown")
          (if default_value [default_value] [])
          default_value)]
    (->
     rich-layer-control
     (set/rename-keys
      {:cql_property    :cql-property
       :data_type       :data-type
       :controller_type :controller-type
       :default_value   :default-value})
     (assoc :default-value default_value))))

(defn- ->rich-layer
  [rich-layer]
  (->
   rich-layer
   (set/rename-keys
    {:alternate_views      :alternate-views
     :tab_label            :tab-label
     :slider_label         :slider-label
     :alternate_view_label :alternate-view-label
     :layer                :layer-id})
   (update :controls #(mapv ->rich-layer-control %))))

(defn- rich-layer->children
  [{:keys [alternate-views timeline]}]
  (set/union
   (set (map :layer alternate-views))
   (set (map :layer timeline))))

(defn- rich-layers->rich-layer-children
  "Converts a list of rich layers to a hashmap, where the keys are the rich layer
   children IDs, and the values are the parent rich layer IDs.
   
   Arguments:
   * `rich-layers`: [{:layer-id ... :alternate-views ... :timeline ...}]
   
   Returns: {child-layer-id #{parent-layer-id ...}}"
  [rich-layers]
  (reduce
   (fn [rich-layer-children {:keys [layer-id] :as rich-layer}]
     (let [children (rich-layer->children rich-layer)]
       (reduce
        (fn [rich-layer-children child]
          "* rich-layer-children: {child-layer-id #{parent-layer-id ...}}
           * child: layer-id"
          (if (get rich-layer-children child)
            (update rich-layer-children child conj layer-id) ; if already exists, add to set
            (assoc rich-layer-children child #{layer-id})))  ; if doesn't exist, create set
        rich-layer-children children)))
   {} rich-layers))

(defn update-rich-layers [db [_ rich-layers]]
  (let [rich-layers (mapv ->rich-layer rich-layers)

        rich-layer-children (rich-layers->rich-layer-children rich-layers)
        
        layer-lookup ; {layer-id rich-layer-id}
        (reduce
         (fn [layer-lookup {:keys [id layer-id] :as rich-layer}]
           (let [children (rich-layer->children rich-layer)]
             (as-> layer-lookup layer-lookup
               (assoc layer-lookup layer-id id)
               (reduce
                #(if (get %1 %2) %1 (assoc %1 %2 id)) ; if child is already in the lookup, don't overwrite (e.g. if the child of the rich layer happens to be a rich layer itself)
                layer-lookup children))))
         {} rich-layers)]
    (->
     db
     (assoc-in [:map :rich-layers :rich-layers] rich-layers)
     (assoc-in [:map :rich-layers :layer-lookup] layer-lookup)
     (assoc-in [:map :rich-layer-children] rich-layer-children))))

(defn update-region-reports [db [_ region-reports]]
  (assoc-in db [:state-of-knowledge :region-reports] region-reports))

(defn layer-started-loading [db [_ layer]]
  (update-in db [:layer-state :loading-state] assoc layer :map.layer/loading))

(defn layer-tile-started-loading [db [_ layer]]
  (update-in db [:layer-state :tile-count layer] inc)) ; note, (inc nil) => 1

(defn layer-loading-error [db [_ layer]]
  (update-in db [:layer-state :error-count layer] inc)) ; note, (inc nil) => 1

(defn layer-finished-loading [db [_ layer]]
  (update-in db [:layer-state :loading-state] assoc layer :map.layer/loaded))

(defn toggle-layer [{:keys [db]} [_ layer]]
  (let [layer-active? ((set (get-in db [:map :active-layers])) layer)]
    {:dispatch [(if layer-active? :map/remove-layer :map/add-layer) layer]}))

(defn toggle-layer-visibility
  [{:keys [db]} [_ layer]]
  (let [hidden-layers (get-in db [:map :hidden-layers])
        hidden? (contains? hidden-layers layer)
        db (update-in db [:map :hidden-layers] #((if hidden? disj conj) % layer))]
    {:db         db
     :dispatch-n [[:map/popup-closed]
                  [:map.layer.selection/maybe-clear]
                  [:maybe-autosave]]}))

(defn toggle-legend-display [{:keys [db]} [_ {:keys [id] :as layer}]]
  (let [db (update-in db [:layer-state :legend-shown] #(if ((set %) layer) (disj % layer) (conj (set %) layer)))
        has-legend? (get-in db [:map :legends id])
        rich-layer  (enhance-rich-layer (layer->rich-layer layer db) db)
        has-cql-filter-values? (get-in rich-layer [:controls :values])]
    {:db         db
     :dispatch-n [[:maybe-autosave]
                  ;; Retrieve layer legend data for display if we don't already have it or aren't
                  ;; already retrieving it
                  (when-not has-legend?
                    [:map.layer/get-legend layer])
                  ;; Retrieve rich layer cql filter data if we don't already have it
                  (when (and rich-layer (not has-cql-filter-values?))
                    [:map.rich-layer/get-cql-filter-values rich-layer])]}))

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [{:keys [db]} [_ layer]]
  (let [layer-active?  ((set (get-in db [:map :active-layers])) layer)
        displayed-layer (rich-layer->displayed-layer layer db)
        bounding_box    (:bounding_box displayed-layer)]
    {:db         db
     :dispatch-n [(when-not layer-active? [:map/add-layer layer])
                  [:map/update-map-view {:bounds bounding_box}]
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

(defn map-print-error [{:keys [db]} _]
  {:message  ["Failed to generate map export image!" b/INTENT-DANGER]
   :dispatch [:ui/hide-loading]})

(defn show-initial-layers
  "Figure out the highest priority layer, and display it"
  ;; Slight hack; note we use :active not :active-layers, because
  ;; during boot we may have loaded hash-state, but we can't hydrate
  ;; the id from the hash state into actual layers, until the layers
  ;; themselves are loaded... by which time the state will have been
  ;; re-set.  So we have this two step process.  Ditto :active-base /
  ;; :active-base-layer
  [{:keys [db]} _]
  (let [{:keys [active active-base initial-bounds? layers]} (:map db)
        legend-ids    (:legend-ids db)
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        active-layers (if active
                        (vec (filter identity (ids->layers active (get-in db [:map :layers]))))
                        startup-layers)
        active-base   (->> (get-in db [:map :grouped-base-layers]) (filter (comp #(= active-base %) :id)) first)
        active-base   (or active-base   ; If no base is set (eg no existing hash-state), use the first candidate
                          (first (get-in db [:map :grouped-base-layers])))
        story-maps    (get-in db [:story-maps :featured-maps])
        featured-map  (get-in db [:story-maps :featured-map])
        featured-map  (first-where #(= (% :id) featured-map) story-maps)
        legends-shown (init-layer-legend-status layers legend-ids)
        legends-get   (map #(rich-layer->displayed-layer % db) legends-shown)
        db            (-> db
                          (assoc-in [:map :active-layers] active-layers)
                          (assoc-in [:map :active-base-layer] active-base)
                          (assoc-in [:story-maps :featured-map] featured-map)
                          (assoc :initialised true))

        feature-location      (get-in db [:feature :location])
        feature-leaflet-props (get-in db [:feature :leaflet-props])
        rich-layers (get-in db [:map :rich-layers :rich-layers])
        cql-get
        (->>
         legend-ids
         (mapv #(get-in db [:map :rich-layers :layer-lookup %]))
         (mapv (fn [id] (first-where #(= (:id %) id) rich-layers))))

        dynamic-pills (get-in db [:dynamic-pills :dynamic-pills])
        active-dynamic-pills (filter #(get-in db [:dynamic-pills :states (:id %) :active?]) dynamic-pills)]
    {:db         db
     :dispatch-n (concat
                  [[:ui/hide-loading]
                   (when (and (seq startup-layers) initial-bounds?)
                     [:map/update-map-view {:bounds (:bounding_box (first startup-layers)) :instant? true}])
                   (when (and feature-location feature-leaflet-props)
                     [:map/feature-info-dispatcher feature-leaflet-props feature-location])
                   [:maybe-autosave]]
                  (mapv #(vector :map.layer/get-legend %) legends-get)
                  (mapv #(vector :map.rich-layer/get-cql-filter-values %) cql-get)
                  (mapv #(vector :dynamic-pill.region-control/get-values %) active-dynamic-pills))}))

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
    (.on leaflet-map "easyPrint-failed"   #(re-frame/dispatch [:map.print/error]))

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
                   :bounds bounds
                   :initial-bounds? false)]
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

(defn map-maybe-clear-selection [{:keys [db]} _]
  (when-not (has-visible-habitat-layers? db)
    {:dispatch [:map.layer.selection/clear]}))

(defn map-finalise-selection [{:keys [db]} [_ bbox]]
  {:db      (update-in db [:map :controls :download] merge {:selecting false
                                                            :bbox      bbox})
   :message [(r/as-element
              [:div "Open layer info ("
               [b/icon {:icon "info-sign" :icon-size 14}]
               ") to download selection"])
             b/INTENT-NONE]
   :dispatch-n [[:left-drawer/open]
                [:left-drawer/tab "active-layers"]]})

(defn map-toggle-selecting [{:keys [db]} _]
  {:dispatch
   (cond
     (get-in db [:map :controls :download :selecting]) [:map.layer.selection/disable]
     (get-in db [:map :controls :download :bbox])      [:map.layer.selection/clear]
     :default                                          [:map.layer.selection/enable])})

(defn rich-layer-tab [{:keys [db]} [_ {:keys [id] :as rich-layer} tab]]
  {:db       (assoc-in
              db [:map :rich-layers :states id :tab]
              tab)
   :dispatch-n [(when
                 (and
                  (= tab "filters")                                                                   ; If we're switching to the filters tab
                  (nil? (:values (first (get-in db [:map :rich-layers :async-datas id :controls]))))) ; and we don't have any filter values
                  [:map.rich-layer/get-cql-filter-values rich-layer])                                 ; then get them
                [:maybe-autosave]]})

(defn rich-layer-get-cql-filter-values [{:keys [db]} [_ {:keys [id] :as rich-layer}]]
  {:http-xhrio
   {:method          :get
    :uri             (get-in db [:config :urls :cql-filter-values-url])
    :params          {:rich-layer-id id}
    :response-format (ajax/json-response-format)
    :on-success      [:map.rich-layer/get-cql-filter-values-success rich-layer]
    :on-failure      [:ajax/default-err-handler]}})

(defn rich-layer-get-cql-filter-values-success [db [_ {:keys [id] :as _rich-layer} {:strs [values filter_combinations]}]]
  (let [values (keywordize-keys values)]
    (as-> db db
     (reduce
      (fn [db {:keys [cql_property values]}]
        (assoc-in db [:map :rich-layers :async-datas id :controls cql_property :values] values))
      db values)
      (assoc-in db [:map :rich-layers :async-datas id :filter-combinations] filter_combinations))))

(defn rich-layer-alternate-views-selected [{:keys [db]} [_ {:keys [id] :as rich-layer} alternate-views-selected]]
  (let [{{old-timeline-value :value
          old-timeline-label :label}
         :timeline-selected
         old-slider-label :slider-label}
        (enhance-rich-layer rich-layer db)

        db (assoc-in db [:map :rich-layers :states id :alternate-views-selected] (get-in alternate-views-selected [:layer :id]))
        {:keys [timeline]
         new-slider-label :slider-label}
        (enhance-rich-layer rich-layer db)

        ; Find a value on the new alternate view's timeline that matches the old
        ; selected value.
        new-timeline-selected
        (first-where
         (fn [{:keys [value label]}]
           (and
            (= value old-timeline-value)
            (= label old-timeline-label)
            (= old-slider-label new-slider-label)))
         timeline)]
    {:db (assoc-in db [:map :rich-layers :states id :timeline-selected] (get-in new-timeline-selected [:layer :id]))
     :dispatch-n
     [(when
       (and alternate-views-selected (not (get-in db [:map :legends (get-in alternate-views-selected [:layer :id])])))
        [:map.layer/get-legend (:layer alternate-views-selected)])
      [:maybe-autosave]]}))

(defn rich-layer-timeline-selected [{:keys [db]} [_ {:keys [id layer] :as _rich-layer} timeline-selected]]
  (let [timeline-selected (when (not= (:layer timeline-selected) layer) timeline-selected)]
    {:db (assoc-in db [:map :rich-layers :states id :timeline-selected] (get-in timeline-selected [:layer :id]))
     :dispatch-n
     [(when
       (and timeline-selected (not (get-in db [:map :legends (get-in timeline-selected [:layer :id])])))
        [:map.layer/get-legend (:layer timeline-selected)])
      [:maybe-autosave]]}))

(defn rich-layer-control-selected [{:keys [db]} [_ {:keys [id] :as _rich-layer} {:keys [cql-property] :as _control} value]]
  {:db       (assoc-in db [:map :rich-layers :states id :controls cql-property :value] value)
   :dispatch [:maybe-autosave]})

(defn rich-layer-reset-filters [{:keys [db]} [_ {:keys [id controls layer] :as _rich-layer}]]
  (merge
   {:db
    (-> db
        (update-in [:map :rich-layers :states id] dissoc :alternate-views-selected)
        (update-in [:map :rich-layers :states id] dissoc :timeline-selected)
        (update-in
         [:map :rich-layers :states id :controls]
         (fn [controls-state]
           (reduce
            (fn [controls-state {:keys [cql-property]}]
              (update controls-state cql-property dissoc :value))
            controls-state controls))))
    :dispatch-n
    [(when-not (get-in db [:map :legends (:id layer)])
       [:map.layer/get-legend layer])
     [:maybe-autosave]]}))

(defn rich-layer-configure
  "Opens a layer to the configuration tab."
  [{:keys [db]} [_ {:keys [id layer-id] :as _rich-layer}]]
  (let [layers  (get-in db [:map :layers])
        layer   (first-where #(= (:id %) layer-id) layers)
        legends (get-in db [:layer-state :legend-shown])]
    {:db (assoc-in db [:map :rich-layers :states id :tab] "filters")
     :dispatch-n
     [[:map/add-layer layer]
      [:left-drawer/tab "active-layers"]
      [:map/update-preview-layer nil]
      (when-not (legends layer) [:map.layer.legend/toggle layer])
      [:maybe-autosave]]}))

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
  (letfn [(dynamic-pill-active?
           [db dynamic-pill]
           "Checks if a dynamic pill has any current active layers.
            
            Args:
            * `db: :seamap/app-state`: Seamap app state
            * `dynamic-pill: :dynamic-pills/dynamic-pill`: Dynamic pill to check for active
              layers
            
            Returns: `true` if the dynamic pill has any active layers, `false` otherwise."
           (s/assert :dynamic-pills/dynamic-pill dynamic-pill)
           (-> (->dynamic-pill dynamic-pill db) :active-layers seq boolean))]
    (let [layers (get-in db [:map :active-layers])
          layers (vec (remove #(= % layer) layers))
          {:keys [habitat bathymetry habitat-obs]} (get-in db [:map :keyed-layers])
          rich-layer (layer->rich-layer layer db)
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
                    (assoc-in [:state-of-knowledge :statistics :habitat-observations :show-layers?] false)))
          dynamic-pills (layer->dynamic-pills layer db)
          deactivated-dynamic-pills (filter #(not (dynamic-pill-active? db %)) dynamic-pills)]
      {:db db
       :dispatch-n
       (concat
        [[:map/popup-closed]
         (when rich-layer [:map.rich-layer/reset-filters rich-layer])
         [:map.layer.selection/maybe-clear]
         [:maybe-autosave]]
        (when (seq deactivated-dynamic-pills)
          (map #(vector :dynamic-pill/active % false) deactivated-dynamic-pills)))})))

(defn add-layer-from-omnibar
  [{:keys [db]} [_ layer]]
  {:db       (assoc-in db [:display :layers-search-omnibar] false)
   :dispatch-n [[:map/add-layer layer]
                [:left-drawer/open]
                [:ui.catalogue/select-tab :main "cat"]
                [:ui.catalogue/catalogue-add-nodes-to-layer :main layer "cat" [:category :data_classification]]]})

(defn update-preview-layer [db [_ preview-layer]]
  (assoc-in db [:map :preview-layer] preview-layer))


(defn toggle-viewport-only [{:keys [db]} _]
  (let [db (update-in db [:map :viewport-only?] not)]
    {:db       db
     :dispatch [:maybe-autosave]}))

(defn pan-to-popup
  "Based on a known location and size of the popup, we can find out if it will be
   outside of the bounds of the map. Knowing this we can pan the map to fix this."
  [{{:keys [feature]
    {{map-width :x map-height :y} :size [map-lat map-lng] :center}
    :map :as db} :db}
   [_ {popup-width :x popup-height :y :as _popup-dimensions}]]
  (let [{{popup-x :x popup-y :y popup-lat :lat popup-lng :lng} :location} feature
        
        view-left       (+ (if (get-in db [:display :left-drawer]) 368 0) 52)
        view-right      (if (boolean (get-in db [:state-of-knowledge :boundaries :active-boundary]))
                          368 0)
        
        map-x           (/ map-width 2)
        map-y           (/ map-height 2)
        overflow-top    (- popup-height popup-y) ; positive values mean we're over the bounds by that many pixels (which tells us how many pixels we'd need to move the map in the opposite direction to have everything in-bounds)
        overflow-bottom (- popup-y map-height)
        overflow-left   (+ (- (/ popup-width 2) popup-x) view-left)
        overflow-right  (+ (- popup-x (- map-width (/ popup-width 2))) view-right)
        x-to-lng        (/ (- popup-lng map-lng) (- popup-x map-x)) ; ratio of lng degrees per x pixel
        y-to-lat        (/ (- popup-lat map-lat) (- popup-y map-y)) ; ratio of lat degrees per y pixel
        map-lat         (cond-> map-lat
                          (pos? overflow-top) (- (* overflow-top y-to-lat))
                          (pos? overflow-bottom) (+ (* overflow-bottom y-to-lat)))
        map-lng         (cond-> map-lng
                          (pos? overflow-left) (- (* overflow-left x-to-lng))
                          (pos? overflow-right) (+ (* overflow-right x-to-lng)))]
    (when feature {:dispatch [:map/update-map-view {:center [map-lat map-lng]}]}))) ; only pan if still popup exists, otherwise the calculations are incorrect! ISA-491

(defn get-layer-legend
  [{:keys [db]} [_ {:keys [id] :as layer}]]
  {:db         (assoc-in db [:map :legends id] :map.legend/loading)
   :http-xhrio {:method          :get
                :uri             (str (get-in db [:config :urls :layer-legend-url]) id)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:map.layer/get-legend-success layer]
                :on-failure      [:map.layer/get-legend-error layer]}})

(defn get-layer-legend-success
  [db [_ {:keys [id] :as _layer} response]]
  (assoc-in db [:map :legends id] response))

(defn get-layer-legend-error
  [db [_ {:keys [id] :as _layer}]]
  (assoc-in db [:map :legends id] :map.legend/error))
