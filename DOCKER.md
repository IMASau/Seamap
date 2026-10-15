# Running Seamap with Docker

A containerised development setup so a new machine only needs Docker — no local
Python, GDAL/ODBC, Node, Yarn, or Java required.

It runs the same two dev processes you'd run by hand:

| Service    | Command (in container)              | URL                     |
|------------|-------------------------------------|-------------------------|
| `backend`  | `python manage.py runserver`        | http://localhost:8000   |
| `frontend` | `yarn watch` (shadow-cljs)          | http://localhost:3451   |

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
- shadow-cljs nREPL: `localhost:8777` (connect your editor here)
- shadow-cljs dashboard: http://localhost:9630 (also serves the hot-reload
  websocket — if you ever see "Reconnecting …" in the browser, this port isn't
  reachable)

Source code on the host is bind-mounted into both containers, so edits trigger
live reload exactly like running the servers locally.

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
- These images target local development only (`DEBUG=True`, the Django dev
  server). They are not hardened for production.
