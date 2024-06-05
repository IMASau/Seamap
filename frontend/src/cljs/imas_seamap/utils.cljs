;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.utils
  (:require [cemerick.url :as url]
            [clojure.set :refer [index rename-keys]]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [goog.crypt.base64 :as b64]
            [cognitect.transit :as t]
            [imas-seamap.blueprint :as b]
            ["copy-to-clipboard" :as copy-to-clipboard]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

;;; Taken from https://github.com/district0x/district-cljs-utils/
(letfn [(merge-in* [a b]
          (if (map? a)
            (merge-with merge-in* a b)
            b))]
  (defn merge-in
    "Merge multiple nested maps."
    [& args]
    (reduce merge-in* nil args)))

(defn select-keys*
  "select-keys, but allows nested selection using vector paths."
  [m paths]
  (apply merge-in
        (map #(cond
                (nil? %)              nil
                (not (sequential? %)) {% (get m %)}
                (= 1 (count %))       {(first %) (get m (first %))}
                :else                 {(first %) (select-keys* (get m (first %)) [(rest %)])})
             paths)))

;;; http://blog.jayfields.com/2011/01/clojure-select-keys-select-values-and.html
(defn select-values [map ks]
  (reduce #(conj %1 (map %2)) [] ks))

(defn -equalise [[k v]] (str (name k) "=" v))

(defn encode-state
  "Returns a string suitable for storing in the URL's hash"
  [{:keys [story-maps] map-state :map {boundaries-state :boundaries statistics-state :statistics} :state-of-knowledge :as db}]
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
        pruned-boundaries (select-keys*
                           boundaries-state
                           [[:active-boundary]
                            [:active-boundary-layer]
                            [:amp :active-network]
                            [:amp :active-park]
                            [:amp :active-zone]
                            [:amp :active-zone-iucn]
                            [:amp :active-zone-id]
                            [:imcra :active-provincial-bioregion]
                            [:imcra :active-mesoscale-bioregion]
                            [:meow :active-realm]
                            [:meow :active-province]
                            [:meow :active-ecoregion]])
        pruned-statistics (select-keys*
                           statistics-state
                           [[:habitat :show-layers?]
                            [:bathymetry :show-layers?]
                            [:habitat-observations :show-layers?]])
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
                                      [:feature :location]
                                      [:feature :leaflet-props]
                                      :autosave?])
                       (assoc :map pruned-map)
                       (assoc-in [:state-of-knowledge :boundaries] pruned-boundaries)
                       (assoc-in [:state-of-knowledge :statistics] pruned-statistics)
                       (assoc :story-maps pruned-story-maps)
                       #_(update-in [:display :catalogue :expanded] #(into {} (filter second %))))
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
                 [:state-of-knowledge :boundaries :active-boundary]
                 [:state-of-knowledge :boundaries :active-boundary-layer]
                 [:state-of-knowledge :boundaries :amp :active-network]
                 [:state-of-knowledge :boundaries :amp :active-park]
                 [:state-of-knowledge :boundaries :amp :active-zone]
                 [:state-of-knowledge :boundaries :amp :active-zone-iucn]
                 [:state-of-knowledge :boundaries :amp :active-zone-id]
                 [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion]
                 [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion]
                 [:state-of-knowledge :boundaries :meow :active-realm]
                 [:state-of-knowledge :boundaries :meow :active-province]
                 [:state-of-knowledge :boundaries :meow :active-ecoregion]
                 [:state-of-knowledge :statistics :habitat :show-layers?]
                 [:state-of-knowledge :statistics :bathymetry :show-layers?]
                 [:state-of-knowledge :statistics :habitat-observations :show-layers?]
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
                 [:feature :location]
                 [:feature :leaflet-props]
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

(defn ids->layers [ids layers]
  ;; Preserve the ordering of ids (these come from the serialised state, so dictates the stacking order)
  ;; For efficiency, create a lookup-by-id map:
  (let [id-layer-map (->> layers
                          (map #(vector (:id %) %))
                          (into {}))]
    (map id-layer-map ids)))

(defn geonetwork-force-xml
  "Turn a Geonetwork record URL into the xml-downoad equivalent for the
  same record.  Assume Geonetwork 3, and handles URLs in the two formats
  https://metadata.imas.utas.edu.au/geonetwork/srv/eng/catalog.search#/metadata/4739e4b0-4dba-4ec5-b658-02c09f27ab9a
  and
  https://metadata.imas.utas.edu.au/geonetwork/srv/api/records/4739e4b0-4dba-4ec5-b658-02c09f27ab9a,
  returning the format
  https://metadata.imas.utas.edu.au/geonetwork/srv/api/records/4739e4b0-4dba-4ec5-b658-02c09f27ab9a/formatters/xml"
  [geonetwork-url]
  (let [url            (url/url geonetwork-url)
        update-service #(update % :path str "/formatters/xml")]
    (if (string/includes? geonetwork-url "catalog.search")
      ;; Hash format; reconstruct:
      (let [uuid (-> url :anchor (subs 10))]
        (-> url
            (update :path string/replace #"eng?/catalog.search$" "api/records/")
            (update :path str uuid)
            update-service
            (assoc :anchor nil)
            str))
      ;; Regular format (/srv/api/records/<uuid>); switch to XML format:
      (-> url
          update-service
          str))))

(defn copy-text [text]
  (.then
   (js/navigator.clipboard.writeText text)
   nil
   #(copy-to-clipboard text)))

(defn append-params-from-map
  [url params]
  (reduce-kv (fn [acc key val] (str acc "?" (name key) "=" val)) url params))

(defn append-query-params
  [url params]
  (let [params (map (fn [[key val]] (str (name key) "=" val)) params)
        params (apply str (interpose "&" params))]
    (str url "?" params)))

(defn uuid4?
  [val]
  (re-matches #"^[0-9a-f]{8}\-[0-9a-f]{4}\-4[0-9a-f]{3}\-[89ab][0-9a-f]{3}\-[0-9a-f]{12}$" val))

(defn map-on-key
  "Takes a seq of hashmaps and converts it to a hashmap of hashmaps, where the key
   for each hashmap is a value within that hashmap.
   
   Params:
    - seq: sequence of hashmaps we're converting to a hashmap.
    - key: key for the hashmaps whose value corresponds to that hashmap's key."
  [seq key]
  (reduce
   (fn [seq val]
     (assoc seq (key val) val))
   {} seq))

(defn first-where
  "Returns the first item in a collection that fulfills the predicate."
  [pred coll]
  (first (filter pred coll)))

(defn create-shadow-dom-element [{:keys [style body]}]
  (let [element       (js/document.createElement "div")
        style-element (js/document.createElement "style")
        body-element  (js/document.createElement "body")
        shadow        (.attachShadow element (clj->js {:mode "closed"}))]

    (set! (.-textContent style-element) style)
    (set! (.-innerHTML body-element) body)

    (.appendChild shadow style-element)
    (.appendChild shadow body-element)

    element))

(defn with-params [url params]
  (let [u (goog/Uri. url)]
    (doseq [[k v] params]
      (.setParameterValue u (name k) v))
    (str u)))

(defn index-of
  "Returns a list of indexes that match the predicate within the collection."
  [pred coll]
  (->> coll
       (map-indexed vector)
       (filter #(pred (second %)))
       (map first)))

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
       [:state-of-knowledge :boundaries :amp :boundaries]
       [:state-of-knowledge :boundaries :imcra :boundaries]
       [:state-of-knowledge :boundaries :meow :boundaries]
       [:state-of-knowledge :region-reports]
       [:story-maps :featured-maps]
       :habitat-colours
       :habitat-titles
       :sorting
       :config])
       (assoc-in [:map :rich-layers] rich-layers))))

(defn decode-html-entities
  "Removes HTML entities from an HTML entity encoded string:
   https://stackoverflow.com/a/34064434"
  [input]
  (-> (-> (js/DOMParser.)
          (.parseFromString input "text/html"))
      .-documentElement .-textContent))

(defn round-to-nearest
  "Rounds val to nearest value in coll."
  [val coll]
  (->> coll
       (map #(vector % (abs (- % val))))
       (sort-by second)
       first first))

(defn format-number
  "Formats a number with comma thousands separator and dot decimal separator. One
   decimal place is the default, because that's the normal case in Seamap."
  ([number fraction-digits]
   (when number
     (.toLocaleString
      number "en-US"
      #js{:maximumFractionDigits fraction-digits
          :minimumFractionDigits fraction-digits})))
  ([number] (format-number number 1)))

(defn format-date-month
  "Formats a date string to MMM YYYY."
  [date-string]
  (when date-string
    (-> date-string
        js/Date.
        (.toLocaleString
         "en-AU"
         #js{:month "short" :year "numeric"}))))

(defn map-server-url?
  "Returns true when a url looks like it comes from MapServer (which
  sometimes needs special handling)."
  [url]
  (re-matches #"^(.+?)/services/(.+?)/MapServer/.+$" url))

(defn feature-server-url?
  "Returns true when a url looks like it comes from FeatureServer (which
  sometimes needs special handling)."
  [url]
  (re-matches #"^(.+?)/services/(.+?)/FeatureServer/.+$" url))

(defn url?
  "Returns true when string is a url."
  [value]
  (re-find #"^https?://(?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?://(?:www\.|(?!www))[a-zA-Z0-9]+\.[^\s]{2,}|www\.[a-zA-Z0-9]+\.[^\s]{2,}$" value))
