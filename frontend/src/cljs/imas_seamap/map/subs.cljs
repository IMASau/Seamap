;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.subs
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [imas-seamap.map.utils :as map-utils :refer [enhance-rich-layer
                                                enhanced->cql-filter
                                                has-time-dimension?
                                                region-stats-habitat-layer
                                                rich-layer-children->parents
                                                sort-layers viewport-layers
                                                match-layer]]
   [imas-seamap.utils :refer [ids->layers map-on-key first-where]]))

(defn map-props [db _] (-> db :map (select-keys [:zoom :center :bounds])))

(rf/reg-sub :dbsubs/layer-state-loading (fn [db _] (get-in db [:layer-state :loading-state])))
(rf/reg-sub :dbsubs/layer-state-error-count (fn [db _] (get-in db [:layer-state :error-count])))
(rf/reg-sub :dbsubs/layer-state-tile-count (fn [db _] (get-in db [:layer-state :tile-count])))
(rf/reg-sub :dbsubs/layer-state-legend-shown (fn [db _] (get-in db [:layer-state :legend-shown])))
(rf/reg-sub :dbsubs/layer-state-opacity (fn [db _] (get-in db [:layer-state :opacity])))
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

;;; Phase A of the rich-layers refactor (docs/rich-layers-refactor-plan.md):
;;; enhance every rich layer once, in one place, producing plain data keyed by
;;; id. Downstream subs and views look up results here instead of re-running
;;; enhance-rich-layer per layer per render.

(rf/reg-sub
 ::layers-by-id
 :<- [:dbsubs.map/layers]
 (fn [layers _query-v]
   (into {} (map (juxt :id identity)) layers)))

(rf/reg-sub
 ::rich-layers-by-id
 :<- [:dbsubs.map/rich-layers]
 (fn [rich-layers _query-v]
   (into {} (map (juxt :id identity)) rich-layers)))

(rf/reg-sub
 ::enhanced-rich-layers
 (fn [_query-v]
   {:layers-by-id      (rf/subscribe [::layers-by-id])
    :rich-layers       (rf/subscribe [:dbsubs.map/rich-layers])
    :rich-layers-by-id (rf/subscribe [::rich-layers-by-id])
    :rl-states         (rf/subscribe [:dbsubs.map/rich-layer-states])
    :rl-async-datas    (rf/subscribe [:dbsubs.map/rich-layer-async-datas])
    :rl-lookup         (rf/subscribe [:dbsubs.map/rich-layer-lookup])})
 (fn [{:keys [layers-by-id rich-layers rich-layers-by-id rl-states rl-async-datas rl-lookup]} _query-v]
   (let [ctx              {:layers-by-id      layers-by-id
                           :rich-layers       rich-layers
                           :rich-layers-by-id rich-layers-by-id
                           :rl-states         rl-states
                           :rl-async-datas    rl-async-datas
                           :rl-lookup         rl-lookup}
         by-rich-layer-id (into {} (map (fn [{:keys [id] :as rich-layer}]
                                          [id (enhance-rich-layer rich-layer ctx)]))
                                rich-layers)
         by-layer-id      (into {} (keep (fn [[layer-id rich-layer-id]]
                                           (when-let [rich-layer (get by-rich-layer-id rich-layer-id)]
                                             [layer-id rich-layer])))
                                rl-lookup)
         displayed->main  (into {} (keep (fn [{:keys [layer-id] :as rich-layer}]
                                           (when layer-id
                                             [(or (get-in rich-layer [:displayed-layer :id]) layer-id) layer-id])))
                                (vals by-rich-layer-id))]
     ;; :by-rich-layer-id  rich-layer id  → enhanced rich layer
     ;; :by-layer-id       layer id       → enhanced rich layer (via layer-lookup,
     ;;                                     so includes alternate/timeline children)
     ;; :displayed->main   displayed layer id → main (catalogue) layer id
     {:by-rich-layer-id by-rich-layer-id
      :by-layer-id      by-layer-id
      :displayed->main  displayed->main})))

;;; Phase B of the rich-layers refactor (docs/rich-layers-refactor-plan.md): the
;;; volatile layer-state family as small, id-keyed data subs. Tile-load events
;;; update these without touching :map/layers, and their outputs are plain data
;;; so unchanged values don't re-render subscribers.
;;; Note the underlying db state is still keyed by layer objects (of the
;;; *displayed* layer for tile state); these subs convert to catalogue-layer ids.

(rf/reg-sub
 ::loading-layers
 :<- [:dbsubs/layer-state-loading]
 :<- [::enhanced-rich-layers]
 (fn [[loading-state {:keys [displayed->main]}] _query-v]
   (->> loading-state
        (keep (fn [[layer state]]
                (when (= state :map.layer/loading)
                  (let [id (:id layer)]
                    (get displayed->main id id)))))
        set)))

(rf/reg-sub
 ::error-layers
 :<- [:dbsubs/layer-state-error-count]
 :<- [:dbsubs/layer-state-tile-count]
 :<- [::enhanced-rich-layers]
 (fn [[error-counts tile-counts {:keys [displayed->main]}] _query-v]
   ;; A layer is in error above a failed-tile ratio threshold, rather than on
   ;; any single error. (Might be nice to make this configurable eventually.)
   (->> tile-counts
        (keep (fn [[layer total-count]]
                (when (and (pos? total-count)
                           (> (/ (get error-counts layer 0) total-count)
                              0.4))
                  (let [id (:id layer)]
                    (get displayed->main id id)))))
        set)))

(rf/reg-sub
 ::expanded-layers
 :<- [:dbsubs/layer-state-legend-shown]
 (fn [legend-shown _query-v]
   (into #{} (map :id) legend-shown)))

(rf/reg-sub
 ::layer-opacities
 :<- [:dbsubs/layer-state-opacity]
 (fn [opacities _query-v]
   (into {} (map (fn [[layer opacity]] [(:id layer) opacity])) opacities)))

;;; Phase B: CQL filters as a lookup map, replacing the :cql-filter-fn closure.
;;; Computed only over active layers — they're the only ones rendered on the map.
(rf/reg-sub
 ::cql-filters
 (fn [_query-v]
   {:enhanced      (rf/subscribe [::enhanced-rich-layers])
    :active-layers (rf/subscribe [:dbsubs.map/active-layers])
    :dynamic-pills (rf/subscribe [:dbsubs/dynamic-pills])
    :dp-states     (rf/subscribe [:dbsubs/dynamic-pill-states])})
 (fn [{:keys [enhanced active-layers dynamic-pills dp-states]} _query-v]
   (let [{:keys [by-layer-id]} enhanced]
     (into {}
           (keep (fn [{:keys [id] :as layer}]
                   (when-let [cql-filter (enhanced->cql-filter layer (get by-layer-id id) dynamic-pills dp-states)]
                     [id cql-filter])))
           active-layers))))

(rf/reg-sub
 :map/layers
 (fn [_query-v]
   {:filters             (rf/subscribe [:dbsubs/filters])
    :sorting             (rf/subscribe [:dbsubs/sorting])
    :layers              (rf/subscribe [:dbsubs.map/layers])
    :active-layers       (rf/subscribe [:dbsubs.map/active-layers])
    :hidden-layers       (rf/subscribe [:dbsubs.map/hidden-layers])
    :categories          (rf/subscribe [:dbsubs.map/categories])
    :rich-layers         (rf/subscribe [:dbsubs.map/rich-layers])
    :rich-layer-children (rf/subscribe [:dbsubs.map/rich-layer-children])
    :enhanced            (rf/subscribe [::enhanced-rich-layers])})
 (fn [{:keys [filters
              sorting
              layers
              active-layers
              hidden-layers
              categories
              rich-layers
              rich-layer-children
              enhanced]} _query-v]
   (let [{:keys [by-layer-id]} enhanced
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

         filtered-layers (set (filter (partial match-layer filter-text categories) layers)) ; get the set of all layers that match the filter
         filtered-layers (rich-layer-children->parents filtered-layers rich-layer-children) ; get the rich-layer parents for this layer, and add them to the searched layers
         filtered-layers (filterv filtered-layers catalogue-layers) ; filtered-layers set converted into vector by filtering on catalogue-layers (sorted)
         sorted-layers   (sort-layers catalogue-layers sorting)

         ;; Bridge closure over the pre-enhanced data; consumers should migrate
         ;; to the :rich-layers-by-layer-id data key so this can be removed
         ;; (closures in the sub output defeat render skipping).
         rich-layer-fn   #(get by-layer-id (:id %))
         visible-layers  (map-utils/visible-layers {:hidden-layers hidden-layers
                                                    :active-layers active-layers})]

     ;; Loading/error/expanded/opacity state and CQL filters live in their own
     ;; id-keyed subs (::loading-layers, ::error-layers, ::expanded-layers,
     ;; ::layer-opacities, ::cql-filters) so that tile-load and control state
     ;; changes don't recompute this sub.
     {:layers           layers
      :groups           (group-by :category filtered-layers)
      :active-layers    active-layers
      :visible-layers   visible-layers
      :filtered-layers  filtered-layers
      :sorted-layers    sorted-layers
      :catalogue-layers catalogue-layers
      :rich-layers-by-layer-id by-layer-id
      :rich-layer-fn    rich-layer-fn})))

;;; This is extracted out from the :map/layers subscription as a
;;; stand-alone. It is a performance-related trade-off; it makes
;;; semantic sense to be included in the main map-layers sub, but the
;;; downside is the viewport-layers are by definition recalculated
;;; when the viewport bounds change, which is basically any map
;;; interaction. Extracting it out as a stand-alone means only the
;;; sidebar needs to subscribe to it, and the map itself receives far
;;; fewer subscription updates.
(rf/reg-sub
 :map/layers.viewport
 :<- [:dbsubs.map/layers]
 :<- [:dbsubs.map/bounds]
 (fn [[layers bounds] _query-v]
   (viewport-layers bounds layers)))

; This sub is something that would have formerly been in the monolithic
; 'map-layers' sub above. This sub is part of a new strategy to break up the
; monolothic sub into smaller subs that are easier to manage and take advantage of
; the the re-frame subscription DAG.
(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map.
   Signals: [::enhanced-rich-layers, :dbsubs.map/layers].
   Note: still keyed by whole layer maps for compatibility with existing
   consumers; id-keying comes with Phase B of the rich-layers refactor."
  [[{:keys [by-layer-id]} layers] _]
  (reduce
   (fn [acc {:keys [id] :as layer}]
     (assoc acc layer (or (get-in by-layer-id [id :displayed-layer]) layer)))
   {} layers))

(defn rich-layers-side-by-side-views
  "Enhanced rich layers that are currently active and have side-by-side views
   configured.
   Signals: [::enhanced-rich-layers, :dbsubs.map/rich-layers,
   :dbsubs.map/active-layers]. (rich-layers is only used to keep a stable,
   configuration-order output.)"
  [[{:keys [by-rich-layer-id]} rich-layers active-layers] _]
  (let [active-ids (set (map :id active-layers))]
    (->> rich-layers
         (map #(get by-rich-layer-id (:id %)))
         (filter
          (fn [{:keys [layer-id side-by-side-views]}]
            (and
             (contains? active-ids layer-id)
             (seq side-by-side-views)))))))

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

(defn layer-legends
  "A lookup of each layer to its legend at the current moment.

   If a layer argument is supplied, returns only the legend for that layer as a
   response."
  [db [_ {:keys [id] :as layer}]]
  (let [; Create our lookup table
        legends-lookup
        (->>
         (get-in db [:map :legends])
         (reduce-kv
          (fn [legends-lookup layer-id legend-info]
            (let [status
                  (cond
                    (keyword? legend-info) legend-info
                    legend-info            :map.legend/loaded
                    :else                  :map.legend/none)
                  layer (first-where #(= (:id %) layer-id) (get-in db [:map :layers]))
                  layer-legend ; Create a single "legend" for our lookup key-value map. This has legend status and its value (info)
                  {:layer-id   layer-id
                   :layer-name (:name layer)
                   :status    status
                   :info      (when (= status :map.legend/loaded) legend-info)}]
              (assoc legends-lookup layer-id layer-legend))) ; Add legend to lookup key-value map
          {}))]
    (if layer
      (get legends-lookup id {:status :map.legend/none}) ; If legend not in lookup key-value map, return none
      legends-lookup)))

(defn layer-visible-layers-legends
  "All the legends for all the visible layers on the map at the current time."
  [[{:keys [visible-layers]} displayed-layers-lookup layer-legends] _]
  (let [displayed-layers (map #(get displayed-layers-lookup %) visible-layers)
        visible-layers-legends (map #(get layer-legends (:id %) {:status :map.legend/none}) displayed-layers)]
    (reverse visible-layers-legends)))

(defn layer-visible-side-by-side-layers-legends
  "Right-hand side legends for all the visible side-by-side layers on the map at
   the current time."
  [[{:keys [visible-layers rich-layer-fn]} layer-legends] _]
  (let [visible-right-layer-ids
        (->> visible-layers
             (map rich-layer-fn)
             (map :side-by-side-views-selected-id)
             (filter identity))
        visible-layers-legends (map #(get layer-legends % {:status :map.legend/none}) visible-right-layer-ids)]
    visible-layers-legends))

(defn print-is-printing?
  "Is the app currently printing?

   So the app can apply custom styles during the print process for custom map print
   images."
  [db _]
  (get db :is-printing?))
