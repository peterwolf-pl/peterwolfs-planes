#!/usr/bin/env bash
# Build the starter-kit datapack zip for Modrinth / Aternos / dedicated servers.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SRC="datapacks/peterwolfs-planes-starter"
DIST="dist"
ZIP_NAME="peterwolfs-planes-starter-datapack.zip"

mkdir -p "$DIST"
rm -f "${DIST}/${ZIP_NAME}"

(
  cd "$SRC"
  zip -r -9 "../../${DIST}/${ZIP_NAME}" \
    pack.mcmeta README.md data \
    -x "*.DS_Store" -x "**/.DS_Store"
)

echo "Built ${DIST}/${ZIP_NAME}"
ls -lh "${DIST}/${ZIP_NAME}"
unzip -l "${DIST}/${ZIP_NAME}" | head -40
