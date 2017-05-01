(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]
            [ajax.core :as ajax]))


(def ^:private test-layer-data
  "Just for offline testing"
  [{:name "National" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NAT_CoastalWaterways_geomorphic" :category "habitat" :bounding_box "109.55225803197766,-44.06642289001932,156.87812778669465,-9.808712193457605" :metadata_url "" :description "" :server_type "geoserver" :date_start "" :date_end "" :detail_resolution false}
   {:name "NSW" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "seamap:SeamapAus_NSW_ocean_ecosystems_2002" :category "habitat" :bounding_box "148.67929118103902,-37.557900847858605,155.62623003773396,-28.01778192721625" :metadata_url "" :description "" :server_type "geoserver" :date_start "" :date_end "" :detail_resolution true}
   {:name "Tasmania" :server_url "http://geoserver.imas.utas.edu.au/geoserver/wms?" :layer_name "cite:SEAMAP_habitats_Geo" :category "habitat" :bounding_box "144.78718799604167,-43.598255819880706,148.4394445006124,-39.36679642738305" :metadata_url "" :description "" :server_type "geoserver" :date_start "" :date_end "" :detail_resolution true}])

(defn str->bounds [bounds-str]
  (as-> bounds-str bnds
    (string/split bnds ",")
    (map js/parseFloat bnds)
    (map vector [:west :south :east :north] bnds)
    (into {} bnds)))

(defn bounds->str [{:keys [north south east west] :as bounds}]
  (string/join "," [south west north east]))

(defn get-feature-info [{:keys [db] :as context} [_ {:keys [size bounds] :as props} {:keys [x y] :as point}]]
  ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
  (merge {:db db}
         (when-not (boolean (get-in db [:map :controls :transect]))
           (let [params {:REQUEST "GetFeatureInfo"
                         :LAYERS "imas:NERP_NBarrett_Flinders_CMR_AUV_GV"
                         :QUERY_layers "imas:NERP_NBarrett_Flinders_CMR_AUV_GV"
                         :WIDTH (:x size)
                         :HEIGHT (:y size)
                         :BBOX (bounds->str bounds)
                         :X x
                         :Y y
                         :I x
                         :J y
                         :SRS "EPSG:4326"
                         :CRS "EPSG:4326"
                         :FORMAT "text/html"
                         :INFO_format "text/html"
                         :SERVICE "WMS"
                         :VERSION "1.3.0"}]
             {:http-xhrio {:method :get
                           :uri "http://geoserver.imas.utas.edu.au/geoserver/wms"
                           :params params
                           :response-format (ajax/text-response-format)
                           :on-success [:map/got-featureinfo point]
                           :on-failure [:ajax/default-err-handler]}}))))

(defn got-feature-info [db [_ point response]]
  (let [zipped (->> response xml/parse-str zip/xml-zip)
        body (zx/xml1-> zipped :html :body)]
    (if (-> body zx/text string/blank?)
      (assoc db :feature nil)
      (assoc db :feature
             {:location point
              :info (xml/emit-str (zip/node body))}))))

(defn process-layer [layer]
  (-> layer
      ;; TODO: convert the dates, etc too
      (update :bounding_box str->bounds)
      (update :category (comp keyword string/lower-case))
      (update :server_type (comp keyword string/lower-case))))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn update-layers [db [_ layers]]
  (->> layers ;test-layer-data
       process-layers
       (assoc-in db [:map :layers])))

(defn toggle-layer [db [_ layer]]
  (update-in db [:map :active-layers]
             #(if (% layer)
                (disj % layer)
                (conj % layer))))

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [db [_ {:keys [bounding_box] :as layer}]]
  (-> db
      (assoc-in [:map :bounds] bounding_box)
      (update-in [:map :active-layers] conj layer)))

(defn layer-visible? [{:keys [west south east north] :as bounds}
                      {:keys [bounding_box]          :as layer}]
  (not (or (> (:south bounding_box) north)
           (< (:north bounding_box) south)
           (> (:west  bounding_box) east)
           (< (:east  bounding_box) west))))

(defn visible-layers [{:keys [west south east north] :as bounds} layers]
  (filter (partial layer-visible? bounds) layers))

(defn habitat-layer? [layer] (-> layer :category (= :habitat)))

(defn update-active-layers
  "Utility to recalculate layers that are displayed.  When the
  viewport or zoom changes, we may need to switch out a layer for a
  coarser/finer resolution one.  Only applies to habitat layers."
  [{{:keys [layers active-layers zoom zoom-cutover bounds]} :map :as db}]
  ;; Basic idea:
  ;; * check that any habitat layer is currently displayed (ie, don't start with no habitats, then zoom in and suddenly display one!)
  ;; * filter out habitat layers from actives
  ;; * add back in those that are visible, and past the zoom cutoff
  ;; * assoc back onto the db
  (let [{active-habitats  true
         filtered-actives false} (group-by habitat-layer? active-layers)]
    (if (seq active-habitats)
      (let [display-more-detail? (> zoom zoom-cutover)
            {detailed-habitats   true
             national-resolution false} (->> layers
                                             (filter habitat-layer?)
                                             (group-by :detail_resolution))
            visible-detailed (visible-layers bounds detailed-habitats)]
        (assoc-in db [:map :active-layers]
                  (->> (if (and display-more-detail? (seq visible-detailed))
                         visible-detailed
                         (take 1 national-resolution))
                       (into filtered-actives)
                       (into #{}))))
      db)))

(defn map-view-updated [db [_ {:keys [zoom center bounds]}]]
  (-> db
      (update-in [:map] assoc
                 :zoom zoom
                 :center center
                 :bounds bounds)
      update-active-layers))
