;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.app-state
  (:require [cljs.spec.alpha :as s]))


(s/def :map/center
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map/zoom integer?)
(s/def :map/zoom-cutover integer?)

;;; categories
(s/def :map.category/id integer?)
(s/def :map.category/name keyword?)
(s/def :map.category/display_name (s/nilable string?))
(s/def :map.category/sort_key (s/nilable string?))
(s/def :map/category
  (s/keys :req-un [:map.category/name
                   :map.category/display_name]))
(s/def :map/categories (s/coll-of :map/category
                                  :kind vector?))

(s/def :map.layer/name string?)
(s/def :map.layer/server_url string?)
(s/def :map.layer/legend_url string?)
(s/def :map.layer/layer_name string?)
(s/def :map.layer/detail_layer (s/nilable string?))
(s/def :map.layer/category :map.category/name)
(s/def :map.layer.bounding_box/west  float?)
(s/def :map.layer.bounding_box/south float?)
(s/def :map.layer.bounding_box/east  float?)
(s/def :map.layer.bounding_box/north float?)
(s/def :map.layer/bounding_box
  (s/keys :req-un [:map.layer.bounding_box/west
                   :map.layer.bounding_box/south
                   :map.layer.bounding_box/east
                   :map.layer.bounding_box/north]))
(s/def :map.layer/metadata_url string?)
(s/def :map.layer/description string?)
(s/def :map.layer/server_type keyword?)
(s/def :map.layer/info_format_type integer?)
(s/def :map.layer/keywords (s/nilable string?))
(s/def :map.layer/style (s/nilable string?))
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
                   :map.layer/info_format_type
                   :map.layer/keywords
                   :map.layer/style]))


(s/def :map.base-layer/id integer?)
(s/def :map.base-layer/name :map.layer/name)
(s/def :map.base-layer/server_url :map.layer/server_url)
(s/def :map.base-layer/attribution string?)
(s/def :map.base-layer/sort_key (s/nilable string?))
(s/def :map.base-layer/layer_group (s/nilable integer?))
(s/def :map/base-layer
  (s/keys :req-un [:map.base-layer/id
                   :map.base-layer/name
                   :map.base-layer/server_url
                   :map.base-layer/attribution
                   :map.base-layer/sort_key
                   :map.base-layer/layer_group]))

(s/def :map.base-layer/layers (s/coll-of :map/base-layer
                                    :kind vector?))


(s/def :map.base-layer-group/id :map.base-layer/id)
(s/def :map.base-layer-group/name :map.layer/name)
(s/def :map.base-layer-group/sort_key :map.base-layer/sort_key)
(s/def :map/base-layer-group
  (s/keys :req-un [:map.base-layer-group/id
                   :map.base-layer-group/name
                   :map.base-layer-group/sort_key]))
(s/def :map/base-layer-groups (s/coll-of :map/base-layer-group
                                         :kind vector?))


(s/def :map.grouped-base-layer/id :map.base-layer/id)
(s/def :map.grouped-base-layer/name :map.base-layer/name)
(s/def :map.grouped-base-layer/server_url :map.base-layer/server_url)
(s/def :map.grouped-base-layer/attribution :map.base-layer/attribution)
(s/def :map.grouped-base-layer/sort_key :map.base-layer/sort_key)
(s/def :map.grouped-base-layer/layer_group :map.base-layer/layer_group)
(s/def :map.grouped-base-layer/layers (s/coll-of :map/base-layer
                                                 :kind vector?))
(s/def :map/grouped-base-layer
  (s/keys :req-un [:map.grouped-base-layer/id
                   :map.grouped-base-layer/name
                   :map.grouped-base-layer/server_url
                   :map.grouped-base-layer/attribution
                   :map.grouped-base-layer/sort_key
                   :map.grouped-base-layer/layer_group
                   :map.grouped-base-layer/layers]))

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

(s/def :map/preview-layer :map/layer)


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
                   :map/categories
                   :map/layers
                   :map/base-layers
                   :map/active-base-layer
                   :map/active-layers
                   :map/hidden-layers
                   :map/preview-layer
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
(s/def :display.catalogue.group/tab string?)
(s/def :display.catalogue.group/expanded (s/coll-of string? :kind set?))
(s/def :display.catalogue/group
  (s/keys :req-un [:display.catalogue.group/tab
                   :display.catalogue.group/expanded]))
(s/def :display/catalogue (s/map-of keyword? :display.catalogue/group))
;;; overlays
(s/def :display/help-overlay boolean?)
(s/def :display/welcome-overlay boolean?)
;;; sidebar
(s/def :sidebar/collapsed boolean?)
(s/def :sidebar/selected string?)
(s/def :display/sidebar (s/keys :req-un [:sidebar/collapsed :sidebar/selected]))
;;; drawer
(s/def :display/left-drawer boolean?)
(s/def :display.drawer-panel/panel #{:drawer-panel/catalogue-layers})
(s/def :display.drawer-panel/props (s/nilable map?))
(s/def :display/drawer-panel
  (s/keys :req-un [:display.drawer-panel/panel
                   :display.drawer-panel/props]))
(s/def :display/drawer-panels (s/coll-of :display/drawer-panel
                                         :kind vector?))


;; state of knowledge
(s/def :state-of-knowledge.boundaries.active-boundary/short string?)
(s/def :state-of-knowledge.boundaries.active-boundary/name string?)
(s/def :state-of-knowledge.boundaries/active-boundary
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.boundaries.active-boundary/id
                    :state-of-knowledge.boundaries.active-boundary/name])))

(s/def :state-of-knowledge.boundaries.amp.network/name string?)
(s/def :state-of-knowledge.boundaries.amp.park/name string?)
(s/def :state-of-knowledge.boundaries.amp.park/network :state-of-knowledge.boundaries.amp.network/name)
(s/def :state-of-knowledge.boundaries.amp.zone/name string?)
(s/def :state-of-knowledge.boundaries.amp.zone-iucn/name string?)

(s/def :state-of-knowledge.boundaries.amp/network
  (s/keys :req-un [:state-of-knowledge.boundaries.amp.network/name]))
(s/def :state-of-knowledge.boundaries.amp/park
  (s/keys :req-un [:state-of-knowledge.boundaries.amp.park/name
                   :state-of-knowledge.boundaries.amp.park/network]))
(s/def :state-of-knowledge.boundaries.amp/zone
  (s/keys :req-un [:state-of-knowledge.boundaries.amp.zone/name]))
(s/def :state-of-knowledge.boundaries.amp/zone-iucn
  (s/keys :req-un [:state-of-knowledge.boundaries.amp.zone-iucn/name]))
(s/def :state-of-knowledge.boundaries.amp/networks (s/coll-of :state-of-knowledge.boundaries.amp/network
                                               :kind vector?))
(s/def :state-of-knowledge.boundaries.amp/parks (s/coll-of :state-of-knowledge.boundaries.amp/park
                                            :kind vector?))
(s/def :state-of-knowledge.boundaries.amp/zones (s/coll-of :state-of-knowledge.boundaries.amp/zone
                                            :kind vector?))
(s/def :state-of-knowledge.boundaries.amp/zones-iucn (s/coll-of :state-of-knowledge.boundaries.amp/zone-iucn
                                                 :kind vector?))
(s/def :state-of-knowledge.boundaries.amp/active-network :state-of-knowledge.boundaries.amp/network)
(s/def :state-of-knowledge.boundaries.amp/active-park :state-of-knowledge.boundaries.amp/park)
(s/def :state-of-knowledge.boundaries.amp/active-zone :state-of-knowledge.boundaries.amp/zone)
(s/def :state-of-knowledge.boundaries.amp/active-zone-iucn :state-of-knowledge.boundaries.amp/zone-iucn)

(s/def :state-of-knowledge.boundaries/amp
  (s/keys :req-un [:state-of-knowledge.boundaries.amp/networks
                   :state-of-knowledge.boundaries.amp/parks
                   :state-of-knowledge.boundaries.amp/zones
                   :state-of-knowledge.boundaries.amp/zones-iucn
                   :state-of-knowledge.boundaries.amp/active-network
                   :state-of-knowledge.boundaries.amp/active-park
                   :state-of-knowledge.boundaries.amp/active-zone
                   :state-of-knowledge.boundaries.amp/active-zone-iucn]))

(s/def :state-of-knowledge.boundaries.imcra.provincial-bioregion/name string?)
(s/def :state-of-knowledge.boundaries.imcra.mesoscale-bioregion/name string?)
(s/def :state-of-knowledge.boundaries.imcra.mesoscale-bioregion/provincial-bioregion :state-of-knowledge.boundaries.imcra.provincial-bioregion/name)

(s/def :state-of-knowledge.boundaries.imcra/provincial-bioregion
  (s/keys :req-un [:state-of-knowledge.boundaries.imcra.provincial-bioregion/name]))
(s/def :state-of-knowledge.boundaries.imcra/mesoscale-bioregion
  (s/keys :req-un [:state-of-knowledge.boundaries.imcra.mesoscale-bioregion/name
                   :state-of-knowledge.boundaries.imcra.mesoscale-bioregion/provincial-bioregion]))
(s/def :state-of-knowledge.boundaries.imcra/provincial-bioregions (s/coll-of :state-of-knowledge.boundaries.imcra/provincial-bioregion
                                                              :kind vector?))
(s/def :state-of-knowledge.boundaries.imcra/mesoscale-bioregions (s/coll-of :state-of-knowledge.boundaries.imcra/mesoscale-bioregion
                                                             :kind vector?))
(s/def :state-of-knowledge.boundaries.imcra/active-provincial-bioregion :state-of-knowledge.boundaries.imcra/provincial-bioregion)
(s/def :state-of-knowledge.boundaries.imcra/active-mesoscale-bioregion :state-of-knowledge.boundaries.imcra/mesoscale-bioregion)

(s/def :state-of-knowledge.boundaries/imcra
  (s/keys :req-un [:state-of-knowledge.boundaries.imcra/provincial-bioregions
                   :state-of-knowledge.boundaries.imcra/mesoscale-bioregions
                   :state-of-knowledge.boundaries.imcra/active-provincial-bioregion
                   :state-of-knowledge.boundaries.imcra/active-mesoscale-bioregion]))

(s/def :state-of-knowledge.boundaries.meow.realm/name string?)
(s/def :state-of-knowledge.boundaries.meow.province/realm :state-of-knowledge.boundaries.meow.realm/name)
(s/def :state-of-knowledge.boundaries.meow.province/name string?)
(s/def :state-of-knowledge.boundaries.meow.ecoregion/realm :state-of-knowledge.boundaries.meow.realm/name)
(s/def :state-of-knowledge.boundaries.meow.ecoregion/province :state-of-knowledge.boundaries.meow.province/name)
(s/def :state-of-knowledge.boundaries.meow.ecoregion/name string?)

(s/def :state-of-knowledge.boundaries.meow/realm
  (s/keys :req-un [:state-of-knowledge.boundaries.meow.realm/name]))
(s/def :state-of-knowledge.boundaries.meow/province
  (s/keys :req-un [:state-of-knowledge.boundaries.meow.province/realm
                   :state-of-knowledge.boundaries.meow.province/name]))
(s/def :state-of-knowledge.boundaries.meow/ecoregion
  (s/keys :req-un [:state-of-knowledge.boundaries.meow.ecoregion/realm
                   :state-of-knowledge.boundaries.meow.ecoregion/province
                   :state-of-knowledge.boundaries.meow.ecoregion/name]))
(s/def :state-of-knowledge.boundaries.meow/realms (s/coll-of :state-of-knowledge.boundaries.meow/realm
                                              :kind vector?))
(s/def :state-of-knowledge.boundaries.meow/provinces (s/coll-of :state-of-knowledge.boundaries.meow/province
                                                 :kind vector?))
(s/def :state-of-knowledge.boundaries.meow/ecoregions (s/coll-of :state-of-knowledge.boundaries.meow/ecoregion
                                                  :kind vector?))
(s/def :state-of-knowledge.boundaries.meow/active-realm :state-of-knowledge.boundaries.meow/realm)
(s/def :state-of-knowledge.boundaries.meow/active-province :state-of-knowledge.boundaries.meow/province)
(s/def :state-of-knowledge.boundaries.meow/active-ecoregion :state-of-knowledge.boundaries.meow/ecoregion)

(s/def :state-of-knowledge.boundaries/meow
  (s/keys :req-un [:state-of-knowledge.boundaries.meow/realms
                   :state-of-knowledge.boundaries.meow/provinces
                   :state-of-knowledge.boundaries.meow/ecoregions
                   :state-of-knowledge.boundaries.meow/active-realm
                   :state-of-knowledge.boundaries.meow/active-province
                   :state-of-knowledge.boundaries.meow/active-ecoregion]))

(s/def :state-of-knowledge/boundaries
  (s/keys :req-un [:state-of-knowledge.boundaries/active-boundary
                   :state-of-knowledge.boundaries/amp
                   :state-of-knowledge.boundaries/imcra
                   :state-of-knowledge.boundaries/meow]))

(s/def :state-of-knowledge.statistics.habitat.result/habitat (s/nilable string?))
(s/def :state-of-knowledge.statistics.habitat.result/area number?)
(s/def :state-of-knowledge.statistics.habitat.result/mapped_percentage (s/nilable number?))
(s/def :state-of-knowledge.statistics.habitat.result/total_percentage number?)
(s/def :state-of-knowledge.statistics.habitat/result
  (s/keys :req-un [:state-of-knowledge.statistics.habitat.result/habitat
                   :state-of-knowledge.statistics.habitat.result/area
                   :state-of-knowledge.statistics.habitat.result/mapped_percentage
                   :state-of-knowledge.statistics.habitat.result/total_percentage]))
(s/def :state-of-knowledge.statistics.habitat/results (s/coll-of :state-of-knowledge.statistics.habitat/result
                                                           :kind vector?))
(s/def :state-of-knowledge.statistics.habitat/loading? boolean?)
(s/def :state-of-knowledge.statistics/habitat
  (s/keys :req-un [:state-of-knowledge.statistics.habitat/results
                   :state-of-knowledge.statistics.habitat/loading?]))

(s/def :state-of-knowledge.statistics.bathymetry.result/resolution (s/nilable string?))
(s/def :state-of-knowledge.statistics.bathymetry.result/rank (s/nilable integer?))
(s/def :state-of-knowledge.statistics.bathymetry.result/area number?)
(s/def :state-of-knowledge.statistics.bathymetry.result/mapped_percentage (s/nilable number?))
(s/def :state-of-knowledge.statistics.bathymetry.result/total_percentage number?)
(s/def :state-of-knowledge.statistics.bathymetry/result
  (s/keys :req-un [:state-of-knowledge.statistics.bathymetry.result/category
                   :state-of-knowledge.statistics.bathymetry.result/rank
                   :state-of-knowledge.statistics.bathymetry.result/area
                   :state-of-knowledge.statistics.bathymetry.result/mapped_percentage
                   :state-of-knowledge.statistics.bathymetry.result/total_percentage]))
(s/def :state-of-knowledge.statistics.bathymetry/results (s/coll-of :state-of-knowledge.statistics.bathymetry/result
                                                              :kind vector?))
(s/def :state-of-knowledge.statistics.bathymetry/loading? :state-of-knowledge.statistics.habitat/loading?)
(s/def :state-of-knowledge.statistics/bathymetry
  (s/keys :req-un [:state-of-knowledge.statistics.bathymetry/results
                   :state-of-knowledge.statistics.bathymetry/loading?]))


(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/deployment_id integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/campaign_name integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/method string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/video_time integer?)

(s/def :state-of-knowledge.statistics.habitat-observations.sediment/sample_id integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/analysed integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/survey integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/method string?)

(s/def :state-of-knowledge.statistics.habitat-observations.squidle/deployment_id integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/campaign_name integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/method string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/images integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/total_annotations integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/public_annotations integer?)

(s/def :state-of-knowledge.statistics.habitat-observations/global-archive
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.global-archive/deployment_id
                    :state-of-knowledge.statistics.habitat-observations.global-archive/campaign_name
                    :state-of-knowledge.statistics.habitat-observations.global-archive/start_date
                    :state-of-knowledge.statistics.habitat-observations.global-archive/end_date
                    :state-of-knowledge.statistics.habitat-observations.global-archive/method
                    :state-of-knowledge.statistics.habitat-observations.global-archive/video_time])))
(s/def :state-of-knowledge.statistics.habitat-observations/sediment
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.sediment/sample_id
                    :state-of-knowledge.statistics.habitat-observations.sediment/analysed
                    :state-of-knowledge.statistics.habitat-observations.sediment/survey
                    :state-of-knowledge.statistics.habitat-observations.sediment/start_date
                    :state-of-knowledge.statistics.habitat-observations.sediment/end_date
                    :state-of-knowledge.statistics.habitat-observations.sediment/method])))
(s/def :state-of-knowledge.statistics.habitat-observations/squidle
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.squidle/deployment_id
                    :state-of-knowledge.statistics.habitat-observations.squidle/campaign_name
                    :state-of-knowledge.statistics.habitat-observations.squidle/start_date
                    :state-of-knowledge.statistics.habitat-observations.squidle/end_date
                    :state-of-knowledge.statistics.habitat-observations.squidle/method
                    :state-of-knowledge.statistics.habitat-observations.squidle/images
                    :state-of-knowledge.statistics.habitat-observations.squidle/total_annotations
                    :state-of-knowledge.statistics.habitat-observations.squidle/public_annotations])))
(s/def :state-of-knowledge.statistics.habitat-observations/loading? boolean?)

(s/def :state-of-knowledge.statistics/habitat-observations
  (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations/global-archive
                   :state-of-knowledge.statistics.habitat-observations/sediment
                   :state-of-knowledge.statistics.habitat-observations/squidle
                   :state-of-knowledge.statistics.habitat-observations/loading?]))

(s/def :state-of-knowledge/statistics
  (s/keys :req-un [:state-of-knowledge.statistics/habitat
                   :state-of-knowledge.statistics/bathymetry
                   :state-of-knowledge.statistics/habitat-observations]))

(s/def :state-of-knowledge/open? boolean?)
(s/def :state-of-knowledge/pill-open? boolean?)
(s/def ::state-of-knowledge
  (s/keys :req-un [:state-of-knowledge/boundaries
                   :state-of-knowledge/statistics
                   :state-of-knowledge/open?
                   :state-of-knowledge/pill-open?]))

(s/def ::display
  (s/keys :req-un [:display/catalogue
                   :display/help-overlay
                   :display/welcome-overlay
                   :display/sidebar
                   :display/left-drawer
                   :display/state-of-knowledge
                   :display/drawer-panels]))


;; filters
(s/def :filters/layers       string?)
(s/def :filters/other-layers string?)
(s/def ::filters (s/keys :req-un [:filters/layers :filters/other-layers]))

(s/def :region-stats/habitat-layer (s/nilable :map/layer))
(s/def ::region-stats (s/keys :req-un [:region-stats/habitat-layer]))

(s/def ::config map?)

(s/def :seamap/app-state
  (s/keys :req-un [::config
                   ::display
                   ::state-of-knowledge
                   ::filters
                   ::region-stats
                   ::habitat-colours
                   ::habitat-titles
                   ::layer-state
                   ::map
                   ::transect]))
