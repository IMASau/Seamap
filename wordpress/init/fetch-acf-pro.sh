#!/bin/sh
# Idempotent ACF Pro fetcher. Runs as the `acf-fetch` one-shot compose service
# before wp-init, so the bind-mounted plugin dir is populated by the time
# wp-init tries to activate it.
#
# Pulls a pinned ACF Pro zip from a private GitHub release. ACF Pro is paid
# plugin source — the wordpress/.gitignore keeps it out of this public repo,
# and the IMAS-internal vendor repo (default IMASau/seamap-vendor) is where
# the zip lives.
#
# Exit codes:
#   0 — plugin already present (escape hatch) OR no token set (opted out).
#       wp-init still runs; it will warn-and-continue if ACF isn't there.
#   1 — token was set (the developer asked us to fetch) but something
#       configured went wrong: bad repo, missing asset, download or
#       extraction failed, post-extract sanity check failed. Non-zero so
#       wp-init's `service_completed_successfully` dependency fails fast
#       instead of pretending the install is healthy.
set -eu

DEST="/dest"

if [ -f "$DEST/acf.php" ]; then
  echo "[acf-fetch] ACF Pro already present at $DEST — skipping."
  exit 0
fi

if [ -z "${GITHUB_TOKEN:-}" ]; then
  cat <<'EOF'
[acf-fetch] no token set (ACF_GITHUB_TOKEN empty) — skipping ACF Pro fetch.

To enable WordPress + story-maps locally, either:
  - set ACF_GITHUB_TOKEN in .env (fine-grained PAT with read access to
    the vendor repo — see docs/local-dev-docker.md), OR
  - drop the unzipped plugin into wordpress/plugins/advanced-custom-fields-pro/.

The stack will still start, but wp-init will skip story-map /
region-report activation.
EOF
  exit 0
fi

# Past this point the developer explicitly asked us to fetch.
# Any failure is a hard fail (exit 1) so wp-init can't silently proceed
# without an ACF install.

if ! apk add --no-cache curl jq unzip >/dev/null; then
  echo "[acf-fetch] ERROR: apk add failed — Alpine package repos unreachable?"
  exit 1
fi

REPO="${ACF_PRO_REPO:-IMASau/seamap-vendor}"
TAG="${ACF_PRO_RELEASE_TAG:-acf-pro-6.8.0.1}"
ASSET="${ACF_PRO_ASSET:-advanced-custom-fields-pro.zip}"

echo "[acf-fetch] fetching $ASSET from $REPO@$TAG…"

if ! RELEASE_JSON=$(curl -fsSL \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/$REPO/releases/tags/$TAG" 2>/dev/null); then
  echo "[acf-fetch] ERROR: could not read release $REPO@$TAG (auth? missing tag?)."
  echo "[acf-fetch] Check ACF_GITHUB_TOKEN scope and ACF_PRO_REPO / ACF_PRO_RELEASE_TAG."
  exit 1
fi

ASSET_URL=$(printf '%s' "$RELEASE_JSON" | jq -r --arg name "$ASSET" \
  '.assets[] | select(.name==$name) | .url')

if [ -z "$ASSET_URL" ] || [ "$ASSET_URL" = "null" ]; then
  echo "[acf-fetch] ERROR: asset '$ASSET' not found in $REPO@$TAG."
  echo "[acf-fetch] Available assets:"
  printf '%s' "$RELEASE_JSON" | jq -r '.assets[].name' | sed 's/^/[acf-fetch]   - /'
  exit 1
fi

if ! curl -fsSL \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/octet-stream" \
  "$ASSET_URL" -o /tmp/acf.zip; then
  echo "[acf-fetch] ERROR: download failed."
  exit 1
fi

mkdir -p /tmp/acf-extract
if ! unzip -q /tmp/acf.zip -d /tmp/acf-extract; then
  echo "[acf-fetch] ERROR: '$ASSET' downloaded but isn't a valid zip."
  exit 1
fi

# Canonical ACF Pro zip nests everything under advanced-custom-fields-pro/.
# Strip that prefix if present, otherwise copy as-is.
if [ -d /tmp/acf-extract/advanced-custom-fields-pro ]; then
  if ! cp -a /tmp/acf-extract/advanced-custom-fields-pro/. "$DEST/"; then
    echo "[acf-fetch] ERROR: cp into $DEST failed (permissions on the bind mount?)."
    exit 1
  fi
else
  if ! cp -a /tmp/acf-extract/. "$DEST/"; then
    echo "[acf-fetch] ERROR: cp into $DEST failed (permissions on the bind mount?)."
    exit 1
  fi
fi

if [ ! -f "$DEST/acf.php" ]; then
  echo "[acf-fetch] ERROR: extraction completed but $DEST/acf.php is missing."
  echo "[acf-fetch] The release asset may not be a canonical ACF Pro zip —"
  echo "[acf-fetch] check 'unzip -l' on the asset uploaded to the vendor release."
  exit 1
fi

INSTALLED_VERSION=$(grep -E "^[[:space:]]*\\*[[:space:]]*Version:" "$DEST/acf.php" 2>/dev/null \
  | head -1 | awk '{print $NF}' || true)
echo "[acf-fetch] done. Installed ACF Pro ${INSTALLED_VERSION:-?} to $DEST."
