#!/usr/bin/env bash
# Build our Automatone fork (git submodule deps/automatone) and publish it to deps/m2,
# where the main build picks it up. Run after cloning and after any change in deps/automatone.
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[ -e "$REPO/deps/automatone/build.gradle" ] || git -C "$REPO" submodule update --init deps/automatone  # only on a fresh clone: never move an existing checkout
"$REPO/scripts/gradle.sh" -p deps/automatone publishToMavenLocal -x javadoc "-Dmaven.repo.local=$REPO/deps/m2"
# Loom caches remapped mod jars by coordinates; our version string never changes,
# so drop the cached copy to make the main build pick up the fresh jar.
rm -rf "$REPO"/.gradle/loom-cache/remapped_mods/*/io/github/ladysnake/automatone
