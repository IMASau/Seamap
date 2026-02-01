# Configuration-Driven Refactor: Remaining Tasks

## Status Summary

### ✅ Completed Phases

- **Phase 1: Extract Configuration** - All deployment configs, content registry created
- **Phase 2: Feature Subscriptions** - Feature flags and branding subscriptions implemented
- **Phase 3: Migrate Leaf Components** - Eliminated `tma?` parameter from all leaf components
- **Test Harness** - Automated tests and deployment switcher UI created

### 🔄 Current Status

Ready for manual testing of the configuration system using the deployment switcher.

### 📋 Remaining Work

1. **Manual Testing** (Next immediate step)
2. **Phase 4: Unify Layouts**
3. **Phase 5: Consolidate Entry Points**

---

## Task 1: Manual Testing of Configuration System

**Priority**: HIGH - Must complete before Phase 4

**Objective**: Verify the configuration system works correctly across all 4 deployments.

### Steps

1. **Start the development server:**
   ```bash
   cd /home/mark/Condense/Seamap/frontend
   npx shadow-cljs watch dev
   ```

2. **Open the application:**
   - Navigate to `http://localhost:3451`
   - Look for blue settings icon in top-right corner

3. **Test each deployment:**
   Use the deployment switcher to test each configuration:

   **For Seamap Australia:**
   - [ ] Logo displays: "Seamap2_V2_RGB.png"
   - [ ] Layer info tooltip: "Layer info / Download data"
   - [ ] Region control label: "Select region"
   - [ ] Features enabled: state-of-knowledge, featured-maps, floating-pills, plot-component
   - [ ] Map centers at: [-27.82, 132.13], zoom 4
   - [ ] CRS: EPSG:3857

   **For TAS Marine Atlas:**
   - [ ] Logo displays: "TMA-blue.png"
   - [ ] Layer info tooltip: "Layer info" (no download)
   - [ ] Region control label: "Find Data in Region"
   - [ ] Features enabled: data-in-region, featured-maps, floating-pills
   - [ ] Custom colors applied (dark blue header)
   - [ ] Map centers at: [-42.5, 146.5], zoom 7
   - [ ] CRS: EPSG:3857

   **For Seamap Antarctica:**
   - [ ] Logo displays: "SeaMapAntarctica_Logo_RGB_3000px.png"
   - [ ] Features enabled: state-of-knowledge, floating-pills
   - [ ] Features disabled: featured-maps, data-in-region
   - [ ] Map centers at: [-80, -90], zoom 2
   - [ ] CRS: EPSG:3031 (Antarctic polar projection)

   **For Futures of Seafood:**
   - [ ] Logo displays: "Futures-of-Seafood-Logo-Reverse-400x218.png"
   - [ ] Features enabled: plot-component, transect-control
   - [ ] Features disabled: floating-pills, featured-maps
   - [ ] Map centers at: [-42.5, 147], zoom 7
   - [ ] CRS: EPSG:3857

4. **Test subscription tester:**
   - [ ] Use the subscription tester in the switcher panel
   - [ ] Test `:feature/enabled?` with various feature keys
   - [ ] Test `:branding/label` with various label keys
   - [ ] Verify results match expected configuration

5. **Check browser console:**
   - [ ] No errors during deployment switching
   - [ ] No warnings about missing subscriptions

### Reference Documentation

- Full testing guide: `docs/testing-configuration-system.md`
- Feature matrix: See table at end of `testing-configuration-system.md`

### Success Criteria

- All 4 deployments load without errors
- Feature flags work correctly for each deployment
- Branding (logos, labels) displays correctly
- Map configuration applies correctly
- No `tma?` related errors in console

---

## Task 2: Phase 4 - Unify Layouts

**Priority**: MEDIUM - Start after testing complete

**Objective**: Create a single unified layout that works for all deployments instead of 4 separate layout files.

### Current State

Currently have 4 separate layout files:
- `src/cljs/imas_seamap/views.cljs` (Seamap Australia)
- `src/cljs/imas_seamap/tas_marine_atlas/views.cljs` (TMA)
- `src/cljs/imas_seamap/seamap_antarctica/views.cljs` (Antarctica)
- `src/cljs/imas_seamap/futuresofseafood/views.cljs` (FoS)

### Target State

Single unified `layout-app` in `src/cljs/imas_seamap/views.cljs` that uses:
- Slot-based component system
- Configuration-driven component registration
- Feature flags to determine which components render

### Implementation Steps

#### Step 4.1: Create Component Registry

Create `src/cljs/imas_seamap/config/components.cljs`:

```clojure
(ns imas-seamap.config.components
  "Registry of all available layout components")

(def component-registry
  "Maps component keys to their implementations"
  {:drawer/catalogue         #'imas-seamap.views/left-drawer-catalogue
   :drawer/active-layers     #'imas-seamap.views/left-drawer-active-layers
   :drawer/featured-maps     #'imas-seamap.views/featured-maps-tab
   :control/transect         #'imas-seamap.views/transect-control
   :control/region           #'imas-seamap.views/region-control
   :control/print            #'imas-seamap.views/print-control
   ;; ... etc
   })

(defn get-component [component-key]
  "Get a component implementation by key"
  (get component-registry component-key))
```

#### Step 4.2: Add Component Slots to Deployment Config

Update `src/cljs/imas_seamap/config/deployments.cljs`:

```clojure
(def seamap-australia
  {;; ... existing config ...
   :layout {:left-drawer {:tabs [:catalogue :active-layers :featured-maps]}
            :right-drawer {:components [:state-of-knowledge :featured-map-drawer]}
            :footer {:components [:plot-component]}
            :controls [:menu :settings :zoom :print :omnisearch :transect :region]}})
```

#### Step 4.3: Create Slot-Based Layout

Update `src/cljs/imas_seamap/views.cljs`:

```clojure
(defn layout-app []
  (let [layout-config @(re-frame/subscribe [:layout/config])]
    [:div#main-wrapper
     [:div#content-wrapper
      [map-component]

      ;; Render footer components based on config
      (when-let [footer-components (:footer layout-config)]
        [:footer
         (for [component-key footer-components]
           ^{:key component-key}
           [(components/get-component component-key)])])]

     ;; Common components (always present)
     [welcome-dialogue]
     [settings-overlay]
     [info-card]
     [loading-display]

     ;; Drawer components based on config
     [left-drawer layout-config]
     [right-drawer]
     [custom-leaflet-controls]]))
```

#### Step 4.4: Migrate TMA-Specific Styles

Move TMA-specific CSS classes to use deployment-based CSS class:

```scss
// Instead of hardcoded .tas-marine-atlas
[class*="tma-"] .some-component { ... }
```

#### Step 4.5: Remove Old Layout Files

After verifying unified layout works:
- Keep `src/cljs/imas_seamap/views.cljs` as the single source
- Delete deployment-specific layout files (or mark as deprecated)

### Files to Modify

- `src/cljs/imas_seamap/config/deployments.cljs` - Add `:layout` configuration
- `src/cljs/imas_seamap/config/components.cljs` - NEW: Component registry
- `src/cljs/imas_seamap/views.cljs` - Update to slot-based layout
- `src/cljs/imas_seamap/subs/branding.cljs` - Add `:layout/config` subscription
- Layout files in subdirectories - Remove or deprecate

### Testing Phase 4

After implementation:
- [ ] All 4 deployments still load correctly
- [ ] Layout components appear in correct slots
- [ ] No duplicate code between deployment layouts
- [ ] Feature-specific components only appear when enabled

---

## Task 3: Phase 5 - Consolidate Entry Points

**Priority**: LOW - Final cleanup phase

**Objective**: Move from 4 separate build targets to a single entry point with runtime deployment selection.

### Current State

Currently have 4 shadow-cljs build targets:
- `:dev` → Seamap Australia
- `:tma` → TAS Marine Atlas
- `:antarctica` → Seamap Antarctica
- `:futuresofseafood` → Futures of Seafood

### Target State

Single `:app` build target that determines deployment at runtime.

### Implementation Steps

#### Step 5.1: Create Deployment Detection

Create `src/cljs/imas_seamap/deployment.cljs`:

```clojure
(ns imas-seamap.deployment
  "Runtime deployment detection"
  (:require [imas-seamap.config.deployments :as deployments]))

(defn detect-deployment []
  "Detect which deployment to use based on URL or environment"
  (let [hostname (.-hostname js/location)]
    (cond
      (re-find #"tasmarineatlas" hostname)    :tas-marine-atlas
      (re-find #"antarctica" hostname)        :seamap-antarctica
      (re-find #"futuresofseafood" hostname)  :futures-of-seafood
      :else                                   :seamap-australia)))

(defn get-deployment-config []
  "Get the deployment config for the current environment"
  (deployments/get-deployment (detect-deployment)))
```

#### Step 5.2: Update shadow-cljs.edn

```clojure
{:builds
 {:app {:target :browser
        :output-dir "resources/public/js"
        :asset-path "/js"
        :modules {:main {:init-fn imas-seamap.core/init}}
        :dev {:compiler-options {:closure-defines {goog.DEBUG true}}}
        :release {:compiler-options {:optimizations :advanced}}}}}
```

#### Step 5.3: Update Core Init

Update `src/cljs/imas_seamap/core.cljs`:

```clojure
(defn ^:export init []
  (let [deployment-config (deployment/get-deployment-config)]
    (re-frame/dispatch-sync [:boot
                             (get-api-url)
                             (get-media-url)
                             (get-wordpress-url)
                             "/img/"
                             deployment-config])
    (reagent/render [views/layout-app]
                    (js/document.getElementById "app"))))
```

#### Step 5.4: Update index.html

Single `resources/public/index.html` that loads the unified app:

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Seamap</title>
  <link rel="stylesheet" href="/css/app.css">
</head>
<body>
  <div id="app"></div>
  <script src="/js/main.js"></script>
</body>
</html>
```

#### Step 5.5: Update Backend Deployment

Update database schema to store deployment configs:

```sql
-- Migration: Add deployment column to appropriate table
ALTER TABLE your_app_table ADD COLUMN deployment VARCHAR(50) DEFAULT 'seamap-australia';
```

Update Django views to pass deployment info to frontend.

### Files to Modify

- `shadow-cljs.edn` - Consolidate build targets
- `src/cljs/imas_seamap/deployment.cljs` - NEW: Runtime detection
- `src/cljs/imas_seamap/core.cljs` - Update init to use detection
- `resources/public/index.html` - Single unified HTML
- Backend deployment configs - Update to pass deployment ID

### Testing Phase 5

- [ ] Single build produces working app for all deployments
- [ ] URL-based detection works correctly
- [ ] All environment variables load correctly
- [ ] Build size hasn't increased significantly

---

## Optional: Database Schema Changes

**If needed for multi-tenancy:**

### Add Deployment Tracking

```sql
-- Track which deployment a user/session is using
CREATE TABLE deployment_sessions (
  id SERIAL PRIMARY KEY,
  session_id VARCHAR(255) NOT NULL,
  deployment_id VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Add deployment field to relevant tables
ALTER TABLE user_preferences ADD COLUMN deployment VARCHAR(50);
ALTER TABLE saved_maps ADD COLUMN deployment VARCHAR(50);
```

### Backend API Changes

Update Django views to:
1. Accept deployment parameter
2. Filter data by deployment where appropriate
3. Return deployment-specific URLs and assets

---

## Success Criteria for Complete Migration

### Functionality
- [ ] All 4 deployments work correctly
- [ ] No `tma?` or deployment-specific boolean flags remain
- [ ] Feature flags control all deployment differences
- [ ] Single codebase, single build (or minimal builds)

### Code Quality
- [ ] No code duplication between deployments
- [ ] Clear separation of config from code
- [ ] Easy to add new deployments
- [ ] All tests passing

### Documentation
- [ ] Configuration system documented
- [ ] Deployment guide updated
- [ ] Component registry documented
- [ ] Migration notes for future developers

### Performance
- [ ] Build time not significantly increased
- [ ] Runtime performance unchanged
- [ ] Bundle size not significantly larger

---

## Notes and Considerations

### Adding New Deployments

After migration complete, adding a new deployment should be:

1. Create deployment config in `deployments.cljs`
2. Add welcome content to `content.cljs`
3. Add logo/assets to `resources/public/img/`
4. Add detection logic to `deployment.cljs` (Phase 5)
5. Update CSS if custom colors needed

No code changes required!

### Backward Compatibility

During migration:
- Keep old entry points working until Phase 5
- Maintain fallback behavior if config not loaded
- Don't break existing saved URLs or bookmarks

### Risk Mitigation

- Test each phase thoroughly before proceeding
- Keep git commits small and focused
- Maintain feature branch until fully tested
- Have rollback plan for production deployment

---

## Questions to Resolve

- [ ] How should production deployment detect which config to use?
- [ ] Should deployment config come from backend API or be embedded?
- [ ] Do we need user-selectable deployments or always automatic?
- [ ] Should saved map states include deployment information?

---

## Reference Documents

- Main proposal: `docs/configuration-driven-refactor-proposal.md`
- Testing guide: `docs/testing-configuration-system.md`
- Configuration files: `src/cljs/imas_seamap/config/`
- Test suite: `test/cljs/imas_seamap/config_test.cljs`
