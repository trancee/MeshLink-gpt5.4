#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  TARGETS=(
    "meshlink/build/outputs/aar"
    "meshlink/build/XCFrameworks/release"
    "$HOME/.m2/repository/ch/trancee/meshlink"
  )
else
  TARGETS=("$@")
fi

required_files=(
  "consumer-rules.pro"
  "Package.swift"
)

for required in "${required_files[@]}"; do
  if [[ ! -f "$required" ]]; then
    echo "Missing required distribution file: $required" >&2
    exit 1
  fi
done

FORBIDDEN_REGEX='(libsodium|sodium|openssl|boringssl|libcrypto|libssl)'

scan_archive() {
  local archive_path="$1"
  local entries
  entries="$(zipinfo -1 "$archive_path")"
  if grep -Eqi "$FORBIDDEN_REGEX" <<<"$entries"; then
    echo "Found forbidden external crypto payload inside archive: $archive_path" >&2
    grep -Ei "$FORBIDDEN_REGEX" <<<"$entries" >&2
    exit 1
  fi
}

scan_target() {
  local target="$1"

  if [[ ! -e "$target" ]]; then
    echo "Missing verification target: $target" >&2
    exit 1
  fi

  if find "$target" -print | grep -Eqi "$FORBIDDEN_REGEX"; then
    echo "Found forbidden external crypto payload in target path: $target" >&2
    find "$target" -print | grep -Ei "$FORBIDDEN_REGEX" >&2
    exit 1
  fi

  while IFS= read -r archive; do
    scan_archive "$archive"
  done < <(
    find "$target" -type f \( -name '*.aar' -o -name '*.jar' -o -name '*.zip' \) | sort
  )

  echo "verify-publish: OK -> $target"
}

for target in "${TARGETS[@]}"; do
  scan_target "$target"
done
