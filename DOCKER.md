# Running Seamap with Docker

A containerised development setup so a new machine only needs Docker — no local
Python, GDAL/ODBC, Node, Yarn, or Java required.

It runs the same dev processes you'd run by hand, plus a local WordPress:

| Service     | Command (in container)              | URL                     |
|-------------|-------------------------------------|-------------------------|
| `backend`   | `python manage.py runserver`        | http://localhost:8000   |
| `frontend`  | `yarn watch` (shadow-cljs)          | http://localhost:3451   |
| `wordpress` | Apache + WordPress (MariaDB in `wp-db`) | http://localhost:8888 |

The backend connects to the **remote SQL Server** configured through
environment variables. There is no local database container — if the DB is on
the UTAS network you must be on the VPN.

## One-time setup

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
   (or Docker Engine + Compose v2).

2. Create your `.env` file from the template and fill in the DB credentials
   (ask a team member for the host / user / password):

   ```sh
   cp .env.example .env
   ```

   `docker compose` passes these values into the backend container. On first
   start the container seeds `backend/webapp/local_settings.py` (gitignored)
   from an env-driven template, so there's no settings file to edit by hand.

## Start everything

```sh
docker compose up --build
```

The first build is slow (compiling native deps, downloading the ODBC driver and
JVM, and `yarn install`). Later starts are fast — dependencies are cached in the
image and in named volumes.

- Backend: http://localhost:8000
- Frontend: http://localhost:3451
- WordPress: http://localhost:8888 (admin: http://localhost:8888/wp-admin/)
- shadow-cljs nREPL: `localhost:8777` (connect your editor here)
- shadow-cljs dashboard: http://localhost:9630 (also serves the hot-reload
  websocket — if you ever see "Reconnecting …" in the browser, this port isn't
  reachable)

Source code on the host is bind-mounted into both containers, so edits trigger
live reload exactly like running the servers locally. File-change events from a
Windows or macOS host don't reach the container, so the frontend's shadow-cljs
polls for changes instead (`SHADOW_CLJS` in `docker-compose.yml`).

Stop with `Ctrl-C`, or run detached with `docker compose up -d` and stop with
`docker compose down`.

## Frontend assets (first checkout)

`yarn watch` only compiles ClojureScript. On a fresh checkout you also need to
build the CSS and the Leaflet web worker once (and after changing them):

```sh
docker compose exec frontend yarn build-css
docker compose exec frontend yarn build-js
docker compose exec frontend yarn build-blueprint-css
docker compose exec frontend yarn build-worker
docker compose exec frontend yarn build-region-report-css   # WordPress region-report plugin
```

## WordPress (story maps + region reports)

The frontend reads **story maps** from WordPress
(`/wp-json/wp/v2/story_map?acf_format=standard`) and links to **region report**
pages (`/region-reports/<slug>/`), which call back into the backend API. Both
are custom post types from `wordpress/plugins/story-map` and
`wordpress/plugins/region-report`.

`docker compose up` brings WordPress up on http://localhost:8888 and runs
`wp-init` (`wordpress/init/bootstrap.sh`), a one-shot wp-cli setup that:

- installs WordPress (admin at http://localhost:8888/wp-admin/, `admin` /
  `admin` unless `WP_ADMIN_USER` / `WP_ADMIN_PASSWORD` are set in `.env`);
- sets `/%postname%/` permalinks (needed for `/wp-json/` and the CPT slugs);
- activates the Seamap plugins and a custom-fields plugin (see below);
- points the region-report URL options (Settings → Region Reports) at the local
  backend and frontend. Only empty options are filled, so your edits stick.

It's idempotent — re-run it after pulling plugin changes or installing a theme:

```sh
docker compose run --rm wp-init
```

The repo's plugins and child theme are bind-mounted read-only, so PHP/JS edits
show up on page reload. Everything else (WP core, uploads, content, plugins or
themes added via wp-admin) lives in the `wp_data` / `wp_db_data` volumes, so it
survives `docker compose down` but **`docker compose down -v` wipes it**.

Run wp-cli against the site with:

```sh
docker compose run --rm --entrypoint wp wp-init <command>    # e.g. post list --post_type=story_map
```

### Custom fields: ACF Pro or Secure Custom Fields

The story map fields (including a repeater) are registered in PHP via ACF.
Production uses **ACF Pro**, which is paid and not in git. The `acf-fetch`
service downloads it from a private IMAS release when `ACF_GITHUB_TOKEN` is set
in `.env` (a fine-grained PAT with *Contents: read* on `IMASau/seamap-vendor`).
Alternatively, unzip it into `wordpress/plugins/advanced-custom-fields-pro/`
(gitignored) yourself.

Without ACF Pro, `wp-init` installs **Secure Custom Fields**, the free
wordpress.org fork of ACF, which includes repeaters and produces the same REST
shape. Adding ACF Pro later and re-running `wp-init` switches over.

### Theme

The `h-code-child` theme needs the commercial **H-Code** parent theme, which
isn't in the repo, so locally the default WordPress theme is used. Story map and
region report REST/pages work without it. To match production, upload H-Code via
wp-admin → Appearance → Themes and re-run `wp-init`, which activates the child
theme.

### Content

A fresh install has no story maps or region reports. Author them in wp-admin, or
import a database dump from a deployed site and rewrite its URLs:

```sh
docker compose exec -T wp-db mariadb -uwordpress -pwordpress wordpress < dump.sql
docker compose run --rm --entrypoint wp wp-init search-replace 'https://<deployed-host>' 'http://localhost:8888' --skip-columns=guid
docker compose run --rm wp-init   # re-assert local permalinks/plugins/options
```

Region report URL options imported this way keep their deployed values; clear
them in Settings → Region Reports (or with `wp option delete`) before re-running
`wp-init` to point them at the local backend.

## TMAG kiosk (without a local backend)

`frontend/resources/public/tmag-kiosk.html` boots the kiosk deployment
(`imas_seamap.tmag_kiosk`) with stories from the local WordPress and Seamap data
(layer catalogue, saved map views) from the local backend. Like `index.html`, the
page is the deployment configuration: it passes the API, media and WordPress
URLs to `init`.

```sh
docker compose up -d backend frontend wordpress
# then open http://localhost:3451/tmag-kiosk.html
```

Saved map views (story shortcodes) must exist in the database the backend uses.

To run without a local backend, `dev-api-proxy` is a read-only nginx
pass-through to the shared **dev** API (which sends no CORS headers). It forwards
GET/HEAD only and refuses anything that would write. Point the page's API and
media URLs at `http://localhost:8001/seamap/` to use it.

```sh
docker compose --profile dev-api up -d dev-api-proxy   # http://localhost:8001/seamap/api/
```

Judge performance (map animation, touch) on a release build, not the `watch`
dev build. `tmag-kiosk-release.html` is the same page loading an optimised build
from `js-release/`, which leaves the dev build alone. It isn't rebuilt on save:

```sh
docker compose exec frontend yarn release-local
# then open http://localhost:3451/tmag-kiosk-release.html
```

## Handy commands

```sh
# Django management commands
docker compose exec backend python manage.py <command>

# A shell in either container
docker compose exec backend bash
docker compose exec frontend bash

# Rebuild after changing a Dockerfile or dependency manifest
docker compose build backend
docker compose build frontend

# Reset cached frontend deps (node_modules + Maven cache)
docker compose down -v
```

## Notes / troubleshooting

- **DB connection refused / timeout:** confirm you're on the UTAS VPN and that
  the host/credentials in `.env` are correct. Docker reaches the network
  through your host, so the VPN must be up on the host machine.
- **Changed `.env` but the backend didn't pick it up:** `env_file` values are
  read into the container at start, so restart the backend
  (`docker compose up -d backend`). If you'd already started once, the seeded
  `backend/webapp/local_settings.py` still reads from the (new) environment, so
  no need to delete it — but you can, and it'll be re-seeded on next start.
- **Frontend dep not found after a change (e.g. `node-sass: not found`):** the
  `frontend_node_modules` named volume is only seeded from the image the first
  time it's created, so it keeps shadowing the image with stale modules even
  after `docker compose build frontend`. Recreate the volume to pick up the new
  dependencies:

  ```sh
  docker compose down -v        # drops node_modules + Maven cache volumes
  # or just the frontend deps volume:
  docker volume rm seamap_frontend_node_modules
  ```
- **Region report pages can't load data from the backend (CORS):** the backend
  must allow `http://localhost:8888`. New `local_settings.py` copies include it;
  if yours was seeded earlier, add it to `CORS_ORIGIN_WHITELIST` (or delete the
  file and restart the backend to re-seed it).
- **`bash
: No such file or directory` / `set: pipefail
`:** a shell script
  was checked out with CRLF line endings. `.gitattributes` now forces LF for
  `*.sh`; delete the file, `git checkout -- <file>`, and rebuild
  the image.
- These images target local development only (`DEBUG=True`, the Django dev
  server). They are not hardened for production.
