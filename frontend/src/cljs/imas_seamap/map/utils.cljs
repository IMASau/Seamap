;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [imas-seamap.db :refer [api-url-base]]
            [imas-seamap.utils :refer [merge-in]]
            [oops.core :refer [gcall ocall]]
            [debux.cs.core :refer-macros [dbg]]))


(def ^:private EPSG-3112
  (gcall "proj4"
         "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(defn wgs84->epsg3112 [pt]
  ;; pt is a vector of [lon lat]
  (js->clj
   (ocall EPSG-3112 :forward (clj->js pt))))

(defn bounds->projected [project-fn {:keys [north south east west] :as bounds}]
  (let [[x0 y0] (wgs84->epsg3112 [west south])
        [x1 y1] (wgs84->epsg3112 [east north])]
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
  (not
   (or (> (:west b1)  (:east b2))
       (< (:east b1)  (:west b2))
       (> (:south b1) (:north b2))
       (< (:north b1) (:south b2)))))

(defn habitat-layer? [layer] (-> layer :category (= :habitat)))

(def ^:private -category-ordering
  ;; Note, bathymetry is only displayed with other categories when
  ;; it's a contour layer, so it can go over the top of habitats
  ;; because they'll be visible underneath:
  (into {} (map vector [:habitat :bathymetry :imagery :third-party :boundaries] (range))))

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
         (map (fn [{:keys [category id] :as layer}]
                [(-category-ordering category) (layer-priority id) layer]))
         (sort-by (comp vec (partial take 2)))
         (map last))))

(defn applicable-layers
  [{{:keys [layers groups priorities priority-cutoff zoom zoom-cutover bounds logic]} :map :as db}
   & {:keys [bbox category server_type]
      :or   {bbox bounds}}]
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
        selected-layers    (filter #(and (layer-ids (:id %)) (match-category? %) (match-server? %)) layers)]
    (sort-layers selected-layers priorities logic-type)))

(defn all-priority-layers
  "Return the list of priority layers: that is, every layer for which
  its priority in *some* group is higher than the priority-cutoff.
  This only applies to habitat and bathymetry layers; other categories
  aren't handled via priorities and are always included."
  [{{:keys [layers priorities priority-cutoff]} :map :as db}]
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

(defmulti download-link (fn [layer bounds download-type] (type->servertype download-type)))

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

(defmethod download-link :wfs [{:keys [server_url detail_layer layer_name bounding_box] :as layer}
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

(defmethod download-link :wms [{:keys [server_url detail_layer layer_name bounding_box] :as layer}
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

