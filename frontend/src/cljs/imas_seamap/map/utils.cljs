(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [oops.core :refer [gcall ocall]]
            [debux.cs.core :refer-macros [dbg]]))


(def ^:private *epsg-3112*
  (gcall "proj4"
         "+proj=lcc +lat_1=-18 +lat_2=-36 +lat_0=0 +lon_0=134 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"))

(defn wgs84->epsg3112 [pt]
  ;; pt is a vector of [lon lat]
  (js->clj
   (ocall *epsg-3112* :forward (clj->js pt))))

(defn bounds->str
  ([bounds] (bounds->str bounds 4326))
  ([{:keys [north south east west] :as bounds} epsg-code]
   (assert (integer? epsg-code))
   (string/join "," [south west north east (str "urn:ogc:def:crs:EPSG:" epsg-code)])))

(defn bbox-intersects? [b1 b2]
  (not
   (or (> (:west b1)  (:east b2))
       (< (:east b1)  (:west b2))
       (> (:south b1) (:north b2))
       (< (:north b1) (:south b2)))))

(defn habitat-layer? [layer] (-> layer :category (= :habitat)))

(def ^:private -category-ordering
  (into {} (map vector [:bathymetry :habitat :imagery :third-party :boundaries] (range))))

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
                                 :map.layer.download/shp     :wfs
                                 :map.layer.download/geotiff :wms})

(defmulti download-link (fn [layer bounds download-type] (type->servertype download-type)))

(defmethod download-link :wfs [{:keys [server_url detail_layer layer_name] :as layer} bounds download-type]
  (-> (url/url (or detail_layer server_url))
      (assoc :query {:service      "wfs"
                     :version      "1.1.0"
                     :request      "GetFeature"
                     :outputFormat (type->format-str download-type)
                     :typeName     layer_name
                     :srsName      "EPSG:4326"
                     :bbox         (bounds->str bounds)})
      str))

(defmethod download-link :wms [{:keys [server_url detail_layer layer_name] :as layer}
                               {:keys [north south east west] :as bounds}
                               download-type]
  ;; Crude ratio calculations for approximating image dimensions:
  (let [ratio (/ (- north south) (- east west))
        width 640]
    (-> (url/url (or detail_layer server_url))
        (assoc :query {:service "wms"
                       :version "1.3.0"
                       :request "GetMap"
                       :SRS     "EPSG:4326"
                       :bbox    (bounds->str bounds)
                       :format  (type->format-str download-type)
                       :width   width
                       :height  (int (* ratio width))
                       :layers  layer_name})
        str)))

