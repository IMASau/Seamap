;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.subs
  (:require
   [clojure.set :as set]
   [imas-seamap.map.utils :as map-utils :refer [enhance-rich-layer
                                                has-time-dimension?
                                                layer->cql-filter
                                                layer->rich-layer
                                                db->ctx
                                                region-stats-habitat-layer
                                                rich-layer->displayed-layer
                                                rich-layer-children->parents
                                                sort-layers viewport-layers
                                                match-layer]]
   [re-frame.core :as rf]
   [imas-seamap.utils :refer [ids->layers map-on-key]]))

(defn map-props [db _] (-> db :map (select-keys [:zoom :center :bounds])))

(defn- make-error-fn
  "Given maps of layer->error-count and layer->total-tile-count, returns
  a function that takes a layer and returns a boolean indicating if
  the layer is problematic or not (ie, rather than just saying we
  should notify the user about any error, we want to notify above a
  certain threshold)"
  [error-counts load-counts ctx]
  (fn [layer]
    (let [layer (rich-layer->displayed-layer layer ctx)
          error-count (get error-counts layer 0)
          total-count (get load-counts layer 0)]
      (and (pos? total-count)
           (> (/ error-count total-count)
              0.4)))))       ; Might be nice to make this configurable eventually

(rf/reg-sub :dbsubs/layer-state (fn [db _] (get db :layer-state)))
(rf/reg-sub :dbsubs/filters (fn [db _] (get db :filters)))
(rf/reg-sub :dbsubs/sorting (fn [db _] (get db :sorting)))
(rf/reg-sub :dbsubs.map/layers (fn [db _] (get-in db [:map :layers])))
(rf/reg-sub :dbsubs.map/active-layers (fn [db _] (get-in db [:map :active-layers])))
(rf/reg-sub :dbsubs.map/hidden-layers (fn [db _] (get-in db [:map :hidden-layers])))
(rf/reg-sub :dbsubs.map/bounds (fn [db _] (get-in db [:map :bounds])))
(rf/reg-sub :dbsubs.map/categories (fn [db _] (get-in db [:map :categories])))
(rf/reg-sub :dbsubs.map/rich-layers (fn [db _] (get-in db [:map :rich-layers :rich-layers])))
(rf/reg-sub :dbsubs.map/rich-layer-states (fn [db _] (get-in db [:map :rich-layers :states])))
(rf/reg-sub :dbsubs.map/rich-layer-async-datas (fn [db _] (get-in db [:map :rich-layers :async-datas])))
(rf/reg-sub :dbsubs.map/rich-layer-lookup (fn [db _] (get-in db [:map :rich-layers :layer-lookup])))
(rf/reg-sub :dbsubs.map/rich-layer-children (fn [db _] (get-in db [:map :rich-layer-children])))
(rf/reg-sub :dbsubs/dynamic-pills (fn [db _] (get-in db [:dynamic-pills :dynamic-pills])))
(rf/reg-sub :dbsubs/dynamic-pill-states (fn [db _] (get-in db [:dynamic-pills :states])))
(rf/reg-sub :dbsubs/dynamic-pill-async-datas (fn [db _] (get-in db [:dynamic-pills :async-datas])))
(rf/reg-sub :dbsubs/display-open-pill (fn [db _] (get-in db [:display :open-pill])))
(rf/reg-sub :dbsubs/split-layer-container-x (fn [db _] (get-in db [:display :split-layer-container-x])))

(rf/reg-sub
 :map/layers
 (fn [_query-v]
   (js/console.log "**** [sub-signal] map/layers")
   {:layer-state         (rf/subscribe [:dbsubs/layer-state])
    :filters             (rf/subscribe [:dbsubs/filters])
    :sorting             (rf/subscribe [:dbsubs/sorting])
    :layers              (rf/subscribe [:dbsubs.map/layers])
    :active-layers       (rf/subscribe [:dbsubs.map/active-layers])
    :hidden-layers       (rf/subscribe [:dbsubs.map/hidden-layers])
    :bounds              (rf/subscribe [:dbsubs.map/bounds])
    :categories          (rf/subscribe [:dbsubs.map/categories])
    :rich-layers         (rf/subscribe [:dbsubs.map/rich-layers])
    :rich-layer-children (rf/subscribe [:dbsubs.map/rich-layer-children])
    :rl-states           (rf/subscribe [:dbsubs.map/rich-layer-states])
    :rl-async-datas      (rf/subscribe [:dbsubs.map/rich-layer-async-datas])
    :rl-lookup           (rf/subscribe [:dbsubs.map/rich-layer-lookup])
    :dynamic-pills       (rf/subscribe [:dbsubs/dynamic-pills])
    :dp-states           (rf/subscribe [:dbsubs/dynamic-pill-states])
    :dp-async-datas      (rf/subscribe [:dbsubs/dynamic-pill-async-datas])
    :open-pill           (rf/subscribe [:dbsubs/display-open-pill])})
 (fn [{:keys [layer-state
              filters
              sorting
              layers
              active-layers
              hidden-layers
              bounds
              categories
              rich-layers
              rich-layer-children
              rl-states
              rl-async-datas
              rl-lookup
              dynamic-pills
              dp-states
              dp-async-datas
              open-pill]} _query-v]
   (js/console.log "[sub] map-layers")
   (let [layers-by-id    (into {} (map (juxt :id identity)) layers)
         rich-layers-by-id (into {} (map (juxt :id identity)) rich-layers)
         ctx             {:layers-by-id      layers-by-id
                          :rich-layers       rich-layers
                          :rich-layers-by-id rich-layers-by-id
                          :rl-states         rl-states
                          :rl-async-datas    rl-async-datas
                          :rl-lookup         rl-lookup
                          :dynamic-pills     dynamic-pills
                          :dp-states         dp-states
                          :dp-async-datas    dp-async-datas
                          :active-layers     active-layers
                          :open-pill         open-pill}
         categories      (map-on-key categories :name)
         filter-text     (:layers filters)

         rlc-ids ; rich-layer-children to hide from the catalogue
         (reduce
          (fn [acc {:keys [layer-id alternate-views timeline]}]
            (->>
             (set/union (set (map :layer alternate-views)) (set (map :layer timeline))) ; Get all the alternate views and timeline layers for the rich layer
             (remove #(= % layer-id))                                                   ; Ignore the ones that match the "main" layer for the rich layer (its catalogue entry)
             set                                                                        ; Convert back to a set after the "remove" op
             (set/union acc)))                                                          ; Add to the accumulative list of all layers to hide from the catalogue
          #{} rich-layers)

         catalogue-layers
         (->>
          layers
          (filter #(get-in categories [(:category %) :display_name])) ; only layers with a category that has a display name are allowed
          (remove
           (fn [{:keys [id]}]
             (some #{id} rlc-ids)))) ; removes rich-layer children (except those that are a child of themselves)

         viewport-layers (viewport-layers bounds catalogue-layers)
         filtered-layers (set (filter (partial match-layer filter-text categories) layers)) ; get the set of all layers that match the filter
         filtered-layers (rich-layer-children->parents filtered-layers rich-layer-children) ; get the rich-layer parents for this layer, and add them to the searched layers
         filtered-layers (filterv filtered-layers catalogue-layers) ; filtered-layers set converted into vector by filtering on catalogue-layers (sorted)
         sorted-layers   (sort-layers catalogue-layers sorting)
         displayed-rich-layers (reduce
                                (fn [displayed-rich-layers layer]
                                  (assoc displayed-rich-layers layer (rich-layer->displayed-layer layer ctx)))
                                {} (keep layers-by-id (map :layer-id rich-layers)))
         displayed-layers->layers (set/map-invert displayed-rich-layers)

         rich-layer-fn   #(enhance-rich-layer (layer->rich-layer % ctx) ctx)
         visible-layers  (map-utils/visible-layers {:hidden-layers hidden-layers
                                                    :active-layers active-layers})]

     {:layers           layers
      :groups           (group-by :category filtered-layers)
      :loading-layers   (->>
                        layer-state :loading-state
                        (filter (fn [[l st]] (= st :map.layer/loading)))
                        keys
                        (map #(or (get displayed-layers->layers %) %))
                        set)
      :error-layers     (make-error-fn (:error-count layer-state) (:tile-count layer-state) ctx)
      :expanded-layers  (->> layer-state :legend-shown set)
      :active-layers    active-layers
      :visible-layers   visible-layers
      :layer-opacities  (fn [layer] (get-in layer-state [:opacity layer] 100))
      :filtered-layers  filtered-layers
      :sorted-layers    sorted-layers
      :viewport-layers  viewport-layers
      :catalogue-layers catalogue-layers
      :rich-layer-fn    rich-layer-fn
      :cql-filter-fn    #(layer->cql-filter % ctx)})))

; This sub is something that would have formerly been in the monolithic
; 'map-layers' sub above. This sub is part of a new strategy to break up the
; monolothic sub into smaller subs that are easier to manage and take advantage of
; the the re-frame subscription DAG. 
(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map."
  [{:keys [layers rich-layer-fn] :as _map-layers} _]
  (map-utils/layer-displayed-layers-lookup layers rich-layer-fn))

(defn rich-layers-side-by-side-views [db _]
  (let [ctx (db->ctx db)
        rich-layers (map #(enhance-rich-layer % ctx) (:rich-layers ctx))
        active-layers (:active-layers ctx)]

    (filter
     (fn [rich-layer]
       (and
        (some #{(:layer-id rich-layer)} (map :id active-layers))
        (seq (get-in rich-layer [:side-by-side-views]))))
     rich-layers)))

(rf/reg-sub :dbsubs.map/zoom (fn [db _] (get-in db [:map :zoom])))
(rf/reg-sub :dbsubs.map/grouped-base-layers (fn [db _] (get-in db [:map :grouped-base-layers])))
(rf/reg-sub :dbsubs.map/active-base-layer (fn [db _] (get-in db [:map :active-base-layer])))
(rf/reg-sub
 :map/base-layers
 (fn [_query-v]
   {:zoom                (rf/subscribe [:dbsubs.map/zoom])
    :grouped-base-layers (rf/subscribe [:dbsubs.map/grouped-base-layers])
    :active-base-layer   (rf/subscribe [:dbsubs.map/active-base-layer])})
 (fn [{:keys [grouped-base-layers active-base-layer zoom]} _query-v]
   (let [enabled-base-layer-fn (fn [{:keys [max_zoom]}] (or (nil? max_zoom) (<= zoom max_zoom))) ; Utility for checking if a basemap is "enabled" - i.e. is it a valid basemap the user can select, or are we beyond the max zoom for the layer
         enabled-base-layers   (filter enabled-base-layer-fn grouped-base-layers)]
     {:grouped-base-layers   grouped-base-layers ; List of selectable basemaps (composite basemaps grouped as a single element)
      :active-base-layer     (if (enabled-base-layer-fn active-base-layer) active-base-layer (first enabled-base-layers)) ; Active basemap is the one selected by the user, unless it's disabled. If the basemap is disabled, then switch to first enabled basemap
      :enabled-base-layer-fn enabled-base-layer-fn})))

(defn display-categories
  "Filter categories to only those that have a display name and at least one layer."
  [{:keys [map]} _]
  (let [{:keys [layers categories]} map
        grouped-layers              (group-by :category layers)]
    (filter (fn [{:keys [display_name name]}] (and display_name (seq (name grouped-layers)))) categories)))

(defn categories-map [db _]
  (let [categories (get-in db [:map :categories])]
    (map-on-key categories :name)))

(defn layer-selection-info [db _]
  {:selecting? (boolean (get-in db [:map :controls :download :selecting]))
   :region     (get-in db [:map :controls :download :bbox])})

(defn region-stats [db _]
  ;; The selected habitat layer for region-stats, providing it is
  ;; active; default selection if there's a single habitat layer:
  {:habitat-layer (region-stats-habitat-layer db)})

(defn map-layers-filter [db _]
  (get-in db [:filters :layers]))

(defn map-other-layers-filter [db _]
  (get-in db [:filters :other-layers]))

(defn map-layer-lookup [db _]
  (reduce
   (fn [acc {:keys [server_url layer_name] :as layer}]
     (assoc acc [server_url layer_name] layer))
   {}
   (get-in db [:map :layers])))

;; TODO: Remove, unused - related to getting boundary and habitat region stats
#_(defn map-layer-extra-params-fn
  "Creates a function that returns a map of additional WMS parameters
  for a given layer argument."
  [db _]
  (let [region-stats?                  (= "tab-management" (get-in db [:display :sidebar :selected]))
        {:keys [lng lat] :as location} (get-in db [:feature :location])
        habitat-layer                  (region-stats-habitat-layer db)
        boundary-layer                 (->> db :map :active-layers (filter #(= :boundaries (:category %))) first)
        info-layer                     (get-in db [:display :info-card :layer])]
    (cond
      ;; Region-stats mode; if:
      ;; * tab is "management", and
      ;; * we have a selected habitat layer, and
      ;; * there's a mouse-click
      ;; Then add a filter for that click-location, and a translucency for /other/ habitat layers
      ;; (Note, regular FILTER so we can specify a CRS and just use 4326; we have both 3112 and 4326 boundaries)
      (and region-stats? location habitat-layer boundary-layer)
      (fn [layer]
        (cond
          (= layer boundary-layer) {:FILTER (str "<Filter xmlns=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">"
                                                 "<Contains><PropertyName>geom</PropertyName>"
                                                 "<gml:Point srsName=\"EPSG:4326\"><gml:coordinates>"
                                                 lng "," lat
                                                 "</gml:coordinates></gml:Point></Contains></Filter>")}
          (= layer habitat-layer)  nil
          :else                    {:opacity 0.1}))

      ;; Displaying info-card for a layer?  Fade-out the others (note
      ;; need to ensure we don't touch the FILTER if it's a boundary
      ;; layer, or we'll trigger layer-load events):
      info-layer
      (fn [layer]
        (cond
          (= layer info-layer)              nil
          (= :boundaries (:category layer)) {:FILTER "" :opacity 0.1}
          :else                             {:opacity 0.1}))

      ;; Everything else; reset the CQL filter for boundaries,
      ;; otherwise leave the default opacity (and everything else)
      :else
      (fn [layer]
        (when (= :boundaries (:category layer))
          {:FILTER ""})))))

(defn organisations
  "Sub to access organisations; overloaded to return a single org
  specified by name."
  [{{:keys [organisations]} :map} [_ org-name]]
  (if org-name
    (some #(and (= org-name (:name %)) %) organisations)
    organisations))

(defn timeseries-layers
  "List of layers that are currently active and have a time dimension."
  [map-layers _]
  (filter has-time-dimension? (:active-layers map-layers)))

(defn show-time-slider?
  "Should the time slider be shown on the map? True if there are currently active
   layers with a time dimension."
  [timeseries-layers _]
  (seq timeseries-layers))

(defn current-time
  "The current time selected for the time dimension. It is designed to be in sync
   with the Leaflet timeDimension component."
  [db _]
  (get-in db [:display :current-time]))

(defn time-available-times
  "The available times for the layers, driven by the timeDimension component."
  [db _]
  (get-in db [:display :available-times]))

(defn time-is-playing?
  "Whether the time dimension is currently playing or paused."
  [db _]
  (get-in db [:display :time-is-playing?]))

(defn time-is-loading?
  "Whether the time dimension is currently loading a new time, driven by
   the timeDimension component."
  [db _]
  (get-in db [:display :time-is-loading?]))

(defn viewport-only? [db _]
  (get-in db [:map :viewport-only?]))

(defn layer-legend [db [_ {:keys [id] :as _layer}]]
  (let [legend-info (get-in db [:map :legends id])
        status      (cond
                      (keyword? legend-info) legend-info
                      legend-info            :map.legend/loaded
                      :else                  :map.legend/none)]
    {:status    status
     :info      (when (= status :map.legend/loaded) legend-info)}))
