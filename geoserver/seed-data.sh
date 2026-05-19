#!/bin/sh
# Seed the geoserver-data named volume from a read-only bind mount of the
# user's geoserver-config-imas clone. We do this (rather than bind-mounting
# the clone directly) for two reasons:
#
#   1. GeoServer mutates GEOSERVER_DATA_DIR at runtime — normalises SLDs,
#      writes logs/, prunes legendsamples, etc. — which would dirty the
#      user's git working tree (and on macOS, force resolution of
#      case-folding conflicts between sibling files like
#      `SeamapAus_MEOW_REALM.sld` vs `SeamapAus_MEOW_realm.sld`).
#
#   2. Local dev needs an admin login. Prod uses Ansible to render a
#      `users.xml` against the committed keystore; here we substitute a
#      plain-text admin / geoserver pair after the rsync.
#
# Idempotent: skips seeding if /data/geoserver/global.xml exists, unless
# GEOSERVER_CONFIG_FORCE_REINIT=1. Re-seed manually with:
#   GEOSERVER_CONFIG_FORCE_REINIT=1 docker compose up geoserver-config-init
# or wipe + recreate:
#   docker compose down -v geoserver
set -eu

SRC="/srv/seed"
DEST="/data/geoserver"

if [ ! -d "$SRC" ] || [ ! -f "$SRC/global.xml" ]; then
  echo "[geoserver-config-init] ERROR: source bind mount at $SRC has no GeoServer config." >&2
  echo "[geoserver-config-init] Run scripts/geoserver-config-clone.sh, or set" >&2
  echo "[geoserver-config-init] GEOSERVER_CONFIG_PATH in .env to your clone of" >&2
  echo "[geoserver-config-init] IMASau/geoserver-config-imas." >&2
  exit 1
fi

mkdir -p "$DEST"

if [ -f "$DEST/global.xml" ] && [ "${GEOSERVER_CONFIG_FORCE_REINIT:-0}" != "1" ]; then
  echo "[geoserver-config-init] $DEST already seeded; skipping."
  echo "[geoserver-config-init] Force re-seed with GEOSERVER_CONFIG_FORCE_REINIT=1."
  exit 0
fi

apk add --no-cache rsync >/dev/null

echo "[geoserver-config-init] seeding $DEST from $SRC…"
# --delete keeps the volume in sync with the source on force-reinit; without
# it, files removed upstream would linger. .git/ stays out — large, useless to
# the running GeoServer, and pulling it in would invite git operations
# against the named volume.
rsync -a --delete --exclude='.git/' --exclude='.gitignore' "$SRC/" "$DEST/"

# Substitute a known admin user. The committed config.xml uses
# pbePasswordEncoder against geoserver.jceks, but the `plain:` prefix in
# users.xml tells GeoServer this user is plaintext regardless of the
# configured default encoder.
USERS_XML="$DEST/security/usergroup/default/users.xml"
mkdir -p "$(dirname "$USERS_XML")"
cat > "$USERS_XML" <<'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<userRegistry xmlns="http://www.geoserver.org/security/users" version="1.0">
  <users>
    <user enabled="true" name="admin" password="plain:geoserver"/>
  </users>
  <groups/>
</userRegistry>
EOF

# Upstream ships security/role/default/roles.xml mapping `admin` -> `ADMIN`,
# so we just need the user record (above) for login to work. Don't touch
# roles.xml here — its schema is fussy and the committed version is correct.

echo "[geoserver-config-init] done. Admin: admin / geoserver."
