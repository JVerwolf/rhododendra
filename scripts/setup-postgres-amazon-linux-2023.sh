#!/usr/bin/env bash
#
# Idempotent PostgreSQL 16 bootstrap for Amazon Linux 2023 (EC2 or local AL2023).
# Run once per host with sudo. Does not modify application code or deploy scripts.
#
# Prerequisites: Amazon Linux 2023, network for dnf.
#
# Optional: set POSTGRES_APP_PASSWORD to a strong secret before running. If set, creates
# role and database `rhododendra` when missing. If unset, installs and starts PostgreSQL only;
# create the role/database manually (see README).
#
# Usage:
#   sudo ./scripts/setup-postgres-amazon-linux-2023.sh
#
# On a non-Amazon-Linux-2023 system this script exits with an error unless you set
#   FORCE=1
# (for example to try the same package layout on a derivative image).
#
set -euo pipefail

PG_MAJOR=16
PG_SERVICE="postgresql-${PG_MAJOR}"
SETUP_BIN="/usr/pgsql-${PG_MAJOR}/bin/postgresql-${PG_MAJOR}-setup"
DATA_DIR="/var/lib/pgsql/${PG_MAJOR}/data"

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

if [[ -n "${POSTGRES_APP_PASSWORD:-}" ]]; then
  echo "[setup-postgres] Ensuring application role and database exist..."
  pass_sql_escaped=$(printf '%s' "${POSTGRES_APP_PASSWORD}" | sed "s/'/''/g")
  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='rhododendra'" | grep -q 1; then
    printf "CREATE ROLE rhododendra LOGIN PASSWORD '%s';\n" "${pass_sql_escaped}" | sudo -u postgres psql -v ON_ERROR_STOP=1
  else
    printf "ALTER ROLE rhododendra WITH PASSWORD '%s';\n" "${pass_sql_escaped}" | sudo -u postgres psql -v ON_ERROR_STOP=1
  fi
  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='rhododendra'" | grep -q 1; then
    sudo -u postgres createdb -O rhododendra rhododendra
  fi
  echo "[setup-postgres] Role and database rhododendra are ready (password from POSTGRES_APP_PASSWORD)."
else
  echo "[setup-postgres] POSTGRES_APP_PASSWORD not set — skipping role/database creation."
  echo "Create manually, for example:"
  echo "  sudo -u postgres createuser -P rhododendra"
  echo "  sudo -u postgres createdb -O rhododendra rhododendra"
fi

echo "[setup-postgres] Done. JDBC default: jdbc:postgresql://localhost:5432/rhododendra"
echo "Sanity check: pg_isready -h localhost -p 5432"
