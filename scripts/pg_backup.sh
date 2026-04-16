#!/usr/bin/env bash
#
# Create a PostgreSQL logical backup (custom format) for Rhododendra.
# Run on the machine where PostgreSQL is installed (local dev or EC2).
#
# Usage:
#   PGDATABASE=rhododendra PGUSER=rhododendra ./scripts/pg_backup.sh /path/to/backup.dump
#
set -euo pipefail
OUT="${1:?output file path required}"
export PGDATABASE="${PGDATABASE:-rhododendra}"
export PGUSER="${PGUSER:-rhododendra}"
pg_dump -Fc -f "$OUT"
