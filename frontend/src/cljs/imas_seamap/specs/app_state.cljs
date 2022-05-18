;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.app-state
  (:require [cljs.spec.alpha :as s]))


(s/def :map/center
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)
(s/def :map/zoom-cutover integer?)

(s/def :map.layer/id integer?)
(s/def :map.layer/name string?)
(s/def :map.layer/server_url string?)
(s/def :map.layer/legend_url string?)
(s/def :map.layer/layer_name string?)
(s/def :map.layer/detail_layer (s/nilable string?))
(s/def :map.layer/category keyword?)
(s/def :map.layer/attribution string?)
(s/def :map.layer/sort_key (s/nilable string?))
(s/def :map.layer/info_format_type integer?)
(s/def :map.layer/layer_group (s/nilable integer?))

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

(s/def :map.layer-group/id integer?)
(s/def :map.layer-group/name string?)
(s/def :map.layer-group/sort_key (s/nilable string?))

(s/def :map/base-layer-group
  (s/keys :req-un [:map.layer-group/id
                   :map.layer-group/name
                   :map.layer-group/sort_key]))

(s/def :map/base-layer-groups (s/coll-of :map/base-layer-group
                                         :kind vector?))

(s/def :map/layer
  (s/keys :req-un [:map.layer/name
                   :map.layer/server_url
                   :map.layer/legend_url
                   :map.layer/layer_name
                   :map.layer/detail_layer
                   :map.layer/category
                   :map.layer/bounding_box
                   :map.layer/metadata_url
                   :map.layer/description
                   :map.layer/server_type
                   :map.layer/info_format_type]))

(s/def :map/base-layer
  (s/keys :req-un [:map.layer/id
                   :map.layer/name
                   :map.layer/server_url
                   :map.layer/attribution
                   :map.layer/sort_key
                   :map.layer/layer_group]))

(s/def :map.layer/layers (s/coll-of :map/base-layer
                                    :kind vector?))

(s/def :map/grouped-base-layer
  (s/keys :req-un [:map.layer/id
                   :map.layer/name
                   :map.layer/server_url
                   :map.layer/attribution
                   :map.layer/sort_key
                   :map.layer/layer_group
                   :map.layer/layers]))

(s/def :map/active-base-layer :map/grouped-base-layer)

(s/def :map/layers (s/coll-of :map/layer))

(s/def :map/base-layers (s/coll-of :map/base-layer
                                   :kind vector?))

(s/def :map/grouped-base-layers(s/coll-of :map/grouped-base-layer
                                          :kind vector?))

(s/def :map/active-layers (s/coll-of :map/layer
                                     :kind vector?))

(s/def :map/hidden-layers (s/coll-of :map/layer
                                     :kind set?))


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

(s/def :map.layer.organisation/name string?)
(s/def :map.layer.organisation/logo (s/nilable string?))
(s/def :map.layer/organisation (s/keys :req-un [:map.layer.organisation/name
                                                :map.layer.organisation/logo]))
(s/def :map/organisations (s/coll-of :map.layer/organisation))

(s/def :map.controls/transect boolean?)
(s/def :map.controls.download/type #{:map.layer.download/geotiff
                                     :map.layer.download/shp
                                     :map.layer.download/csv})
(s/def :map.controls.download/selecting boolean?)
(s/def :map.controls.download/layer :map/layer)
(s/def :map.controls.download/link string?)
(s/def :map.controls.download/bbox (s/nilable :map.layer/bounding_box))
(s/def :map.controls.download/display-link boolean?)
(s/def :map.controls/download
  (s/nilable (s/keys :req-un [:map.controls.download/selecting]
                     :opt-un [:map.controls.download/layer
                              :map.controls.download/type
                              :map.controls.download/link
                              :map.controls.download/bbox
                              :map.controls.download/display-link])))
(s/def :map/controls (s/keys :req-un [:map.controls/transect
                                      :map.controls/download]))

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
                   :map/base-layers
                   :map/active-base-layer
                   :map/active-layers
                   :map/hidden-layers
                   :map/groups
                   :map/organisations
                   :map/priorities
                   :map/priority-cutoff
                   :map/logic]))

(s/def :layer/loading-state #{:map.layer/loading :map.layer/loaded})
(s/def :map.state/error-count (s/map-of :map/layer integer?))
(s/def :map.state/tile-count (s/map-of :map/layer integer?))
(s/def :map.state/legend-shown (s/coll-of :map/layer :kind set?))
(s/def :map.state/loading-state (s/map-of :map/layer :layer/loading-state))
(s/def :map.state/opacity (s/map-of :map/layer (s/int-in 0 100)))
(s/def ::layer-state (s/keys :opt-un [:map.state/loading-state
                                      :map.state/error-count
                                      :map.state/tile-count
                                      :map.state/legend-shown
                                      :map.state/opacity]))

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

;;; catalogue
(s/def :display.catalogue/tab string?)
(s/def :display.catalogue/expanded (s/coll-of string? :kind set?))
(s/def :display/catalogue (s/keys :opt-un [:display.catalogue/tab :display.catalogue/expanded]))
;;; overlays
(s/def :display/help-overlay boolean?)
(s/def :display/welcome-overlay boolean?)
;;; sidebar
(s/def :sidebar/collapsed boolean?)
(s/def :sidebar/selected string?)
(s/def :display/sidebar (s/keys :req-un [:sidebar/collapsed :sidebar/selected]))
;;; drawer
(s/def :display/seamap-drawer boolean?)
(s/def :display.drawer-panel/panel #{:drawer-panel/layer-panel
                                     :drawer-panel/management-layers
                                     :drawer-panel/thirdparty-layers})
(s/def :display.drawer-panel/props (s/nilable map?))
(s/def :display/drawer-panel
  (s/keys :req-un [:display.drawer-panel/panel
                   :display.drawer-panel/props]))
(s/def :display/drawer-panels (s/coll-of :display/drawer-panel
                                         :kind vector?))
(s/def ::display
  (s/keys :req-un [:display/catalogue
                   :display/help-overlay
                   :display/welcome-overlay
                   :display/sidebar
                   :display/seamap-drawer
                   :display/drawer-panels]))

(s/def :filters/layers       string?)
(s/def :filters/other-layers string?)
(s/def ::filters (s/keys :req-un [:filters/layers :filters/other-layers]))

(s/def :region-stats/habitat-layer (s/nilable :map/layer))
(s/def ::region-stats (s/keys :req-un [:region-stats/habitat-layer]))

(s/def ::config map?)

(s/def :seamap/app-state
  (s/keys :req-un [::config
                   ::display
                   ::filters
                   ::region-stats
                   ::habitat-colours
                   ::habitat-titles
                   ::layer-state
                   ::map
                   ::transect]))
