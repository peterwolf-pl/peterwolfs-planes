# Server Resource Pack – Peter Wolf's Planes 1.1.0

## Artefakty

| Plik | Opis |
|---|---|
| `peterwolfs-planes-1.1.0-resourcepack.zip` | Gotowy resource pack (format 88 / MC 26.2) |
| `peterwolfs-planes-1.1.0-resourcepack.zip.sha1` | SHA-1 do `resource-pack-sha1` |
| `server.properties.snippet` | Fragment konfiguracji serwera |

**SHA-1:** `61d0bd917265e09474505f676fb615d888ab5a9e`

## Co jest w packu

- Tekstury itemów i entity
- Modele itemów (JSON)
- Tłumaczenia EN / PL (w tym dogfight keybinds)
- Ikona i banner moda

## Czego pack **nie** zastępuje

Geometria samolotów, fizyka lotu, tryb walki, networking — to jest w **Fabric mod jar**.
Serwer i gracze muszą mieć zainstalowane:

```
peterwolfs-planes-1.1.0.jar
```

Resource pack jest dodatkiem (np. branding serwera / wymuszony download tekstur).

## Konfiguracja serwera

1. Wrzuć ZIP na hosting HTTPS (np. CDN, GitHub Releases, własny nginx).
2. Uzupełnij `server.properties`:

```properties
resource-pack=https://YOUR_HOST/peterwolfs-planes-1.1.0-resourcepack.zip
resource-pack-sha1=61d0bd917265e09474505f676fb615d888ab5a9e
require-resource-pack=false
```

3. Restart serwera. Klienci dostaną prośbę o pobranie packa przy joinie.

## Przebudowa

```bash
./scripts/build-resourcepack.sh
```
