#!/usr/bin/env bash
#
# Deploy to staging, then replace the remote PostgreSQL database with a logical
# backup of your local database (pg_dump -Fc via scripts/server/pg_backup.sh).
#
# Order: local pg_dump -> ./deploy.sh staging -> remote stop -> pg_restore -> start.
# Remote PGPASSWORD is taken from SPRING_DATASOURCE_PASSWORD in the same secrets
# file deploy uses (~/.config/rhododendra/staging.env by default), unless you set
# REMOTE_PGPASSWORD in the environment.
#
# Local connection for pg_dump uses PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD
# (same as pg_backup.sh / libpq).
#
# Usage:
#   ./deploy-staging-with-local-db.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SKIP_SECRETS=0
SECRETS_FILE=""
SECRETS_FILE_FROM_FLAG=0
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-secrets)   SKIP_SECRETS=1; shift ;;
    --secrets-file)   SECRETS_FILE="${2:-}"; SECRETS_FILE_FROM_FLAG=1; shift 2 ;;
    -h|--help)
      cat <<'EOF'
Usage: ./deploy-staging-with-local-db.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>

  Deploys to staging (./deploy.sh staging …), uploads a fresh local pg_dump, then
  stops the app, runs pg_restore --clean on the server, and starts again with PROFILE=staging.

  Local DB: set PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD as needed for pg_dump.

  Remote DB password: set REMOTE_PGPASSWORD, or use SPRING_DATASOURCE_PASSWORD in the
  staging secrets file (default ~/.config/rhododendra/staging.env, overridable with
  --secrets-file or RHODODENDRA_SECRETS_FILE).
EOF
      exit 0
      ;;
    --) shift; while [[ $# -gt 0 ]]; do POSITIONAL+=( "$1" ); shift; done ;;
    -*) echo "Unknown flag: $1" >&2; exit 1 ;;
    *)  POSITIONAL+=( "$1" ); shift ;;
  esac
done
if (( ${#POSITIONAL[@]} > 0 )); then
  set -- "${POSITIONAL[@]}"
fi

if [[ $# -ne 1 ]]; then
  echo "Usage: ./deploy-staging-with-local-db.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>" >&2
  exit 1
fi

SSH_KEY="${1/#\~/$HOME}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "SSH private key file not found: $SSH_KEY" >&2
  exit 1
fi

REMOTE_USER="${REMOTE_USER:-ec2-user}"
STAGING_HOST="${STAGING_HOST:-44.237.219.139}"
REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
REMOTE_BIN_DIR="${REMOTE_BIN_DIR:-$REMOTE_BASE_DIR/bin}"
REMOTE_TARGET="${REMOTE_USER}@${STAGING_HOST}"
SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=20)

PG_BACKUP_LOCAL="${PG_BACKUP_LOCAL:-$SCRIPT_DIR/scripts/server/pg_backup.sh}"

if [[ -z "$SECRETS_FILE" && -n "${RHODODENDRA_SECRETS_FILE:-}" ]]; then
  SECRETS_FILE="$RHODODENDRA_SECRETS_FILE"
fi
if [[ -z "$SECRETS_FILE" ]]; then
  SECRETS_FILE="$HOME/.config/rhododendra/staging.env"
fi

if [[ ! -f "$PG_BACKUP_LOCAL" ]]; then
  echo "Missing $PG_BACKUP_LOCAL" >&2
  exit 1
fi

REMOTE_PG_PASSWORD="${REMOTE_PGPASSWORD:-}"
if [[ -z "$REMOTE_PG_PASSWORD" ]]; then
  if [[ ! -r "$SECRETS_FILE" ]]; then
    echo "Cannot read secrets file for remote DB password: $SECRETS_FILE" >&2
    echo "Set REMOTE_PGPASSWORD or fix --secrets-file / RHODODENDRA_SECRETS_FILE." >&2
    exit 1
  fi
  line="$(grep -E '^SPRING_DATASOURCE_PASSWORD=' "$SECRETS_FILE" | tail -n1 || true)"
  if [[ -z "$line" ]]; then
    echo "SPRING_DATASOURCE_PASSWORD not found in $SECRETS_FILE" >&2
    echo "Add it or export REMOTE_PGPASSWORD before running." >&2
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

LOCAL_DUMP="$(mktemp "${TMPDIR:-/tmp}/rhododendra-staging-dump.XXXXXX.dump")"
cleanup_local_dump() {
  rm -f "$LOCAL_DUMP"
}
trap cleanup_local_dump EXIT
chmod 600 "$LOCAL_DUMP"

echo "[deploy-staging-db] Dumping local PostgreSQL to $LOCAL_DUMP …"
"$PG_BACKUP_LOCAL" "$LOCAL_DUMP"

DEPLOY_EXTRA=()
(( SKIP_SECRETS )) && DEPLOY_EXTRA+=( --skip-secrets )
(( SECRETS_FILE_FROM_FLAG )) && DEPLOY_EXTRA+=( --secrets-file "$SECRETS_FILE" )
DEPLOY_ARGS=(./deploy.sh "${DEPLOY_EXTRA[@]}" staging "$SSH_KEY")

echo "[deploy-staging-db] Running ${DEPLOY_ARGS[*]} …"
(cd "$SCRIPT_DIR" && "${DEPLOY_ARGS[@]}")

REMOTE_DUMP="/tmp/rhododendra-restore-$(date -u +%Y%m%dT%H%M%SZ)-$$.dump"
echo "[deploy-staging-db] Uploading dump to ${REMOTE_TARGET}:${REMOTE_DUMP} …"
scp "${SSH_OPTS[@]}" "$LOCAL_DUMP" "$REMOTE_TARGET:$REMOTE_DUMP"

pw_q="$(printf '%q' "$REMOTE_PG_PASSWORD")"
dump_q="$(printf '%q' "$REMOTE_DUMP")"
rb_q="$(printf '%q' "$REMOTE_BASE_DIR")"
ra_q="$(printf '%q' "$REMOTE_APP_DIR")"
rd_q="$(printf '%q' "$REMOTE_DATA_DIR")"
bin_q="$(printf '%q' "$REMOTE_BIN_DIR")"

echo "[deploy-staging-db] Stopping app, restoring database, removing remote dump, starting app …"
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
PROFILE=staging REMOTE_BASE_DIR=${rb_q} REMOTE_APP_DIR=${ra_q} REMOTE_DATA_DIR=${rd_q} JAR_PATH=${ra_q}/rhododendra-0.0.1-SNAPSHOT.jar LOG_PATH=${ra_q}/log.log ${bin_q}/start.sh
EOF

echo "[deploy-staging-db] Done."
