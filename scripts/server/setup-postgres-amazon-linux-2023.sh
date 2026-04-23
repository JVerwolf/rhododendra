#!/usr/bin/env bash
#
# Idempotent PostgreSQL bootstrap for Amazon Linux 2023.
# Run with sudo. Safe to re-run: package install, cluster init, service enable/start,
# pg_hba local-auth hardening, readiness wait, and optional role/database creation.
#
# Optional environment:
#   PG_MAJOR               Desired major (default: 15 on AL2023 today).
#   POSTGRES_APP_PASSWORD  If set, ensures role/database `rhododendra` and updates password.
#   PGHOST / PGPORT        Readiness target (default localhost:5432).
#   FORCE=1                Allow running on non-AL2023 hosts (unsupported).
#
set -euo pipefail

readonly PG_MAJOR="${PG_MAJOR:-15}"
readonly APP_USER="rhododendra"
readonly APP_DB="rhododendra"
readonly PG_HOST="${PGHOST:-localhost}"
readonly PG_PORT="${PGPORT:-5432}"

readonly PACKAGE_SERVER="postgresql${PG_MAJOR}-server"
readonly PACKAGE_CLIENT="postgresql${PG_MAJOR}"
readonly DATA_DIR="/var/lib/pgsql/data"

escape_sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

psql_as_postgres() {
  sudo -u postgres psql -d postgres -v ON_ERROR_STOP=1 "$@"
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

echo "[setup-postgres] Installing PostgreSQL ${PG_MAJOR} packages (${PACKAGE_SERVER}, ${PACKAGE_CLIENT})..."
dnf install -y "${PACKAGE_SERVER}" "${PACKAGE_CLIENT}"

# AL2023 exposes postgresql-setup in /usr/bin for distro-packaged PostgreSQL.
if [[ ! -x /usr/bin/postgresql-setup ]]; then
  echo "Expected setup binary missing: /usr/bin/postgresql-setup" >&2
  exit 1
fi

if [[ ! -f "${DATA_DIR}/PG_VERSION" ]]; then
  echo "[setup-postgres] Initializing database cluster..."
  /usr/bin/postgresql-setup --initdb
else
  echo "[setup-postgres] Data directory already initialized: ${DATA_DIR}"
fi

echo "[setup-postgres] Enabling and starting postgresql.service..."
systemctl enable --now postgresql.service

if ! systemctl is-active --quiet postgresql.service; then
  echo "PostgreSQL service failed to start. Check: journalctl -u postgresql -e" >&2
  exit 1
fi

configure_pg_hba() {
  local hba_file backup_file
  hba_file="$(psql_as_postgres -tAc "SHOW hba_file;" | xargs)"
  if [[ -z "$hba_file" ]]; then
    echo "Unable to determine hba_file from PostgreSQL." >&2
    exit 1
  fi

  backup_file="${hba_file}.bak.$(date -u +%Y%m%dT%H%M%SZ)"
  cp -a "$hba_file" "$backup_file"

  # Keep postgres peer auth for local admin; require passwords for app/local TCP.
  cat > "$hba_file" <<'HBA_EOF'
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             postgres                                peer
local   all             all                                     scram-sha-256
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
local   replication     all                                     peer
host    replication     all             127.0.0.1/32            scram-sha-256
host    replication     all             ::1/128                 scram-sha-256
HBA_EOF
  chown postgres:postgres "$hba_file"
  chmod 600 "$hba_file"
  systemctl reload postgresql.service
  echo "[setup-postgres] Updated pg_hba.conf (backup: ${backup_file})."
}

configure_pg_hba

wait_for_postgres() {
  local i
  for i in $(seq 1 40); do
    if sudo -u postgres pg_isready -h "${PG_HOST}" -p "${PG_PORT}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

echo "[setup-postgres] Waiting for PostgreSQL to accept connections (${PG_HOST}:${PG_PORT})..."
if ! wait_for_postgres; then
  echo "Error: PostgreSQL did not become ready on ${PG_HOST}:${PG_PORT} within the timeout." >&2
  echo "Check: journalctl -u postgresql -e" >&2
  echo "If the server uses a different address or port, set PGHOST/PGPORT and re-run." >&2
  exit 1
fi

if [[ -n "${POSTGRES_APP_PASSWORD:-}" ]]; then
  echo "[setup-postgres] Ensuring application role and database exist..."
  pass_sql_escaped="$(escape_sql_literal "${POSTGRES_APP_PASSWORD}")"

  if psql_as_postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='${APP_USER}'" | grep -q 1; then
    printf "ALTER ROLE %s WITH LOGIN PASSWORD '%s';\n" "${APP_USER}" "${pass_sql_escaped}" | psql_as_postgres
  else
    printf "CREATE ROLE %s WITH LOGIN PASSWORD '%s';\n" "${APP_USER}" "${pass_sql_escaped}" | psql_as_postgres
  fi

  if psql_as_postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${APP_DB}'" | grep -q 1; then
    psql_as_postgres -c "ALTER DATABASE ${APP_DB} OWNER TO ${APP_USER};"
  else
    sudo -u postgres createdb -O "${APP_USER}" "${APP_DB}"
  fi

  psql_as_postgres -c "GRANT ALL PRIVILEGES ON DATABASE ${APP_DB} TO ${APP_USER};"
  echo "[setup-postgres] Role and database ${APP_DB} are ready (password from POSTGRES_APP_PASSWORD)."
else
  echo "[setup-postgres] POSTGRES_APP_PASSWORD not set — skipping role/database creation."
  echo "Create manually, for example:"
  echo "  sudo -u postgres createuser -P ${APP_USER}"
  echo "  sudo -u postgres createdb -O ${APP_USER} ${APP_DB}"
fi

echo "[setup-postgres] Done. JDBC default: jdbc:postgresql://${PG_HOST}:${PG_PORT}/${APP_DB}"
echo "Sanity check: sudo -u postgres pg_isready -h ${PG_HOST} -p ${PG_PORT}"
