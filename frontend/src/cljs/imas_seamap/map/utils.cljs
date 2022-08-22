;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [imas-seamap.db :refer [api-url-base]]
            [imas-seamap.utils :refer [merge-in first-where]]
            ["proj4" :as proj4]
            [clojure.string :as str]
            [imas-seamap.interop.leaflet :as leaflet]
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
  (let [category-display-name (get-in categories [category :display_name])
        organisation          (or organisation "Ungrouped")
        data_classification   (or data_classification "Ungrouped")]
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
   (juxt #(get-in categories [(:category %) :display_name]) :category :data_classification :id)
   #(< %1 %2) ; comparator so nil is always last (instead of first)
   layers))

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
  {:style nil
   :body
   (str
    "<div>"
    "    <h4>No info available</h4>"
    "    Layer summary not configured"
    "</div>")})

(defn feature-info-html
  [response]
  (let [parsed (.parseFromString (js/DOMParser.) response "text/html")
        body  (first (array-seq (.querySelectorAll parsed "body")))
        style  (first (array-seq (.querySelectorAll parsed "style")))] ; only grabs the first style element
    (when (.-firstElementChild body)
      {:style (when style (.-innerHTML style))
       :body (.-innerHTML body)})))

(defn feature-info-json
  [response]
  (let [parsed (js->clj (.parse js/JSON response))
        id (get-in parsed ["features" 0 "id"])
        properties (map (fn [[label value]] {:label label :value value}) (get-in parsed ["features" 0 "properties"]))
        property-to-row (fn [{:keys [label value]}] (str "<tr><td>" label "</td><td>" value "</td></tr>"))
        property-rows (str/join "" (map (fn [property] (property-to-row property)) properties))]
    (when (or id (not-empty properties))
      {:style
       (str
        ".feature-info-json {"
        "    max-height: 257px;"
        "    overflow-y: scroll;"
        "}"

        ".feature-info-json table {"
        "    border-spacing: 0;"
        "}"

        ".feature-info-json tr:nth-child(odd) {"
        "    background-color: rgb(235, 235, 235);"
        "}"

        ".feature-info-json td {"
        "    padding: 3px 0px 3px 10px;"
        "    vertical-align: top;"
        "}")
       :body
       (str
        "<div class=\"feature-info-json\">"
        "    <h4>" id "</h4>"
        "    <table>" property-rows "</table>"
        "</div>")})))

(defn sort-by-sort-key
  "Sorts a collection by its sort-key first and its id second."
  [coll]
  (sort-by (juxt #(or (:sort_key %) "zzzzzzzzzz") :id) coll))

(defn normal-latitude
  "Latitude can get pretty wacky if one loops around the entire globe a few times.
   This puts the latitude within the normal latitude range."
  [lat]
  (-> lat
   (+ 180)
   (mod 360)
   (- 180)))

(defn normal-bounds
  "Latitude can get pretty wacky if one loops around the entire globe a few times.
   This puts the bounds within the normal latitude range."
  [{:keys [west _south east _north] :as bounds}]
  (let [east (normal-latitude east)
        west (normal-latitude west)
        east (if (< east west) (+ east 360) east)]
    (assoc bounds :east east :west west)))

(defn layer-visible? [bounds {:keys [bounding_box] :as _layer}]
  (let [{:keys [west south east north]} (normal-bounds bounds)]
    (or
     (empty? bounds) ; if no bounds assume we can see the whole map
     (not (or (> (:south bounding_box) north)
              (< (:north bounding_box) south)
              (> (:west  bounding_box) east)
              (< (:east  bounding_box) west))))))

(defn viewport-layers [{:keys [_west _south _east _north] :as bounds} layers]
  (filter (partial layer-visible? bounds) layers))

(defn latlng-distance [[lat1 lng1] [lat2 lng2]]
  (.distanceTo (leaflet/latlng. lat1 lng1) (leaflet/latlng. lat2 lng2)))

(defn map->bounds [{:keys [west south east north] :as _bounds}]
  [[south west]
   [north east]])
