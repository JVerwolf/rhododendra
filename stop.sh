#!/bin/bash
#
# Run on EC2: stop the Rhododendra JVM (matched by JAR path), not every process on port 80.
# See README.md "start.sh and stop.sh (on the server)" for variables and examples.
#
set -euo pipefail

REMOTE_BASE_DIR="${REMOTE_BASE_DIR:-/home/ec2-user/rhododendra}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-$REMOTE_BASE_DIR/app}"
JAR_PATH="${JAR_PATH:-$REMOTE_APP_DIR/rhododendra-0.0.1-SNAPSHOT.jar}"

# Stop only the Rhododendra JVM process, not unrelated services on port 80.
PIDS="$(pgrep -f "java.*$JAR_PATH" || true)"
if [[ -z "$PIDS" ]]; then
  echo "No running Rhododendra process found."
  exit 0
fi

echo "Stopping Rhododendra process(es): $PIDS"
sudo kill $PIDS

for _ in {1..50}; do
  if ! pgrep -f "java.*$JAR_PATH" >/dev/null 2>&1; then
    echo "Rhododendra stopped."
    exit 0
  fi
  sleep 0.2
done

echo "Process still running; forcing shutdown."
sudo kill -9 $PIDS || true