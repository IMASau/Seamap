# Rich Layers: Configuration Guide (DRAFT)

This document explains how rich layers are **configured**: what features are
available, what inputs each requires, and how the configuration is represented —
first in the backend database where it is authored, then in the frontend app-db
where it is consumed.

For how the frontend *processes* this data at runtime (subscriptions,
`enhance-rich-layer`, the signal graph), see `rich-layers-overview.md` and
`rich-layers-refactor-plan.md`.

## Concept

A **rich layer** wraps a single catalogue layer (its "main" layer) and adds
interactive behaviour on top of it: switchable alternate views, a timeline
slider, side-by-side split comparison, and CQL filter controls. Under the hood
each of these features points at *other* ordinary catalogue layers, so one
conceptual layer in the UI is really a family of layers, with the rich-layer
record describing how they relate. All referenced layers must already exist as
normal `Layer` records (WMS details, bounding box, style, etc.); the rich-layer
machinery only orchestrates them.

Any layer referenced by a rich layer's alternate views or timeline — other than
the main layer itself — is automatically **hidden from the catalogue**, so users
only ever see the main layer entry.

## Backend representation

Configuration is authored in the Django admin, on the **RichLayer** model
(`catalogue.models`, admin page "Rich layers") with its four child tables edited
inline. All features are optional: a rich layer with no children configured
simply shows its tab chrome with nothing in it, so in practice you configure at
least one of the four.

### `RichLayer` (one per rich layer)

| Field | Required | Notes |
|---|---|---|
| `layer` | yes | FK to the main catalogue `Layer` — the entry users see and add |
| `tab_label` | yes | Label for the layer's "configure display" tab in the UI |
| `slider_label` | blank ok | Label above the timeline slider (e.g. "Year") |
| `alternate_view_label` | defaults to `"Alternate View"` | Label above the alternate-view dropdown |
| `icon` | yes | Blueprint icon name shown on the layer card |
| `tooltip` | yes | Tooltip for the icon |

### `RichLayerAlternateView` (0..n per rich layer)

Different variants of the same data the user can switch between (e.g. different
classification schemes). Selecting one swaps the *displayed* layer while the
catalogue entry stays the same.

| Field | Required | Notes |
|---|---|---|
| `layer` | yes | FK to the variant's catalogue `Layer` |
| `sort_key` | optional | `CharField(10)`; ordering is **lexicographic**, nulls last — zero-pad numeric keys |

**Cross-rich-layer alternates:** if an alternate view's layer is itself the main
layer of *another* rich layer, that other rich layer's timeline and
`slider_label` take over while the alternate is selected. This is how an
alternate view can carry its own timeline. When switching alternates, the
frontend tries to keep the "same" timeline position by matching the old
selection's `(value, label)` against the new timeline (only when the slider
label is unchanged).

### `RichLayerTimeline` (0..n per rich layer)

Temporal (or otherwise ordered) snapshots presented as a snapping slider.

| Field | Required | Notes |
|---|---|---|
| `layer` | yes | FK to the snapshot's catalogue `Layer` |
| `value` | yes | Float; the slider position. Spacing between stops is proportional to value gaps |
| `label` | yes | Shown under the slider stop |

Include an entry for the **main layer itself** with its own value — that entry
acts as the slider's default/rest position (selecting it clears the timeline
selection rather than recording one).

### `RichLayerSideBySideView` (0..n per rich layer)

Layers offered as the right-hand pane of a split-screen comparison against the
displayed layer.

| Field | Required | Notes |
|---|---|---|
| `layer` | yes | FK to the comparison catalogue `Layer` |
| `display_name` | yes | Short label used on the split divider (both sides use these when available) |
| `sort_key` | optional | Same lexicographic ordering rules as alternate views |

### `RichLayerControl` (0..n per rich layer)

Attribute filters rendered as UI controls; each contributes a CQL clause to the
displayed layer's requests.

| Field | Required | Notes |
|---|---|---|
| `cql_property` | yes | Attribute name on the layer's feature type (must exist in its WFS output) |
| `label` | yes | Control label |
| `data_type` | yes | `string` or `number` (numbers get numeric CQL comparison and a float `default_value`) |
| `controller_type` | yes | `slider`, `dropdown`, or `multi-dropdown` |
| `icon`, `tooltip` | optional | Empty strings stored as null |
| `default_value` | optional | See below |
| `show_invalid` | default true | Whether options incompatible with the other controls' current values appear disabled (`true`) or are hidden (`false`) |

Default-value semantics: the effective value is *user selection* →
`default_value` → for sliders, the **maximum** harvested value (e.g. the latest
year). For `multi-dropdown`, `default_value` is parsed as a **JSON array
string** (e.g. `["a", "b"]`); a bare string is wrapped in a one-element list.
(A known wart — a proper JSONField would need a data migration.)

**Requirements for controls to work:** control values are harvested from the
layer's data via WFS, so the main layer must be of type `wms`/`wms-non-tiled`,
its `server_url` must also answer WFS `GetFeature` for the same `layer_name`,
and every `cql_property` must be a real attribute. The layer's own `filter`
(base CQL) is applied when harvesting, so values outside it never appear.

## Serving the configuration

- **Rich-layer list**: a read-only REST endpoint serialises each `RichLayer`
  with its four child collections inline (alternate/side-by-side views ordered
  by `sort_key` nulls-last, timeline ordered by `value`). Fetched once at app
  boot.
- **Control values**: `GET /api/habitat/cqlfiltervalues?rich-layer-id=<id>`
  performs the WFS harvest and returns:
  - `values` — per `cql_property`, the sorted distinct values (nulls last);
  - `filter_combinations` — the distinct *tuples* of property values that
    co-occur in the data. The UI uses these to mark which options remain valid
    given the other controls' selections (see `show_invalid`).
  Responses are cached for 15 minutes; harvest failures return empty lists
  silently. The fetch is lazy — triggered the first time a user opens the
  layer's "filters" tab.

## Frontend representation (app-db)

On boot the raw configs are keywordised and renamed (snake→kebab: e.g.
`tab_label` → `tab-label`, `show_invalid` → `show-invalid?`; note the FK
`layer` becomes **`layer-id`** on the rich layer itself, while child entries
keep `:layer` holding a layer *id*). Three derived structures are built:

| Location | Shape | Purpose |
|---|---|---|
| `[:map :rich-layers :rich-layers]` | `[{:id … :layer-id … :alternate-views […] :timeline […] :side-by-side-views […] :controls […]} …]` | The static config, as fetched |
| `[:map :rich-layers :layer-lookup]` | `{layer-id → rich-layer-id}` | Maps **every** referenced layer (main + children) to its rich layer; first-wins, so a child that is itself a rich-layer main keeps its own mapping |
| `[:map :rich-layer-children]` | `{child-layer-id → #{main-layer-id …}}` | Used so catalogue search on a hidden child surfaces its parent |

Runtime state lives separately, keyed by rich-layer id:

| Location | Contents |
|---|---|
| `[:map :rich-layers :states <rl-id>]` | User selections: `:tab`, `:alternate-views-selected` (a layer id), `:timeline-selected` (a layer id), `:side-by-side-views-selected-id`, `{:controls {<cql-property> {:value …}}}` |
| `[:map :rich-layers :async-datas <rl-id>]` | Harvested `{:controls {<cql-property> {:values […]}}}` and `:filter-combinations` |

Config, state, and async data are combined by `enhance-rich-layer` — computed
once per state change in the `::enhanced-rich-layers` subscription
(`map/subs.cljs`) and looked up as data everywhere else. The key derived value
is `:displayed-layer`: **timeline selection → alternate-view selection → main
layer**, which is what map rendering, legends (including pinned legends),
feature-info clicks, and CQL requests all target. Event handlers use the same
helpers via the `db->ctx` bridge (`map/utils.cljs`).

## Worked example

A habitat rich layer with two alternates, a three-step timeline and a depth
filter:

- `RichLayer`: layer = *Seamap Habitat (National)*, tab_label = "Configure
  display", slider_label = "Year", icon = "layers", tooltip = "This layer has
  display options".
- Alternate views: *Habitat (Broad classes)* (sort_key `01`), *Habitat
  (Detailed)* (sort_key `02`).
- Timeline: *Habitat 2015* (value 2015, label "2015"), *Habitat 2020* (value
  2020, label "2020"), and the main layer itself (value 2024, label "2024") as
  the default position.
- Control: cql_property `depth_zone`, label "Depth zone", data_type `string`,
  controller_type `dropdown`, show_invalid true.

Result: the catalogue shows one entry; 2015/2020 variants and both alternates
are hidden from the catalogue; the layer card offers the tab with the alternate
dropdown, year slider, and depth filter; and whichever variant is displayed is
the one queried, legended, and CQL-filtered.

## Gotchas

- `sort_key` is a 10-char string sorted lexicographically: `"10" < "2"` — pad
  numbers (`"02"`, `"10"`).
- A timeline entry for the main layer is effectively required for sensible
  slider behaviour; without one the slider has no rest position corresponding
  to the default display.
- Slider controls with no `default_value` default to the *maximum* harvested
  value — intended for "latest year" semantics; set a default if that's wrong
  for your data.
- Dropdown options with a null value are presented as the string `"None"` and
  translated back to null when filtering.
- The alternate-switch timeline carry-over matches on `(value, label)` with an
  unchanged slider label — keep values and labels consistent across related
  rich layers if you want the position preserved.
- `cqlfiltervalues` failures are silent (empty controls) and cached for 15
  minutes — if a control shows no options, check the layer's WFS by hand before
  assuming a config error, and expect a delay after fixing server-side issues.
- Deleting referenced layers is blocked (`on_delete=PROTECT`) — remove the
  rich-layer references first.
