#!/bin/sh
# Clone (or fast-forward update) IMASau/geoserver-config-imas to the path
# that docker-compose.yml bind-mounts into the geoserver container's
# GEOSERVER_DATA_DIR.
#
# Default location: ../geoserver-config-imas (sibling of this repo).
# Override with GEOSERVER_CONFIG_PATH=/some/path.
#
# Read-only; safe to re-run. ~620 MB on first clone.
set -eu

REPO_URL="${GEOSERVER_CONFIG_REPO_URL:-https://github.com/IMASau/geoserver-config-imas.git}"
DEST="${GEOSERVER_CONFIG_PATH:-$(cd "$(dirname "$0")/.." && pwd)/../geoserver-config-imas}"

if [ -d "$DEST/.git" ]; then
  echo "[geoserver-config-clone] existing clone at $DEST — updating (ff-only)…"
  if ! git -C "$DEST" pull --ff-only; then
    echo "[geoserver-config-clone] WARN: ff-only pull failed — local changes or divergent history."
    echo "[geoserver-config-clone] Resolve in $DEST manually; not touching it from here."
    exit 0
  fi
elif [ -d "$DEST" ]; then
  echo "[geoserver-config-clone] ERROR: $DEST exists but isn't a git checkout." >&2
  echo "[geoserver-config-clone] Move/remove it, or set GEOSERVER_CONFIG_PATH elsewhere." >&2
  exit 1
else
  echo "[geoserver-config-clone] cloning $REPO_URL -> $DEST (~620 MB; takes a few minutes)…"
  git clone "$REPO_URL" "$DEST"
fi

REV=$(git -C "$DEST" rev-parse --short HEAD)
echo "[geoserver-config-clone] ready. $DEST @ $REV"
