# Testing the Configuration-Driven Architecture

This guide explains how to test the configuration system implemented in Phases 1-3.

## Overview

The configuration-driven architecture allows switching between deployments at runtime by loading different deployment configs. This guide covers both automated and manual testing approaches.

## Automated Tests

### Running the Tests

```bash
# Run all tests (when test runner is configured)
npx shadow-cljs compile test
node out/test.js

# Or use the REPL
npx shadow-cljs cljs-repl dev
```

In the REPL:

```clojure
(require '[imas-seamap.config-test :as test])
(test/run-all-tests-and-report)
```

### What the Tests Cover

- **Phase 1 Tests**: Deployment configs exist and are valid
- **Phase 2 Tests**: Feature and branding subscriptions work correctly
- **Phase 3 Tests**: Component migrations eliminated `tma?` flag properly

## Manual Testing in Browser

### Option 1: Using the Deployment Switcher (Recommended)

The deployment switcher is a dev-only UI component that allows switching between deployments.

#### 1. Add the Switcher to Your Layout

Edit `src/cljs/imas_seamap/views.cljs` (or your main layout file):

```clojure
(ns imas-seamap.views
  (:require
    ;; ... existing requires ...
    [imas-seamap.dev.deployment-switcher :as dev-switcher]))

(defn layout-app []
  (let [;; ... existing code ...
        ]
    [:div#main-wrapper
     ;; ... existing components ...

     ;; Add the deployment switcher (only shows in dev mode)
     [dev-switcher/switcher-panel]]))
```

#### 2. Add the CSS

Add the suggested CSS from `deployment_switcher.cljs` to `src/ui/css/app.scss`.

#### 3. Start the Dev Server

```bash
npx shadow-cljs watch dev
```

#### 4. Open the App

Navigate to `http://localhost:3451` and you should see a floating settings icon in the top-right corner.

#### 5. Test Deployment Switching

1. Click the settings icon to open the switcher panel
2. Click "Switch to This" on any deployment card
3. The app will reload with the new configuration
4. Verify the UI updates with the new branding and features

### Option 2: REPL-Based Testing

For more granular testing, use the REPL:

#### 1. Start Shadow-CLJS with REPL

```bash
npx shadow-cljs watch dev
# In another terminal:
npx shadow-cljs cljs-repl dev
```

#### 2. Load a Deployment Config

```clojure
(require '[re-frame.core :as re-frame])
(require '[imas-seamap.config.deployments :as deployments])

;; Load Seamap Australia config
(re-frame/dispatch-sync
 [:boot
  "http://localhost:8000/api/"
  "http://localhost:8000/media/"
  "http://localhost:8888/"
  "/img/"
  (deployments/get-deployment :seamap-australia)])
```

#### 3. Test Feature Subscriptions

```clojure
;; Check if a feature is enabled
@(re-frame/subscribe [:feature/enabled? :state-of-knowledge])
;; => true (for Australia)

@(re-frame/subscribe [:feature/enabled? :data-in-region])
;; => false (for Australia)

;; Get all features
@(re-frame/subscribe [:feature/all])
;; => #{:state-of-knowledge :featured-maps :floating-pills ...}
```

#### 4. Test Branding Subscriptions

```clojure
;; Get logo info
@(re-frame/subscribe [:branding/logo])
;; => {:src "img/Seamap2_V2_RGB.png", :alt "Seamap Australia", :height 96}

;; Get a specific label
@(re-frame/subscribe [:branding/label :layer-info])
;; => "Layer info / Download data" (for Australia)

;; Get map config
@(re-frame/subscribe [:map/crs])
;; => :epsg-3857

@(re-frame/subscribe [:map/initial-center])
;; => [-27.819644755 132.133333]
```

#### 5. Switch to a Different Deployment

```clojure
;; Switch to TAS Marine Atlas
(re-frame/dispatch-sync
 [:boot
  "http://localhost:8000/api/"
  "http://localhost:8000/media/"
  "http://localhost:8888/"
  "/img/"
  (deployments/get-deployment :tas-marine-atlas)])

;; Verify the switch
@(re-frame/subscribe [:deployment/id])
;; => :tas-marine-atlas

@(re-frame/subscribe [:feature/enabled? :data-in-region])
;; => true (TMA has this feature)

@(re-frame/subscribe [:branding/label :layer-info])
;; => "Layer info" (TMA doesn't show download)

@(re-frame/subscribe [:branding/label :region-control])
;; => "Find Data in Region" (TMA-specific label)
```

## Testing Checklist

Use this checklist to verify each deployment works correctly:

### For Each Deployment

- [ ] **Boot Process**
  - [ ] App loads without errors
  - [ ] Config is stored in app-db
  - [ ] Deployment ID is correct

- [ ] **Feature Flags**
  - [ ] Correct features are enabled/disabled
  - [ ] `:feature/enabled?` subscription works
  - [ ] `:feature/all` returns correct set

- [ ] **Branding**
  - [ ] Logo displays correctly
  - [ ] Title is correct
  - [ ] Labels are deployment-specific
  - [ ] Colors apply correctly (TMA/FoS)

- [ ] **Map Configuration**
  - [ ] Map centers at correct location
  - [ ] Zoom level is correct
  - [ ] CRS is correct (Antarctica uses :epsg-3031)

- [ ] **Component Behavior**
  - [ ] Layer info tooltip shows correct text
  - [ ] Region control shows correct label
  - [ ] Floating pills appear/disappear correctly
  - [ ] Plot component appears/disappears correctly
  - [ ] Featured maps tab appears/disappears correctly

### Specific Test Cases

#### Test Case 1: TMA vs Australia Layer Info

**Setup**: Switch between TMA and Australia

**Expected**:
- **Australia**: Layer info tooltip says "Layer info / Download data"
- **TMA**: Layer info tooltip says "Layer info"

**Test**:
```clojure
;; Australia
(re-frame/dispatch-sync [:boot ... (deployments/get-deployment :seamap-australia)])
@(re-frame/subscribe [:branding/label :layer-info])
;; => "Layer info / Download data"

;; TMA
(re-frame/dispatch-sync [:boot ... (deployments/get-deployment :tas-marine-atlas)])
@(re-frame/subscribe [:branding/label :layer-info])
;; => "Layer info"
```

#### Test Case 2: Feature-Specific Components

**Setup**: Switch between deployments with different features

**Expected**:
- **Australia**: Has state-of-knowledge panel, floating pills, plot component
- **TMA**: Has data-in-region panel, NO state-of-knowledge
- **Antarctica**: Has state-of-knowledge, NO featured maps
- **FoS**: Has plot component, NO floating pills

**Test**:
```clojure
;; Check features for each deployment
(doseq [deployment-id [:seamap-australia :tas-marine-atlas
                       :seamap-antarctica :futures-of-seafood]]
  (re-frame/dispatch-sync [:boot ... (deployments/get-deployment deployment-id)])
  (println deployment-id "features:" @(re-frame/subscribe [:feature/all])))
```

#### Test Case 3: Antarctica CRS

**Setup**: Switch to Antarctica

**Expected**: Map uses Antarctic polar projection (EPSG:3031)

**Test**:
```clojure
(re-frame/dispatch-sync [:boot ... (deployments/get-deployment :seamap-antarctica)])
@(re-frame/subscribe [:map/crs])
;; => :epsg-3031 (not :epsg-3857)

@(re-frame/subscribe [:map/initial-center])
;; => [-80 -90]
```

## Troubleshooting

### Subscriptions Return nil

**Problem**: All subscriptions return `nil`

**Cause**: Deployment config not loaded into app-db

**Solution**: Ensure `:boot` event is dispatched with config parameter:

```clojure
(re-frame/dispatch-sync
 [:boot
  "http://localhost:8000/api/"
  "http://localhost:8000/media/"
  "http://localhost:8888/"
  "/img/"
  (deployments/get-deployment :seamap-australia)])  ;; <-- Don't forget this!
```

### Components Still Using tma?

**Problem**: Components still receiving `tma?` parameter

**Cause**: Call sites haven't been updated yet

**Solution**: Check that all component calls removed the parameter:

```clojure
;; WRONG
[left-drawer-catalogue false]

;; CORRECT
[left-drawer-catalogue]
```

### Switcher Panel Not Showing

**Problem**: Deployment switcher doesn't appear

**Cause**: Either not in dev mode or not added to layout

**Solution**:
1. Check `config/debug?` is true
2. Verify switcher is added to layout-app
3. Check browser console for errors

### Features Not Working

**Problem**: Features behave inconsistently across deployments

**Cause**: Components may still have hardcoded behavior

**Solution**: Ensure all feature checks use subscriptions:

```clojure
;; WRONG
(when tma? [some-component])

;; CORRECT
(when (features/feature? :some-feature) [some-component])
```

## Next Steps

Once testing confirms:
- ✅ All deployments load correctly
- ✅ Feature flags work as expected
- ✅ Branding is deployment-specific
- ✅ Components render correctly
- ✅ No `tma?` parameters remain

You're ready to proceed to **Phase 4: Unify Layouts** where we'll:
- Create a single slot-based `layout-app`
- Create component registries
- Consolidate all deployment-specific views

## Reference: Deployment Feature Matrix

| Feature | Australia | TMA | Antarctica | FoS |
|---------|-----------|-----|------------|-----|
| state-of-knowledge | ✓ | ✗ | ✓ | ✗ |
| data-in-region | ✗ | ✓ | ✗ | ✗ |
| featured-maps | ✓ | ✓ | ✗ | ✗ |
| floating-pills | ✓ | ✓ | ✓ | ✗ |
| plot-component | ✓ | ✗ | ✗ | ✓ |
| transect-control | ✓ | ✗ | ✗ | ✓ |
| data-providers | ✓ | ✗ | ✗ | ✗ |
| data-download | ✓ | ✗ | ✗ | ✗ |
