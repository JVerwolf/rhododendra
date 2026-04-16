#!/usr/bin/env bash
#
# Idempotent PostgreSQL 16 bootstrap for macOS (Homebrew).
# Safe to re-run from the repo root or any directory.
#
# Prerequisites: Homebrew (https://brew.sh). Run as your normal macOS user — not root
# (Homebrew and brew services LaunchAgents expect a logged-in GUI user).
#
# Optional environment:
#   POSTGRES_APP_PASSWORD — password for role rhododendra (default: rhododendra, matches local
#     defaults in application.properties / migrateAndIndex Gradle properties).
#   PGHOST / PGPORT — override if PostgreSQL listens somewhere other than localhost:5432.
#
# Usage:
#   ./scripts/dev/setup-postgres-macos.sh
#
set -euo pipefail

readonly PG_FORMULA="postgresql@16"
readonly APP_USER="rhododendra"
readonly APP_DB="rhododendra"
readonly DEFAULT_PASSWORD="rhododendra"
readonly PG_HOST="${PGHOST:-localhost}"
readonly PG_PORT="${PGPORT:-5432}"

if [[ "$(id -u)" -eq 0 ]]; then
  echo "Error: do not run as root. Use your macOS login user so Homebrew and brew services work." >&2
  exit 1
fi

if ! command -v brew >/dev/null 2>&1; then
  echo "Error: Homebrew (brew) not found. Install from https://brew.sh" >&2
  exit 1
fi

escape_sql_literal() {
  printf '%s' "$1" | sed "s/'/''/g"
}

readonly APP_PASSWORD="${POSTGRES_APP_PASSWORD:-$DEFAULT_PASSWORD}"
APP_PASSWORD_SQL="$(escape_sql_literal "${APP_PASSWORD}")"
readonly APP_PASSWORD_SQL

export HOMEBREW_NO_AUTO_UPDATE="${HOMEBREW_NO_AUTO_UPDATE:-1}"

echo "[setup-postgres-macos] Ensuring ${PG_FORMULA} is installed..."
if ! brew list --formula "${PG_FORMULA}" >/dev/null 2>&1; then
  brew install "${PG_FORMULA}"
fi

PG_PREFIX="$(brew --prefix "${PG_FORMULA}")"
readonly PG_PREFIX
readonly PG_BIN="${PG_PREFIX}/bin"
export PATH="${PG_BIN}:${PATH}"

echo "[setup-postgres-macos] Ensuring brew service is started..."
brew services start "${PG_FORMULA}" >/dev/null 2>&1 || true

wait_for_postgres() {
  local i
  for i in $(seq 1 40); do
    if "${PG_BIN}/pg_isready" -h "${PG_HOST}" -p "${PG_PORT}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

kickstart_launch_agent() {
  # Homebrew registers a LaunchAgent; it can be "loaded" but not running until kickstarted
  # (common right after install or restart). Label matches: brew services list output.
  local label="homebrew.mxcl.${PG_FORMULA}"
  launchctl kickstart -k "gui/$(id -u)/${label}" 2>/dev/null || true
}

if ! wait_for_postgres; then
  echo "[setup-postgres-macos] Server not ready; trying launchctl kickstart for LaunchAgent..."
  kickstart_launch_agent
fi

if ! wait_for_postgres; then
  echo "Error: PostgreSQL is not accepting connections on ${PG_HOST}:${PG_PORT}." >&2
  echo "Try: brew services info ${PG_FORMULA}" >&2
  echo "If Brew uses a non-default port, set PGPORT before re-running this script." >&2
  exit 1
fi

echo "[setup-postgres-macos] Ensuring role and database exist..."

psql_base=( "${PG_BIN}/psql" -h "${PG_HOST}" -p "${PG_PORT}" -d postgres -v ON_ERROR_STOP=1 )

if "${psql_base[@]}" -tAc "SELECT 1 FROM pg_roles WHERE rolname='${APP_USER}'" | grep -q 1; then
  printf "ALTER ROLE %s WITH PASSWORD '%s';\n" "${APP_USER}" "${APP_PASSWORD_SQL}" | "${psql_base[@]}"
else
  printf "CREATE ROLE %s WITH LOGIN PASSWORD '%s';\n" "${APP_USER}" "${APP_PASSWORD_SQL}" | "${psql_base[@]}"
fi

if "${psql_base[@]}" -tAc "SELECT 1 FROM pg_database WHERE datname='${APP_DB}'" | grep -q 1; then
  "${psql_base[@]}" -c "ALTER DATABASE ${APP_DB} OWNER TO ${APP_USER};"
else
  "${PG_BIN}/createdb" -h "${PG_HOST}" -p "${PG_PORT}" -O "${APP_USER}" "${APP_DB}"
fi

echo "[setup-postgres-macos] Done."
echo "  JDBC: jdbc:postgresql://${PG_HOST}:${PG_PORT}/${APP_DB}"
echo "  User: ${APP_USER}"
echo "  Add to shell profile (optional): export PATH=\"${PG_BIN}:\$PATH\""
echo "  Client check: ${PG_BIN}/pg_isready -h ${PG_HOST} -p ${PG_PORT}"
