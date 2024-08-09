;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.app-state
  (:require [cljs.spec.alpha :as s]))


(s/def :map/center
  (s/coll-of number? :count 2 :kind vector?))

(s/def :map.size/x number?)
(s/def :map.size/y number?)
(s/def :map/size
  (s/keys :req-un [:map.size/x
                   :map.size/y]))

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

(s/def :map.layer/id integer?)
(s/def :map.layer/name string?)
(s/def :map.layer/server_url string?)
(s/def :map.layer/legend_url (s/nilable string?))
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
(s/def :map.layer/server_type keyword?)
(s/def :map.layer/info_format_type integer?)
(s/def :map.layer/keywords (s/nilable string?))
(s/def :map.layer/style (s/nilable string?))
(s/def :map.layer/tooltip (s/nilable string?))
(s/def :map.layer/crs string?)
(s/def :map/layer
  (s/keys :req-un [:map.layer/name
                   :map.layer/server_url
                   :map.layer/legend_url
                   :map.layer/layer_name
                   :map.layer/detail_layer
                   :map.layer/category
                   :map.layer/bounding_box
                   :map.layer/metadata_url
                   :map.layer/server_type
                   :map.layer/info_format_type
                   :map.layer/keywords
                   :map.layer/style
                   :map.layer/tooltip
                   :map.layer/crs]))


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

(s/def :map/preview-layer (s/nilable :map/layer))

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


(s/def :map/viewport-only? boolean?)

(s/def :map/keyed-layers
  (s/map-of keyword?
            (s/or
             :ids    (s/coll-of integer? :kind vector?)
             :layers (s/coll-of :map/layer :kind vector?))))

; Rich layer data
(s/def :map.rich-layers.rich-layer/id integer?)
(s/def :map.rich-layers.rich-layer/layer-id :map.layer/id)
(s/def :map.rich-layers.rich-layer/tab-label string?)
(s/def :map.rich-layers.rich-layer/slider-label string?)
(s/def :map.rich-layers.rich-layer/icon string?)
(s/def :map.rich-layers.rich-layer/tooltip string?)
(s/def :map.rich-layers.rich-layer.alternate-view/layer :map.layer/id)
(s/def :map.rich-layers.rich-layer.alternate-view/sort_key (s/nilable string?))
(s/def :map.rich-layers.rich-layer/alternate-view
  (s/keys :req-un [:map.rich-layers.rich-layer.alternate-view/layer
                   :map.rich-layers.rich-layer.alternate-view/sort_key]))
(s/def :map.rich-layers.rich-layer/alternate-views
  (s/coll-of :map.rich-layers.rich-layer/alternate-views :kind vector?))
(s/def :map.rich-layers.rich-layer.timeline1/layer :map.layer/id)
(s/def :map.rich-layers.rich-layer.timeline1/value number?)
(s/def :map.rich-layers.rich-layer.timeline1/label string?)
(s/def :map.rich-layers.rich-layer/timeline1
  (s/keys :req-un [:map.rich-layers.rich-layer.timeline1/layer
                   :map.rich-layers.rich-layer.timeline1/value
                   :map.rich-layers.rich-layer.timeline1/label]))
(s/def :map.rich-layers.rich-layer/timeline
  (s/coll-of :map.rich-layers.rich-layer/timeline1 :kind vector?))
(s/def :map.rich-layers.rich-layer.control/label string?)
(s/def :map.rich-layers.rich-layer.control/icon (s/nilable string?))
(s/def :map.rich-layers.rich-layer.control/tooltip (s/nilable string?))
(s/def :map.rich-layers.rich-layer.control/cql-property string?)
(s/def :map.rich-layers.rich-layer.control/data-type #{"string" "number"})
(s/def :map.rich-layers.rich-layer.control/controller-type #{"slider" "dropdown" "multi-dropdown"})
(s/def :map.rich-layers.rich-layer.control/default-value
  (s/or :multi-dropdown (s/coll-of (s/or :string string? :number number?) :kind vector?)
        :default        (s/nilable (s/or :string string? :number number?))))
(s/def :map.rich-layers.rich-layer/control
  (s/keys :req-un [:map.rich-layers.rich-layer.control/label
                   :map.rich-layers.rich-layer.control/icon
                   :map.rich-layers.rich-layer.control/tooltip
                   :map.rich-layers.rich-layer.control/cql-property
                   :map.rich-layers.rich-layer.control/data-type
                   :map.rich-layers.rich-layer.control/controller-type
                   :map.rich-layers.rich-layer.control/default-value]))
(s/def :map.rich-layers.rich-layer/controls
  (s/coll-of :map.rich-layers.rich-layer/control :kind vector?))

(s/def :map.rich-layers/rich-layer
  (s/keys :req-un [:map.rich-layers.rich-layer/id
                   :map.rich-layers.rich-layer/layer-id
                   :map.rich-layers.rich-layer/tab-label
                   :map.rich-layers.rich-layer/slider-label
                   :map.rich-layers.rich-layer/icon
                   :map.rich-layers.rich-layer/tooltip
                   :map.rich-layers.rich-layer/alternate-views
                   :map.rich-layers.rich-layer/timeline
                   :map.rich-layers.rich-layer/controls]))
(s/def :map.rich-layers/rich-layers
  (s/coll-of :map.rich-layers/rich-layer :kind vector?))

; Rich layer state
(s/def :map.rich-layers.state/tab string?)
(s/def :map.rich-layers.state/alternate-views-selected :map.rich-layers.rich-layer.alternate-view/layer)
(s/def :map.rich-layers.state/timeline-selected :map.rich-layers.rich-layer.timeline1/layer)
(s/def :map.rich-layers.state.control/value
  (s/or :multi-dropdown (s/coll-of (s/or :string string? :number number?) :kind vector?)
        :default        (s/nilable (s/or :string string? :number number?))))
(s/def :map.rich-layers.state/control
  (s/keys :opt-un [:map.rich-layers.state.control/value]))
(s/def :map.rich-layers.state/controls
  (s/map-of :map.rich-layers.rich-layer.control/cql-property :map.rich-layers.state/control))
(s/def :map.rich-layers/state
  (s/keys :req-un [:map.rich-layers.state/tab
                   :map.rich-layers.state/alternate-views-selected
                   :map.rich-layers.state/timeline-selected
                   :map.rich-layers.state/controls]))
(s/def :map.rich-layers/states
  (s/map-of :map.rich-layers.rich-layer/id :map.rich-layers/state))

; Rich layer async data
(s/def :map.rich-layers.async-data.control/value :map.rich-layers.state.control/value)
(s/def :map.rich-layers.async-data.control/values
  (s/coll-of :map.rich-layers.async-data.control/value :kind vector?))
(s/def :map.rich-layers.async-data/control
  (s/keys :req-un [:map.rich-layers.async-data.control/values]))
(s/def :map.rich-layers.async-data/controls
  (s/map-of :map.rich-layers.rich-layer.control/cql-property :map.rich-layers.async-data/control))
(s/def :map.rich-layers.async-data/filter-combination
  (s/map-of :map.rich-layers.rich-layer.control/cql-property :map.rich-layers.state.control/value))
(s/def :map.rich-layers.async-data/filter-combinations
  (s/coll-of :map.rich-layers.async-data/filter-combination :kind vector?))
(s/def :map.rich-layers/async-data
  (s/keys :req-un [:map.rich-layers.async-data/controls
                   :map.rich-layers.async-data/filter-combinations]))
(s/def :map.rich-layers/async-datas
  (s/map-of :map.rich-layers.rich-layer/id :map.rich-layers/async-data))

; Rich layer lookup
(s/def :map.rich-layers/layer-lookup
  (s/map-of :map.layer/id :map.rich-layers.rich-layer/id))

(s/def :map/rich-layers
  (s/keys :req-un [:map.rich-layers/rich-layers
                   :map.rich-layers/states
                   :map.rich-layers/async-datas
                   :map.rich-layers/layer-lookup]))


(s/def :map/legends
  (s/map-of :map.layer/id
            (s/or
             :status #{:map.legend/loading :map.legend/unsupported-layer :map.legend/error}
             :info   map?)))

(s/def ::habitat-titles  (s/map-of string? (s/nilable string?)))
(s/def ::habitat-colours (s/map-of string? string?))

(s/def ::map
  (s/keys :req-un [:map/center
                   :map/size
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
                   :map/organisations
                   :map/viewport-only?
                   :map/keyed-layers
                   :map/rich-layers]))

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
(s/def :transect/distance (s/nilable float?))
(s/def :transect/show? (s/nilable boolean?))
(s/def :transect/habitat ::transect-results-format)
(s/def :transect/bathymetry ::transect-results-format)
(s/def :transect/mouse-percentage (s/nilable number?))
(s/def ::transect (s/keys :req-un [:transect/query :transect/show? :transect/habitat :transect/bathymetry]
                          :opt-un [:transect/mouse-percentage :transect/distance]))

;;; Site Configuration
(s/def :site-configuration/outage-message string?) ; html string
(s/def :site-configuration/last-modified string?) ; date string
(s/def ::site-configuration
  (s/nilable
   (s/keys :req-un [:site-configuration/outage-message
                    :site-configuration/last-modified])))

;;; display
(s/def :display.mouse-pos/x number?)
(s/def :display.mouse-pos/y number?)
(s/def :display/mouse-pos
  (s/nilable (s/keys :opt-un [:display.mouse-pos/x
                              :display.mouse-pos/y])))

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

(s/def :display.right-sidebar/id string?)
(s/def :display.right-sidebar/type #{:state-of-knowledge :story-map :dynamic-pill})
(s/def :display/right-sidebar
  (s/keys :req-un [:display.right-sidebar/id
                   :display.right-sidebar/type]))
(s/def :display/right-sidebars (s/coll-of :display/right-sidebar
                                          :kind vector?))

(s/def :display/open-pill (s/nilable string?))
(s/def :display/outage-message-open? boolean?)


;; state of knowledge
(s/def :state-of-knowledge.boundaries.active-boundary/short string?)
(s/def :state-of-knowledge.boundaries.active-boundary/name string?)
(s/def :state-of-knowledge.boundaries/active-boundary
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.boundaries.active-boundary/id
                    :state-of-knowledge.boundaries.active-boundary/name])))

(s/def :state-of-knowledge.boundaries/active-boundary-layer (s/nilable :map/layer))

(s/def :state-of-knowledge.boundaries.amp/active-network map?)
(s/def :state-of-knowledge.boundaries.amp/active-park map?)
(s/def :state-of-knowledge.boundaries.amp/active-zone map?)
(s/def :state-of-knowledge.boundaries.amp/active-zone-iucn map?)
(s/def :state-of-knowledge.boundaries.amp/active-zone-id map?)

(s/def :state-of-knowledge.boundaries/amp
  (s/keys :req-un [:state-of-knowledge.boundaries.amp/active-network
                   :state-of-knowledge.boundaries.amp/active-park
                   :state-of-knowledge.boundaries.amp/active-zone
                   :state-of-knowledge.boundaries.amp/active-zone-iucn
                   :state-of-knowledge.boundaries.amp/active-zone-id]))

(s/def :state-of-knowledge.boundaries.imcra/active-provincial-bioregion map?)
(s/def :state-of-knowledge.boundaries.imcra/active-mesoscale-bioregion map?)

(s/def :state-of-knowledge.boundaries/imcra
  (s/keys :req-un [:state-of-knowledge.boundaries.imcra/active-provincial-bioregion
                   :state-of-knowledge.boundaries.imcra/active-mesoscale-bioregion]))

(s/def :state-of-knowledge.boundaries.meow/active-realm map?)
(s/def :state-of-knowledge.boundaries.meow/active-province map?)
(s/def :state-of-knowledge.boundaries.meow/active-ecoregion map?)

(s/def :state-of-knowledge.boundaries/meow
  (s/keys :req-un [:state-of-knowledge.boundaries.meow/active-realm
                   :state-of-knowledge.boundaries.meow/active-province
                   :state-of-knowledge.boundaries.meow/active-ecoregion]))

(s/def :state-of-knowledge/boundaries
  (s/keys :req-un [:state-of-knowledge.boundaries/active-boundary
                   :state-of-knowledge.boundaries/active-boundary-layer
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
(s/def :state-of-knowledge.statistics.habitat/show-layers? boolean?)
(s/def :state-of-knowledge.statistics/habitat
  (s/keys :req-un [:state-of-knowledge.statistics.habitat/results
                   :state-of-knowledge.statistics.habitat/loading?
                   :state-of-knowledge.statistics.habitat/show-layers?]))

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
(s/def :state-of-knowledge.statistics.bathymetry/show-layers? :state-of-knowledge.statistics.habitat/show-layers?)
(s/def :state-of-knowledge.statistics/bathymetry
  (s/keys :req-un [:state-of-knowledge.statistics.bathymetry/results
                   :state-of-knowledge.statistics.bathymetry/loading?
                   :state-of-knowledge.statistics.bathymetry/show-layers?]))


(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/deployments integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/campaigns integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/method string?)
(s/def :state-of-knowledge.statistics.habitat-observations.global-archive/video_time integer?)

(s/def :state-of-knowledge.statistics.habitat-observations.sediment/samples integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/analysed integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/survey integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.sediment/method string?)

(s/def :state-of-knowledge.statistics.habitat-observations.squidle/deployments integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/campaigns integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/start_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/end_date string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/method string?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/images integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/total_annotations integer?)
(s/def :state-of-knowledge.statistics.habitat-observations.squidle/public_annotations integer?)

(s/def :state-of-knowledge.statistics.habitat-observations/global-archive
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.global-archive/deployments
                    :state-of-knowledge.statistics.habitat-observations.global-archive/campaigns
                    :state-of-knowledge.statistics.habitat-observations.global-archive/start_date
                    :state-of-knowledge.statistics.habitat-observations.global-archive/end_date
                    :state-of-knowledge.statistics.habitat-observations.global-archive/method
                    :state-of-knowledge.statistics.habitat-observations.global-archive/video_time])))
(s/def :state-of-knowledge.statistics.habitat-observations/sediment
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.sediment/samples
                    :state-of-knowledge.statistics.habitat-observations.sediment/analysed
                    :state-of-knowledge.statistics.habitat-observations.sediment/survey
                    :state-of-knowledge.statistics.habitat-observations.sediment/start_date
                    :state-of-knowledge.statistics.habitat-observations.sediment/end_date
                    :state-of-knowledge.statistics.habitat-observations.sediment/method])))
(s/def :state-of-knowledge.statistics.habitat-observations/squidle
  (s/nilable
   (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations.squidle/deployments
                    :state-of-knowledge.statistics.habitat-observations.squidle/campaigns
                    :state-of-knowledge.statistics.habitat-observations.squidle/start_date
                    :state-of-knowledge.statistics.habitat-observations.squidle/end_date
                    :state-of-knowledge.statistics.habitat-observations.squidle/method
                    :state-of-knowledge.statistics.habitat-observations.squidle/images
                    :state-of-knowledge.statistics.habitat-observations.squidle/total_annotations
                    :state-of-knowledge.statistics.habitat-observations.squidle/public_annotations])))
(s/def :state-of-knowledge.statistics.habitat-observations/loading? boolean?)
(s/def :state-of-knowledge.statistics.habitat-observations/show-layers? boolean?)

(s/def :state-of-knowledge.statistics/habitat-observations
  (s/keys :req-un [:state-of-knowledge.statistics.habitat-observations/global-archive
                   :state-of-knowledge.statistics.habitat-observations/sediment
                   :state-of-knowledge.statistics.habitat-observations/squidle
                   :state-of-knowledge.statistics.habitat-observations/loading?
                   :state-of-knowledge.statistics.habitat-observations/show-layers?]))

(s/def :state-of-knowledge/statistics
  (s/keys :req-un [:state-of-knowledge.statistics/habitat
                   :state-of-knowledge.statistics/bathymetry
                   :state-of-knowledge.statistics/habitat-observations]))

(s/def :state-of-knowledge/open? boolean?)
(s/def ::state-of-knowledge
  (s/keys :req-un [:state-of-knowledge/boundaries
                   :state-of-knowledge/statistics]))


;; story-maps
(s/def :story-maps.story-map.map-link/subtitle string?)
(s/def :story-maps.story-map.map-link/description string?)
(s/def :story-maps.story-map.map-link/shortcode string?)
(s/def :story-maps.story-map/map-link
  (s/keys :req-un [:story-maps.story-map.map-link/subtitle
                   :story-maps.story-map.map-link/description
                   :story-maps.story-map.map-link/shortcode]))

(s/def :story-maps.story-map/id integer?)
(s/def :story-maps.story-map/title string?)
(s/def :story-maps.story-map/content string?)
(s/def :story-maps.story-map/image (s/nilable string?))
(s/def :story-maps.story-map/map-links (s/coll-of :story-maps.story-map/map-link
                                                  :kind vector?))
(s/def :story-maps/story-map
  (s/keys :req-un [:story-maps.story-map/id
                   :story-maps.story-map/title
                   :story-maps.story-map/content
                   :story-maps.story-map/image
                   :story-maps.story-map/map-links]))

(s/def :story-maps/featured-maps (s/coll-of :story-maps/story-map
                                            :kind vector?))
(s/def :story-maps/featured-map (s/nilable :story-maps/story-map))
(s/def :story-maps/open? boolean?)
(s/def ::story-maps
  (s/keys :req-un [:story-maps/featured-maps
                   :story-maps/featured-map
                   :story-maps/open?]))


;; display
(s/def ::display
  (s/keys :req-un [:display/mouse-pos
                   :display/catalogue
                   :display/help-overlay
                   :display/welcome-overlay
                   :display/sidebar
                   :display/left-drawer
                   :display/right-sidebars
                   :display/open-pill
                   :display/outage-message-open?]))


;; filters
(s/def :filters/layers       string?)
(s/def :filters/other-layers string?)
(s/def ::filters (s/keys :req-un [:filters/layers :filters/other-layers]))

(s/def :region-stats/habitat-layer (s/nilable :map/layer))
(s/def ::region-stats (s/keys :req-un [:region-stats/habitat-layer]))

;; config
(s/def :config/url-paths (s/map-of keyword? string?))
(s/def :config/urls      (s/nilable (s/map-of keyword? string?)))
(s/def ::config
  (s/keys :req-un [:config/url-paths
                   :config/urls]))

;; dynamic pills
(s/def :dynamic-pills.dynamic-pill/id integer?)
(s/def :dynamic-pills.dynamic-pill/text string?)
(s/def :dynamic-pills.dynamic-pill/icon (s/nilable string?))
(s/def :dynamic-pills.dynamic-pill/tooltip (s/nilable string?))
(s/def :dynamic-pills.dynamic-pill.layer/layer :map.layer/id)
(s/def :dynamic-pills.dynamic-pill.layer/metadata (s/nilable string?))
(s/def :dynamic-pills.dynamic-pill/layer
  (s/keys :req-un [:dynamic-pills.dynamic-pill.layer/layer
                   :dynamic-pills.dynamic-pill.layer/metadata]))
(s/def :dynamic-pills.dynamic-pill/layers (s/coll-of :dynamic-pills.dynamic-pill/layer :kind vector?))
(s/def :dynamic-pills.dynamic-pill.region-control/label string?)
(s/def :dynamic-pills.dynamic-pill.region-control/icon (s/nilable string?))
(s/def :dynamic-pills.dynamic-pill.region-control/tooltip (s/nilable string?))
(s/def :dynamic-pills.dynamic-pill.region-control/cql-property string?)
(s/def :dynamic-pills.dynamic-pill.region-control/data-type #{"string" "number"})
(s/def :dynamic-pills.dynamic-pill.region-control/controller-type #{"slider" "dropdown" "multi-dropdown"})
(s/def :dynamic-pills.dynamic-pill.region-control/default-value
  (s/or :multi-dropdown (s/coll-of (s/or :string string? :number number?) :kind vector?)
        :default        (s/nilable (s/or :string string? :number number?))))
(s/def :dynamic-pills.dynamic-pill/region-control
  (s/keys :req-un [:dynamic-pills.dynamic-pill.region-control/label
                   :dynamic-pills.dynamic-pill.region-control/icon
                   :dynamic-pills.dynamic-pill.region-control/tooltip
                   :dynamic-pills.dynamic-pill.region-control/cql-property
                   :dynamic-pills.dynamic-pill.region-control/data-type
                   :dynamic-pills.dynamic-pill.region-control/controller-type
                   :dynamic-pills.dynamic-pill.region-control/default-value]))
(s/def :dynamic-pills/dynamic-pill
  (s/keys :req-un [:dynamic-pills.dynamic-pill/id
                   :dynamic-pills.dynamic-pill/text
                   :dynamic-pills.dynamic-pill/icon
                   :dynamic-pills.dynamic-pill/tooltip
                   :dynamic-pills.dynamic-pill/layers
                   :dynamic-pills.dynamic-pill/region-control]))
(s/def :dynamic-pills/dynamic-pills (s/coll-of :dynamic-pills/dynamic-pill :kind vector?))

(s/def :dynamic-pills.state/active? (s/nilable boolean?))
(s/def :dynamic-pills.state.region-control/value
  (s/or :multi-dropdown (s/coll-of (s/or :string string? :number number?) :kind vector?)
        :default        (s/nilable (s/or :string string? :number number?))))
(s/def :dynamic-pills.state/region-control
  (s/keys :req-un [:dynamic-pills.state.region-control/value]))
(s/def :dynamic-pills/state
  (s/keys :req-un [:dynamic-pills.state/active?
                   :dynamic-pills.state/region-control]))
(s/def :dynamic-pills/states (s/map-of :dynamic-pills.dynamic-pill/id :dynamic-pills/state))

; Dynamic pill async data
(s/def :dynamic-pills.async-data.region-control/value :dynamic-pills.state.region-control/value)
(s/def :dynamic-pills.async-data.region-control/values
  (s/coll-of :dynamic-pills.async-data.region-control/value :kind vector?))
(s/def :dynamic-pills.async-data/region-control
  (s/keys :req-un [:dynamic-pills.async-data.region-control/values]))
(s/def :dynamic-pills/async-data
  (s/keys :req-un [:dynamic-pills.async-data/region-control]))
(s/def :dynamic-pills/async-datas
  (s/map-of :dynamic-pills.dynamic-pill/id :dynamic-pills/async-data))

(s/def ::dynamic-pills
  (s/keys :req-un [:dynamic-pills/dynamic-pills
                   :dynamic-pills/states
                   :dynamic-pills/async-datas]))

(s/def :seamap/app-state
  (s/keys :req-un [::config
                   ::site-configuration
                   ::display
                   ::state-of-knowledge
                   ::story-maps
                   ::filters
                   ::region-stats
                   ::habitat-colours
                   ::habitat-titles
                   ::layer-state
                   ::map
                   ::dynamic-pills
                   ::transect]))
