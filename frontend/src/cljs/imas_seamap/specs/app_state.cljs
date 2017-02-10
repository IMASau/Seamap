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

(s/def :map.controls/transect boolean?)
(s/def :map/controls (s/keys :req-un [:map.controls/transect]))

(s/def ::map
  (s/keys :req-un [:map/pos :map/zoom :map/layer-idx :map/markers :map/controls]))

(s/def :geojson/type string?)
(s/def :geojson/geometry map?)
(s/def :geojson/properties map?)
(s/def ::geojson object?) ; Problem here is keys are actually strings!  We'll leave it here for now
(s/def ::transect (s/nilable ::geojson))

(s/def :seamap/app-state
  (s/keys :req-un [::map ::transect]))
