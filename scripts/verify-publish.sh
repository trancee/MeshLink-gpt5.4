#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/verify-publish.sh [options] [target ...]

Scans publication outputs for forbidden third-party crypto payloads and checks
that Package.swift is configured for a remote SwiftPM binary target.

Options:
  --package-file <path>             Package manifest path (default: Package.swift)
  --expected-package-url <url>      Fail if Package.swift does not contain this URL
  --expected-package-checksum <sum> Fail if Package.swift does not contain this checksum
  --help                            Show this help text
EOF
}

PACKAGE_FILE="Package.swift"
EXPECTED_PACKAGE_URL=""
EXPECTED_PACKAGE_CHECKSUM=""
POSITIONAL_TARGETS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package-file)
      PACKAGE_FILE="$2"
      shift 2
      ;;
    --expected-package-url)
      EXPECTED_PACKAGE_URL="$2"
      shift 2
      ;;
    --expected-package-checksum)
      EXPECTED_PACKAGE_CHECKSUM="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      POSITIONAL_TARGETS+=("$1")
      shift
      ;;
  esac
done

if [[ ${#POSITIONAL_TARGETS[@]} -eq 0 ]]; then
  TARGETS=(
    "meshlink/build/outputs/aar"
    "meshlink/build/XCFrameworks/release"
    "$HOME/.m2/repository/ch/trancee/meshlink"
  )
else
  TARGETS=("${POSITIONAL_TARGETS[@]}")
fi

required_files=(
  "consumer-rules.pro"
  "$PACKAGE_FILE"
)

for required in "${required_files[@]}"; do
  if [[ ! -f "$required" ]]; then
    echo "Missing required distribution file: $required" >&2
    exit 1
  fi
done

FORBIDDEN_REGEX='(libsodium|sodium|openssl|boringssl|libcrypto|libssl)'

archive_entries() {
  local archive_path="$1"

  if command -v zipinfo >/dev/null 2>&1; then
    zipinfo -1 "$archive_path"
    return
  fi

  unzip -Z1 "$archive_path"
}

verify_package_manifest() {
  if ! grep -Fq '.binaryTarget(' "$PACKAGE_FILE"; then
    echo "Package.swift must define a binaryTarget." >&2
    exit 1
  fi

  if grep -Fq 'path:' "$PACKAGE_FILE"; then
    echo "Package.swift must use a remote binaryTarget URL, not a local path." >&2
    exit 1
  fi

  if ! grep -Fq 'url:' "$PACKAGE_FILE" || ! grep -Fq 'checksum:' "$PACKAGE_FILE"; then
    echo "Package.swift must include both url and checksum for the binaryTarget." >&2
    exit 1
  fi

  if [[ -n "$EXPECTED_PACKAGE_URL" ]] && ! grep -Fq "$EXPECTED_PACKAGE_URL" "$PACKAGE_FILE"; then
    echo "Package.swift does not contain expected URL: $EXPECTED_PACKAGE_URL" >&2
    exit 1
  fi

  if [[ -n "$EXPECTED_PACKAGE_CHECKSUM" ]] && ! grep -Fq "$EXPECTED_PACKAGE_CHECKSUM" "$PACKAGE_FILE"; then
    echo "Package.swift does not contain expected checksum: $EXPECTED_PACKAGE_CHECKSUM" >&2
    exit 1
  fi

  echo "verify-publish: OK -> $PACKAGE_FILE"
}

scan_archive() {
  local archive_path="$1"
  local entries
  entries="$(archive_entries "$archive_path")"

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

verify_package_manifest

for target in "${TARGETS[@]}"; do
  scan_target "$target"
done
