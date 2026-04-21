#!/bin/bash
#
# Run on EC2 (after deploy): start Rhododendra in the background with nohup.
# Uses REMOTE_DATA_DIR as cwd so Lucene ./index matches …/data/index.
#
# Optional secrets file (not in git): RHODODENDRA_ENV_FILE (default
# /etc/rhododendra/rhododendra.env). If readable, it is sourced before building
# the environment passed to Java (SPRING_DATASOURCE_* and OAuth client vars).
#
set -euo pipefail

PROFILE="${PROFILE:-prod}"
REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
JAR_PATH="${JAR_PATH:-$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar}"
LOG_PATH="${LOG_PATH:-$REMOTE_APP_DIR/log.log}"

RHODODENDRA_ENV_FILE="${RHODODENDRA_ENV_FILE:-/etc/rhododendra/rhododendra.env}"

SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/rhododendra}"
SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-rhododendra}"

if [[ -r "$RHODODENDRA_ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$RHODODENDRA_ENV_FILE"
  set +a
fi

JAR_BASENAME="$(basename "$JAR_PATH")"
JAR_BASENAME_REGEX="${JAR_BASENAME//./\\.}"
if pgrep -f "java.*${JAR_BASENAME_REGEX}" >/dev/null 2>&1; then
  echo "Rhododendra already running (matched JAR ${JAR_BASENAME})."
  exit 0
fi

mkdir -p "$REMOTE_APP_DIR" "$REMOTE_DATA_DIR"
cd "$REMOTE_DATA_DIR"

DS_ENV=( )
DS_ENV+=( "SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL" )
DS_ENV+=( "SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME" )
if [[ -n "${SPRING_DATASOURCE_PASSWORD:-}" ]]; then
  DS_ENV+=( "SPRING_DATASOURCE_PASSWORD=$SPRING_DATASOURCE_PASSWORD" )
fi
for name in GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET FACEBOOK_CLIENT_ID FACEBOOK_CLIENT_SECRET; do
  if [[ -n "${!name:-}" ]]; then
    DS_ENV+=( "${name}=${!name}" )
  fi
done

# Wait for port 80 (bound wait so a stuck old JVM does not hang forever after deploy).
for _ in {1..600}; do
  if ! sudo lsof -i :80 >/dev/null 2>&1; then
    break
  fi
  echo "Waiting for open port 80..."
  sleep 0.1
done
if sudo lsof -i :80 >/dev/null 2>&1; then
  echo "Port 80 still in use after 60s. Stop the old app (e.g. run bin/stop.sh) and retry."
  exit 1
fi

# Indexes are resolved from the current working directory (`./index`).
sudo env "${DS_ENV[@]}" \
  nohup java \
  -Dspring.profiles.active="$PROFILE" \
  -jar "$JAR_PATH" \
  >> "$LOG_PATH" 2>&1 &
