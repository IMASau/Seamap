(ns imas-seamap.specs.app-state
  (:require [cljs.spec :as s]))


(s/def :map/center
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)

(s/def :map.controls/transect boolean?)
(s/def :map/controls (s/keys :req-un [:map.controls/transect]))

(s/def :map.layer/name string?)
(s/def :map.layer/server_url string?)
(s/def :map.layer/layer_name string?)
(s/def :map.layer/category keyword?)

(s/def :map.layer.bb/west  float?)
(s/def :map.layer.bb/south float?)
(s/def :map.layer.bb/east  float?)
(s/def :map.layer.bb/north float?)
(s/def :map.layer/bounding_box (s/keys :req-un [:map.layer.bb/west
                                                :map.layer.bb/south
                                                :map.layer.bb/east
                                                :map.layer.bb/north]))
(s/def :map.layer/metadata_url string?)
(s/def :map.layer/description string?)
(s/def :map.layer/zoom_info string?)    ; TODO
(s/def :map.layer/server_type keyword?)
(s/def :map.layer/legend_url string?)
(s/def :map.layer/date_start string?)   ; TODO
(s/def :map.layer/date_end string?)     ; TODO
(s/def :map/layer
  (s/keys :req-un [:map.layer/name
                   :map.layer/server_url
                   :map.layer/layer_name
                   :map.layer/category
                   :map.layer/bounding_box
                   :map.layer/metadata_url
                   :map.layer/description
                   :map.layer/zoom_info
                   :map.layer/server_type
                   :map.layer/legend_url
                   :map.layer/date_start
                   :map.layer/date_end]))
(s/def :map/layers (s/coll-of :map/layer))

(s/def :map/active-layers (s/coll-of :map/layer
                                     :kind set?))

(s/def ::map
  (s/keys :req-un [:map/center :map/zoom :map/controls :map/layers :map/active-layers]))

(s/def :geojson/type string?)
(s/def :geojson/geometry map?)
(s/def :geojson/properties map?)
(s/def ::geojson (s/keys :req-un [:geojson/type :geojson/geometry :geojson/properties]))
(s/def ::transect (s/nilable ::geojson))

(s/def ::config map?)

(s/def :seamap/app-state
  (s/keys :req-un [::map ::transect ::config]))
