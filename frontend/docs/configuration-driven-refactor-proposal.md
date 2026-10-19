# Configuration-Driven Architecture Refactor Proposal

## Status (updated 2026-10-15)

This document was written against a four-deployment codebase. Since then:

- **Phases 1 to 3 are implemented** on `feature/configuration-driven-app`: `config/deployments.cljs`, `config/content.cljs`, `subs/features.cljs`, and `subs/branding.cljs` exist; the `tma?` flag is gone from all leaf components; settings, region select, the floating pills, and the layer preview are feature-gated; the drawer logo, link, drawer class, and the layer-info tooltip read from branding config (the welcome dialogue, control tooltips, colours, and links have subscriptions but no consumers yet); `imas-seamap.core/init` accepts a deployment id. Feature keywords are namespaced (`:feature/state-of-knowledge`), which the examples below now use.
- **Phase 4 is started**: an atom-based component registry (`config/components.cljs`), a `:layout` key in each deployment config, and the Seamap Australia `layout-app` reads its CSS class, footer slot, and several feature flags from config. The other deployments still have their own `layout-app`.
- **`develop` was merged on 2026-10-15.** It added two deployments (the Natural Hazards Atlas, NHAT, and the TMAG kiosk), side-by-side maps, a time dimension, pinned legends, image-based printing, and a `:dbsubs/*` subscription layer. Those changes invalidate several assumptions in the original text. Affected sections are marked **Updated**; sections 6 and 7 are **New** and cover ground the original proposal did not.
- Related documents: `handler-registration-options.md`, `configuration-refactor-remaining-tasks.md`, `testing-configuration-system.md`, and `ADRs/01-sub-handler-registration.md` at the repository root.

---

## Current State Analysis

### Deployments

**Updated.** The codebase supports **6 deployments**, each with its own `core.cljs` entry point and `views.cljs` layout:

| Deployment | Entry Point | CSS Class(es) | Own `db.cljs` | Own `events.cljs` |
|------------|-------------|---------------|---------------|-------------------|
| Seamap Australia | `imas_seamap/core.cljs` | `seamap` | base | base |
| Tasmania Marine Atlas (TMA) | `imas_seamap/tas_marine_atlas/core.cljs` | `tas-marine-atlas` | ✓ | ✓ |
| Seamap Antarctica | `imas_seamap/seamap_antarctica/core.cljs` | `seamap seamap-antarctica` | ✓ | ✓ |
| Futures of Seafood (FoS) | `imas_seamap/futuresofseafood/core.cljs` | `futures-of-seafood` | base | ✓ |
| Natural Hazards Atlas (NHAT) | `imas_seamap/natural_hazards_atlas/core.cljs` | `natural-hazards-atlas` | ✓ | ✓ (large) |
| TMAG kiosk | `imas_seamap/tmag_kiosk/core.cljs` | `seamap tmag-kiosk` | base | ✓ |

Two details the original proposal missed:

- `mount-root` adds the deployment class to `document.body` as well as to `#main-wrapper`, and SCSS selects on the body class (for example `.natural-hazards-atlas .leaflet-bar-timecontrol`). Any unified `mount-root` must do the same.
- The kiosk is served from its own `resources/public/tmag-kiosk.html` rather than through the Ansible-managed `index.html`. That file is not committed (it is untracked in the working tree), so the repository currently has no page that boots the kiosk.

### Current Patterns

1. **Handler Registry** - Each `core.cljs` defines a `config-handlers` map with `:subs` and `:events` (roughly 100 subs and 200 events, mostly identical across deployments)
2. **Handler Override** - **Updated.** Deployments do not only add handlers, they *replace* base ones. NHAT overrides about sixteen core events (`:boot`, `:construct-urls`, `:merge-state`, `:re-boot`, `:load-hash-state`, `:initialise-layers`, `:create-save-state`, `:maybe-autosave`, `:map/feature-info-dispatcher`, `:map/add-layer`, `:map/add-layer-from-omnibar`, `:map.layer/download`, `:map/popup-closed`, both `:map.time/*` events, `:ui.side-by-side/active?`) and three subs (`:map.layer/legend`, `:map.layer/displayed-layers-lookup`, `:map.time/available-times`). TMA and the kiosk override `:boot`, `:re-boot`, and `:initialise-layers`. NHAT and the kiosk also register a no-op `:sok/boundary-layer-filter` because a shared component used to subscribe to it unconditionally.
3. **Namespace-registered subs** - **Updated.** `map/subs.cljs` registers `:dbsubs/*` (layer 2) and `::msubs/*` (layer 3) subscriptions directly at load time, outside `config-handlers`. Registration is now a mixed model; see `ADRs/01-sub-handler-registration.md`.
4. **Flag Propagation** - Removed on the branch. The `tma?` boolean used to propagate through 3+ levels of components.
5. **Component Replacement** - Each deployment has its own `layout-app`. NHAT also has its own `map-component` (two synchronised Leaflet maps with a draggable divider), its own catalogue tabs, a bespoke "View Settings" drawer tab, and a drawer footer banner. Antarctica has its own `map-component` for the CRS.
6. **Open extension via multimethods** - `right-drawer` (`views.cljs`) and `legend` (`map/layer_views.cljs`) are multimethods, and NHAT adds a `:color-scale-bar` legend from its own namespace without touching core. This pattern already solves part of the problem this proposal addresses.
7. **Schema Variants** - Separate `db.cljs` files define different state shapes, and separate `encode-state` / `parse-state` / `ajax-loaded-info` functions (NHAT's in `natural_hazards_atlas/utils.cljs`) define different *persisted*-state shapes.

### Feature Variations by Deployment

**Updated.** ✓ means the deployment's layout renders it today. Where the branch's `deployments.cljs` disagrees with the code, the code is shown and the discrepancy noted.

| Feature | Australia | TMA | Antarctica | FoS | NHAT | Kiosk |
|---------|-----------|-----|------------|-----|------|-------|
| State-of-Knowledge (panel, pills) | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| Data-in-Region | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Featured Maps tab | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ (only tab) |
| Rich-layer side-by-side pills | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Pinned legends panel | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| On-map hazard legend (colour scale bar) | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Plot Component (transect) | ✓ | ✗ | ✗ | ✓ | ✗ | ✗ |
| Transect Control | ✓ | ✓ ("Measure") | ✓ ("Take a measurement") | ✓ | ✗ | ✗ |
| Region select control | ✓ | ✓ (data-in-region variant) | ✓ | ✓ | ✓ ("Select Area of Interest") | ✗ |
| Settings button | ✓ | ✓ (config says ✗) | ✓ | ✓ | ✗ | ✗ |
| Autosave toggle in control bar | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Print, share, shortcuts, help controls | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Omnisearch | ✓ | ✓ | ✓ | ✓ | ✓ (hazard layers only) | ✓ |
| Welcome dialogue | ✓ | ✓ | ✓ | ✓ | ✓ (+ "don't show again") | ✗ |
| Left drawer collapsible | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ (always open) |
| Catalogue sub-tabs | Category / Organisation | same | same | same | Hazards / Supporting | none |
| Drawer footer banner | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Side-by-side compare maps | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Time dimension / timeline controls | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Hotkey set | full | full | full | full | full + numpad zoom | zoom, pan, search, reset |
| Custom CRS | ✗ | ✗ | ✓ (EPSG:3031) | ✗ | ✗ | ✗ |
| Map max bounds | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Data Providers | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Extra boot requests | SOK, region reports | data-in-region | SOK | none | CMIP phases, models, scenarios, seasons | none |

### Current Problems

The main issue is **variant parent components**. When a child component 2-3 levels deep needs to behave differently:

1. The `tma?` flag must be passed through every intermediate component
2. Parent components that don't use the flag still need to accept and forward it
3. Adding a new variation requires modifying multiple files
4. Each deployment maintains its own `views.cljs` with a full `layout-app` copy

Example of current flag propagation in `views.cljs`:
```clojure
;; Level 1: left-drawer calls
[left-drawer-catalogue true]

;; Level 2: left-drawer-catalogue passes to layer-catalogue
(defn left-drawer-catalogue [tma?]
  [layer-catalogue :main catalogue-layers {...} tma?])

;; Level 3: layer-catalogue passes to layer-card
;; Level 4: layer-card uses it for tooltip text
```

**Updated.** A second problem, visible only now that NHAT exists, is **variation by override**. A deployment that needs a different boot sequence, a different persisted-state shape, or a different layer-URL resolution has no extension point, so it copies the core handler and edits the copy. NHAT carries three near-identical copies of the boot flow, a copy of `merge-state`, and copies of `encode-state` and `parse-state`. Every change to the core versions must be replayed by hand. Feature flags and layout slots do not help with this; sections 5 to 7 propose extension points for it.

A third problem is that the **map component itself varies**: Antarctica changes the CRS and zoom limits, NHAT renders two synchronised maps with max bounds. The original proposal treated the map as fixed.

---

## Proposed Configuration-Driven Architecture

### 1. Unified Deployment Configuration Schema

**Updated.** The schema is implemented for the original four deployments in `src/cljs/imas_seamap/config/deployments.cljs`. This section lists the keys, marks which are implemented, and shows the two new deployments, which need several extensions.

| Key | Purpose | Status |
|-----|---------|--------|
| `:id`, `:css-class`, `:drawer-class` | Identity and styling hooks. `:css-class` may hold several classes and is applied to `#main-wrapper` and `document.body`. | Implemented (body class: proposed) |
| `:branding` | Logo, URL, title, welcome dialogue, labels, links. See *Branding Configuration*. | Implemented; runtime URLs and dialogue options proposed |
| `:colors` | CSS custom property overrides. | Implemented |
| `:map` | `:crs`, `:center`, `:zoom`, plus `:bounds`, `:max-bounds`, `:min-zoom`, `:max-zoom`. NHAT starts from bounds, not a centre; Antarctica needs zoom limits. | Partly implemented |
| `:features` | Set of `:feature/*` keywords. | Implemented |
| `:layout` | Slots: `:map`, `:left-drawer`, `:right-drawer`, `:footer`, `:floating-pills`, `:map-overlays`, `:controls`, `:omnisearch`, `:hotkeys`. See section 3. | `:footer` only |
| `:initial-state` | Deep-merged into the base db. | Proposed |
| `:url-paths` | Merged over the base URL paths; may *override* base keys (NHAT replaces `:layer` and `:layer-legend`). | Proposed |
| `:boot` | Extra initial requests and the events the boot flow waits for. See section 6. | New |
| `:persisted-state` | Extra paths included in the URL hash, autosave, and the re-boot keep-list. See section 7. | New |

`:tabs`, `:controls`, and `:drawers` currently exist both at the top level and under `:layout`; drop the top-level copies once the layout reads `:layout`.

The Natural Hazards Atlas, expressed in the extended schema:

```clojure
(def natural-hazards-atlas
  {:id           :natural-hazards-atlas
   :css-class    "natural-hazards-atlas"
   :drawer-class "natural-hazards-atlas-drawer"

   :branding
   {:logo    {:src "img/Climate_Futures_NHAT_logo_COLOUR.png" :alt "Natural Hazards Atlas"}
    :url     :url-base/wordpress                 ; resolved at runtime (see Branding Configuration)
    :title   "Natural Hazards Atlas"
    :welcome {:title            ["Welcome to the" [:br] "Natural Hazards Atlas"]
              :content          :welcome/natural-hazards-atlas
              :dont-show-again? true              ; checkbox in the dialogue footer
              :button-text      "Get Started"}
    :labels  {:region-control "Select Area of Interest"
              :layer-info     "Layer info"
              :loading        "Loading Natural Hazards Atlas..."}
    :links   {:how-to-use [:url-base/wordpress "resources/how-to-use-the-atlas/"]}}

   :map {:crs        :epsg-3857
         :bounds     {:west 142.975 :south -44.025 :east 150.025 :north -39.175}
         :zoom       7
         :min-zoom   2
         :max-bounds {:west 133.849 :south -55.7054 :east 159.99 :north -34.4356}}

   :features #{:feature/region-select
               :feature/layer-preview
               :feature/side-by-side-maps
               :feature/time-dimension
               :feature/hazard-layers        ; current-view controls, hazard legend, one-active-hazard rule
               :feature/autosave-control}
               ;; :feature/supporting-layers is deliberately absent (hidden until the client confirms)

   :layout
   {:map          :map/side-by-side            ; two synced maps; see section 3
    :left-drawer  {:collapsible? true
                   :tabs [{:id "catalogue" :title "Catalogue" :tooltip "All available map layers"
                           :component :drawer/catalogue
                           :catalogue {:default-tab "hazards"
                                       :tabs [{:id "hazards" :title "Hazards"
                                               :layers-sub [:map.layers/filtered-hazard-layers]
                                               :ordering   [:category :data_classification]}
                                              {:id "supporting-layers" :title "Supporting Layers"
                                               :layers-sub [:map.layers/filtered-supporting-layers]
                                               :ordering   [:data_classification]
                                               :requires   :feature/supporting-layers}]}}
                          {:id "active-layers" :title "Active Layers" :component :drawer/active-layers}
                          {:id "current-view"  :title "View Settings" :tooltip "Configure the map layers"
                           :component :nhat/current-view}]
                   :footer [:nhat/how-to-use-banner]}
    :right-drawer {:dynamic-pill :dynamic-pill/drawer}
    :footer         []
    :floating-pills [:pill/side-by-side-views]
    :map-overlays   [:nhat/hazard-layer-legend :nhat/no-data-warning]
    :controls     {:top-left  [:control/menu :control/autosave :control/zoom
                               [:control/print :control/omnisearch :control/region :control/share :control/reset]
                               [:control/shortcuts :control/help]]      ; a nested vector is a control-block
                   :top-right [:control/layers]}
    :omnisearch   {:layers-sub [:map.layers/hazard-layers]}
    :hotkeys      [:zoom :zoom-numpad :pan :drawer :region :search :reset :share :help]}

   :initial-state
   {:display      {:catalogue {:main   {:tab "hazards" :expanded #{}}
                               :region {:tab "hazards" :expanded #{}}}}
    :current-view {:cmip-phases [] :models [] :scenarios [] :seasonal-datas []
                   :selected-cmip-phase-id nil :selected-model-id nil
                   :selected-scenario-id nil :selected-seasonal-data-id nil
                   :selected-time-period-id "medium" :is-historic? false}}

   :url-paths {:layer             "nhat/layers/"          ; overrides the base path
               :layer-legend      "nhat/layerlegend/"     ; overrides the base path
               :cmip-phases       "nhat/cmipphases/"
               :scientific-models "nhat/scientificmodels/"
               :scenarios         "nhat/scenarios/"
               :seasons           "nhat/seasons/"}

   ;; Sections 6 and 7. In practice these would live in the :feature/hazard-layers
   ;; module and be pulled in by the feature flag, not written per deployment.
   :boot            {:requests [{:url :cmip-phases-url       :on-success :current-view/update-cmip-phases}
                                {:url :scientific-models-url :on-success :current-view/update-models}
                                {:url :scenarios-url         :on-success :current-view/update-scenarios}
                                {:url :seasons-url           :on-success :current-view/update-seasonal-datas}]
                     :wait-for [:current-view/update-cmip-phases :current-view/update-models
                                :current-view/update-scenarios   :current-view/update-seasonal-datas]}
   :persisted-state {:hash   [[:current-view :selected-cmip-phase-id] [:current-view :selected-model-id]
                              [:current-view :selected-scenario-id]   [:current-view :selected-seasonal-data-id]
                              [:current-view :is-historic?]           [:current-view :selected-time-period-id]
                              [:display :side-by-side :active?]       [:display :side-by-side :split-ratio]
                              [:display :current-time]]
                     :loaded [[:current-view :cmip-phases] [:current-view :models]
                              [:current-view :scenarios]   [:current-view :seasonal-datas]]}})
```

The TMAG kiosk, which is the smallest deployment and the best second migration target:

```clojure
(def tmag-kiosk
  {:id           :tmag-kiosk
   :css-class    "seamap tmag-kiosk"
   :drawer-class "seamap-drawer"

   :branding {:logo    {:src "img/Seamap2_V2_RGB.png" :alt "Seamap Australia"}
              :url     "https://seamapaustralia.org/"
              :title   "Seamap Australia (TMAG kiosk)"
              :welcome nil}                         ; no welcome dialogue

   :map      {:crs :epsg-3857 :center [-27.819644755 132.133333] :zoom 4}
   :features #{:feature/featured-maps}

   :layout   {:map          :map/single
              :left-drawer  {:collapsible? false      ; always open: no close button, no menu control
                             :tabs         nil        ; a single panel, no tab strip
                             :component    :drawer/featured-maps}
              :right-drawer {:story-map :featured-map/drawer}
              :footer [] :floating-pills [] :map-overlays []
              :controls {:top-left  [:control/zoom [:control/omnisearch]]
                         :top-right [[:control/reset]]}
              :hotkeys  [:zoom :pan :search :reset]}

   :initial-state {:display {:left-drawer true}}})
```

### 2. Feature-Gated Component Rendering

**Implemented** in `subs/features.cljs`; shown here for reference. Prefer `:branding/label` for text differences and `feature?` for presence differences.

Replace flag propagation with subscription-based feature checks:

```clojure
;; src/cljs/imas_seamap/subs/features.cljs

(re-frame/reg-sub
 :deployment/config
 (fn [db _]
   (get db :deployment-config)))   ; as implemented; :config holds url-paths/urls/url-base

(re-frame/reg-sub
 :feature/enabled?
 :<- [:deployment/config]
 (fn [config [_ feature-key]]
   (contains? (:features config) feature-key)))

(re-frame/reg-sub
 :feature/all
 :<- [:deployment/config]
 (fn [config _]
   (:features config)))

;; Convenience macro for components
(defn feature? [feature-key]
  @(re-frame/subscribe [:feature/enabled? feature-key]))
```

Usage in components (no more `tma?` parameter):

```clojure
;; Before (current)
(defn layer-card [{:keys [layer tma?]}]
  [:div.layer-card
   [layer-card-content layer]
   [layer-control
    {:tooltip (if tma? "Layer info" "Layer info / Download data")}]])

;; After (proposed)
(defn layer-card [{:keys [layer]}]
  (let [show-download? (feature? :feature/data-download)]
    [:div.layer-card
     [layer-card-content layer]
     [layer-control
      {:tooltip (if show-download?
                  "Layer info / Download data"
                  "Layer info")}]]))
```

### 3. Slot-Based Layout System

**Updated.** One `layout-app` for all deployments, with the slots below. The current implementation (Seamap Australia only) covers `:footer` plus feature-gated pills, settings, and layer preview. The NHAT sidebar and map are the reason for most of the additions.

| Slot | Config key | Notes |
|------|------------|-------|
| Map | `:layout :map` | `:map/single` (default) or `:map/side-by-side`. See *The map slot* below. |
| Left drawer | `:layout :left-drawer` | `:collapsible?`; ordered `:tabs`, each with a `:component` and an optional `:catalogue` sub-tab config; or `:tabs nil` with a single `:component`; optional `:footer` components rendered under the tabs. |
| Right drawer | `:layout :right-drawer` | Map of drawer type to component key. Can stay the existing `right-drawer` multimethod; NHAT uses only the dynamic-pill drawer. |
| Footer | `:layout :footer` | Below the map (transect plot). Implemented. |
| Floating pills | `:layout :floating-pills` | Pill component keys. The side-by-side-views pill is common to five deployments and is currently copied into three `views.cljs` files. |
| Map overlays | `:layout :map-overlays` | Components positioned over the map that are not Leaflet controls: pinned legend panels, NHAT's hazard legend and no-data warning. |
| Controls | `:layout :controls` | Keyed by Leaflet corner; a nested vector is a `control-block`. Needed because the kiosk puts reset top-right and NHAT adds an autosave button. |
| Omnisearch | `:layout :omnisearch` | Optional `:layers-sub` restricting the searchable layers (NHAT searches hazard layers only). |
| Hotkeys | `:layout :hotkeys` | Which hotkey groups are active. The kiosk omits the drawer, help, share, and region keys; NHAT adds numpad zoom. |

```clojure
;; src/cljs/imas_seamap/views/layout.cljs

(defn layout-app []
  (let [{:keys [map left-drawer footer floating-pills map-overlays controls]}
        @(re-frame/subscribe [:layout/config])
        css-class @(re-frame/subscribe [:branding/css-class])]
    [:div#main-wrapper {:class (str css-class (layout-state-classes))}   ; catalogue-open, plot-open, map-printing ...
     [:div#content-wrapper
      [(resolve-component (or map :map/single))]
      (for [k footer]       ^{:key k} [(resolve-component k)])
      (for [k map-overlays] ^{:key k} [(resolve-component k)])]
     [welcome-dialogue]                       ; renders nothing when (:welcome branding) is nil
     [outage-message-dialogue]
     (when (feature? :feature/settings) [settings-overlay])
     [info-card]
     [download-component]
     [loading-display]
     [left-drawer-slot left-drawer]
     [right-drawer @(re-frame/subscribe [:ui/right-sidebar])]
     [layers-search-omnibar]
     [controls-slot controls]
     (when (seq floating-pills) [floating-pills-slot floating-pills])
     (when (feature? :feature/layer-preview)
       [layer-preview @(re-frame/subscribe [:ui/preview-layer-url])])]))

(defn left-drawer-slot [{:keys [collapsible? tabs component footer]}]
  (let [open? (or (not collapsible?) @(re-frame/subscribe [:left-drawer/open?]))]
    [components/drawer {:isOpen open? :isCloseButtonShown false :hasBackdrop false ...}
     (if tabs
       [b/tabs {:selected-tab-id @(re-frame/subscribe [:left-drawer/tab]) ...}
        (for [{:keys [id title tooltip component catalogue]} tabs]
          ^{:key id}
          [b/tab {:id id
                  :title (r/as-element [b/tooltip {:content tooltip} title])
                  :panel (r/as-element [(resolve-component component) {:catalogue catalogue}])}])]
       [(resolve-component component)])
     (for [k footer] ^{:key k} [(resolve-component k)])]))

(defn controls-slot [{:keys [top-left top-right]}]
  [:<>
   [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch  (render-controls top-left)]
   [:div.custom-leaflet-controls.leaflet-top.leaflet-right.leaflet-touch (render-controls top-right)]])
```

The catalogue component reads its sub-tab list from the `:catalogue` prop instead of hard-coding "By Category" and "By Organisation", and the default catalogue tab id moves from `db.cljs` into that config.

#### The map slot

Two ways to handle NHAT's two-map view:

- **Slot (first step).** Register NHAT's existing `map-component` as `:map/side-by-side` and leave it in the NHAT namespace. Cheapest, but keeps the duplicated Leaflet container setup.
- **Flag (target).** Fold the second map into the shared `map-component` behind `:feature/side-by-side-maps`. `develop` already threads a `map-id` through `catalogue-layers`, `:map.feature/info`, `popup`, and the time events, so most of the plumbing exists. The shared component would also read `:min-zoom`, `:max-zoom`, `:max-bounds`, and `:crs` from `:map/props`, which removes the Antarctica map file as well.

Recommendation: slot now, flag after sections 5 to 7 land, because side-by-side also depends on NHAT's event overrides.

### 4. Component Registry

**Updated.** The implemented registry (`config/components.cljs`) is an atom populated at namespace load, not the static map originally proposed. That is the right choice for NHAT and the kiosk: their bespoke components are registered from their own namespaces, so core never requires deployment code.

```clojure
;; natural_hazards_atlas/views.cljs
(components-registry/register-components!
 {:nhat/current-view        current-view
  :nhat/how-to-use-banner   how-to-use-banner
  :nhat/hazard-layer-legend hazard-layer-legend
  :nhat/no-data-warning     no-data-warning
  :map/side-by-side         nhat-map/map-component})

;; views.cljs (already in place)
(components-registry/register-components!
 {:footer/plot          plot-component
  :drawer/catalogue     left-drawer-catalogue
  :drawer/active-layers left-drawer-active-layers
  :drawer/featured-maps featured-maps
  :pill/boundaries      floating-boundaries-pill
  :pill/zones           floating-zones-pill
  ...})
```

Two consequences:

1. **Load order.** A deployment's namespace must be loaded before `layout-app` first renders. With a single entry point (section 8) either `core.cljs` requires every deployment namespace, or the build emits one module per deployment. `resolve-component` should log a clear error for a missing key rather than silently render nothing.
2. **Prefer multimethods where the dispatch value is data.** `right-drawer` and `legend` already use `defmethod`, and NHAT extends `legend` from its own namespace. Keep that pattern for "render X according to its `:type`" cases; use the registry for layout slots chosen by config.

### 5. Feature-Based Handler Registration

**Updated.** The original design ("base handlers plus per-feature additions") cannot express what the deployments actually do. See `handler-registration-options.md` for the alternatives considered and `ADRs/01-sub-handler-registration.md` for the map-valued sub syntax that `register-handlers!` should also accept.

Requirements learned from `develop`:

1. **Features add handlers.** SOK adds `:sok/*`; NHAT adds `:current-view/*`, `:map.layers/hazard-layers`, and so on. This was the original requirement.
2. **Features replace handlers.** NHAT replaces `:map.layer/legend`, `:map.layer/displayed-layers-lookup`, `:map/add-layer`, `:map/feature-info-dispatcher`, and a dozen more with versions that know about hazard layers and the second map. Registration must be ordered (base first, then features in a fixed order, last write wins) and the registry should warn when a feature overrides a base key so overrides stay visible.
3. **Most overrides should become extension points.** NHAT's overrides exist for four reasons; three of them can be hooks in core rather than copies of the handler:
   - *Boot sequence* (`:boot`, `:re-boot`, `:initialise-layers`, `:construct-urls`, `:load-hash-state`, `:merge-state`): section 6.
   - *Persisted-state shape* (`:create-save-state`, `:maybe-autosave`, `:merge-state`, `:re-boot`): section 7.
   - *Displayed-layer resolution* (`:map.layer/displayed-layers-lookup`, `:map/feature-info-dispatcher`, `:map.layer/download`): make layer-to-displayed-layer resolution one function in `map/utils.cljs` that consults a resolver registry (or a multimethod on layer kind). NHAT registers one resolver for `:hazardlayer` layers and the three overrides disappear.
   - *Behavioural rules* (`:map/add-layer` "only one hazard layer active", `:map.time/current-time` "pause at the end"): leave as overrides. They are genuinely NHAT behaviour.
4. **Subs registered outside the map are fine.** `:dbsubs/*` and `::msubs/*` are registered at load; the feature registry only needs to own the public `:map/...`, `:sok/...` style keys.
5. **The no-op `:sok/boundary-layer-filter` goes away** now that `catalogue-layers` gates the subscription on `:feature/state-of-knowledge`.

```clojure
;; src/cljs/imas_seamap/handlers/registry.cljs

(def feature-handlers
  "Feature keyword -> {:subs {...} :events {...}}. Each feature module owns its map."
  {:feature/state-of-knowledge sok-registry/handlers
   :feature/data-in-region     dir-registry/handlers
   :feature/featured-maps      sm-registry/handlers
   :feature/hazard-layers      nhat-registry/handlers        ; adds :current-view/*, overrides :map/add-layer ...
   :feature/side-by-side-maps  side-by-side-registry/handlers})

(def feature-order
  "Registration order. Later features may override earlier ones."
  [:feature/featured-maps :feature/state-of-knowledge :feature/data-in-region
   :feature/side-by-side-maps :feature/hazard-layers])

(defn handlers-for [features]
  (reduce (fn [acc feature]
            (let [{:keys [subs events]} (get feature-handlers feature)]
              (doseq [k (keys subs)   :when (contains? (:subs acc) k)]   (js/console.warn "sub override" feature k))
              (doseq [k (keys events) :when (contains? (:events acc) k)] (js/console.warn "event override" feature k))
              (-> acc (update :subs merge subs) (update :events merge events))))
          base-handlers
          (filter (set features) feature-order)))

(defn register-for-features! [features]
  (register-handlers! (handlers-for features)))
```

Validation step: for each of the six deployments, diff `(handlers-for (:features config))` against the keys of today's `config-handlers` map before deleting any of them.

### 6. Boot Flow and Data Loading

**New.** Each deployment has its own copy of three async-flow definitions (`boot-flow`, `boot-flow-hash-state`, `boot-flow-save-state`) and its own `initialise-layers`, differing only in which requests are made and which responses the flow waits for before `:map/initialise-display`. NHAT adds four requests and waits for four events; TMA, NHAT, and the kiosk skip the SOK and region-report requests.

Proposal: core builds the flow from contributions, and features (or the deployment's `:boot` key) contribute requests, wait-events, and URL paths.

```clojure
;; A feature module contributes:
{:requests  [{:url :cmip-phases-url :on-success :current-view/update-cmip-phases}]  ; appended to :initialise-layers
 :wait-for  [:current-view/update-cmip-phases]                                       ; appended to the "all loaded" rule
 :url-paths {:cmip-phases "nhat/cmipphases/"}}                                       ; merged into :construct-urls

;; src/cljs/imas_seamap/boot.cljs
(defn boot-flow [{:keys [wait-for loading-message]} mode arg]
  {:first-dispatch [:ui/show-loading loading-message]
   :rules
   (concat
    [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}]
    (case mode
      :fresh      [{:when :seen? :events :construct-urls :dispatch [:initialise-layers]}]
      :hash-state [{:when :seen? :events :construct-urls :dispatch [:load-hash-state arg]}
                   {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}]
      :save-state [{:when :seen? :events :construct-urls :dispatch [:get-save-state arg [:load-hash-state]]}
                   {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}])
    base-rules                                                 ; grouped base layers, keyed/rich layer joins
    [{:when :seen-all-of? :events (into base-wait-for wait-for)
      :dispatch-n [[:map/initialise-display] [:transect/maybe-query]]}
     {:when :seen? :events :ui/hide-loading
      :dispatch-n [[:welcome-layer/open] [:display.outage-message/open true]] :halt? true}
     {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}])})
```

`:initialise-layers` becomes the base requests plus `(mapcat :requests contributions)`; `:construct-urls` iterates over the merged `:url-paths` map instead of destructuring a fixed list; the loading message comes from `:branding :labels :loading`. Four deployment `events.cljs` files lose their boot copies.

`:url-paths` must allow overriding base keys: NHAT points `:layer` at `nhat/layers/` and `:layer-legend` at `nhat/layerlegend/`.

### 7. Persisted State: URL Hash, Autosave, and Re-boot

**New.** Three path lists define what survives a reload, and all three vary by deployment:

| List | Core location | Used by | NHAT difference |
|------|---------------|---------|-----------------|
| `encode-state` paths | `utils.cljs` | `:create-save-state`, `:maybe-autosave` | adds `:current-view` selections, side-by-side state, current time, and the `:map-2` independent-map copies; drops SOK |
| `filter-state` allow-list | `utils.cljs` (`parse-state`) | `:load-hash-state`, `:merge-state` | same additions. This list is a security boundary against injected state, so it must stay an explicit allow-list |
| `ajax-loaded-info` paths | `utils.cljs` | `:re-boot` (state kept across a reset) | adds the four `:current-view` lists |

NHAT duplicates all three in `natural_hazards_atlas/utils.cljs` and overrides `merge-state` only to call its own `parse-state`. TMA has the same duplication for data-in-region.

Proposal: each feature module contributes paths and core concatenates them.

```clojure
;; feature contribution
{:persisted-state
 {:hash   [[:current-view :selected-cmip-phase-id] [:current-view :selected-model-id] ...
           [:display :side-by-side :active?] [:display :side-by-side :split-ratio]]
  :loaded [[:current-view :cmip-phases] [:current-view :models] ...]}}

;; core
(defn persisted-paths [kind]
  (into base-paths (mapcat #(get-in % [:persisted-state kind]) active-contributions)))

(defn encode-state   [db]    (... (select-keys* db (persisted-paths :hash)) ...))
(defn filter-state   [state] (select-keys* state (persisted-paths :hash)))
(defn ajax-loaded-info [db]  (select-keys* db (persisted-paths :loaded)))
```

Independent-map state (`utils/independent-map-state-path :map-2 ...`) is just more paths, contributed by `:feature/side-by-side-maps`. With this in place `:create-save-state`, `:maybe-autosave`, `:merge-state`, `:load-hash-state`, and `:re-boot` no longer need deployment overrides.

A per-deployment **hash round-trip test** (encode, parse, compare) is the first automated test worth writing; see *Testing Strategy*.

### 8. Unified Entry Point

**Updated.** Single `core.cljs` that reads the deployment id from the page. The branch already has the deployment-id arity on `imas-seamap.core/init`; what remains is feature-driven handler registration, the body class, and requiring every deployment namespace so their registry entries exist before first render.

```clojure
;; src/cljs/imas_seamap/core.cljs

(ns imas-seamap.core
  (:require [imas-seamap.config.deployments :as deployments]
            [imas-seamap.handlers.registry :as registry]
            [imas-seamap.views.layout :as layout]
            ;; Deployment namespaces register their components and feature handlers on load.
            ;; With per-deployment build modules (Build Configuration, option C) these move
            ;; into the module entries instead.
            [imas-seamap.natural-hazards-atlas.views]
            [imas-seamap.state-of-knowledge.registry]
            ...))

(defn mount-root [css-class]
  (re-frame/clear-subscription-cache!)
  (Blueprint/FocusStyleManager.onlyShowFocusOnTabs)
  (apply js/document.body.classList.add (string/split css-class #" "))   ; SCSS selects on body classes
  (.render @root (r/as-element [hotkeys-provider {...} [:f> layout/layout-app]])))

(defn ^:export init
  [api-url-base media-url-base wordpress-url-base img-url-base deployment-id]
  (let [config (deployments/get-deployment (keyword deployment-id))]   ; throws on unknown id
    (registry/register-for-features! (:features config))
    (re-frame/dispatch-sync
     [:boot {:config config
             :url-base {:api-url-base api-url-base :media-url-base media-url-base
                        :wordpress-url-base wordpress-url-base :img-url-base img-url-base}}])
    (dev-setup)
    (mount-root (:css-class config))))
```

Valid deployment ids: `seamap-australia`, `tas-marine-atlas`, `seamap-antarctica`, `futures-of-seafood`, `natural-hazards-atlas`, `tmag-kiosk`.

`dev/deployment_switcher.cljs` dispatches `:boot` with the current positional arguments plus a config; it must follow whatever signature `:boot` ends up with.

---

## Migration Path

**Updated 2026-10-15.** Phases 1 to 3 are complete, Phase 4 is in progress, and `develop` is merged. Phase 4b is new.

### Phase 1: Extract Configuration — done

`config/deployments.cljs` and `config/content.cljs` exist for the original four deployments. NHAT and the kiosk still need configs (section 1).

### Phase 2: Add Feature Subscriptions — done

`:feature/enabled?`, the `feature?` helper, and the `:branding/*` subscriptions are in `subs/features.cljs` and `subs/branding.cljs`. The config is stored in app-db by `:boot` when a deployment config is passed; deployments that still boot without one see no features and no branding, which is the current state of all but Seamap Australia.

### Phase 3: Migrate Leaf Components — done

`tma?` is gone. Settings, region select, data-in-region control, the SOK pills, and the layer preview are feature-gated. The boundary-filter subscription in `catalogue-layers` is gated too, which lets the no-op `:sok/boundary-layer-filter` hack go.

Still hard-coded despite having a subscription: the welcome dialogue (title, body, citations), the region and transect control tooltips, the `:colors` overrides (no `color-style-element` exists), and `:links`. These are Phase 4 work since they sit in the per-deployment layouts.

### Phase 4: Unify Layouts (Medium Risk) — in progress

1. Finish the Seamap Australia layout: left drawer tabs, controls, right drawer, floating pills, and map overlays all from `:layout` (today only `:footer` is).
2. Add the kiosk config and migrate the kiosk next. It is the smallest deployment and exercises the two drawer modes the Australia layout does not: `:tabs nil` and `:collapsible? false`. Decide whether `tmag-kiosk.html` stays separate or becomes an Ansible variant of `index.html`.
3. Migrate Futures of Seafood, then TMA, then Antarctica. Antarctica needs `:min-zoom`/`:max-zoom` from config; TMA needs the data-in-region control slot. Reconcile each config with the code before migrating (TMA renders settings and a "Measure" transect control today; its config says it has neither).
4. Move the side-by-side-views floating pill, currently copied into three `views.cljs` files, into the shared pill slot.
5. **NHAT: consume config only.** Give NHAT a config so branding, features, and labels work, and register its bespoke components in the registry, but keep its own `layout-app` and `map-component` until Phase 4b is done. Moving NHAT onto the unified layout before the handler and boot extension points exist would mean re-creating its overrides inside core.

### Phase 4b: Handler, Boot, and Persisted-State Extension Points (Medium Risk) — new

1. Handler registry with ordered override (section 5). Split the six `config-handlers` maps into base plus feature modules; diff the generated maps against the originals to prove nothing changes.
2. Boot flow from contributions (section 6). Validate by diffing the generated async-flow rules against the hand-written ones for each deployment.
3. Persisted-state contributions (section 7), with the hash round-trip test in place first.
4. Displayed-layer resolver registry; remove NHAT's three resolution overrides.
5. Move NHAT onto the unified layout with `:map :map/side-by-side`.

### Phase 5: Consolidate Entry Points (Higher Risk)

1. Unify all `core.cljs` files into the single entry point (section 8). All six deployments boot through `events/boot` with a deployment config.
2. Update `index.html` / Ansible, and `tmag-kiosk.html`, to pass the deployment id.
3. Remove deployment-specific `core.cljs`, `views.cljs`, `db.cljs`, `events.cljs`, and `utils.cljs` files as they empty out.

### Phase 6 (optional): Side-by-side maps in the shared map component

Fold NHAT's two-map component into the shared one behind `:feature/side-by-side-maps`, and read CRS, zoom limits, and max bounds from `:map/props`. Removes the last two deployment-specific map files.

---

## Benefits

1. **No more variant parents** - Components check features directly via subscriptions
2. **Declarative** - Adding a feature to a deployment = adding a keyword to a set
3. **Single source of truth** - All deployment differences visible in one file
4. **Testable** - Can test any component with any feature combination
5. **Extensible** - New deployments = new config map, minimal code changes
6. **Reduced duplication** - One `layout-app`, not four

## Trade-offs

1. **Runtime overhead** - Feature checks on every render (mitigated by re-frame's subscription caching)
2. **Migration effort** - Gradual refactor required across many files
3. **Indirection** - Component resolution via registry is less explicit than direct requires
4. **Learning curve** - Team needs to understand new patterns

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing deployments | Migrate one deployment at a time, keep old code working |
| Performance regression | Profile before/after, re-frame subscriptions are cached |
| Lost type safety | Use spec to validate configuration maps |
| Merge conflicts during migration | Coordinate with team, avoid parallel work on same files |
| NHAT regressions from shared changes | NHAT has no config yet and shares `views.cljs` components; keep its layout separate until Phase 4b, and add its hash round-trip test before touching persisted state |
| Registry entries missing at first render | `resolve-component` logs a clear error; single entry point requires all deployment namespaces, or use per-deployment build modules |
| Override order surprises | Fixed `feature-order`, warnings on override, and a per-deployment diff of generated handler maps against today's `config-handlers` |

---

## Files to Create

**Updated.** Files marked ✓ exist on the branch.

```
src/cljs/imas_seamap/
├── config/
│   ├── deployments.cljs      ✓ All deployment configurations (add NHAT, kiosk)
│   ├── content.cljs          ✓ Welcome / citation content
│   └── components.cljs       ✓ Component registry (atom; currently untracked, must be committed)
├── subs/
│   ├── features.cljs         ✓ Feature flag subscriptions
│   └── branding.cljs         ✓ Branding, map config, layout subscriptions
├── handlers/
│   └── registry.cljs           Feature-based handler registration with ordered override (section 5)
├── boot.cljs                   Boot flow built from feature contributions (section 6)
├── persisted_state.cljs        encode-state / filter-state / ajax-loaded-info from contributions (section 7)
├── state_of_knowledge/registry.cljs
├── story_maps/registry.cljs
├── tas_marine_atlas/data_in_region/registry.cljs
├── natural_hazards_atlas/registry.cljs   :feature/hazard-layers handlers, boot and persisted-state contributions
└── views/
    └── layout.cljs             Unified slot-based layout (section 3)

test/cljs/imas_seamap/
├── config_test.cljs          ✓ (no shadow-cljs test build target yet; see Testing Strategy)
├── handlers_registry_test.cljs
└── persisted_state_test.cljs   hash round-trip per deployment
```

## Files to Eventually Remove

After full migration:
- `tas_marine_atlas/core.cljs`, `tas_marine_atlas/views.cljs`, `tas_marine_atlas/db.cljs`, most of `tas_marine_atlas/events.cljs`
- `seamap_antarctica/core.cljs`, `seamap_antarctica/views.cljs`, `seamap_antarctica/db.cljs`, `seamap_antarctica/events.cljs`, `seamap_antarctica/map/views.cljs` (after Phase 6)
- `futuresofseafood/core.cljs`, `futuresofseafood/views.cljs`, `futuresofseafood/events.cljs`, `futuresofseafood/map/views.cljs`
- `tmag_kiosk/core.cljs`, `tmag_kiosk/views.cljs`, `tmag_kiosk/events.cljs`, `tmag_kiosk/map/views.cljs`
- `natural_hazards_atlas/core.cljs`, `natural_hazards_atlas/db.cljs`, the boot and persisted-state copies in `natural_hazards_atlas/events.cljs` and `natural_hazards_atlas/utils.cljs`, and (after Phase 6) `natural_hazards_atlas/map/views.cljs`. The current-view views, subs, and behavioural events stay as the `:feature/hazard-layers` module.
- `dev/deployment_switcher.cljs` once a real deployment switch is a page reload with a different id

---

---

## Branding Configuration

### Current Branding Implementation

Branding is currently spread across multiple locations:

| Element | Location | How It Varies |
|---------|----------|---------------|
| Color palette | `src/index.scss` (lines 79-153) | SCSS variables per deployment |
| CSS class | Each `views.cljs` layout-app | `.seamap`, `.tas-marine-atlas`, `.futures-of-seafood` |
| Drawer class | Each `views.cljs` left-drawer | `.seamap-drawer`, `.tas-marine-atlas-drawer`, `.fos-drawer` |
| Logo image | Each `views.cljs` left-drawer | Different image paths |
| Logo link | Each `views.cljs` left-drawer | Different URLs |
| Welcome text | Each `views.cljs` welcome-dialogue | Completely different content |
| Control labels | Scattered in components | e.g., "Find Data in Region" vs "Select region" |

### Proposed Branding Configuration Schema

Extend the deployment config to include comprehensive branding:

```clojure
(def seamap-australia
  {:id          :seamap-australia
   :css-class   "seamap"
   :drawer-class "seamap-drawer"

   :branding
   {:logo        {:src    "img/Seamap2_V2_RGB.png"
                  :alt    "Seamap Australia"
                  :height 96}
    :url         "https://seamapaustralia.org"
    :title       "Seamap Australia"

    ;; Welcome dialogue content (hiccup or content key)
    :welcome     {:title   "Welcome to Seamap Australia."
                  :content :welcome/seamap-australia}

    ;; Configurable labels for controls/UI elements
    :labels      {:region-control    "Select region"
                  :transect-control  "Draw transect"
                  :layer-info        "Layer info / Download data"}

    ;; External links
    :links       {:main-site   "https://seamapaustralia.org"
                  :help        "https://seamapaustralia.org/help"
                  :contact     "https://seamapaustralia.org/contact"}}

   ;; Color overrides (applied as CSS custom properties)
   ;; Only specify overrides - base theme provides defaults
   :colors      {}  ;; Uses default seamap colors

   ;; ... rest of config
   })

(def tas-marine-atlas
  {:id          :tas-marine-atlas
   :css-class   "tas-marine-atlas"
   :drawer-class "tas-marine-atlas-drawer"

   :branding
   {:logo        {:src    "img/TMA_Banner_size_website.png"
                  :alt    "Tasmania Marine Atlas"
                  :height 86}
    :url         "https://tasmarineatlas.org"
    :title       "Tasmania's Marine Atlas"

    :welcome     {:title   "Welcome to Tasmania's Marine Atlas."
                  :content :welcome/tas-marine-atlas}  ;; References content registry

    :labels      {:region-control   "Find Data in Region"
                  :transect-control "Measure"
                  :layer-info       "Layer info"}

    :links       {:main-site    "https://tasmarineatlas.org"
                  :sea-country  "https://storymaps.arcgis.com/stories/22672ae2d60f424f8d1e024232cfa885"}}

   :colors      {:color-1 "rgb(18, 37, 55)"
                 :color-2 "rgb(11, 80, 113)"
                 :color-8 "rgb(98, 191, 236)"}

   ;; ...
   })

(def futures-of-seafood
  {:id          :futures-of-seafood
   :css-class   "futures-of-seafood"
   :drawer-class "fos-drawer"

   :branding
   {:logo        {:src    "img/Futures-of-Seafood-Logo-Reverse-400x218.png"
                  :alt    "Futures of Seafood"
                  :height 86}
    :url         "https://futuresofseafood.com.au"
    :title       "Futures of Seafood"

    :welcome     {:title   "Welcome to Futures of Seafood."
                  :content :welcome/default}

    :labels      {:region-control   "Select region"
                  :transect-control "Draw transect"
                  :layer-info       "Layer info"}}

   :colors      {:color-2          "#062440"
                 :color-highlight-1 "#00b0a5"
                 :color-7          "white"}

   ;; ...
   })
```

### Runtime Values and Dialogue Options

**Updated.** Three NHAT and kiosk requirements the schema above does not cover:

- **Runtime URLs.** NHAT's logo links to the WordPress base URL passed to `init`, not a literal, and its drawer footer and welcome text link into that site. Allow a branding value to be a keyword naming a runtime URL base (`:url-base/wordpress`) or a vector `[:url-base/wordpress "resources/how-to-use-the-atlas/"]`, resolved by a `:branding/resolve-url` subscription that reads `[:config :url-base]`. Welcome content that needs a runtime URL should be a function of the url-base map rather than static hiccup.
- **Welcome dialogue options.** `:welcome` gains `:dont-show-again?` (NHAT shows a checkbox; the others always set the cookie on close, matching the `dont-show-again?` argument `:welcome-layer/close` now takes), `:button-text` ("Get Started" versus "Get Started!"), and may be `nil` to disable the dialogue entirely, as the kiosk needs.
- **Labels.** Add `:loading` (the boot-screen message, "Loading Natural Hazards Atlas...") and the per-control tooltips NHAT changes (`:region-control "Select Area of Interest"`). Helper-overlay text should move next to each control's registration rather than live in a per-deployment list in `layout-app`.

### Content Registry for Welcome Dialogues

Store rich content separately and reference by key:

```clojure
;; src/cljs/imas_seamap/config/content.cljs

(def welcome-content
  {:welcome/seamap-australia
   [:div.overview
    [:p "Seamap Australia is a nationally synthesised product of
         seafloor habitat data collected from various stakeholders
         around Australia..."]
    [:p.citation
     "Lucieer V, Walsh P, Flukes E, Butler C, Proctor R, Johnson C (2017). "
     [:em "Seamap Australia - a national seafloor habitat classification scheme"]
     ". Institute for Marine and Antarctic Studies (IMAS)..."]]

   :welcome/tas-marine-atlas
   [:div.overview
    [:p "The waters and coastlines represented in the Tasmania's Marine
         Atlas are of significance to Tasmanian Aboriginal people..."]
    [:p "Any absence of information about Tasmanian Aboriginal people's
         connection to Sea Country..."]
    [:p "Learn more about Sea Country and Tasmanian Aboriginal People "
     [:a {:href "https://storymaps.arcgis.com/stories/22672ae2d60f424f8d1e024232cfa885"
          :target "_blank"} "here"] "."]]

   :welcome/default
   [:div.overview
    [:p "Welcome to this marine data portal."]]})

(defn get-content [content-key]
  (get welcome-content content-key))
```

### Applying Colors via CSS Custom Properties

Instead of hardcoding colors in SCSS, apply them dynamically:

```clojure
;; src/cljs/imas_seamap/views/branding.cljs

(defn color-style-element
  "Generates a <style> element with CSS custom properties for the deployment"
  []
  (let [colors @(re-frame/subscribe [:branding/colors])]
    (when (seq colors)
      [:style
       (str ":root {\n"
            (str/join "\n"
              (for [[k v] colors]
                (str "  --" (name k) ": " v ";")))
            "\n}")])))

;; In layout-app:
(defn layout-app []
  (let [config @(re-frame/subscribe [:deployment/config])]
    [:div {:class (:css-class config)}
     [color-style-element]  ;; Injects color overrides
     ;; ... rest of layout
     ]))
```

Alternatively, keep the SCSS variables but generate deployment-specific CSS at build time.

### Branding Subscriptions

```clojure
;; src/cljs/imas_seamap/subs/branding.cljs

(re-frame/reg-sub
 :branding/config
 :<- [:deployment/config]
 (fn [config _]
   (:branding config)))

(re-frame/reg-sub
 :branding/logo
 :<- [:branding/config]
 (fn [branding _]
   (:logo branding)))

(re-frame/reg-sub
 :branding/label
 :<- [:branding/config]
 (fn [branding [_ label-key]]
   (get-in branding [:labels label-key])))

(re-frame/reg-sub
 :branding/colors
 :<- [:deployment/config]
 (fn [config _]
   (:colors config)))

(re-frame/reg-sub
 :branding/welcome
 :<- [:branding/config]
 (fn [branding _]
   (:welcome branding)))
```

### Using Branding in Components

```clojure
;; Left drawer header with configurable logo
(defn branding-header []
  (let [{:keys [src alt height]} @(re-frame/subscribe [:branding/logo])
        url @(re-frame/subscribe [:branding/url])]
    [:div.branding-header
     [:a {:href url :target "_blank"}
      [:img {:src src :alt alt :style {:height height}}]]]))

;; Control with configurable label
(defn region-control []
  (let [label @(re-frame/subscribe [:branding/label :region-control])]
    [control-button
     {:icon     :select-region
      :tooltip  label
      :on-click #(dispatch [:region/start-selection])}]))

;; Welcome dialogue with configurable content
(defn welcome-dialogue []
  (let [open?   @(re-frame/subscribe [:welcome-layer/open?])
        {:keys [title content]} @(re-frame/subscribe [:branding/welcome])
        body    (if (keyword? content)
                  (content/get-content content)
                  content)]
    [b/dialogue
     {:title    title
      :is-open  open?
      :on-close #(dispatch [:welcome-layer/close])}
     [:div.bp3-dialog-body body]]))
```

### Summary: Branding Handled by Configuration

| Branding Element | Configuration Location | How It's Applied |
|------------------|----------------------|------------------|
| Logo image | `:branding :logo :src` | Subscription → `<img>` component |
| Logo link | `:branding :url` | Subscription → `<a href>` |
| Logo height | `:branding :logo :height` | Subscription → inline style |
| Welcome title | `:branding :welcome :title` | Subscription → dialogue title |
| Welcome content | `:branding :welcome :content` | Content registry lookup |
| Control labels | `:branding :labels :*` | Subscription per control |
| External links | `:branding :links :*` | Subscription per link |
| Color overrides | `:colors` | CSS custom properties (dynamic or build-time) |
| CSS class | `:css-class` | Applied to root element |
| Drawer class | `:drawer-class` | Applied to left-drawer |

This approach means:
- **No hardcoded strings** in component code
- **All branding visible** in deployment config
- **New deployment** = new config map with branding section
- **Content changes** don't require code changes (just config/content updates)

---

## Next Steps

1. Commit `config/components.cljs`, the Antarctica logo image, and `tmag-kiosk.html`, all untracked on the branch. (The FoS logo the config references is already tracked.) Fix or delete `config/examples.cljs`: its `validate-deployments` uses `set/difference` without requiring `clojure.set`, and nothing requires the namespace so the build never compiles it.
2. Add NHAT and kiosk configs to `deployments.cljs` (section 1) and reconcile the TMA config with its code.
3. Add a shadow-cljs test build and the hash round-trip test (Testing Strategy) before Phase 4b touches persisted state.
4. Finish Phase 4 for Seamap Australia, then the kiosk.
5. Start Phase 4b with the handler registry diff, which is additive and verifiable.

---

## Database Schema Initialization

### Current State

**Updated.** Four deployments have their own `db.cljs`; FoS and the kiosk use the base. The base itself gained pinned-legend, split-layer, side-by-side, and time fields on `develop`, so the `base-db` sketch below is behind the real file and should be regenerated from it.

| Schema Difference | Australia | TMA | Antarctica | FoS | NHAT | Kiosk |
|-------------------|-----------|-----|------------|-----|------|-------|
| Map start | centre `[-27.82, 132.13]`, zoom 4 | centre `[-42.20, 146.74]`, zoom 7 | centre `[-80, -90]`, zoom 2 | base | `:center nil`, `:bounds` Tasmania, zoom 7 | base |
| `:state-of-knowledge` | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ |
| `:data-in-region` | ✗ | ✓ | ✗ | ✗ | ✓ (copied, unused) | ✗ |
| `:current-view` | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| `:display` time fields (`:current-time`, `:available-times`, playing/loading) | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| `:independent-map-state :map-2` (created at runtime) | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| `:display :welcome-overlay` | `false` | `true` | `false` | base | `false` | base |
| `:display :catalogue :region` | ✗ | ✓ | ✗ | ✗ | ✓ | ✗ |
| Default catalogue tab | `"cat"` | `"cat"` | `"cat"` | `"cat"` | `"hazards"` | `"cat"` |
| URL path differences | SOK, region reports | data-in-region | SOK | none | overrides `:layer`, `:layer-legend`; adds four `nhat/*` | none |

### Proposed Solution

#### Add `:initial-state` to Deployment Config

Extend the deployment configuration to include state initialization overrides:

```clojure
;; src/cljs/imas_seamap/config/deployments.cljs

(def seamap-australia
  {:id          :seamap-australia
   :css-class   "seamap"

   ;; ... (branding, features, etc. as before)

   ;; Map initialization
   :map         {:crs    :epsg-3857
                 :center [-27.819644755 132.133333]
                 :zoom   4}

   ;; Feature-specific state initialization
   :initial-state
   {:display {:welcome-overlay false}  ;; Override default
    :state-of-knowledge
    {:boundaries {:active-boundary nil
                  :active-boundary-layer nil
                  :amp   {:active-network nil
                          :active-park    nil
                          :active-zone    nil
                          :active-zone-iucn nil
                          :active-zone-id nil}
                  :imcra {:active-provincial-bioregion nil
                          :active-mesoscale-bioregion  nil}
                  :meow  {:active-realm     nil
                          :active-province  nil
                          :active-ecoregion nil}}
     :statistics {:habitat {:results [] :loading? false :show-layers? false}
                  :bathymetry {:results [] :loading? false :show-layers? false}
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

(def tas-marine-atlas
  {:id          :tas-marine-atlas
   :css-class   "tas-marine-atlas"

   ;; ... (branding, features, etc.)

   :map         {:crs    :epsg-3857
                 :center [-42.20157676555315 146.74253188097842]
                 :zoom   7}

   :initial-state
   {:display {:welcome-overlay true   ;; TMA shows welcome by default
              :catalogue {:main   {:tab "cat" :expanded #{}}
                          :region {:tab "cat" :expanded #{}}}}  ;; TMA has region catalogue
    :data-in-region {:data     nil
                     :query-id nil}}

   :url-paths {:data-in-region "habitat/datainregion"}})

(def seamap-antarctica
  {:id          :seamap-antarctica
   :css-class   "seamap"

   ;; ... (branding, features, etc.)

   :map         {:crs    :epsg-3031  ;; Antarctic polar projection
                 :center [-80 -90]
                 :zoom   2}

   :initial-state
   {:display {:welcome-overlay false}
    :state-of-knowledge  ;; Same as Australia
    {:boundaries {:active-boundary nil
                  ;; ... (same structure)
                  }
     :statistics {;; ... (same structure)
                  }}}

   :url-paths  ;; Same SOK paths as Australia
   {:amp-boundaries        "habitat/ampboundaries"
    ;; ... (same as Australia)
    }})
```

#### Unified `db.cljs` with Base Schema

Create a single `db.cljs` that provides the base schema, which gets merged with deployment-specific state:

```clojure
;; src/cljs/imas_seamap/db.cljs

(defn base-db
  "Returns the base database schema with common defaults.
   Deployment-specific overrides are merged in during boot."
  []
  {:initialised     false
   :map             {:center          [0 0]     ;; Will be overridden by deployment config
                     :initial-bounds? true
                     :size            {}
                     :zoom            1          ;; Will be overridden by deployment config
                     :zoom-cutover    10
                     :bounds          {}
                     :categories      []
                     :layers          []
                     :base-layers     []
                     :base-layer-groups []
                     :grouped-base-layers []
                     :active-base-layer nil
                     :organisations   []
                     :active-layers   []
                     :hidden-layers   #{}
                     :preview-layer   nil
                     :viewport-only?  false
                     :keyed-layers    {}
                     :rich-layers     {:rich-layers  []
                                       :states       {}
                                       :async-datas  {}
                                       :layer-lookup {}}
                     :legends         {}
                     :controls        {:transect false
                                       :download nil}}
   :site-configuration nil
   :story-maps      {:featured-maps []
                     :featured-map  nil}
   :layer-state     {:loading-state {}
                     :tile-count    {}
                     :error-count   {}
                     :legend-shown  #{}
                     :opacity       {}}
   :filters         {:layers       ""
                     :other-layers ""}
   :transect        {:query      nil
                     :show?      false
                     :habitat    nil
                     :bathymetry nil}
   :region-stats    {:habitat-layer nil}
   :habitat-colours {}
   :habitat-titles  {}
   :display         {:mouse-pos             {}
                     :help-overlay          false
                     :welcome-overlay       false  ;; Default: closed
                     :settings-overlay      false
                     :left-drawer           true
                     :left-drawer-tab       "catalogue"
                     :layers-search-omnibar false
                     :catalogue             {:main {:tab      "cat"
                                                    :expanded #{}}}
                     :sidebar               {:collapsed false
                                             :selected  "tab-activelayers"}
                     :right-sidebars        []
                     :open-pill             nil
                     :outage-message-open?  false}
   :dynamic-pills   {:dynamic-pills []
                     :states        {}
                     :async-datas   {}}
   :autosave?       false
   :config          {:url-paths  ;; Base URL paths - deployment-specific paths are merged
                     {:site-configuration "siteconfiguration/"
                      :layer              "layers/"
                      :base-layer         "baselayers/"
                      :base-layer-group   "baselayergroups/"
                      :organisation       "organisations/"
                      :classification     "classifications/"
                      :region-stats       "habitat/regions/"
                      :descriptor         "descriptors/"
                      :save-state         "savestates"
                      :category           "categories/"
                      :keyed-layers       "keyedlayers/"
                      :rich-layers        "richlayers/"
                      :dynamic-pills      "dynamicpills/"
                      :layer-legend       "layerlegend/"
                      :cql-filter-values  "habitat/cqlfiltervalues"
                      :dynamic-pill-region-control-values "habitat/dynamicpillregioncontrolvalues"
                      :layer-previews     "layer_previews/"
                      :story-maps         "wp-json/wp/v2/story_map?acf_format=standard"}
                     :urls      nil
                     :url-base  {:api-url-base       "http://localhost:8000/api/"
                                 :media-url-base     "http://localhost:8000/media/"
                                 :wordpress-url-base "http://localhost:8888/"
                                 :img-url-base       "/img/"}}})

(defn merge-deployment-state
  "Deep merges deployment-specific state overrides into base schema"
  [base-db deployment-config]
  (let [{:keys [initial-state url-paths map]} deployment-config]
    (-> base-db
        ;; Merge map config (center may be nil when the deployment starts from bounds)
        (update :map merge (select-keys map [:crs :center :zoom :bounds :min-zoom :max-zoom :max-bounds]))
        ;; Deep-merge deployment-specific state. The one-level merge-with originally
        ;; proposed here would replace the whole [:display :catalogue] map; use the
        ;; existing imas-seamap.utils/merge-in instead.
        (merge-in initial-state)
        ;; Merge deployment-specific URL paths
        (update-in [:config :url-paths]
                   merge url-paths))))

(defn default-db
  "Returns the complete database schema for a deployment.
   Must be called with deployment config after handlers are registered."
  [deployment-config]
  (merge-deployment-state (base-db) deployment-config))
```

#### Update `:boot` Event Handler

Modify the boot event to use the deployment config for initialization:

```clojure
;; src/cljs/imas_seamap/events.cljs

(re-frame/reg-event-fx
 :boot
 [(re-frame/inject-cofx :save-code)
  (re-frame/inject-cofx :hash-code)
  (re-frame/inject-cofx :local-storage/get [:seamap-app-state])]
 (fn [{:keys [db]} [_ {:keys [config api-url-base media-url-base
                               wordpress-url-base img-url-base]}]]
   (let [initial-db (db/default-db config)]  ;; Use config to generate initial state
     {:db (-> initial-db
              (assoc :config config)  ;; Store deployment config
              (assoc-in [:config :url-base :api-url-base] api-url-base)
              (assoc-in [:config :url-base :media-url-base] media-url-base)
              (assoc-in [:config :url-base :wordpress-url-base] wordpress-url-base)
              (assoc-in [:config :url-base :img-url-base] img-url-base))
      :dispatch-n [[:construct-urls]
                   [:ui/show-loading]
                   ;; ... rest of boot sequence
                   ]})))
```

### Benefits

1. **Single source of truth** - Base schema in one place, overrides declarative
2. **Eliminates duplicate db.cljs files** - All deployments share base schema
3. **Feature-driven state** - Only features that are enabled get state sections
4. **Testable** - Can test state initialization for any deployment config

### Migration Notes

- Keep existing deployment `db.cljs` files initially for reference
- Validate merged state matches original state for each deployment
- Use clojure.spec to validate state shape after merge

- **Updated.** NHAT's `:current-view` and the kiosk's always-open drawer are `:initial-state` entries; the independent `:map-2` state is created at runtime and needs no initial value
- Regenerate `base-db` from the current `db.cljs` rather than from the sketch above

---

## Build Configuration Changes

### Current State

**Updated.** The `shadow-cljs.edn` currently compiles all **6** entry points into a single bundle:

```clojure
:modules {:app {:entries [imas-seamap.core
                          imas-seamap.futuresofseafood.core
                          imas-seamap.seamap-antarctica.core
                          imas-seamap.tas-marine-atlas.core
                          imas-seamap.natural-hazards-atlas.core
                          imas-seamap.tmag-kiosk.core]}}
```

Each deployment's `index.html` selects which init function to call via Ansible templating; the kiosk has a separate `tmag-kiosk.html`. There is no test build target.

### Proposed Changes

#### Option A: Keep Single Bundle (Recommended for Phase 1-4)

During migration phases 1-4, keep the existing build configuration to minimize risk:

```clojure
;; shadow-cljs.edn - NO CHANGES during migration

:modules {:app {:entries [imas-seamap.core
                          imas-seamap.futuresofseafood.core
                          imas-seamap.seamap-antarctica.core
                          imas-seamap.tas-marine-atlas.core
                          imas-seamap.natural-hazards-atlas.core
                          imas-seamap.tmag-kiosk.core]}}
```

**Rationale**: All code is already being shipped to all deployments anyway. Keeping the multi-entry setup allows gradual migration without affecting deployments that haven't migrated yet.

#### Option B: Unified Entry Point (Phase 5 - After Full Migration)

Once all deployments use the unified `core.cljs`, simplify to a single entry point:

```clojure
;; shadow-cljs.edn - AFTER Phase 5 migration complete

:builds {:dev {:target           :browser
               :output-dir       "resources/public/js/"
               :asset-path       "js"
               :closure-defines  {goog.DEBUG true
                                  "re_frame.trace.trace_enabled_QMARK_" true}
               :modules          {:app {:entries [imas-seamap.core]}}  ;; Single entry
               :compiler-options {:pretty-print true}
               :devtools
               {:preloads         [devtools.preload
                                   imas-seamap.specs.preload]}}

         :min {:target            :browser
               :closure-defines   {goog.DEBUG false}
               :output-dir        "resources/public/js/"
               :asset-path        "js"
               :module-hash-names true
               :build-options     {:manifest-name "manifest.json"}
               :modules           {:app {:entries [imas-seamap.core]}}}  ;; Single entry

         :mindev {:target            :browser
                  :output-dir        "resources/public/js/"
                  :asset-path        "js"
                  :closure-defines   {goog.DEBUG false}
                  :module-hash-names true
                  :build-options     {:manifest-name "manifest.json"}
                  :modules           {:app {:entries [imas-seamap.core]}}  ;; Single entry
                  :compiler-options  {:pseudo-names true
                                      :source-map   true}}}}
```

#### Option C: Per-Deployment Bundles (Advanced - Optional)

If you want deployment-specific bundles (smaller payload, deployment isolation), create separate builds:

```clojure
;; shadow-cljs.edn - Advanced configuration (optional)

:builds {:seamap-australia
         {:target            :browser
          :output-dir        "resources/public/js/seamap-australia"
          :asset-path        "js/seamap-australia"
          :closure-defines   {goog.DEBUG false
                              imas-seamap.config/DEPLOYMENT :seamap-australia}
          :module-hash-names true
          :modules           {:app {:entries [imas-seamap.core]}}}

         :tas-marine-atlas
         {:target            :browser
          :output-dir        "resources/public/js/tas-marine-atlas"
          :asset-path        "js/tas-marine-atlas"
          :closure-defines   {goog.DEBUG false
                              imas-seamap.config/DEPLOYMENT :tas-marine-atlas}
          :module-hash-names true
          :modules           {:app {:entries [imas-seamap.core]}}}

         ;; ... repeat for antarctica and fos
         }
```

This requires separate `index.html` files per deployment or build-time templating.

### Recommendation

Use **Option A** during migration, then switch to **Option B** after Phase 5 completion. Option C is worth a second look once the registry-at-load pattern is in use: with one shadow-cljs module per deployment (a shared `:base` module and `:depends-on` per deployment), Seamap Australia does not load NHAT's current-view code, and the "which namespaces must be required before first render" question from section 4 answers itself. Add a `:test` build (`:node-test` target) regardless of which option is chosen.

---

## Index.html Updates

### Current State

**Updated.** There are two pages: the Ansible-managed `index.html` below, and `resources/public/tmag-kiosk.html`, which calls `imas_seamap.tmag_kiosk.core.init` directly. Both need the deployment id once Phase 5 lands.

Single `index.html` with Ansible-managed block that swaps init calls:

```html
<script>
// BEGIN ANSIBLE MANAGED BLOCK
const
  api_url_base = "http://localhost:8000/api/",
  media_url_base = "http://localhost:8000/media/",
  wordpress_url_base = "http://localhost:8888/",
  img_url_base = "/img/";
 imas_seamap.futuresofseafood.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
     /* imas_seamap.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base); */
// END ANSIBLE MANAGED BLOCK
</script>
```

### Proposed Changes

#### Phase 1-4: Dual Support (Both Old and New)

Keep existing Ansible templating but extend it to support both patterns:

```html
<script>
// BEGIN ANSIBLE MANAGED BLOCK: Deployment Configuration
const
  deployment_id = "futures-of-seafood",  // <-- Ansible template variable: {{ deployment_id }}
  api_url_base = "http://localhost:8000/api/",      // <-- {{ api_url_base }}
  media_url_base = "http://localhost:8000/media/",  // <-- {{ media_url_base }}
  wordpress_url_base = "http://localhost:8888/",     // <-- {{ wordpress_url_base }}
  img_url_base = "/img/";                            // <-- {{ img_url_base }}

// Check if unified init is available (migrated deployments)
if (typeof imas_seamap.core.init_unified === 'function') {
  imas_seamap.core.init_unified(deployment_id, api_url_base, media_url_base, wordpress_url_base, img_url_base);
} else {
  // Fallback to deployment-specific init (old deployments)
  {% if deployment == "seamap-australia" %}
  imas_seamap.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% elif deployment == "tas-marine-atlas" %}
  imas_seamap.tas_marine_atlas.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% elif deployment == "seamap-antarctica" %}
  imas_seamap.seamap_antarctica.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% elif deployment == "futures-of-seafood" %}
  imas_seamap.futuresofseafood.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% elif deployment == "natural-hazards-atlas" %}
  imas_seamap.natural_hazards_atlas.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% elif deployment == "tmag-kiosk" %}
  imas_seamap.tmag_kiosk.core.init(api_url_base, media_url_base, wordpress_url_base, img_url_base);
  {% endif %}
}
// END ANSIBLE MANAGED BLOCK
</script>
```

This allows gradual migration - unmigrated deployments continue using their specific init, while migrated ones use the unified init.

#### Phase 5: Fully Unified (After Migration Complete)

Once all deployments are migrated, simplify to single init call:

```html
<!doctype html>
<html lang="en">
  <head>
    <title>{{ site_title }}</title>  <!-- Ansible template variable -->
    <meta charset='utf-8'>
    <link rel="stylesheet" href="css/blueprint.css" />
    <link rel="stylesheet" href="https://unpkg.com/@blueprintjs/select@3.19.1/lib/css/blueprint-select.css" />
    <link rel="stylesheet" href="https://unpkg.com/@blueprintjs/icons@3.27.0/lib/css/blueprint-icons.css" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet-draw@1.0.4/dist/leaflet.draw.css" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet-sidebar-v2@1.0.0/css/leaflet-sidebar.min.css" />
    <link rel="stylesheet" href="css/style.npm-packaged.css" />
  </head>
  <body>
    <div id="app"></div>
    <div id="toaster"></div>
    <script src="https://use.fontawesome.com/d25c01a5e8.js"></script>
    <script src="js/app.js"></script>
    <script>
// BEGIN ANSIBLE MANAGED BLOCK: Deployment Configuration
const deploymentConfig = {
  deployment_id: "{{ deployment }}",           // e.g., "seamap-australia"
  api_url_base: "{{ api_url_base }}",         // e.g., "https://seamapaustralia.org/api/"
  media_url_base: "{{ media_url_base }}",     // e.g., "https://seamapaustralia.org/media/"
  wordpress_url_base: "{{ wordpress_url_base }}", // e.g., "https://seamapaustralia.org/"
  img_url_base: "{{ img_url_base }}"          // e.g., "/img/"
};

imas_seamap.core.init(
  deploymentConfig.deployment_id,
  deploymentConfig.api_url_base,
  deploymentConfig.media_url_base,
  deploymentConfig.wordpress_url_base,
  deploymentConfig.img_url_base
);
// END ANSIBLE MANAGED BLOCK
    </script>
  </body>
</html>
```

### Ansible Template Variables

Update your Ansible playbook to define deployment-specific variables:

```yaml
# ansible/vars/seamap-australia.yml
deployment: seamap-australia
site_title: Seamap Australia
api_url_base: https://seamapaustralia.org/api/
media_url_base: https://seamapaustralia.org/media/
wordpress_url_base: https://seamapaustralia.org/
img_url_base: /img/

# ansible/vars/tas-marine-atlas.yml
deployment: tas-marine-atlas
site_title: Tasmania's Marine Atlas
api_url_base: https://tasmarineatlas.org/api/
media_url_base: https://tasmarineatlas.org/media/
wordpress_url_base: https://tasmarineatlas.org/
img_url_base: /img/

# ... (repeat for other deployments)
```

### Alternative: Multiple index.html Files (If No Ansible)

If not using Ansible, maintain separate HTML files:

```
resources/public/
├── index.html                          # Development/default
├── index-seamap-australia.html
├── index-tas-marine-atlas.html
├── index-seamap-antarctica.html
└── index-futures-of-seafood.html
```

Each with hardcoded deployment ID:

```html
<!-- index-seamap-australia.html -->
<script>
imas_seamap.core.init(
  "seamap-australia",
  "https://seamapaustralia.org/api/",
  "https://seamapaustralia.org/media/",
  "https://seamapaustralia.org/",
  "/img/"
);
</script>
```

---

## Testing Strategy

### Current State

**Updated.** `test/cljs/imas_seamap/config_test.cljs` exists on the branch, but `shadow-cljs.edn` has no test build, so it has never run in CI. A dev-only deployment switcher (`dev/deployment_switcher.cljs`) re-boots the app with a different config for manual testing. Everything else is manual, across six deployments.

### Proposed Testing Approach

#### 4.1 Unit Tests for Configuration System

Create tests for the core configuration and feature system:

```clojure
;; test/cljs/imas_seamap/config/deployments_test.cljs

(ns imas-seamap.config.deployments-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [imas-seamap.config.deployments :as deployments]))

(deftest all-deployments-have-required-keys
  (doseq [[id config] deployments/deployments]
    (testing (str "Deployment " id " has required config keys")
      (is (keyword? (:id config)))
      (is (string? (:css-class config)))
      (is (map? (:branding config)))
      (is (map? (:map config)))
      (is (set? (:features config)))
      (is (map? (:drawers config)))
      (is (vector? (:tabs config)))
      (is (vector? (:controls config))))))

(deftest feature-sets-are-valid
  (let [valid-features #{:state-of-knowledge :data-in-region :featured-maps
                         :floating-pills :plot-component :transect-control
                         :data-providers :data-download :boundaries-pill
                         :zones-pill}]
    (doseq [[id config] deployments/deployments]
      (testing (str "Deployment " id " only uses valid features")
        (is (every? valid-features (:features config)))))))

(deftest map-config-is-valid
  (doseq [[id config] deployments/deployments]
    (testing (str "Deployment " id " has valid map config")
      (let [{:keys [crs center zoom]} (:map config)]
        (is (keyword? crs))
        (is (vector? center))
        (is (= 2 (count center)))
        (is (number? (first center)))
        (is (number? (second center)))
        (is (number? zoom))
        (is (>= zoom 1))))))
```

#### 4.2 Database Initialization Tests

Validate that merged state matches expected structure:

```clojure
;; test/cljs/imas_seamap/db_test.cljs

(ns imas-seamap.db-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [imas-seamap.db :as db]
            [imas-seamap.config.deployments :as deployments]))

(deftest base-db-has-required-structure
  (let [base (db/base-db)]
    (is (false? (:initialised base)))
    (is (map? (:map base)))
    (is (map? (:display base)))
    (is (map? (:config base)))))

(deftest deployment-merge-preserves-base-structure
  (doseq [[id config] deployments/deployments]
    (testing (str "Deployment " id " merge preserves base structure")
      (let [merged (db/default-db config)]
        (is (map? (:map merged)))
        (is (map? (:display merged)))
        (is (= (:center (:map config))
               (get-in merged [:map :center])))
        (is (= (:zoom (:map config))
               (get-in merged [:map :zoom])))))))

(deftest feature-specific-state-only-for-enabled-features
  (let [sok-config (get deployments/deployments :seamap-australia)
        dir-config (get deployments/deployments :tas-marine-atlas)
        sok-db (db/default-db sok-config)
        dir-db (db/default-db dir-config)]

    (testing "SOK deployment has SOK state"
      (is (contains? sok-db :state-of-knowledge))
      (is (map? (:state-of-knowledge sok-db))))

    (testing "TMA deployment has data-in-region state"
      (is (contains? dir-db :data-in-region))
      (is (map? (:data-in-region dir-db))))))
```

#### 4.2b Persisted-State Round-Trip Tests

The highest-value automated test, because section 7 changes the URL-hash and autosave format for every deployment:

```clojure
;; test/cljs/imas_seamap/persisted_state_test.cljs

(deftest hash-round-trip-per-deployment
  (doseq [[id config] deployments/deployments]
    (testing (str "Deployment " id " encodes and parses its own state")
      (let [db       (-> (db/default-db config) (populate-sample-state config))
            encoded  (ps/encode-state db config)
            parsed   (ps/parse-state encoded config)]
        ;; every path the deployment persists survives, and nothing else gets in
        (is (= (select-keys* db (ps/persisted-paths config :hash))
               (select-keys* parsed (ps/persisted-paths config :hash))))
        (is (empty? (extra-paths parsed (ps/persisted-paths config :hash))))))))

(deftest existing-hashes-still-parse
  ;; Fixture hashes captured from production URLs for each deployment before the refactor
  (doseq [[id hash] fixtures/production-hashes]
    (is (map? (ps/parse-state hash (deployments/get-deployment id))))))
```

Capture a few real shareable-URL hashes per deployment (especially NHAT, whose hashes carry `:current-view` and `:map-2` state) before Phase 4b starts.

#### 4.3 Visual Regression Testing

Use a tool like Percy.io or BackstopJS for visual regression testing:

```json
// backstop.json
{
  "id": "seamap_visual_regression",
  "viewports": [
    {"label": "desktop", "width": 1920, "height": 1080}
  ],
  "scenarios": [
    {
      "label": "Seamap Australia - Homepage",
      "url": "http://localhost:3451?deployment=seamap-australia",
      "selectors": ["#app"],
      "delay": 2000
    },
    {
      "label": "TAS Marine Atlas - Homepage",
      "url": "http://localhost:3451?deployment=tas-marine-atlas",
      "selectors": ["#app"],
      "delay": 2000
    },
    {
      "label": "Seamap Antarctica - Homepage",
      "url": "http://localhost:3451?deployment=seamap-antarctica",
      "selectors": ["#app"],
      "delay": 2000
    },
    {
      "label": "Futures of Seafood - Homepage",
      "url": "http://localhost:3451?deployment=futures-of-seafood",
      "selectors": ["#app"],
      "delay": 2000
    },
    {
      "label": "Natural Hazards Atlas - Homepage",
      "url": "http://localhost:3451?deployment=natural-hazards-atlas",
      "selectors": ["#app"],
      "delay": 2000
    },
    {
      "label": "TMAG kiosk - Homepage",
      "url": "http://localhost:3451/tmag-kiosk.html",
      "selectors": ["#app"],
      "delay": 2000
    }
  ],
  "paths": {
    "bitmaps_reference": "test/visual/reference",
    "bitmaps_test": "test/visual/test",
    "html_report": "test/visual/report",
    "ci_report": "test/visual/ci_report"
  }
}
```

#### 4.4 Feature Flag Tests

Test that components correctly respond to feature flags:

```clojure
;; test/cljs/imas_seamap/subs/features_test.cljs

(ns imas-seamap.subs.features-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [re-frame.core :as re-frame]
            [imas-seamap.subs.features]))

(deftest feature-enabled-subscription
  (testing "Returns true when feature is enabled"
    (re-frame/reg-cofx
     :test-db
     (fn [cofx _]
       (assoc cofx :db {:config {:features #{:state-of-knowledge :featured-maps}}})))

    (let [result (re-frame/subscribe [:feature/enabled? :state-of-knowledge])]
      (is (true? @result)))

    (let [result (re-frame/subscribe [:feature/enabled? :data-in-region])]
      (is (false? @result)))))
```

#### 4.5 Migration Testing Checklist

Create a checklist for testing each deployment during migration:

```markdown
## Deployment Migration Testing Checklist

Test on: [ ] Local  [ ] Staging  [ ] Production

### Visual Tests
- [ ] Logo displays correctly
- [ ] Color scheme matches original
- [ ] Welcome dialogue shows correct content
- [ ] Left drawer tabs are correct
- [ ] Map controls appear in correct positions

### Functional Tests
- [ ] Map loads at correct center/zoom
- [ ] Can add/remove layers
- [ ] Can toggle layer visibility
- [ ] Feature-specific panels work (SOK/Data-in-Region/etc)
- [ ] Omnisearch functions correctly
- [ ] Transect tool works (if enabled)
- [ ] Region selection works
- [ ] Featured maps load (if enabled)
- [ ] Print function works
- [ ] Share function works
- [ ] Settings overlay opens
- [ ] Help overlay opens

### Data Tests
- [ ] Layers load from correct API endpoints
- [ ] Feature data loads correctly
- [ ] State persistence works (autosave/load)
- [ ] URL parameters work correctly
- [ ] A shareable URL created before the migration still loads
- [ ] NHAT: current view, time period, and side-by-side state survive a reload; only one hazard layer active at a time
- [ ] Kiosk: drawer cannot be closed, reset clears search-added layers, no welcome dialogue

### Browser Tests
Test on:
- [ ] Chrome
- [ ] Firefox
- [ ] Safari
- [ ] Edge

### Performance Tests
- [ ] Initial load time < 3s
- [ ] Layer toggle responsive
- [ ] No console errors
- [ ] No memory leaks over 5min session
```

#### 4.6 Continuous Integration Setup

Add to CI pipeline (GitHub Actions example):

```yaml
# .github/workflows/test.yml
name: Test All Deployments

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2

    - name: Setup Node
      uses: actions/setup-node@v2
      with:
        node-version: '16'

    - name: Install dependencies
      run: npm install

    - name: Run ClojureScript unit tests
      # requires a :test build with :target :node-test in shadow-cljs.edn (not yet present)
      run: npx shadow-cljs compile test && node out/test.js

    - name: Build all deployments
      run: npx shadow-cljs release min

    - name: Start dev server
      run: npx shadow-cljs start dev-http &

    - name: Wait for server
      run: npx wait-on http://localhost:3451

    - name: Run visual regression tests
      run: npm run test:visual

    - name: Upload visual diffs
      if: failure()
      uses: actions/upload-artifact@v2
      with:
        name: visual-diffs
        path: test/visual/
```

#### 4.7 Manual Testing Protocol

For each migration phase, follow this protocol:

1. **Pre-Migration Baseline**
   - Capture screenshots of all deployments
   - Document current behavior
   - Note any existing bugs

2. **During Migration**
   - Test migrated deployment in isolation
   - Compare with baseline screenshots
   - Verify feature flags work as expected

3. **Post-Migration Verification**
   - Full smoke test of all deployments
   - Cross-browser testing
   - Performance profiling

4. **Rollback Plan**
   - Keep old code branches
   - Document rollback procedure
   - Test rollback process before deployment

### Testing Timeline

| Phase | Testing Focus | Risk Level |
|-------|---------------|------------|
| Phase 1: Extract Config | Config structure validation | Low |
| Phase 2: Add Subscriptions | Subscription tests, no visual changes | Low |
| Phase 3: Migrate Components | Component tests, visual regression | Medium |
| Phase 4: Unify Layouts | Full integration tests, all deployments | Medium |
| Phase 4b: Extension Points | Handler-map diffs, boot-rule diffs, hash round-trip per deployment | Medium |
| Phase 5: Single Entry Point | Build validation, deployment tests | Higher |

### Test Automation Tools

Recommended tools:

- **Unit Tests**: shadow-cljs built-in test runner
- **Visual Regression**: BackstopJS or Percy.io
- **E2E Tests**: Playwright or Cypress
- **Performance**: Lighthouse CI
- **Accessibility**: axe-core
