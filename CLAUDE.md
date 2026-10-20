# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Seamap is a web map for exploring marine spatial data (IMAS / UTAS, written by Condense). It is a monorepo:

- `frontend/` — ClojureScript SPA (shadow-cljs, re-frame, reagent, Leaflet, Blueprint.js).
- `backend/` — Django + DRF API backed by a **remote SQL Server** (mssql-django / pyodbc, ODBC Driver 18). Also uses GDAL/`ogr2ogr`, shapely, geopandas.
- `backend/carbonabatementsidebar/frontend/` — a separate small Vite + React + TypeScript app, built into Django static files.
- `database/` — SQL Server schema as source (Tables, Views, Procedures, Functions, Types) plus `_snapshot/`; `scripts/capture-schema.sh` + `sanitize-schema.py` regenerate the snapshot from the live dev DB.
- `etl/` — standalone pyodbc ETL (extract/transform) pulling observation data from Squidle, MARS and GlobalArchive.
- `wordpress/` — WordPress plugins (`region-report`, `story-map`) and child theme that embed/link to Seamap.

`frontend/README.md` is stale (references lein/figwheel); the build is shadow-cljs + yarn.

## Development

Never create or change data in the shared dev database, including through the dev API (eg POSTing save states); GETs are fine.

Docker is the supported dev setup (see `DOCKER.md`). It needs a repo-root `.env` with DB credentials; the DB is on the UTAS network, so the **VPN must be up**.

```sh
docker compose up --build            # backend :8000, frontend :3451, nREPL :8777, shadow dashboard :9630, WordPress :8888
docker compose exec backend python manage.py <command>
docker compose run --rm wp-init      # re-run idempotent WordPress setup (wordpress/init/bootstrap.sh)
docker compose run --rm --entrypoint wp wp-init <wp-cli command>
docker compose down -v               # resets node_modules/.m2 AND wipes the local WordPress DB/content
docker compose --profile dev-api up -d dev-api-proxy   # read-only proxy to the dev API (GET only) for running without a local backend
```

WordPress (MariaDB `wp-db` + `wordpress` + one-shot `acf-fetch`/`wp-init`) mounts the repo's plugins and child theme read-only. It uses ACF Pro if `ACF_GITHUB_TOKEN` is set (or the plugin is unzipped into the gitignored `wordpress/plugins/advanced-custom-fields-pro/`), and otherwise falls back to Secure Custom Fields. The H-Code parent theme is commercial and not in the repo. `.gitattributes` forces LF on `*.sh`, because CRLF breaks the scripts inside containers.

On first checkout (and after changing SCSS, `src/ui` JSX, or the worker), build frontend assets — `yarn watch` only compiles CLJS:

```sh
cd frontend
yarn build-css && yarn build-blueprint-css   # SCSS → resources/public/css
yarn build-js                                # Babel: src/ui/*.jsx → src/gen (consumed by CLJS)
yarn build-worker                            # Parcel: esri-leaflet web worker
yarn build-region-report-css                 # ../wordpress/plugins/region-report/style.css
yarn watch                                   # shadow-cljs watch dev
npx shadow-cljs release min                  # prod build (CI uses `min` on master, `mindev` on develop)
```

Backend settings: `webapp/settings.py` is the base; `webapp/local_settings.py` (gitignored, `DJANGO_SETTINGS_MODULE=webapp.local_settings`) is seeded from `local_settings.py.example` by `backend/docker/entrypoint.sh` and reads config from env. Extra Django apps can be added via the `EXTRA_APPS` env var.

Tests: there are effectively none (`tests.py` files are empty stubs). Runtime spec checks in dev come from `imas-seamap.specs.preload`. CI (`.github/workflows/`) only builds the frontend release tarball and runs a weekly `pip-audit` on `backend/deploy_requirements.txt`.

## Frontend architecture

- **Multiple deployments, one bundle.** `shadow-cljs.edn` compiles several entry namespaces into one `app` module: `imas-seamap.core` (Seamap Australia) plus `futuresofseafood`, `seamap-antarctica`, `tas-marine-atlas`, `natural-hazards-atlas`, `tmag-kiosk`. Each deployment's `core.cljs` exports an `init` that the hosting `index.html` calls with API/media/WordPress/image base URLs. A deployment may override `db`, `events`, `subs`, `views` in its own subdirectory.
- **Config-map handler registration.** Each deployment's `core.cljs` defines a large `config-handlers` map `{:subs {...} :events {...}}` registered by `register-handlers!` (`imas-seamap.core`). Shared handlers live in the main namespaces (`events.cljs`, `subs.cljs`, `map/`, `state_of_knowledge/`, `story_maps/`). Conventions in that map:
  - sub value is a fn → `reg-sub`; a vector → `(apply reg-sub k v)` (use `:<-` inputs for layer-3 subs).
  - event value is a fn → `reg-event-db`; a vector `[handler & interceptors]` → `reg-event-fx`.
  - When adding or changing a handler, **update every deployment's `config-handlers` that should have it** — they are copies, not inherited.
  - Layer-2 "raw db" subs are registered directly with the `:dbsubs/...` / `:dbsubs.map/...` prefix (e.g. in `map/subs.cljs`); prefer building derived subs on these rather than reading `db` directly (see `ADRs/01-sub-handler-registration.md`).
- **TMAG kiosk** (`tmag_kiosk/`): the kiosk deployment. It reads stories live from its own WordPress (`:kiosk.stories/*` in `tmag_kiosk/events.cljs`, refreshed every 5 min, and never swapped mid-story) and applies each step's shortcode with Seamap's `:merge-state` (save states are cached by shortcode). Its story fields (`read_time`, per-step `media`) come from the story-map plugin and are not used by the main Seamap site. Local entry page: `resources/public/tmag-kiosk.html`. **Read `frontend/docs/tmag-kiosk.md` before working on the kiosk**: its decisions and requirements (eg flights hold 60 fps at 4K on integrated graphics), how it works, how to test it, and the open items. Keep that doc current with the code.
- `standard-interceptors` add debug logging in dev (with a noisy-event exclusion list) and analytics in release (`events-for-analytics`).
- `src/ui/` holds React/JSX components (Blueprint-based) compiled by Babel into `src/gen/` and required from CLJS; `src/thirdparty/` and `custom_modules/` hold vendored/patched Leaflet plugins.
- App state is persisted/shared via the URL hash and backend save-states (`/api/savestates`).
- **Rich layers** (layers wrapping base layers with alternate views, timelines, side-by-side, CQL filter controls) are the most complex area; read `frontend/docs/rich-layers-overview.md`, `rich-layers-configuration.md`, and `rich-layers-refactor-plan.md` before touching them. `frontend/docs/performance-analysis.md` covers subscription performance work.

## Backend architecture

- `webapp/` — project settings, `urls.py` (DRF router + hand-written `api/habitat/*` endpoints), site configuration endpoint.
- `catalogue/` — layer catalogue models (layers, base layers, categories, organisations, rich layers, keyed layers, dynamic pills, region reports, save states), DRF viewsets, and management commands (feature index build, layer preview generation, Squidle data retrieval).
- `habitat/` — spatial query endpoints (transects, region subsets, boundary lists AMP/IMCRA/MEOW, habitat/bathymetry statistics, region report data) that largely call raw SQL / stored procedures in the SQL Server DB (see `database/`). A few catalogue models are `managed = False` and map onto DB views (`VW_*`).
- `carbonabatementsidebar/` — Django views/API plus its own Vite frontend (`yarn build` there copies output into Django static).
