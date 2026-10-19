;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.config.deployments
  "Deployment configurations for all Seamap instances.

   Each deployment configuration defines:
   - Unique identifier and CSS class
   - Branding (logo, title, colors, welcome content)
   - Map configuration (CRS, center or bounds, zoom and zoom limits)
   - Feature flags (which features are enabled)
   - UI layout (:layout - the slots described in docs/configuration-driven-refactor-proposal.md)
   - Initial state overrides
   - Deployment-specific URL paths

   Feature keywords are namespaced (:feature/*) so uses are greppable. The full
   list of recognised features is `all-features` at the end of this file.

   Branding URLs may be a literal string, a keyword naming a runtime URL base
   (:url-base/wordpress, :url-base/api, :url-base/media, :url-base/img), or a
   vector [url-base-keyword path] joined at runtime. See subs/branding.cljs.")

;;; =============================================================================
;;; SEAMAP AUSTRALIA
;;; =============================================================================

(def seamap-australia
  {:id          :seamap-australia
   :css-class   "seamap"
   :drawer-class "seamap-drawer"

   ;; Branding
   :branding
   {:logo        {:src    "img/Seamap2_V2_RGB.png"
                  :alt    "Seamap Australia"
                  :height 96}
    :url         "https://seamapaustralia.org"
    :title       "Seamap Australia"
    :welcome     {:title       [:<> "Welcome to" [:br] "Seamap Australia."]
                  :content     :welcome/seamap-australia
                  :button-text "Get Started!"
                  :footer      :welcome-footer/seamap-australia}
    :labels      {:region-control    "Select Habitat Data"
                  :transect-control  "Transect/Measure"
                  :layer-info        "Layer info / Download data"}
    :links       {:main-site "https://seamapaustralia.org"}}

   ;; Map configuration
   :map         {:crs    :epsg-3857
                 :center [-27.819644755 132.133333]
                 :zoom   4}

   ;; Feature flags - which features are enabled for this deployment
   :features    #{:feature/state-of-knowledge
                  :feature/featured-maps
                  :feature/floating-pills
                  :feature/boundaries-pill
                  :feature/zones-pill
                  :feature/plot-component
                  :feature/transect-control
                  :feature/region-select
                  :feature/data-providers
                  :feature/data-download
                  :feature/settings
                  :feature/viewport-only
                  :feature/pinned-legends
                  :feature/layer-preview}

   ;; Layout configuration (Phase 4: Unified layout). Hotkeys and helper overlay
   ;; use the shared defaults.
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers :featured-maps]}
                 :right-drawer   {:story-map          :featured-map/drawer
                                  :state-of-knowledge :sok/panel
                                  :dynamic-pill       :dynamic-pill/drawer}
                 :footer         [:footer/plot]
                 :floating-pills [:pill/state-of-knowledge :pill/boundaries :pill/zones
                                  :pill/dynamic :pill/side-by-side-views]
                 :map-overlays   [:overlay/pinned-legends]
                 :controls       {:top-left  [:control/menu :control/settings :control/zoom
                                              [:control/print :control/omnisearch :control/transect
                                               :control/region :control/share :control/reset]
                                              [:control/shortcuts :control/help]]
                                  :top-right [:control/layers]}}

   ;; Feature-specific state initialization
   :initial-state
   {:display {:welcome-overlay false}
    :state-of-knowledge
    {:boundaries {:active-boundary nil
                  :active-boundary-layer nil
                  :amp   {:active-network   nil
                          :active-park      nil
                          :active-zone      nil
                          :active-zone-iucn nil
                          :active-zone-id   nil}
                  :imcra {:active-provincial-bioregion nil
                          :active-mesoscale-bioregion  nil}
                  :meow  {:active-realm     nil
                          :active-province  nil
                          :active-ecoregion nil}}
     :statistics {:habitat {:results      []
                            :loading?     false
                            :show-layers? false}
                  :bathymetry {:results      []
                               :loading?     false
                               :show-layers? false}
                  :habitat-observations {:global-archive nil
                                        :sediment       nil
                                        :squidle        nil
                                        :loading?       false
                                        :show-layers?   false}}}}

   ;; Feature-specific URL paths
   :url-paths {:amp-boundaries        "habitat/ampboundaries"
               :imcra-boundaries      "habitat/imcraboundaries"
               :meow-boundaries       "habitat/meowboundaries"
               :habitat-statistics    "habitat/habitatstatistics"
               :bathymetry-statistics "habitat/bathymetrystatistics"
               :habitat-observations  "habitat/habitatobservations"
               :region-reports        "regionreports/"
               :region-report-pages   "region-reports/"}})

;;; =============================================================================
;;; TASMANIA MARINE ATLAS
;;; =============================================================================

(def tas-marine-atlas
  {:id          :tas-marine-atlas
   :css-class   "tas-marine-atlas"
   :drawer-class "tas-marine-atlas-drawer"

   ;; Branding
   :branding
   {:logo        {:src    "img/TMA_Banner_size_website.png"
                  :alt    "Tasmania Marine Atlas"
                  :height 86}
    :url         "https://tasmarineatlas.org"
    :title       "Tasmania's Marine Atlas"
    :welcome     {:title       [:<> "Welcome to" [:br] "Tasmania's Marine Atlas."]
                  :content     :welcome/tas-marine-atlas
                  :button-text "Explore the Atlas"}
    :labels      {:region-control   "Find Data in Region"
                  :transect-control "Measure"
                  :layer-info       "Layer info"}
    :links       {:main-site    "https://tasmarineatlas.org"
                  :sea-country  "https://storymaps.arcgis.com/stories/22672ae2d60f424f8d1e024232cfa885"}}

   ;; Color overrides (TMA has custom color scheme)
   :colors      {:color-1 "rgb(18, 37, 55)"
                 :color-2 "rgb(11, 80, 113)"
                 :color-8 "rgb(98, 191, 236)"}

   ;; Map configuration
   :map         {:crs    :epsg-3857
                 :center [-42.20157676555315 146.74253188097842]
                 :zoom   7}

   ;; Feature flags - TMA has data-in-region instead of state-of-knowledge.
   ;; The transect control is present but labelled "Measure" (no habitat plot).
   :features    #{:feature/featured-maps
                  :feature/data-in-region
                  :feature/floating-pills
                  :feature/transect-control
                  :feature/settings
                  :feature/pinned-legends}

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs          [:catalogue :active-layers :featured-maps]
                                  :close-button? false} ; the menu control toggles the drawer
                 :right-drawer   {:story-map      :featured-map/drawer
                                  :data-in-region :data-in-region/panel
                                  :dynamic-pill   :dynamic-pill/drawer}
                 :footer         []  ; No plot component for TMA
                 ;; No dynamic pills: TMA fetches them but has never defined any, and lacks
                 ;; the :ui/open-pill handlers the interactive pill needs. Add :pill/dynamic
                 ;; and those handlers together if a pill is ever configured for TMA.
                 :floating-pills [:pill/side-by-side-views]
                 :map-overlays   [:overlay/pinned-legends]
                 :controls       {:top-left  [:control/menu :control/settings :control/zoom-basic
                                              [:control/print :control/omnisearch :control/transect
                                               :control/data-in-region :control/share :control/reset]
                                              [:control/shortcuts :control/help]]
                                  :top-right [:control/layers]}
                 ;; No transect plot, so no "Toggle Transect Panel"; measure/region relabelled
                 :hotkeys        [:zoom-in :zoom-out :pan-up :pan-down :pan-left :pan-right
                                  :drawer
                                  {:id :transect :label "Start/Clear Measure"}
                                  {:id :region   :label "Start/Clear Find Data in Region"}
                                  :cancel :search :reset :share :help]
                 ;; TODO: Separate helper overlay for TasMarineAtlas?
                 :helper-overlay [{:selector       ".SelectionListItem:first-child .layer-card .layer-header"
                                   :helperPosition "bottom"
                                   :helperText     "Toggle layer visibility, view info and metadata, show legend, adjust transparency, choose from download options (habitat data)"
                                   :padding        0}
                                  {:id "state-of-knowledge-pill"
                                   :helperText "Select a management region to view summaries of the current state of research knowledge for the area"}
                                  {:selector       ".leaflet-control-layers-toggle"
                                   :helperText     "Select from available basemaps"
                                   :helperPosition "left"}
                                  {:id "menu-button" :helperText "Expand/Collapse data menu"}
                                  {:id "zoom-in-control" :helperText "Zoom in"}
                                  {:id "zoom-out-control" :helperText "Zoom out"}
                                  {:id "layer-search" :helperText "Freetext search for a specific layer by name or keywords"}
                                  {:id "print-control" :helperText "Export current map view as an image"}
                                  {:id "omnisearch-control" :helperText "Search all available layers in catalogue"}
                                  {:id "transect-control" :helperText "Take a measurement"}
                                  {:id "select-control" :helperText "Search for data in region"}
                                  {:id "share-control" :helperText "Create a shareable URL for current map view"}
                                  {:id "reset-control" :helperText "Reset the application back to its initial state"}
                                  {:id "shortcuts-control" :helperText "View keyboard shortcuts"}
                                  {:id "overlay-control" :helperText "You are here!"}
                                  {:selector       ".bp3-tab-panel.catalogue>.bp3-tabs>.bp3-tab-list"
                                   :helperText     "Filter layers by category or responsible organisation"
                                   :helperPosition "bottom"
                                   :padding        0}]}

   ;; Feature-specific state initialization
   :initial-state
   {:display {:welcome-overlay true   ;; TMA shows welcome by default
              :catalogue {:main   {:tab "cat" :expanded #{}}
                          :region {:tab "cat" :expanded #{}}}}
    :data-in-region {:data     nil
                     :query-id nil}}

   ;; Feature-specific URL paths
   :url-paths {:data-in-region "habitat/datainregion"}})

;;; =============================================================================
;;; SEAMAP ANTARCTICA
;;; =============================================================================

(def seamap-antarctica
  {:id          :seamap-antarctica
   :css-class   "seamap"
   :drawer-class "seamap-drawer"

   ;; Branding
   :branding
   {:logo        {:src    "img/SeaMapAntarctica_Logo_RGB_3000px.png"
                  :alt    "Seamap Antarctica"
                  :height 96}
    :url         "https://seamapantarctica.org"
    :title       "Seamap Antarctica"
    :welcome     {:title   "Welcome to Seamap Antarctica."
                  :content :welcome/seamap-antarctica}
    :labels      {:transect-control "Transect/Measure"
                  :layer-info       "Layer info"}}

   ;; Map configuration - Antarctic polar projection
   :map         {:crs      :epsg-3031
                 :center   [-80 -90]
                 :zoom     2
                 :min-zoom 1
                 :max-zoom 15}  ; ISA-657 (see imas-seamap.interop.leaflet/crs-epsg3031)

   ;; Feature flags. The current Antarctica layout has no region-select control
   ;; and no SOK pills (its floating-pills only shows side-by-side views), but it
   ;; does register the SOK handlers and has a Featured Maps tab.
   :features    #{:feature/state-of-knowledge
                  :feature/featured-maps
                  :feature/floating-pills
                  :feature/transect-control
                  :feature/settings
                  :feature/viewport-only
                  :feature/pinned-legends
                  :feature/layer-preview}

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers :featured-maps]}
                 :right-drawer   {:story-map          :featured-map/drawer
                                  :state-of-knowledge :sok/panel
                                  :dynamic-pill       :dynamic-pill/drawer}
                 :footer         []  ; No plot component
                 :floating-pills [:pill/side-by-side-views]
                 :map-overlays   [:overlay/pinned-legends]
                 :controls       {:top-left  [:control/menu :control/settings :control/zoom
                                              [:control/print :control/omnisearch :control/transect
                                               :control/share :control/reset]
                                              [:control/shortcuts :control/help]]
                                  :top-right [:control/layers]}}

   ;; Feature-specific state initialization (same as Australia)
   :initial-state
   {:display {:welcome-overlay false}
    :state-of-knowledge
    {:boundaries {:active-boundary nil
                  :active-boundary-layer nil
                  :amp   {:active-network   nil
                          :active-park      nil
                          :active-zone      nil
                          :active-zone-iucn nil
                          :active-zone-id   nil}
                  :imcra {:active-provincial-bioregion nil
                          :active-mesoscale-bioregion  nil}
                  :meow  {:active-realm     nil
                          :active-province  nil
                          :active-ecoregion nil}}
     :statistics {:habitat {:results      []
                            :loading?     false
                            :show-layers? false}
                  :bathymetry {:results      []
                               :loading?     false
                               :show-layers? false}
                  :habitat-observations {:global-archive nil
                                        :sediment       nil
                                        :squidle        nil
                                        :loading?       false
                                        :show-layers?   false}}}}

   ;; Feature-specific URL paths (same as Australia)
   :url-paths {:amp-boundaries        "habitat/ampboundaries"
               :imcra-boundaries      "habitat/imcraboundaries"
               :meow-boundaries       "habitat/meowboundaries"
               :habitat-statistics    "habitat/habitatstatistics"
               :bathymetry-statistics "habitat/bathymetrystatistics"
               :habitat-observations  "habitat/habitatobservations"
               :region-reports        "regionreports/"
               :region-report-pages   "region-reports/"}})

;;; =============================================================================
;;; FUTURES OF SEAFOOD
;;; =============================================================================

(def futures-of-seafood
  {:id          :futures-of-seafood
   :css-class   "futures-of-seafood"
   :drawer-class "fos-drawer"

   ;; Branding
   :branding
   {:logo        {:src    "img/Futures-of-Seafood-Logo-Reverse-400x218.png"
                  :alt    "Futures of Seafood"
                  :height 86}
    :url         "https://futuresofseafood.com.au"
    :title       "Futures of Seafood"
    :welcome     nil ; the FoS boot flow never opens the welcome dialogue
    :labels      {:region-control   "Select Habitat Data"
                  :transect-control "Transect/Measure"
                  :layer-info       "Layer info"}}

   ;; Color overrides (FoS has custom color scheme)
   :colors      {:color-2          "#062440"
                 :color-highlight-1 "#00b0a5"
                 :color-7          "white"}

   ;; Map configuration: FoS boots from the base db, so it starts where Seamap
   ;; Australia does.
   :map         {:crs    :epsg-3857
                 :center [-27.819644755 132.133333]
                 :zoom   4}

   ;; Feature flags - minimal feature set, plus the time slider for time-series layers
   :features    #{:feature/plot-component
                  :feature/transect-control
                  :feature/time-dimension
                  :feature/floating-pills
                  :feature/settings
                  :feature/viewport-only
                  :feature/region-select
                  :feature/pinned-legends
                  :feature/layer-preview}

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers]}
                 :right-drawer   {:dynamic-pill :dynamic-pill/drawer}
                 :footer         [:footer/plot]
                 :floating-pills [:pill/side-by-side-views]
                 :map-overlays   [:overlay/pinned-legends]
                 :controls       {:top-left  [:control/menu :control/settings :control/zoom
                                              [:control/print :control/omnisearch :control/transect
                                               :control/region :control/share :control/reset]
                                              [:control/shortcuts :control/help]]
                                  :top-right [:control/layers]}}

   ;; Feature-specific state initialization (minimal)
   :initial-state
   {:display {:welcome-overlay false}}

   ;; Feature-specific URL paths (none specific to FoS)
   :url-paths {}})

;;; =============================================================================
;;; NATURAL HAZARDS ATLAS
;;; =============================================================================

;; Not yet consumed by natural_hazards_atlas/core.cljs, which still boots without a
;; deployment config. Documents the deployment's current behaviour; see the
;; proposal's sections 5 to 7 for the boot-flow and persisted-state contributions
;; that are deliberately not expressed here yet.
(def natural-hazards-atlas
  {:id           :natural-hazards-atlas
   :css-class    "natural-hazards-atlas"
   :drawer-class "natural-hazards-atlas-drawer"

   :branding
   {:logo        {:src    "img/Climate_Futures_NHAT_logo_COLOUR.png"
                  :alt    "Natural Hazards Atlas"}
    :url         :url-base/wordpress               ; resolved at runtime
    :title       "Natural Hazards Atlas"
    :welcome     {:title            "Welcome to the Natural Hazards Atlas"
                  :content          :welcome/natural-hazards-atlas
                  :dont-show-again? true
                  :button-text      "Get Started"}
    :labels      {:region-control   "Select Area of Interest"
                  :layer-info       "Layer info"
                  :loading          "Loading Natural Hazards Atlas..."}
    :links       {:main-site  :url-base/wordpress
                  :how-to-use [:url-base/wordpress "resources/how-to-use-the-atlas/"]}}

   ;; Starts from bounds rather than a centre, to guarantee full coverage of
   ;; Tasmania. Max bounds match LISTmap's restricted extent.
   :map         {:crs        :epsg-3857
                 :bounds     {:west 142.975 :south -44.025 :east 150.025 :north -39.175}
                 :zoom       7
                 :min-zoom   2
                 :max-bounds {:west 133.849 :south -55.7054 :east 159.99 :north -34.4356}}

   ;; :feature/supporting-layers is deliberately absent: the Supporting Layers
   ;; catalogue tab is hidden until the client confirms the list.
   :features    #{:feature/region-select
                  :feature/layer-preview
                  :feature/floating-pills
                  :feature/side-by-side-maps
                  :feature/time-dimension
                  :feature/hazard-layers
                  :feature/autosave-control}

   :layout      {:map            :map/side-by-side
                 :left-drawer    {:tabs   [:catalogue :active-layers :nhat/current-view]
                                  :catalogue {:default-tab "hazards"
                                              :tabs [{:id         "hazards"
                                                      :title      "Hazards"
                                                      :layers-sub [:map.layers/filtered-hazard-layers]
                                                      :ordering   [:category :data_classification]}
                                                     {:id         "supporting-layers"
                                                      :title      "Supporting Layers"
                                                      :layers-sub [:map.layers/filtered-supporting-layers]
                                                      :ordering   [:data_classification]
                                                      :requires   :feature/supporting-layers}]}
                                  :footer [:nhat/how-to-use-banner]}
                 :right-drawer   {:dynamic-pill :dynamic-pill/drawer}
                 :footer         []
                 :floating-pills [:pill/side-by-side-views]
                 :map-overlays   [:nhat/hazard-layer-legend :nhat/no-data-warning]
                 :controls       {:top-left  [:control/menu :control/autosave :control/zoom
                                              [:control/print :control/omnisearch :control/region
                                               :control/share :control/reset]
                                              [:control/shortcuts :control/help]]
                                  :top-right [:control/layers]}
                 :omnisearch     {:layers-sub [:map.layers/hazard-layers]}
                 :hotkeys        [:zoom :zoom-numpad :pan :drawer :region :search :reset :share :help]}

   :initial-state
   {:display      {:welcome-overlay false
                   :catalogue {:main   {:tab "hazards" :expanded #{}}
                               :region {:tab "hazards" :expanded #{}}}}
    :current-view {:cmip-phases               []
                   :models                    []
                   :scenarios                 []
                   :seasonal-datas            []
                   :selected-cmip-phase-id    nil
                   :selected-model-id         nil
                   :selected-scenario-id      nil
                   :selected-seasonal-data-id nil
                   :selected-time-period-id   "medium"
                   :is-historic?              false}}

   ;; :layer and :layer-legend override the base paths (extra Thredds styling info).
   :url-paths   {:layer             "nhat/layers/"
                 :layer-legend      "nhat/layerlegend/"
                 :cmip-phases       "nhat/cmipphases/"
                 :scientific-models "nhat/scientificmodels/"
                 :scenarios         "nhat/scenarios/"
                 :seasons           "nhat/seasons/"}})

;;; =============================================================================
;;; TMAG KIOSK
;;; =============================================================================

;; Not yet consumed by tmag_kiosk/core.cljs. An unattended touchscreen at the
;; Tasmanian Museum and Art Gallery: map plus a permanently open Featured Maps
;; drawer, and nothing else. Reset sits top-right because the kiosk has no other
;; way to clear search-added layers.
(def tmag-kiosk
  {:id           :tmag-kiosk
   :css-class    "seamap tmag-kiosk"
   :drawer-class "seamap-drawer"

   :branding
   {:logo        {:src    "img/Seamap2_V2_RGB.png"
                  :alt    "Seamap Australia"}
    :url         "https://seamapaustralia.org/"
    :title       "Seamap Australia (TMAG kiosk)"
    :welcome     nil                               ; no welcome dialogue
    :labels      {:layer-info "Layer info"}}

   :map         {:crs    :epsg-3857
                 :center [-27.819644755 132.133333]
                 :zoom   4}

   :features    #{:feature/featured-maps}

   :layout      {:map            :map/single
                 :left-drawer    {:collapsible? false   ; always open: no close button, no menu control
                                  :tabs         nil     ; a single panel, no tab strip
                                  :component    :drawer/featured-maps}
                 :right-drawer   {:story-map :featured-map/drawer}
                 :footer         []
                 :floating-pills []
                 :map-overlays   []
                 :controls       {:top-left  [:control/zoom [:control/omnisearch]]
                                  :top-right [[:control/reset]]}
                 :hotkeys        [:zoom :pan :search :reset]}

   :initial-state
   {:display {:welcome-overlay false
              :left-drawer     true}}

   :url-paths   {}})

;;; =============================================================================
;;; DEPLOYMENT REGISTRY
;;; =============================================================================

(def deployments
  "Registry of all deployment configurations, keyed by deployment ID"
  {:seamap-australia      seamap-australia
   :tas-marine-atlas      tas-marine-atlas
   :seamap-antarctica     seamap-antarctica
   :futures-of-seafood    futures-of-seafood
   :natural-hazards-atlas natural-hazards-atlas
   :tmag-kiosk            tmag-kiosk})

(def all-features
  "Every feature keyword a deployment may declare. Keep in sync with the
   Feature Variations table in docs/configuration-driven-refactor-proposal.md."
  #{:feature/state-of-knowledge  ; SOK panel, boundaries, statistics
    :feature/boundaries-pill     ; SOK boundaries floating pill
    :feature/zones-pill          ; SOK AMP zones floating pill
    :feature/data-in-region      ; TMA data-in-region panel and control
    :feature/featured-maps       ; story-map drawer tab and right drawer
    :feature/floating-pills      ; the floating-pills bar (dynamic and side-by-side pills)
    :feature/plot-component      ; transect depth-profile plot under the map
    :feature/transect-control    ; transect / measure control
    :feature/region-select       ; region-select control
    :feature/data-providers      ; data-providers listing (site configuration)
    :feature/data-download       ; download options in layer info
    :feature/settings            ; settings button and overlay
    :feature/viewport-only       ; "viewport layers only" catalogue filter toggle in settings
    :feature/pinned-legends      ; pinned legends panels over the map
    :feature/layer-preview       ; hover preview image for catalogue layers
    :feature/side-by-side-maps   ; two synchronised maps with a divider (NHAT)
    :feature/time-dimension      ; timeline controls for time-series layers (NHAT)
    :feature/hazard-layers       ; current-view controls, hazard legend, one-active-hazard rule (NHAT)
    :feature/supporting-layers   ; NHAT supporting-layers catalogue tab (currently hidden)
    :feature/autosave-control})  ; autosave toggle in the map control bar (NHAT)

;;; =============================================================================
;;; HELPER FUNCTIONS
;;; =============================================================================

(defn get-deployment
  "Get a deployment configuration by ID.
   Throws an error if deployment ID is not found."
  [deployment-id]
  (or (get deployments deployment-id)
      (throw (ex-info "Unknown deployment ID"
                      {:deployment-id deployment-id
                       :available-ids (keys deployments)}))))

(defn feature-enabled?
  "Check if a feature is enabled for a deployment"
  [deployment-config feature-key]
  (contains? (:features deployment-config) feature-key))
