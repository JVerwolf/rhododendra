#!/usr/bin/env bash
#
# Restore a PostgreSQL logical backup in custom format (from scripts/server/pg_backup.sh).
# Run on the machine where PostgreSQL is installed (local dev or EC2).
#
# Drops and recreates objects in the target database (--clean --if-exists). Ensure
# you have a current backup before running against production.
#
# Usage:
#   PGDATABASE=rhododendra PGUSER=rhododendra ./scripts/server/pg_restore.sh /path/to/backup.dump
#
set -euo pipefail
IN="${1:?backup file path required}"
export PGDATABASE="${PGDATABASE:-rhododendra}"
export PGUSER="${PGUSER:-rhododendra}"
pg_restore --clean --if-exists -d "$PGDATABASE" "$IN"
