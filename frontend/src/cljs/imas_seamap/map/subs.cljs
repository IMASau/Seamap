(ns imas-seamap.map.subs
  (:require [clojure.string :as string]
            [imas-seamap.map.utils :refer [bbox-intersects? all-priority-layers wgs84->epsg3112]]
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
        ;; Ignore filtering etc for boundary layers:
        boundaries                                  (->> layers
                                                         (filter #(= :boundaries (:category %)))
                                                         (sort-by :name))
        filter-text                                 (get-in db [:filters :layers])
        layers                                      (if (= (:type logic) :map.layer-logic/automatic)
                                                      (all-priority-layers db)
                                                      layers)
        visible-layers                              (filter #(bbox-intersects? bounds (:bounding_box %)) layers)
        {:keys [third-party]}                       (group-by :category visible-layers)
        filtered-layers                             (filter (partial match-layer filter-text) visible-layers)]
    {:groups         (assoc (group-by :category filtered-layers)
                            :boundaries boundaries)
     :loading-layers (->> layer-state (filter (fn [[l [st _]]] (= st :map.layer/loading))) keys set)
     :error-layers   (->> layer-state (filter (fn [[l [_ errors?]]] errors?)) keys set)
     :active-layers  active-layers}))

(defn layer-selection-info [db _]
  {:selecting? (boolean (get-in db [:map :controls :download :selecting]))
   :region     (get-in db [:map :controls :download :bbox])})

(defn region-stats [{:keys [region-stats] :as db} _]
  ;; The selected habitat layer for region-stats, providing it is
  ;; active; default selection if there's a single habitat layer:
  (let [habitat-layers (filter #(= :habitat (:category %)) (get-in db [:map :active-layers]))]
    (cond
      (= 1 (count habitat-layers)) {:habitat-layer (first habitat-layers)}
      (some #{(:habitat-layer region-stats)} habitat-layers) region-stats)))

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

(defn map-layer-extra-params-fn
  "Creates a function that returns a map of additional WMS parameters
  for a given layer argument."
  [db _]
  (let [region-stats?                  (= "tab-management" (get-in db [:display :sidebar :selected]))
        {:keys [lng lat] :as location} (get-in db [:feature :location])
        habitat-layer                  (get-in db [:region-stats :habitat-layer])
        boundary-layer                 (->> db :map :active-layers (filter #(= :boundaries (:category %))) first)
        info-layer                     (get-in db [:display :info-card :layer])]
    (cond
      ;; Region-stats mode; if:
      ;; * tab is "management", and
      ;; * we have a selected habitat layer, and
      ;; * there's a mouse-click
      ;; Then add a CQL filter for that click-location, and a translucency for /other/ habitat layers
      (and region-stats? location habitat-layer boundary-layer)
      (fn [layer]
        (let [click-pt (wgs84->epsg3112 [lng lat])]
          (cond
            (= layer boundary-layer) {:CQL_FILTER (str "CONTAINS(geom,POINT("
                                                       (string/join " " click-pt)
                                                       "))")}
            (= layer habitat-layer)  nil
            :default                 {:opacity 0.1})))

      ;; Displaying info-card for a layer?  Fade-out the others (note
      ;; need to ensure we don't touch the CQL_FILTER if it's a
      ;; boundary layer, or we'll trigger layer-load events):
      info-layer
      (fn [layer]
        (cond
          (= layer info-layer)              nil
          (= :boundaries (:category layer)) {:CQL_FILTER "INCLUDE" :opacity 0.1}
          :default                          {:opacity 0.1}))

      ;; Everything else; reset the CQL filter for boundaries,
      ;; otherwise leave the default opacity (and everything else)
      :default
      (fn [layer]
        (when (= :boundaries (:category layer))
          {:CQL_FILTER "INCLUDE"})))))

(defn organisations
  "Sub to access organisations; overloaded to return a single org
  specified by name."
  [{{:keys [organisations]} :map} [_ org-name]]
  (if org-name
    (some #(and (= org-name (:name %)) %) organisations)
    organisations))
