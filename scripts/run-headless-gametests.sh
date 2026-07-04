#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_DIR="${MC_ELMA_ROPE_GAMETEST_TMP_DIR:-/private/tmp}"
TMP_REPORT="$(mktemp "$TMP_DIR/mc_elma_rope-gametest.XXXXXX")"
REPORT="${MC_ELMA_ROPE_GAMETEST_REPORT:-$ROOT/build/gametest-results/mc_elma_rope-gametest.xml}"

mkdir -p "$(dirname "$REPORT")"
cleanup() {
    rm -f "$TMP_REPORT"
}
trap cleanup EXIT

export JAVA_TOOL_OPTIONS="-Dfabric-api.gametest=true -Dfabric-api.gametest.report-file=$TMP_REPORT ${JAVA_TOOL_OPTIONS:-}"

cd "$ROOT"
./gradlew runServer --no-daemon
cp "$TMP_REPORT" "$REPORT"
echo "Headless Fabric GameTest report written to $REPORT"
