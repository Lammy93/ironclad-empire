#!/usr/bin/env bash
# Build our Automatone fork (git submodule deps/automatone) and publish it to deps/m2,
# where the main build picks it up. Run after cloning and after any change in deps/automatone.
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$REPO" submodule update --init deps/automatone
"$REPO/scripts/gradle.sh" -p deps/automatone publishToMavenLocal -x javadoc "-Dmaven.repo.local=$REPO/deps/m2"
