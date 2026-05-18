# Phase 3 — Adversarial Review (Round 1)

**Slug:** `task-13-shadow-cljs-hotreload`
**Reviewer:** Claude general-purpose subagent (Sonnet) — per reviewer-model assignment policy (Round 1 = Claude).
**Round:** 1 of 2 — Round 2 sign-off (Template 8) reserved for Pro/GPT-5.5 on demand.
**Date:** 2026-05-18
**Subject artefact:** [`phase-3-specific.md`](phase-3-specific.md)
**Revised artefact:** Not written — workflow short-circuited at end of Phase 3 cycle after a Google search (2026-05-18) confirmed the standard fix pattern is well-known. See [`00-fsm-state.md`](00-fsm-state.md) for context. The amendments this review would have produced are recorded above for the record but were not merged into a v2.
**Template used:** Template 2 (Phase 3: Spec Review) from `_fsm-prompts/adversarial-review-prompt-templates.md`, post-amendments (with the standing Lateral findings, premature-verdict, and self-critique slots).

## Verdict

**`spec-needs-amendment`** — the contract framing is sound (not `spec-is-wrong-shape`, not `spec-is-tight-but-premature`); specific lines need amendment.

## Material findings — applied to v2

### 1. AC #3 has a verifiable hole (mechanism leak)

The (banner-absent ∧ reload-works) success cell can be reached two ways: (a) genuinely re-establishing the WS connection, or (b) suppressing the banner via CSS / DOM-patch while reload happens to work for unrelated reasons. The AC's prose disclaimer is not enforceable by the observation. INV #5's WS-stays-open check was the saving grace but AC #3 didn't reference it.

**Amendment in v2:** Bind AC #3 explicitly to INV #5 — banner-absent passes iff INV #5's underlying-WS check also passes. The cosmetic-suppression cell (banner-absent ∧ INV #5 fails) is now an explicit AC-failure.

### 2. AC #1 lacked a time bound (unverifiable)

"Within a developer-tolerable interval" admits a 60-second reload as "passing". §6 acknowledged this but the AC had no upper bound.

**Amendment in v2:** Specified **30 seconds** (typical shadow-cljs ballpark per reviewer).

### 3. Missing INV: new host-published ports must be `.env`-overridable

`docs/local-dev-docker.md:33` documents that sibling-IMAS-stack port conflicts are a real concern; existing 3451/8777 ports have `${FRONTEND_*_PORT:-...}` overrides. A Phase 5 mechanism that publishes 9630 without that override would silently regress the multi-stack story.

**Amendment in v2:** Added **INV #6** — any new host-published port follows the `${ENV_VAR:-default}` pattern and is added to `.env.example`.

### 4. AC #2 step (iii) "evaluate a form successfully" — too vague

**Amendment in v2:** Tightened to `(+ 1 1)` → `2` round-trip.

### 5. AC #4 "discoverable place" — vague

**Amendment in v2:** Pinned to "same numbered step list a fresh developer reads to bring the stack up". A 'known limitations' or 'advanced' appendix does not satisfy.

### 6. INV #4 "(a) second browser OR (b) incognito" — verification mechanism leak

The OR-pattern left two materially different verifications (browser-engine vs profile/extensions) as a choice in the invariant.

**Amendment in v2:** Require both — incognito + extensions disabled AND a second browser engine.

### 7. INV #5 "stays open" admits flap-and-reconnect

**Amendment in v2:** Specified **single uninterrupted** WebSocket connection from page-load through successful `.cljs` edit propagation.

### 8. AC #3 follow-up obligation soft

**Amendment in v2:** "raised as a follow-up" → "raised as a follow-up backlog task **before the change is merged**".

### 9. INV #1 silent on `frontend/Dockerfile` EXPOSE list

**Amendment in v2:** Folded `frontend/Dockerfile`'s `EXPOSE` directive into INV #1 — any port newly published to the host is also added to `EXPOSE`.

## Deferred — recorded for possible future Phase 3.1 or Phase 5 handling

| # | Finding | Why deferred |
|---|---|---|
| D1 | New INV: running browser bundle must match source-of-truth shadow-cljs config (stale cache risk in `frontend-shadow-cache` named volume) | Q4 diagnostic in §5 already covers this; promoting to invariant adds complexity for a hypothetical-only risk |
| D2 | New AC: nREPL + hot-reload working *concurrently* in one session | AC #1 + AC #2 step (iii) cover them independently; concurrent test added to Out of Scope with note that Phase 5 should add it if `network_mode: host` or similar is selected |
| D3 | New AC: hot-reload across `docker compose restart frontend` | Verification plan covers this implicitly; explicit AC seemed over-specification |

## Lateral findings

- **L1.** Phase 5 candidate seeds in v1 §3 included `network_mode: host` without flagging Linux's port-map-ignore behaviour. Left for Phase 5 to handle when those candidates are properly enumerated.
- **L2.** shadow-cljs version pin (2.19.9) not explicitly protected against Phase 5 "upgrade-and-replace" candidate. INV #2 covers indirectly via the no-`:release`-impact bar.

## Disposition

9 material amendments applied to v2 (items 1–9 above). 3 deferred items recorded (D1–D3). v2 path: [`phase-3-specific-v2.md`](phase-3-specific-v2.md).
