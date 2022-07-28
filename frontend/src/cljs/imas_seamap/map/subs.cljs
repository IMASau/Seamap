;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.map.subs
  (:require [clojure.string :as string]
            [imas-seamap.utils :refer [map-on-key]]
            [imas-seamap.map.utils :refer [bbox-intersects? all-priority-layers region-stats-habitat-layer layer-search-keywords sort-layers]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

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
  [filter-text categories layer]
  (if-let [search-re (try
                       (-> filter-text string/trim (string/split #"\s+") make-re)
                       (catch :default e nil))]
    (re-find search-re (layer-search-keywords categories layer))
    false))

(defn- make-error-fn
  "Given maps of layer->error-count and layer->total-tile-count, returns
  a function that takes a layer and returns a boolean indicating if
  the layer is problematic or not (ie, rather than just saying we
  should notify the user about any error, we want to notify above a
  certain threshold)"
  [error-counts load-counts]
  (fn [layer]
    (let [error-count (get error-counts layer 0)
          total-count (get load-counts layer 0)]
      (and (pos? total-count)
           (> (/ error-count total-count)
              0.4)))))       ; Might be nice to make this configurable eventually

(defn map-layers [{:keys [layer-state filters map] :as db} _]
  (let [{:keys [layers active-layers hidden-layers bounds logic]} map
        categories      (map-on-key (:categories map) :name)
        filter-text     (:layers filters)
        layers (filter #(get-in categories [(:category %) :display_name]) layers) ; only layers with a category that has a display name are allowed
        layers          (if (= (:type logic) :map.layer-logic/automatic)
                          (all-priority-layers db)
                          layers)
        filtered-layers (filter (partial match-layer filter-text categories) layers)]
    {:groups          (group-by :category filtered-layers)
     :loading-layers  (->> layer-state :loading-state (filter (fn [[l st]] (= st :map.layer/loading))) keys set)
     :error-layers    (make-error-fn (:error-count layer-state) (:tile-count layer-state))
     :expanded-layers (->> layer-state :legend-shown set)
     :active-layers   active-layers
     :visible-layers  (filter (fn [layer] (not (contains? hidden-layers layer))) active-layers)
     :layer-opacities (fn [layer] (get-in layer-state [:opacity layer] 100))
     :filtered-layers filtered-layers}))

(defn map-base-layers [{:keys [map]} _]
  (select-keys map [:grouped-base-layers :active-base-layer]))

(defn display-categories
  "Filter categories to only those that have a display name and at least one layer."
  [{:keys [map]} _]
  (let [{:keys [layers categories]} map
        grouped-layers              (group-by :category layers)]
    (filter (fn [{:keys [display_name name]}] (and display_name (seq (name grouped-layers)))) categories)))

(defn categories-map [db _]
  (let [categories (get-in db [:map :categories])]
    (map-on-key categories :name)))

(defn layer-selection-info [db _]
  {:selecting? (boolean (get-in db [:map :controls :download :selecting]))
   :region     (get-in db [:map :controls :download :bbox])})

(defn region-stats [db _]
  ;; The selected habitat layer for region-stats, providing it is
  ;; active; default selection if there's a single habitat layer:
  {:habitat-layer (region-stats-habitat-layer db)})

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

;; TODO: Remove, unused - related to getting boundary and habitat region stats
#_(defn map-layer-extra-params-fn
  "Creates a function that returns a map of additional WMS parameters
  for a given layer argument."
  [db _]
  (let [region-stats?                  (= "tab-management" (get-in db [:display :sidebar :selected]))
        {:keys [lng lat] :as location} (get-in db [:feature :location])
        habitat-layer                  (region-stats-habitat-layer db)
        boundary-layer                 (->> db :map :active-layers (filter #(= :boundaries (:category %))) first)
        info-layer                     (get-in db [:display :info-card :layer])]
    (cond
      ;; Region-stats mode; if:
      ;; * tab is "management", and
      ;; * we have a selected habitat layer, and
      ;; * there's a mouse-click
      ;; Then add a filter for that click-location, and a translucency for /other/ habitat layers
      ;; (Note, regular FILTER so we can specify a CRS and just use 4326; we have both 3112 and 4326 boundaries)
      (and region-stats? location habitat-layer boundary-layer)
      (fn [layer]
        (cond
          (= layer boundary-layer) {:FILTER (str "<Filter xmlns=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\">"
                                                 "<Contains><PropertyName>geom</PropertyName>"
                                                 "<gml:Point srsName=\"EPSG:4326\"><gml:coordinates>"
                                                 lng "," lat
                                                 "</gml:coordinates></gml:Point></Contains></Filter>")}
          (= layer habitat-layer)  nil
          :else                    {:opacity 0.1}))

      ;; Displaying info-card for a layer?  Fade-out the others (note
      ;; need to ensure we don't touch the FILTER if it's a boundary
      ;; layer, or we'll trigger layer-load events):
      info-layer
      (fn [layer]
        (cond
          (= layer info-layer)              nil
          (= :boundaries (:category layer)) {:FILTER "" :opacity 0.1}
          :else                             {:opacity 0.1}))

      ;; Everything else; reset the CQL filter for boundaries,
      ;; otherwise leave the default opacity (and everything else)
      :else
      (fn [layer]
        (when (= :boundaries (:category layer))
          {:FILTER ""})))))

(defn organisations
  "Sub to access organisations; overloaded to return a single org
  specified by name."
  [{{:keys [organisations]} :map} [_ org-name]]
  (if org-name
    (some #(and (= org-name (:name %)) %) organisations)
    organisations))
