#!/usr/bin/env bash
#
# Idempotent PostgreSQL 16 bootstrap for Amazon Linux 2023 (EC2 or local AL2023).
# Run with sudo. Safe to re-run: packages, cluster init, systemd enable/start, readiness wait,
# and (when POSTGRES_APP_PASSWORD is set) role/database/password/owner alignment.
#
# Prerequisites: Amazon Linux 2023, network for dnf.
#
# Optional environment:
#   POSTGRES_APP_PASSWORD — if set, ensures role and database `rhododendra` exist and applies
#     this password (CREATE ROLE or ALTER ROLE). If unset, only installs/starts PostgreSQL.
#   PGHOST / PGPORT — used only for pg_isready readiness checks (default localhost:5432).
#     Admin SQL still uses the local Unix socket as the postgres OS user (peer auth).
#
# Usage (from repo checkout):
#   sudo ./scripts/server/setup-postgres-amazon-linux-2023.sh
#   sudo -E env POSTGRES_APP_PASSWORD='your-strong-secret' ./scripts/server/setup-postgres-amazon-linux-2023.sh
#
# On EC2 after deploy, the same file is copied to REMOTE_BIN_DIR (default ~/rhododendra/bin/).
#
# On a non-Amazon-Linux-2023 system this script exits with an error unless you set
#   FORCE=1
# (for example to try the same package layout on a derivative image).
#
set -euo pipefail

readonly PG_MAJOR=16
readonly APP_USER="rhododendra"
readonly APP_DB="rhododendra"
readonly PG_SERVICE="postgresql-${PG_MAJOR}"
readonly SETUP_BIN="/usr/pgsql-${PG_MAJOR}/bin/postgresql-${PG_MAJOR}-setup"
readonly DATA_DIR="/var/lib/pgsql/${PG_MAJOR}/data"
readonly PG_BINDIR="/usr/pgsql-${PG_MAJOR}/bin"
readonly PG_HOST="${PGHOST:-localhost}"
readonly PG_PORT="${PGPORT:-5432}"

escape_sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

psql_as_postgres() {
  sudo -u postgres "${PG_BINDIR}/psql" -d postgres -v ON_ERROR_STOP=1 "$@"
}

if [[ "${EUID:-}" -ne 0 ]]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

if [[ -f /etc/os-release ]]; then
  # shellcheck source=/dev/null
  . /etc/os-release
  if [[ "${ID:-}" != "amzn" ]] || [[ "${VERSION_ID:-}" != 2023* ]]; then
    echo "Error: this script is intended for Amazon Linux 2023 (found ID=${ID:-unknown} VERSION_ID=${VERSION_ID:-unknown})." >&2
    if [[ "${FORCE:-}" != "1" ]]; then
      echo "Set FORCE=1 to run anyway (unsupported)." >&2
      exit 1
    fi
    echo "[setup-postgres] FORCE=1 set — continuing (unsupported)." >&2
  fi
fi

echo "[setup-postgres] Installing PostgreSQL ${PG_MAJOR} packages..."
dnf install -y "postgresql${PG_MAJOR}-server" "postgresql${PG_MAJOR}"

if [[ ! -x "$SETUP_BIN" ]]; then
  echo "Expected setup binary missing: $SETUP_BIN (is postgresql${PG_MAJOR}-server installed?)" >&2
  exit 1
fi

if [[ ! -f "${DATA_DIR}/PG_VERSION" ]]; then
  echo "[setup-postgres] Initializing database cluster..."
  "$SETUP_BIN" initdb
else
  echo "[setup-postgres] Data directory already initialized: ${DATA_DIR}"
fi

echo "[setup-postgres] Enabling and starting ${PG_SERVICE}..."
systemctl enable --now "${PG_SERVICE}.service"

if ! systemctl is-active --quiet "${PG_SERVICE}.service"; then
  echo "PostgreSQL service failed to start. Check: journalctl -u ${PG_SERVICE} -e" >&2
  exit 1
fi

echo "[setup-postgres] Server status:"
systemctl status "${PG_SERVICE}.service" --no-pager || true

wait_for_postgres() {
  local i
  for i in $(seq 1 40); do
    if sudo -u postgres "${PG_BINDIR}/pg_isready" -h "${PG_HOST}" -p "${PG_PORT}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

echo "[setup-postgres] Waiting for PostgreSQL to accept connections (${PG_HOST}:${PG_PORT})..."
if ! wait_for_postgres; then
  echo "Error: PostgreSQL did not become ready on ${PG_HOST}:${PG_PORT} within the timeout." >&2
  echo "Check: journalctl -u ${PG_SERVICE} -e" >&2
  echo "If the server uses a different address or port, set PGHOST/PGPORT and re-run." >&2
  exit 1
fi

if [[ -n "${POSTGRES_APP_PASSWORD:-}" ]]; then
  echo "[setup-postgres] Ensuring application role and database exist..."
  pass_sql_escaped="$(escape_sql_literal "${POSTGRES_APP_PASSWORD}")"

  if psql_as_postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='${APP_USER}'" | grep -q 1; then
    printf "ALTER ROLE %s WITH PASSWORD '%s';\n" "${APP_USER}" "${pass_sql_escaped}" | psql_as_postgres
  else
    printf "CREATE ROLE %s WITH LOGIN PASSWORD '%s';\n" "${APP_USER}" "${pass_sql_escaped}" | psql_as_postgres
  fi

  if psql_as_postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${APP_DB}'" | grep -q 1; then
    psql_as_postgres -c "ALTER DATABASE ${APP_DB} OWNER TO ${APP_USER};"
  else
    sudo -u postgres "${PG_BINDIR}/createdb" -O "${APP_USER}" "${APP_DB}"
  fi

  echo "[setup-postgres] Role and database ${APP_DB} are ready (password from POSTGRES_APP_PASSWORD)."
else
  echo "[setup-postgres] POSTGRES_APP_PASSWORD not set — skipping role/database creation."
  echo "Create manually, for example:"
  echo "  sudo -u postgres createuser -P ${APP_USER}"
  echo "  sudo -u postgres createdb -O ${APP_USER} ${APP_DB}"
fi

echo "[setup-postgres] Done. JDBC default: jdbc:postgresql://${PG_HOST}:${PG_PORT}/${APP_DB}"
echo "Sanity check: sudo -u postgres ${PG_BINDIR}/pg_isready -h ${PG_HOST} -p ${PG_PORT}"
