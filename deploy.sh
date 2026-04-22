#!/bin/bash
#
# Deploy Rhododendra to EC2: upload JAR + scripts, sync Lucene index, then restart the app.
# PostgreSQL runs on the server separately; configure secrets on the host (see README).
# Never add credential files to git or scp them from here — use /etc/… or AWS SSM/Secrets Manager.
#
# Optional overrides (defaults under scripts/server/): LOCAL_SERVER_SCRIPTS_DIR, SETUP_EC2_LOCAL,
# SETUP_POSTGRES_LOCAL, PG_BACKUP_LOCAL, PG_RESTORE_LOCAL
#
# Examples (SSH private key path is always a positional argument — required):
#   ./deploy.sh /path/to/ssh_private_key              # prod (default)
#   ./deploy.sh prod /path/to/ssh_private_key
#   ./deploy.sh staging /path/to/ssh_private_key
#
# Optional flags (any order; must appear BEFORE positionals are parsed):
#   --skip-secrets                Do not push /etc/rhododendra/rhododendra.env this run.
#   --secrets-file <path>         Override the local secrets source file.
#                                 Default: ~/.config/rhododendra/<env>.env
#
# Exit on error, treat unset variables as errors, and fail pipelines on first failing command.
set -euo pipefail

# Absolute path to this repo (so paths work no matter where you invoke the script from).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Arguments and toggles
# ---------------------------------------------------------------------------
# Parse optional --flags first, then the remaining positional args keep the
# prior contract: [<prod|staging>] <ssh-key-path>.
SKIP_SECRETS=0
SECRETS_FILE=""
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-secrets)   SKIP_SECRETS=1; shift ;;
    --secrets-file)   SECRETS_FILE="${2:-}"; shift 2 ;;
    -h|--help)
      echo "Usage: ./deploy.sh [--skip-secrets] [--secrets-file <path>] [<prod|staging>] <path-to-ssh-private-key>"
      exit 0
      ;;
    --) shift; while [[ $# -gt 0 ]]; do POSITIONAL+=( "$1" ); shift; done ;;
    -*) echo "Unknown flag: $1"; exit 1 ;;
    *)  POSITIONAL+=( "$1" ); shift ;;
  esac
done
# Guard the `set --` against an empty array. macOS ships bash 3.2, where
# "${POSITIONAL[@]}" on an empty array fails under `set -u`; on empty, we want
# to fall through to the 0-args case below with a friendly usage message.
if (( ${#POSITIONAL[@]} > 0 )); then
  set -- "${POSITIONAL[@]}"
fi

# Required positional arguments: SSH private key path (and optional prod|staging).
case $# in
  0)
    echo "Usage: ./deploy.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>"
    echo "       ./deploy.sh [--skip-secrets] [--secrets-file <path>] <prod|staging> <path-to-ssh-private-key>"
    exit 1
    ;;
  1)
    ENVIRONMENT="prod"
    SSH_KEY_ARG="${1}"
    ;;
  2)
    ENVIRONMENT="${1}"
    SSH_KEY_ARG="${2}"
    if [[ "$ENVIRONMENT" != "prod" && "$ENVIRONMENT" != "staging" ]]; then
      echo "Invalid environment: $ENVIRONMENT (expected prod or staging)"
      echo "Usage: ./deploy.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>"
      echo "       ./deploy.sh [--skip-secrets] [--secrets-file <path>] <prod|staging> <path-to-ssh-private-key>"
      exit 1
    fi
    ;;
  *)
    echo "Too many arguments."
    echo "Usage: ./deploy.sh [--skip-secrets] [--secrets-file <path>] <path-to-ssh-private-key>"
    echo "       ./deploy.sh [--skip-secrets] [--secrets-file <path>] <prod|staging> <path-to-ssh-private-key>"
    exit 1
    ;;
esac

SSH_KEY="${SSH_KEY_ARG/#\~/$HOME}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "SSH private key file not found: $SSH_KEY"
  exit 1
fi

# ---------------------------------------------------------------------------
# SSH and host selection (override host with env vars)
# ---------------------------------------------------------------------------
REMOTE_USER="${REMOTE_USER:-ec2-user}"
STAGING_HOST="${STAGING_HOST:-44.237.219.139}"
PROD_HOST="${PROD_HOST:-54.212.15.213}"

# ---------------------------------------------------------------------------
# Local paths: what we upload from this machine
# ---------------------------------------------------------------------------
LOCAL_JAR="${LOCAL_JAR:-$SCRIPT_DIR/build/libs/rhododendra-0.0.1-SNAPSHOT.jar}"
LOCAL_INDEX_DIR="${LOCAL_INDEX_DIR:-$SCRIPT_DIR/index}"
LOCAL_SERVER_SCRIPTS_DIR="${LOCAL_SERVER_SCRIPTS_DIR:-$SCRIPT_DIR/scripts/server}"
SETUP_EC2_LOCAL="${SETUP_EC2_LOCAL:-$LOCAL_SERVER_SCRIPTS_DIR/setup-ec2.sh}"
SETUP_POSTGRES_LOCAL="${SETUP_POSTGRES_LOCAL:-$LOCAL_SERVER_SCRIPTS_DIR/setup-postgres-amazon-linux-2023.sh}"
PG_BACKUP_LOCAL="${PG_BACKUP_LOCAL:-$LOCAL_SERVER_SCRIPTS_DIR/pg_backup.sh}"
PG_RESTORE_LOCAL="${PG_RESTORE_LOCAL:-$LOCAL_SERVER_SCRIPTS_DIR/pg_restore.sh}"
DEPLOY_SECRETS_LOCAL="${DEPLOY_SECRETS_LOCAL:-$LOCAL_SERVER_SCRIPTS_DIR/deploy-secrets.sh}"

# Default dev-side secrets source (NOT in the repo). One file per environment so
# a prod deploy cannot accidentally pick up staging credentials and vice versa.
# Override with `--secrets-file <path>` or RHODODENDRA_SECRETS_FILE env var.
if [[ -z "$SECRETS_FILE" && -n "${RHODODENDRA_SECRETS_FILE:-}" ]]; then
  SECRETS_FILE="$RHODODENDRA_SECRETS_FILE"
fi
if [[ -z "$SECRETS_FILE" ]]; then
  SECRETS_FILE="$HOME/.config/rhododendra/${ENVIRONMENT:-prod}.env"
fi

# ---------------------------------------------------------------------------
# Remote layout: JAR and log under app/; Lucene index under data/
# ---------------------------------------------------------------------------
REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
REMOTE_BIN_DIR="${REMOTE_BIN_DIR:-$REMOTE_BASE_DIR/bin}"
REMOTE_INDEX_DIR="${REMOTE_INDEX_DIR:-$REMOTE_DATA_DIR/index}"

# ---------------------------------------------------------------------------
# Map environment name to EC2 host and Spring profile (prod vs staging SSL/domain in the JAR)
# ---------------------------------------------------------------------------
if [[ "$ENVIRONMENT" == "prod" ]]; then
  HOST="$PROD_HOST"
  PROFILE="prod"
elif [[ "$ENVIRONMENT" == "staging" ]]; then
  HOST="$STAGING_HOST"
  PROFILE="staging"
fi

REMOTE_TARGET="${REMOTE_USER}@${HOST}"
# Fail fast instead of hanging on password prompts; don’t wait forever on a dead host.
# Encrypted keys still need ssh-agent (BatchMode cannot prompt for key passphrase).
SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=20)

# ---------------------------------------------------------------------------
# Preflight: fail fast if the build or required scripts are missing locally
# ---------------------------------------------------------------------------
for required in \
  "$LOCAL_JAR" \
  "$LOCAL_INDEX_DIR" \
  "$SCRIPT_DIR/start.sh" \
  "$SCRIPT_DIR/stop.sh" \
  "$SETUP_EC2_LOCAL" \
  "$SETUP_POSTGRES_LOCAL" \
  "$PG_BACKUP_LOCAL" \
  "$PG_RESTORE_LOCAL" \
  "$DEPLOY_SECRETS_LOCAL"; do
  if [[ ! -e "$required" ]]; then
    echo "Missing required file or directory: $required"
    exit 1
  fi
done

# Secrets preflight: catch a missing/unreadable/broken local file BEFORE we
# touch the server. We invoke deploy-secrets.sh in --dry-run mode so the *same*
# validation logic (mode 0600, required keys, parseability) runs here. Any
# subsequent failure during the real upload is then limited to remote/transport
# problems, which deploy-secrets.sh handles with backup+restore.
if (( SKIP_SECRETS == 0 )); then
  if [[ ! -r "$SECRETS_FILE" ]]; then
    echo ""
    echo "Secrets file missing or unreadable: $SECRETS_FILE"
    echo "  - Create it outside the repo (mode 0600) with the keys from .env.example, or"
    echo "  - Pass --secrets-file <path> to point at a different file, or"
    echo "  - Pass --skip-secrets to deploy code only (leaves existing server secrets intact)."
    exit 1
  fi
  echo "[deploy] Validating secrets file (dry-run)..."
  if ! "$DEPLOY_SECRETS_LOCAL" \
      --env "$ENVIRONMENT" \
      --ssh-key "$SSH_KEY" \
      --secrets-file "$SECRETS_FILE" \
      --dry-run; then
    echo ""
    echo "Secrets preflight failed; aborting before any remote changes."
    exit 1
  fi
fi

echo "Deploying to $ENVIRONMENT ($REMOTE_TARGET)"
echo "Remote base: $REMOTE_BASE_DIR"
echo "Remote app dir: $REMOTE_APP_DIR"
echo "Remote data dir: $REMOTE_DATA_DIR"
if (( SKIP_SECRETS )); then
  echo "Secrets:     SKIPPED (--skip-secrets); previous server file is unchanged"
else
  echo "Secrets:     $SECRETS_FILE -> $REMOTE_TARGET:/etc/rhododendra/rhododendra.env"
fi
echo ""

# ---------------------------------------------------------------------------
# Remote: ensure target directories exist before copying files
# ---------------------------------------------------------------------------
echo "[deploy] Creating remote directories..."
echo "[deploy] Connecting via SSH to ${REMOTE_TARGET} (mkdir); if this hangs, the server is not reachable on port 22."
if ! ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" "mkdir -p \"$REMOTE_APP_DIR\" \"$REMOTE_DATA_DIR\" \"$REMOTE_BIN_DIR\" \"$REMOTE_INDEX_DIR\""; then
  echo ""
  echo "Deploy failed: could not run SSH on ${REMOTE_TARGET}."
  echo "If you saw a timeout: the instance may be stopped, the public IP may have changed, port 22 may be"
  echo "blocked for your IP (security group), or you may need VPN/bastion access."
  echo "Sanity-check: ssh -i <your-private-key> ${REMOTE_TARGET} true"
  exit 1
fi
echo "[deploy] Remote directories OK."

# ---------------------------------------------------------------------------
# Upload application JAR and helper scripts (start/stop live under bin/)
# ---------------------------------------------------------------------------
echo "[deploy] Uploading JAR..."
scp "${SSH_OPTS[@]}" "$LOCAL_JAR" "$REMOTE_TARGET:$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar"
echo "[deploy] Uploading start.sh, stop.sh, and server maintenance scripts..."
scp "${SSH_OPTS[@]}" "$SCRIPT_DIR/start.sh" "$REMOTE_TARGET:$REMOTE_BIN_DIR/start.sh"
scp "${SSH_OPTS[@]}" "$SCRIPT_DIR/stop.sh" "$REMOTE_TARGET:$REMOTE_BIN_DIR/stop.sh"
scp "${SSH_OPTS[@]}" "$SETUP_EC2_LOCAL" "$REMOTE_TARGET:$REMOTE_BIN_DIR/setup-ec2.sh"
scp "${SSH_OPTS[@]}" "$SETUP_POSTGRES_LOCAL" "$REMOTE_TARGET:$REMOTE_BIN_DIR/setup-postgres-amazon-linux-2023.sh"
scp "${SSH_OPTS[@]}" "$PG_BACKUP_LOCAL" "$REMOTE_TARGET:$REMOTE_BIN_DIR/pg_backup.sh"
scp "${SSH_OPTS[@]}" "$PG_RESTORE_LOCAL" "$REMOTE_TARGET:$REMOTE_BIN_DIR/pg_restore.sh"

# ---------------------------------------------------------------------------
# Secrets: push the dev-side env file to /etc/rhododendra/rhododendra.env with
# root:root 0600, backup-before-replace, and cleanup on failure. Runs BEFORE the
# JVM restart so start.sh picks up the new values immediately. If this step
# fails, we abort the deploy and the JVM keeps running on the previous JAR and
# previous secrets (deploy-secrets.sh restores its own backup on remote errors).
# ---------------------------------------------------------------------------
if (( SKIP_SECRETS == 0 )); then
  echo "[deploy] Pushing secrets to /etc/rhododendra/rhododendra.env..."
  "$DEPLOY_SECRETS_LOCAL" \
    --env "$ENVIRONMENT" \
    --ssh-key "$SSH_KEY" \
    --secrets-file "$SECRETS_FILE" \
    --host "$HOST" \
    --remote-user "$REMOTE_USER"
else
  echo "[deploy] Skipping secrets upload (--skip-secrets)."
fi

# ---------------------------------------------------------------------------
# Lucene: mirror local index/ to the server; --delete removes stale segment files remotely
# ---------------------------------------------------------------------------
echo "[deploy] Syncing Lucene index (this can take a while; per-file progress below)..."
# Keep ssh flags in sync with SSH_OPTS (rsync invokes ssh separately).
rsync -az --delete --progress \
  -e "ssh -i \"$SSH_KEY\" -o BatchMode=yes -o ConnectTimeout=20" \
  "$LOCAL_INDEX_DIR/" "$REMOTE_TARGET:$REMOTE_INDEX_DIR/"

# ---------------------------------------------------------------------------
# Remote: make scripts executable
# ---------------------------------------------------------------------------
echo "[deploy] chmod scripts on server..."
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "chmod +x \"${REMOTE_BIN_DIR}/start.sh\" \"${REMOTE_BIN_DIR}/stop.sh\" \"${REMOTE_BIN_DIR}/setup-ec2.sh\" \"${REMOTE_BIN_DIR}/setup-postgres-amazon-linux-2023.sh\" \"${REMOTE_BIN_DIR}/pg_backup.sh\" \"${REMOTE_BIN_DIR}/pg_restore.sh\""

# ---------------------------------------------------------------------------
# Remote restart: stop old JVM, then start with PROFILE matching prod or staging
# ---------------------------------------------------------------------------
echo "[deploy] Restarting remote server..."
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "REMOTE_BASE_DIR=\"$REMOTE_BASE_DIR\" REMOTE_APP_DIR=\"$REMOTE_APP_DIR\" JAR_PATH=\"$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar\" \"$REMOTE_BIN_DIR/stop.sh\""
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "PROFILE=\"$PROFILE\" REMOTE_BASE_DIR=\"$REMOTE_BASE_DIR\" REMOTE_APP_DIR=\"$REMOTE_APP_DIR\" REMOTE_DATA_DIR=\"$REMOTE_DATA_DIR\" JAR_PATH=\"$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar\" LOG_PATH=\"$REMOTE_APP_DIR/log.log\" \"$REMOTE_BIN_DIR/start.sh\""

echo "Deploy complete."
