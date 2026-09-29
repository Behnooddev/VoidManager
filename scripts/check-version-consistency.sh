#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version="$(grep -E '^versionName=' "$root/version.properties" | cut -d= -f2 | tr -d '[:space:]')"

if [[ -z "$version" ]]; then
  echo "versionName is missing in version.properties" >&2
  exit 1
fi

if ! grep -qE "^## \[$version\]" "$root/CHANGELOG.md"; then
  echo "CHANGELOG.md has no entry for version $version" >&2
  exit 1
fi

echo "Version $version is consistent"
