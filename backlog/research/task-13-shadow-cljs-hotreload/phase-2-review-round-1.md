# Phase 2 — Adversarial Review (Round 1)

**Slug:** `task-13-shadow-cljs-hotreload`
**Reviewer:** Claude general-purpose subagent (Sonnet)
**Round:** 1 of 2 — Round 2 sign-off (Template 8) to follow after v2.
**Date:** 2026-05-18
**Subject artefact:** [`phase-2-understand.md`](phase-2-understand.md)
**Revised artefact:** [`phase-2-understand-v2.md`](phase-2-understand-v2.md)

> ⚠️ Round 1 was dispatched *before* the canonical adversarial-review prompt templates landed in the repo (`backlog/research/_fsm-prompts/adversarial-review-prompt-templates.md`). The brief was an orchestrator-written equivalent of Template 1 (Phase 2 Framing Review) — same anti-anchoring rule, same lens. Future phase reviews will use Templates 1–10 verbatim.

## Verdict

**Accept with material amendments.**

## Material Findings

### 1. Carried-forward Q1 (websocket URL) is answerable from on-disk evidence

v1 §4 dismissed Q1 ("what URL is the browser attempting?") as a Phase 3 runtime task. But the compiled dev build at `frontend/resources/public/js/app.js` (15.4 MB, dated 2026-05-18) is on the affected developer's disk and contains hard-coded `CLOSURE_DEFINES`:

- `shadow.cljs.devtools.client.env.server_host = "localhost"`
- `shadow.cljs.devtools.client.env.server_port = 9630`
- `shadow.cljs.devtools.client.env.use_document_host = true`
- `shadow.cljs.devtools.client.env.use_document_protocol = false`
- `shadow.cljs.devtools.client.env.devtools_url = ""`

`use_document_host = true` → host resolved from `document.location.hostname` (so `localhost` when loaded from `http://localhost:3451/index.html`). `server_port = 9630` → that port is the target. Compose publishes only `3451` and `8777`. The effective WS target is `ws://localhost:9630/...` — unreachable.

**Orchestrator verification (2026-05-18, independent grep of `app.js`):**

```
server_port":9630
server_host":"localhost"
use_document_host":true
use_document_protocol":false
devtools_url":""
```

Literal `9630` appears 4 times. Finding stands.

**Amendment:** Promote Q1 to **answered** with code-artifact evidence. DevTools confirmation is a *nice-to-have* for Phase 3, not the gate it was thought to be.

### 2. Mild trajectory lock-in through enumerated affordances

v1 is overall neutral, but four signposts cluster the reader toward "the fix is about port 9630 or `:devtools-url`":

- §1 bolds **"No mapping for port 9630"** (only emphasis in that paragraph).
- §3 (P)-6 explicitly names `:devtools-url`, `:proxy-url`, "and related WS-routing keys" as in-scope for later phases.
- §3 (P)-7 explicitly names "extending the port map" as "mechanically trivial".
- §4 new-Q1 again highlights 9630.

Each item is individually defensible (and (P)-6 / (P)-7 *are* honestly classified as preferences), but together they pre-shape Phase 5's solution space.

**Amendment:** Drop the bold; rewrite (P)-6 and (P)-7 to describe current state without naming candidate fixes. Let Phase 5 do the enumeration.

### 3. shadow-cljs 2.19.9 is in fact pinned and verifiable

v1 §1 hedged: "not pinned in EDN, would come from `package.json`/`shadow-cljs` install — not separately re-verified". §5 then claimed "every Column A fact was verified". Both slightly off: `frontend/package.json:36` pins `"shadow-cljs": "2.19.9"`.

**Orchestrator verification (2026-05-18):** confirmed `frontend/package.json:36` reads `"shadow-cljs": "2.19.9"`.

**Amendment:** Move 2.19.9 into the verified-fact block, cite `package.json:36`, drop §5's blanket overclaim.

### 4. Q2 deserves to be elevated to a gating observation

Q2 ("does a `.cljs` edit actually trigger hot reload while the banner is showing?") is *the* contract-verification step, but in v1 it sits alongside Q1 and Q3 with the same priority.

**Amendment:** Mark Q2 as the gating runtime observation that must precede Phase 3+ solution selection. Spell out the falsifying scenario: if a save propagates without manual refresh, the contract is already met and the work scope collapses to "cosmetic pill only".

## Non-Material Observations

- v1 was sometimes ambiguous about "in the codebase" vs. "in a clean checkout" — compiled `js/app.js` was treated as out-of-bounds (defensible but cost a load-bearing finding).
- Newly-raised Q3 (`frontend-shadow-cache` volume) and Q4 (cross-browser reproduction) are well-framed additions.
- Symptom-vs-contract framing in §2 was sound; v1 resisted the banner-cosmetic anchor effectively.

## Disposition

v1 → v2 with all four material amendments applied. Round 2 sign-off (per Template 8) to follow.
