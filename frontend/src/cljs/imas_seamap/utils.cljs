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
  [{map-state :map :as db}]
  (let [pruned-map (-> (select-keys map-state [:center :zoom :active-layers :active-base-layer])
                       (rename-keys {:active-layers :active :active-base-layer :active-base})
                       (update :active (partial map :id))
                       (update :active-base :id))
        db         (-> db
                       (select-keys* [[:display :sidebar :selected]
                                      [:display :catalogue]
                                      :layer-state
                                      [:transect :show?]
                                      [:transect :query]])
                       (assoc :map pruned-map)
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
                 [:transect :show?]
                 [:transect :query]
                 [:map :active]
                 [:map :active-base]
                 [:map :center]
                 [:map :zoom]
                 :legend-ids
                 :opacity-ids]))

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
