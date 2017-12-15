;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.utils
  (:require [cemerick.url :as url]
            [clojure.set :refer [index rename-keys]]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [debux.cs.core :refer-macros [dbg]]))

;;; http://blog.jayfields.com/2011/01/clojure-select-keys-select-values-and.html
(defn select-values [map ks]
  (reduce #(conj %1 (map %2)) [] ks))

(defn -equalise [[k v]] (str (name k) "=" v))

(defn encode-state
  "Returns a string suitable for storing in the URL's hash"
  [{map-state :map :as db}]
  (let [pruned (-> (select-keys map-state [:center :zoom :active-layers])
                   (rename-keys {:active-layers :active})
                   (update :center #(string/join ";" %))
                   (update :active #(string/join ";" (map :layer_name %))))]
    (->> pruned
         (map -equalise)
         (string/join "|"))))

(defn parse-state [hash-str]
  (let [parsed (->> (string/split hash-str "|")
                    (map #(string/split % "="))
                    (filter #(= 2 (count %)))
                    (into {})
                    keywordize-keys)]
    (-> parsed
        (update :center #(mapv js/parseFloat (string/split % ";")))
        (update :active #(filterv (comp not string/blank?) (string/split % ";")))
        (update :zoom js/parseInt))))

(defn names->active-layers [names layers]
  (let [by-name (index layers [:layer_name])]
    (->> names
         (map #(first (get by-name {:layer_name %})))
         (remove nil?)
         vec)))

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
