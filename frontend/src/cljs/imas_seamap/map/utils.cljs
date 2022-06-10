;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [imas-seamap.db :refer [api-url-base]]
            [imas-seamap.utils :refer [merge-in]]
            ["proj4" :as proj4]
            [clojure.string :as str]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


(def ^:private EPSG-3112
  (proj4
   "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(defn wgs84->epsg3112 [pt]
  ;; pt is a vector of [lon lat]
  (js->clj
   (EPSG-3112.forward (clj->js pt))))

(defn bounds->projected [project-fn {:keys [north south east west] :as _bounds}]
  (let [[x0 y0] (project-fn [west south])
        [x1 y1] (project-fn [east north])]
    {:west  x0
     :south y0
     :east  x1
     :north y1}))

;;; Note, the namespace format (",EPSG:4326") used here has important
;;; correlation with the WFS version used; see
;;; https://docs.geoserver.org/latest/en/user/services/wfs/axis_order.html
(defn bounds->str
  ([bounds] (bounds->str 4326 bounds))
  ([epsg-code {:keys [north south east west] :as _bounds}]
   (assert (integer? epsg-code))
   (string/join "," [west south east north (str "EPSG:" epsg-code)])))

(defn bounds->geojson [{:keys [north south east west]}]
  {:type "Polygon"
   :coordinates [[[west south] [west north] [east north] [east south] [west south]]]})

(defn bbox-intersects? [b1 b2]
  ;; Corner case handled here: if either bounds is empty, just return
  ;; true.  Map bounds are populated by leaflet events, and at startup
  ;; we haven't received any such events yet, so the map bounds are
  ;; empty and we would otherwise filter out all (otherwise
  ;; applicable) layers.
  (or (empty? b1)
      (empty? b2)
      (not
       (or (> (:west b1)  (:east b2))
           (< (:east b1)  (:west b2))
           (> (:south b1) (:north b2))
           (< (:north b1) (:south b2))))))

(defn habitat-layer? [layer] (-> layer :category (= :habitat)))

(defn layer-search-keywords
  "Returns the complete search keywords of a layer, space-separated."
  [categories {:keys [name category layer_name description organisation data_classification keywords]}]
  (let [category-display-name (get-in categories [category :display_name])]
    (string/join " " [name category category-display-name layer_name description organisation data_classification keywords])))

(defn layer-name
  "Returns the most specific layer name; ie either detail_layer if
  defined, or layer_name otherwise"
  [layer]
  (or (:detail_layer layer)
      (:layer_name layer)))

(defn region-stats-habitat-layer
  "Encapsulate the logic for identifying the region-stats selected
  habitat layer.  This is the one that has been selected (provided it
  is still active), or if there is only a single habitat layer then
  assume that one."
  [{:keys [region-stats] :as db}]
  (let [habitat-layers (filter #(= :habitat (:category %)) (get-in db [:map :active-layers]))]
    (cond
      (= 1 (count habitat-layers)) (first habitat-layers)
      (some #{(:habitat-layer region-stats)} habitat-layers) (:habitat-layer region-stats))))

(defn sort-layers
  [layers categories]
  (sort-by
   (juxt #(get-in categories [(:category %) :display_name]) :data_classification :id)
   #(< %1 %2) ; comparator so nil is always last (instead of first)
   layers))

(defn sort-layers-presentation
  "Return layers in an order suitable for presentation (essentially,
  bathymetry at the bottom, third-party on top, and habitat layers by
  priority when in auto mode)"
  [layers group-priorities logic-type]
  (let [layer-priority (fn [id]
                         (if (= logic-type :map.layer-logic/automatic)
                           ;; If automatic, pick the highest priority present
                           (reduce (fn [p {:keys [layer priority]}]
                                     (if (= layer id) (min p priority) p))
                                   99
                                   group-priorities)
                           ;; If in manual mode, just use a default priority
                           1))]
    ;; Schwarztian transform (map-sort-map):
    (->> layers
         (map (fn [{:keys [id] :as layer}]
                [(layer-priority id) layer]))
         (sort-by (comp vec (partial take 1)))
         (map last))))

(defn applicable-layers
  [{{:keys [layers groups priorities priority-cutoff zoom zoom-cutover bounds logic]} :map :as _db}
   & {:keys [category server_type]}]
  ;; Generic utility to retrieve a list of relevant layers, filtered as necessary.
  ;; figures out cut-off point, and restricts to those on the right side of it;
  ;; filters to those groups intersecting the bbox;
  ;; sorts by priority and grouping.
  (let [logic-type         (:type logic)
        match-category?    #(if category (= category (:category %)) true)
        match-server?      #(if server_type (= server_type (:server_type %)) true)
        detail-resolution? (< zoom-cutover zoom)
        group-ids          (->> groups
                                (filter (fn [{:keys [bounding_box detail_resolution]}]
                                          ;; detail_resolution only applies to habitat layers:
                                          (and (or (nil? detail_resolution)
                                                   (= detail_resolution detail-resolution?))
                                               (bbox-intersects? bounds bounding_box))))
                                (map :id)
                                set)
        group-priorities   (filter #(and (or (= logic-type :map.layer-logic/manual)
                                             (< (:priority %) priority-cutoff))
                                         (group-ids (:group %)))
                                   priorities)
        layer-ids          (->> group-priorities (map :layer) (into #{}))
        selected-layers    (filter #(and (layer-ids (:id %)) (match-category? %) (match-server? %)) layers)
        viewport-layers (filter #(bbox-intersects? bounds (:bounding_box %)) selected-layers)]
    (sort-layers-presentation viewport-layers priorities logic-type)))

(defn all-priority-layers
  "Return the list of priority layers: that is, every layer for which
  its priority in *some* group is higher than the priority-cutoff.
  This only applies to habitat and bathymetry layers; other categories
  aren't handled via priorities and are always included."
  [{{:keys [layers priorities priority-cutoff]} :map :as _db}]
  (let [priority-layer-ids (->> priorities
                                (filter #(< (:priority %) priority-cutoff))
                                (map :layer)
                                set)]
    (filter #(or (not (#{:habitat :bathymetry} (:category %)))
                 (priority-layer-ids (:id %)))
            layers)))

(def ^:private type->format-str {:map.layer.download/csv     "csv"
                                 :map.layer.download/shp     "shape-zip"
                                 :map.layer.download/geotiff "image/geotiff"})

(def ^:private type->servertype {:map.layer.download/csv     :wfs
                                 :map.layer.download/shp     :api
                                 :map.layer.download/geotiff :wms})

(defn download-type->str [type-key]
  (get {:map.layer.download/csv     "CSV"
        :map.layer.download/shp     "Shapefile"
        :map.layer.download/geotiff "GeoTIFF"}
       type-key))

(defmulti download-link (fn [_layer _bounds download-type] (type->servertype download-type)))

(defmethod download-link :api [{:keys [id] :as layer} bounds download-type]
  ;; At the moment we still use geoserver for CSV downloads (all), and
  ;; shp downloads of the entire data, ie when bounds arg is nil.
  (if-not bounds
    ((get-method download-link :wfs) layer bounds download-type)
    ;; Bugger, we need a way to get the base URL... might have to
    ;; scrap that since we don't have the db available.
    (let [base-url (str api-url-base "habitat/subset/")
          bounds-arg (->> bounds (bounds->projected wgs84->epsg3112) (bounds->str 3112))]
      (-> (url/url base-url)
          (assoc :query {:layer_id id
                         :bounds   bounds-arg
                         :format   "raw"})
          str))))

(defmethod download-link :wfs [{:keys [server_url detail_layer layer_name] :as _layer}
                               bounds
                               download-type]
  (-> (url/url server_url)
      (assoc :query {:service      "wfs"
                     :version      "1.1.0"
                     :request      "GetFeature"
                     :outputFormat (type->format-str download-type)
                     :typeName     (or detail_layer layer_name)
                     :srsName      "EPSG:4326"})
      ;; only include bbox when we're requesting a sub-region (there's
      ;; an issue where including the bbox, for the full region,
      ;; causes issues.  I don't think this was always the case, but
      ;; not investigating further)
      (merge-in (when bounds {:query {:bbox (bounds->str bounds)}}))
      str))

(defmethod download-link :wms [{:keys [server_url detail_layer layer_name bounding_box] :as _layer}
                               bounds
                               download-type]
  ;; Crude ratio calculations for approximating image dimensions (note, bbox could be param or layer extent):
  (let [{:keys [north south east west] :as bounds} (or bounds bounding_box)
        ratio (/ (- north south) (- east west))
        width 640]
    (-> (url/url server_url)
        (assoc :query {:service     "wms"
                       :version     "1.1.1"
                       :request     "GetMap"
                       :SRS         "EPSG:4326"
                       :transparent true
                       :bbox        (bounds->str bounds)
                       :format      (type->format-str download-type)
                       :width       width
                       :height      (int (* ratio width))
                       :layers      (or detail_layer layer_name)})
        str)))

(def feature-info-none
  {:info (str
          "<div>"
          "<h4>No info available</h4>"
          "Layer summary not configured"
          "</div>")})

(defn feature-info-html
  [response]
  (let [parsed (.parseFromString (js/DOMParser.) response "text/html")
        body  (first (array-seq (.querySelectorAll parsed "body")))]
    (if (.-firstElementChild body)
      {:info (.-innerHTML body)}
      {:status :feature-info/empty})))

(defn feature-info-json
  [response]
  (let [parsed (js->clj (.parse js/JSON response))
        id (get-in parsed ["features" 0 "id"])
        properties (map (fn [[label value]] {:label label :value value}) (get-in parsed ["features" 0 "properties"]))
        property-to-row (fn [{:keys [label value]}] (str "<tr><td>" label "</td><td>" value "</td></tr>"))
        property-rows (str/join "" (map (fn [property] (property-to-row property)) properties))]
    (if (or id (not-empty properties))
      {:info (str
              "<div class=\"feature-info-json\">"
              "<h4>" id "</h4>"
              "<table>" property-rows "</table>"
              "</div>")}
      {:status :feature-info/empty})))

(def info-format
  {1 "text/html"
   2 "application/json"
   3 nil})

(defn get-layers-info-format
  [layers]
  (let [info-formats (map (fn [layer] (:info_format_type layer)) layers)
        info-format (get info-format (apply max info-formats))]
    info-format))

(defn group-basemap-layers
  "Groups each basemap layer by their layer group"
  [layers groups]
  (let [layers-grouped (group-by :layer_group layers)
        
        ungrouped-layers (get layers-grouped nil) ; Extract the independent layers (those without a layer group)
        layers-grouped (dissoc layers-grouped nil)

        basemap-groups (reduce-kv (fn [acc key layers]
                                    (let [layers (sort-by (juxt #(or (:sort_key %) "zzzzzzzzzz") :id) layers) ; using sort key to sort layers into z-order
                                          bottom-layer (first layers)
                                          group (first (filter #(= (:id %) key) groups))]
                                      (conj acc
                                            (-> bottom-layer
                                                (merge group)
                                                (assoc :layers (drop 1 layers))))))
                                  [] layers-grouped)
        
        max-id (apply max (map :id basemap-groups)) ; using max id to avoid collisions with existing groups
        ungrouped-layers-groups (map-indexed (fn [idx layer]
                                               (-> layer
                                                   (assoc :id (+ max-id idx 1))
                                                   (assoc :layers [])))
                                             ungrouped-layers)
        basemap-groups (concat basemap-groups ungrouped-layers-groups)
        basemap-groups (sort-by (juxt #(or (:sort_key %) "zzzzzzzzzz") :id) basemap-groups)]
    basemap-groups))
