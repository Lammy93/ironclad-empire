#!/usr/bin/env bash
# Check the shipped (remapped) jar for abstract methods left unimplemented in production
# (e.g. a mod interface method whose only implementation is an inherited, remapped Minecraft method).
set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC="$(grep -E '^\s*minecraft_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
FAPI="$(grep -E '^\s*fabric_version' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
FLK="$(grep -oE 'fabric-language-kotlin:[^"]+' "$REPO/build.gradle" | cut -d: -f2)"
BASE="$(grep -E '^\s*archives_base_name' "$REPO/gradle.properties" | cut -d= -f2 | tr -d ' \t')"
JAR="${1:-$(ls -t "$REPO"/build/libs/"$BASE"-*.jar | grep -v -- '-sources\.jar$' | head -1)}"
docker run --rm -v mfc-gradle:/g -v "$REPO:$REPO:ro" -w /tmp -e MC="$MC" -e FAPI="$FAPI" -e FLK="$FLK" -e JAR="$JAR" -e REPO="$REPO" eclipse-temurin:21-jdk bash -c '
  set -e; mkdir -p cp nested; cd /tmp
  MCJAR=$(ls /g/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/$MC*/*.jar 2>/dev/null | grep -v sources | head -1)
  [ -n "$MCJAR" ] || MCJAR=$(find /g/caches/fabric-loom -path "*$MC*" -name "*intermediary*.jar" | grep -v sources | head -1)
  echo "minecraft: $MCJAR"
  python3 - 2>/dev/null || true
  # Minecraft libraries from the version manifest
  for lib in $(grep -oE "\"name\": *\"[^\"]+\"" /g/caches/fabric-loom/$MC/minecraft-info.json | cut -d\" -f4); do
    g=$(echo $lib | cut -d: -f1); a=$(echo $lib | cut -d: -f2); v=$(echo $lib | cut -d: -f3)
    find /g/caches/modules-2/files-2.1/$g/$a/$v -name "$a-$v.jar" 2>/dev/null | head -1
  done | sort -u > libs.txt
  F=$(find /g/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/$FAPI -name "fabric-api-$FAPI.jar" | head -1)
  K=$(find /g/caches/modules-2/files-2.1/net.fabricmc/fabric-language-kotlin/$FLK -name "fabric-language-kotlin-$FLK.jar" | head -1)
  L=$(find /g/caches/modules-2/files-2.1/net.fabricmc/fabric-loader -name "fabric-loader-*.jar" | grep -v sources | sort -V | tail -1)
  for b in "$F" "$K" "$JAR"; do d=nested/$(basename "$b"); mkdir -p "$d"; (cd "$d" && jar xf "$b" META-INF/jars 2>/dev/null || true); done
  NESTED=$(find nested -name "*.jar" | tr "\n" " ")
  java "$REPO/scripts/prodcheck/ProdCheck.java" "$JAR" "$MCJAR" "$F" "$K" "$L" $NESTED $(cat libs.txt | tr "\n" " ")
'
