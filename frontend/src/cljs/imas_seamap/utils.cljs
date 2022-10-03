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
  (let [pruned-map (-> (select-keys map-state [:center :zoom :active-layers :active-base-layer :viewport-only? :bounds])
                       (rename-keys {:active-layers :active :active-base-layer :active-base})
                       (update :active (partial map :id))
                       (update :active-base :id))
        pruned-boundaries (select-keys*
                           boundaries-state
                           [[:active-boundary]
                            [:active-boundary-layer]
                            [:amp :active-network]
                            [:amp :active-park]
                            [:amp :active-zone]
                            [:amp :active-zone-iucn]
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
                                      [:display :catalogue]
                                      [:display :left-drawer]
                                      [:display :left-drawer-tab]
                                      [:filters :layers]
                                      :layer-state
                                      [:transect :show?]
                                      [:transect :query]
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

(defn filter-state
  "Given a state map, presumably from the hashed state, filter down to
  only expected/allowed paths to prevent injection attacks."
  [state]
  (select-keys* state
                [[:display :sidebar :selected]
                 [:display :catalogue]
                 [:display :left-drawer]
                 [:display :left-drawer-tab]
                 [:filters :layers]
                 [:state-of-knowledge :boundaries :active-boundary]
                 [:state-of-knowledge :boundaries :active-boundary-layer]
                 [:state-of-knowledge :boundaries :amp :active-network]
                 [:state-of-knowledge :boundaries :amp :active-park]
                 [:state-of-knowledge :boundaries :amp :active-zone]
                 [:state-of-knowledge :boundaries :amp :active-zone-iucn]
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

;;; https://github.com/metosin/komponentit/blob/master/src/cljs/komponentit/clipboard.cljs (EPL)
(defn copy-text [text]
  (let [el (js/document.createElement "textarea")
        prev-focus-el js/document.activeElement
        y-pos (or (.. js/window -pageYOffset)
                  (.. js/document -documentElement -scrollTop))]
    (set! (.-style el) #js {:position "absolute"
                            :left "-9999px"
                            :top (str y-pos "px")
                            ;; iOS workaround?
                            :fontSize "12pt"
                            ;; reset box-model
                            :border "0"
                            :padding "0"
                            :margin "0"})
    (set! (.-value el) text)
    (.addEventListener el "focus" (fn [_] (.scrollTo js/window 0 y-pos)))
    (js/document.body.appendChild el)
    (.setSelectionRange el 0 (.. el -value -length))
    (.focus el)
    (js/document.execCommand "copy")
    (.blur el)
    (when prev-focus-el
      (.focus prev-focus-el))
    (.removeAllRanges (.getSelection js/window))
    (js/window.document.body.removeChild el)))

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
    [:state-of-knowledge :boundaries :amp :networks]
    [:state-of-knowledge :boundaries :amp :parks]
    [:state-of-knowledge :boundaries :amp :zones]
    [:state-of-knowledge :boundaries :amp :zones-iucn]
    [:state-of-knowledge :boundaries :imcra :provincial-bioregions]
    [:state-of-knowledge :boundaries :imcra :mesoscale-bioregions]
    [:state-of-knowledge :boundaries :meow :realms]
    [:state-of-knowledge :boundaries :meow :provinces]
    [:state-of-knowledge :boundaries :meow :ecoregions]
    [:story-maps :featured-maps]
    :habitat-colours
    :habitat-titles
    :sorting
    :config]))

(defn decode-html-entities
  "Removes HTML entities from an HTML entity encoded string:
   https://stackoverflow.com/a/34064434"
  [input]
  (-> (-> (js/DOMParser.)
          (.parseFromString input "text/html"))
      .-documentElement .-textContent))
