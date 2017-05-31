(ns imas-seamap.map.subs
  (:require [clojure.string :as string]
            [imas-seamap.utils :refer [bbox-intersects?]]
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
  [filter-text {:keys [name layer_name description] :as layer}]
  (let [layer-text (string/join " " [name layer_name description])
        search-re  (-> filter-text string/trim (string/split #"\s+") make-re)]
    (re-find search-re layer-text)))

(defn map-layers [db _]
  (let [{:keys [layers active-layers bounds]} (get-in db [:map])
        filter-text                           (get-in db [:filters :layers])
        visible-layers                        (filter #(bbox-intersects? bounds (:bounding_box %)) layers)
        filtered-layers                       (filter (partial match-layer filter-text) visible-layers)]
    {:groups        (group-by :category filtered-layers)
     :active-layers active-layers}))

(defn map-layer-logic [db _]
  (get-in db [:map :logic]
          {:type :map.layer-logic/automatic
           :trigger :map.logic.trigger/automatic}))

(defn map-layers-filter [db _]
  (get-in db [:filters :layers]))
