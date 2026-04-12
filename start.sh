#!/bin/bash
#
# Run on EC2 (after deploy): start Rhododendra in the background with nohup.
# Uses REMOTE_DATA_DIR as cwd so Lucene ./index matches …/data/index; passes --db.path to Spring.
# See README.md "start.sh and stop.sh (on the server)" for variables and examples.
#
set -euo pipefail

PROFILE="${PROFILE:-prod}"
REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
REMOTE_DATA_DIR="${REMOTE_DATA_DIR:-$REMOTE_BASE_DIR/data}"
REMOTE_DB_PATH="${REMOTE_DB_PATH:-$REMOTE_DATA_DIR/rhododendra.sqlite}"
JAR_PATH="${JAR_PATH:-$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar}"
LOG_PATH="${LOG_PATH:-$REMOTE_APP_DIR/log.log}"

mkdir -p "$REMOTE_APP_DIR" "$REMOTE_DATA_DIR"
cd "$REMOTE_DATA_DIR"

while sudo lsof -i :80; do
  echo "Waiting for open port 80..."
  sleep 0.1
done

# Indexes are resolved from the current working directory (`./index`).
sudo nohup java \
  -Dspring.profiles.active="$PROFILE" \
  -jar "$JAR_PATH" \
  --db.path="$REMOTE_DB_PATH" \
  >> "$LOG_PATH" 2>&1 &
