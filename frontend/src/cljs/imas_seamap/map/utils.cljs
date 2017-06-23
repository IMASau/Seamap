(ns imas-seamap.map.utils
  (:require [debux.cs.core :refer [dbg]]))


(defn bbox-intersects? [b1 b2]
  (not
   (or (> (:west b1)  (:east b2))
       (< (:east b1)  (:west b2))
       (> (:south b1) (:north b2))
       (< (:north b1) (:south b2)))))

(defn habitat-layer? [layer] (-> layer :category (= :habitat)))

(def ^:private -category-ordering
  (into {} (map vector [:bathymetry :habitat :imagery :third-party] (range))))

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
   & {:keys [bbox category]
      :or   {bbox bounds}}]
  ;; Generic utility to retrieve a list of relevant layers, filtered as necessary.
  ;; figures out cut-off point, and restricts to those on the right side of it;
  ;; filters to those groups intersecting the bbox;
  ;; sorts by priority and grouping.
  (let [logic-type         (:type logic)
        match-category?    #(if category (= category (:category %)) true)
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
        selected-layers    (filter #(and (layer-ids (:id %)) (match-category? %)) layers)]
    (sort-layers selected-layers priorities logic-type)))



