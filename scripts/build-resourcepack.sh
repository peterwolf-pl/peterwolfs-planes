#!/usr/bin/env bash
# Build a Minecraft server resource pack from mod client assets.
# Output: dist/peterwolfs-planes-<version>-resourcepack.zip (+ .sha1 + snippets)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION="$(grep '^mod_version=' gradle.properties | cut -d= -f2)"
RP_DIR="server-resource-pack"
DIST_DIR="dist"
ZIP_NAME="peterwolfs-planes-${VERSION}-resourcepack.zip"

rm -rf "$RP_DIR"
mkdir -p "$RP_DIR/assets" "$DIST_DIR"

cp -R src/main/resources/assets/peterwolfs_planes "$RP_DIR/assets/"
cp src/main/resources/assets/peterwolfs_planes/icon.png "$RP_DIR/pack.png"

cat > "$RP_DIR/pack.mcmeta" << 'EOF'
{
  "pack": {
    "description": "Peter Wolf's Planes – textures, item models & lang (MC 26.2 / format 88)",
    "min_format": [88, 0],
    "max_format": [88, 0]
  }
}
EOF

cat > "$RP_DIR/README.txt" << EOF
Peter Wolf's Planes – Server Resource Pack
Version: ${VERSION}
Minecraft: 26.2 (resource pack format 88)

Contents:
  assets/peterwolfs_planes/  item models, textures, languages (EN/PL), icon, banner

Install (client, optional – assets are already inside the Fabric mod jar):
  Copy the .zip into .minecraft/resourcepacks/ and enable it.

Install (dedicated server – forced resource pack):
  1. Host this ZIP over HTTPS (public URL).
  2. In server.properties set:
       resource-pack=<HTTPS URL to the zip>
       resource-pack-sha1=<sha1 from the .sha1 file>
       require-resource-pack=true   # optional: kick if client declines
  3. Restart the server.

IMPORTANT:
  This resource pack only provides textures / item models / lang.
  Entity geometry and flight physics live in the Fabric mod jar.
  Server + clients still need peterwolfs-planes-${VERSION}.jar installed.
EOF

(
  cd "$RP_DIR"
  zip -r -9 "../${DIST_DIR}/${ZIP_NAME}" \
    pack.mcmeta pack.png README.txt assets \
    -x "*.DS_Store" -x "**/.DS_Store"
)

ZIP="${DIST_DIR}/${ZIP_NAME}"
if command -v shasum >/dev/null; then
  SHA1="$(shasum -a 1 "$ZIP" | awk '{print $1}')"
elif command -v sha1sum >/dev/null; then
  SHA1="$(sha1sum "$ZIP" | awk '{print $1}')"
else
  SHA1="$(openssl dgst -sha1 "$ZIP" | awk '{print $NF}')"
fi

echo "$SHA1" > "${ZIP}.sha1"

cat > "${DIST_DIR}/server.properties.snippet" << EOF
# Paste into your dedicated server server.properties
# Host the zip at a public HTTPS URL, then set resource-pack= to that URL.

resource-pack=https://YOUR_HOST/peterwolfs-planes-${VERSION}-resourcepack.zip
resource-pack-sha1=${SHA1}
resource-pack-id=
resource-pack-prompt={"text":"Peter Wolf's Planes textures (optional if you already have the mod)"}
require-resource-pack=false
EOF

cat > "${DIST_DIR}/SERVER_RESOURCE_PACK.md" << EOF
# Server Resource Pack – Peter Wolf's Planes ${VERSION}

## Artefakty

| Plik | Opis |
|---|---|
| \`${ZIP_NAME}\` | Gotowy resource pack (format 88 / MC 26.2) |
| \`${ZIP_NAME}.sha1\` | SHA-1 do \`resource-pack-sha1\` |
| \`server.properties.snippet\` | Fragment konfiguracji serwera |

**SHA-1:** \`${SHA1}\`

## Co jest w packu

- Tekstury itemów i entity
- Modele itemów (JSON)
- Tłumaczenia EN / PL (w tym dogfight keybinds)
- Ikona i banner moda

## Czego pack **nie** zastępuje

Geometria samolotów, fizyka lotu, tryb walki, networking — to jest w **Fabric mod jar**.
Serwer i gracze muszą mieć zainstalowane:

\`\`\`
peterwolfs-planes-${VERSION}.jar
\`\`\`

Resource pack jest dodatkiem (np. branding serwera / wymuszony download tekstur).

## Konfiguracja serwera

1. Wrzuć ZIP na hosting HTTPS (np. CDN, GitHub Releases, własny nginx).
2. Uzupełnij \`server.properties\`:

\`\`\`properties
resource-pack=https://YOUR_HOST/peterwolfs-planes-${VERSION}-resourcepack.zip
resource-pack-sha1=${SHA1}
require-resource-pack=false
\`\`\`

3. Restart serwera. Klienci dostaną prośbę o pobranie packa przy joinie.

## Przebudowa

\`\`\`bash
./scripts/build-resourcepack.sh
\`\`\`
EOF

echo "Built ${ZIP}"
echo "SHA1  ${SHA1}"
ls -lh "$ZIP" "${ZIP}.sha1"
