# Phase 3 — Be Specific

**Slug:** `task-13-shadow-cljs-hotreload`
**Subagent run date:** 2026-05-18
**Inputs received:** Phase 2 v2 ([`phase-2-understand-v2.md`](phase-2-understand-v2.md)) + carried-forward open questions Q1–Q5 from the FSM state board ([`00-fsm-state.md`](00-fsm-state.md)). Originating task ACs consulted for AC text only (not framing); `docs/local-dev-docker.md:25-29` consulted for the documented service contract.

> ⚠️ Phase 3 produces a **contract specification**. Acceptance Criteria are observable outcomes; Invariants are properties that must hold. **Mechanism choices** — anything implementable two materially different ways — are demoted to Open Questions for Phase 5, not lifted into the spec.

## 1. Acceptance Criteria

### AC #1: A `.cljs` source edit propagates to the running host browser without a manual refresh

- **Observable outcome:** With the stack started per `docs/local-dev-docker.md` (`docker compose up -d`) and `http://localhost:3451/index.html` open in a host browser at a known state, a developer modifies a `.cljs` source file under `frontend/src/` (e.g. changes a visible string in a top-level Reagent view or a `re-frame` `reg-event-fx` body), saves the file, and within a developer-tolerable interval the change becomes visible / effective in the already-open browser tab **without the developer manually reloading the page**.
- **Verified by:** Manual / observational run (Phase 5 or later, whichever phase first runs the stack). Procedure: pick a `.cljs` file whose effect is visible in the rendered DOM (e.g. a label string in a sidebar component); record before-state in browser; save edit; record after-state without touching the browser refresh control; pass = after-state reflects the edit, fail = it does not. Save the procedure + observation in the runtime-observation log so subsequent sessions can re-run it.
- **Mechanism-free check:** Two engineers could satisfy this AC by (a) wiring the existing devtools websocket through a different host/port, (b) running shadow-cljs differently, (c) using a different reload pathway entirely. The AC says nothing about *how* — it only constrains the developer-visible outcome. ✓ mechanism-free.

### AC #2: The documented dev workflow in `docs/local-dev-docker.md` continues to work end-to-end after the change

- **Observable outcome:** A developer following `docs/local-dev-docker.md` as written (or with the one-line update permitted by originating-task AC #3 — see AC #4 below) can: (i) bring the stack up via `docker compose up -d`, (ii) open `http://localhost:3451/index.html` and see the app render and become interactive (sidebar tabs respond, welcome modal appears, Leaflet base map loads, Django API calls to `http://localhost:8000` succeed — i.e. the baseline already observed in Phase 2 v2 §1), (iii) connect an nREPL client to `localhost:8777` and evaluate a form successfully.
- **Verified by:** Manual / observational run. Steps (i) and (ii) are already passing per Phase 2 v2 §1; the AC is that they continue to pass after the change. Step (iii) verified by nREPL connect from any standard editor or `lein repl :connect localhost:8777` and evaluation of e.g. `(+ 1 1)`.
- **Mechanism-free check:** Does not commit to any particular network topology, port map, or shadow-cljs configuration — only to the developer-visible URLs/ports already documented. ✓ mechanism-free.

### AC #3: The "Reconnecting …" banner state is resolved consistent with the underlying connection state

- **Observable outcome:** On a fresh load of `http://localhost:3451/index.html` (cache cleared, no browser extensions interfering — see INV #4), one of the following is true:
  - (a) The red `shadow-cljs — Reconnecting …` pill is not present, **and** AC #1 holds; or
  - (b) The pill is present **and** AC #1 demonstrably also fails (i.e. saving a `.cljs` does *not* propagate without manual refresh).
- **Verified by:** Manual / observational run, paired with the AC #1 procedure: the AC #1 observation also records pill-state, and the pair must be consistent — (banner-absent ∧ reload-works) or (banner-present ∧ reload-broken). The pair (banner-present ∧ reload-works) is a *separate cosmetic surprise* and **fails this AC** even though AC #1 would pass; that scenario must be raised as a follow-up.
- **Mechanism-free check:** The AC binds the banner to the *underlying* connection contract rather than treating the banner as an end in itself. It does not say "remove the banner" (a mechanism — could be done by CSS hack). It says "banner and underlying state must agree", which is satisfiable by any implementation that genuinely resolves the underlying state. ✓ mechanism-free.

### AC #4: Any host-side step the developer must perform is documented in `docs/local-dev-docker.md`

- **Observable outcome:** If the eventual fix requires the developer to take any action *outside* `docker compose up -d` (e.g. run a one-time host command, install a host-side tool, set an env var, modify their `/etc/hosts`, start a proxy in a separate terminal), that action is documented in `docs/local-dev-docker.md` in a discoverable place (preserves originating-task AC #3). If the fix is fully self-contained inside the Compose stack and `frontend/`, no doc change is required and this AC is trivially satisfied.
- **Verified by:** Source review at PR time; cross-check that a fresh developer following only `docs/local-dev-docker.md` can reach the AC #1 outcome.
- **Mechanism-free check:** Conditional and outcome-shaped — it does not assume a particular fix shape. ✓ mechanism-free.

## 2. Invariants

### INV #1: The user-facing service contract in `docs/local-dev-docker.md` is preserved

- **Statement:** The developer continues to reach the frontend at `http://localhost:3451` and the shadow-cljs nREPL at `localhost:8777`. The set of *documented* host-visible endpoints is not narrowed (it may be widened by the fix — see Phase 5 mechanism seeds — but a widening must be reflected in AC #4's doc update).
- **Why load-bearing:** This URL/port pair is the documented contract (Phase 2 v2 §3 first **(C)** bullet; `docs/local-dev-docker.md:25-29`). Other developers, README links, bookmarks, and possibly tooling (editors, scripts) depend on it. Silently breaking it is worse than the symptom being fixed.
- **Verification approach:** Diff `docs/local-dev-docker.md` and `docker-compose.yml` at PR time; any change to the published port map for `frontend` is allowed only if `:3451` and `:8777` both remain accessible and the doc is updated to match.

### INV #2: No production-build artefact is changed by the dev-only fix

- **Statement:** The shadow-cljs **`:release` / production** build configuration and outputs are unaffected by changes made to satisfy this contract. Changes are confined to dev-build wiring, dev-image config, Compose config, and dev docs.
- **Why load-bearing:** A "fix" that happens to leak dev-only websocket targets, debug flags, or unintended `goog.define` values into a production bundle would be a quietly worse outcome than the original symptom. The repo ships a single shadow-cljs config file; per-build isolation is the only thing preventing dev changes from contaminating release.
- **Verification approach:** Inspect any diff to `frontend/shadow-cljs.edn` and confirm changes are scoped to the `:dev` build or to top-level keys that don't apply to `:release`. If `docker-compose.yml` changes, confirm production deployment paths do not depend on the modified service definitions. Optionally: compile a `:release` build before and after and diff the resulting bundles (smoke test, not gating).

### INV #3: The fix does not require the developer to disable browser security features

- **Statement:** Satisfying AC #1 does not require the developer to disable Same-Origin Policy, mixed-content blocking, certificate validation, or any browser security feature; nor does it require launching the browser with non-standard flags.
- **Why load-bearing:** A "works if you launch Chrome with `--disable-web-security`" workaround is technically a fix to AC #1 but is unacceptable as a documented dev workflow.
- **Verification approach:** AC #4's doc step (if any) reviewed against this invariant at PR time.

### INV #4: The contract is verified against a clean browser environment

- **Statement:** AC #1, #2, #3 observations are performed in a browser session free of confounders: cache cleared (or incognito/private), no relevant extensions interfering, no stale service worker. This is a **verification** invariant — it constrains the test, not the implementation.
- **Why load-bearing:** Phase 2 v2 §4 newly-raised Q3 (cross-browser reproduction) exists because the symptom has only been observed in one browser session. A "fix" that works under one set of confounders but not another is not a fix.
- **Verification approach:** AC #1 observation is repeated in either (a) a second browser, or (b) the same browser in incognito/private mode with extensions disabled. Both passes constitute the AC #1 success criterion. The second observation is also the natural moment to fold in carry-forward Q5.

### INV #5: The shadow-cljs devtools client's *intended* WS target is reachable from the host browser at runtime

- **Statement:** Whatever host:port the compiled `:dev` bundle's shadow-cljs devtools client attempts to open a websocket to from the host browser at runtime, that target is in fact reachable from the host browser. This is the underlying connection contract that AC #3's banner state proxies.
- **Why load-bearing:** This is the *neutral* statement of the failure mode Phase 2 v2 §1 documents (compiled `app.js` advertises `ws://localhost:9630/...`, port 9630 is not in the Compose host port map). Stating it as an invariant rather than an AC keeps the spec mechanism-free: any candidate that satisfies the invariant — by changing what the client requests, by changing what is reachable, or by changing both — is admissible.
- **Verification approach:** Open the host browser's DevTools → Network → WS panel on a fresh load. Confirm at least one websocket request from the page resolves to a 101 Switching Protocols and stays open (i.e. is not in a connecting/closed/reconnecting state) **for the duration in which a `.cljs` edit successfully hot-reloads (AC #1)**. The "stays open during a successful edit" pairing is what binds this invariant to the contract; a transient connect that closes before reload would not satisfy it.

## 3. Carried Forward / Escalated Questions

### Carried forward from Phase 2 v2

**Q1 — WS URL the browser attempts to connect to:**
- **Answered in Phase 2 v2 (and restated here as a known starting fact):** `ws://localhost:9630/<shadow-cljs-ws-path>`. Source: compiled `frontend/resources/public/js/app.js` CLOSURE_DEFINES (`server_host="localhost"`, `server_port=9630`, `use_document_host=true`). Port 9630 is not in the Compose host port map (`docker-compose.yml:91-93`) and not in `frontend/Dockerfile`'s `EXPOSE` list (`frontend/Dockerfile:23`). No further action required in Phase 3.

**Q2 — GATING: Does a `.cljs` edit propagate to the browser without manual refresh while the banner is showing?**
- **Folded into the spec as a verification condition (AC #1 + AC #3 pairing).** Phase 3 explicitly does *not* answer Q2 — answering it requires running the stack, which is out of scope for this phase. Instead, the spec captures both arms of the falsifying scenario from Phase 2 v2 §4:
  - **Arm A (banner present, reload works):** AC #3 fails on the "(banner-present ∧ reload-works)" combination; the surprise is recorded as a follow-up; the load-bearing AC #1 already passes; remaining work is cosmetic. Scope collapses.
  - **Arm B (banner present, reload broken):** AC #1 fails outright. Phase 5 mechanism enumeration is justified. The spec is the success criterion the candidates compete against.
  Either way, the first phase that runs the stack records the pair (banner-state, reload-works) and the spec is unambiguous about which arm has occurred.

**Q3 — ~88 KB of browser console output from the page load:**
- **Folded into the verification plan as a diagnostic to collect (not gating).** When AC #1 is observed for the first time, the browser console is also captured to disk (or at minimum, scanned for `WebSocket`, `shadow-cljs`, `1006`, `refused`, `timeout`, `TLS`/`SSL` strings) and the result is appended to the runtime-observation log. The output is diagnostic — it sharpens any Phase 5 candidate selection in Arm B — but it is not itself part of any AC or INV. If it surfaces a surprise, that surprise is raised as a newly-raised question and re-enters the FSM.

**Q4 — `frontend-shadow-cache` named volume — could a stale entry contribute to the symptom?**
- **Out of scope as a contract concern; folded into the verification plan as one diagnostic step.** Rationale: the spec is about the running system's contract, not about cache hygiene. However, *before* concluding from Arm B that the contract is genuinely broken, the AC #1 observation should be repeated once after `docker compose down -v` (drops the named volume) and `docker compose up -d` (rebuilds it). If a single `down -v` round-trip flips Arm B → Arm A, the symptom is volume-state-dependent and a different (much smaller) class of fix is warranted; this is recorded but the contract specification itself does not change.

**Q5 — Cross-browser reproduction:**
- **Folded into the spec as INV #4 (verification-side invariant).** The contract is verified against a clean browser environment with at least one cross-browser or incognito check, which structurally answers Q5 as part of normal verification. No separate question remains.

### Mechanism choices demoted to Phase 5

- **Mechanism candidate:** Set `:devtools-url` (or `:proxy-url`) in `frontend/shadow-cljs.edn` to redirect the WS target.
  - **Demoted because:** Implementable as `:devtools-url "http://localhost:9630"`, *or* as a path-only `:devtools-url "/shadow-cljs"` behind a reverse proxy, *or* not at all if a different mechanism is chosen. INV #5 ("WS target is reachable from host browser") admits all three.
  - **Phase 5 candidate seeds:** (a) Set `:devtools-url` to point at a host-mapped port; (b) Set `:proxy-url`; (c) Don't touch shadow-cljs.edn and change the network shape instead.

- **Mechanism candidate:** Publish port 9630 in `docker-compose.yml` and/or `frontend/Dockerfile`.
  - **Demoted because:** Implementable by adding `9630:9630` to the Compose port map and `EXPOSE 9630` to the Dockerfile, *or* by switching the frontend service to `network_mode: host` (macOS Docker-Desktop caveats apply), *or* by introducing a reverse proxy on an already-mapped port (e.g. 3451) that fronts the shadow-cljs WS. INV #5 admits all of these.
  - **Phase 5 candidate seeds:** (a) Add 9630 to Compose host port map; (b) Use `network_mode: host` for the frontend service; (c) Add a reverse proxy (Caddy/Nginx/Traefik) inside the Compose network that fronts 9630 behind `localhost:3451`.

- **Mechanism candidate:** Change the shadow-cljs build to bind the dev server to a different interface or port.
  - **Demoted because:** Multiple shadow-cljs config knobs and multiple in-container processes could satisfy "the dev server is reachable from the host"; INV #5 is silent on which one.
  - **Phase 5 candidate seeds:** (a) Per-build `:dev-http-port` adjustments; (b) override `server_port` via `:devtools` keys; (c) leave shadow-cljs alone and adjust at the network layer instead.

- **Mechanism candidate:** Pin / upgrade / downgrade shadow-cljs from 2.19.9.
  - **Demoted because:** A behavioural difference between shadow-cljs versions is a possible (not confirmed) contributor; whether a version change is the right lever depends on what Phase 4 (Prior Art) and Phase 5 enumerate. INV #2 (no production-build impact) constrains but does not select.
  - **Phase 5 candidate seeds:** (a) Stay on 2.19.9; (b) Upgrade to current `2.x`; (c) Downgrade if a known-good local-Docker version exists in upstream issue history.

## 4. Out of Scope

- **Host-native (non-Docker) shadow-cljs workflow.** Phase 2 v2 §3 **(P)** marks this as out of scope; the originating task targets the documented Docker workflow.
- **Production / `:release` build behaviour.** Constrained by INV #2 but not the subject of any AC; release-build smoke testing is a verification side-effect, not a contract goal.
- **Backend Django, MSSQL, GeoServer, or any other Compose service.** Phase 2 v2 §1 confirms these are working baseline.
- **The shadow-cljs *dashboard* / inspect / REPL-via-browser features that live on port 9630.** Restoring developer access to those is plausibly a side-benefit of any mechanism that satisfies INV #5, but is not part of this contract; surfacing it would widen scope beyond the originating task ACs.
- **`backlog/` tooling, schema-capture flow, and other unrelated repo concerns.**
- **Performance / latency of hot reload.** "Within a developer-tolerable interval" in AC #1 is intentionally fuzzy; tightening it (e.g. "under 2 s") would be a separate contract.
- **CI / production deploy plumbing.** This is a local-dev-only contract.

## 5. Verification Plan (Pre-Test Strategy)

For each AC and INV, distinguish **automated** vs **manual / observational** verification.

### AC #1 — `.cljs` edit propagates without manual refresh

- **Automated:** None planned. ClojureScript hot-reload-in-browser is not currently exercised by repo automated tests, and creating an end-to-end browser-driving harness for this single AC is disproportionate.
- **Manual / observational:** Procedure described under AC #1. Performed first under INV #4 conditions (clean browser, no extensions), then repeated in a second browser / incognito to satisfy INV #4's cross-confounder check. Outcome and pill-state both recorded in the runtime-observation log.

### AC #2 — Documented workflow still works

- **Automated:** None. (Possible future smoke-test: a Compose-aware health check that hits `:3451`, `:8000`, `:8777` and asserts they answer, but explicitly out of scope here.)
- **Manual / observational:** Walk through `docs/local-dev-docker.md` steps 1–N from a clean clone (or `docker compose down -v && docker compose up -d`). Confirm baseline (app renders, API works, nREPL connects). Verified once before any change (to capture the pre-state, since Phase 2 v2 §1 already documents this is currently passing) and once after.

### AC #3 — Banner state agrees with underlying state

- **Automated:** None.
- **Manual / observational:** Paired with AC #1's observation. Pill-state recorded, reload-state recorded, the pair checked against the four-cell truth table; only (banner-absent ∧ reload-works) and (banner-present ∧ reload-broken) pass.

### AC #4 — Host-side steps documented

- **Automated:** None.
- **Manual / observational:** Source review at PR time; "fresh developer" pass-through of `docs/local-dev-docker.md` to confirm reachability of AC #1.

### INV #1 — Service contract preserved

- **Automated:** Possible: a unit-test-style check that greps `docs/local-dev-docker.md:25-29` for the `:3451` and `:8777` strings and the `docker-compose.yml` port-map block for the same; cheap. Not gating.
- **Manual / observational:** Diff review at PR time.

### INV #2 — No production-build impact

- **Automated:** Optional smoke test — `shadow-cljs release :app` (or whichever the project's `:release` build alias is) before and after, with a byte-size delta and a `goog.define` scan of the resulting bundle. Not gating.
- **Manual / observational:** EDN diff review; ensure changes scoped to the `:dev` build or to keys that don't apply to `:release`.

### INV #3 — No browser-security disable

- **Automated:** None.
- **Manual / observational:** Doc review at PR time.

### INV #4 — Clean browser verification

- **Automated:** None.
- **Manual / observational:** Built into AC #1's procedure (incognito + at least one second browser).

### INV #5 — WS target reachable

- **Manual / observational:** Open DevTools → Network → WS during AC #1's procedure. Record the WS URL the page opens, its status code, its duration, and whether it remains open across the `.cljs` save event. Captured to the runtime-observation log alongside the AC #1 evidence.
- **Diagnostic capture (folded-in Q3):** Browser console output (~88 KB observed in earlier session) captured alongside, scanned for known failure-mode tokens (`WebSocket`, `1006`, `refused`, `timeout`, `ERR_*`).

### Carry-forward Q4 diagnostic

Before drawing a conclusion from Arm B (banner-present ∧ reload-broken), repeat AC #1's observation once after `docker compose down -v && docker compose up -d`. If the result flips to Arm A, log "volume-state-dependent" and the Phase 5 candidate enumeration narrows accordingly.

## 6. Flagged Inaccuracies / Drift From Phase 2 (if any)

None. Phase 3 did not re-verify Phase 2 v2's code-artefact claims; it took them as given and built the contract on top of them. If Phase 5 or later runtime observation contradicts any Phase 2 v2 fact, that contradiction is raised then.
