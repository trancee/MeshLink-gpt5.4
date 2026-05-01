#!/usr/bin/env bash
set -euo pipefail

TARGET_DIR="${1:-meshlink/build}"

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

if [[ -d "$TARGET_DIR" ]]; then
  if find "$TARGET_DIR" -iname '*sodium*' -o -iname '*libsodium*' | grep -q .; then
    echo "Found forbidden external crypto payload in $TARGET_DIR" >&2
    exit 1
  fi
fi

echo "verify-publish: OK"
