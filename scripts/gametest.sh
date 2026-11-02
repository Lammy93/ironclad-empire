#!/usr/bin/env bash
# Run the headless game tests and print one line per test.
# Fails if the run does not produce a fresh report (e.g. the server never started).
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT="$REPO/build/gametest.xml"
LOG="${MFC_GAMETEST_LOG:-$REPO/build/gametest.log}"
rm -f "$REPORT"
mkdir -p "$REPO/build"
"$REPO/scripts/gradle.sh" runGametest > "$LOG" 2>&1
if [ ! -s "$REPORT" ]; then
  echo "NO REPORT: the game-test server did not run. Last lines of $LOG:"
  grep -E '^e: |error:|What went wrong|Incompatible|Exception|Caused by' "$LOG" | head -20
  exit 2
fi
python3 -I "$REPO/scripts/gametest-summary.py" "$REPORT"
