(ns imas-seamap.specs.app-state
  (:require [cljs.spec.alpha :as s]))


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
(s/def :map/layer
  (s/keys :req-un [:map.layer/name
                   :map.layer/server_url
                   :map.layer/layer_name
                   :map.layer/category
                   :map.layer/bounding_box
                   :map.layer/metadata_url
                   :map.layer/description
                   :map.layer/server_type]))
(s/def :map/layers (s/coll-of :map/layer))

(s/def :map/active-layers (s/coll-of :map/layer
                                     :kind vector?))

(s/def :map.layer-group.priority/layer integer?)
(s/def :map.layer-group.priority/group integer?)
(s/def :map.layer-group.priority/priority integer?)
(s/def :map.layer-group/priority (s/keys :req-un [:map.layer-group.priorty/layer
                                                  :map.layer-group.priorty/group
                                                  :map.layer-group.priorty/priority]))
(s/def :map/priorities (s/coll-of :map.layer-group/priority))

(s/def :map.layer.group/id integer?)
(s/def :map.layer.group/name string?)
(s/def :map.layer.group/detail_resolution (s/nilable boolean?))
(s/def :map.layer/group (s/keys :req-un [:map.layer.group/id
                                         :map.layer/bounding_box
                                         :map.layer.group/name
                                         :map.layer.group/detail_resolution]))
(s/def :map/groups (s/coll-of :map.layer/group))

(s/def :map/priority-cutoff (s/and pos? integer?))

(s/def :map.logic/type #{:map.layer-logic/automatic :map.layer-logic/manual})
(s/def :map.logic/trigger #{:map.logic.trigger/automatic :map.logic.trigger/user})
(s/def :map/logic (s/keys :req-un [:map.logic/type :map.logic/trigger]))

(s/def ::habitat-titles  (s/map-of string? (s/nilable string?)))
(s/def ::habitat-colours (s/map-of string? string?))

(s/def ::map
  (s/keys :req-un [:map/center
                   :map/zoom
                   :map/zoom-cutover
                   :map/controls
                   :map/layers
                   :map/active-layers
                   :map/groups
                   :map/priorities
                   :map/priority-cutoff
                   :map/logic]))

(s/def :layer/loading-state #{:map.layer/loading :map.layer/loaded})
(s/def ::layer-state (s/map-of :map/layer
                               (s/cat :loading-state :layer/loading-state
                                      :seen-errors   boolean?)))

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
  (s/keys :req-un [::config
                   ::filters
                   ::habitat-colours
                   ::habitat-titles
                   ::layer-state
                   ::map
                   ::transect]))
