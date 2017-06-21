(ns imas-seamap.map.utils
  (:require [imas-seamap.utils :refer [bbox-intersects?]]))


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
         sort
         (map last))))




