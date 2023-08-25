;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.utils
  (:require [clojure.set :refer [rename-keys]]
            [goog.crypt.base64 :as b64]
            [cognitect.transit :as t]
            [imas-seamap.utils :refer [select-keys*]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn encode-state
  "Returns a string suitable for storing in the URL's hash"
  [{:keys [story-maps] map-state :map :as db}]
  (let [pruned-rich-layers
        (reduce-kv
         (fn [acc key {:keys [alternate-views-selected timeline-selected tab] :as _val}]
           (let [pruned-rich-layer
                 (cond-> {:tab tab}
                   alternate-views-selected (assoc :alternate-views-selected alternate-views-selected)
                   timeline-selected        (assoc :timeline-selected timeline-selected))]
             (assoc acc key pruned-rich-layer)))
         {} (:rich-layers map-state))
        
        pruned-map (-> (select-keys map-state [:center :zoom :active-layers :active-base-layer :viewport-only? :bounds])
                       (rename-keys {:active-layers :active :active-base-layer :active-base})
                       (update :active (partial map :id))
                       (update :active-base :id)
                       (assoc :rich-layers pruned-rich-layers))
        pruned-story-maps (-> (select-keys story-maps [:featured-map :open?])
                              (update :featured-map :id))
        db         (-> db
                       (select-keys* [[:display :sidebar :selected]
                                      [:display :catalogue :main]
                                      [:display :left-drawer]
                                      [:display :left-drawer-tab]
                                      [:filters :layers]
                                      :layer-state
                                      [:transect :show?]
                                      [:transect :query]
                                      :autosave?])
                       (assoc :map pruned-map)
                       (assoc :story-maps pruned-story-maps))
        legends    (->> db :layer-state :legend-shown (map :id))
        opacities  (->> db :layer-state :opacity (reduce (fn [acc [k v]] (if (= v 100) acc (conj acc [(:id k) v]))) {}))
        db*        (-> db
                       (dissoc :layer-state)
                       (assoc :legend-ids legends)
                       (assoc :opacity-ids opacities))]
    (b64/encodeString (t/write (t/writer :json) db*))))

(defn- filter-state
  "Given a state map, presumably from the hashed state, filter down to
  only expected/allowed paths to prevent injection attacks."
  [state]
  (select-keys* state
                [[:display :sidebar :selected]
                 [:display :catalogue :main]
                 [:display :left-drawer]
                 [:display :left-drawer-tab]
                 [:filters :layers]
                 [:story-maps :featured-map]
                 [:story-maps :open?]
                 [:transect :show?]
                 [:transect :query]
                 [:map :active]
                 [:map :active-base]
                 [:map :center]
                 [:map :zoom]
                 [:map :bounds]
                 [:map :viewport-only?]
                 [:map :rich-layers]
                 :legend-ids
                 :opacity-ids
                 :autosave?
                 :config]))

(defn parse-state [hash-str]
  (try
    (let [decoded   (b64/decodeString hash-str)
          reader    (t/reader :json)]
      (->> decoded
           (t/read reader)
           filter-state))
    (catch js/Object e {})))

(defn ajax-loaded-info
  "Returns db of all the info retrieved via ajax"
  [db]
  (let [rich-layers
        (reduce-kv
         (fn [acc key val]
           (assoc
            acc
            key
            (assoc
             val
             :alternate-views-selected nil
             :timeline-selected        nil
             :tab                      "legend")))
         {} (get-in db [:map :rich-layers]))]
    (->
     (select-keys*
      db
      [[:map :layers]
       [:map :base-layers]
       [:map :base-layer-groups]
       [:map :grouped-base-layers]
       [:map :organisations]
       [:map :categories]
       [:map :keyed-layers]
       [:map :leaflet-map]
       [:map :legends]
       [:map :rich-layer-children]
       [:story-maps :featured-maps]
       :habitat-colours
       :habitat-titles
       :sorting
       :config])
     (assoc-in [:map :rich-layers] rich-layers))))
