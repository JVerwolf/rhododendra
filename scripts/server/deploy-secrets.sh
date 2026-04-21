#!/usr/bin/env bash
#
# deploy-secrets.sh — Idempotently install a secrets env file on the remote host.
#
# Reads a plain `KEY=value` file from the dev machine (never in git) and installs it
# atomically as /etc/rhododendra/rhododendra.env on the server:
#   owner: root:root, mode: 0600
#
# Safe by design:
#   - Validates the local file before touching the server (existence, regular file,
#     readability, mode 0600, required keys present).
#   - Uploads to a per-process staging file in the SSH user's home (not /etc/) so
#     we never need to write to privileged locations with an unprivileged user.
#   - Backs up the existing remote file to a timestamped sibling before replacing it
#     and prunes old backups to --keep-backups (default 5). If the atomic install
#     fails, the newest backup is restored so the app keeps running on the prior
#     secrets.
#   - Cleans up the staging file on every exit path via `trap` (even on error).
#   - Never prints secret values to stdout/logs; only key names.
#   - Does NOT restart the app — the caller (deploy.sh or operator) decides when
#     the JVM picks up the new values.
#
# Usage:
#   ./scripts/server/deploy-secrets.sh \
#     --env prod|staging \
#     --ssh-key /path/to/key \
#     [--secrets-file /path/to/env]   # default: ~/.config/rhododendra/<env>.env
#     [--host a.b.c.d]                # override PROD_HOST / STAGING_HOST
#     [--remote-user ec2-user]        # override REMOTE_USER
#     [--keep-backups 5]
#     [--dry-run]
#
# Exit codes:
#   0  success (or dry-run success)
#   1  usage/preflight error (nothing touched on remote)
#   2  remote install failed (backup restored if possible)
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Defaults (overridable by flags or env; env var wins only if no flag passed)
# ---------------------------------------------------------------------------
ENVIRONMENT=""
SSH_KEY_ARG=""
SECRETS_FILE=""
HOST_OVERRIDE=""
REMOTE_USER="${REMOTE_USER:-ec2-user}"
STAGING_HOST="${STAGING_HOST:-44.237.219.139}"
PROD_HOST="${PROD_HOST:-54.212.15.213}"
KEEP_BACKUPS="${KEEP_BACKUPS:-5}"
DRY_RUN=0

# Remote install target — keep in sync with start.sh RHODODENDRA_ENV_FILE default.
REMOTE_ENV_DIR="${REMOTE_ENV_DIR:-/etc/rhododendra}"
REMOTE_ENV_FILE="${REMOTE_ENV_FILE:-$REMOTE_ENV_DIR/rhododendra.env}"

# Required keys that every prod/staging secrets file must provide. Extra keys pass
# through unchanged; we only fail when required names are missing so rotations can
# add new vars without schema churn here.
REQUIRED_KEYS=( "SPRING_DATASOURCE_PASSWORD" )

usage() {
  cat <<EOF
Usage: $0 --env <prod|staging> --ssh-key <path> [options]

Options:
  --env <prod|staging>     Target environment (required).
  --ssh-key <path>         SSH private key for the remote host (required).
  --secrets-file <path>    Local env file to upload.
                           Default: ~/.config/rhododendra/<env>.env
  --host <addr>            Override PROD_HOST/STAGING_HOST.
  --remote-user <user>     Override REMOTE_USER (default: ec2-user).
  --keep-backups <N>       Backups to retain on the server (default: 5).
  --dry-run                Validate locally and print the plan; do not upload.
  -h, --help               Show this help.
EOF
}

# ---------------------------------------------------------------------------
# Flag parsing — positional-free so deploy.sh can forward args cleanly
# ---------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env)           ENVIRONMENT="${2:-}"; shift 2 ;;
    --ssh-key)       SSH_KEY_ARG="${2:-}"; shift 2 ;;
    --secrets-file)  SECRETS_FILE="${2:-}"; shift 2 ;;
    --host)          HOST_OVERRIDE="${2:-}"; shift 2 ;;
    --remote-user)   REMOTE_USER="${2:-}"; shift 2 ;;
    --keep-backups)  KEEP_BACKUPS="${2:-}"; shift 2 ;;
    --dry-run)       DRY_RUN=1; shift ;;
    -h|--help)       usage; exit 0 ;;
    *)               echo "[secrets] Unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
done

if [[ -z "$ENVIRONMENT" || -z "$SSH_KEY_ARG" ]]; then
  echo "[secrets] --env and --ssh-key are required." >&2
  usage >&2
  exit 1
fi
if [[ "$ENVIRONMENT" != "prod" && "$ENVIRONMENT" != "staging" ]]; then
  echo "[secrets] Invalid --env: $ENVIRONMENT (expected prod or staging)" >&2
  exit 1
fi
if ! [[ "$KEEP_BACKUPS" =~ ^[0-9]+$ ]] || (( KEEP_BACKUPS < 1 )); then
  echo "[secrets] --keep-backups must be a positive integer." >&2
  exit 1
fi

SSH_KEY="${SSH_KEY_ARG/#\~/$HOME}"
if [[ ! -f "$SSH_KEY" ]]; then
  echo "[secrets] SSH private key not found: $SSH_KEY" >&2
  exit 1
fi

# Default secrets source is per-environment under the user's XDG config dir so
# multiple environments (and multiple apps) do not step on each other.
if [[ -z "$SECRETS_FILE" ]]; then
  SECRETS_FILE="${HOME}/.config/rhododendra/${ENVIRONMENT}.env"
fi

# ---------------------------------------------------------------------------
# Local preflight — everything below this point can cause failure BEFORE any
# network call, so a typo/missing file never reaches the server.
# ---------------------------------------------------------------------------
if [[ ! -e "$SECRETS_FILE" ]]; then
  echo "[secrets] Secrets file does not exist: $SECRETS_FILE" >&2
  echo "          Create it (outside the repo) with mode 0600 and populate the keys" >&2
  echo "          listed in .env.example for this environment." >&2
  exit 1
fi
if [[ ! -f "$SECRETS_FILE" ]]; then
  echo "[secrets] Secrets path is not a regular file: $SECRETS_FILE" >&2
  exit 1
fi
if [[ ! -r "$SECRETS_FILE" ]]; then
  echo "[secrets] Secrets file is not readable: $SECRETS_FILE" >&2
  exit 1
fi

# `stat -c` is GNU (Linux); `stat -f` is BSD (macOS). Dev laptops are typically
# macOS while CI may run Linux — try both so the script is portable.
if stat -c '%a' "$SECRETS_FILE" >/dev/null 2>&1; then
  LOCAL_MODE="$(stat -c '%a' "$SECRETS_FILE")"
else
  LOCAL_MODE="$(stat -f '%Lp' "$SECRETS_FILE")"
fi
# Accept 600 and tolerate 400 (read-only is also fine). Anything broader than the
# owner is a configuration smell we want the user to fix before we ship the file.
case "$LOCAL_MODE" in
  600|400) ;;
  *)
    echo "[secrets] Refusing to deploy: local file mode is $LOCAL_MODE, expected 600." >&2
    echo "          Run: chmod 600 \"$SECRETS_FILE\"" >&2
    exit 1
    ;;
esac

# Parse keys without sourcing (avoid executing anything from the file locally).
# Lines starting with # or blank are ignored; keys must match shell-style names.
PRESENT_KEYS=()
while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
  if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)= ]]; then
    PRESENT_KEYS+=( "${BASH_REMATCH[1]}" )
  fi
done < "$SECRETS_FILE"

MISSING=()
for required in "${REQUIRED_KEYS[@]}"; do
  found=0
  for k in "${PRESENT_KEYS[@]}"; do
    [[ "$k" == "$required" ]] && { found=1; break; }
  done
  (( found )) || MISSING+=( "$required" )
done
if (( ${#MISSING[@]} )); then
  echo "[secrets] Missing required keys in $SECRETS_FILE: ${MISSING[*]}" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# Host selection (flag > env var > default)
# ---------------------------------------------------------------------------
if [[ -n "$HOST_OVERRIDE" ]]; then
  HOST="$HOST_OVERRIDE"
elif [[ "$ENVIRONMENT" == "prod" ]]; then
  HOST="$PROD_HOST"
else
  HOST="$STAGING_HOST"
fi
REMOTE_TARGET="${REMOTE_USER}@${HOST}"
SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=20)

echo "[secrets] Environment:    $ENVIRONMENT"
echo "[secrets] Remote target:  $REMOTE_TARGET"
echo "[secrets] Local file:     $SECRETS_FILE (mode $LOCAL_MODE)"
echo "[secrets] Remote target:  $REMOTE_ENV_FILE (root:root 0600)"
echo "[secrets] Keys present:   ${PRESENT_KEYS[*]:-<none>}"
echo "[secrets] Keep backups:   $KEEP_BACKUPS"

if (( DRY_RUN )); then
  echo "[secrets] --dry-run: validated locally; not uploading."
  exit 0
fi

# ---------------------------------------------------------------------------
# Staging upload — cleanup via trap so a Ctrl-C / error still removes the temp
# file on the remote home directory (it would otherwise contain plaintext
# secrets until the next manual cleanup).
# ---------------------------------------------------------------------------
# $$ is the local PID; good enough as a per-run identifier on the remote home.
REMOTE_STAGING="\$HOME/.rhododendra-secrets.$$.env"
cleanup_remote_staging() {
  # Best-effort: if the SSH session or key is broken we cannot clean up, but we
  # tried. Never fail the script from the trap.
  ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" "rm -f $REMOTE_STAGING" >/dev/null 2>&1 || true
}
trap cleanup_remote_staging EXIT

echo "[secrets] Uploading to remote staging path..."
# scp does not set a fine-grained remote mode, so we explicitly chmod after
# upload before doing anything sudo-related with the file.
scp "${SSH_OPTS[@]}" "$SECRETS_FILE" "${REMOTE_TARGET}:${REMOTE_STAGING}"
ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" "chmod 600 $REMOTE_STAGING"

# ---------------------------------------------------------------------------
# Remote install — one SSH call running bash with strict mode and an explicit
# backup/restore flow. Variables are expanded locally and quoted into the
# heredoc so we do not depend on the remote user's environment.
# ---------------------------------------------------------------------------
echo "[secrets] Installing on remote with backup + atomic replace..."
if ! ssh "${SSH_OPTS[@]}" "$REMOTE_TARGET" \
  STAGING="$REMOTE_STAGING" \
  DEST_DIR="$REMOTE_ENV_DIR" \
  DEST="$REMOTE_ENV_FILE" \
  KEEP="$KEEP_BACKUPS" \
  'bash -s' <<'REMOTE_EOF'
set -euo pipefail

# Resolve $HOME-relative staging path (ssh sends the literal string).
STAGING_RESOLVED="$(eval echo "$STAGING")"
if [[ ! -f "$STAGING_RESOLVED" ]]; then
  echo "[secrets][remote] staging file missing: $STAGING_RESOLVED" >&2
  exit 10
fi

# Ensure the target directory exists with sane perms. 0755 on the directory is
# intentional (secrets are protected by the FILE mode, not the dir) so that
# other ops tooling can traverse /etc/rhododendra without sudo.
sudo mkdir -p "$DEST_DIR"
sudo chmod 0755 "$DEST_DIR"
sudo chown root:root "$DEST_DIR"

BACKUP=""
if [[ -f "$DEST" ]]; then
  # UTC timestamp so ls sorts backups chronologically regardless of locale.
  TS="$(date -u +%Y%m%dT%H%M%SZ)"
  BACKUP="${DEST}.bak.${TS}"
  sudo cp -a "$DEST" "$BACKUP"
  sudo chown root:root "$BACKUP"
  sudo chmod 0600 "$BACKUP"
fi

# Atomic replace: `install` writes to a temp file and renames into place, so
# the destination is never observed half-written (important: the app may be
# reading this file during deploy).
if ! sudo install -o root -g root -m 0600 "$STAGING_RESOLVED" "$DEST"; then
  echo "[secrets][remote] install failed; attempting to restore backup..." >&2
  if [[ -n "$BACKUP" && -f "$BACKUP" ]]; then
    sudo install -o root -g root -m 0600 "$BACKUP" "$DEST" || true
    echo "[secrets][remote] restored from $BACKUP" >&2
  fi
  exit 11
fi

# Post-install verification: if any of these fail we roll back to the backup so
# the server is guaranteed to be in a known-good state.
ACTUAL="$(sudo stat -c '%U %G %a' "$DEST")"
if [[ "$ACTUAL" != "root root 600" ]]; then
  echo "[secrets][remote] ownership/mode check failed: got '$ACTUAL'" >&2
  if [[ -n "$BACKUP" && -f "$BACKUP" ]]; then
    sudo install -o root -g root -m 0600 "$BACKUP" "$DEST" || true
    echo "[secrets][remote] restored from $BACKUP" >&2
  fi
  exit 12
fi

# Prune old backups to --keep-backups. We intentionally keep the N newest and
# delete the rest; the active file is not a backup and is untouched.
# shellcheck disable=SC2012  # `ls -1t` is simpler than find here for timestamps
mapfile -t OLD < <(sudo bash -c "ls -1t ${DEST}.bak.* 2>/dev/null | tail -n +$((KEEP + 1))")
for f in "${OLD[@]}"; do
  [[ -n "$f" ]] && sudo rm -f -- "$f"
done

echo "[secrets][remote] installed $DEST ($ACTUAL); backups retained: $KEEP"
REMOTE_EOF
then
  echo "[secrets] Remote install failed. Previous secrets (if any) remain active." >&2
  exit 2
fi

echo "[secrets] Done. Restart the app to pick up new values (deploy.sh does this automatically)."
