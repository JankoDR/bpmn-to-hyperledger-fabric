#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_PATH="$SCRIPT_DIR/fabric-samples"
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage: remove-fabric-samples-git-references.sh [options]

Options:
  -t, --target <path>   Custom target directory (default: ./fabric-samples)
  -n, --dry-run         Print what would be removed without deleting
  -h, --help            Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--target)
      shift
      if [[ $# -eq 0 ]]; then
        echo "Error: missing value for --target" >&2
        exit 1
      fi
      TARGET_PATH="$1"
      ;;
    -n|--dry-run)
      DRY_RUN=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

if [[ ! -d "$TARGET_PATH" ]]; then
  echo "Error: target directory does not exist: $TARGET_PATH" >&2
  exit 1
fi

echo "Cleaning Git references in: $TARGET_PATH"

if [[ "$DRY_RUN" -eq 1 ]]; then
  find "$TARGET_PATH" -name .git -print | sed 's/^/Would remove: /'
  find "$TARGET_PATH" -type f -name .gitmodules -print | sed 's/^/Would remove: /'
  echo "Done (dry run)."
  exit 0
fi

find "$TARGET_PATH" -name .git -print -exec rm -rf {} +
find "$TARGET_PATH" -type f -name .gitmodules -print -exec rm -f {} +

echo "Done."
