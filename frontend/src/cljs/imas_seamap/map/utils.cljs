;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.utils
  (:require [cemerick.url :as url]
            [clojure.string :as string]
            [clojure.set :as set]
            [goog.dom.xml :as gxml]
            [cljs.spec.alpha :as s]
            [imas-seamap.utils :refer [merge-in select-values first-where url? control->cql-filter ids->layers]]
            ["proj4" :as proj4]
            [reagent.dom.server :refer [render-to-string]]
            [imas-seamap.interop.leaflet :as leaflet]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


(def ^:private projections
  {"EPSG:4326" (proj4 "+proj=longlat +datum=WGS84 +no_defs +type=crs")
   "EPSG:3857" (proj4 "+proj=merc +a=6378137 +b=6378137 +lat_ts=0 +lon_0=0 +x_0=0 +y_0=0 +k=1 +units=m +nadgrids=@null +wktext +no_defs +type=crs")
   "EPSG:3112" (proj4 "+proj=lcc +lat_0=0 +lon_0=134 +lat_1=-18 +lat_2=-36 +x_0=0 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs +type=crs")})

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

;;; Note, the namespace format (",EPSG:4326") used here has important
;;; correlation with the WFS version used; see
;;; https://docs.geoserver.org/latest/en/user/services/wfs/axis_order.html
(defn bounds->str
  ([bounds] (bounds->str 4326 bounds))
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

(defmulti download-link (fn [_layer _bounds download-type _api-url-base] (type->servertype download-type)))

(defmethod download-link :api [{:keys [id] :as layer} bounds download-type api-url-base]
  ;; At the moment we still use geoserver for CSV downloads (all), and
  ;; shp downloads of the entire data, ie when bounds arg is nil.
  (if-not bounds
    ((get-method download-link :wfs) layer bounds download-type)
    (let [base-url (str api-url-base "habitat/subset/")
          bounds-arg (->> bounds (bounds->projected #(project-coords % "EPSG:3112")) (bounds->str 3112))]
      (-> (url/url base-url)
          (assoc :query {:layer_id id
                         :bounds   bounds-arg
                         :format   "raw"})
          str))))

(defmethod download-link :wfs [{:keys [server_url detail_layer layer_name] :as _layer}
                               bounds
                               download-type
                               _api-url-base]
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
                               download-type
                               _api-url-base]
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
        fields)])}))

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
  (let [m (. e -target)]
    {:zoom   (. m getZoom)
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

(defn ->alternate-view [{layer-id :layer :as alternate-view} db]
  (let [layers (get-in db [:map :layers])
        layer  (first-where #(= (:id %) layer-id) layers)]
    (assoc alternate-view :layer layer)))

(defn ->timeline [{layer-id :layer :as timeline} db]
  (let [layers (get-in db [:map :layers])
        layer  (first-where #(= (:id %) layer-id) layers)]
    (assoc timeline :layer layer)))

(defn control->value [{:keys [cql-property controller-type default-value] :as _control} {:keys [id] :as _rich-layer} {alternate-view-rich-layer-id :id :as _alternate-view-rich-layer} db]
  (let [value  (get-in db [:map :rich-layers :states id :controls cql-property :value])
        values (get-in db [:map :rich-layers :async-datas (or alternate-view-rich-layer-id id) :controls cql-property :values])]
    (or
     value
     default-value
     (when (= controller-type "slider") (apply max values)))))

(defn control->value-map
  "Returns a map of the control's cql-property to its value."
  [{:keys [cql-property] :as control} rich-layer alternate-view-rich-layer db]
  (let [value (control->value control rich-layer alternate-view-rich-layer db)]
    {cql-property value}))

(defn control-is-default-value? [{:keys [cql-property controller-type default-value] :as control} {:keys [id] :as rich-layer} {alternate-view-rich-layer-id :id :as alternate-view-rich-layer} db]
  (let [value  (control->value control rich-layer alternate-view-rich-layer db)
        values (get-in db [:map :rich-layers :async-datas (or alternate-view-rich-layer-id id) :controls cql-property :values])]
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

(defn- ->control [{:keys [cql-property] :as control} rich-layer {alternate-view-rich-layer-id :id :as alternate-view-rich-layer} db]
  (let [values (get-in db [:map :rich-layers :async-datas alternate-view-rich-layer-id :controls cql-property :values])
        value  (control->value control rich-layer alternate-view-rich-layer db)
        other-controls
        (->>
         (or (:controls alternate-view-rich-layer) (:controls rich-layer))
         (remove #(= cql-property (:cql-property %)))
         (map #(assoc % :value (control->value % rich-layer alternate-view-rich-layer db))))
        filter-combinations (get-in db [:map :rich-layers :async-datas alternate-view-rich-layer-id :filter-combinations])
        valid-filter-combinations (reduce #(remove-incompatible-combinations %1 %2) filter-combinations other-controls)
        valid-values (set (map #(get % cql-property) valid-filter-combinations))
        values
        (mapv
         (fn [value]
           (hash-map
            :value (or value "None") ; nil is substituted with "None" for the dropdown
            :valid? (boolean (some #(= % value) valid-values))))
         values)
        
        values (if (and value (not (first-where #(= (:value %) value) values))) (conj values {:value value :valid? false}) values)]
    (assoc
     control
     :values values
     :value  value
     :is-default-value? (control-is-default-value? control rich-layer alternate-view-rich-layer db)
     :cql-filter (control->cql-filter control value))))

(defn rich-layer->controls-value-map
  "Returns a map of the rich layer's controls' CQL propeties to their values.

   * `rich-layer: :map.rich-layers/rich-layer`: Rich layer to get the controls from
   * `db: :seamap/app-state`: Seamap app state

   Example: `rich-layer` -> `{\"cql-property1\" 100 \"cql-property2\" 200}`"
  [rich-layer alternate-view-rich-layer db]
  (s/assert :map.rich-layers/rich-layer rich-layer)
  (apply merge (map #(control->value-map % rich-layer alternate-view-rich-layer db) (:controls rich-layer))))

(defn enhance-rich-layer
  "Takes a rich-layer and enhances the info with other layer data."
  [{:keys [id slider-label alternate-views timeline controls]
    :as rich-layer} db]
  (let [{:keys [tab]
         alternate-views-selected-id :alternate-views-selected
         timeline-selected-id        :timeline-selected
         :as state}
        (get-in db [:map :rich-layers :states id])
        async-data                (get-in db [:map :rich-layers :async-datas id])

        alternate-views              (mapv #(->alternate-view % db) alternate-views)
        alternate-views-selected     (first-where #(= (get-in % [:layer :id]) alternate-views-selected-id) alternate-views)
        alternate-view-rich-layer-id (get-in db [:map :rich-layers :layer-lookup alternate-views-selected-id])
        alternate-view-rich-layer-id (when-not (= alternate-view-rich-layer-id id) alternate-view-rich-layer-id)
        alternate-view-rich-layer    (first-where #(= (:id %) alternate-view-rich-layer-id) (get-in db [:map :rich-layers :rich-layers]))

        timeline                  (mapv #(->timeline % db) (or (:timeline alternate-view-rich-layer) timeline))
        timeline-selected         (first-where #(= (get-in % [:layer :id]) timeline-selected-id) timeline)
        slider-label              (or (:slider-label alternate-view-rich-layer) slider-label)

        controls                  (mapv #(->control % rich-layer alternate-view-rich-layer db) (if alternate-view-rich-layer (:controls alternate-view-rich-layer) controls))

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
        :tab                      (or tab "legend")
        :controls                 controls
        :alternate-views          alternate-views
        :alternate-views-selected alternate-views-selected
        :timeline                 timeline
        :timeline-selected        timeline-selected
        :timeline-disabled?       (boolean (and alternate-views-selected (not (:timeline alternate-view-rich-layer))))
        :slider-label             slider-label
        :displayed-layer          (:layer (or timeline-selected alternate-views-selected))
        :cql-filter               cql-filter)))))

(defn layer->rich-layer [{:keys [id] :as _layer} db]
  (let [{:keys [rich-layers layer-lookup]} (get-in db [:map :rich-layers])
        rich-layer-id (get layer-lookup id)]
    (first-where #(= (:id %) rich-layer-id) rich-layers)))

(defn rich-layer->displayed-layer
  "If a layer is a rich-layer, then return the currently displayed layer (including
   default if no alternate view or timeline selected). If layer is not a
   rich-layer, then the layer is just returned."
  [layer db]
  (let [rich-layer (enhance-rich-layer (layer->rich-layer layer db) db)]
    (or (:displayed-layer rich-layer) layer)))

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

(defn layer->dynamic-pills
  "Returns the dynamic pills for a layer."
  [{:keys [id] :as _layer} db]
  (let [dynamic-pills (get-in db [:dynamic-pills :dynamic-pills])]
    (filter
     (fn [{:keys [layers] :as _dynamic-pill}]
       (some #{id} (set (map :layer layers))))
     dynamic-pills)))

(defn ->dynamic-pill [{:keys [id region-control] :as dynamic-pill} {{:keys [active-layers layers]} :map :as db}]
  (let [value (get-in db [:dynamic-pills :states id :region-control :value])
        active-layers
        (ids->layers
         (set/intersection
          (set (map :layer (:layers dynamic-pill)))
          (set (map :id active-layers)))
         layers)
        displayed-layers
        (map #(rich-layer->displayed-layer % db) active-layers)

        displayed-rich-layer-filters
        (mapv
         (fn [displayed-layer]
           (let [rich-layer (layer->rich-layer displayed-layer db)

                 alternate-view-rich-layer-id (get-in db [:map :rich-layers :layer-lookup (:alternate-views-selected rich-layer)])
                 alternate-view-rich-layer-id (when-not (= alternate-view-rich-layer-id id) alternate-view-rich-layer-id)
                 alternate-view-rich-layer    (first-where #(= (:id %) alternate-view-rich-layer-id) (get-in db [:map :rich-layers :rich-layers]))]
             (rich-layer->controls-value-map rich-layer alternate-view-rich-layer db)))
         displayed-layers)

        active-layers-metadata (map (fn [layer] (:metadata (first-where #(= (:layer %) (:id layer)) (:layers dynamic-pill)))) active-layers)] ; get the metadata for the active layers, forming a list of the same arity
    (->
     dynamic-pill
     (merge (get-in db [:dynamic-pills :states id]))
     (merge (get-in db [:dynamic-pills :async-datas id]))
     (assoc
      :region-control
      (->
       region-control
       (merge (get-in db [:dynamic-pills :states id :region-control]))
       (merge (get-in db [:dynamic-pills :async-datas id :region-control]))))
     (assoc
      :expanded?
      (=
       (get-in db [:display :open-pill])
       (str "dynamic-pill-" id)))
     (assoc :active-layers active-layers)
     (assoc :active-layers-metadata active-layers-metadata)
     (assoc :displayed-layers displayed-layers)
     (assoc :cql-filter (control->cql-filter region-control value))
     (assoc :displayed-rich-layer-filters displayed-rich-layer-filters))))

(defn layer->cql-filter
  "Returns the CQL filter for a layer."
  [layer db]
  (let [rich-layer-cql-filter (:cql-filter (enhance-rich-layer (layer->rich-layer layer db) db))
        dynamic-pills-cql-filters (filter identity (map #(:cql-filter (->dynamic-pill % db)) (layer->dynamic-pills layer db)))
        cql-filters (if rich-layer-cql-filter (conj dynamic-pills-cql-filters rich-layer-cql-filter) dynamic-pills-cql-filters)
        cql-filter (apply str (interpose " AND " cql-filters))]
    (when (seq cql-filter) cql-filter)))
