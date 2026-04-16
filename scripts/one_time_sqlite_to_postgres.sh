#!/usr/bin/env bash
#
# One-time paths to move from the old SQLite deployment to PostgreSQL:
#
# 1) Source of truth is JSON (scraper output): point SPRING_DATASOURCE_* at an empty Postgres
#    database, then run:
#      ./gradlew migrateAndIndex -PdataJsonDir=/path/to/json
#    This rebuilds tables and loads all rows from JSON.
#
# 2) You only have a legacy .sqlite file: use a third-party loader (e.g. pgloader) from SQLite
#    to PostgreSQL, or export CSV from SQLite and import with COPY. Schema types differ; prefer (1)
#    when the JSON directory is available.
#
# 3) Backups after cutover: use pg_dump (see scripts/server/pg_backup.sh).
#
echo "This script documents migration options; read the comments in the source."
exit 0
