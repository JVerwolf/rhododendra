#!/bin/bash
#
# Deploy Rhododendra to EC2: upload JAR + scripts, sync Lucene index, optionally sync SQLite,
# then stop the running app and start the new build.
#
# Examples (SSH private key path is always a positional argument — required):
#   ./deploy.sh /path/to/ssh_private_key              # prod (default)
#   ./deploy.sh prod /path/to/ssh_private_key
#   ./deploy.sh staging /path/to/ssh_private_key
#   SYNC_DB=true ./deploy.sh prod /path/to/ssh_private_key
#   SYNC_DB=true LOCAL_DB_PATH=/path/to/db.sqlite ./deploy.sh staging /path/to/ssh_private_key
#   BACKUP_REMOTE=false ./deploy.sh prod /path/to/ssh_private_key
#
# Exit on error, treat unset variables as errors, and fail pipelines on first failing command.
set -euo pipefail

# Absolute path to this repo (so paths work no matter where you invoke the script from).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Arguments and toggles
# ---------------------------------------------------------------------------
# Positional args:
#   ./deploy.sh <path-to-ssh-private-key>                     → environment prod
#   ./deploy.sh <prod|staging> <path-to-ssh-private-key>
# SYNC_DB=true copies local SQLite to the server after deploy (optional; large / sensitive).
# Requires an interactive terminal: you must type yes when prompted (no automated bypass).
SYNC_DB="${SYNC_DB:-false}"
# If true, copy existing remote DB to a timestamped .bak before any new upload overwrites it.
BACKUP_REMOTE="${BACKUP_REMOTE:-true}"

# Required positional arguments: SSH private key path (and optional prod|staging).
case $# in
  0)
    echo "Usage: ./deploy.sh <path-to-ssh-private-key>"
    echo "       ./deploy.sh <prod|staging> <path-to-ssh-private-key>"
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
      echo "Usage: ./deploy.sh <path-to-ssh-private-key>"
      echo "       ./deploy.sh <prod|staging> <path-to-ssh-private-key>"
      exit 1
    fi
    ;;
  *)
    echo "Too many arguments."
    echo "Usage: ./deploy.sh <path-to-ssh-private-key>"
    echo "       ./deploy.sh <prod|staging> <path-to-ssh-private-key>"
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
LOCAL_DB_PATH="${LOCAL_DB_PATH:-$SCRIPT_DIR/data/rhododendra.sqlite}"

# ---------------------------------------------------------------------------
# Remote layout: JAR and log under app/; SQLite + Lucene index under data/
# ---------------------------------------------------------------------------
REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
REMOTE_BIN_DIR="${REMOTE_BIN_DIR:-$REMOTE_BASE_DIR/bin}"
REMOTE_INDEX_DIR="${REMOTE_INDEX_DIR:-$REMOTE_DATA_DIR/index}"
REMOTE_DB_PATH="${REMOTE_DB_PATH:-$REMOTE_DATA_DIR/rhododendra.sqlite}"

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
for required in "$LOCAL_JAR" "$LOCAL_INDEX_DIR" "$SCRIPT_DIR/start.sh" "$SCRIPT_DIR/stop.sh"; do
  if [[ ! -e "$required" ]]; then
    echo "Missing required file or directory: $required"
    exit 1
  fi
done

# If DB sync is requested, require the local database file to exist.
if [[ "$SYNC_DB" == "true" ]] && [[ ! -f "$LOCAL_DB_PATH" ]]; then
  echo "SYNC_DB=true but DB file does not exist: $LOCAL_DB_PATH"
  exit 1
fi

# ---------------------------------------------------------------------------
# Remote DB overwrite: always confirm before replacing the server database (SYNC_DB=true)
# ---------------------------------------------------------------------------
if [[ "$SYNC_DB" == "true" ]]; then
  if [[ ! -t 0 ]]; then
    echo "SYNC_DB=true requires an interactive terminal so you can confirm overwriting the remote database."
    echo "Run this script from a real shell (not piped or CI) when using SYNC_DB=true."
    exit 1
  fi
  echo ""
  echo "WARNING: This deploy will REPLACE the remote SQLite database at:"
  echo "  ${REMOTE_TARGET}:${REMOTE_DB_PATH}"
  echo "with your local file:"
  echo "  ${LOCAL_DB_PATH}"
  echo ""
  read -r -p "Type exactly 'yes' to proceed with remote database overwrite: " _db_confirm
  if [[ "${_db_confirm}" != "yes" ]]; then
    echo "Aborted (remote database was not overwritten)."
    exit 1
  fi
fi

echo "Deploying to $ENVIRONMENT ($REMOTE_TARGET)"
echo "Remote base: $REMOTE_BASE_DIR"
echo "Remote app dir: $REMOTE_APP_DIR"
echo "Remote data dir: $REMOTE_DATA_DIR"
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
# Optional: backup current remote DB before we might replace it (see SYNC_DB block below)
# ---------------------------------------------------------------------------
if [[ "$BACKUP_REMOTE" == "true" ]]; then
  echo "[deploy] Backing up remote DB if present..."
  TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
  ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
    "if [ -f \"$REMOTE_DB_PATH\" ]; then cp \"$REMOTE_DB_PATH\" \"$REMOTE_DB_PATH.$TIMESTAMP.bak\"; fi"
fi

# ---------------------------------------------------------------------------
# Upload application JAR and helper scripts (start/stop live under bin/)
# ---------------------------------------------------------------------------
echo "[deploy] Uploading JAR..."
scp "${SSH_OPTS[@]}" "$LOCAL_JAR" "$REMOTE_TARGET:$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar"
echo "[deploy] Uploading start.sh and stop.sh..."
scp "${SSH_OPTS[@]}" "$SCRIPT_DIR/start.sh" "$REMOTE_TARGET:$REMOTE_BIN_DIR/start.sh"
scp "${SSH_OPTS[@]}" "$SCRIPT_DIR/stop.sh" "$REMOTE_TARGET:$REMOTE_BIN_DIR/stop.sh"

# ---------------------------------------------------------------------------
# Lucene: mirror local index/ to the server; --delete removes stale segment files remotely
# ---------------------------------------------------------------------------
echo "[deploy] Syncing Lucene index (this can take a while; per-file progress below)..."
# Keep ssh flags in sync with SSH_OPTS (rsync invokes ssh separately).
rsync -az --delete --progress \
  -e "ssh -i \"$SSH_KEY\" -o BatchMode=yes -o ConnectTimeout=20" \
  "$LOCAL_INDEX_DIR/" "$REMOTE_TARGET:$REMOTE_INDEX_DIR/"

# ---------------------------------------------------------------------------
# Optional: copy local SQLite database to the server (same path start.sh uses via --db.path)
# ---------------------------------------------------------------------------
if [[ "$SYNC_DB" == "true" ]]; then
  echo "[deploy] Uploading SQLite database..."
  scp "${SSH_OPTS[@]}" "$LOCAL_DB_PATH" "$REMOTE_TARGET:$REMOTE_DB_PATH"
fi

# ---------------------------------------------------------------------------
# Remote: make scripts executable
# ---------------------------------------------------------------------------
echo "[deploy] chmod scripts on server..."
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "chmod +x \"$REMOTE_BIN_DIR/start.sh\" \"$REMOTE_BIN_DIR/stop.sh\""

# ---------------------------------------------------------------------------
# Remote restart: stop old JVM, then start with PROFILE matching prod or staging
# ---------------------------------------------------------------------------
echo "[deploy] Restarting remote server..."
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "REMOTE_BASE_DIR=\"$REMOTE_BASE_DIR\" REMOTE_APP_DIR=\"$REMOTE_APP_DIR\" JAR_PATH=\"$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar\" \"$REMOTE_BIN_DIR/stop.sh\""
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  "PROFILE=\"$PROFILE\" REMOTE_BASE_DIR=\"$REMOTE_BASE_DIR\" REMOTE_APP_DIR=\"$REMOTE_APP_DIR\" REMOTE_DATA_DIR=\"$REMOTE_DATA_DIR\" REMOTE_DB_PATH=\"$REMOTE_DB_PATH\" JAR_PATH=\"$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar\" LOG_PATH=\"$REMOTE_APP_DIR/log.log\" \"$REMOTE_BIN_DIR/start.sh\""

echo "Deploy complete."

