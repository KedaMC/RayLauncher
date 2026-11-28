#!/usr/bin/env bash
# Print the CHANGELOG.md section of the launcher version found at a git ref.
# Usage: changelog.sh <ref>
set -euo pipefail

REF="$1"
VERSION="$(git show "${REF}:ZalithLauncher/gradle.properties" | grep -E '^launcher_version_name=' | head -n1 | cut -d= -f2- | tr -d '\r')"

SECTION="$(git show "${REF}:CHANGELOG.md" \
  | tr -d '\r' \
  | perl -0pe 's/<!--.*?-->//gs' \
  | VERSION="$VERSION" awk '/^## / { found = ($0 == "## " ENVIRON["VERSION"]); next } found' \
  | sed -e '/./,$!d')"

if [[ -z "${SECTION//[[:space:]]/}" ]]; then
  echo "CHANGELOG.md has no entry for '## ${VERSION}' at ${REF}" >&2
  exit 1
fi

printf '%s\n' "$SECTION"
