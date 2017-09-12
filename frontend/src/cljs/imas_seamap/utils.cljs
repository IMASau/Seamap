(ns imas-seamap.utils
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]))

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
        (update :center #(map js/parseFloat (string/split % ";")))
        (update :active string/split ";")
        (update :zoom js/parseInt)
        (rename-keys {:active :active-layers}))))
