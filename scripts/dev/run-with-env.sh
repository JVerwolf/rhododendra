#!/usr/bin/env bash
#
# Run bootRun with variables from a local env file (never committed).
# Default file: repo-root `.env`. Override with RHODODENDRA_ENV_FILE.
#
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
ENV_FILE="${RHODODENDRA_ENV_FILE:-$ROOT/.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  set +a
fi
exec ./gradlew bootRun "$@"
