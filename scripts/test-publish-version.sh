#!/usr/bin/env bash
set -euo pipefail

expected_version="${1:-1.0.0-SNAPSHOT}"
root_version="${1:-0.1.0-SNAPSHOT}"
actual_version="$(./gradlew :properties :dawn4k-native:properties :dawn4k:properties \
  :dawn4k-toolkit-core:properties :dawn4k-toolkit-surface:properties :dawn4k-toolkit-glfw:properties \
  --daemon --console=plain -PreleaseVersion="${1-}" \
  | sed -n 's/^version: //p' \
  )"
expected_versions="$(printf '%s\n%s\n%s\n%s\n%s\n%s' "$root_version" "$expected_version" "$expected_version" "$expected_version" "$expected_version" "$expected_version")"

if [[ "${actual_version}" != "${expected_versions}" ]]; then
  printf 'Expected root/library versions:\n%s\nGot:\n%s\n' "${expected_versions}" "${actual_version}" >&2
  exit 1
fi
