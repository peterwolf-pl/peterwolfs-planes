# Modrinth Page Content: Peter Wolf's Planes

---

## 📌 Modrinth Summary (max ~140 characters)

> Steerable planes with flight physics, craftable aircraft, paragliding, squadron AI & dogfight guns/bombs.

*(PL):*
> Kierowalne samoloty z fizyką lotu, recepturami, paralotnią, AI eskadry i walką powietrzną (karabiny/bomby).

---

## 📜 Modrinth Description

```markdown
# ✈️ Peter Wolf's Planes

**Peter Wolf's Planes** adds steerable aircraft to Minecraft with custom 3D models, realistic-feeling flight physics, **survival crafting recipes**, cockpit instrumentation, villager squadron AI, a paraglider backpack — and full **dogfight combat**.

---

## 🛫 Aircraft

| Aircraft | Notes |
|---|---|
| **Biplane** | Classic starter plane |
| **Large Biplane** | Bigger airframe |
| **Large Twin-Engine Biplane** | Twin-engine hauler (furnace “engines”) |
| **Red Baron's Triplane** | Nimble WWI-style triplane |
| **Water Plane** | Seaplane with floats (needs a boat in the recipe) |
| **Monoplane** | Fast single-wing fighter (~2× top speed) |

All craft use key-controlled throttle and rudder with mouse look for pitch/bank, plus a glassmorphic cockpit HUD (speed, altitude, V/S, throttle).

---

## 🛠️ Crafting recipes (survival)

Craft in a **Crafting Table**. Shapes are 3×3.

### Biplane (`plane`)
```
W P W
I I I
  S
```
- **W** white wool · **P** oak planks · **I** iron ingot · **S** stick

### Large Biplane
```
W P W
I I I
P S P
```
- **W** white wool · **P** oak planks · **I** iron ingot · **S** stick

### Large Twin-Engine Biplane
```
I F I
W P W
I S I
```
- **I** iron ingot · **F** furnace · **W** white wool · **P** oak planks · **S** stick

### Red Baron's Triplane
```
R G R
I I I
  S
```
- **R** red wool · **G** gold ingot · **I** iron ingot · **S** stick

### Water Plane
```
  B
W P W
I I I
```
- **B** oak boat · **W** white wool · **P** oak planks · **I** iron ingot

### Monoplane (fast fighter)
```
I D I
W P W
  S
```
- **I** iron ingot · **D** diamond · **W** white wool · **P** oak planks · **S** stick

### Paraglider Backpack (chest armor)
```
L P L
S W S
L   L
```
- **L** leather · **P** phantom membrane · **S** string · **W** white wool

### Squadron Horn
```
  G
I N I
  I
```
- **G** gold ingot · **I** iron ingot · **N** note block

> **Villager Pilot Spawn Egg** is creative / loot / starter-kit only (not craftable).

---

## ⚔️ Dogfight Combat Mode

While piloting, press **V** to arm combat mode (HUD: **Combat: ARMED**).

| Control | Action |
|---|---|
| **V** | Toggle combat mode (ARMED / SAFE) |
| **Left Mouse Button** | Fire twin machine guns |
| **B** | Drop a **lit TNT** bomb (1× TNT; free in Creative) |

- Machine guns: dual tracers along the nose, free ammo.
- Bombs inherit plane velocity; short fuse.
- Occupied airframes take projectile/explosion damage (HUD airframe HP).
- Your own munitions do not damage your plane.
- Keybinds: **Controls → Peter Wolf's Planes**.

---

## 🪂 Paraglider

- Equip **Paraglider Backpack** in the chest slot.
- Deploys in free-fall (after enough fall distance).
- **A/D** bank · **W** dive · **S** flare · **double-W** sprint dive · **double-A/D** spiral · **double-S** lock minimum sink.
- **Ridge lift** on large slopes (≥10×10×10 height/length/width).
- **H** — LIFT HUD (climb / sink + ridge strength).
- `/liftparticles` — optional rising-cloud visualization of updrafts.

---

## 🪖 Other features

- **Squadron Horn** + **Villager Pilot**: V-formation wingmen.
- **Tail push**: empty plane, approach tail, **Shift + Right-Click**.
- Worldgen **runway** structures with loot.
- Starter kit (ops): `/planes kit` or `/planekit` — all aircraft + paraglider.
- Optional first-join datapack: `peterwolfs-planes-starter-datapack.zip`.

---

## 🛠️ Requirements

- **Minecraft**: `26.2`
- **Fabric Loader**: `>=0.19.3`
- **Fabric API**: `>=0.153.0+26.2`
- **Java**: `>=25`
```

---

## 📝 Version notes (paste into Modrinth release)

### 1.1.5
```markdown
### Crafting recipes
- Survival recipes for all 6 aircraft, the paraglider backpack, and the squadron horn.
- See description for 3×3 patterns and materials.
```

### 1.1.4
```markdown
### Starter kit
- `/planes kit` / `/planekit` (ops) — all aircraft + paraglider + TNT.
- Optional first-join datapack for servers.
```

### 1.1.0–1.1.3 (highlights)
```markdown
- Dogfight combat (V / LMB / B), airframe HP.
- Paraglider double-S min-sink lock, ridge lift, LIFT HUD (H), lift particles.
```
