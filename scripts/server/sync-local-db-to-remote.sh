#!/usr/bin/env bash
#
# Copy the local Rhododendra PostgreSQL database to prod or staging over SSH:
# pg_dump (custom format) locally, upload the dump, stop the remote app,
# pg_restore --clean, remove the dump, start the app.
#
# Does not deploy JAR, index, or secrets — use ./deploy.sh for that. This script
# is invoked standalone or at the end of deploy when SYNC_LOCAL_DB=1 or --sync-local-db.
#
# Remote PGPASSWORD: SPRING_DATASOURCE_PASSWORD in the secrets file (same defaults
# as deploy: ~/.config/rhododendra/<env>.env), or REMOTE_PGPASSWORD, or RHODODENDRA_SECRETS_FILE.
#
# Production: type "prod" when stdin is a TTY, or SKIP_PROD_CONFIRM=1 (required if not a TTY).
#
# Local pg_dump uses PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD (libpq).
#
# Usage:
#   ./scripts/server/sync-local-db-to-remote.sh [--secrets-file <path>] [<prod|staging>] <path-to-ssh-private-key>
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

SECRETS_FILE=""
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --secrets-file) SECRETS_FILE="${2:-}"; shift 2 ;;
    -h|--help)
      cat <<'EOF'
Usage: ./scripts/server/sync-local-db-to-remote.sh [--secrets-file <path>] [<prod|staging>] <path-to-ssh-private-key>

  prod is the default environment when only the SSH key is given (same as ./deploy.sh).

  Remote host: PROD_HOST / STAGING_HOST (defaults match deploy.sh). Remote layout:
  REMOTE_BASE_DIR, REMOTE_APP_DIR, etc. (same env vars as deploy/start).

  Requires pg_restore.sh already on the server (e.g. from a prior deploy).
EOF
      exit 0
      ;;
    --) shift; while [[ $# -gt 0 ]]; do POSITIONAL+=( "$1" ); shift; done ;;
    -*) echo "Unknown flag: $1" >&2; exit 1 ;;
    *) POSITIONAL+=( "$1" ); shift ;;
  esac
done
if (( ${#POSITIONAL[@]} > 0 )); then
  set -- "${POSITIONAL[@]}"
fi

case $# in
  1)
    ENVIRONMENT="prod"
    SSH_KEY_ARG="${1}"
    ;;
  2)
    ENVIRONMENT="${1}"
    SSH_KEY_ARG="${2}"
    if [[ "$ENVIRONMENT" != "prod" && "$ENVIRONMENT" != "staging" ]]; then
      echo "Invalid environment: $ENVIRONMENT (expected prod or staging)" >&2
      exit 1
    fi
    ;;
  *)
    echo "Usage: ./scripts/server/sync-local-db-to-remote.sh [--secrets-file <path>] <path-to-ssh-private-key>" >&2
    echo "       ./scripts/server/sync-local-db-to-remote.sh [--secrets-file <path>] <prod|staging> <path-to-ssh-private-key>" >&2
    exit 1
    ;;
esac

SSH_KEY="${SSH_KEY_ARG/#\~/$HOME}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "SSH private key file not found: $SSH_KEY" >&2
  exit 1
fi

REMOTE_USER="${REMOTE_USER:-ec2-user}"
STAGING_HOST="${STAGING_HOST:-44.237.219.139}"
PROD_HOST="${PROD_HOST:-54.212.15.213}"

if [[ "$ENVIRONMENT" == "prod" ]]; then
  HOST="$PROD_HOST"
  PROFILE="prod"
else
  HOST="$STAGING_HOST"
  PROFILE="staging"
fi

REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
REMOTE_BIN_DIR="${REMOTE_BIN_DIR:-$REMOTE_BASE_DIR/bin}"
REMOTE_TARGET="${REMOTE_USER}@${HOST}"
SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=20)

PG_BACKUP_LOCAL="${PG_BACKUP_LOCAL:-$REPO_ROOT/scripts/server/pg_backup.sh}"

if [[ -z "$SECRETS_FILE" && -n "${RHODODENDRA_SECRETS_FILE:-}" ]]; then
  SECRETS_FILE="$RHODODENDRA_SECRETS_FILE"
fi
if [[ -z "$SECRETS_FILE" ]]; then
  SECRETS_FILE="$HOME/.config/rhododendra/${ENVIRONMENT}.env"
fi

if [[ ! -f "$PG_BACKUP_LOCAL" ]]; then
  echo "Missing $PG_BACKUP_LOCAL" >&2
  exit 1
fi

REMOTE_PG_PASSWORD="${REMOTE_PGPASSWORD:-}"
if [[ -z "$REMOTE_PG_PASSWORD" ]]; then
  if [[ ! -r "$SECRETS_FILE" ]]; then
    echo "Cannot read secrets file for remote DB password: $SECRETS_FILE" >&2
    echo "Set REMOTE_PGPASSWORD or use --secrets-file / RHODODENDRA_SECRETS_FILE." >&2
    exit 1
  fi
  line="$(grep -E '^SPRING_DATASOURCE_PASSWORD=' "$SECRETS_FILE" | tail -n1 || true)"
  if [[ -z "$line" ]]; then
    echo "SPRING_DATASOURCE_PASSWORD not found in $SECRETS_FILE" >&2
    exit 1
  fi
  REMOTE_PG_PASSWORD="${line#SPRING_DATASOURCE_PASSWORD=}"
  REMOTE_PG_PASSWORD="${REMOTE_PG_PASSWORD%$'\r'}"
  if [[ "$REMOTE_PG_PASSWORD" == \"*\" ]]; then
    REMOTE_PG_PASSWORD="${REMOTE_PG_PASSWORD#\"}"
    REMOTE_PG_PASSWORD="${REMOTE_PG_PASSWORD%\"}"
  elif [[ "$REMOTE_PG_PASSWORD" == \'*\' ]]; then
    REMOTE_PG_PASSWORD="${REMOTE_PG_PASSWORD#\'}"
    REMOTE_PG_PASSWORD="${REMOTE_PG_PASSWORD%\'}"
  fi
fi

if [[ "$ENVIRONMENT" == "prod" ]]; then
  if [[ "${SKIP_PROD_CONFIRM:-}" == "1" ]]; then
    :
  elif [[ -t 0 ]]; then
    read -r -p "Overwrite PRODUCTION database from local pg_dump. Type prod to continue: " reply
    if [[ "$reply" != "prod" ]]; then
      echo "Aborted." >&2
      exit 1
    fi
  else
    echo "Refusing to overwrite prod without a TTY or SKIP_PROD_CONFIRM=1." >&2
    exit 1
  fi
fi

LOCAL_DUMP="$(mktemp "${TMPDIR:-/tmp}/rhododendra-${ENVIRONMENT}-dump.XXXXXX.dump")"
cleanup_local_dump() {
  rm -f "$LOCAL_DUMP"
}
trap cleanup_local_dump EXIT
chmod 600 "$LOCAL_DUMP"

echo "[sync-db] Target: $ENVIRONMENT ($REMOTE_TARGET)"
echo "[sync-db] Dumping local PostgreSQL to $LOCAL_DUMP …"
"$PG_BACKUP_LOCAL" "$LOCAL_DUMP"

REMOTE_DUMP="/tmp/rhododendra-restore-${ENVIRONMENT}-$(date -u +%Y%m%dT%H%M%SZ)-$$.dump"
echo "[sync-db] Uploading dump to ${REMOTE_TARGET}:${REMOTE_DUMP} …"
scp "${SSH_OPTS[@]}" "$LOCAL_DUMP" "$REMOTE_TARGET:$REMOTE_DUMP"

pw_q="$(printf '%q' "$REMOTE_PG_PASSWORD")"
dump_q="$(printf '%q' "$REMOTE_DUMP")"
rb_q="$(printf '%q' "$REMOTE_BASE_DIR")"
ra_q="$(printf '%q' "$REMOTE_APP_DIR")"
rd_q="$(printf '%q' "$REMOTE_DATA_DIR")"
bin_q="$(printf '%q' "$REMOTE_BIN_DIR")"
profile_q="$(printf '%q' "$PROFILE")"

echo "[sync-db] Stopping app, pg_restore, removing remote dump, starting app (PROFILE=$PROFILE) …"
# shellcheck disable=SC2087
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" bash -s <<EOF
set -euo pipefail
REMOTE_DUMP=${dump_q}
cleanup() { rm -f "\$REMOTE_DUMP"; }
trap cleanup EXIT
REMOTE_BASE_DIR=${rb_q} REMOTE_APP_DIR=${ra_q} JAR_PATH=${ra_q}/rhododendra-0.0.1-SNAPSHOT.jar ${bin_q}/stop.sh
export PGDATABASE="\${PGDATABASE:-rhododendra}"
export PGUSER="\${PGUSER:-rhododendra}"
export PGPASSWORD=${pw_q}
${bin_q}/pg_restore.sh "\$REMOTE_DUMP"
trap - EXIT
rm -f "\$REMOTE_DUMP"
PROFILE=${profile_q} REMOTE_BASE_DIR=${rb_q} REMOTE_APP_DIR=${ra_q} REMOTE_DATA_DIR=${rd_q} JAR_PATH=${ra_q}/rhododendra-0.0.1-SNAPSHOT.jar LOG_PATH=${ra_q}/log.log ${bin_q}/start.sh
EOF

echo "[sync-db] Done."
