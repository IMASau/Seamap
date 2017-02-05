(ns imas-seamap.specs.app-state
  (:require [cljs.spec :as s]))


(s/def :map/pos
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)

(s/def :map/layer-idx (s/and integer? (comp not neg?)))

(s/def :map-marker/title string?)

(s/def :map/marker-type
  (s/keys :req-un [:map/pos :map-marker/title]))

(s/def :map/markers
  (s/coll-of :map/marker-type))

(s/def ::map
  (s/keys :req-un [:map/pos :map/zoom :map/layer-idx :map/markers]))

(s/def :seamap/app-state
  (s/keys :req-un [::map]))
