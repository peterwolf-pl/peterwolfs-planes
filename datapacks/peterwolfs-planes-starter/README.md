# Peter Wolf's Planes – Starter Kit Datapack

Gives every player a **starter kit** with all aircraft and the paraglider.

## Contents of the kit

| Item | ID |
|---|---|
| Peter Wolf's Plane | `peterwolfs_planes:plane` |
| Large Biplane | `peterwolfs_planes:large_plane` |
| Large Twin-Engine Biplane | `peterwolfs_planes:large_twin_engine_plane` |
| Red Baron's Triplane | `peterwolfs_planes:triplane` |
| Water Plane | `peterwolfs_planes:water_plane` |
| Monoplane | `peterwolfs_planes:monoplane` |
| **Paraglider Backpack** | `peterwolfs_planes:paraglider_backpack` |
| Squadron Horn | `peterwolfs_planes:squadron_horn` |
| Villager Pilot Spawn Egg ×2 | `peterwolfs_planes:villager_pilot_spawn_egg` |
| TNT ×16 (for dogfight bombs) | `minecraft:tnt` |

## Requirements

- Minecraft **26.2**
- Fabric + **Fabric API**
- **Peter Wolf's Planes** mod (`peterwolfs_planes` ≥ 1.1.0)

## Install on Modrinth / Aternos / any server

1. Stop the server (optional but safer).
2. Copy the zip **or** the folder into:

```text
world/datapacks/peterwolfs-planes-starter.zip
```

   On Modrinth App / Hosted servers this is usually under **Files → world → datapacks**.

3. Start the server (or run `/reload`).
4. Enable if needed:

```mcfunction
/datapack enable "file/peterwolfs-planes-starter.zip"
```

   or if unpacked as a folder:

```mcfunction
/datapack enable "file/peterwolfs-planes-starter"
```

5. Check:

```mcfunction
/datapack list
```

## Behaviour

- **First join** (once per player per world): automatic starter kit.
- Players tagged `peterwolfs_planes_starter_received` will not get it again.

### Manual commands

```mcfunction
# Give kit to yourself
/function peterwolfs_planes_starter:give

# Give kit to everyone online
/function peterwolfs_planes_starter:give_all
```

### From the mod (ops)

```mcfunction
/planes kit
/planes kit @a
/planes kit Steve
```

## Disable auto-kit

Remove or rename the advancement file:

```text
data/peterwolfs_planes_starter/advancement/starter_kit_trigger.json
```

Then `/reload`. Manual `/function` and `/planes kit` still work.

## Rebuild zip

From the project root:

```bash
./scripts/build-starter-datapack.sh
```

Output: `dist/peterwolfs-planes-starter-datapack.zip`
