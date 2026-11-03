#!/usr/bin/env bash
# Start the real Minecraft client headless (Xvfb + Mesa software OpenGL) in Docker.
#   scripts/client-check.sh audit   apply every mixin up front (-Dmfc.mixinAudit=true), report, exit
#   scripts/client-check.sh boot    boot to the title screen; pass if it gets there without a crash
set -uo pipefail
MODE="${1:-audit}"
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMMON_GIT="$(git -C "$REPO" rev-parse --path-format=absolute --git-common-dir)"
LOG="$REPO/build/client-check-$MODE.log"; mkdir -p "$REPO/build"
TASK=runClientCheck; [ "$MODE" = audit ] && TASK=runClientAudit
NAME="mfc-client-$MODE-$$"
timeout "${MFC_CLIENT_TIMEOUT:-1200}" docker run --rm --name "$NAME" \
  -v "$REPO:$REPO" -v "$COMMON_GIT:$COMMON_GIT" -v mfc-gradle:/root/.gradle -v mfc-apt:/var/cache/apt -w "$REPO" \
  -e LIBGL_ALWAYS_SOFTWARE=1 -e TASK="$TASK" eclipse-temurin:21-jdk bash -c '
    (apt-get update -qq && apt-get install -y -qq xvfb libgl1 libgl1-mesa-dri libglx-mesa0 libxrender1 libxtst6 libxi6 libxcursor1 libxrandr2 libxxf86vm1 libopenal1 pulseaudio-utils >/dev/null) 2>&1 | tail -2
    Xvfb :99 -screen 0 1280x720x24 -nolisten tcp >/dev/null 2>&1 &
    export DISPLAY=:99; sleep 2
    ./gradlew --no-daemon "$TASK"' > "$LOG" 2>&1 &
PID=$!
RESULT=""
for _ in $(seq 1 240); do
  sleep 5
  if [ "$MODE" = audit ]; then
    grep -q 'MIXIN AUDIT OK' "$LOG" && RESULT=PASS && break
    grep -qE 'MIXIN AUDIT FAILED|Mixin apply .* failed|---- Minecraft Crash Report|BUILD FAILED' "$LOG" && RESULT=FAIL && break
  else
    grep -qE 'Created: [0-9]+x[0-9]+x[0-9]+ minecraft:textures/atlas/gui.png|Sound engine started' "$LOG" && RESULT=PASS && break
    grep -qE -- '---- Minecraft Crash Report|Mixin apply .* failed|BUILD FAILED|Exception in thread "Render thread"' "$LOG" && RESULT=FAIL && break
  fi
  kill -0 $PID 2>/dev/null || break
done
docker stop "$NAME" >/dev/null 2>&1; wait $PID 2>/dev/null
[ -z "$RESULT" ] && RESULT=FAIL
echo "client-check $MODE: $RESULT (log: $LOG)"
grep -E 'MIXIN AUDIT|Mixin apply .* failed|InvalidInjectionException|Critical injection failure|Caused by|crash report' "$LOG" | head -15
[ "$RESULT" = PASS ]
