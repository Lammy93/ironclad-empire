#!/usr/bin/env bash
# Every gate a play-test build must pass before it goes to a player:
#   1. server game tests   2. client mixin audit   3. client boot to title   4. production-jar check
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0
echo "== 1/4 game tests";        R=$("$REPO/scripts/gametest.sh" 2>&1 | tail -1); echo "$R"; case "$R" in *"/"*" passed") n=${R%% *}; [ "${n%/*}" = "${n#*/}" ] || fail=1;; *) fail=1;; esac
echo "== 2/4 client mixin audit"; "$REPO/scripts/client-check.sh" audit 2>&1 | head -1 | grep -q PASS && echo PASS || { echo FAIL; fail=1; }
echo "== 3/4 client boot";        "$REPO/scripts/client-check.sh" boot  2>&1 | head -1 | grep -q PASS && echo PASS || { echo FAIL; fail=1; }
echo "== 4/4 production jar";     P=$("$REPO/scripts/prodcheck.sh" 2>&1 | grep -E 'UNIMPLEMENTED|checked='); echo "$P" | tail -1; echo "$P" | grep -q 'problems=0' || { echo "$P" | head -10; fail=1; }
[ "$fail" = 0 ] && echo "ALL GATES PASS" || echo "GATES FAILED"
exit $fail
