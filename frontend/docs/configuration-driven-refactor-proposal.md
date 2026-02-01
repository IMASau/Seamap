# Configuration-Driven Architecture Refactor Proposal

## Current State Analysis

### Deployments

The codebase supports **4 deployments**, each with its own `core.cljs` entry point:

| Deployment | Entry Point | CSS Class |
|------------|-------------|-----------|
| Seamap Australia | `imas_seamap/core.cljs` | `seamap` |
| Tasmania Marine Atlas (TMA) | `imas_seamap/tas_marine_atlas/core.cljs` | `tas-marine-atlas` |
| Seamap Antarctica | `imas_seamap/seamap_antarctica/core.cljs` | `seamap` |
| Futures of Seafood | `imas_seamap/futuresofseafood/core.cljs` | `futures-of-seafood` |

### Current Patterns

1. **Handler Registry** - Each `core.cljs` defines a `config-handlers` map with `:subs` and `:events`
2. **Flag Propagation** - The `tma?` boolean propagates through 3+ levels of components
3. **Component Replacement** - Each deployment has its own `layout-app` in a custom `views.cljs`
4. **Schema Variants** - Separate `db.cljs` files define different state shapes

### Feature Variations by Deployment

| Feature | Australia | TMA | Antarctica | FoS |
|---------|-----------|-----|------------|-----|
| State-of-Knowledge | ✓ | ✗ | ✓ | ✗ |
| Data-in-Region | ✗ | ✓ | ✗ | ✗ |
| Featured Maps | ✓ | ✓ | ✗ | ✗ |
| Floating Pills | ✓ | ✗ | ✓ | ✗ |
| Plot Component | ✓ | ✗ | ✗ | ✓ |
| Custom CRS | ✗ | ✗ | ✓ (EPSG:3031) | ✗ |
| Data Providers | ✓ | ✗ | ✗ | ✗ |
| Transect Control | ✓ | ✗ | ✗ | ✓ |

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

---

## Proposed Configuration-Driven Architecture

### 1. Unified Deployment Configuration Schema

Create a single configuration file that declares all deployment differences:

```clojure
;; src/cljs/imas_seamap/config/deployments.cljs

(def seamap-australia
  {:id          :seamap-australia
   :css-class   "seamap"

   ;; Branding
   :branding    {:logo   "img/Seamap2_V2_RGB.png"
                 :title  "Seamap Australia"
                 :url    "https://seamapaustralia.org"}

   ;; Map configuration
   :map         {:crs    :epsg-3857
                 :center [-28.0 134.0]
                 :zoom   5}

   ;; Feature flags (declarative set)
   :features    #{:state-of-knowledge
                  :featured-maps
                  :floating-pills
                  :plot-component
                  :transect-control
                  :data-providers
                  :data-download}

   ;; Right drawer slot mappings
   :drawers     {:state-of-knowledge :sok/panel
                 :story-map          :featured-map/drawer}

   ;; Left drawer tabs (ordered)
   :tabs        [:catalogue :active-layers :featured-maps]

   ;; Map controls (ordered)
   :controls    [:menu :settings :zoom :print :omnisearch
                 :transect :region :share :reset :shortcuts :help]})

(def tas-marine-atlas
  {:id          :tas-marine-atlas
   :css-class   "tas-marine-atlas"

   :branding    {:logo   "img/TMA_Banner_size_website.png"
                 :title  "Tasmania Marine Atlas"
                 :url    "https://tasmarineatlas.org"}

   :map         {:crs    :epsg-3857
                 :center [-42.20 146.74]
                 :zoom   7}

   :features    #{:featured-maps
                  :data-in-region}  ;; No state-of-knowledge

   :drawers     {:story-map      :featured-map/drawer
                 :data-in-region :data-in-region/panel}

   :tabs        [:catalogue :active-layers :featured-maps]

   :controls    [:menu :settings :zoom :print :omnisearch
                 :region :share :reset :shortcuts :help]})

(def seamap-antarctica
  {:id          :seamap-antarctica
   :css-class   "seamap"

   :branding    {:logo   "img/SeaMapAntarctica_Logo_RGB_3000px.png"
                 :title  "Seamap Antarctica"
                 :url    "https://seamapantarctica.org"}

   :map         {:crs    :epsg-3031  ;; Antarctic polar projection
                 :center [-80 -90]
                 :zoom   2}

   :features    #{:state-of-knowledge
                  :floating-pills
                  :boundaries-pill
                  :zones-pill}

   :drawers     {:state-of-knowledge :sok/panel}

   :tabs        [:catalogue :active-layers]  ;; No featured maps

   :controls    [:menu :settings :zoom :print :omnisearch
                 :region :share :reset :shortcuts :help]})

(def futures-of-seafood
  {:id          :futures-of-seafood
   :css-class   "futures-of-seafood"

   :branding    {:logo   "img/Futures-of-Seafood-Logo.png"
                 :title  "Futures of Seafood"
                 :url    "https://futuresofseafood.org"}

   :map         {:crs    :epsg-3857
                 :center [-28.0 134.0]
                 :zoom   5}

   :features    #{:plot-component
                  :transect-control}

   :drawers     {}

   :tabs        [:catalogue :active-layers]

   :controls    [:menu :settings :zoom :print :omnisearch
                 :transect :region :share :reset :shortcuts :help]})

(def deployments
  {:seamap-australia   seamap-australia
   :tas-marine-atlas   tas-marine-atlas
   :seamap-antarctica  seamap-antarctica
   :futures-of-seafood futures-of-seafood})
```

### 2. Feature-Gated Component Rendering

Replace flag propagation with subscription-based feature checks:

```clojure
;; src/cljs/imas_seamap/subs/features.cljs

(re-frame/reg-sub
 :deployment/config
 (fn [db _]
   (get db :config)))

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
  (let [show-download? (feature? :data-download)]
    [:div.layer-card
     [layer-card-content layer]
     [layer-control
      {:tooltip (if show-download?
                  "Layer info / Download data"
                  "Layer info")}]]))
```

### 3. Slot-Based Layout System

Replace multiple `layout-app` variants with a single configurable layout:

```clojure
;; src/cljs/imas_seamap/views/layout.cljs

(defn layout-app []
  (let [config @(re-frame/subscribe [:deployment/config])]
    [:div {:class (:css-class config)}
     ;; Core content
     [content-wrapper]

     ;; Configurable panels
     [left-drawer {:tabs (:tabs config)
                   :branding (:branding config)}]
     [right-drawer {:slots (:drawers config)}]

     ;; Configurable controls
     [controls-panel {:controls (:controls config)}]

     ;; Feature-gated components
     (when (feature? :floating-pills)
       [floating-pills])
     (when (feature? :plot-component)
       [plot-component])

     ;; Always present
     [helper-overlay]
     [welcome-dialogue]
     [settings-overlay]
     [info-card]
     [loading-display]]))

(defn left-drawer [{:keys [tabs branding]}]
  [:div.left-drawer
   [branding-header branding]
   [tab-container
    (for [tab tabs]
      ^{:key tab}
      [(resolve-tab tab)])]])

(defn right-drawer [{:keys [slots]}]
  (let [active-drawer @(re-frame/subscribe [:ui/right-drawer])]
    (when-let [component-key (get slots active-drawer)]
      [(resolve-component component-key)])))

(defn controls-panel [{:keys [controls]}]
  [:div.leaflet-control-container
   (for [control controls]
     ^{:key control}
     [(resolve-control control)])])
```

### 4. Component Registry

Register components by keyword for dynamic resolution:

```clojure
;; src/cljs/imas_seamap/registry/components.cljs

(def component-registry
  {;; Right drawer panels
   :sok/panel              sok/state-of-knowledge-panel
   :featured-map/drawer    featured-map-drawer
   :data-in-region/panel   data-in-region-panel
   :dynamic-pill/drawer    dynamic-pill-drawer

   ;; Plot components
   :transect/plot          plot-component})

(def tab-registry
  {:catalogue     left-drawer-catalogue
   :active-layers left-drawer-active-layers
   :featured-maps left-drawer-featured-maps})

(def control-registry
  {:menu       menu-button
   :settings   settings-button
   :zoom       zoom-control
   :print      print-control
   :omnisearch omnisearch-control
   :transect   transect-control
   :region     region-control
   :share      share-control
   :reset      reset-control
   :shortcuts  shortcuts-control
   :help       help-control})

(defn resolve-component [k]
  (or (get component-registry k)
      (throw (ex-info "Unknown component" {:key k}))))

(defn resolve-tab [k]
  (or (get tab-registry k)
      (throw (ex-info "Unknown tab" {:key k}))))

(defn resolve-control [k]
  (or (get control-registry k)
      (throw (ex-info "Unknown control" {:key k}))))
```

### 5. Feature-Based Handler Registration

Extend the existing `config-handlers` pattern to be feature-driven:

```clojure
;; src/cljs/imas_seamap/handlers/registry.cljs

(def base-handlers
  "Handlers required by all deployments"
  {:subs   {...}
   :events {...}})

(def feature-handlers
  "Handlers registered only when feature is enabled"
  {:state-of-knowledge
   {:subs   {:sok/habitat-statistics    sok-subs/habitat-statistics
             :sok/bathymetry-statistics sok-subs/bathymetry-statistics
             :sok/boundaries            sok-subs/boundaries
             ...}
    :events {:sok/open                  sok-events/open
             :sok/close                 sok-events/close
             ...}}

   :data-in-region
   {:subs   {:data-in-region/data       dir-subs/data-in-region}
    :events {:data-in-region/open       dir-events/open
             :data-in-region/get        dir-events/get-data
             ...}}

   :featured-maps
   {:subs   {:featured-maps/all         fm-subs/all-maps}
    :events {:featured-maps/load        fm-events/load
             ...}}})

(defn register-for-features! [features]
  (register-handlers! base-handlers)
  (doseq [feature features]
    (when-let [handlers (get feature-handlers feature)]
      (register-handlers! handlers))))
```

### 6. Unified Entry Point

Single `core.cljs` that reads deployment from build configuration:

```clojure
;; src/cljs/imas_seamap/core.cljs

(ns imas-seamap.core
  (:require [imas-seamap.config.deployments :as deployments]
            [imas-seamap.handlers.registry :as registry]
            [imas-seamap.views.layout :as layout]))

(defn ^:export init
  [deployment-id api-url-base media-url-base wordpress-url-base img-url-base]
  (let [deployment-key (keyword deployment-id)
        config         (get deployments/deployments deployment-key)]

    (when-not config
      (throw (ex-info "Unknown deployment" {:id deployment-id})))

    ;; Register handlers based on features
    (registry/register-for-features! (:features config))

    ;; Boot with configuration
    (re-frame/dispatch-sync
     [:boot {:config           config
             :api-url-base     api-url-base
             :media-url-base   media-url-base
             :wordpress-url-base wordpress-url-base
             :img-url-base     img-url-base}])

    (dev-setup)
    (mount-root)))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (rdom/render [layout/layout-app]
               (.getElementById js/document "app")))
```

---

## Migration Path

### Phase 1: Extract Configuration (Low Risk)

1. Create `config/deployments.cljs` with configuration maps
2. Document current feature sets for each deployment
3. No changes to existing code - configuration is additive

### Phase 2: Add Feature Subscriptions (Low Risk)

1. Implement `:feature/enabled?` subscription
2. Add `(feature? ...)` helper function
3. Store config in app-db during boot
4. Existing code continues to work

### Phase 3: Migrate Leaf Components (Medium Risk)

1. Start with leaf components that use `tma?`
2. Replace `tma?` checks with `(feature? :feature-key)` calls
3. Remove `tma?` parameter from component signatures
4. Work upward through component tree

Key files to migrate:
- `map/layer_views.cljs` - `layer-card`, `layer-catalogue-controls`
- `views.cljs` - `left-drawer-catalogue`, `left-drawer-active-layers`

### Phase 4: Unify Layouts (Medium Risk)

1. Create slot-based `layout-app` in new namespace
2. Implement component/tab/control registries
3. Migrate one deployment at a time:
   - Start with simplest (Futures of Seafood)
   - Then TMA
   - Then Antarctica
   - Finally Seamap Australia

### Phase 5: Consolidate Entry Points (Higher Risk)

1. Unify all `core.cljs` files into single entry point
2. Update build configuration to pass deployment ID
3. Remove deployment-specific `core.cljs` files
4. Remove deployment-specific `views.cljs` files

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

---

## Files to Create

```
src/cljs/imas_seamap/
├── config/
│   └── deployments.cljs      # All deployment configurations
├── registry/
│   └── components.cljs       # Component/tab/control registries
├── subs/
│   └── features.cljs         # Feature flag subscriptions
├── handlers/
│   └── registry.cljs         # Feature-based handler registration
└── views/
    └── layout.cljs           # Unified slot-based layout
```

## Files to Eventually Remove

After full migration:
- `tas_marine_atlas/core.cljs`
- `tas_marine_atlas/views.cljs`
- `seamap_antarctica/core.cljs`
- `seamap_antarctica/views.cljs` (partial - keep map CRS config)
- `futuresofseafood/core.cljs`
- `futuresofseafood/views.cljs`

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

1. Review this proposal with the team
2. Decide on migration timeline and phases
3. Start with Phase 1 (configuration extraction) as it's additive and low-risk
4. Set up feature flags in CI to test all deployments

---

## Database Schema Initialization

### Current State

Each deployment has its own `db.cljs` file with subtle but important differences:

| Schema Difference | Australia | TMA | Antarctica | FoS |
|-------------------|-----------|-----|------------|-----|
| Map center | `[-27.82, 132.13]` | `[-42.20, 146.74]` | `[-80, -90]` | (uses main) |
| Map zoom | `4` | `7` | `2` | (uses main) |
| `:state-of-knowledge` | ✓ | ✗ | ✓ | ✗ |
| `:data-in-region` | ✗ | ✓ | ✗ | ✗ |
| `:display :welcome-overlay` | `false` | `true` | `false` | (uses main) |
| `:display :catalogue :region` | ✗ | ✓ | ✗ | ✗ |
| URL paths for SOK | ✓ | ✗ | ✓ | ✗ |
| URL path for `:data-in-region` | ✗ | ✓ | ✗ | ✗ |

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
        ;; Merge map config
        (assoc-in [:map :center] (:center map))
        (assoc-in [:map :zoom] (:zoom map))
        ;; Merge deployment-specific state
        (merge-with (fn [base override]
                      (if (map? base)
                        (merge base override)
                        override))
                    initial-state)
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

---

## Build Configuration Changes

### Current State

The `shadow-cljs.edn` currently compiles all 4 entry points into a single bundle:

```clojure
:modules {:app {:entries [imas-seamap.core
                          imas-seamap.futuresofseafood.core
                          imas-seamap.seamap-antarctica.core
                          imas-seamap.tas-marine-atlas.core]}}
```

Each deployment's `index.html` selects which init function to call via Ansible templating.

### Proposed Changes

#### Option A: Keep Single Bundle (Recommended for Phase 1-4)

During migration phases 1-4, keep the existing build configuration to minimize risk:

```clojure
;; shadow-cljs.edn - NO CHANGES during migration

:modules {:app {:entries [imas-seamap.core
                          imas-seamap.futuresofseafood.core
                          imas-seamap.seamap-antarctica.core
                          imas-seamap.tas-marine-atlas.core]}}
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

Use **Option A** during migration, then switch to **Option B** after Phase 5 completion. Only consider Option C if bundle size becomes a concern (unlikely since features are already conditionally loaded).

---

## Index.html Updates

### Current State

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

No formal test infrastructure exists. Testing is likely manual across deployments.

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
| Phase 5: Single Entry Point | Build validation, deployment tests | Higher |

### Test Automation Tools

Recommended tools:

- **Unit Tests**: shadow-cljs built-in test runner
- **Visual Regression**: BackstopJS or Percy.io
- **E2E Tests**: Playwright or Cypress
- **Performance**: Lighthouse CI
- **Accessibility**: axe-core
