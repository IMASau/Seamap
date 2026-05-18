# Phase 2 — Understand / State

**Slug:** `task-13-shadow-cljs-hotreload`
**Subagent run date:** 2026-05-18
**Inputs received:** Column A facts (sanitized) from the Anchoring Filter; three carried-forward open questions; the resolved "real outcome wanted" (functional hot reload, not banner cosmetics).

> ⚠️ Phase 2 produces a **neutral problem statement**. No solutions, mechanisms, or fixes. If a sentence answers "how", it belongs in Phase 3 or later, not here.

## 1. Current State

What is factually happening right now, verified against the codebase. No causes, no mechanisms, no fixes.

- A developer running the local Docker stack (`docker compose up -d`) and opening `http://localhost:3451/index.html` in a host-browser sees the ClojureScript app render and become interactive: sidebar tabs respond, the welcome modal appears, the Leaflet base map loads, and calls to the Django API (`http://localhost:8000`) succeed.

- A red pill reading **"shadow-cljs — Reconnecting …"** is pinned to the lower-left of the viewport and remains pinned. The pill is present after a hard reload (cache cleared) and after `docker compose up -d --force-recreate frontend`.

- The frontend service is defined in `docker-compose.yml:87-98`:
  - Image built from `./frontend` (`docker-compose.yml:88-89`).
  - Host port map: `${FRONTEND_HOST_PORT:-3451}:3451` and `${FRONTEND_NREPL_PORT:-8777}:8777` (`docker-compose.yml:91-93`). **No mapping for port 9630.**
  - Volumes: bind-mount of `./frontend → /app`, plus named volumes for `node_modules` and `.shadow-cljs` cache (`docker-compose.yml:94-97`).
  - Foreground command: `yarn run watch` (`docker-compose.yml:98`).

- `frontend/Dockerfile` builds on `node:16-bookworm-slim` and adds `openjdk-17-jre-headless` (`frontend/Dockerfile:4,8-9`). It declares `EXPOSE 3451 8777` (`frontend/Dockerfile:23`) — no `EXPOSE 9630`.

- `frontend/docker-entrypoint.sh` runs `yarn install --frozen-lockfile` on first start when `/app/node_modules` is empty, then (for `yarn run watch`) launches `yarn run build-js` once, backgrounds `yarn run watch-js` (babel `src/ui → src/gen`), runs the CSS bundle builds, and finally `exec "$@"` (i.e. `yarn run watch`).

- `frontend/package.json` defines `"watch": "npx shadow-cljs watch dev"` — so `yarn run watch` is the shadow-cljs watch process for the `:dev` build.

- `frontend/shadow-cljs.edn` at the project level configures:
  - `:nrepl {:port 8777}` (`frontend/shadow-cljs.edn:18`).
  - `:dev-http {3451 "resources/public"}` (`frontend/shadow-cljs.edn:19`) — a static-asset HTTP server on port 3451 rooted at `resources/public`.
  - `:builds :dev :devtools {:preloads [devtools.preload imas-seamap.specs.preload]}` (`frontend/shadow-cljs.edn:32-35`).
  - The `:dev` build target is `:browser` with `goog.DEBUG true` and `re-frame trace_enabled` true (`frontend/shadow-cljs.edn:21-24`).

- `frontend/shadow-cljs.edn` does **not** set any of: `:devtools-url`, `:devtools :hud`, `:devtools :proxy-url`, websocket host / port keys, `:reload-strategy`, or `:open-file-command`.

- shadow-cljs version in use: **2.19.9** (per the originating brief; not pinned in EDN, would come from `package.json`/`shadow-cljs` install — not separately re-verified here).

- Container startup log (verbatim, from the originating brief):
  - `shadow-cljs - HTTP server available at http://localhost:3451`
  - `shadow-cljs - server version: 2.19.9 running at http://localhost:9630`
  - `shadow-cljs - nREPL server started on port 8777`
  - `shadow-cljs - watching build :dev`
  - `[:dev] Build completed. (773 files, 772 compiled, 0 warnings, 15.40s)`
  - No errors or warnings logged around the websocket or the devtools subsystem.

- Compose-network hostname of the frontend container: `seamap-frontend-1` (per the originating brief; consistent with Compose's default `<project>-<service>-<index>` naming).

- `docs/local-dev-docker.md:25-35` documents the user-visible contract for the stack: "Frontend (shadow-cljs) → http://localhost:3451" and "shadow-cljs nREPL → localhost:8777". Port 9630 is not mentioned anywhere in that doc, nor is the shadow-cljs dashboard / devtools subsystem.

- Two runtime observations relevant to the contract are **not yet captured**: (a) the actual URL the browser tries to open the devtools websocket on, and (b) whether saving a `.cljs` file under `frontend/src/` propagates to the running browser without a manual refresh while the red pill is showing. See §4.

## 2. The Problem

**Real outcome wanted (load-bearing contract):** A developer using the documented local Docker workflow (`docker compose up -d`, then `http://localhost:3451/index.html` in a host browser) can edit a `.cljs` source file under `frontend/src/` and see the change in the running browser **without manually refreshing the page**. This is the everyday ClojureScript dev loop the stack is meant to support, per `docs/local-dev-docker.md:25-35` and the "shadow-cljs watch" framing throughout the dev docs.

**Symptom currently observed (proxy indicator, not the contract):** The shadow-cljs devtools client running in the browser advertises, via the persistent red "Reconnecting …" pill, that its connection back to the shadow-cljs server is not established. The pill is the visible UI affordance shadow-cljs uses for this state.

**Why the distinction matters here:** The originating brief used the pill's absence as the primary acceptance criterion (AC #1). The Anchoring Filter resolved that **AC #2** — a `.cljs` edit actually round-tripping into the browser — is the contract; AC #1 is a symptom indicator that may correlate with the contract but is not itself the contract. A "fix" that hides the pill while leaving hot reload broken would not satisfy the real outcome; a "fix" that restores hot reload while leaving a cosmetic banner would satisfy it (though the banner's persistence in that scenario would be a separate, lower-priority surprise).

**The contract failure, stated neutrally:** The shadow-cljs devtools client in the browser is in a state that it itself labels "Reconnecting", and it is not currently established whether the underlying capability (a `.cljs` edit propagating to the browser without manual refresh) does or does not work in this state. Either way, the developer-facing signal is that the dev loop is broken or degraded, contrary to the documented workflow.

## 3. Constraints

Hard facts about the environment. Marked **(C)** constraint or **(P)** preference.

- **(C)** The browser is on the developer's host (macOS, per the broader docs); the shadow-cljs server is inside a Compose-network container. Cross-boundary network access from host browser → container is mediated by the published port maps in `docker-compose.yml:91-93`. This is a Docker-Desktop / Compose constraint, not a project preference.

- **(C)** shadow-cljs's devtools client is a piece of JavaScript that the shadow-cljs compiler injects into the browser bundle for the `:dev` build. Its connection target (host/port/protocol) is determined by shadow-cljs's own conventions and the build's EDN configuration — the project does not get to redefine the protocol shape.

- **(C)** `docs/local-dev-docker.md` documents `http://localhost:3451` as the frontend entry point and `localhost:8777` as the nREPL endpoint. Any change to that user-facing contract has documentation cost.

- **(C)** The frontend image is `node:16-bookworm-slim` + OpenJDK 17; the comment in `frontend/Dockerfile:1-3` and `docs/local-dev-docker.md:184-186` explains the Node 16 pin (node-sass 7.x prebuilt binary availability). The Node version is therefore not freely movable as part of this work.

- **(P)** "We run dev in Docker via compose." The repository now ships both a `docker-compose.yml` and `docs/local-dev-docker.md` describing it as the local dev workflow; whether a host-native shadow-cljs run is also supported is out of scope here. Treating "in Docker" as the target is a project preference (per the originating task's framing), not an external mandate.

- **(P)** No currently-set `:devtools-url`, `:proxy-url`, or related WS-routing keys in `frontend/shadow-cljs.edn`. The current EDN file represents author intent at the time of writing; adding such keys is in scope for later phases if warranted, and is not a constraint.

- **(P)** Compose only publishes ports `3451` and `8777` today. This represents current state, not a hard constraint — extending the port map is mechanically trivial.

## 4. Open Questions

### Carried forward

1. **What websocket URL/host:port does the browser actually attempt to connect to?** — **escalated** — Cannot be answered from the codebase alone. shadow-cljs computes the devtools-client connection target at runtime based on a combination of its compiled-in defaults, the build EDN, and the URL the page was loaded from. To resolve it I would need to either (a) open the DevTools Network → WS tab against a running stack and read the URL of the failing connection, or (b) inspect the generated `js/` output served at runtime to find the encoded fallback. The Phase 2 brief forbids running the stack; this is a Phase 3+/runtime-investigation task.

2. **Does editing a `.cljs` file under `frontend/src/` actually trigger a hot reload while the banner is showing, or is reload itself broken?** — **escalated** — Cannot be answered without running the stack and performing the edit. This is the load-bearing verification step for the real-outcome contract in §2 and must be answered before any solution is selected; a later phase needs to make this observation explicitly. Note that the absence of a "Build completed" log-line being followed by a UI update would also signal failure, so the runtime investigation should observe both the container log and the browser behaviour during the edit.

3. **What is in the ~88 KB of browser console output from the page load?** — **escalated** — Not retrieved. May contain an explicit websocket error message, the attempted URL, or the failure mode (refused / timed out / 1006 abnormal closure / TLS handshake / etc.). Retrieval is a Phase 3 runtime task.

### Newly raised in this phase

1. **Does shadow-cljs 2.19.9 by default attempt its devtools-client websocket against the same host:port as the `:dev-http` server, against the separate `server version` port (9630 in the startup log), or somewhere else?** — Surfaced because the startup log reports two ports (3451 for `dev-http`, 9630 for `server`), and the EDN config does not pin which the client should use. The answer determines whether the existing `3451→3451` port map is enough for the WS or whether something else is being attempted. Best answered by combining the runtime-URL observation from carried-forward Q1 with shadow-cljs 2.19.9 documentation in Phase 4 (Prior Art).

2. **Are there any custom shadow-cljs hooks, watchers, or middleware configured in `frontend/shadow-cljs.edn` that could alter devtools-client behaviour?** — Surfaced while verifying the EDN. Answer per `frontend/shadow-cljs.edn:1-61`: **no** — no `:nrepl-middleware`, no top-level `:hooks`, no per-build `:build-hooks`, no devtools-client overrides beyond `:preloads`. This rules out one whole class of "the project is doing something custom" stories.

3. **What does the `frontend-shadow-cache` volume contain, and could a stale entry there contribute to the symptom?** — Surfaced because `docker-compose.yml:97` mounts a persistent `frontend-shadow-cache` named volume at `/app/.shadow-cljs`, and `--force-recreate frontend` recreates the container but **not** the named volume. Whether the symptom persists across a `docker compose down -v` (which would drop the volume) is unstated. Answering this is a Phase 3 runtime task.

4. **Is the symptom reproducible from a second host browser (different browser, fresh profile, or incognito)?** — Surfaced because the only data point is one browser on one developer's host. Cross-browser reproduction would rule out a per-browser extension / cache / cookie story before committing to a project-side change. Phase 3 runtime task.

## 5. Flagged Inaccuracies

None. Every Column A fact was verified against the codebase and is accurate:

- `frontend/shadow-cljs.edn:18` — `:nrepl {:port 8777}` ✓
- `frontend/shadow-cljs.edn:19` — `:dev-http {3451 "resources/public"}` ✓
- `frontend/shadow-cljs.edn:32-35` — `:dev :devtools :preloads` set to `[devtools.preload imas-seamap.specs.preload]` ✓
- `frontend/shadow-cljs.edn` (full file, lines 1-61) — no `:devtools-url`, `:hud`, `:proxy-url`, WS host/port, `:reload-strategy`, or `:open-file-command` keys present ✓
- `docker-compose.yml:91-93` — host port maps are `3451→3451` and `8777→8777`; port `9630` is not in the file ✓
- `docker-compose.yml:98` — frontend command is `yarn run watch` ✓
- `frontend/package.json` — `"watch": "npx shadow-cljs watch dev"` ✓
- `frontend/Dockerfile:4` — `FROM node:16-bookworm-slim` ✓
- `frontend/Dockerfile:8-9` — installs `openjdk-17-jre-headless` ✓
- `frontend/Dockerfile:23` — `EXPOSE 3451 8777` (and no 9630) ✓

shadow-cljs version 2.19.9 and container hostname `seamap-frontend-1` were taken at face value from the originating runtime observation and not separately re-verified here (no version is pinned in the EDN; the hostname is a runtime fact, not a code fact).
