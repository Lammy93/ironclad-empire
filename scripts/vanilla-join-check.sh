#!/usr/bin/env bash
# Start the dev server (offline mode) and try to join with a plain vanilla client (mineflayer bot).
# Passes when the server turns the client away with ModPresenceHandshake.REQUIRED_MESSAGE.
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC="$(grep -E '^\s*minecraft_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
mkdir -p "$REPO/run"
printf 'online-mode=false\nserver-port=25565\nspawn-protection=0\n' > "$REPO/run/server.properties"
echo 'eula=true' > "$REPO/run/eula.txt"
LOG="$REPO/build/vanilla-join-server.log"; mkdir -p "$REPO/build"
(MFC_NAME=mfc-vanilla-join "$REPO/scripts/gradle.sh" runServer --args=nogui > "$LOG" 2>&1 &)
for _ in $(seq 1 120); do sleep 5; grep -qE 'Done \([0-9.]+s\)|BUILD FAILED|Failed to start' "$LOG" && break; done
grep -q 'Done (' "$LOG" || { echo "server did not start, see $LOG"; docker stop mfc-vanilla-join >/dev/null 2>&1; exit 2; }
OUT="$(docker run --rm --network container:mfc-vanilla-join -v "$REPO/scripts/vanilla-join:/bot:ro" -e MC="$MC" node:20-slim \
  bash -c 'cp /bot/bot.js /tmp/ && cd /tmp && npm i --silent mineflayer@4 >/dev/null 2>&1 && sed -i "s/1.21.1/$MC/" bot.js && node bot.js' 2>&1 | tail -3)"
docker stop mfc-vanilla-join >/dev/null 2>&1
echo "$OUT"
echo "$OUT" | grep -q 'KICKED:.*Ironclad Empire is required' && { echo "PASS: vanilla client turned away"; exit 0; }
echo "FAIL: vanilla client was not turned away"; exit 1
