# FSM State — task-13 shadow-cljs hot reload in Docker

**Slug:** `task-13-shadow-cljs-hotreload`
**Originating task:** [task-13](../../tasks/task-13%20-%20Fix-shadow-cljs-hot-reload-websocket-reconnecting-state-in-docker-frontend.md)
**Started:** 2026-05-18
**Orchestrator:** Claude (Opus 4.7)

## Status Line

**Current phase:** **PARKED — workflow short-circuited at end of Phase 3 cycle.** Google search (2026-05-18) confirmed the fix pattern is well-known and well-documented in the shadow-cljs community. Per operator decision (option 1 of the post-search choices), the remaining FSM ceremony was skipped and the minimal fix was applied directly. Implementation: publish port 9630 in `docker-compose.yml` (with `${FRONTEND_SHADOW_PORT:-9630}` env override, matching the existing 3451/8777 pattern); add `9630` to `frontend/Dockerfile`'s `EXPOSE`; document the port in `docs/local-dev-docker.md` and `.env.example`. Runtime verification is still required by the operator.

## Phase Table

| # | Phase | Status | Artefact | Review |
|---|---|---|---|---|
| 1 | Anchoring Filter | ✅ Done | (in-conversation scratchpad) | n/a (pre-phase) |
| 2 | Understand / State | ✅ Done | [v1](phase-2-understand.md) · [v2](phase-2-understand-v2.md) | [Round 1](phase-2-review-round-1.md) — *Accept with material amendments* |
| 3 | Be Specific | ◐ Partial — v1 + Round 1 review only; v2 deliberately not merged | [v1](phase-3-specific.md) | [Round 1](phase-3-review-round-1.md) — *spec-needs-amendment* (9 findings recorded, not applied) |
| 4 | Prior Art (parallel) | 🚫 Skipped | — | — |
| 5/6 | Consider & Analyse | 🚫 Skipped | — | — |
| 7 | Decide (ADR) | 🚫 Skipped | — | — |
| 8 | Implement | ✅ Applied directly (outside FSM) | `docker-compose.yml`, `frontend/Dockerfile`, `.env.example`, `docs/local-dev-docker.md` | — |
| 9 | Verify / Close | ⏳ Operator runtime check | — | — |

Status legend: ⏳ awaiting go · ◒ in flight · ✅ done · ⚠️ blocked.

## Restated Contract (post-Anchoring)

**Real outcome wanted (per user):** Hot reload works for local Docker dev — editing a `.cljs` under `frontend/src/` updates the running browser without manual refresh.

**Symptom currently observed:** Red `shadow-cljs — Reconnecting …` banner pinned on `http://localhost:3451/index.html`. Banner is a *visible proxy* for the WS-disconnect state; whether hot reload itself works is **not yet runtime-verified** (Phase 3 gating observation).

## Facts (Column A, distilled)

- Browser on host loads `http://localhost:3451/index.html` → app renders, API calls succeed, red `Reconnecting …` banner persists.
- Symptom persists across hard reload and `docker compose up -d --force-recreate frontend`.
- Frontend container: `node:16-bookworm-slim` + OpenJDK 17; runs `yarn run watch` → `npx shadow-cljs watch dev`.
- shadow-cljs version: 2.19.9 (pinned in `frontend/package.json:36`).
- Container hostname: `seamap-frontend-1`.
- `frontend/shadow-cljs.edn`: `:nrepl {:port 8777}`, `:dev-http {3451 "resources/public"}`, `:builds :dev :devtools {:preloads [...]}`. Not set: `:devtools-url`, `:devtools :hud`, `:devtools :proxy-url`, ws host/port, `:reload-strategy`, `:open-file-command`, `:hooks`, `:build-hooks`, `:nrepl-middleware`.
- Container startup log: HTTP at `:3451`, server `:9630`, nREPL `:8777`, build completed 773 files / 0 warnings.
- Compose host port mappings: `3451→3451`, `8777→8777`. Port `9630` is NOT mapped to host.
- **Compiled `app.js` CLOSURE_DEFINES (orchestrator-verified):** `server_host="localhost"`, `server_port=9630`, `use_document_host=true`, `use_document_protocol=false`, `devtools_url=""`.
- ACs as written: (1) banner absent on fresh load; (2) `.cljs` edit triggers hot reload without manual refresh; (3) fix documented if host-side step required.

## Open Questions Carried Into Phase 3

1. **✅ Answered (Round 1 review finding #1):** WS URL is `ws://localhost:9630/...` per on-disk `app.js` `CLOSURE_DEFINES` (`server_host="localhost"`, `server_port=9630`, `use_document_host=true`). Port 9630 unmapped in Compose → unreachable from host browser.
2. **⚠️ GATING — escalated to Phase 3:** Does editing a `.cljs` file under `frontend/src/` propagate to the browser without manual refresh, while the banner is showing? Falsifying scenario: if it propagates, scope collapses to "cosmetic pill only" and Phase 5 enumeration is unnecessary.
3. **Escalated to Phase 3:** Contents of the ~88 KB browser console output (less load-bearing now that Q1 is answered; may corroborate failure mode).
4. **Escalated to Phase 3:** Does the symptom survive `docker compose down -v` (which drops the `frontend-shadow-cache` named volume)?
5. **Escalated to Phase 3:** Is the symptom reproducible from a second host browser / incognito?

**Resolved during Anchoring Filter:** Real contract is **functional hot reload in Docker dev**, not banner-cosmetics. AC #1 (banner) is a symptom indicator; AC #2 (functional reload) is the load-bearing outcome.

## Claims Stripped (Column B)

The following originating-brief claims were excluded from subsequent phase briefs to prevent anchoring:

- "Likely cause: shadow-cljs advertises a websocket URL/host that resolves correctly inside the container but not from the host browser." (assumed mechanism — partially borne out *post hoc* by the `app.js` finding, but correctly not preloaded into Phase 2)
- "Probable fix is `:devtools-url`, `:proxy-url`, or exposing port 9630." (pre-selected solutions — to be enumerated as candidates in Phase 5, alongside others)
- "Cosmetic only" framing. (scope claim, contradicted by user's resolution of Q4 in the Anchoring Filter)
- Implicit "small fix / single config tweak" framing.

## Decisions Made

- **2026-05-18 (Phase 2 + Review Round 1):** The shadow-cljs 2.19.9 devtools-client WS target is fully determined by code-artifact evidence in the compiled `app.js`: `ws://localhost:9630/<path>` from the host browser, where port 9630 is not in the Compose host-port map. Phase 3's investigative load is reduced — the **where is it going?** question is answered, and only the **does the contract actually fail?** question (Q2, gating runtime observation) remains.

## Escalations / Blockers

- (None.)

## Cost Ledger

| Phase | Tokens (est.) | Notes |
|---|---|---|
| Anchoring Filter | ~5k (orchestrator only, no subagent) | In-conversation; no Agent dispatch. |
| Phase 2 (Understand) — v1 | ~57k (general-purpose / Sonnet subagent) | Wrote v1; no inaccuracies flagged in Column A; escalated all 3 runtime Qs. |
| Phase 2 Adversarial Review (Round 1) | ~59k (general-purpose / Sonnet subagent) | Verdict: *Accept with material amendments*. 4 material findings; CLOSURE_DEFINES finding load-bearing. |
| Phase 2 v2 merge | ~3k (orchestrator) | All 4 material amendments applied; gating Q2 elevated. |

## Session Rules of Engagement

- No phase advances without explicit "go" from the human operator.
- Adversarial review fires after every phase before the state board is updated.
- Subagents receive Column A + carried-forward questions only; never the originating brief verbatim.
- Material feedback from adversarial review is merged into a `<artefact>-v2.md` file alongside the original.
- Mechanism choices (anything implementable two materially different ways) are demoted to Open Questions for Phase 5, not lifted into the spec.
- From Phase 3 onward, adversarial-review briefs follow the canonical templates in `backlog/research/_fsm-prompts/adversarial-review-prompt-templates.md` (Templates 2–10) rather than ad-hoc orchestrator phrasing.
- **Reviewer-model assignment policy (Oliver, 2026-05-18):** Round 1 adversarial reviews use a **Claude `general-purpose` subagent** (default Sonnet). The strongest available external model (Gemini Pro / GPT-5.5 via the `llm-prompt` skill) is reserved for **Round 2 sign-off (Template 8)** and **Step-back / Trajectory Rethink (Template 9)** — i.e., the passes where independent-model lens is the load-bearing benefit. Record reviewer + model in the Cost Ledger.
