(ns imas-seamap.map.subs
  (:require [clojure.string :as string]
            [imas-seamap.map.utils :refer [bbox-intersects? all-priority-layers]]
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

(defn map-layers [{:keys [map layer-state filters] :as db} _]
  (let [{:keys [layers active-layers bounds logic]} (get-in db [:map])
        filter-text                                 (get-in db [:filters :layers])
        filter-text-others                          (get-in db [:filters :other-layers])
        layers                                      (if (= (:type logic) :map.layer-logic/automatic)
                                                      (all-priority-layers db)
                                                      layers)
        visible-layers                              (filter #(bbox-intersects? bounds (:bounding_box %)) layers)
        {:keys [third-party]}                       (group-by :category visible-layers)
        filtered-layers                             (filter (partial match-layer filter-text) visible-layers)]
    ;; We have separate filters for main layers, and third-party -- so
    ;; take the third-party group before filtering, and filter it
    ;; separately:
    {:groups         (assoc (group-by :category filtered-layers)
                            :third-party
                            (filter (partial match-layer filter-text-others) third-party))
     :loading-layers (->> layer-state (filter (fn [[l [st _]]] (= st :map.layer/loading))) keys set)
     :error-layers   (->> layer-state (filter (fn [[l [_ errors?]]] errors?)) keys set)
     :active-layers  active-layers}))

(defn map-layer-priorities [db _]
  (get-in db [:map :priorities]))

(defn map-layer-logic [db _]
  (get-in db [:map :logic]
          {:type    :map.layer-logic/automatic
           :trigger :map.logic.trigger/automatic}))

(defn map-layers-filter [db _]
  (get-in db [:filters :layers]))

(defn map-other-layers-filter [db _]
  (get-in db [:filters :other-layers]))

(defn map-layer-lookup [db _]
  (reduce
   (fn [acc {:keys [server_url layer_name] :as layer}]
     (assoc acc [server_url layer_name] layer))
   {}
   (get-in db [:map :layers])))
