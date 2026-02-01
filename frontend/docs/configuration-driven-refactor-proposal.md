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
