#!/usr/bin/env bash
# Capture the live UTAS Seamap dev DB schema as a reviewable schema.sql artifact.
#
# Flow:
#   1. sqlpackage /Action:Extract  : remote dev DB → intermediate dacpac  (VPN required)
#   2. sqlpackage /Action:Script   : dacpac → raw schema.sql, target is a scratch
#                                    DB on the LOCAL mssql container so the script
#                                    is "create everything from scratch"
#   3. scripts/sanitize-schema.py  : strip DacFx header, drop scratch :setvars,
#                                    stub configured linked-server views,
#                                    fail-loud validation against forbidden patterns
#   4. manage.py dump_migration_baseline : walk the codebase's MigrationLoader
#                                          graph (no DB) and emit INSERTs for
#                                          django_migrations.
#   5. Write database/_snapshot/{schema.sql, migrations_baseline.sql, CAPTURED.txt}
#
# Why .sql, not .dacpac:
#   - reviewable in PRs (binary blob isn't)
#   - keeps credentials/principals out by construction (sanitiser is a tripwire)
#   - dbinit just runs sqlcmd -i; no sqlpackage at container startup
#
# Prereqs:
#   - VPN connection to UTAS (the remote DB is on the internal network).
#   - Local mssql container running (used as the Script target):
#       docker compose up -d mssql
#   - UTAS DB credentials provided one of:
#       a) Env vars: UTAS_DB_HOST / UTAS_DB_NAME / UTAS_DB_USER / UTAS_DB_PASSWORD
#       b) backend/webapp/dev_settings.py with a DATABASES['default'] entry
#          (gitignored — keep credentials out of git).
#
# Modelled on imas-craybase/scripts/capture-schema.sh.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SNAPSHOT_DIR="$REPO_ROOT/database/_snapshot"
DEV_SETTINGS="$REPO_ROOT/backend/webapp/dev_settings.py"

extract_from_dev_settings() {
    [[ -f "$DEV_SETTINGS" ]] || { echo ""; return; }
    python3 - "$DEV_SETTINGS" "$1" <<'PY'
import re, sys
key = sys.argv[2]
with open(sys.argv[1]) as f:
    text = f.read()
m = re.search(rf"'{key}'\s*:\s*'([^']+)'", text)
print(m.group(1) if m else "", end="")
PY
}

UTAS_DB_HOST="${UTAS_DB_HOST:-$(extract_from_dev_settings HOST)}"
UTAS_DB_NAME="${UTAS_DB_NAME:-$(extract_from_dev_settings NAME)}"
UTAS_DB_USER="${UTAS_DB_USER:-$(extract_from_dev_settings USER)}"
UTAS_DB_PASSWORD="${UTAS_DB_PASSWORD:-$(extract_from_dev_settings PASSWORD)}"

# Default to the IMAS naming convention; user can override via env.
UTAS_DB_HOST="${UTAS_DB_HOST:-imasseamapdev-db.its.utas.edu.au}"
UTAS_DB_NAME="${UTAS_DB_NAME:-IMASSeamap}"

if [[ -z "$UTAS_DB_USER" || -z "$UTAS_DB_PASSWORD" ]]; then
    echo "[capture] missing UTAS DB credentials." >&2
    echo "[capture] Provide UTAS_DB_USER + UTAS_DB_PASSWORD as env vars," >&2
    echo "[capture] or fill backend/webapp/dev_settings.py with a" >&2
    echo "[capture] DATABASES['default'] entry (gitignored)." >&2
    exit 2
fi

if ! nc -z -G 5 "$UTAS_DB_HOST" 1433 >/dev/null 2>&1; then
    echo "[capture] $UTAS_DB_HOST:1433 unreachable — is the UTAS VPN up?" >&2
    exit 3
fi

# Pull SA password for the local mssql (Script target) from .env / defaults.
if [[ -f "$REPO_ROOT/.env" ]]; then
    set -a; . "$REPO_ROOT/.env"; set +a
fi
SA_PASSWORD="${MSSQL_SA_PASSWORD:-SeamapDev123!}"
SCRATCH_DB="schema_capture_scratch"

# Verify local mssql is up (we need it as the Script target).
if ! docker compose --project-directory "$REPO_ROOT" ps --status running mssql \
        | grep -q mssql; then
    echo "[capture] local mssql container isn't running." >&2
    echo "[capture] start it first: docker compose up -d mssql" >&2
    exit 4
fi

mkdir -p "$SNAPSHOT_DIR"

# Make sure the image is current (cheap if cache-hit).
docker compose --project-directory "$REPO_ROOT" build dbinit >/dev/null

# --- 1. Extract from remote --------------------------------------------------

SRC_CS="Server=tcp:${UTAS_DB_HOST},1433;Database=${UTAS_DB_NAME};User Id=${UTAS_DB_USER};Password=${UTAS_DB_PASSWORD};Encrypt=True;TrustServerCertificate=True;Connection Timeout=30;"

echo "[capture] extracting from $UTAS_DB_HOST/$UTAS_DB_NAME ..."
docker compose --project-directory "$REPO_ROOT" run --rm --no-deps \
    --entrypoint sqlpackage \
    -v "$SNAPSHOT_DIR:/snapshot" \
    dbinit \
    /Action:Extract \
    /TargetFile:/snapshot/_temp.dacpac \
    /SourceConnectionString:"$SRC_CS" \
    /p:ExtractAllTableData=false \
    /p:IgnorePermissions=true \
    /p:IgnoreUserLoginMappings=true \
    /p:VerifyExtraction=false

# --- 2. Script against an empty scratch DB on the local mssql ----------------

# Reset the scratch DB so the Script target is genuinely empty (otherwise
# sqlpackage emits a delta, not a full create).
echo "[capture] resetting $SCRATCH_DB on local mssql ..."
docker compose --project-directory "$REPO_ROOT" exec -T mssql \
    /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" \
    -Q "IF DB_ID(N'${SCRATCH_DB}') IS NOT NULL DROP DATABASE [${SCRATCH_DB}]; CREATE DATABASE [${SCRATCH_DB}];" \
    >/dev/null

TARGET_CS="Server=tcp:mssql,1433;Database=${SCRATCH_DB};User Id=sa;Password=${SA_PASSWORD};Encrypt=True;TrustServerCertificate=True;Connection Timeout=30;"

echo "[capture] scripting dacpac → raw schema.sql ..."
# ExcludeObjectTypes physically keeps users/logins/roles out of the generated
# SQL; the sanitiser tripwire validates this stays effective.
docker compose --project-directory "$REPO_ROOT" run --rm --no-deps \
    --entrypoint sqlpackage \
    -v "$SNAPSHOT_DIR:/snapshot" \
    dbinit \
    /Action:Script \
    /SourceFile:/snapshot/_temp.dacpac \
    /TargetConnectionString:"$TARGET_CS" \
    /OutputPath:/snapshot/_raw.sql \
    /p:ExcludeObjectTypes="Users;Logins;RoleMembership;Permissions;Credentials;DatabaseRoles;ServerRoleMembership;ServerRoles;Audits" \
    /p:IgnorePermissions=true \
    /p:ScriptDatabaseOptions=false

# Clean up the scratch DB.
docker compose --project-directory "$REPO_ROOT" exec -T mssql \
    /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$SA_PASSWORD" \
    -Q "IF DB_ID(N'${SCRATCH_DB}') IS NOT NULL DROP DATABASE [${SCRATCH_DB}];" \
    >/dev/null

# --- 3. Sanitise schema.sql -------------------------------------------------

echo "[capture] sanitising ..."
python3 "$REPO_ROOT/scripts/sanitize-schema.py" \
    "$SNAPSHOT_DIR/_raw.sql" \
    "$SNAPSHOT_DIR/schema.sql"

# --- 4. Derive migrations_baseline.sql from the codebase --------------------

# The codebase IS the source of truth for which migrations exist.
# `dump_migration_baseline` walks MigrationLoader.graph.nodes with
# connection=None — no DB access — and emits one INSERT per node.
echo "[capture] dumping django_migrations baseline from codebase ..."
docker compose --project-directory "$REPO_ROOT" run --rm --no-deps \
    --entrypoint bash \
    -v "$SNAPSHOT_DIR:/snapshot" \
    -e USE_SQLITE=1 \
    backend \
    -c 'python manage.py dump_migration_baseline --settings webapp.docker_settings' \
    > "$SNAPSHOT_DIR/migrations_baseline.sql"

n_rows=$(grep -c "^INSERT INTO" "$SNAPSHOT_DIR/migrations_baseline.sql" || true)
if [ "$n_rows" -eq 0 ]; then
    echo "[capture] ERROR: migrations_baseline.sql produced 0 rows." >&2
    echo "[capture]   Expected at least the stock Django migrations" >&2
    echo "[capture]   (admin/auth/contenttypes/sessions). Something is wrong" >&2
    echo "[capture]   with the backend image or settings — refusing to write." >&2
    rm -f "$SNAPSHOT_DIR/migrations_baseline.sql"
    exit 5
fi
echo "[capture]   $n_rows migration rows derived from codebase"

# Intermediate artifacts: not committed, not needed once schema.sql is built.
rm -f "$SNAPSHOT_DIR/_temp.dacpac" "$SNAPSHOT_DIR/_raw.sql"

# --- 5. Provenance -----------------------------------------------------------

cat > "$SNAPSHOT_DIR/CAPTURED.txt" <<EOF
captured: $(date -u +%Y-%m-%dT%H:%M:%SZ)
source:   ${UTAS_DB_HOST}/${UTAS_DB_NAME}
by:       $(id -un)@$(hostname -s)
git-sha:  $(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)
schema-sha256:    $(shasum -a 256 "$SNAPSHOT_DIR/schema.sql" | awk '{print $1}')
baseline-sha256:  $(shasum -a 256 "$SNAPSHOT_DIR/migrations_baseline.sql" | awk '{print $1}')
baseline-rows:    ${n_rows}
EOF

echo "[capture] done."
ls -lh "$SNAPSHOT_DIR/schema.sql" "$SNAPSHOT_DIR/migrations_baseline.sql" "$SNAPSHOT_DIR/CAPTURED.txt"
