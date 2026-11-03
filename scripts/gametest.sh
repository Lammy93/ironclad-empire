#!/usr/bin/env bash
# Run the headless game tests and print one line per test.
# Fails if the run does not produce a fresh report (e.g. the server never started).
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT="$REPO/build/gametest.xml"
LOG="${MFC_GAMETEST_LOG:-$REPO/build/gametest.log}"
rm -f "$REPORT"
mkdir -p "$REPO/build"
# A server that hangs on shutdown must not stall the run: stop the container after the deadline.
NAME="mfc-gametest-$$"
# The test world lives in RAM (tmpfs): writing many fresh region files to /mnt/user (Unraid shfs) can stall the
# final save for minutes, and long-running servers on /mnt/user can block an array stop.
MFC_DOCKER_ARGS="--tmpfs $REPO/build/gametest:rw,size=2g" MFC_NAME="$NAME" timeout "${MFC_GAMETEST_TIMEOUT:-1500}" "$REPO/scripts/gradle.sh" runGametest > "$LOG" 2>&1
RC=$?
if [ "$RC" = 124 ]; then
  docker stop "$NAME" >/dev/null 2>&1
  echo "WARNING: game-test run hit the ${MFC_GAMETEST_TIMEOUT:-1500}s deadline (server did not shut down); container stopped"
fi
if [ ! -s "$REPORT" ]; then
  echo "NO REPORT: the game-test server did not run. Last lines of $LOG:"
  grep -E '^e: |error:|What went wrong|Incompatible|Exception|Caused by' "$LOG" | head -20
  exit 2
fi
python3 -I "$REPO/scripts/gametest-summary.py" "$REPORT"
