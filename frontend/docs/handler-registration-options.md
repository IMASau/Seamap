# Handler Registration Options for Configuration-Driven Architecture

**Context**: Exploring approaches for conditionally registering re-frame subscriptions and event handlers based on which components are actually used in a deployment.

**Goal**: Register handlers only for components that are enabled, without putting handler registration details directly in deployment configurations.

---

## The Challenge

Looking at the current code, each deployment has a large `config-handlers` map (100+ handlers). Many handlers are shared across deployments, but some are feature-specific:
- `:sok/*` handlers (12+) only used by Australia/Antarctica
- `:data-in-region/*` handlers (3) only used by TMA
- `:sm/*` handlers (2) only used by deployments with featured maps

The proposal's section 5 suggests feature-based registration, but several alternative approaches are worth considering.

---

## Option 1: Feature-Based Handler Modules (Refinement of Proposal)

**Concept**: Group handlers by feature into separate modules that export a registration function.

```clojure
;; src/cljs/imas_seamap/state_of_knowledge/registry.cljs
(ns imas-seamap.state-of-knowledge.registry
  (:require [imas-seamap.state-of-knowledge.events :as sok-events]
            [imas-seamap.state-of-knowledge.subs :as sok-subs]))

(defn handlers
  "Returns handlers map for state-of-knowledge feature"
  []
  {:subs   {:sok/habitat-statistics               sok-subs/habitat-statistics
            :sok/habitat-statistics-download-url   sok-subs/habitat-statistics-download-url
            :sok/bathymetry-statistics             sok-subs/bathymetry-statistics
            :sok/bathymetry-statistics-download-url sok-subs/bathymetry-statistics-download-url
            :sok/habitat-observations              sok-subs/habitat-observations
            :sok/amp-boundaries                    sok-subs/amp-boundaries
            :sok/imcra-boundaries                  sok-subs/imcra-boundaries
            :sok/meow-boundaries                   sok-subs/meow-boundaries
            :sok/valid-boundaries                  sok-subs/valid-boundaries
            :sok/boundaries                        sok-subs/boundaries
            :sok/active-boundary                   sok-subs/active-boundary
            :sok/active-boundaries?                sok-subs/active-boundaries?
            :sok/active-zones?                     sok-subs/active-zones?
            :sok/open?                             sok-subs/open?
            :sok/boundary-layer-filter             sok-subs/boundary-layer-filter-fn
            :sok/region-report-url                 sok-subs/region-report-url}

   :events {:sok/update-amp-boundaries            sok-events/update-amp-boundaries
            :sok/update-imcra-boundaries          sok-events/update-imcra-boundaries
            :sok/update-meow-boundaries           sok-events/update-meow-boundaries
            ;; ... all other SOK events
            }})
```

```clojure
;; src/cljs/imas_seamap/tas_marine_atlas/data_in_region/registry.cljs
(ns imas-seamap.tas-marine-atlas.data-in-region.registry
  (:require [imas-seamap.tas-marine-atlas.events :as tma-events]
            [imas-seamap.tas-marine-atlas.subs :as tma-subs]))

(defn handlers []
  {:subs   {:data-in-region/data tma-subs/data-in-region}
   :events {:data-in-region/open [tma-events/data-in-region-open]
            :data-in-region/get  [tma-events/get-data-in-region]
            :data-in-region/got  tma-events/got-data-in-region}})
```

```clojure
;; src/cljs/imas_seamap/handlers/registry.cljs
(ns imas-seamap.handlers.registry
  (:require [imas-seamap.core :as core]
            ;; Require ALL feature modules upfront (but don't register yet)
            [imas-seamap.state-of-knowledge.registry :as sok-registry]
            [imas-seamap.tas-marine-atlas.data-in-region.registry :as dir-registry]
            [imas-seamap.story-maps.registry :as sm-registry]))

(def feature-handler-modules
  "Maps feature flags to handler registration functions"
  {:state-of-knowledge sok-registry/handlers
   :data-in-region     dir-registry/handlers
   :featured-maps      sm-registry/handlers})

(defn register-for-features!
  "Registers handlers for enabled features"
  [features base-handlers]
  ;; Register base handlers (always needed)
  (core/register-handlers! base-handlers)

  ;; Register feature-specific handlers
  (doseq [feature features]
    (when-let [handler-fn (get feature-handler-modules feature)]
      (js/console.log "Registering handlers for feature:" feature)
      (core/register-handlers! (handler-fn)))))
```

**Pros:**
- Clean separation of concerns - each feature owns its handlers
- Easy to find all handlers for a feature
- No handler code in deployment configs
- Still requires all namespaces (no lazy loading)

**Cons:**
- Still loads all handler code even if not used
- Requires new file per feature module
- Some duplication with existing events/subs namespaces

---

## Option 2: Component Self-Registration Pattern

**Concept**: Components register their own handlers when first rendered using React lifecycle hooks.

```clojure
;; src/cljs/imas_seamap/state_of_knowledge/views.cljs
(ns imas-seamap.state-of-knowledge.views
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.state-of-knowledge.handlers :as handlers]))

(defonce ^:private handlers-registered? (atom false))

(defn ensure-handlers-registered!
  "Idempotent handler registration - safe to call multiple times"
  []
  (when-not @handlers-registered?
    (js/console.log "Registering state-of-knowledge handlers")
    (handlers/register!)  ;; Calls re-frame/reg-sub and reg-event-fx
    (reset! handlers-registered? true)))

(defn state-of-knowledge-panel []
  (r/with-let [_ (ensure-handlers-registered!)]
    (let [boundaries @(re-frame/subscribe [:sok/boundaries])
          habitat-stats @(re-frame/subscribe [:sok/habitat-statistics])]
      ;; ... component rendering
      )))
```

```clojure
;; src/cljs/imas_seamap/state_of_knowledge/handlers.cljs
(ns imas-seamap.state-of-knowledge.handlers
  (:require [re-frame.core :as re-frame]
            [imas-seamap.state-of-knowledge.events :as events]
            [imas-seamap.state-of-knowledge.subs :as subs]))

(defn register!
  "Register all state-of-knowledge handlers"
  []
  ;; Register subscriptions
  (re-frame/reg-sub :sok/habitat-statistics subs/habitat-statistics)
  (re-frame/reg-sub :sok/bathymetry-statistics subs/bathymetry-statistics)
  ;; ... more subs

  ;; Register events
  (re-frame/reg-event-fx :sok/get-habitat-statistics events/get-habitat-statistics)
  (re-frame/reg-event-fx :sok/get-bathymetry-statistics events/get-bathymetry-statistics)
  ;; ... more events
  )
```

**Pros:**
- True lazy registration - only when component actually renders
- Component ownership is explicit
- No central registry needed
- Works well with code splitting

**Cons:**
- Handlers registered at render time (slight perf hit on first render)
- Need atom to track registration state
- Less declarative than upfront registration
- Harder to see all handlers at a glance

---

## Option 3: Lazy Namespace Loading with Auto-Registration

**Concept**: Use namespace side-effects to auto-register when required.

```clojure
;; src/cljs/imas_seamap/state_of_knowledge/handlers.cljs
(ns imas-seamap.state-of-knowledge.handlers
  (:require [re-frame.core :as re-frame]
            [imas-seamap.state-of-knowledge.events :as events]
            [imas-seamap.state-of-knowledge.subs :as subs]))

;; Auto-register handlers when namespace is loaded
(re-frame/reg-sub :sok/habitat-statistics subs/habitat-statistics)
(re-frame/reg-sub :sok/bathymetry-statistics subs/bathymetry-statistics)
;; ... all subs

(re-frame/reg-event-fx :sok/get-habitat-statistics events/get-habitat-statistics)
;; ... all events

(js/console.log "State-of-knowledge handlers registered")
```

```clojure
;; In component namespace - just require the handlers
(ns imas-seamap.state-of-knowledge.views
  (:require [imas-seamap.state-of-knowledge.handlers]))  ;; Side-effect: registers handlers
```

**Pros:**
- Simplest code - just require the namespace
- Automatic registration
- Works with ClojureScript's namespace system

**Cons:**
- Relies on side-effects (less explicit)
- Harder to control registration timing
- All handler code must be loaded when namespace is required
- Can't easily conditionally register

---

## Option 4: Manifest-Based Registration

**Concept**: Components declare their handler dependencies in metadata, central registry handles registration.

```clojure
;; src/cljs/imas_seamap/registry/component_manifests.cljs
(ns imas-seamap.registry.component-manifests)

(def manifests
  "Component handler dependency manifests"

  {:sok/panel
   {:requires-handlers #{:state-of-knowledge}
    :component-ns      'imas-seamap.state-of-knowledge.views}

   :data-in-region/panel
   {:requires-handlers #{:data-in-region}
    :component-ns      'imas-seamap.tas-marine-atlas.data-in-region.views}

   :featured-map/drawer
   {:requires-handlers #{:featured-maps}
    :component-ns      'imas-seamap.story-maps.views}})
```

```clojure
;; src/cljs/imas_seamap/views/layout.cljs
(ns imas-seamap.views.layout
  (:require [imas-seamap.registry.components :as components]
            [imas-seamap.registry.component-manifests :as manifests]
            [imas-seamap.handlers.registry :as handler-registry]))

(defonce ^:private loaded-handlers (atom #{}))

(defn ensure-handlers-for-component!
  "Ensures handlers are registered for a component before rendering"
  [component-key]
  (when-let [manifest (get manifests/manifests component-key)]
    (doseq [handler-set (:requires-handlers manifest)]
      (when-not (contains? @loaded-handlers handler-set)
        (handler-registry/register-feature! handler-set)
        (swap! loaded-handlers conj handler-set)))))

(defn right-drawer []
  (let [active-drawer @(re-frame/subscribe [:ui/right-drawer])
        config @(re-frame/subscribe [:deployment/config])
        component-key (get-in config [:drawers active-drawer])]

    (when component-key
      (ensure-handlers-for-component! component-key)  ;; Lazy load handlers
      [(components/resolve-component component-key)])))
```

**Pros:**
- Declarative dependencies
- Central visibility of what depends on what
- Lazy registration when component actually used
- Easy to audit and optimize

**Cons:**
- Extra layer of indirection
- Manifest must be kept in sync with actual dependencies
- More complex setup

---

## Option 5: Hybrid - Feature Flags + Just-In-Time Registration

**Concept**: Use deployment feature flags to pre-load feature namespaces, but defer actual registration until component render.

```clojure
;; src/cljs/imas_seamap/core.cljs
(defn init [api-url-base media-url-base wordpress-url-base img-url-base deployment-id]
  (let [deployment-config (deployments/get-deployment (keyword deployment-id))]

    ;; Register base handlers (always needed)
    (register-handlers! base-handlers)

    ;; Pre-require feature namespaces based on feature flags
    ;; (This loads the code but doesn't register handlers yet)
    (when (contains? (:features deployment-config) :state-of-knowledge)
      (require 'imas-seamap.state-of-knowledge.handlers))

    (when (contains? (:features deployment-config) :data-in-region)
      (require 'imas-seamap.tas-marine-atlas.data-in-region.handlers))

    ;; ... boot continues
    ))
```

Components still use Option 2's self-registration pattern.

**Pros:**
- Only loads handler code for enabled features
- Registration deferred until actually needed
- Best of both worlds for code splitting

**Cons:**
- Most complex approach
- Dynamic require in ClojureScript can be tricky
- Two-phase loading (require + register)

---

## Comparison Matrix

| Approach | Code Loaded | Registration Timing | Complexity | Visibility | Recommendation |
|----------|-------------|---------------------|------------|------------|----------------|
| **Option 1: Feature Modules** | All upfront | Boot time | Low | High | Simple & clear |
| **Option 1.5: Symbol-Based Lazy** | On feature enable | Boot time | Low-Med | Medium | ⭐ **Best for lazy loading** |
| **Option 2: Self-Registration** | On component use | First render | Medium | Medium | Good for code splitting |
| **Option 3: Namespace Side-Effects** | When required | Namespace load | Low | Low | Avoid - too implicit |
| **Option 4: Manifest-Based** | On component use | First render | High | High | Overkill for this use case |
| **Option 5: Hybrid** | Feature-gated + JIT | First render | High | Medium | Only if bundle size critical |

---

## Recommendation: **Option 1 (Feature-Based Handler Modules)**

Given the constraints and architecture, both **Option 1** and **Option 1.5** are good choices:

### Choose Option 1 if:
- Simplicity is paramount
- All features' handler code is relatively small
- Boot time is not a concern
- You want direct function references (better IDE support)

This has the core benefits:

1. **Keeps configs clean** - No handler registration details in deployment configs
2. **Simple mental model** - "This feature uses these handlers"
3. **Easy migration** - Extract handlers from existing `config-handlers` maps into feature modules
4. **Good visibility** - Easy to see what handlers each feature needs
5. **Works with existing code** - Fits naturally with current `register-handlers!` pattern

### Implementation Steps

1. Create `registry.cljs` files for each major feature:
   - `state_of_knowledge/registry.cljs`
   - `tas_marine_atlas/data_in_region/registry.cljs`
   - `story_maps/registry.cljs`

2. Extract feature-specific handlers from `core.cljs` into these registries

3. Update core init to use feature-based registration:

```clojure
(defn init [... deployment-id]
  (let [deployment-config (deployments/get-deployment (keyword deployment-id))]
    (handler-registry/register-for-features!
      (:features deployment-config)
      base-handlers)
    ;; ... rest of boot
    ))
```

4. Base handlers remain in `core.cljs` as a large map (or extracted to `handlers/base.cljs`)

---

## Alternative Consideration: Option 2 for Specific Cases

While Option 1 is best for the general case, **Option 2 (Component Self-Registration)** could be useful for:
- Very large features with many handlers (100+)
- Features that are rarely used even when enabled
- Code that you want to split into separate bundles

These could be handled on a case-by-case basis while using Option 1 for most features.

---

## Notes

- All options assume the existing feature flag system in deployment configs is preserved
- The goal is to avoid putting handler registration *details* in configs, not to avoid referencing features
- Option 1 strikes the best balance between simplicity, visibility, and keeping configs declarative
