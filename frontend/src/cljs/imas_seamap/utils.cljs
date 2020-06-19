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
            [debux.cs.core :refer-macros [dbg]]
            [imas-seamap.blueprint :as b]))

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
  (let [pruned-map (-> (select-keys map-state [:center :zoom :active-layers])
                       (rename-keys {:active-layers :active})
                       (update :active (partial map :id)))
        db         (-> db
                       (select-keys* [[:display :sidebar :selected]
                                      [:display :catalogue]
                                      :layer-state
                                      [:transect :show?]
                                      [:transect :query]])
                       (assoc :map pruned-map)
                       #_(update-in [:display :catalogue :expanded] #(into {} (filter second %))))
        legends    (->> db :layer-state :legend-shown (map :id))
        db*        (-> db
                       (dissoc :layer-state)
                       (assoc :legend-ids legends))]
    (b64/encodeString (t/write (t/writer :json) db*))))

(defn filter-state
  "Given a state map, presumably from the hashed state, filter down to
  only expected/allowed paths to prevent injection attacks."
  [state]
  (select-keys* state
                [[:display :sidebar :selected]
                 [:display :catalogue :tab]
                 [:display :catalogue :expanded]
                 [:transect :show?]
                 [:transect :query]
                 [:map :active]
                 [:map :center]
                 [:map :zoom]
                 :legend-ids]))

(defn parse-state [hash-str]
  (try
   (let [decoded   (b64/decodeString hash-str)
         reader    (t/reader :json)]
     (->> decoded
          (t/read reader)
          filter-state))
   (catch js/Object e {})))

(defn ids->layers [ids layers]
  (let [ids (set ids)]
    (filter #(contains? ids (:id %)) layers)))

(defn geonetwork-force-xml [geonetwork-url]
  (let [url            (url/url geonetwork-url)
        update-service #(update % :path string/replace #"/[^/]+$" "/xml.metadata.get")]
    (if (string/includes? geonetwork-url "search#!")
      (let [uuid (-> url :anchor (subs 1))]
        (-> url
            update-service
            (update :query assoc :uuid uuid)
            (assoc :anchor nil)
            str))
      ;; Regular format; switch to XML format:
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
