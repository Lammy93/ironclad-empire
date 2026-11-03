#!/usr/bin/env bash
# Run the Gradle wrapper inside a throwaway JDK 21 container.
# Usage: scripts/gradle.sh <gradle args...>
# The Gradle cache lives in the Docker volume "mfc-gradle" so nothing is installed on the host.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# A linked worktree's .git file points at the main repo's .git; mount that too so git works in the container.
COMMON_GIT="$(git -C "$REPO" rev-parse --path-format=absolute --git-common-dir)"
IMAGE="${MFC_JDK_IMAGE:-eclipse-temurin:21-jdk}"
NAME="${MFC_NAME:-mfc-gradle-$$}"

TTY=()
[ -t 1 ] && TTY=(-t)

exec docker run --rm "${TTY[@]}" --name "$NAME" \
  -v "$REPO:$REPO" \
  -v "$COMMON_GIT:$COMMON_GIT" \
  -v mfc-gradle:/root/.gradle \
  -w "$REPO" \
  -e GRADLE_OPTS="-Dorg.gradle.daemon=false" \
  ${MFC_DOCKER_ARGS:-} \
  "$IMAGE" ./gradlew --no-daemon "$@"
