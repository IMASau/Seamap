# Rich-Layers Refactor Plan: Data-Driven Signal Graph

## Goal

Replace the current imperative, function-returning `map-layers` subscription with a
DAG of small, memoizable re-frame subscriptions that produce data, not closures.
Eliminate redundant calls to `enhance-rich-layer`.

## Governing invariant: data, not closures

**No subscription output may contain a function.** This is the invariant the whole
refactor serves, not a detail of any one step.

Reagent only skips a component re-render when the new sub value `=` the old one.
Every recompute of `:map/layers` mints fresh closures (`:rich-layer-fn`,
`:cql-filter-fn`, `:error-layers`, `:layer-opacities`), so its output is *never*
equal to its previous output — and all ~13 subscriber sites across the six apps
re-render whenever *any* of its 16 inputs changes. Until the closures are gone,
signal-graph extraction improves recompute granularity but buys **zero render
skipping**. Every new sub introduced by this plan must return plain data.

A corollary norm: **key lookups by `:id`, not by whole layer maps.** Loading,
error, opacity and visibility state are currently keyed/membership-tested with
full layer maps (`(get error-counts layer 0)`, `(some #{layer} visible-layers)`,
opacity keyed by layer — see the FIXME in `layer-set-opacity`). That is slow
(hashing ~30-key maps per row per render) and fragile (any change to a layer map
silently orphans its state). All new subs use id-keyed maps and id sets. This is
also a prerequisite for Step 4 (see caveat there).

## Current state (August 2026, post PR #323)

Done:
- Layer-2 `:dbsubs/*` extraction; `:map/layers` is signal-driven with 16 named inputs.
- `:map/layers.viewport` extracted, so map panning no longer recomputes the monolith
  via `:bounds`.
- `:map/base-layers` converted to layer-3; `:map/props` pruned.
- Events migrated from raw `db` to a `ctx` map via `db->ctx` (completed Aug 2026;
  the partial migration caused real bugs — see "Events side" below).

Not yet done (the substance of this plan):
- `:map/layers` still returns four closures; `enhance-rich-layer` still runs many
  times per render; `:layer-state` still recomputes the monolith on every tile event.
- `:map/rich-layers-side-by-side-views` and `:map.layer/displayed-layers-lookup`
  bypass the signal graph entirely (see Phase A).

> **Update:** Phases A–C are implemented, with one deliberate exception (below).
> `::enhanced-rich-layers`, the id-keyed layer-state family (`::loading-layers`,
> `::error-layers`, `::expanded-layers`, `::layer-opacities`), `::cql-filters`,
> and the catalogue pipeline (`::visible-layers`, `::catalogue-layers`,
> `::filtered-layers`, `::sorted-layers`) are all in place. `:map/layers` is now
> a pure-data facade over the narrow subs — **no closures anywhere in sub
> outputs**; views build any lookup closures locally from
> `:rich-layers-by-layer-id`. Derived subs (`visible-layers-legends`,
> `visible-side-by-side-layers-legends`, `timeseries-layers`, NHAT's
> displayed-layers lookups) are re-registered over narrow signals. The unused
> `:groups` key was dropped.
>
> **Deliberately deferred:** the "augmented layers" step (assoc `:rich-layer`
> onto layer maps). Its precondition — id-keying as the norm for layer
> membership/keys in the db (`:active-layers` sets, `:layer-state` maps, drag
> lists) — hasn't been done, and augmenting layer maps before that would break
> whole-map equality tests throughout. Remaining tidy-up: migrate view sites off
> the `:map/layers` facade to the narrow subs they use, then delete the facade;
> id-key the db layer-state; then revisit augmented layers.

## Key observations driving the ordering

1. **`:layer-state` is the highest-frequency input wired into the monolith.**
   `:tile-count`, `:error-count` and `:loading-state` are bumped by tile events —
   i.e. continuously while panning/zooming. Every tile load recomputes all of
   `:map/layers` (catalogue filtering, sorting, rich-layer enhancement) and, per
   the invariant above, re-renders every subscriber. This is the same class of
   leak the viewport-layers extraction fixed for `:bounds`, and probably larger.
   Hence the layer-state family is pulled forward into Phase B, ahead of the
   original Step 3 ordering.

2. **`layer->cql-filter` compounds enhancement costs.** Each call runs
   `enhance-rich-layer` *twice* (once directly, once inside
   `rich-layer->displayed-layer`), plus `->dynamic-pill` per matching pill — and
   `->dynamic-pill` itself calls `rich-layer->displayed-layer` for every active
   layer. Per-visible-layer cost is roughly quadratic in active layers.

3. **Enhancement isn't cheap even once.** `->control` is
   O(controls² × filter-combinations) with nested scans, so memoising
   enhancement to once-per-state-change matters beyond mere call-count reduction.

4. **Two subs escaped the signal-graph work entirely:**
   - `:map/rich-layers-side-by-side-views` is a raw-db sub that runs `db->ctx`
     and enhances *every* rich layer on *every* app-db change.
   - `:map.layer/displayed-layers-lookup` derives from `[:map/layers]`,
     inheriting all 16 signals plus the closure churn.
   Both become trivial lookups over `::enhanced-rich-layers`.

## Target subscription DAG

```
[:map :layers] ──► ::layers-by-id ──┐
[:map :rich-layers :rich-layers] ──► ::rich-layers-by-id ──┤
[:map :rich-layers :states] ───────────────────────────────┼──► ::enhanced-rich-layers
[:map :rich-layers :async-datas] ──────────────────────────┤      {:by-rich-layer-id ...
[:map :rich-layers :layer-lookup] ─────────────────────────┘       :by-layer-id ...
                                                                    :displayed->main ...}
                                                                        │
        ┌───────────────────────┬───────────────────────────┬──────────┤
        ▼                       ▼                            ▼          ▼
  ::cql-filters          ::loading-layers            ::error-layers   :map/rich-layers-
  (active layers only)   ::expanded-layers           (id set)          side-by-side-views
                         ::layer-opacities (id map)

[:map :active-layers] ──► ::catalogue-layers / ::visible-layers
[:filters :layers] ─────► ::filtered-layers
[:sorting] ─────────────► ::sorted-layers
```

Each sub has well-defined inputs, returns plain data, and re-frame only recomputes
it when its specific inputs change.

## Phase A: `::enhanced-rich-layers` (was Step 1)

**The biggest win for the least disruption.**

- Add `::layers-by-id` and `::rich-layers-by-id` as their own cheap subs. These
  index maps are currently rebuilt inline on every `map-layers` recompute, and
  again in `db->ctx` on every event.
- Create `::enhanced-rich-layers` with inputs {layers-by-id, rich-layers,
  rich-layers-by-id, rl-states, rl-async-datas, rl-lookup}, calling
  `enhance-rich-layer` for every rich-layer once:
  ```clojure
  {:by-rich-layer-id  {5 <enhanced-rl> ...}
   :by-layer-id       {2 <enhanced-rl> ...}   ;; layer-lookup, but to full objects
   :displayed->main   {42 2, 43 2 ...}}       ;; id-keyed, for loading-state remapping
  ```
- Immediately re-register on top of it (small diffs, real wins):
  - `:map/rich-layers-side-by-side-views` — becomes a filter over
    `:by-rich-layer-id` + active-layers.
  - `:map.layer/displayed-layers-lookup` — becomes
    `(get-in enhanced [:by-layer-id id :displayed-layer])`.
- `map-layers` consumes `::enhanced-rich-layers` instead of building
  `:rich-layer-fn`. Eliminates redundant `enhance-rich-layer` calls from
  `rich-layer->displayed-layer`, `rich-layer->side-by-side-views-selected`,
  `rich-layer->layer-under-point`, and `layer->cql-filter`.

**Bridge caveat:** keeping `:rich-layer-fn` as `#(get by-layer-id (:id %))`
during migration is fine, but it is still a closure and still poisons equality.
Treat it as scaffolding with a scheduled demolition; expose the lookup map itself
as a data key from day one so consumers can migrate off the fn.

### Key files
- `src/cljs/imas_seamap/map/subs.cljs` — new sub definitions, update `map-layers`
- `src/cljs/imas_seamap/map/utils.cljs` — helpers that currently call
  `enhance-rich-layer` need variants that accept pre-enhanced data

## Phase B: volatile state out of the monolith (was Steps 2–3, reordered)

1. Extract the layer-state family, all id-keyed:

   | New sub | Inputs | Output |
   |---|---|---|
   | `::loading-layers` | loading-state, displayed->main | set of loading layer IDs |
   | `::error-layers` | error-count, tile-count, displayed->main | set of layer IDs over the 40% error threshold (a *set*, not a predicate fn) |
   | `::layer-opacities` | layer-state | id-keyed opacity map |
   | `::expanded-layers` | layer-state | set of legend-shown layer IDs |

2. Drop `:layer-state` from `:map/layers` signals; migrate `layer-card` /
   `layer-component` props from `(loading-fn layer)`-style calls to lookups.

3. Extract `::cql-filters`, replacing the `:cql-filter-fn` closure:
   - Computed **only over active layers** — that's all the map needs; there is no
     reason to be able to compute filters for 2,000 catalogue layers.
   - Built on `::enhanced-rich-layers` plus an enhanced-dynamic-pills sub, so
     `->dynamic-pill` also runs once per state change (see observation 2).
   - Returns `{layer-id "depth > 10 AND year = 2020", ...}`.

**Acceptance criterion:** after this phase, panning/zooming produces **zero**
`:map/layers` recomputes and zero catalogue re-renders. Measurable with
re-frame-10x, or a counter in the sub during dev.

## Phase C: carve up the remainder; augmented layers (was Steps 3–4)

1. Split what's left of `map-layers` into focused subs:

   | New sub | Inputs | Output |
   |---|---|---|
   | `::catalogue-layers` | layers, categories, rich-layer-children | layers filtered to catalogue-visible |
   | `::filtered-layers` | catalogue-layers, filter-text, rich-layer-children | layers matching search |
   | `::sorted-layers` | catalogue-layers, sorting | sorted layer list |

   (`::viewport-layers` already extracted as `:map/layers.viewport`.)

2. Migration:
   - Keep `map-layers` as a facade initially, assembling its return map from the
     smaller subs.
   - Migrate the ~13 subscriber sites to the narrow subs they actually use (most
     destructure only 4–8 keys).
   - Remove `map-layers` once nothing depends on it.

3. Assoc `:rich-layer` onto layer maps ("augmented layers"):

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

   - Done in `::catalogue-layers`: assoc `:rich-layer` onto each layer that has one.
   - Downstream code keeps working with "layers" — the abstraction that works well.
   - Rich-layer features are optional data; UI checks `(:rich-layer layer)`.
   - No special lookup functions needed; no `db` threading.

   **Equality caveat:** augmenting a layer map changes its equality, which breaks
   every whole-map membership test and map key that still exists
   (`(some #{layer} active-layers)`, opacity/loading keyed by layer, ...). So this
   step comes *after* id-keying is the norm — or augmented layers must only be
   produced at the leaves (card props), never stored anywhere identity comparisons
   happen (`:active-layers`, `:visible-layers`, `:layer-state`).

## Events side

Event handlers keep using `db->ctx` — they fire orders of magnitude less often
than renders, and re-frame discourages dereferencing subs inside handlers. One
guard: extract the ctx/lookup construction into a single pure function shared by
`db->ctx` and the Phase A subs, so the reactive and non-reactive worlds can't
drift apart. (That drift — helpers changed to expect `ctx` while some callers
still passed `db`, failing silently via destructuring — is exactly what caused
the rich-layers feature-info regression fixed in Aug 2026.)

## What this eliminates

| Current pain | Fix | Phase |
|---|---|---|
| `enhance-rich-layer` called N times per render | Computed once in `::enhanced-rich-layers` | A |
| Raw-db / monolith-derived stragglers (`side-by-side-views`, `displayed-layers-lookup`) | Re-registered over `::enhanced-rich-layers` | A |
| Tile events recompute the entire catalogue | Layer-state family split out; `:layer-state` dropped from `map-layers` signals | B |
| `:rich-layer-fn` / `:cql-filter-fn` / `:error-layers` / `:layer-opacities` closures defeat render skipping | Replaced with id-keyed data | A, B |
| `->dynamic-pill` recomputed per layer per render | Enhanced once in its own sub | B |
| `map-layers` recomputes on any input change | Smaller subs only recompute when their inputs change | B, C |
| `db` threaded through every helper | Each sub destructures only what it needs | A–C |
| Whole layer maps as keys/set members | Id-keyed lookups throughout | A–C |
| Cross-rich-layer indirection is hidden | Resolved once during enhancement, result is just data | A |
| Parallel layer/rich-layer structures joined ad-hoc | Rich-layer data lives on the layer map | C |

## Notes

- Phases are ordered by impact and can be done incrementally; each lands
  independently.
- Phase A alone solves the recomputation problem; the *render-skipping* win only
  arrives as closures are actually removed from sub outputs and consumers switch
  to data (Phase B and the start of C).
- The `enhance-rich-layer` function itself doesn't need to change for Phase A; it
  just gets called in one place instead of many.
- Verify improvements empirically: recompute counters / re-frame-10x traces while
  panning, before and after each phase.
