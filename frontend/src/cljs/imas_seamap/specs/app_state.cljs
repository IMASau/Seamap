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

(s/def :map.boundaries/active-boundary
  (s/nilable #{:map.boundaries.active-boundary/amp
               :map.boundaries.active-boundary/imcra
               :map.boundaries.active-boundary/meow}))

(s/def :map.boundaries.amp.network/name string?)
(s/def :map.boundaries.amp.park/name string?)
(s/def :map.boundaries.amp.park/network :map.boundaries.amp.network/name)
(s/def :map.boundaries.amp.zone/name string?)
(s/def :map.boundaries.amp.zone-iucn/name string?)

(s/def :map.boundaries.amp/network
  (s/keys :req-un [:map.boundaries.amp.network/name]))
(s/def :map.boundaries.amp/park
  (s/keys :req-un [:map.boundaries.amp.park/name
                   :map.boundaries.amp.park/network]))
(s/def :map.boundaries.amp/zone
  (s/keys :req-un [:map.boundaries.amp.zone/name]))
(s/def :map.boundaries.amp/zone-iucn
  (s/keys :req-un [:map.boundaries.amp.zone-iucn/name]))
(s/def :map.boundaries.amp/networks (s/coll-of :map.boundaries.amp/network
                                               :kind vector?))
(s/def :map.boundaries.amp/parks (s/coll-of :map.boundaries.amp/park
                                            :kind vector?))
(s/def :map.boundaries.amp/zones (s/coll-of :map.boundaries.amp/zone
                                            :kind vector?))
(s/def :map.boundaries.amp/zones-iucn (s/coll-of :map.boundaries.amp/zone-iucn
                                                 :kind vector?))
(s/def :map.boundaries.amp/active-network :map.boundaries.amp/network)
(s/def :map.boundaries.amp/active-park :map.boundaries.amp/park)
(s/def :map.boundaries.amp/active-zone :map.boundaries.amp/zone)
(s/def :map.boundaries.amp/active-zone-iucn :map.boundaries.amp/zone-iucn)

(s/def :map.boundaries/amp
  (s/keys :req-un [:map.boundaries.amp/networks
                   :map.boundaries.amp/parks
                   :map.boundaries.amp/zones
                   :map.boundaries.amp/zones-iucn
                   :map.boundaries.amp/active-network
                   :map.boundaries.amp/active-park
                   :map.boundaries.amp/active-zone
                   :map.boundaries.amp/active-zone-iucn]))

(s/def :map.boundaries.imcra.provincial-bioregion/name string?)
(s/def :map.boundaries.imcra.mesoscale-bioregion/name string?)
(s/def :map.boundaries.imcra.mesoscale-bioregion/provincial-bioregion :map.boundaries.imcra.provincial-bioregion/name)

(s/def :map.boundaries.imcra/provincial-bioregion
  (s/keys :req-un [:map.boundaries.imcra.provincial-bioregion/name]))
(s/def :map.boundaries.imcra/mesoscale-bioregion
  (s/keys :req-un [:map.boundaries.imcra.mesoscale-bioregion/name
                   :map.boundaries.imcra.mesoscale-bioregion/provincial-bioregion]))
(s/def :map.boundaries.imcra/provincial-bioregions (s/coll-of :map.boundaries.imcra/provincial-bioregion
                                                              :kind vector?))
(s/def :map.boundaries.imcra/mesoscale-bioregions (s/coll-of :map.boundaries.imcra/mesoscale-bioregion
                                                             :kind vector?))
(s/def :map.boundaries.imcra/active-provincial-bioregion :map.boundaries.imcra/provincial-bioregion)
(s/def :map.boundaries.imcra/active-mesoscale-bioregion :map.boundaries.imcra/mesoscale-bioregion)

(s/def :map.boundaries/imcra
  (s/keys :req-un [:map.boundaries.imcra/provincial-bioregions
                   :map.boundaries.imcra/mesoscale-bioregions
                   :map.boundaries.imcra/active-provincial-bioregion
                   :map.boundaries.imcra/active-mesoscale-bioregion]))

(s/def :map.boundaries.meow.realm/name string?)
(s/def :map.boundaries.meow.province/realm :map.boundaries.meow.realm/name)
(s/def :map.boundaries.meow.province/name string?)
(s/def :map.boundaries.meow.ecoregion/realm :map.boundaries.meow.realm/name)
(s/def :map.boundaries.meow.ecoregion/province :map.boundaries.meow.province/name)
(s/def :map.boundaries.meow.ecoregion/name string?)

(s/def :map.boundaries.meow/realm
  (s/keys :req-un [:map.boundaries.meow.realm/name]))
(s/def :map.boundaries.meow/province
  (s/keys :req-un [:map.boundaries.meow.province/realm
                   :map.boundaries.meow.province/name]))
(s/def :map.boundaries.meow/ecoregion
  (s/keys :req-un [:map.boundaries.meow.ecoregion/realm
                   :map.boundaries.meow.ecoregion/province
                   :map.boundaries.meow.ecoregion/name]))
(s/def :map.boundaries.meow/realms (s/coll-of :map.boundaries.meow/realm
                                              :kind vector?))
(s/def :map.boundaries.meow/provinces (s/coll-of :map.boundaries.meow/province
                                                 :kind vector?))
(s/def :map.boundaries.meow/ecoregions (s/coll-of :map.boundaries.meow/ecoregion
                                                  :kind vector?))
(s/def :map.boundaries.meow/active-realm :map.boundaries.meow/realm)
(s/def :map.boundaries.meow/active-province :map.boundaries.meow/province)
(s/def :map.boundaries.meow/active-ecoregion :map.boundaries.meow/ecoregion)

(s/def :map.boundaries/meow
  (s/keys :req-un [:map.boundaries.meow/realms
                   :map.boundaries.meow/provinces
                   :map.boundaries.meow/ecoregions
                   :map.boundaries.meow/active-realm
                   :map.boundaries.meow/active-province
                   :map.boundaries.meow/active-ecoregion]))

(s/def :map/boundaries
  (s/keys :req-un [:map.boundaries/active-boundary
                   :map.boundaries/amp
                   :map.boundaries/imcra
                   :map.boundaries/meow]))

(s/def :map.boundary-statistics.habitat.result/habitat (s/nilable string?))
(s/def :map.boundary-statistics.habitat.result/area number?)
(s/def :map.boundary-statistics.habitat.result/mapped_percentage (s/nilable number?))
(s/def :map.boundary-statistics.habitat.result/total_percentage number?)
(s/def :map.boundary-statistics.habitat/result
  (s/keys :req-un [:map.boundary-statistics.habitat.result/habitat
                   :map.boundary-statistics.habitat.result/area
                   :map.boundary-statistics.habitat.result/mapped_percentage
                   :map.boundary-statistics.habitat.result/total_percentage]))
(s/def :map.boundary-statistics.habitat/results (s/coll-of :map.boundary-statistics.habitat/result
                                                           :kind vector?))
(s/def :map.boundary-statistics.habitat/loading? boolean?)
(s/def :map.boundary-statistics/habitat
  (s/keys :req-un [:map.boundary-statistics.habitat/results
                   :map.boundary-statistics.habitat/loading?]))

(s/def :map.boundary-statistics.bathymetry.result/resolution (s/nilable string?))
(s/def :map.boundary-statistics.bathymetry.result/rank (s/nilable integer?))
(s/def :map.boundary-statistics.bathymetry.result/area number?)
(s/def :map.boundary-statistics.bathymetry.result/mapped_percentage (s/nilable number?))
(s/def :map.boundary-statistics.bathymetry.result/total_percentage number?)
(s/def :map.boundary-statistics.bathymetry/result
  (s/keys :req-un [:map.boundary-statistics.bathymetry.result/category
                   :map.boundary-statistics.bathymetry.result/rank
                   :map.boundary-statistics.bathymetry.result/area
                   :map.boundary-statistics.bathymetry.result/mapped_percentage
                   :map.boundary-statistics.bathymetry.result/total_percentage]))
(s/def :map.boundary-statistics.bathymetry/results (s/coll-of :map.boundary-statistics.bathymetry/result
                                                              :kind vector?))
(s/def :map.boundary-statistics.bathymetry/loading? :map.boundary-statistics.habitat/loading?)
(s/def :map.boundary-statistics/bathymetry
  (s/keys :req-un [:map.boundary-statistics.bathymetry/results
                   :map.boundary-statistics.bathymetry/loading?]))

(s/def :map.boundary-statistics.habitat-observations.global-archive/campaign_name string?)
(s/def :map.boundary-statistics.habitat-observations.global-archive/deployment_id string?)
(s/def :map.boundary-statistics.habitat-observations.global-archive/date string?)
(s/def :map.boundary-statistics.habitat-observations.global-archive/method (s/nilable string?))
(s/def :map.boundary-statistics.habitat-observations.global-archive/video_time (s/nilable integer?))
(s/def :map.boundary-statistics.habitat-observations/global-archive
  (s/keys :req-un [:map.boundary-statistics.habitat-observations.global-archive/campaign_name
                   :map.boundary-statistics.habitat-observations.global-archive/deployment_id
                   :map.boundary-statistics.habitat-observations.global-archive/date
                   :map.boundary-statistics.habitat-observations.global-archive/method
                   :map.boundary-statistics.habitat-observations.global-archive/video_time]))
(s/def :map.boundary-statistics.habitat-observations/global-archives (s/coll-of :map.boundary-statistics.habitat-observations/global-archive
                                                                                :kind vector?))

(s/def :map.boundary-statistics.habitat-observations.sediment/survey string?)
(s/def :map.boundary-statistics.habitat-observations.sediment/sample_id string?)
(s/def :map.boundary-statistics.habitat-observations.sediment/date (s/nilable string?))
(s/def :map.boundary-statistics.habitat-observations.sediment/method string?)
(s/def :map.boundary-statistics.habitat-observations.sediment/analysed string?)
(s/def :map.boundary-statistics.habitat-observations/sediment
  (s/keys :req-un [:map.boundary-statistics.habitat-observations.sediment/survey
                   :map.boundary-statistics.habitat-observations.sediment/sample_id
                   :map.boundary-statistics.habitat-observations.sediment/date
                   :map.boundary-statistics.habitat-observations.sediment/method
                   :map.boundary-statistics.habitat-observations.sediment/method
                   :map.boundary-statistics.habitat-observations.sediment/analysed]))
(s/def :map.boundary-statistics.habitat-observations/sediments (s/coll-of :map.boundary-statistics.habitat-observations/sediment
                                                                          :kind vector?))

(s/def :map.boundary-statistics.habitat-observations.global-archive/campaign_name string?)
(s/def :map.boundary-statistics.habitat-observations.global-archive/deployment_id string?)
(s/def :map.boundary-statistics.habitat-observations.global-archive/date (s/nilable string?))
(s/def :map.boundary-statistics.habitat-observations.squidle/method string?)
(s/def :map.boundary-statistics.habitat-observations.squidle/images integer?)
(s/def :map.boundary-statistics.habitat-observations.squidle/total_annotations integer?)
(s/def :map.boundary-statistics.habitat-observations.squidle/public_annotations integer?)
(s/def :map.boundary-statistics.habitat-observations/squidle
  (s/keys :req-un [:map.boundary-statistics.habitat-observations.squidle/campaign_name
                   :map.boundary-statistics.habitat-observations.squidle/deployment_id
                   :map.boundary-statistics.habitat-observations.squidle/date
                   :map.boundary-statistics.habitat-observations.squidle/method
                   :map.boundary-statistics.habitat-observations.squidle/images
                   :map.boundary-statistics.habitat-observations.squidle/total_annotations
                   :map.boundary-statistics.habitat-observations.squidle/public_annotations]))
(s/def :map.boundary-statistics.habitat-observations/squidles (s/coll-of :map.boundary-statistics.habitat-observations/squidle
                                                                         :kind vector?))

(s/def :map.boundary-statistics.habitat-observations/loading? boolean?)

(s/def :map.boundary-statistics/habitat-observations
  (s/keys :req-un [:map.boundary-statistics.habitat-observations/global-archives
                   :map.boundary-statistics.habitat-observations/sediments
                   :map.boundary-statistics.habitat-observations/squidles
                   :map.boundary-statistics.habitat-observations/loading?]))

(s/def :map/boundary-statistics
  (s/keys :req-un [:map.boundary-statistics/habitat
                   :map.boundary-statistics/bathymetry
                   :map.boundary-statistics/habitat-observations]))

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
                   :map/logic
                   :map/boundaries
                   :map/boundary-statistics]))

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
(s/def :display/right-drawer  boolean?)
(s/def :display.drawer-panel/panel #{:drawer-panel/catalogue-layers})
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
                   :display/left-drawer
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
