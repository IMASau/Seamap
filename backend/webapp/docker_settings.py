"""Settings for the Docker-based local dev setup.

Selected via ``DJANGO_SETTINGS_MODULE=webapp.docker_settings`` in
``docker-compose.yml``. Reads database/cache config from environment.

Committed deliberately — the project ``.gitignore`` excludes
``local_settings*.py`` so devs can keep personal overrides; this file is
the shared baseline for the Docker stack.
"""

import os

from .settings import *  # noqa: F401, F403
from .settings import BASE_DIR

DEBUG = True
ALLOWED_HOSTS = ["*"]

# Set USE_SQLITE=1 to bypass SQL Server entirely (handy for plain Django work
# that does not touch the production schema).
USE_SQLITE = os.environ.get("USE_SQLITE", "0") == "1"

if USE_SQLITE:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": os.path.join(BASE_DIR, "db.sqlite3"),
        },
        "transects": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": os.path.join(BASE_DIR, "db.sqlite3"),
        },
    }
else:
    _mssql_common = {
        "ENGINE": "mssql",
        "HOST": os.environ.get("DB_HOST", "mssql"),
        "PORT": os.environ.get("DB_PORT", "1433"),
        "USER": os.environ.get("DB_USER", "sa"),
        "PASSWORD": os.environ.get("DB_PASSWORD", ""),
        "OPTIONS": {
            "driver": "ODBC Driver 18 for SQL Server",
            "extra_params": "TrustServerCertificate=yes;Encrypt=yes",
        },
    }
    DATABASES = {
        "default": dict(_mssql_common, NAME=os.environ.get("DB_NAME", "seamap")),
        "transects": dict(
            _mssql_common,
            NAME=os.environ.get("DB_TRANSECTS_NAME", os.environ.get("DB_NAME", "seamap")),
        ),
    }

OGR2OGR_PATH = os.environ.get("OGR2OGR_PATH", "/usr/bin/ogr2ogr")

CORS_ORIGIN_WHITELIST = [
    "http://localhost:3451",
    "http://127.0.0.1:3451",
    "http://localhost:8000",
    "http://127.0.0.1:8000",
]

# The `sql` app's 0001_initial RunScript loads database/Types|Tables|... in
# author-defined order and has known FK / drift problems against a fresh DB.
# Skip its migrations so `manage.py migrate` succeeds; if you need the
# Seamap-specific tables / views, run dbinit with LOAD_SCHEMA=1 (best-effort)
# or apply the relevant database/*.sql files manually.
MIGRATION_MODULES = {
    "sql": None,
}
