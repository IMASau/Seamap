#!/usr/bin/env python3
"""One-shot DB bootstrap for the local Seamap mssql container.

Order of operations:
  1. Wait for mssql to accept connections.
  2. CREATE DATABASE if missing.
  3. If a snapshot schema.sql is present at /srv/database/_snapshot/schema.sql,
     check whether it's already been applied (sha256 against a sentinel row
     in the target DB) and, if not, drop+recreate the DB, run
     ``sqlcmd -i schema.sql``, then run ``sqlcmd -i migrations_baseline.sql``
     to populate django_migrations from the codebase-derived baseline.
  4. If LOAD_SCHEMA=1 and there is no snapshot, fall back to a best-effort
     load of database/{Types,Tables,Functions,Views,Procedures}/*.sql. Useful
     for working with the hand-rolled SSDT tree directly; expect FK / drift
     failures (see DOCKER_DEV-style notes in docs/local-dev-docker.md).

After dbinit, the backend entrypoint runs ``manage.py migrate``: no-op for
baselined migrations, runs anything added since the last capture, and fires
post_migrate so django_content_type / auth_permission get populated from
INSTALLED_APPS.

Env vars:
  DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
  FORCE_REINIT=1   — re-apply schema.sql even if the sentinel says we're current.
                     Drops & recreates the database first to guarantee a clean
                     apply (sqlcmd -i is not idempotent on an already-populated DB).
  SKIP_BOOTSTRAP=1 — skip schema apply entirely (Django migrate still runs).
  LOAD_SCHEMA=1    — only relevant when there's no snapshot. Fall back to
                     loading hand-rolled database/*.sql files.

Modelled on imas-craybase/docker/dbinit.py.
"""
from __future__ import annotations

import hashlib
import os
import re
import subprocess
import sys
import time
from pathlib import Path

DB_HOST = os.environ.get("DB_HOST", "mssql")
DB_PORT = os.environ.get("DB_PORT", "1433")
DB_NAME = os.environ.get("DB_NAME", "seamap")
DB_USER = os.environ.get("DB_USER", "sa")
DB_PASSWORD = os.environ["DB_PASSWORD"]

DATABASE_DIR = Path("/database")
SNAPSHOT_DIR = DATABASE_DIR / "_snapshot"
SCHEMA_PATH = SNAPSHOT_DIR / "schema.sql"
BASELINE_PATH = SNAPSHOT_DIR / "migrations_baseline.sql"
CAPTURED_PATH = SNAPSHOT_DIR / "CAPTURED.txt"
SENTINEL_TABLE = "_bootstrap_sentinel"

LOAD_SCHEMA = os.environ.get("LOAD_SCHEMA", "0") == "1"
LEGACY_LOAD_ORDER = ["Types", "Tables", "Functions", "Views", "Procedures"]


def sqlcmd(args: list[str], *, db: str | None = None, capture: bool = True) -> subprocess.CompletedProcess:
    cmd = [
        "/opt/mssql-tools18/bin/sqlcmd",
        "-C",
        "-S", f"tcp:{DB_HOST},{DB_PORT}",
        "-U", DB_USER,
        "-P", DB_PASSWORD,
        "-b",
        "-l", "30",
    ]
    if db:
        cmd += ["-d", db]
    cmd += args
    return subprocess.run(cmd, text=True, capture_output=capture)


def wait_for_server(timeout_s: int = 180) -> None:
    deadline = time.time() + timeout_s
    last_err = ""
    while time.time() < deadline:
        proc = sqlcmd(["-Q", "SELECT 1"], db="master")
        if proc.returncode == 0:
            print(f"[dbinit] mssql up at {DB_HOST}:{DB_PORT}", flush=True)
            return
        last_err = (proc.stderr or proc.stdout or "").strip()
        print("[dbinit] waiting for mssql ...", flush=True)
        time.sleep(3)
    sys.exit(f"[dbinit] timed out waiting for mssql; last error: {last_err}")


def schema_sha256() -> str:
    h = hashlib.sha256()
    with SCHEMA_PATH.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def ensure_database() -> None:
    q = (
        f"IF DB_ID(N'{DB_NAME}') IS NULL "
        f"CREATE DATABASE [{DB_NAME}] COLLATE Latin1_General_CI_AS;"
    )
    proc = sqlcmd(["-Q", q], db="master")
    if proc.returncode != 0:
        sys.exit(f"[dbinit] could not create database: {proc.stderr or proc.stdout}")


def drop_and_recreate_database() -> None:
    q = (
        f"IF DB_ID(N'{DB_NAME}') IS NOT NULL BEGIN "
        f"  ALTER DATABASE [{DB_NAME}] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; "
        f"  DROP DATABASE [{DB_NAME}]; "
        f"END; "
        f"CREATE DATABASE [{DB_NAME}] COLLATE Latin1_General_CI_AS;"
    )
    proc = sqlcmd(["-Q", q], db="master")
    if proc.returncode != 0:
        sys.exit(f"[dbinit] could not recreate database: {proc.stderr or proc.stdout}")


def read_sentinel() -> str | None:
    q = (
        f"IF OBJECT_ID(N'dbo.{SENTINEL_TABLE}', 'U') IS NOT NULL "
        f"SELECT TOP 1 schema_sha FROM dbo.{SENTINEL_TABLE} "
        f"ORDER BY applied_at DESC;"
    )
    proc = sqlcmd(["-h", "-1", "-W", "-Q", q], db=DB_NAME)
    if proc.returncode != 0:
        return None
    for line in proc.stdout.splitlines():
        line = line.strip()
        if len(line) == 64 and all(c in "0123456789abcdef" for c in line):
            return line
    return None


def read_captured_source() -> str:
    if not CAPTURED_PATH.exists():
        return ""
    for line in CAPTURED_PATH.read_text().splitlines():
        if line.startswith("source:"):
            return line.split(":", 1)[1].strip()[:255]
    return ""


def write_sentinel(sha: str) -> None:
    source = read_captured_source().replace("'", "''")
    q = f"""
    IF OBJECT_ID(N'dbo.{SENTINEL_TABLE}', 'U') IS NULL
        CREATE TABLE dbo.{SENTINEL_TABLE} (
            id INT IDENTITY(1,1) PRIMARY KEY,
            schema_sha CHAR(64) NOT NULL,
            source NVARCHAR(255) NULL,
            applied_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
        );
    INSERT INTO dbo.{SENTINEL_TABLE} (schema_sha, source)
    VALUES ('{sha}', N'{source}');
    """
    proc = sqlcmd(["-Q", q], db=DB_NAME)
    if proc.returncode != 0:
        sys.exit(f"[dbinit] could not write sentinel: {proc.stderr or proc.stdout}")


_MSG_RE = re.compile(
    r"^Msg\s+(?P<num>\d+),\s+Level\s+(?P<level>\d+),\s+State\s+(?P<state>\d+)"
    r"(?:,\s+Server\s+\S+?)?"
    r"(?:,\s+Procedure\s+(?P<proc>\S+?))?"
    r"(?:,\s+Line\s+(?P<line>\d+))?\s*$"
)


def report_apply_errors(output: str) -> int:
    """Scan sqlcmd output for Msg ... error lines and print a summary.

    The sanitiser swaps `:on error exit` → `:on error ignore` so the session
    continues past per-batch errors (mainly stale view bodies on the source
    DB pointing at renamed tables). Errors are still printed inline by
    sqlcmd; here we group them under a clear summary banner so they're
    obvious in the dbinit log.

    Returns the number of distinct error batches reported.
    """
    lines = output.splitlines()
    errors: list[tuple[str, str]] = []
    for i, line in enumerate(lines):
        m = _MSG_RE.match(line)
        if not m:
            continue
        # The next non-empty line is the human message.
        detail = ""
        for j in range(i + 1, min(i + 4, len(lines))):
            if lines[j].strip():
                detail = lines[j].strip()
                break
        proc = m.group("proc") or "(batch)"
        ln = m.group("line") or "?"
        errors.append((f"{proc}:{ln}", detail))
    if not errors:
        return 0
    print(
        f"[dbinit] schema apply finished with {len(errors)} failing batch(es):",
        flush=True,
    )
    for where, detail in errors:
        print(f"[dbinit]   - {where}  {detail}", flush=True)
    return len(errors)


def apply_schema() -> None:
    print(f"[dbinit] sqlcmd -i {SCHEMA_PATH.name} → {DB_NAME} ...", flush=True)
    # No `-b`: schema.sql has `:on error ignore` (set by the sanitiser), so
    # the session continues past stale-view-body failures. We capture stdout
    # to surface a summary of failed batches at the end.
    proc = subprocess.run(
        [
            "/opt/mssql-tools18/bin/sqlcmd",
            "-C",
            "-S", f"tcp:{DB_HOST},{DB_PORT}",
            "-U", DB_USER,
            "-P", DB_PASSWORD,
            "-d", DB_NAME,
            "-v", f"DatabaseName={DB_NAME}",
            "-i", str(SCHEMA_PATH),
        ],
        text=True,
        capture_output=True,
    )
    sys.stdout.write(proc.stdout)
    sys.stderr.write(proc.stderr)
    combined = (proc.stdout or "") + "\n" + (proc.stderr or "")
    report_apply_errors(combined)
    if proc.returncode != 0:
        # sqlcmd should return 0 even with `:on error ignore` if it got to
        # the end of the script. A non-zero exit here means a process-level
        # failure (couldn't connect, file unreadable, etc.) — those we do
        # want to surface.
        sys.exit(f"[dbinit] sqlcmd apply failed with exit {proc.returncode}")


def apply_migrations_baseline() -> None:
    if not BASELINE_PATH.exists():
        print(
            f"[dbinit] no {BASELINE_PATH.name} — skipping baseline.\n"
            f"[dbinit] django_migrations will be empty; `manage.py migrate`\n"
            f"[dbinit] will try to run every migration against the captured\n"
            f"[dbinit] schema and most will fail. Run scripts/capture-schema.sh.",
            flush=True,
        )
        return
    print(f"[dbinit] sqlcmd -i {BASELINE_PATH.name} → {DB_NAME} ...", flush=True)
    proc = subprocess.run(
        [
            "/opt/mssql-tools18/bin/sqlcmd",
            "-C",
            "-S", f"tcp:{DB_HOST},{DB_PORT}",
            "-U", DB_USER,
            "-P", DB_PASSWORD,
            "-d", DB_NAME,
            "-v", f"DatabaseName={DB_NAME}",
            "-b",
            "-i", str(BASELINE_PATH),
        ],
        text=True,
        capture_output=True,
    )
    sys.stdout.write(proc.stdout)
    sys.stderr.write(proc.stderr)
    if proc.returncode != 0:
        sys.exit(
            f"[dbinit] migrations_baseline.sql apply failed with exit "
            f"{proc.returncode}: {proc.stderr or proc.stdout}"
        )


def load_legacy_directory(subdir: str) -> None:
    folder = DATABASE_DIR / subdir
    if not folder.is_dir():
        return
    files = sorted(folder.glob("*.sql"))
    if not files:
        return
    print(f"[dbinit] loading {len(files)} file(s) from {subdir}/", flush=True)
    for sql_file in files:
        proc = subprocess.run(
            [
                "/opt/mssql-tools18/bin/sqlcmd",
                "-C",
                "-S", f"tcp:{DB_HOST},{DB_PORT}",
                "-U", DB_USER,
                "-P", DB_PASSWORD,
                "-d", DB_NAME,
                "-i", str(sql_file),
            ],
            text=True,
        )
        status = "ok" if proc.returncode == 0 else f"FAILED (exit {proc.returncode})"
        print(f"[dbinit]   {sql_file.name}: {status}", flush=True)


def bootstrap() -> None:
    if os.environ.get("SKIP_BOOTSTRAP") == "1":
        ensure_database()
        print("[dbinit] SKIP_BOOTSTRAP=1 — leaving schema to Django migrate", flush=True)
        return

    if SCHEMA_PATH.exists():
        sha = schema_sha256()
        force = os.environ.get("FORCE_REINIT") == "1"
        current = None if force else read_sentinel()

        if current == sha:
            print(f"[dbinit] schema {sha[:12]} already applied — skipping", flush=True)
            return

        if current:
            print(
                f"[dbinit] sentinel says {current[:12]}, schema is {sha[:12]} — "
                f"dropping & re-applying", flush=True,
            )
        elif force:
            print("[dbinit] FORCE_REINIT=1 — dropping & re-applying", flush=True)
        else:
            print(f"[dbinit] applying schema {sha[:12]} to fresh database", flush=True)

        drop_and_recreate_database()
        apply_schema()
        apply_migrations_baseline()
        write_sentinel(sha)
        print(f"[dbinit] schema {sha[:12]} applied", flush=True)
        return

    # No snapshot — fall back behaviours.
    ensure_database()
    if LOAD_SCHEMA:
        print(
            "[dbinit] no snapshot — falling back to best-effort load of "
            "database/*.sql (LOAD_SCHEMA=1)",
            flush=True,
        )
        for subdir in LEGACY_LOAD_ORDER:
            load_legacy_directory(subdir)
    else:
        print(
            f"[dbinit] no snapshot at {SCHEMA_PATH} — skipping schema apply.\n"
            f"[dbinit] run scripts/capture-schema.sh (VPN required) to create one.\n"
            f"[dbinit] Django manage.py migrate will still build Django-managed tables.",
            flush=True,
        )


def main() -> None:
    wait_for_server()
    bootstrap()
    print("[dbinit] done", flush=True)


if __name__ == "__main__":
    main()
