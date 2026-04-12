#!/bin/bash
#
# Run on EC2: stop the Rhododendra JVM (matched by JAR basename in the java cmdline), not every process on port 80.
# See README.md "start.sh and stop.sh (on the server)" for variables and examples.
#
set -euo pipefail

REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
JAR_PATH="${JAR_PATH:-$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar}"
# Match the deployed JAR by filename only so we still find processes started before the app/ layout
# (e.g. java -jar /home/ec2-user/rhododendra-0.0.1-SNAPSHOT.jar vs .../app/...).
JAR_BASENAME="$(basename "$JAR_PATH")"
# pgrep -f uses regex; escape dots in the JAR name so "0.0.1" is literal.
JAR_BASENAME_REGEX="${JAR_BASENAME//./\\.}"

rhododendra_java_pids() {
  pgrep -f "java.*${JAR_BASENAME_REGEX}" || true
}

PIDS="$(rhododendra_java_pids)"
if [[ -z "$PIDS" ]]; then
  echo "No running Rhododendra process found."
  exit 0
fi

echo "Stopping Rhododendra process(es): $PIDS"
sudo kill -15 $PIDS 2>/dev/null || true

for _ in {1..25}; do
  if [[ -z "$(rhododendra_java_pids)" ]]; then
    echo "Rhododendra stopped."
    exit 0
  fi
  sleep 0.2
done

PIDS="$(rhododendra_java_pids)"
if [[ -n "$PIDS" ]]; then
  echo "Processes still running; sending SIGKILL: $PIDS"
  sudo kill -9 $PIDS 2>/dev/null || true
fi

sleep 0.3
if [[ -n "$(rhododendra_java_pids)" ]]; then
  echo "Warning: Rhododendra Java process may still be running."
  exit 1
fi
echo "Rhododendra stopped."