(ns imas-seamap.specs.app-state
  (:require [cljs.spec :as s]))


(s/def :map/pos
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)

(s/def :map.controls/transect boolean?)
(s/def :map/controls (s/keys :req-un [:map.controls/transect]))

(s/def ::map
  (s/keys :req-un [:map/pos :map/zoom :map/controls]))

(s/def :geojson/type string?)
(s/def :geojson/geometry map?)
(s/def :geojson/properties map?)
(s/def ::geojson (s/keys :req-un [:geojson/type :geojson/geometry :geojson/properties]))
(s/def ::transect (s/nilable ::geojson))

(s/def ::config map?)

(s/def :seamap/app-state
  (s/keys :req-un [::map ::transect ::config]))
