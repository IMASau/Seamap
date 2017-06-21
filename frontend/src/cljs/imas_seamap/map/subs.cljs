(ns imas-seamap.map.subs
  (:require [clojure.string :as string]
            [imas-seamap.map.utils :refer [bbox-intersects?]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))

(defn map-props [db _] (:map db))

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
  [filter-text {:keys [name layer_name description organisation data_classification] :as layer}]
  (let [layer-text (string/join " " [name layer_name description organisation data_classification])
        search-re  (-> filter-text string/trim (string/split #"\s+") make-re)]
    (re-find search-re layer-text)))

(defn map-layers [{:keys [map filters] :as db} _]
  (let [{:keys [layers active-layers bounds]} (get-in db [:map])
        filter-text                           (get-in db [:filters :layers])
        filter-text-others                    (get-in db [:filters :other-layers])
        visible-layers                        (filter #(bbox-intersects? bounds (:bounding_box %)) layers)
        {:keys [third-party]}                 (group-by :category visible-layers)
        filtered-layers                       (filter (partial match-layer filter-text) visible-layers)]
    ;; We have separate filters for main layers, and third-party -- so
    ;; take the third-party group before filtering, and filter it
    ;; separately:
    {:groups        (assoc (group-by :category filtered-layers)
                           :third-party
                           (filter (partial match-layer filter-text-others) third-party))
     :active-layers active-layers}))

(defn map-current-priorities
  "Return the layer-group-priorities that are applicable for the
  current zoom level, etc"
  [{{:keys [groups priorities bounds zoom zoom-cutover]} :map :as db} _]
  (let [detail-resolution? (< zoom-cutover zoom)
        group-ids (->> groups
                       (filter (fn [{:keys [bounding_box detail_resolution]}]
                                 (and (= detail_resolution detail-resolution?)
                                      (bbox-intersects? bounds bounding_box))))
                       (map :id)
                       set)]
    (filter #(group-ids (:group %)) priorities)))

(defn map-layer-logic [db _]
  (get-in db [:map :logic]
          {:type    :map.layer-logic/automatic
           :trigger :map.logic.trigger/automatic}))

(defn map-layers-filter [db _]
  (get-in db [:filters :layers]))

(defn map-other-layers-filter [db _]
  (get-in db [:filters :other-layers]))
