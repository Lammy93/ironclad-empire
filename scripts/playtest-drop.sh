#!/usr/bin/env bash
# Build a ready-to-play mods folder and copy it to the test PC.
#
# Usage: scripts/playtest-drop.sh [-n] <label>
#   -n       dry run: assemble locally and list the files, do not copy
#   <label>  name of the drop; docs/playtest/<label>.md becomes WHAT-TO-TRY.txt
#
# Result on the PC: C:\Users\ozzyz\Documents\Ironclad Empire\IE-<label>\
set -euo pipefail

DRY=0
if [ "${1:-}" = "-n" ]; then DRY=1; shift; fi
LABEL="${1:?usage: scripts/playtest-drop.sh [-n] <label>}"

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC="$(grep -E '^\s*minecraft_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
FAPI="$(grep -E '^\s*fabric_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
FLK="$(grep -oE 'fabric-language-kotlin:[^"]+' "$REPO/build.gradle" | cut -d: -f2)"
LOADER="$(grep -E '^\s*loader_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
NOTES="$REPO/docs/playtest/$LABEL.md"
[ -f "$NOTES" ] || { echo "missing $NOTES (the what-to-try list)"; exit 1; }

OUT="$REPO/build/playtest/IE-$LABEL"
rm -rf "$OUT"; mkdir -p "$OUT/mods"

# 1. build the mod
"$REPO/scripts/gradle.sh" build -x runGametest > "$REPO/build/playtest-build.log" 2>&1 || { tail -20 "$REPO/build/playtest-build.log"; exit 1; }
BASE="$(grep -E '^\s*archives_base_name' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
JAR="$(ls -t "$REPO"/build/libs/"$BASE"-*.jar | grep -v -- '-sources\.jar$' | head -1)"
cp "$JAR" "$OUT/mods/ironclad-empire-$LABEL.jar"

# 2. exact dependency versions from Modrinth
modrinth_url() { # project version_number
  curl -fsS "https://api.modrinth.com/v2/project/$1/version" | python3 -I -c '
import json, sys
want = sys.argv[1]
for v in json.load(sys.stdin):
    if v["version_number"] == want:
        print([f for f in v["files"] if f["primary"]][0]["url"]); break
else:
    sys.exit("version not found: " + want)' "$2"
}
curl -fsSL -o "$OUT/mods/fabric-api-$FAPI.jar" "$(modrinth_url fabric-api "$FAPI")"
curl -fsSL -o "$OUT/mods/fabric-language-kotlin-$FLK.jar" "$(modrinth_url fabric-language-kotlin "$FLK")"
for j in "$OUT"/mods/*.jar; do python3 -I -c 'import sys, zipfile; zipfile.ZipFile(sys.argv[1]).testzip()' "$j"; done

# 3. notes
cat > "$OUT/README.txt" <<EOF
Ironclad Empire - play-test build "$LABEL"
==========================================

Install (Modrinth App or Prism Launcher):
 1. Create a new instance: Minecraft $MC, loader Fabric, loader version $LOADER or newer.
 2. Open the instance folder and copy the jars from the "mods" folder here into its "mods" folder.
 3. Start the game and make a NEW world (Singleplayer > Create New World), game mode Fortress.

What to try: see WHAT-TO-TRY.txt. Please note anything broken or odd (screenshots welcome).
EOF
cp "$NOTES" "$OUT/WHAT-TO-TRY.txt"

echo "assembled $OUT:"; (cd "$OUT" && find . -type f | sort)
if [ "$DRY" = 1 ]; then echo "(dry run: not copied)"; exit 0; fi

# 4. copy to the PC
DEST='C:/Users/ozzyz/Documents/Ironclad Empire'
ssh lammy-pc "powershell -NoProfile -Command \"New-Item -ItemType Directory -Force -Path '$DEST' | Out-Null\"" 2>/dev/null
scp -rq "$OUT" "lammy-pc:$DEST/" 2>/dev/null
echo "copied to lammy-pc: $DEST/IE-$LABEL"
