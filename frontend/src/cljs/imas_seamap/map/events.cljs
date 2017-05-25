(ns imas-seamap.map.events
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]
            [ajax.core :as ajax]))


(defn bounds->str [{:keys [north south east west] :as bounds}]
  (string/join "," [south west north east]))

(defn get-feature-info [{:keys [db] :as context} [_ {:keys [size bounds] :as props} {:keys [x y] :as point}]]
  ;; FIXME: We assume that all img layers are on the same server
  (let [img-layers (->> db :map :active-layers (filter #(= :imagery (:category %))))
        server-url (-> img-layers first :server_url)
        layers (->> img-layers (map :layer_name) (string/join ","))]
   (merge {:db db}
          (when-not (boolean (get-in db [:map :controls :transect]))
            ;; http://docs.geoserver.org/stable/en/user/services/wms/reference.html#getfeatureinfo
            (let [params {:REQUEST "GetFeatureInfo"
                          :LAYERS layers
                          :QUERY_layers layers
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
                            :uri server-url
                            :params params
                            :response-format (ajax/text-response-format)
                            :on-success [:map/got-featureinfo point]
                            :on-failure [:ajax/default-err-handler]}})))))

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
      (update :category (comp keyword string/lower-case))
      (update :server_type (comp keyword string/lower-case))))

(defn process-layers [layers]
  (mapv process-layer layers))

(defn update-layers [db [_ layers]]
  (->> layers
       process-layers
       (assoc-in db [:map :layers])))

(defn toggle-layer [{:keys [db]} [_ layer]]
  {:db (update-in db [:map :active-layers]
                  #(if (% layer)
                     (disj % layer)
                     (conj % layer)))
   ;; If someone triggers this, we also switch to manual mode:
   :dispatch [:map.layers.logic/manual]})

(defn zoom-to-layer
  "Zoom to the layer's extent, adding it if it wasn't already."
  [{:keys [db]} [_ {:keys [bounding_box] :as layer}]]
  {:db (-> db
           (assoc-in [:map :bounds] bounding_box)
           (update-in [:map :active-layers] conj layer))
   ;; If someone triggers this, we also switch to manual mode:
   :dispatch [:map.layers.logic/manual]})

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
  "Utility to recalculate layers that are displayed when automatic
  layer-switching is in place.  When the viewport or zoom changes, we
  may need to switch out a layer for a coarser/finer resolution one.
  Only applies to habitat layers."
  [{{:keys [layers active-layers zoom zoom-cutover bounds logic]} :map :as db}]
  ;; Basic idea:
  ;; * check that any habitat layer is currently displayed (ie, don't start with no habitats, then zoom in and suddenly display one!)
  ;; * filter out habitat layers from actives
  ;; * add back in those that are visible, and past the zoom cutoff
  ;; * assoc back onto the db
  (let [{active-habitats  true
         filtered-actives false} (group-by habitat-layer? active-layers)]
    (if (and (= logic :map.layer-logic/automatic)
             (seq active-habitats))
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

(defn map-layer-logic-manual [db _]
  (assoc-in db [:map :logic] :map.layer-logic/manual))

(defn map-layer-logic-automatic [db _]
  (assoc-in db [:map :logic] :map.layer-logic/automatic))

(defn map-layer-logic-toggle [{:keys [map] :as db} _]
  (if (= :map.layer-logic/manual
         (:logic map))
    (assoc-in db [:map :logic] :map.layer-logic/automatic)
    (assoc-in db [:map :logic] :map.layer-logic/manual)))

(defn map-view-updated [db [_ {:keys [zoom center bounds]}]]
  (-> db
      (update-in [:map] assoc
                 :zoom zoom
                 :center center
                 :bounds bounds)
      update-active-layers))
