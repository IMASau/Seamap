(ns imas-seamap.specs.app-state
  (:require [cljs.spec :as s]))


(s/def :map/center
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)
(s/def :map/zoom-cutover integer?)

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
(s/def :map.layer/server_type keyword?)
(s/def :map.layer/date_start (s/nilable string?)) ; TODO
(s/def :map.layer/date_end (s/nilable string?))   ; TODO
(s/def :map/layer
  (s/keys :req-un [:map.layer/name
                   :map.layer/server_url
                   :map.layer/layer_name
                   :map.layer/category
                   :map.layer/bounding_box
                   :map.layer/metadata_url
                   :map.layer/description
                   :map.layer/server_type
                   :map.layer/date_start
                   :map.layer/date_end]))
(s/def :map/layers (s/coll-of :map/layer))

(s/def :map/active-layers (s/coll-of :map/layer
                                     :kind set?))

(s/def :map.logic/type #{:map.layer-logic/automatic :map.layer-logic/manual})
(s/def :map.logic/trigger #{:map.logic.trigger/automatic :map.logic.trigger/user})
(s/def :map/logic (s/keys :req-un [:map.logic/type :map.logic/trigger]))

(s/def ::map
  (s/keys :req-un [:map/center :map/zoom :map/zoom-cutover :map/controls :map/layers :map/active-layers :map/logic]))

(s/def ::transect-results-format
  (s/or :empty   nil?
        :state   #{:empty :loading}
        :error   string?
        :results vector?))

(s/def :geojson/type string?)
(s/def :geojson/geometry map?)
(s/def :geojson/properties map?)
(s/def ::geojson (s/keys :req-un [:geojson/type :geojson/geometry :geojson/properties]))
(s/def :transect/query (s/nilable ::geojson))
(s/def :transect/show? (s/nilable boolean?))
(s/def :transect/habitat ::transect-results-format)
(s/def :transect/bathymetry ::transect-results-format)
(s/def :transect/mouse-percentage (s/nilable number?))
(s/def ::transect (s/keys :req-un [:transect/query :transect/show? :transect/habitat :transect/bathymetry]
                          :opt-un [:transect/mouse-percentage]))

(s/def :filters/layers       string?)
(s/def :filters/other-layers string?)
(s/def ::filters (s/keys :req-un [:filters/layers :filters/other-layers]))

(s/def ::config map?)

(s/def :seamap/app-state
  (s/keys :req-un [::map ::transect ::filters ::config]))
