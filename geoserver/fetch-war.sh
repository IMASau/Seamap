#!/bin/sh
# Fetch the IMAS-built GeoServer WAR from a GitHub release of
# IMASau/geoserver-build into /srv/war/geoserver.war.
#
# Mirrors the production deploy (imas-data-infrastructure/roles/git_release):
# - hits the releases API
# - filters releases whose .name contains GEOSERVER_WAR_TAG_FILTER (default "master")
# - takes the first asset whose name starts with "geoserver"
# - downloads with Accept: application/octet-stream
#
# The repo is public, so GITHUB_TOKEN is optional (only needed to avoid the
# 60/hr unauthenticated rate limit). Idempotent: skips work if the cached WAR
# already exists, unless GEOSERVER_WAR_FORCE_REFRESH=1.
set -u

DEST_DIR="/srv/war"
DEST="$DEST_DIR/geoserver.war"

mkdir -p "$DEST_DIR"

if [ -f "$DEST" ] && [ "${GEOSERVER_WAR_FORCE_REFRESH:-0}" != "1" ]; then
  echo "[geoserver-fetch] WAR already cached at $DEST."
  echo "[geoserver-fetch] Set GEOSERVER_WAR_FORCE_REFRESH=1 to re-download."
  exit 0
fi

apk add --no-cache curl jq >/dev/null

REPO="${GEOSERVER_WAR_REPO:-IMASau/geoserver-build}"
FILTER="${GEOSERVER_WAR_TAG_FILTER:-master}"
ASSET_PREFIX="${GEOSERVER_WAR_ASSET_PREFIX:-geoserver}"

# Optional auth — avoids unauthenticated rate limit on the releases API.
AUTH_HEADER=""
if [ -n "${GITHUB_TOKEN:-}" ]; then
  AUTH_HEADER="Authorization: Bearer $GITHUB_TOKEN"
fi

echo "[geoserver-fetch] listing releases of $REPO (filter: name~='$FILTER')…"
RELEASES_JSON=$(curl -fsSL \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/$REPO/releases?per_page=100") || {
  echo "[geoserver-fetch] ERROR: could not list releases of $REPO." >&2
  exit 1
}

# Pick the latest release whose name contains the filter token (e.g. "master").
# The API returns releases in created-at-desc order, so [0] after filter is latest.
RELEASE_JSON=$(printf '%s' "$RELEASES_JSON" \
  | jq --arg f "$FILTER" '[.[] | select(.name | contains($f))] | .[0]')

if [ -z "$RELEASE_JSON" ] || [ "$RELEASE_JSON" = "null" ]; then
  echo "[geoserver-fetch] ERROR: no release with name containing '$FILTER' found." >&2
  exit 1
fi

RELEASE_NAME=$(printf '%s' "$RELEASE_JSON" | jq -r '.name')
ASSET_URL=$(printf '%s' "$RELEASE_JSON" \
  | jq -r --arg p "$ASSET_PREFIX" \
      '.assets[] | select(.name | startswith($p)) | .url' \
  | head -1)
ASSET_NAME=$(printf '%s' "$RELEASE_JSON" \
  | jq -r --arg p "$ASSET_PREFIX" \
      '.assets[] | select(.name | startswith($p)) | .name' \
  | head -1)

if [ -z "$ASSET_URL" ] || [ "$ASSET_URL" = "null" ]; then
  echo "[geoserver-fetch] ERROR: no asset starting with '$ASSET_PREFIX' in release '$RELEASE_NAME'." >&2
  exit 1
fi

echo "[geoserver-fetch] release: $RELEASE_NAME"
echo "[geoserver-fetch] asset:   $ASSET_NAME"
echo "[geoserver-fetch] url:     $ASSET_URL"
echo "[geoserver-fetch] downloading…"

curl -fSL \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -H "Accept: application/octet-stream" \
  "$ASSET_URL" -o "$DEST.part" || {
  echo "[geoserver-fetch] ERROR: download failed." >&2
  rm -f "$DEST.part"
  exit 1
}

mv "$DEST.part" "$DEST"
SIZE=$(du -h "$DEST" | awk '{print $1}')
echo "[geoserver-fetch] done. $ASSET_NAME ($SIZE) -> $DEST"
