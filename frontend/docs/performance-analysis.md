# Performance Analysis

## Overview

This document identifies performance anti-patterns in the SeaMap ClojureScript/re-frame application. The app handles large volumes of layer data and has accumulated patterns that cause unnecessary re-renders and expensive computations.

---

## Critical Issues (High Impact)

### 1. Subscriptions Returning New Collections Every Time

**Location:** `src/cljs/imas_seamap/subs.cljs:163-173`

The `dynamic-pills` subscription creates new collections on every evaluation:

```clojure
(defn dynamic-pills [{{:keys [dynamic-pills]} :dynamic-pills :as db} _]
  (let [dynamic-pills (mapv #(->dynamic-pill % db) dynamic-pills)]
    {:filtered
     (filterv
      #(seq (:active-layers %))
      dynamic-pills)
     :mapped
     (reduce
      (fn [mapped {:keys [id] :as dynamic-pill}]
        (assoc mapped id dynamic-pill))
      {} dynamic-pills)}))
```

**Problems:**
- `mapv` creates a new vector every time
- `filterv` creates a new vector every time
- `reduce` creates a new map every time
- Returns a new map with `:filtered` and `:mapped` keys on every call
- Even if source data hasn't changed, subscribers re-render due to reference inequality

**Impact:** Any component using `@(re-frame/subscribe [:dynamic-pills])` re-renders on every db change, not just when dynamic-pills data changes.

---

### 2. Inline Functions in Subscriptions

**Location:** `src/cljs/imas_seamap/map/subs.cljs:103-104`

The `map-layers` subscription returns inline anonymous functions:

```clojure
{:rich-layer-fn   #(enhance-rich-layer (layer->rich-layer % db) db)
 :cql-filter-fn   #(layer->cql-filter % db)}
```

**Problems:**
- `#(...)` creates a new function object on every subscription evaluation
- Components receiving these functions via props re-render every time
- Functions close over `db`, preventing any memoization

**Impact:** Every layer component re-renders constantly, even when layer data hasn't changed.

---

### 3. O(n²) Linear Searches

**Locations:**
- `src/cljs/imas_seamap/views.cljs:247`
- `src/cljs/imas_seamap/map/layer_views.cljs:497`

```clojure
:visible? (some #{layer} visible-layers)
```

**Problem:** `some` performs a linear search through `visible-layers` for each layer. With 100 active layers and 100 visible layers, this is 10,000 operations per render.

**Also found:**
- `src/cljs/imas_seamap/map/subs.cljs:75` - `(some #{id} rlc-ids)`
- `src/cljs/imas_seamap/map/layer_views.cljs:332` - Creates set inside render loop

---

### 4. Inline Anonymous Event Handlers

**Locations:** Throughout `views.cljs` and `map/views.cljs` (50+ occurrences)

```clojure
:on-click #(re-frame/dispatch [:help-layer/close])
:on-change #(re-frame/dispatch [:map.layers/filter value])
```

**Examples in views.cljs:**
- Line 75: `:on-close #(re-frame/dispatch [:help-layer/close])`
- Line 99: `:on-click #(re-frame/dispatch [:help-layer/open])`
- Line 172: `select-tab #(re-frame/dispatch [:ui.catalogue/select-tab catid %1])`
- Line 196: `:on-click #(re-frame/dispatch [:map/toggle-viewport-only])`
- Line 233: `#(re-frame/dispatch [:map.layers/filter value])`
- Lines 292-294: Event handlers in `plot-component`

**Examples in map/views.cljs:**
- Lines 224-227, 239-242, 250-253, 261-264: Event handlers for each layer type
- Pattern repeated for every layer type (WMS, tile, ESRI vector, ESRI image)

**Problem:** Each inline `#(...)` creates a new function reference on every render. React/Reagent compares props by reference, so child components always see "changed" props and re-render.

**Impact:** With 100 visible layers × 4 event handlers = 400 new function objects per render.

---

## Medium Impact Issues

### 5. Duplicate Computation in Subscriptions

**Location:** `src/cljs/imas_seamap/state_of_knowledge/subs.cljs:50-60`

```clojure
(defn valid-boundaries [db _]
  (let [{:keys [amp imcra meow]} (get-in db [:state-of-knowledge :boundaries])
        valid-boundaries (filter-valid-boundaries (get-in db [:state-of-knowledge :boundaries]))]
    (merge
     (:amp valid-boundaries)
     (:imcra valid-boundaries)
     (:meow valid-boundaries)
     (filter-valid-boundaries (get-in db [:state-of-knowledge :boundaries]))  ;; DUPLICATE!
     ...)))
```

**Problem:** `filter-valid-boundaries` is called twice with the same argument.

---

### 6. Subscriptions Not Using Layer 2/3 Composition

**Location:** `src/cljs/imas_seamap/map/subs.cljs:49-104`

The `map-layers` subscription does all computation in a single function without using `:<-` signal functions:

```clojure
(defn map-layers [{:keys [layer-state filters sorting]
                   {:keys [layers active-layers bounds categories rich-layer-children] :as db-map} :map
                   :as db} _]
  (let [categories (map-on-key categories :name)
        ;; ... 50 lines of computation
        ]))
```

**Problems:**
- Recomputes everything when any part of `db` changes
- No decomposition into smaller, cacheable subscriptions
- Creates multiple intermediate collections (`set`, `map`, `filter` results)

**Should be decomposed into:**
```clojure
(re-frame/reg-sub :map/categories-by-name :<- [:map/categories] ...)
(re-frame/reg-sub :map/filtered-layers :<- [:map/layers] :<- [:map/filter-text] ...)
(re-frame/reg-sub :map/loading-layers :<- [:map/layer-state] ...)
```

---

### 7. Statistics Subscriptions Create New Collections

**Location:** `src/cljs/imas_seamap/state_of_knowledge/subs.cljs:8-26`

```clojure
(defn habitat-statistics [db _]
  (let [{:keys [results loading? show-layers?]} (get-in db [:state-of-knowledge :statistics :habitat])
        results (map #(assoc % :color (get-in db [:habitat-colours (:habitat %)])) results)]
    {:results results :loading? loading? :show-layers? show-layers?}))
```

**Problem:** `map` creates a new sequence every time, even if `results` and colors haven't changed.

---

### 8. Random UUID Generated on Every Render

**Location:** `src/cljs/imas_seamap/map/views.cljs:366-374`

```clojure
^{:key (str active-base-layer)}
[leaflet/pane {:name (str (random-uuid) (.now js/Date)) ...}]
```

**Problem:** `(random-uuid)` is called on every render, creating new pane names. Leaflet may have issues with duplicate/changing pane names.

---

### 9. Cascading Event Dispatches

**Location:** `src/cljs/imas_seamap/events.cljs:27-64`

Boot flow uses cascading `dispatch-n`:

```clojure
{:when :seen? :events :construct-urls
 :dispatch-n [[:initialise-layers]
              [:sok/get-habitat-statistics]
              [:sok/get-bathymetry-statistics]
              [:sok/get-habitat-observations]]}

{:when :seen-all-of? :events [:map/update-layers :map/update-keyed-layers :map/update-rich-layers]
 :dispatch-n [[:map/join-keyed-layers] [:map/join-rich-layers]]}
```

**Problem:** Each dispatch triggers subscription recomputation. Cascading dispatches cause multiple re-render waves during boot.

---

### 10. Layers Stored as Vector, Not Indexed

**Location:** `src/cljs/imas_seamap/db.cljs`

```clojure
:layers []  ;; Vector requires linear search
```

**Problem:** Looking up a layer by ID requires:
```clojure
(first-where #(= (:id %) layer-id) layers)  ;; O(n)
```

**Should be:**
```clojure
:layers {}  ;; Map indexed by ID = O(1) lookup
```

---

### 11. Unreliable Key in Helper Overlay

**Location:** `src/cljs/imas_seamap/views.cljs:86`

```clojure
^{:key (hash eprops)}
[:div.helper-layer-wrapper ...]
```

**Problem:** Using `(hash eprops)` as a React key is unreliable:
- Hash collisions are possible
- DOM element properties are mutable
- Should use a stable identifier

---

## Summary Table

| Issue | Severity | Location | Impact |
|-------|----------|----------|--------|
| `dynamic-pills` returns new collections | HIGH | subs.cljs:163-173 | Components re-render on every DB change |
| `map-layers` inline functions | HIGH | map/subs.cljs:103-104 | Child components re-render constantly |
| Inline `#(re-frame/dispatch...)` | HIGH | views.cljs (50+ locations) | 50+ new functions per render |
| Linear search `(some #{layer} ...)` | HIGH | views.cljs:247, layer_views.cljs:497 | O(n²) with 100+ layers |
| `map-layers` monolithic computation | MEDIUM | map/subs.cljs:49-104 | Heavy computation on every DB change |
| `valid-boundaries` duplicate call | MEDIUM | state_of_knowledge/subs.cljs:50-60 | Unnecessary recomputation |
| Statistics subs create new seqs | MEDIUM | state_of_knowledge/subs.cljs:8-26 | Unnecessary re-renders |
| UUID on every render | MEDIUM | map/views.cljs:366-374 | Potential Leaflet issues |
| Cascading dispatch-n | MEDIUM | events.cljs:27-64 | Multiple re-render waves |
| Layers as vector not map | MEDIUM | db.cljs | O(n) lookups throughout |
| Unreliable hash key | LOW | views.cljs:86 | Potential DOM instability |

---

## Recommended Fixes

### Priority 1: Memoize Subscription Outputs

**Fix `dynamic-pills`:**

```clojure
;; Before - single subscription returning new collections
(re-frame/reg-sub :dynamic-pills dynamic-pills)

;; After - decompose into Layer 2/3 subscriptions
(re-frame/reg-sub
 :dynamic-pills/raw
 (fn [db _]
   (get-in db [:dynamic-pills :dynamic-pills])))

(re-frame/reg-sub
 :dynamic-pills/enhanced
 :<- [:dynamic-pills/raw]
 :<- [:db]  ;; or specific paths needed
 (fn [[pills db] _]
   (mapv #(->dynamic-pill % db) pills)))

(re-frame/reg-sub
 :dynamic-pills/filtered
 :<- [:dynamic-pills/enhanced]
 (fn [pills _]
   (filterv #(seq (:active-layers %)) pills)))

(re-frame/reg-sub
 :dynamic-pills/by-id
 :<- [:dynamic-pills/enhanced]
 (fn [pills _]
   (into {} (map (juxt :id identity)) pills)))
```

---

### Priority 2: Extract Stable Functions from `map-layers`

```clojure
;; Define at namespace level, memoized
(def enhance-rich-layer-memo (memoize enhance-rich-layer))
(def layer->cql-filter-memo (memoize layer->cql-filter))

;; Or create factory functions that return stable references
(defn make-rich-layer-fn [db-snapshot]
  ;; Cache based on relevant db paths, not entire db
  (let [relevant-data (select-keys db-snapshot [:rich-layers :layer-state])]
    (fn [layer]
      (enhance-rich-layer (layer->rich-layer layer relevant-data) relevant-data))))
```

---

### Priority 3: Convert Linear Searches to Set Lookups

```clojure
;; Before
:visible? (some #{layer} visible-layers)

;; After - ensure visible-layers is a set upstream
(re-frame/reg-sub
 :map/visible-layer-ids
 :<- [:map/visible-layers]
 (fn [layers _]
   (into #{} (map :id) layers)))

;; In component
(let [visible-ids @(re-frame/subscribe [:map/visible-layer-ids])]
  {:visible? (contains? visible-ids (:id layer))})
```

---

### Priority 4: Create Stable Event Dispatch Handlers

**Option A: Define handlers at namespace level**

```clojure
;; handlers.cljs
(def close-help #(re-frame/dispatch [:help-layer/close]))
(def open-help #(re-frame/dispatch [:help-layer/open]))

;; In component
:on-click handlers/close-help  ;; Stable reference
```

**Option B: Use a dispatch helper macro/function**

```clojure
;; utils.cljs
(defn dispatch-handler [event-vec]
  ;; Returns a memoized handler for this event
  (let [handlers (atom {})]
    (fn [event-vec]
      (or (@handlers event-vec)
          (let [h #(re-frame/dispatch event-vec)]
            (swap! handlers assoc event-vec h)
            h)))))

(def handler (dispatch-handler))

;; In component
:on-click (handler [:help-layer/close])  ;; Returns same fn for same event
```

**Option C: Use re-frame's built-in `dispatch` with `partial`**

```clojure
;; Define once
(def dispatch-close-help (partial re-frame/dispatch [:help-layer/close]))

;; Use
:on-click dispatch-close-help
```

---

### Priority 5: Index Layers by ID

```clojure
;; db.cljs - change schema
:layers {}  ;; Map of layer-id -> layer

;; When loading layers (events.cljs)
(re-frame/reg-event-db
 :map/update-layers
 (fn [db [_ layers]]
   (assoc-in db [:map :layers]
             (into {} (map (juxt :id identity)) layers))))

;; Lookup is now O(1)
(get-in db [:map :layers layer-id])
```

---

### Priority 6: Decompose `map-layers` Subscription

```clojure
;; Break into focused subscriptions
(re-frame/reg-sub
 :map/categories-indexed
 :<- [:map/categories]
 (fn [categories _]
   (into {} (map (juxt :name identity)) categories)))

(re-frame/reg-sub
 :map/rich-layer-child-ids
 :<- [:map/rich-layers]
 (fn [rich-layers _]
   (reduce (fn [acc {:keys [layer-id alternate-views timeline]}]
             (into acc (remove #{layer-id}
                               (concat (map :layer alternate-views)
                                       (map :layer timeline)))))
           #{} rich-layers)))

(re-frame/reg-sub
 :map/loading-layer-ids
 :<- [:map/layer-state]
 (fn [layer-state _]
   (->> (:loading-state layer-state)
        (filter (fn [[_ st]] (= st :map.layer/loading)))
        (map first)
        set)))

;; Components subscribe to specific pieces they need
```

---

### Priority 7: Fix Duplicate Computation

```clojure
;; Before
(defn valid-boundaries [db _]
  (let [boundaries (get-in db [:state-of-knowledge :boundaries])
        valid-boundaries (filter-valid-boundaries boundaries)]
    (merge
     (:amp valid-boundaries)
     (:imcra valid-boundaries)
     (:meow valid-boundaries)
     (filter-valid-boundaries boundaries)  ;; REMOVE THIS
     ...)))

;; After
(defn valid-boundaries [db _]
  (let [boundaries (get-in db [:state-of-knowledge :boundaries])
        {:keys [amp imcra meow] :as valid} (filter-valid-boundaries boundaries)]
    (merge amp imcra meow
           (select-keys (:amp boundaries) [...])
           ...)))
```

---

### Priority 8: Cache UUID for Panes

```clojure
;; Before
[leaflet/pane {:name (str (random-uuid) (.now js/Date)) ...}]

;; After - generate once per layer
(defn layer-pane [layer]
  (let [pane-id (str "pane-" (:id layer))]  ;; Deterministic, stable
    [leaflet/pane {:name pane-id ...}]))
```

---

## Estimated Impact

Fixing the high-priority issues (1-4) should reduce unnecessary re-renders by 70-80%. The most impactful changes are:

1. **Decomposing `dynamic-pills`** - Stops cascade of re-renders on unrelated db changes
2. **Extracting inline functions from `map-layers`** - Stops layer components re-rendering constantly
3. **Converting linear searches to sets** - Reduces O(n²) to O(n) for layer visibility checks
4. **Stable event handlers** - Prevents 50+ new function allocations per render

---

## Testing Recommendations

1. **Profile before/after** - Use React DevTools Profiler to measure render counts
2. **Add render counters** - Temporarily add `(js/console.log "render" component-name)` to key components
3. **Test with production data** - Performance issues are most visible with 100+ layers
4. **Check subscription cache hits** - Use re-frame-10x to monitor subscription recomputation

---

## Measuring Performance Improvements

### Key Metrics to Track

**1. Component Render Metrics**
- **Render count** - How many times each component renders
- **Render duration** - Time spent in each render
- **Wasted renders** - Renders where props/state didn't actually change

**2. Subscription Metrics**
- **Subscription recalculations** - How often subscriptions recompute
- **Subscription cache hit rate** - When memoization is working
- **Subscription execution time** - Duration of expensive computations

**3. Interaction Metrics**
- **Time to Interactive (TTI)** - Initial page load
- **Input latency** - Click/type → response time
- **Frame rate** - During interactions (should be 60fps)

---

### Recommended Measurement Tools

#### 1. React DevTools Profiler (Built-in)

**Setup:**
```javascript
// Start profiling before the interaction
// Perform actions (filter layers, toggle visibility, etc.)
// Stop profiling and review "Ranked" chart
```

**What to measure:**
- Component render counts before/after fixes
- Identify components with highest render time
- Look for "gray" bars (commits with no DOM changes = wasted work)

**Specific test cases:**
- Toggle a single layer → How many components re-render?
- Type in search box → Renders per keystroke?
- Initial load with 100+ layers → Time to render?

---

#### 2. re-frame-10x (Critical for re-frame apps)

**Installation:**
```clojure
;; project.clj or deps.edn
[day8.re-frame/re-frame-10x "1.9.9"]

;; In your core namespace
(ns imas-seamap.core
  (:require [day8.re-frame-10x]))
```

**What it shows:**
- Every subscription evaluation (with timing)
- Which subscriptions are cached vs recalculated
- Event flow and cascading dispatches
- Layer 2/3 subscription dependency graphs

**Key metrics:**
- Subscription "Run" count per interaction
- Time spent in each subscription
- Events per user action (fewer is better)

---

#### 3. Chrome Performance Timeline

**Workflow:**
```
1. Open DevTools → Performance tab
2. Check "Screenshots" and "Memory"
3. Click Record
4. Perform specific test action (e.g., "show all layers")
5. Stop recording
```

**What to analyze:**
- **Long Tasks** (yellow blocks) - Should be <50ms
- **Layout thrashing** - Repeated layout calculations
- **Memory sawtooth** - GC pressure from allocations
- **Frame rate** - Aim for steady 60fps (16.67ms per frame)

---

#### 4. Custom Instrumentation

**Add before/after counters:**

```clojure
;; In a utility namespace
(ns imas-seamap.perf
  (:require [re-frame.core :as rf]))

(defonce metrics (atom {}))

(defn record-render [component-name]
  (when ^boolean goog.DEBUG
    (swap! metrics update-in [:renders component-name] (fnil inc 0))))

(defn clear-metrics! []
  (reset! metrics {}))

(defn report-metrics []
  (js/console.table (clj->js @metrics)))

;; Add to window for easy access
(set! js/window.perfMetrics (fn [] (report-metrics)))
```

**In components:**
```clojure
(defn layer-item [layer]
  (perf/record-render :layer-item)
  [:div ...])
```

**Console usage:**
```javascript
// Perform action, then:
window.perfMetrics()
// Shows table of component render counts
```

---

#### 5. Memory Profiling

**Steps:**
1. Open DevTools → Memory tab
2. Take heap snapshot before interaction
3. Perform action (e.g., filter layers 10 times)
4. Take another heap snapshot
5. Compare snapshots → Look for retained objects

**Watch for:**
- Growing collections that should be cached
- Functions/closures that pile up (from inline handlers)
- Unexpected object retention

---

### Benchmark Test Suite

**Create reproducible test scenarios:**

```clojure
;; test/performance/benchmark.cljs
(ns imas-seamap.performance.benchmark
  (:require [re-frame.core :as rf]))

(defn benchmark [name f]
  (let [start (.now js/performance)]
    (f)
    (.setTimeout js/window
      (fn []
        (let [end (.now js/performance)]
          (js/console.log (str name ": " (- end start) "ms"))))
      0)))

(defn run-benchmarks []
  (benchmark "Load 100 layers"
    #(rf/dispatch [:test/load-layers 100]))

  (benchmark "Filter to 50% visible"
    #(rf/dispatch [:map.layers/filter "habitat"]))

  (benchmark "Toggle all layers"
    #(dotimes [i 100]
       (rf/dispatch [:map.layer/toggle i]))))
```

---

### Specific Test Cases for Identified Issues

**Issue #1: `dynamic-pills` re-renders**
```
1. Open React Profiler
2. Toggle a layer that's NOT in dynamic-pills
3. Count how many pill components re-render (should be 0 after fix)
```

**Issue #3: O(n²) searches**
```
1. Chrome Performance: Record with 100 visible layers
2. Render the layer list
3. Look for hot functions in "Bottom-Up" view
4. `some` function should disappear from top consumers after fix
```

**Issue #4: Inline handlers**
```
1. Memory timeline: Open/close help layer 50 times
2. Should see flat memory (not sawtooth from function allocations)
```

---

### Automated Performance Testing

**Continuous monitoring with Playwright/Puppeteer:**

```clojure
;; Example pseudo-code for automated tests

(def performance-tests
  [{:name "Layer filter latency"
    :action (type "#filter-input" "habitat")
    :assert (< response-time 100)}

   {:name "Render 100 layers"
    :action (dispatch [:load-layers 100])
    :assert (< time-to-interactive 2000)}])
```

---

### Recommended Workflow for Each Fix

**1. Baseline (before fix):**
- Open React DevTools Profiler + re-frame-10x
- Perform specific test action (e.g., "filter layers")
- Screenshot profiler flame graph
- Record: render count, total time, subscription runs

**2. Apply fix**

**3. Measure (after fix):**
- Same test action with same tools
- Compare metrics side-by-side

**4. Document results:**
- Example: "Before: 47 renders, 230ms. After: 12 renders, 45ms (80% reduction)"

---

### Quick Wins to Measure First

Start with these fixes as they show the most dramatic improvements:

1. **Fix Issue #1** (`dynamic-pills`) → Measure subscription recalculation count in re-frame-10x
2. **Fix Issue #4** (inline handlers) → Count renders in React Profiler when clicking buttons
3. **Fix Issue #3** (O(n²) searches) → Chrome Performance profiler showing hot functions disappearing
