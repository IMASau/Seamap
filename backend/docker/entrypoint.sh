#!/usr/bin/env bash
# Backend container entrypoint.
#
# The source tree is bind-mounted over /app at runtime, so we can't bake
# local_settings.py into the image — it would be shadowed by the host tree.
# Instead, seed it from the env-driven template on first start. The template
# reads its values from the environment (populated by docker-compose's env_file
# from the repo-root .env), so the copy needs no editing to work.
#
# An existing local_settings.py is left untouched, so anyone running outside
# Docker keeps their own hand-maintained settings.
set -euo pipefail

SETTINGS="/app/webapp/local_settings.py"
TEMPLATE="/app/webapp/local_settings.py.example"

if [ ! -f "$SETTINGS" ]; then
    echo "entrypoint: seeding webapp/local_settings.py from template"
    cp "$TEMPLATE" "$SETTINGS"
fi

exec "$@"
