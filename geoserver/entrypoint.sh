#!/bin/sh
# Drop the fetched WAR into webapps/, set CATALINA_OPTS for GEOSERVER_DATA_DIR,
# then exec the standard Tomcat command. The geoserver-fetch service is
# expected to have populated /srv/war/geoserver.war before this container
# starts (compose `depends_on: service_completed_successfully` enforces it).
set -eu

WAR_SRC="/srv/war/geoserver.war"
WAR_DEST="/usr/local/tomcat/webapps/geoserver.war"
DATA_DIR="${GEOSERVER_DATA_DIR:-/data/geoserver}"

if [ ! -f "$WAR_SRC" ]; then
  echo "[geoserver] ERROR: WAR not found at $WAR_SRC."
  echo "[geoserver] The geoserver-fetch service should have populated this volume."
  echo "[geoserver] Try: docker compose run --rm geoserver-fetch"
  exit 1
fi

if [ ! -f "$DATA_DIR/global.xml" ]; then
  echo "[geoserver] ERROR: $DATA_DIR doesn't look like a GeoServer data dir (no global.xml)."
  echo "[geoserver] The geoserver-config-init service should have seeded it."
  echo "[geoserver] Check: docker compose logs geoserver-config-init"
  exit 1
fi

echo "[geoserver] copying WAR into webapps/…"
cp "$WAR_SRC" "$WAR_DEST"

# Tomcat will explode the WAR on startup into webapps/geoserver/. If a stale
# explode exists from a previous WAR version, blow it away first so the new
# WAR's classes win.
rm -rf /usr/local/tomcat/webapps/geoserver

# Per the prod Ansible role: GEOSERVER_DATA_DIR is set via a -D JVM flag in
# CATALINA_OPTS. Heap defaults are deliberately leaner than prod (-Xms8G/-Xmx8G)
# so this fits on a developer laptop; override GEOSERVER_HEAP for chunky datasets.
HEAP_OPTS="${GEOSERVER_HEAP:--Xms512m -Xmx2g}"
EXTRA_OPTS="${GEOSERVER_EXTRA_OPTS:-}"
export CATALINA_OPTS="-DGEOSERVER_DATA_DIR=$DATA_DIR $HEAP_OPTS $EXTRA_OPTS"

echo "[geoserver] CATALINA_OPTS=$CATALINA_OPTS"
echo "[geoserver] launching $@"
exec "$@"
