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
# Graceful degradation: if GITHUB_TOKEN is unset, or the asset isn't found,
# this script exits 0 with a clear message so non-WP contributors aren't
# blocked. wp-init's bootstrap.sh will then warn about the missing plugin
# and skip activation.
set -u

DEST="/dest"

if [ -f "$DEST/acf.php" ]; then
  echo "[acf-fetch] ACF Pro already present at $DEST — skipping."
  exit 0
fi

if [ -z "${GITHUB_TOKEN:-}" ]; then
  cat <<'EOF'
[acf-fetch] GITHUB_TOKEN not set — cannot fetch ACF Pro.

To enable WordPress + story-maps locally, either:
  - set GITHUB_TOKEN in .env (fine-grained PAT with read access to
    the vendor repo is enough — see docs/local-dev-docker.md), OR
  - drop the unzipped plugin into wordpress/plugins/advanced-custom-fields-pro/.

Skipping. The stack will still start; story-map / region-report
activation will be skipped by wp-init.
EOF
  exit 0
fi

apk add --no-cache curl jq unzip >/dev/null

REPO="${ACF_PRO_REPO:-IMASau/seamap-vendor}"
TAG="${ACF_PRO_RELEASE_TAG:-acf-pro-6.8.0.1}"
ASSET="${ACF_PRO_ASSET:-advanced-custom-fields-pro.zip}"

echo "[acf-fetch] fetching $ASSET from $REPO@$TAG…"

RELEASE_JSON=$(curl -fsSL \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/$REPO/releases/tags/$TAG" 2>/dev/null) || {
  echo "[acf-fetch] ERROR: could not read release $REPO@$TAG (auth? missing tag?)."
  echo "[acf-fetch] Check GITHUB_TOKEN scope and ACF_PRO_REPO / ACF_PRO_RELEASE_TAG."
  exit 0
}

ASSET_URL=$(printf '%s' "$RELEASE_JSON" | jq -r --arg name "$ASSET" \
  '.assets[] | select(.name==$name) | .url')

if [ -z "$ASSET_URL" ] || [ "$ASSET_URL" = "null" ]; then
  echo "[acf-fetch] ERROR: asset '$ASSET' not found in $REPO@$TAG."
  echo "[acf-fetch] Available assets:"
  printf '%s' "$RELEASE_JSON" | jq -r '.assets[].name' | sed 's/^/[acf-fetch]   - /'
  exit 0
fi

curl -fsSL \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/octet-stream" \
  "$ASSET_URL" -o /tmp/acf.zip || {
  echo "[acf-fetch] ERROR: download failed."
  exit 0
}

mkdir -p /tmp/acf-extract
if ! unzip -q /tmp/acf.zip -d /tmp/acf-extract; then
  echo "[acf-fetch] ERROR: '$ASSET' downloaded but isn't a valid zip."
  exit 0
fi

# Canonical ACF Pro zip nests everything under advanced-custom-fields-pro/.
# Strip that prefix if present, otherwise copy as-is.
if [ -d /tmp/acf-extract/advanced-custom-fields-pro ]; then
  cp -a /tmp/acf-extract/advanced-custom-fields-pro/. "$DEST/"
else
  cp -a /tmp/acf-extract/. "$DEST/"
fi

INSTALLED_VERSION=$(grep -E "^[[:space:]]*\\*[[:space:]]*Version:" "$DEST/acf.php" 2>/dev/null \
  | head -1 | awk '{print $NF}')
echo "[acf-fetch] done. Installed ACF Pro ${INSTALLED_VERSION:-?} to $DEST."
