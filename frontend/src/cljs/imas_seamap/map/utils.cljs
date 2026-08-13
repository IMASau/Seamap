;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.set :as set]
            [goog.dom.xml :as gxml]
            [goog.object :as gobject]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [merge-in select-values first-where url? control->cql-filter]]
            ["proj4" :as proj4]
            [reagent.dom.server :refer [render-to-string]]
            [imas-seamap.interop.leaflet :as leaflet]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


(def ^:private projections
  {"EPSG:4326" (proj4 "+proj=longlat +datum=WGS84 +no_defs +type=crs")
   "EPSG:3857" (proj4 "+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs +type=crs")
   "EPSG:3112" (proj4 "+proj=lcc +lat_0=0 +lon_0=134 +lat_1=-18 +lat_2=-36 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs +type=crs")
   "EPSG:3031" (proj4 "+proj=stere +lat_0=-90 +lat_ts=-71 +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs +type=crs")})

(defn project-coords
  [coords crs]
  (let [projection (get projections crs)]
    (-> coords clj->js projection.forward js->clj)))

(defn bounds->projected [project-fn {:keys [north south east west] :as _bounds}]
  (let [[x0 y0] (project-fn [west south])
        [x1 y1] (project-fn [east north])]
    {:west  x0
     :south y0
     :east  x1
     :north y1}))

(defn bounds->str:wms
  "Prepare a string suitable for use in the BBOX parameter for *WMS* queries.
  Note that this has different semantics from WFS, and is version-dependent.
  https://docs.geoserver.org/latest/en/user/services/wms/basics.html#axis-ordering
  For now, we ignore the epsg-code if provided, and assume this is only used with version 1.1"
  ([bounds] (bounds->str:wms 4326 bounds))
  ([_epsg-code {:keys [north south east west] :as _bounds}]
   (assert (or (integer? _epsg-code) (string? _epsg-code)))
   (string/join "," [west south east north])))

(defn bounds->str:wfs
  "Prepare a string suitable for use in the BBOX parameter for *WFS* queries.
  Note that this has different semantics from WMS, and is version-dependent:
  https://docs.geoserver.org/latest/en/user/services/wfs/axis_order.html  "
  ([bounds] (bounds->str:wms 4326 bounds))
  ([epsg-code {:keys [north south east west] :as _bounds}]
   (assert (or (integer? epsg-code) (string? epsg-code)))
   (string/join "," [west south east north (if (integer? epsg-code) (str "EPSG:" epsg-code) epsg-code)])))

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

(defn has-time-dimension? [layer] (#{:wms-timeseries} (:layer_type layer)))

(defn ms-to-iso
  "Converts a Unix timestamp in milliseconds (milliseconds since the Unix epoch, 1970-01-01T00:00:00Z) into an ISO 8601 formatted UTC timestamp string.
   
   Args:
   * `epoch-milliseconds`: Milliseconds since the Unix epoch.
  
    Returns: ISO 8601 timestamp (e.g. `\"2026-05-01T06:27:10.336Z\"`)."
  [epoch-milliseconds]
  (-> epoch-milliseconds js/Date. .toISOString))

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
  [layers sorting-info]
  (letfn [(get-sort [ordering layer] (get-in sorting-info [ordering (ordering layer) 0]))] ; gets the sort key of a value in an ordering in the sorting-info
   (sort-by
    (juxt #(get-sort :category %) #(get-sort :data_classification %) :sort_key)
    #(< %1 %2) ; comparator so nil is always last (instead of first)
    layers)))

(def ^:private type->format-str {:map.layer.download/csv                 "csv"
                                 :map.layer.download/shp                 "shape-zip"
                                 :map.layer.download/geotiff-wms         "image/geotiff"
                                 :map.layer.download/geotiff-wcs         "image/geotiff"
                                 :map.layer.download/netcdf-thredds-wcs  "NetCDF3"
                                 :map.layer.download/geotiff-thredds-wcs "GeoTIFF"})

(def ^:private type->servertype {:map.layer.download/csv         :wfs
                                 :map.layer.download/shp         :api
                                 :map.layer.download/geotiff-wms :wms
                                 :map.layer.download/geotiff-wcs :wcs
                                 :map.layer.download/netcdf-thredds-wcs  :thredds-wcs
                                 :map.layer.download/geotiff-thredds-wcs :thredds-wcs})

(defn download-type->str [type-key]
  (get {:map.layer.download/csv                 "CSV"
        :map.layer.download/shp                 "Shapefile"
        :map.layer.download/geotiff-wms         "GeoTIFF"
        :map.layer.download/geotiff-wcs         "GeoTIFF"
        :map.layer.download/netcdf-thredds-wcs  "NetCDF"
        :map.layer.download/geotiff-thredds-wcs "GeoTIFF"}
       type-key))

(defmulti download-link (fn [_layer _bounds download-type _api-url-base _time] (type->servertype download-type)))

(defmethod download-link :api [{:keys [id] :as layer} bounds download-type api-url-base _time]
  ;; At the moment we still use geoserver for CSV downloads (all), and
  ;; shp downloads of the entire data, ie when bounds arg is nil.
  (if-not bounds
    ((get-method download-link :wfs) layer bounds download-type)
    (let [base-url (str api-url-base "habitat/subset/")
          bounds-arg (->> bounds (bounds->projected #(project-coords % "EPSG:3112")) (bounds->str:wfs 3112))]
      (-> (url/url base-url)
          (assoc :query {:layer_id id
                         :bounds   bounds-arg
                         :format   "raw"})
          str))))

(defmethod download-link :wfs [{:keys [server_url detail_layer layer_name] :as _layer}
                               bounds
                               download-type
                               _api-url-base
                               _time]
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
      (merge-in (when bounds {:query {:bbox (bounds->str:wfs bounds)}}))
      str))

(defmethod download-link :wms [{:keys [server_url detail_layer layer_name bounding_box] :as _layer}
                               bounds
                               download-type
                               _api-url-base
                               _time]
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
                       :bbox        (bounds->str:wms bounds)
                       :format      (type->format-str download-type)
                       :width       width
                       :height      (int (* ratio width))
                       :layers      (or detail_layer layer_name)})
        str)))
(defmethod download-link :wcs [{:keys [server_url detail_layer layer_name bounding_box] :as _layer}
                               bounds
                               download-type
                               _api-url-base
                               _time]
  (let [{:keys [north south east west]} bounds]
    (-> (url/url server_url)
        (assoc :query {:service     "WCS"
                       :version     "2.0.1"
                       :request     "GetCoverage"
                       :compression "DEFLATE"
                       :format      (type->format-str download-type)
                       :coverageId (or detail_layer layer_name)})
        (str (when bounds (str "&subset=Lat(" south "," north ")&subset=Long(" west "," east ")")))))) ; Add bounds params if provided. Can't be part of query dict because 'subset' param is used twice.

(defmethod download-link :thredds-wcs
  [{:keys [server_url detail_layer layer_name] :as _layer}
   bounds
   download-type
   _api-url-base
   time]
  (-> (url/url (string/replace server_url "/wms/" "/wcs/"))
      (assoc :query (merge
                     {:service     "WCS"
                      :version     "1.0.0"
                      :request     "GetCoverage"
                      :coverage    (or detail_layer layer_name)
                      :crs         "OGC:CRS84"
                      :format      (type->format-str download-type)}
                     (when bounds {:bbox (bounds->str:wms bounds)})
                     (when (= download-type :map.layer.download/geotiff-thredds-wcs)
                       {:time time})))
      str))

(defmulti feature-info-response->display
  "Converts a response and info format into readable information for the feature info popup"
  :info-format)

(defmethod feature-info-response->display "text/html"
  [{:keys [response _info-format _layers]}]
  (let [parsed (.parseFromString (js/DOMParser.) response "text/html")
        body  (first (array-seq (.querySelectorAll parsed "body")))
        style  (first (array-seq (.querySelectorAll parsed "style")))] ; only grabs the first style element
    (when (.-firstElementChild body)
      {:style (when style (.-innerHTML style))
       :body (.-innerHTML body)})))

(defmethod feature-info-response->display "application/json"
  [{:keys [response _info-format layers]}]
  (let [title           (->> layers
                             (map :name)
                             (interpose ", ")
                             (apply str))]
    (when (seq (get response "features"))
      {:style
       (str
        ".feature-info-json {"
        "    max-height: 257px;"
        "    overflow-y: auto;"
        "    width: 391px;"
        "}"

        ".feature-info-json table {"
        "    border-spacing: 0;"
        "    width: 100%;"
        "}"

        ".feature-info-json table:not(:last-child) {"
        "    margin-bottom: 10px;"
        "    padding-bottom: 10px;"
        "    border-bottom: 2px dashed rgb(235, 235, 235);"
        "}"

        ".feature-info-json tr:nth-child(odd) {"
        "    background-color: rgb(235, 235, 235);"
        "}"

        ".feature-info-json td {"
        "    padding: 3px 0px 3px 10px;"
        "    vertical-align: top;"
        "}"

        ".feature-info-json h4 {"
        "    width: 100%;"
        "    text-overflow: ellipsis;"
        "    overflow-x: hidden;"
        "}")
       :body
       (render-to-string
        [:div.feature-info-json
         [:h4 title]
         (map-indexed
          (fn [i feature]
            ^{:key i}
            [:table
             (map-indexed
              (fn [j [label value]]
                (let [value (if (seq (str value)) (str value) "-")
                      url?  (url? value)]
                  ^{:key j}
                  [:tr
                   [:td label]
                   [:td
                    (if url?
                      [:a {:href value :target "_blank"} value]
                      value)]]))
              (get feature "properties"))])
          (get response "features"))])})))

(defmethod feature-info-response->display "text/xml"
  [{:keys [response _info-format layers]}]
  (let [title (->> layers
                   (map :name)
                   (interpose ", ")
                   (apply str))
        doc (gxml/loadXml response)
        fields (gxml/selectNodes doc "/esri_wms:FeatureInfoResponse/esri_wms:FIELDS")]
    (when (seq fields)
      {:style
       (str
        ".feature-info-xml {"
        "    max-height: 257px;"
        "    overflow-y: auto;"
        "    width: 391px;"
        "}"

        ".feature-info-xml table {"
        "    border-spacing: 0;"
        "    width: 100%;"
        "}"

        ".feature-info-xml table:not(:last-child) {"
        "    margin-bottom: 10px;"
        "    padding-bottom: 10px;"
        "    border-bottom: 2px dashed rgb(235, 235, 235);"
        "}"

        ".feature-info-xml tr:nth-child(odd) {"
        "    background-color: rgb(235, 235, 235);"
        "}"

        ".feature-info-xml td {"
        "    padding: 3px 0px 3px 10px;"
        "    vertical-align: top;"
        "}"

        ".feature-info-xml h4 {"
        "    width: 100%;"
        "    text-overflow: ellipsis;"
        "    overflow-x: hidden;"
        "}")
       :body
       (render-to-string
        [:div.feature-info-xml
         [:h4 title]
         (map-indexed
          (fn [i node]
            ^{:key i}
            [:table
             (map-indexed
              (fn [j attr]
                (let [value (if (seq (str attr.value)) (str attr.value) "-")
                      url?  (url? value)]
                  ^{:key j}
                  [:tr
                   [:td attr.name]
                   [:td
                    (if url?
                      [:a {:href value :target "_blank"} value]
                      value)]]))
              node.attributes)])
          fields)])})))

(defmethod feature-info-response->display :default
  [{:keys [_info-format _response _layers]}]
  {:style nil
   :body
   (str
    "<div>"
    "    <h4>No info available</h4>"
    "    Layer summary not configured"
    "</div>")})

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

(defn latlng-distance [[lng1 lat1] [lng2 lat2]]
  (.distanceTo (leaflet/latlng. lat1 lng1) (leaflet/latlng. lat2 lng2)))

(defn map->bounds [{:keys [west south east north] :as _bounds}]
  [[south west]
   [north east]])

(defn latlng->vec [ll]
  (-> ll
      js->clj
      (select-values ["lat" "lng"])))

(defn bounds->map [bounds]
  {:north (. bounds getNorth)
   :south (. bounds getSouth)
   :east  (. bounds getEast)
   :west  (. bounds getWest)})

(defn leaflet-props [e]
  (let [m    (. e -target)
        zoom (. m getZoom)
        crs  (-> m .-options .-crs)]
    {:zoom   zoom
     :scale  (-> crs (.scale zoom))
     :crs    (.-code crs)
     :size   (-> m (. getSize) (js->clj :keywordize-keys true) (select-keys [:x :y]))
     :center (-> m (. getCenter) latlng->vec)
     :bounds (-> m (. getBounds) bounds->map)}))

(defn mouseevent->coords [e]
  (merge
   (-> e
       ;; Note need to round; fractional offsets (eg as in wordpress
       ;; navbar) cause fractional x/y which causes geoserver to
       ;; return errors in GetFeatureInfo
       (.. -containerPoint round)
       (js->clj :keywordize-keys true)
       (select-keys [:x :y]))
   (-> e
       (. -latlng)
       (js->clj :keywordize-keys true)
       (select-keys [:lat :lng]))))

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

(defn visible-layers
  "Shows only layers which should be visible from the map."
  [{:keys [hidden-layers active-layers]}]
  (remove #(hidden-layers %) active-layers))

#_(defn has-active-layers?
  "utility to simplify a check for any active layers (we want to disable
  some behaviour if there are no layers active, for example)"
  [{:keys [map] :as _db}]
  (-> map :active-layers seq))

(defn has-visible-habitat-layers?
  "Utility to simplify a check for any visible habitat layers."
  [{:keys [map] :as _db}]
  (->>
   (visible-layers map)
   (filter #(= (:category %) :habitat))
   seq boolean))

;;; --- Rich-layer context map ---
;;; All rich-layer utility functions take a `ctx` map instead of the full db.
;;; This makes data dependencies explicit and enables memoisation in the signal graph.
;;;
;;; ctx keys:
;;;   :layers-by-id       {id layer-map ...}        — from [:map :layers]
;;;   :rich-layers        [rich-layer ...]           — from [:map :rich-layers :rich-layers]
;;;   :rich-layers-by-id  {id rich-layer ...}        — derived from above
;;;   :rl-states          {rl-id state-map ...}      — from [:map :rich-layers :states]
;;;   :rl-async-datas     {rl-id async-data ...}     — from [:map :rich-layers :async-datas]
;;;   :rl-lookup          {layer-id rl-id ...}       — from [:map :rich-layers :layer-lookup]
;;;   :dynamic-pills      [dp ...]                   — from [:dynamic-pills :dynamic-pills]
;;;   :dp-states          {dp-id state ...}          — from [:dynamic-pills :states]
;;;   :dp-async-datas     {dp-id async-data ...}     — from [:dynamic-pills :async-datas]
;;;   :active-layers      [layer ...]                — from [:map :active-layers]
;;;   :open-pill          string-or-nil              — from [:display :open-pill]
;;;   :split-layer-container-x  number               — from [:display :split-layer-container-x]

(defn db->ctx
  "Builds a rich-layer context map from the full app-state db.
   Use this as a bridge in event handlers during migration."
  [db]
  (let [layers (get-in db [:map :layers])
        rich-layers (get-in db [:map :rich-layers :rich-layers])]
    {:layers-by-id      (into {} (map (juxt :id identity)) layers)
     :rich-layers       rich-layers
     :rich-layers-by-id (into {} (map (juxt :id identity)) rich-layers)
     :rl-states         (get-in db [:map :rich-layers :states])
     :rl-async-datas    (get-in db [:map :rich-layers :async-datas])
     :rl-lookup         (get-in db [:map :rich-layers :layer-lookup])
     :dynamic-pills     (get-in db [:dynamic-pills :dynamic-pills])
     :dp-states         (get-in db [:dynamic-pills :states])
     :dp-async-datas    (get-in db [:dynamic-pills :async-datas])
     :active-layers     (get-in db [:map :active-layers])
     :open-pill         (get-in db [:display :open-pill])
     :split-layer-container-x (get-in db [:display :split-layer-container-x])}))

(defn- resolve-layer
  "Resolve a layer id to a full layer map via the context."
  [{:keys [layers-by-id]} layer-id]
  (get layers-by-id layer-id))

(defn ->alternate-view [{layer-id :layer :as alternate-view} ctx]
  (assoc alternate-view :layer (resolve-layer ctx layer-id)))

(defn ->timeline [{layer-id :layer :as timeline} ctx]
  (assoc timeline :layer (resolve-layer ctx layer-id)))

(defn ->side-by-side-view [{layer-id :layer :as side-by-side-view} ctx]
  (assoc side-by-side-view :layer (resolve-layer ctx layer-id)))

(defn control->value [{:keys [cql-property controller-type default-value] :as _control} {:keys [id] :as _rich-layer} {:keys [rl-states rl-async-datas]}]
  (let [value  (get-in rl-states [id :controls cql-property :value])
        values (get-in rl-async-datas [id :controls cql-property :values])]
    (or
     value
     default-value
     (when (= controller-type "slider") (apply max values)))))

(defn control->value-map
  "Returns a map of the control's cql-property to its value."
  [{:keys [cql-property] :as control} rich-layer ctx]
  (let [value (control->value control rich-layer ctx)]
    {cql-property value}))

(defn control-is-default-value? [{:keys [cql-property controller-type default-value] :as control} {:keys [id] :as rich-layer} {:keys [rl-async-datas] :as ctx}]
  (let [value  (control->value control rich-layer ctx)
        values (get-in rl-async-datas [id :controls cql-property :values])]
    (boolean
     (or
      (= value default-value)
      (and (not default-value) (= controller-type "slider") (= value (apply max values)))))))

(defn remove-incompatible-combinations
  "Removes filter combinations that are incompatible with the current
   filter values."
  [filter-combinations {:keys [cql-property controller-type value]}]
  (if value
    (case controller-type
      "multi-dropdown"
      (filterv
       (fn [filter-combination]
         (or
          (not (seq value)) ; if no value, then all combinations are valid
          (some #(= % (get filter-combination cql-property)) (map #(when (not= % "None") %) value)))) ; if value, then only combinations with that value are valid ("None" is substituted back to nil)
       filter-combinations)

      (filterv #(= (get % cql-property) value) filter-combinations))
    filter-combinations))

(defn- ->control [{:keys [cql-property] :as control} {:keys [id controls] :as rich-layer} {:keys [rl-async-datas] :as ctx}]
  (let [values (get-in rl-async-datas [id :controls cql-property :values])
        value  (control->value control rich-layer ctx)
        other-controls
        (->>
         controls
         (remove #(= cql-property (:cql-property %)))
         (map #(assoc % :value (control->value % rich-layer ctx))))
        filter-combinations (get-in rl-async-datas [id :filter-combinations])
        valid-filter-combinations (reduce #(remove-incompatible-combinations %1 %2) filter-combinations other-controls)
        valid-values (set (map #(get % cql-property) valid-filter-combinations))]
    (assoc
     control
     :values
     (mapv
      (fn [value]
        (hash-map
         :value (or value "None") ; nil is substituted with "None" for the dropdown
         :valid? (boolean (some #(= % value) valid-values))))
      values)
     :value  value
     :is-default-value? (control-is-default-value? control rich-layer ctx)
     :cql-filter (control->cql-filter control value))))

(defn rich-layer->controls-value-map
  "Returns a map of the rich layer's controls' CQL propeties to their values.

   * `rich-layer: :map.rich-layers/rich-layer`: Rich layer to get the controls from
   * `ctx`: Rich-layer context map

   Example: `rich-layer` -> `{\"cql-property1\" 100 \"cql-property2\" 200}`"
  [rich-layer ctx]
  (s/assert :map.rich-layers/rich-layer rich-layer)
  (apply merge (map #(control->value-map % rich-layer ctx) (:controls rich-layer))))

(defn- assert-ctx!
  "Guard against passing the app db where a rich-layer ctx is expected (see
   db->ctx). Passing the db otherwise fails silently: destructuring the ctx keys
   yields nils, and every layer quietly resolves as \"not a rich layer\". This
   mistake has caused several real bugs when pre-refactor branches were merged.
   Dev builds only; compiled out of production."
  [ctx]
  (when ^boolean goog.DEBUG
    (assert (not (contains? ctx :map))
            "Expected a rich-layer ctx but got what looks like the app db; convert with db->ctx first")))

(defn enhance-rich-layer
  "Takes a rich-layer and enhances the info with other layer data."
  [{:keys [id layer-id slider-label alternate-views timeline side-by-side-views controls]
    :as rich-layer} {:keys [rl-states rl-async-datas rl-lookup rich-layers-by-id] :as ctx}]
  (assert-ctx! ctx)
  (let [{:keys [tab side-by-side-views-selected-id]
         alternate-views-selected-id :alternate-views-selected
         timeline-selected-id        :timeline-selected
         :as state}
        (get rl-states id)
        async-data                (get rl-async-datas id)
        layer                     (resolve-layer ctx layer-id)

        alternate-views              (mapv #(->alternate-view % ctx) alternate-views)
        alternate-views-selected     (first-where #(= (get-in % [:layer :id]) alternate-views-selected-id) alternate-views)
        alternate-view-rich-layer-id (get rl-lookup alternate-views-selected-id)
        alternate-view-rich-layer-id (when-not (= alternate-view-rich-layer-id id) alternate-view-rich-layer-id)
        alternate-view-rich-layer    (get rich-layers-by-id alternate-view-rich-layer-id)

        timeline                  (mapv #(->timeline % ctx) (or (:timeline alternate-view-rich-layer) timeline))
        timeline-selected         (first-where #(= (get-in % [:layer :id]) timeline-selected-id) timeline)
        slider-label              (or (:slider-label alternate-view-rich-layer) slider-label)
        displayed-layer           (:layer (or timeline-selected alternate-views-selected))

        controls                  (mapv #(->control % rich-layer ctx) controls)

        side-by-side-views          (mapv #(->side-by-side-view % ctx) side-by-side-views)
        ;; Note that the left layer of a side-by-side view isn't "selected" in the
        ;; traditional sense, as it's not determined at all by the side-by-side controls.
        ;; But we do want to check if the layer is in our side-by-side views layer list to
        ;; see if we can get a shorter "display name" to use for the labels on the split
        ;; view slider (ISA-696).
        side-by-side-views-left-selected    (first-where #(= (get-in % [:layer :id]) (or (:id displayed-layer) layer-id)) side-by-side-views)
        side-by-side-views-right-selected   (first-where #(= (get-in % [:layer :id]) side-by-side-views-selected-id) side-by-side-views)
        side-by-side-views-left-label-text  (or (:display_name side-by-side-views-left-selected) (:name layer))
        side-by-side-views-right-label-text (:display_name side-by-side-views-right-selected)

        cql-filter                (->>
                                   controls
                                   (map :cql-filter)
                                   (filter identity)
                                   (interpose " AND ")
                                   (apply str))]
    (when rich-layer
      (->
       rich-layer
       (merge state)
       (merge async-data)
       (assoc
        :layer                    layer
        :tab                      (or tab "legend")
        :controls                 controls
        :alternate-views          alternate-views
        :alternate-views-selected alternate-views-selected
        :timeline                 timeline
        :timeline-selected        timeline-selected
        :timeline-disabled?       (boolean (and alternate-views-selected (not (:timeline alternate-view-rich-layer))))
        :slider-label             slider-label
        :displayed-layer          displayed-layer
        :side-by-side-views          side-by-side-views
        :side-by-side-views-selected side-by-side-views-right-selected
        :side-by-side-views-left-label-text  side-by-side-views-left-label-text
        :side-by-side-views-right-label-text side-by-side-views-right-label-text
        :cql-filter                 cql-filter)))))

(defn layer->rich-layer [{:keys [id] :as _layer} {:keys [rich-layers-by-id rl-lookup] :as ctx}]
  (assert-ctx! ctx)
  (let [rich-layer-id (get rl-lookup id)]
    (get rich-layers-by-id rich-layer-id)))

(defn layer->rich-layer?
  "True if a layer is a rich layer, otherwise false."
  [{:keys [id] :as _layer} {:keys [rl-lookup] :as ctx}]
  (assert-ctx! ctx)
  (boolean (get rl-lookup id)))

; FIXME: This function should be removed at some point. It's very data-munging.
(defn rich-layer->displayed-layer
  "If a layer is a rich-layer, then return the currently displayed layer (including
   default if no alternate view or timeline selected). If layer is not a
   rich-layer, then the layer is just returned."
  [layer ctx]
  (let [rich-layer (enhance-rich-layer (layer->rich-layer layer ctx) ctx)]
    (or (:displayed-layer rich-layer) layer)))

; FIXME: Ditto
(defn rich-layer->side-by-side-views-selected-layer
  "If a layer is a rich-layer, then return the currently displayed side-by-side
   view selected layer.
   Nil if no side-by-side view is selected, or if the layer is not a rich-layer."
  [layer ctx]
  (let [rich-layer (enhance-rich-layer (layer->rich-layer layer ctx) ctx)]
    (get-in rich-layer [:side-by-side-views-selected :layer])))

(defn rich-layer->side-by-side-views-selected
  "If a layer is a rich-layer with a currently visible split layer, then return
   that split layer."
  [layer ctx]
  (let [{:keys [side-by-side-views-selected]} (enhance-rich-layer (layer->rich-layer layer ctx) ctx)]
    (when side-by-side-views-selected (:layer side-by-side-views-selected))))

(defn rich-layer-children->parents
  [layers rich-layer-children]
  (reduce
   (fn [acc val]
     (let [parents (get rich-layer-children val)] ; get the rich-layer parents for this layer
       (->
        acc
        (conj val)             ; add the layer into the set
        (set/union parents)))) ; add the layer's rich-layer parents into the set (if any exist)
   #{} layers))

(defn rich-layer->layer-under-point
  "Takes a rich layer and coordinates and returns the actual displayed layer under
   that point. This pulls complicated \"what layer is the user actually clicking\"
   logic for rich layers out from the \"feature-info-dispatcher\" event, leaving
   that a bit neater."
  [rich-layer {:keys [x] :as _point} {:keys [split-layer-container-x] :as ctx}]
  (let [enhanced-rich-layer (enhance-rich-layer rich-layer ctx)
        side-by-side-views-selected-layer (get-in enhanced-rich-layer [:side-by-side-views-selected :layer])]
    (if (and side-by-side-views-selected-layer (> x split-layer-container-x)) ; if we have a split view and we click on the right side of the split view...
      side-by-side-views-selected-layer                                       ; ...return the split view layer...
      (or                                                                     ; ...else...
       (:displayed-layer enhanced-rich-layer)                                 ; ...return the "displayed layer" if we have it...
       (:layer enhanced-rich-layer)))))                                       ; ...else return the default layer

; Extracted function from the monolithic map-layers sub so that it can be used
; (sparingly) in events.
; Hopefully with some refactoring of map-layers, it won't be necessary hand off
; the entire db to this utility. At the same time, we can remove/refactor the use
; of the "enhance-rich-layer" utility.
(defn rich-layer-fn
  "Function that gets an \"enchanced\" rich layer from a layer passed to it"
  [db]
  (let [ctx (db->ctx db)]
    #(enhance-rich-layer (layer->rich-layer % ctx) ctx)))

; Extracted function from a sub so that it can be used (sparingly) in events.
(defn layer-displayed-layers-lookup
  "A lookup map of the raw (catalogue) layer to what layers should actually be
   displayed on the map."
  [layers rich-layer-fn]
  (reduce
   (fn [m layer]
     (assoc m layer (or (:displayed-layer (rich-layer-fn layer)) layer)))
   {} layers))

; TODO: Refactor so that `db` isn't a necessary argument
(defn displayed-layers-under-point
  "From the list of visible layers on the map, get the layers displayed on the map
   under the current point.

   The current point can matter for things like split view layers."
  [visible-layers point db]
  (let [ctx (db->ctx db)]
    (map
     (fn [layer]
       (if (layer->rich-layer? layer ctx)
         (rich-layer->layer-under-point (layer->rich-layer layer ctx) point ctx)
         layer))
     visible-layers)))

(defn layer->dynamic-pills
  "Returns the dynamic pills for a layer."
  [{:keys [id] :as _layer} {:keys [dynamic-pills]}]
  (filter
   (fn [{:keys [layers] :as _dynamic-pill}]
     (some #{id} (set (map :layer layers))))
   dynamic-pills))

(defn ->dynamic-pill [{:keys [id region-control] :as dynamic-pill} {:keys [layers-by-id active-layers dp-states dp-async-datas open-pill] :as ctx}]
  (let [value (get-in dp-states [id :region-control :value])
        active-layers
        (let [dp-layer-ids (set (map :layer (:layers dynamic-pill)))
              active-ids   (set/intersection dp-layer-ids (set (map :id active-layers)))]
          (keep layers-by-id active-ids))
        displayed-layers
        (map #(rich-layer->displayed-layer % ctx) active-layers)
        displayed-rich-layer-filters (mapv #(rich-layer->controls-value-map (layer->rich-layer % ctx) ctx) displayed-layers)
        active-layers-metadata (map (fn [layer] (:metadata (first-where #(= (:layer %) (:id layer)) (:layers dynamic-pill)))) active-layers)]
    (->
     dynamic-pill
     (merge (get dp-states id))
     (merge (get dp-async-datas id))
     (assoc
      :region-control
      (->
       region-control
       (merge (get-in dp-states [id :region-control]))
       (merge (get-in dp-async-datas [id :region-control]))))
     (assoc
      :expanded?
      (=
       open-pill
       (str "dynamic-pill-" id)))
     (assoc :active-layers active-layers)
     (assoc :active-layers-metadata active-layers-metadata)
     (assoc :displayed-layers displayed-layers)
     (assoc :cql-filter (control->cql-filter region-control value))
     (assoc :displayed-rich-layer-filters displayed-rich-layer-filters))))

(defn layer->cql-filter
  "Returns the CQL filter for a layer."
  [{layer-cql-filter :filter :as layer} ctx]
  (let [rich-layer-cql-filter     (:cql-filter (enhance-rich-layer (layer->rich-layer layer ctx) ctx)) ; string or nil
        layer-cql-filter          (:filter (rich-layer->displayed-layer layer ctx))
        dynamic-pills-cql-filters (filter identity (map #(:cql-filter (->dynamic-pill % ctx)) (layer->dynamic-pills layer ctx))) ; list of strings
        cql-filters
        (cond-> dynamic-pills-cql-filters
          (seq rich-layer-cql-filter) (conj rich-layer-cql-filter) ; if rich-layer cql filter exists, add it
          (seq layer-cql-filter)      (conj layer-cql-filter))     ; if layer cql filter exists, add it
        cql-filter (apply str (interpose " AND " cql-filters))] ; combine with AND
    (when (seq cql-filter) cql-filter))) ; return nil if no filter

(defn- get-divider-x
  "Gets the current x-coordinate (Leaflet container point) of the side-by-side view divider. nil if no divider is active"
  [db]
  (get-in db [:display :split-layer-container-x]))

(defn which-side-of-divider
  "Accepts a lat-lon point and returns :left, :right, and nil depending on if the point is on left or right side of the side-by-side divider (nil if no divider)"
  [{:keys [lat lng] :as _point} db]
  (when-let [leaflet-map (get-in db [:map :leaflet-map])]
    (let [divider-x (get-divider-x db)
          point-x (-> leaflet-map (.latLngToContainerPoint (leaflet/latlng. lat lng)) .-x)]
      (when divider-x
        (if (> point-x divider-x) :right :left)))))

(defn popup-visible?
  "Is there a popup visibly open on the map?
   If there's no popup (i.e. user hasn't clicked on the map for a feature popup
   *or* they have clicked on the map but then dismissed the feature popup) then
   this is false.
   If the user *has* opened a feature popup, but it's obscured because it's
   currently on the wrong side of a side-by-side view divider, then this is also
   false.
   If there is a popup the user has opened, and they can currently see it, and
   they haven't yet dismissed it, then this is true."
  [db]
  (boolean
   (when-let [{:keys [location show? side-of-divider]} (:feature db)]
    (let [current-side-of-divider (which-side-of-divider location db)]
      (and show? (= current-side-of-divider side-of-divider))))))

(defn- make-re
  "Given a list of words to match, construct a regexp that matches all
  of them, in any order.  That is, [\"one\" \"two\"] should match both
  \"onetwo\" and \"twoone\"."
  [words]
  (re-pattern
   (str "(?i)^"
        (string/join (map #(str "(?=.*" % ")") words))
        ".*$")))

(defn match-layer
  "Given a string of search words, attempt to match them *all* against
  a layer (designed so it can be used to filter a list of layers, in
  conjunction with partial)."
  [filter-text categories layer]
  (if-let [search-re (try
                       (-> filter-text string/trim (string/split #"\s+") make-re)
                       (catch :default e nil))]
    (re-find search-re (layer-search-keywords categories layer))
    false))

;;; Seamap is hosted under https, meaning the browser will block ajax
;;; (ie, getfeatureinfo) requests to plain http URLs.  Servers still
;;; using http need specil handling:
(defn is-insecure? [url] (-> url string/lower-case (string/starts-with? "http:")))
