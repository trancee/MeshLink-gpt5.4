#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/package-xcframework.sh [options]

Packages the release MeshLink XCFramework into a SwiftPM-ready zip and, when
Swift is available, computes its checksum.

Options:
  --framework <path>       Path to the .xcframework directory
                           (default: meshlink/build/XCFrameworks/release/MeshLink.xcframework)
  --zip <path>             Output zip path
                           (default: meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip)
  --checksum-file <path>   File to write the checksum into
                           (default: <zip>.checksum)
  --skip-checksum          Skip checksum generation
  --help                   Show this help text
EOF
}

FRAMEWORK_PATH="meshlink/build/XCFrameworks/release/MeshLink.xcframework"
ZIP_PATH="meshlink/build/XCFrameworks/release/MeshLink.xcframework.zip"
CHECKSUM_FILE="${ZIP_PATH}.checksum"
SKIP_CHECKSUM=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --framework)
      FRAMEWORK_PATH="$2"
      shift 2
      ;;
    --zip)
      ZIP_PATH="$2"
      shift 2
      ;;
    --checksum-file)
      CHECKSUM_FILE="$2"
      shift 2
      ;;
    --skip-checksum)
      SKIP_CHECKSUM=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ ! -d "$FRAMEWORK_PATH" ]]; then
  echo "XCFramework directory not found: $FRAMEWORK_PATH" >&2
  exit 1
fi

mkdir -p "$(dirname "$ZIP_PATH")"
mkdir -p "$(dirname "$CHECKSUM_FILE")"
rm -f "$ZIP_PATH" "$CHECKSUM_FILE"

FRAMEWORK_DIR="$(cd "$(dirname "$FRAMEWORK_PATH")" && pwd)"
FRAMEWORK_NAME="$(basename "$FRAMEWORK_PATH")"
ZIP_NAME="$(basename "$ZIP_PATH")"
ZIP_DIR="$(cd "$(dirname "$ZIP_PATH")" && pwd)"
ZIP_ABS_PATH="$ZIP_DIR/$ZIP_NAME"
CHECKSUM_NAME="$(basename "$CHECKSUM_FILE")"
CHECKSUM_DIR="$(cd "$(dirname "$CHECKSUM_FILE")" && pwd)"
CHECKSUM_ABS_PATH="$CHECKSUM_DIR/$CHECKSUM_NAME"

if command -v ditto >/dev/null 2>&1; then
  (
    cd "$FRAMEWORK_DIR"
    ditto -c -k --sequesterRsrc --keepParent "$FRAMEWORK_NAME" "$ZIP_ABS_PATH"
  )
else
  (
    cd "$FRAMEWORK_DIR"
    zip -rq "$ZIP_ABS_PATH" "$FRAMEWORK_NAME"
  )
fi

CHECKSUM=""
if [[ "$SKIP_CHECKSUM" == false ]]; then
  if ! command -v swift >/dev/null 2>&1; then
    echo "swift is required to compute the XCFramework checksum" >&2
    exit 1
  fi

  CHECKSUM="$(swift package compute-checksum "$ZIP_ABS_PATH")"
  printf '%s\n' "$CHECKSUM" > "$CHECKSUM_ABS_PATH"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "zip_path=$ZIP_ABS_PATH"
    echo "checksum_file=$CHECKSUM_ABS_PATH"
    if [[ -n "$CHECKSUM" ]]; then
      echo "checksum=$CHECKSUM"
    fi
  } >> "$GITHUB_OUTPUT"
fi

echo "ZIP_PATH=$ZIP_ABS_PATH"
if [[ -n "$CHECKSUM" ]]; then
  echo "CHECKSUM_FILE=$CHECKSUM_ABS_PATH"
  echo "CHECKSUM=$CHECKSUM"
fi
