# Local development with Docker

This brings up the full stack — SQL Server, Django backend, shadow-cljs
frontend — in containers so you don't need GDAL, the Microsoft ODBC driver,
a JVM, or SQL Server installed on the host.

## Prerequisites

- Docker Desktop with the Compose v2 plugin.
- ~6 GB of free disk for images and the SQL Server data volume.
- macOS Apple Silicon: SQL Server's image is amd64 only and runs under
  Rosetta. First boot is 30–60 seconds; queries are slower than native.

## First run

```bash
cp .env.example .env             # tweak passwords/ports if other IMAS stacks are running
docker compose build             # ~5 min once GDAL/ODBC layers are cached
docker compose up -d              # boots mssql → dbinit → backend → frontend
docker compose exec backend python manage.py migrate
```

Then:

| Service                 | URL                          |
|-------------------------|------------------------------|
| Backend (Django)        | http://localhost:8000        |
| Frontend (shadow-cljs)  | http://localhost:3451        |
| shadow-cljs nREPL       | localhost:8777               |
| shadow-cljs dev server  | http://localhost:9630 (hot-reload websocket lives here; must be reachable from the host browser for live `.cljs` reload) |
| SQL Server              | localhost:1435 (sa / `MSSQL_SA_PASSWORD` from `.env`) |
| GeoServer admin         | http://localhost:8080/geoserver/web/ (admin / geoserver) |
| WordPress (story maps)  | http://localhost:8888 — REST at `/wp-json/wp/v2/story_map?acf_format=standard`; admin at `/wp-admin/` (admin / admin) |

The default `.env.example` ports avoid clashing with sibling IMAS stacks
(`imas-craybase` uses 1433, `imas-immerse` uses 1434).

## DB schema bootstrap

Modelled directly on `imas-craybase`'s schema-capture flow:

1. `scripts/capture-schema.sh` (VPN required) runs `sqlpackage /Action:Extract`
   against `imasseamapdev-db.its.utas.edu.au/IMASSeamap`, then
   `sqlpackage /Action:Script` against an empty scratch DB on the local
   `mssql` container to produce a "create everything" script. Sanitisation
   (`scripts/sanitize-schema.py`) strips DacFx headers, swaps any
   linked-server view bodies for empty stubs, and tripwires against
   credentials / principals slipping through.
2. `manage.py dump_migration_baseline` walks the codebase's
   `MigrationLoader.graph` (no DB connection) and emits one
   `INSERT INTO django_migrations` per node — so the local DB looks
   already-migrated to Django.
3. Both artifacts land under `database/_snapshot/` and are committed
   (~kB to MB-scale text, PR-reviewable).
4. `backend/docker/dbinit.py` applies `schema.sql` + `migrations_baseline.sql`
   at container start with a sha-sentinel skip mechanism — cheap re-boots
   when the snapshot hasn't changed, drop+recreate when it has.

### Running a capture (VPN required, one-time-ish)

```bash
# 1. Bring up just mssql (it's the Script target for sqlpackage):
docker compose up -d mssql

# 2. Provide UTAS DB credentials via env vars, OR drop them into
#    backend/webapp/dev_settings.py (gitignored):
UTAS_DB_USER=<your-utas-db-user> \
UTAS_DB_PASSWORD=<your-utas-db-password> \
    ./scripts/capture-schema.sh

# 3. Commit the new artifacts:
git add database/_snapshot/{schema.sql,migrations_baseline.sql,CAPTURED.txt}
git commit -m "Capture Seamap DB schema snapshot"

# 4. Apply locally:
docker compose up dbinit            # picks up the new sha, drops & re-applies
```

Capture defaults to `imasseamapdev-db.its.utas.edu.au` / `IMASSeamap`
(IMAS naming convention; verified reachable). Override with
`UTAS_DB_HOST` / `UTAS_DB_NAME` if needed.

### Fallback: no snapshot

Without `database/_snapshot/schema.sql`, `dbinit` just creates the empty
database and lets `manage.py migrate` build the Django-managed tables
(`auth`, `sessions`, `admin`, `catalogue`, `carbonabatementsidebar`, …).
The `sql` app's migrations (which would try to load the hand-rolled
`database/Types/HabitatTableType.sql` etc.) are disabled in
`webapp/docker_settings.py` (see `MIGRATION_MODULES`) — same workaround
craybase uses. Sentry tasks that touch Seamap-specific tables won't
reproduce until a snapshot is captured.

For ad-hoc loads against the no-snapshot DB, `./database` is bind-mounted
read-only into both the `mssql` container and the `backend` container at
`/database`:

```bash
docker compose exec mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa \
    -C -P "$MSSQL_SA_PASSWORD" -d seamap \
    -i /database/Views/VW_TIMELINE_COUNT_STATS_NETWORKS.sql
```

## WordPress (story maps + region reports)

Production Seamap puts "story maps" and "region report" pages on a sibling
WordPress install at the same domain as the frontend. Both are bespoke custom
post types — `story_map` and `region_report` — registered in PHP by the
`wordpress/plugins/story-map/` and `wordpress/plugins/region-report/` plugins
(Condense-authored, AGPL). ACF Pro provides the field-group machinery; the
field group itself is also registered in PHP (`story-map.php` →
`acf_add_local_field_group(...)`), so there is no field-group export to
import — activating the plugin is enough to produce the right REST shape.

The frontend reads:

```
GET http://localhost:8888/wp-json/wp/v2/story_map?acf_format=standard
→ [{ id, title.rendered, acf.description, acf.image, acf.map_links[] }, ...]
```

See `frontend/src/cljs/imas_seamap/story_maps/events.cljs:7` for the
response-shape contract.

### Boot

```bash
docker compose up -d wp-db wordpress     # MariaDB + WP image (no install yet)
docker compose run --rm wp-init          # one-shot: wp core install,
                                         #          activate plugins,
                                         #          set permalinks
```

`wp-init` is idempotent. Re-run it after pulling a plugin change or after
`docker compose down` (the WP install survives in the `wp-data` named volume;
`down -v` wipes it).

Admin is at <http://localhost:8888/wp-admin/> with `WP_ADMIN_USER` /
`WP_ADMIN_PASSWORD` from `.env` (default `admin` / `admin` — change in `.env`
if the box is reachable beyond loopback).

### Authoring a story map

`wp-admin` → **Story Maps** → **Add New**. Fill in title + the ACF "Story Map"
sidebar (Image, Description, one or more Map Links rows with a `shortcode`
that resolves against the Django `save-state` API). On save, the post appears
in the frontend at <http://localhost:3451> under the left-drawer **Featured
Maps** tab.

### Layout

```
wordpress/
├── plugins/
│   ├── story-map/                       (vendored, AGPL — registers CPT + ACF group)
│   ├── region-report/                   (vendored, AGPL — registers CPT + chart widget)
│   └── advanced-custom-fields-pro/      (paid plugin, .gitignored — see below)
├── themes/
│   └── h-code-child/                    (vendored — currently unused locally;
│                                         needs the commercial h-code parent
│                                         to activate)
└── init/
    └── bootstrap.sh                     (wp-init container entrypoint)
```

ACF Pro is paid plugin source and is not committed — `wordpress/.gitignore`
keeps the directory out of git. Obtain a copy from a deployed instance:

```bash
ssh seamapaus-dev "tar czf - -C /var/www/seamapaustralia-dev.imas.utas.edu.au \
  wp-content/plugins/advanced-custom-fields-pro" \
  | tar xzf - -C wordpress/  --strip-components=1
```

(The plugin works without a licence key — you just don't get auto-updates.)

### What's intentionally omitted

- **Production theme (`h-code`)** — commercial; the frontend never visits a
  themed WP page, so a default theme is fine locally. `h-code-child` is
  vendored for parity but won't activate cleanly without the parent.
- **Elementor / WPBakery / HCode addons** — page-builders for the WP
  front-end. The Seamap SPA only consumes ACF fields via REST, so these
  don't affect the contract.
- **Content snapshot** — story_map posts and region_report pages from prod
  aren't imported. Hand-author what you need, or pull a WXR via the wp-admin
  Tools → Export UI on a deployed instance.

## Faster iteration with SQLite

If you don't need SQL Server (frontend work, layout, plain Python logic
that doesn't touch the production schema), set `USE_SQLITE=1` and skip
the `mssql` / `dbinit` services:

```bash
USE_SQLITE=1 docker compose up backend frontend
```

`docker_settings.py` routes both `default` and `transects` aliases to a
local SQLite file inside the backend container.

## Working in a git worktree

Doing dev-stack work in a `git worktree` keeps the main checkout free for
other branches. One gotcha: docker compose names its project after the
checkout directory, and **named volumes are scoped to the project name**, so
moving to a worktree will normally spawn empty new volumes (fresh WP install,
empty SQL Server, etc.) rather than reusing the main checkout's data.

To keep the existing volumes, pin the project name in the worktree's `.env`:

```bash
echo 'COMPOSE_PROJECT_NAME=seamap' >> .env
```

`COMPOSE_PROJECT_NAME=seamap` matches the default project name used by the
main checkout at `~/repos/IMASau/Seamap/`, so `docker compose up` from either
directory talks to the same `seamap_wp-data`, `seamap_mssql-data`, etc.
volumes.

A few sharp edges to know about:

- `.env` is git-ignored, so you'll need to seed the worktree's `.env` from
  the main checkout (`cp ../Seamap/.env .env`) before bringing things up.
- ACF Pro (`wordpress/plugins/advanced-custom-fields-pro/`) is also
  git-ignored. If you're moving an in-progress dev environment, copy or move
  it from the main checkout's `wordpress/plugins/` into the worktree's.
- Only one checkout should run the stack at a time — port bindings collide
  even if the project name matches.

## Common operations

```bash
# Django shell
docker compose exec backend python manage.py shell

# Backend logs
docker compose logs -f backend

# Rebuild after dependency / Dockerfile changes
docker compose build backend --no-cache

# Drop everything (including the DB volume) and start fresh
docker compose down -v
```

## nREPL

Point your editor at `localhost:8777` for a CLJS nREPL session against
the running shadow-cljs build.

## Local GeoServer

The stack includes a vanilla OSGeo GeoServer 2.24.4 on
http://localhost:8080/geoserver/web/ (admin / `geoserver`). It starts empty
— the `geoserver-data` named volume persists workspaces / layers across
restarts.

For higher-fidelity reproduction of production GeoServer behaviour:

- **Layers / workspaces / styles**: `github.com/IMASau/geoserver-config-imas`
  is the production GeoServer data directory. Clone it and point GeoServer
  at it by replacing the `geoserver-data:/opt/geoserver_data` volume with a
  bind mount of the clone. ~600 MB on disk.
- **IMAS GeoServer image** (with AODN extensions, layer-filter plugin,
  etc.): `github.com/IMASau/geoserver-build` builds the production WAR.
  Swap the `image:` in `docker-compose.yml` for the artifact that build
  produces if you need the same GeoServer extensions as production.

The vanilla OSGeo image is enough for testing the Django-side handling of
WMS/WFS responses (timeouts, malformed XML, etc.) — those Sentry tasks
are about client-side resilience, not GeoServer-specific quirks.

`catalogue.Layer.server_url` is per-row in the database, so a fresh local
DB will still point most layers at `geoserver.imas.utas.edu.au`. To route
a specific layer through the local instance:

1. Add the workspace + layer in the GeoServer admin UI (or import a data
   directory dump under `/opt/geoserver_data`).
2. Update the corresponding `catalogue_layer.server_url` row to
   `http://geoserver:8080/geoserver/wms` (the in-compose hostname) — or
   `http://host.docker.internal:8080/geoserver/wms` if you want both
   containerised and host-based clients to hit the same URL.

For shapefiles / GeoTIFFs you keep on the host, uncomment the
`./geoserver-input:/opt/geoserver_input:ro` bind mount in
`docker-compose.yml` and point GeoServer data stores at
`/opt/geoserver_input/...`.

## Known limitations

- **Most layers still point at remote GeoServer** — Sentry-flagged
  `ReadTimeout` issues reproduce against `geoserver.imas.utas.edu.au`
  unless you've rewired the relevant `catalogue_layer.server_url` row to
  the local instance above.
- **No fixtures** — Sentry tasks that reference specific tables /
  views (`FINALPRODUCT_SeamapAus_current`,
  `VW_TIMELINE_COUNT_STATS_NETWORKS`) need that schema loaded into the
  local database first. The `database/` tree is the source of truth.
- **node-sass pin** — `package.json` still pins `node-sass@7.0.3`, which
  only ships prebuilt binaries up to Node 16. The frontend image runs
  Node 16 + yarn for that reason. Replacing `node-sass` with `sass`
  (Dart Sass) would let us move to a current Node LTS.
- **`yarn`, not `npm`** — the package set includes `link:` deps to local
  `custom_modules/`, which npm doesn't support. The entrypoint runs
  `yarn install --frozen-lockfile` on first start.
- **SQL Server on Apple Silicon** — runs via Rosetta. Slow but functional.
- **No WordPress content snapshot** — local WP boots empty; story_map and
  region_report posts have to be hand-authored or imported manually (see
  the WordPress section above).
