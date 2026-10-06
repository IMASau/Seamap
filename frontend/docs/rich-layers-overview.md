# Rich-Layers: Data & Logic Overview

## What is a "layer"?

A base **layer** is a flat map fetched from the Django backend representing a single WMS/map layer. Key fields: `id`, `name`, `server_url`, `layer_name`, `category`, `bounding_box`, `filter` (CQL), `style`, etc. All layers live in `[:map :layers]` as a flat list.

## What is a "rich-layer"?

A **rich-layer** is a *wrapper* around a base layer that adds interactive UI features. It's a separate entity (with its own `id`) that *references* base layers by ID. Stored at `[:map :rich-layers :rich-layers]`.

A raw rich-layer from the API has:

| Field | Purpose |
|---|---|
| `id` | Rich-layer's own ID |
| `layer-id` | The "main" base layer ID |
| `tab-label`, `slider-label`, `icon`, `tooltip` | UI chrome |
| `alternate-views` | List of `{:layer <layer-id>, :sort_key}` — different layer variants the user can switch between |
| `timeline` | List of `{:layer <layer-id>, :value, :label}` — temporal snapshots |
| `side-by-side-views` | List of `{:layer <layer-id>, ...}` — for split-screen comparison |
| `controls` | List of filter controls (sliders, dropdowns) with `cql-property`, `controller-type`, `default-value`, etc. |

So a single rich-layer can reference **many** base layers (via alternate-views, timeline, side-by-side-views).

## The three state stores

Rich-layer data is split across three locations in the app-db:

1. **Config** `[:map :rich-layers :rich-layers]` — static definitions from API
2. **User state** `[:map :rich-layers :states {rl-id {...}}]` — user selections: which tab is active, which alternate-view/timeline/side-by-side is selected, control filter values
3. **Async data** `[:map :rich-layers :async-datas {rl-id {...}}]` — server-fetched control values and valid filter combinations

Plus a lookup table `[:map :rich-layers :layer-lookup]` mapping `{layer-id -> rich-layer-id}` for *all* referenced layers (main + alternates + timeline + side-by-side).

## `enhance-rich-layer` — the key function

`enhance-rich-layer` (`src/cljs/imas_seamap/map/utils.cljs`) is where the "enrichment" happens. It takes a raw rich-layer + a **ctx** map (a bundle of the lookups it needs — see `db->ctx`; a dev-build assertion rejects a raw `db`) and produces a fully resolved object by:

1. **Resolving layer IDs → layer objects**: Each `{:layer 42}` in alternate-views/timeline/side-by-side gets its `:layer` replaced with the full layer map (via `->alternate-view`, `->timeline`, `->side-by-side-view`)

2. **Applying user state**: Reads from `[:map :rich-layers :states id]` to find which alternate-view, timeline entry, and side-by-side view the user selected

3. **Computing `displayed-layer`**: The *actual* layer being shown = `timeline-selected` > `alternate-views-selected` > main layer. This is the critical derived value.

4. **Enriching controls**: Each control gets its `:value` (from user state, or default), `:values` (from async data, with validity flags based on filter-combinations), and a `:cql-filter` string

5. **Building `cql-filter`**: Joins all control CQL filters with `AND`

6. **Cross-rich-layer alternate views**: If the selected alternate-view points to a *different* rich-layer, its timeline and slider-label override the current one

## How the subscriptions use this

- **`::enhanced-rich-layers`** (`src/cljs/imas_seamap/map/subs.cljs`) calls `enhance-rich-layer` once per rich layer per state change, producing id-keyed data: `{:by-rich-layer-id …, :by-layer-id …, :displayed->main …}`. Everything downstream — views, other subs — *looks up* enhanced data here rather than enhancing on demand.
- **`::catalogue-layers`** computes `rlc-ids` — IDs of child layers (alternate-view/timeline layers that aren't the "main" layer) — and **hides them from the catalogue**; `::filtered-layers` / `::sorted-layers` layer search and sort on top of it.
- **`::cql-filters`** produces a `{layer-id → cql-string}` lookup for the *active* layers (combining rich-layer control filters + dynamic-pill filters + the layer's own `:filter`).
- **`::loading-layers` / `::error-layers` / `::expanded-layers` / `::layer-opacities`** carry the volatile per-layer state as id-keyed data, using `:displayed->main` to remap tile state (keyed by the displayed layer) back to catalogue layers.
- **`:map/layers`** is a pure-data facade assembling the catalogue pipeline plus `:rich-layers-by-layer-id`; subscription outputs contain **no closures**, so components only re-render when data they use actually changes. Views needing a function build one locally (e.g. `#(get rich-layers-by-layer-id (:id %))`).

Event handlers can't use subscriptions, so they build an equivalent ctx with `db->ctx` per event — acceptable because events fire far less often than renders.

## Data flow diagram

```
API fetch
  ↓
[:map :layers]  ←── flat list of base layers
[:map :rich-layers :rich-layers]  ←── raw rich-layer configs (layer IDs, not objects)
[:map :rich-layers :layer-lookup]  ←── {layer-id → rich-layer-id}
  ↓
User interacts → [:map :rich-layers :states {rl-id {selections...}}]
Server fetch   → [:map :rich-layers :async-datas {rl-id {values...}}]
  ↓
::enhanced-rich-layers sub — enhance-rich-layer(raw-rl, ctx), once per rich layer
  → resolves IDs to layer objects
  → applies user selections
  → computes displayed-layer, cql-filter
  → {:by-rich-layer-id …, :by-layer-id …, :displayed->main …}
  ↓
narrow subs (::cql-filters, ::loading-layers, legends, …) and views
look up enhanced data by id                        (events: db->ctx bridge)
```

## Historical pain points (resolved 2026; kept for context)

These were address in an August 2026 refactor; kept for historical context:

1. ~~`db` threading everywhere~~ — helpers now take a small `ctx`; subs supply it from narrow signals, events via `db->ctx`. A dev-build assertion (`assert-ctx!`) catches passing the raw db, which previously failed silently and caused several real bugs.
2. ~~`enhance-rich-layer` called redundantly~~ — computed once per state change in `::enhanced-rich-layers`.
3. ~~`map-layers` returns functions~~ — all subscription outputs are now data; the closures (`:rich-layer-fn`, `:cql-filter-fn`, `:error-layers`, `:layer-opacities`) are gone or built locally in views.

Still true and worth knowing:

4. **Cross-rich-layer references** (an alternate-view pointing to another rich-layer's timeline) add a non-obvious layer of indirection — now at least resolved in one place, during enhancement.
5. **Layer objects as keys** — `[:layer-state …]`, `:active-layers` membership and the displayed-layers lookup still key by whole layer maps rather than ids; id-keying the db is the remaining prerequisite for the deferred "augmented layers" step (see the refactor plan).
