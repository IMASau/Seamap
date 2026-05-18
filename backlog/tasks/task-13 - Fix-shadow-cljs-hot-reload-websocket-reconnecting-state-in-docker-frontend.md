---
id: TASK-13
title: Fix shadow-cljs hot-reload websocket reconnecting state in docker frontend
status: In Progress
assignee:
  - '@claude'
created_date: '2026-05-18 04:12'
updated_date: '2026-05-18 05:55'
labels:
  - docker
  - frontend
  - dev-stack
dependencies: []
priority: medium
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When the frontend runs inside docker (`yarn run watch` via docker compose), the browser shows a persistent red 'shadow-cljs — Reconnecting …' banner and hot reload never recovers from the initial connection. Likely cause: shadow-cljs advertises a websocket URL/host that resolves correctly inside the container but not from the host browser. Probable fix is an explicit `:devtools {:devtools-url ...}` or `:dev-http {:proxy-url ...}` setting in `frontend/shadow-cljs.edn`, or exposing the shadow-cljs HTTP/admin port (9630) and pointing the browser there. Cosmetic only — initial CLJS build serves fine, just no hot reload.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Red 'Reconnecting …' banner does not appear on a fresh load of http://localhost:3451/index.html
- [ ] #2 Editing a .cljs source file under frontend/src/ triggers a hot reload in the browser without a manual page refresh
- [ ] #3 Fix is documented (one-line note in docs/local-dev-docker.md if any host-side step is required)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Describe (factual state, no causes/fixes yet)

**Symptom (browser)**
- On every load of http://localhost:3451/index.html, a red pill `shadow-cljs — Reconnecting …` appears pinned to the lower-left of the viewport and stays.
- The CLJS app itself renders and is interactive (sidebar tabs, welcome modal, Leaflet base map; Django API calls succeed).
- Symptom persists across hard reload (cache cleared) and across `docker compose up -d --force-recreate frontend`.

**Stack configuration (frontend container)**
- Image: `node:16-bookworm-slim` + OpenJDK 17, `yarn run watch` → `npx shadow-cljs watch dev`.
- shadow-cljs version: 2.19.9.
- Container hostname inside the compose network: `seamap-frontend-1`.

**`frontend/shadow-cljs.edn` — what's set**
- `:nrepl {:port 8777}`
- `:dev-http {3451 "resources/public"}` — serves static assets only.
- `:builds :dev :devtools {:preloads [devtools.preload imas-seamap.specs.preload]}`
- Not set: `:devtools-url`, `:devtools :hud`, `:devtools :proxy-url`, websocket host/port, `:reload-strategy`, `:open-file-command`.

**Container log on startup (verbatim, relevant lines)**
```
shadow-cljs - HTTP server available at http://localhost:3451
shadow-cljs - server version: 2.19.9 running at http://localhost:9630
shadow-cljs - nREPL server started on port 8777
shadow-cljs - watching build :dev
[:dev] Build completed. (773 files, 772 compiled, 0 warnings, 15.40s)
```
No errors or warnings around the websocket or devtools.

**Host port mappings (`docker compose port frontend …`)**
- `3451 → 0.0.0.0:3451` (shadow-cljs `:dev-http` static server)
- `8777 → 0.0.0.0:8777` (nREPL)
- `9630` — not mapped to the host. This is shadow-cljs's own admin/devtools HTTP+websocket server.

**Known but unchecked at describe time**
- Whether the browser is actually attempting a websocket connection and to which URL (haven't opened devtools Network → WS view yet).
- Whether file edits to a `.cljs` source under `frontend/src/cljs/` would otherwise hot-reload — i.e. whether the 'Reconnecting' banner is the only symptom or whether reload itself is also broken. Haven't tried editing a file.
- The 88 KB of console output from the page load (truncated by the tool) — may contain more specific connection errors.

## FSM Phase 2 — Understand / State (complete, 2026-05-18)

Research dossier: `backlog/research/task-13-shadow-cljs-hotreload/` — see `00-fsm-state.md` for the running state board.

Phase 2 cycle complete (v1 + Round 1 adversarial review + v2 merge). Key load-bearing finding (caught by the adversarial reviewer, not Phase 2 v1):

- The compiled `frontend/resources/public/js/app.js` contains hard-coded shadow-cljs devtools-client `CLOSURE_DEFINES`: `server_host="localhost"`, `server_port=9630`, `use_document_host=true`, `use_document_protocol=false`, `devtools_url=""`.
- Effective WS target from the host browser is therefore `ws://localhost:9630/<path>`. Compose publishes only ports 3451 and 8777 → port 9630 is unreachable from the host → the "Reconnecting" pill is the visible signal of that unreachability.

Gating runtime observation, escalated to Phase 3 (must precede solution enumeration):

- Does editing a `.cljs` under `frontend/src/` propagate to the browser without manual refresh, while the banner is showing? Falsifying scenario: if it propagates, scope collapses to "cosmetic pill only".

Status: awaiting human "go" before dispatching Phase 3 (Be Specific) and its Template 2 (Spec Review) adversarial pass.

## Fix applied (2026-05-18)

After a Google search confirmed the standard shadow-cljs-in-Docker fix is well-known, the FSM workflow was short-circuited at end of the Phase 3 cycle. Minimal-mechanism fix applied directly:

- **`docker-compose.yml`** — added `${FRONTEND_SHADOW_PORT:-9630}:9630` to the `frontend` service `ports` block, matching the existing env-override pattern for ports 3451 and 8777.
- **`frontend/Dockerfile`** — `EXPOSE 3451 8777` → `EXPOSE 3451 8777 9630`.
- **`.env.example`** — added `FRONTEND_SHADOW_PORT=9630` next to the other host-port vars.
- **`docs/local-dev-docker.md`** — added a row to the URL table for `http://localhost:9630` with a one-line explanation that the hot-reload websocket lives there.

No `:devtools-url` change in `frontend/shadow-cljs.edn`: the compiled `app.js` already has `use_document_host=true`, so the runtime resolves the host from the page (`localhost`); only port 9630 needed to be made reachable.

Why a minimal fix rather than Phase-5 candidate enumeration: per the search, this is the canonical pattern (shadow-cljs UsersGuide; multiple community references; reference impl at github.com/suud/cljs-docker). The FSM ceremony's value-add on this specific problem was marginal once the pattern was confirmed standard.

Runtime verification (gating, operator-side): with the change applied, run `docker compose up -d --force-recreate frontend`, open `http://localhost:3451/index.html` in a fresh browser, confirm: (a) the red 'Reconnecting …' banner does not appear, and (b) editing a `.cljs` source file under `frontend/src/` triggers a hot reload in the browser without a manual refresh. If either fails, re-enter the FSM at Phase 3 with the v1 review's deferred items (D1–D3) on the table.

FSM dossier: `backlog/research/task-13-shadow-cljs-hotreload/`.
<!-- SECTION:NOTES:END -->
