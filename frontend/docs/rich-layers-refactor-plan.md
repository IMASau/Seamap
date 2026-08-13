# Rich-Layers Refactor Plan: Data-Driven Signal Graph

## Goal

Replace the current imperative, function-returning `map-layers` subscription with a DAG of small, memoizable re-frame subscriptions that produce data, not closures. Eliminate redundant calls to `enhance-rich-layer`.

## Target subscription DAG

```
[:map :layers]  ──┐
[:map :rich-layers :rich-layers] ──┼──► ::enhanced-rich-layers  (computed once, keyed by rl-id)
[:map :rich-layers :states]  ──┤           │
[:map :rich-layers :async-datas] ──┘           │
                                               ▼
                                   ::layer-id->enhanced-rl  (lookup: layer-id → enhanced rl)
                                               │
[:map :active-layers] ─────────────────────────┼──► ::catalogue-layers
[:map :categories] ────────────────────────────┘    ::visible-layers
[:filters :layers] ────────────────────────────────► ::filtered-layers
                                                     etc.
```

Each sub has well-defined inputs. Re-frame only recomputes a sub when its specific inputs change.

## Step 1: Extract `::enhanced-rich-layers` sub

**The biggest win for the least disruption.**

- Create a new subscription `::enhanced-rich-layers` that calls `enhance-rich-layer` for every rich-layer once, returning a map keyed by rich-layer ID.
- Have `map-layers` depend on this sub instead of building `:rich-layer-fn`.
- Eliminates redundant `enhance-rich-layer` calls from `rich-layer->displayed-layer`, `rich-layer->side-by-side-views-selected`, `rich-layer->layer-under-point`, and `layer->cql-filter`.

### Key files
- `src/cljs/imas_seamap/map/subs.cljs` — new sub definition, update `map-layers`
- `src/cljs/imas_seamap/map/utils.cljs` — helper functions that currently call `enhance-rich-layer` will need variants that accept pre-enhanced data

### Approach
- `::enhanced-rich-layers` returns:
  ```clojure
  {:by-rich-layer-id  {5 <enhanced-rl> ...}
   :by-layer-id       {2 <enhanced-rl> ...}   ;; layer-lookup, but to full objects
   :displayed->main   {42 2, 43 2 ...}}       ;; for loading-state remapping
  ```
- `map-layers` replaces `:rich-layer-fn` with a data lookup into `:by-layer-id`
- UI call sites that currently do `(rich-layer-fn layer)` switch to `(get by-layer-id (:id layer))`

## Step 2: Extract `::cql-filters` as a lookup map

- New subscription `::cql-filters` depends on `::enhanced-rich-layers` and dynamic-pills state.
- Returns `{layer-id "depth > 10 AND year = 2020", ...}`
- Replaces the `:cql-filter-fn` closure in `map-layers`.
- Consumers just `(get cql-filters layer-id)`.

### Key files
- `src/cljs/imas_seamap/map/subs.cljs` — new sub
- UI components that currently call `:cql-filter-fn` — switch to sub lookup

## Step 3: Break `map-layers` into smaller subs

Split the monolithic `map-layers` into focused subscriptions:

| New sub | Inputs | Output |
|---|---|---|
| `::catalogue-layers` | layers, categories, rich-layer-children | layers filtered to catalogue-visible |
| `::filtered-layers` | catalogue-layers, filter-text, rich-layer-children | layers matching search |
| `::sorted-layers` | catalogue-layers, sorting | sorted layer list |
| `::viewport-layers` | catalogue-layers, bounds | layers in current viewport |
| `::loading-layers` | layer-state, displayed->main | set of loading layer IDs |
| `::error-layers` | layer-state, enhanced-rich-layers | error predicate data (not a fn) |
| `::expanded-layers` | layer-state | set of legend-shown layer IDs |

Each recomputes only when its specific inputs change, rather than on any db change.

### Migration
- Keep `map-layers` as a facade initially, assembling its return map from these smaller subs.
- Migrate UI call sites to use the individual subs directly over time.
- Remove `map-layers` once nothing depends on it.

## Step 4: Assoc `:rich-layer` onto layer maps

Instead of layers and rich-layers as parallel structures joined ad-hoc, produce "augmented layers":

```clojure
;; A regular layer stays as-is:
{:id 1 :name "Bathymetry" :category :bathymetry ...}

;; A rich-layer main layer gets extra keys:
{:id 2 :name "Habitat 2020" :category :habitat ...
 :rich-layer {:id 5
              :displayed-layer <layer-map>
              :cql-filter "depth > 10"
              :alternate-views [...]
              :timeline [...]
              :controls [...]
              ...}}
```

- Done in `::catalogue-layers` sub: assoc `:rich-layer` onto each layer that has one.
- Downstream code keeps working with "layers" — the abstraction that works well.
- Rich-layer features are optional data; UI checks `(:rich-layer layer)`.
- No special lookup functions needed; no `db` threading.

## What this eliminates

| Current pain | Fix | Step |
|---|---|---|
| `enhance-rich-layer` called N times per render | Computed once in `::enhanced-rich-layers` sub | 1 |
| `:rich-layer-fn` / `:cql-filter-fn` closures defeat memoization | Replaced with data (maps/lookups) | 1, 2 |
| `db` threaded through every helper | Each sub destructures only what it needs | 3 |
| `map-layers` recomputes on any db change | Smaller subs only recompute when their inputs change | 3 |
| Cross-rich-layer indirection is hidden | Resolved once during enhancement, result is just data | 1 |
| Parallel layer/rich-layer structures joined ad-hoc | Rich-layer data lives on the layer map | 4 |

## Notes

- Steps are ordered by impact and can be done incrementally.
- Step 1 alone solves the recomputation problem.
- Steps 2-4 are architectural cleanup that can happen over time.
- The `enhance-rich-layer` function itself doesn't need to change for step 1; it just gets called in one place instead of many.
