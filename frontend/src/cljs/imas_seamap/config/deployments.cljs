;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.config.deployments
  "Deployment configurations for all Seamap instances.

   Each deployment configuration defines:
   - Unique identifier and CSS class
   - Branding (logo, title, colors, welcome content)
   - Map configuration (CRS, center, zoom)
   - Feature flags (which features are enabled)
   - UI layout (tabs, drawers, controls)
   - Initial state overrides
   - Deployment-specific URL paths")

;;; FIXME: if features are name-spaced we can more-easily search for them, find all uses of eg ":feature/" etc

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
    :welcome     {:title   "Welcome to Seamap Australia."
                  :content :welcome/seamap-australia}
    :labels      {:region-control    "Select region"
                  :transect-control  "Draw transect (habitat data) or take a measurement"
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
                  :feature/layer-preview}

   ;; Right drawer slot mappings (what can appear in the right drawer)
   :drawers     {:story-map          :featured-map/drawer
                 :state-of-knowledge :sok/panel}

   ;; Left drawer tabs (ordered)
   :tabs        [:catalogue :active-layers :featured-maps]

   ;; Map controls (ordered, displayed in UI)
   :controls    [:menu :settings :zoom :print :omnisearch
                 :transect :region :share :reset :shortcuts :help]

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers :featured-maps]}
                 :footer         [:footer/plot]
                 :floating-pills [:pill/boundaries :pill/zones]
                 :controls       [:control/menu :control/settings :control/zoom
                                  :control/print :control/omnisearch :control/transect
                                  :control/region :control/share :control/reset
                                  :control/shortcuts :control/help]}

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
    :welcome     {:title   "Welcome to Tasmania's Marine Atlas."
                  :content :welcome/tas-marine-atlas}
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

   ;; Feature flags - TMA has data-in-region instead of state-of-knowledge
   ;; Note: TMA does NOT have :settings feature enabled
   :features    #{:feature/featured-maps
                  :feature/data-in-region
                  :feature/floating-pills}

   ;; Right drawer slot mappings
   :drawers     {:story-map      :featured-map/drawer
                 :data-in-region :data-in-region/panel}

   ;; Left drawer tabs (ordered)
   :tabs        [:catalogue :active-layers :featured-maps]

   ;; Map controls (no transect control for TMA)
   :controls    [:menu :settings :zoom :print :omnisearch
                 :region :share :reset :shortcuts :help]

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers :featured-maps]}
                 :footer         []  ; No plot component for TMA
                 :floating-pills []  ; TMA has floating-pills feature but no specific pills configured
                 :controls       [:control/menu :control/settings :control/zoom
                                  :control/print :control/omnisearch :control/region
                                  :control/share :control/reset :control/shortcuts
                                  :control/help]}

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
    :labels      {:region-control   "Select region"
                  :transect-control "Take a measurement"
                  :layer-info       "Layer info"}}

   ;; Map configuration - Antarctic polar projection
   :map         {:crs    :epsg-3031
                 :center [-80 -90]
                 :zoom   2}

   ;; Feature flags - Antarctica has state-of-knowledge and floating pills
   :features    #{:feature/state-of-knowledge
                  :feature/floating-pills
                  :feature/boundaries-pill
                  :feature/zones-pill
                  :feature/settings
                  :feature/region-select
                  :feature/layer-preview}

   ;; Right drawer slot mappings
   :drawers     {:state-of-knowledge :sok/panel}

   ;; Left drawer tabs (no featured maps for Antarctica)
   :tabs        [:catalogue :active-layers]

   ;; Map controls
   :controls    [:menu :settings :zoom :print :omnisearch
                 :region :share :reset :shortcuts :help]

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers]}
                 :footer         []  ; No plot component
                 :floating-pills [:pill/boundaries :pill/zones]
                 :controls       [:control/menu :control/settings :control/zoom
                                  :control/print :control/omnisearch :control/region
                                  :control/share :control/reset :control/shortcuts
                                  :control/help]}

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
    :welcome     {:title   "Welcome to Futures of Seafood."
                  :content :welcome/futures-of-seafood}
    :labels      {:region-control   "Select region"
                  :transect-control "Draw transect (habitat data) or take a measurement"
                  :layer-info       "Layer info"}}

   ;; Color overrides (FoS has custom color scheme)
   :colors      {:color-2          "#062440"
                 :color-highlight-1 "#00b0a5"
                 :color-7          "white"}

   ;; Map configuration (same as Australia)
   :map         {:crs    :epsg-3857
                 :center [-28.0 134.0]
                 :zoom   5}

   ;; Feature flags - minimal feature set
   :features    #{:feature/plot-component
                  :feature/transect-control
                  :feature/settings
                  :feature/region-select
                  :feature/layer-preview}

   ;; Right drawer slot mappings (empty for FoS)
   :drawers     {}

   ;; Left drawer tabs (basic catalogue and active layers only)
   :tabs        [:catalogue :active-layers]

   ;; Map controls
   :controls    [:menu :settings :zoom :print :omnisearch
                 :transect :region :share :reset :shortcuts :help]

   ;; Layout configuration (Phase 4: Unified layout)
   :layout      {:left-drawer    {:tabs [:catalogue :active-layers]}
                 :footer         [:footer/plot]
                 :floating-pills []  ; No floating pills for FoS
                 :controls       [:control/menu :control/settings :control/zoom
                                  :control/print :control/omnisearch :control/transect
                                  :control/region :control/share :control/reset
                                  :control/shortcuts :control/help]}

   ;; Feature-specific state initialization (minimal)
   :initial-state
   {:display {:welcome-overlay false}}

   ;; Feature-specific URL paths (none specific to FoS)
   :url-paths {}})

;;; =============================================================================
;;; DEPLOYMENT REGISTRY
;;; =============================================================================

(def deployments
  "Registry of all deployment configurations, keyed by deployment ID"
  {:seamap-australia   seamap-australia
   :tas-marine-atlas   tas-marine-atlas
   :seamap-antarctica  seamap-antarctica
   :futures-of-seafood futures-of-seafood})

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
