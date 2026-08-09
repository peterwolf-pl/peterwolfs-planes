# Changelog

## 1.1.14

- Rebuild the water plane as a single high-wing floatplane with short fuselage-mounted wing supports.
- Complete the opaque water-plane entity atlas so the fuselage, floats, and enlarged UV faces no longer render with transparent gaps.
- Keep the parked water plane level on its two full-length floats instead of applying the conventional tail-low two-wheel ground pose.
- Stop treating water contact as locked ground handling, restoring mouse-driven elevator and aileron authority while the floatplane accelerates and lifts off the water.
- Add deterministic ClientGameTest coverage for the parked high-wing silhouette and real mouse-driven pitch/roll response.
- Treat the water surface as float support while retaining live mouse controls, preventing stall pitch from tipping the aircraft onto its nose after landing and braking.
- Cut a full-chord cockpit opening into the high wing so its center section no longer blocks the first-person pilot view.
- Scale floatplane aileron authority with water speed so mouse input cannot roll a stationary aircraft, while control returns progressively during the takeoff run.

## 1.1.13

- Shorten the rear suspension lines, especially the outer pair, so they terminate at the wing underside instead of passing nearly through the full aerofoil thickness.
- Move dynamic brake-branch endpoints just below and slightly inside the trailing edge to keep them connected without penetrating the canopy.

## 1.1.12

- Increase the normal steering visual range so the active hand makes a clearly readable lateral sweep from the rear camera.
- Strengthen the active wing-tip and trailing-edge down/back deformation while concentrating most of the bend in the outer tip section.

## 1.1.11

- Change paraglider steering-hand travel from a forward/back arm swing to a mirrored lateral sweep, making brake input clearly visible from the rear camera while preserving smooth input interpolation and line-to-hand tracking.

## 1.1.10

- Correct anatomical paraglider side mapping so each brake line connects its wing side to the matching hand and A/D deforms the matching wing tip.
- Smooth the actual steering-input-driven hand pull and release, tighten the active brake line, and add a subtle connected relaxation curve to the opposite line.
- Make outer wing tips inherit the reduced mid-wing bend so deformation grows toward the tip without splitting the wing or rotating an entire half as one rigid panel.
- Add deterministic Fabric ClientGameTest coverage with neutral, left-turn, released, and right-turn screenshots.

## 1.1.9

- Fix paraglider brake lines pointing in opposite front/back directions by using a stable mirrored line orientation.
- Attach all brake-line branches to the transformed lower trailing edge of the actual outer wing sections, including steering deformation.

## 1.1.8

- Paraglider: all lines thinner — suspension ~0.28 and brake lines ~0.36 (main riser slightly thicker than branches).
- Paraglider: **brake lines** from each hand to the trailing edge near the wing tips (main riser + 3 thin branches per side, dark graphite/red, slightly more visible than suspension lines).
- Paraglider: pilot **hand animation** follows real A/D (and double-tap spiral) steering — active hand lowers smoothly; releases return to the neutral brake-handle pose.
- Paraglider: turning side **brake line tightens** and tracks the animated hand; the opposite line stays neutral or slightly relaxed with a subtle sag (no rope physics).
- Paraglider: **wing-tip deformation** on the steering side — tip and outer trailing edge bend down/back proportionally (~12° max tip, ~7% chord TE move); leading edge and span stay intact; visual only (no physics change).
- First-person brake lines use a thinner translucent tint so they do not obstruct the view.

## 1.1.7

- Specialized aircraft API and cargo mass hooks.

## 1.1.6

- Villager pilot AI: real **takeoff rotation** (no longer stuck taxiing straight forever).
- Villager pilot: **climbs** with commanded absolute pitch and a minimum vertical speed (~0.14–0.22 b/t) after liftoff.
- Villager pilot: **obstacle avoidance** — multi-range terrain/heightmap scan, solid-block checks (trees/buildings), climb-over or bank toward freer side.

## 1.1.5

- Add **survival crafting recipes** for all six aircraft, the paraglider backpack, and the squadron horn.
- Update Modrinth description with full recipe patterns (`MODRINTH_PAGE.md`).

## 1.1.4

- **Starter kit** for servers: every aircraft + paraglider (+ horn, pilot eggs, TNT).
  - Command (ops): `/planes kit` / `/planes kit @a` / `/planekit`
  - Function (built-in): `/function peterwolfs_planes:kit`
  - Optional datapack (first-join auto-kit): `dist/peterwolfs-planes-starter-datapack.zip` — drop into `world/datapacks/`
- Runway chest loot can roll all plane types and the paraglider.

## 1.1.3

- LIFT HUD (**H**): clearly shows **climb / sink / level** with real vertical speed (m/s and b/t), plus ridge updraft strength. Centered bar: green = climb, red = descent.

## 1.1.2

- Paraglider: **LIFT HUD** toggle with **H** — top-right panel shows ridge-lift strength (NONE / WEAK / MED / STRONG), b/t and m/s, plus a fill bar.
- Paraglider: **ridge-lift particles** via `/liftparticles` or `/planes liftparticles [true|false]` — rising cloud / ash / end-rod particles mark updrafts around the pilot (per-player, toggle).

## 1.1.1

- Paraglider: **double-tap S** toggles **locked minimum-sink** (stays on without holding S; double-tap S again to unlock). Slightly flatter glide than hold-S flare (0.5 blocks lost per 10 horizontal).
- Paraglider: **ridge lift** over slopes — updrafts appear only on faces that are at least **10×10×10** (height × length × width). Taller and steeper faces produce stronger lift and can make the pilot climb.
- Dive inputs (W / spirals) still override locked S; releasing them returns to the locked sink mode.

## 1.1.0

- Add **dogfight combat mode** for all aircraft (toggle with **V** while piloting).
- Twin **machine guns**: hold **Left Mouse Button** while armed to fire rapid dual streams along the plane heading (no ammo cost).
- **TNT bombardment**: press **B** while armed to drop a lit TNT charge that inherits plane velocity (consumes 1× TNT from inventory; creative is free).
- Piloted airframes now take projectile and explosion damage (HP bar on the cockpit HUD); destroying the airframe ejects passengers with an explosion effect.
- Cockpit HUD shows combat state (ARMED/SAFE), airframe integrity, and weapon hints.
- Configurable keybinds under *Peter Wolf's Planes* in Controls.

## 1.0.10

- Make paraglider descent ratios exact and server-authoritative: single W loses 3 blocks per 10 horizontal, double W loses 5 per 10, and a double-tap A/D spiral loses 4 per 10.
- Add a combined double-W plus double-A/D spiral dive that loses 8 blocks per 10 horizontal while retaining double-W forward speed.
- Synchronize the active paraglider flight mode from the client so server physics and local prediction use the same horizontal and vertical speeds.

## 1.0.9

- Store paraglider deployment in the player's standard synchronized entity data so Flashback snapshots preserve the flight pose when recording starts in mid-air.
- Recover the deployed canopy and fixed seated Y-arm pose in legacy Flashback recordings that began after the original one-time deployment packet.
- Mark client-only rendering mixins explicitly so the common player-state mixin remains safe on dedicated servers.

## 1.0.8

- Bank the player's full body into paraglider turns and roll the first-person camera with the same smooth angle so the horizon visibly tilts.
- Replace vanilla arm and leg movement while the paraglider is deployed with a fixed raised Y-arm pose and a seated, legs-forward flight pose.

## 1.0.7

- Allow players to push empty aircraft by approaching the tail and holding Shift + Right-Click, guiding the plane like a wheelbarrow with tail-up visual pitch.
- Add Paraglider Backpack armor item (chestplate slot) with persistent 3D backpack model rendered on the player's back during gameplay.
- Redesign Paraglider model with a continuous 4-block wide colorful aerofoil canopy wing and 4 diagonal suspension lines converging directly into the top of the backpack.
- Add advanced Paraglider flight controls and physics:
  - **A / D**: Bank turns with unified player body and canopy roll tilt into the turn.
  - **W**: Accelerates forward flight with steeper dive posture (+20° pitch tilt).
  - **S**: Brakes/flares forward flight for flatter, slower descent ($v_y = -0.02$ b/t, -18° pitch tilt), significantly extending flight duration and distance.
  - **Double-Tap W (Sprint Dive)**: Triggers fast dive mode ($v_{horiz} = 1.10$ b/t, $v_y = -0.33$ b/t) for an exact 3 blocks altitude loss per 10 blocks horizontal distance.
- Add inventory item asset definitions for `paraglider_backpack` and `monoplane`.
- Add fast single-wing Monoplane aircraft with 2x top speed compared to biplanes (~177 km/h / 49.2 m/s).

## 1.0.6

- Clamp cockpit HUD speed and vertical speed to zero while the occupied plane is grounded, settled, and at idle power.
- Keep grounded idle physics in ground handling for a few ticks when contact flickers, preventing tiny client/server corrections from showing as movement.
- Restore the -12 degree ground pose only as a client visual for an empty, settled plane after the pilot exits.

## 1.0.5

- Remove the remaining -12 degree ground model pitch so the tail no longer jumps up and down when client ground contact flickers.
- Keep visual pitch tied only to the real flight pitch.

## 1.0.4

- Keep the physical/synced entity pitch at 0 degrees while taxiing on the ground.
- Move the -12 degree taildragger nose-up pose into client-only visual model interpolation.
- Reduce ground jitter caused by syncing a pitched vehicle pose while colliding with the ground.

## 1.0.3

- Smooth ground handling by keeping taxi movement horizontal instead of applying the nose-up pitch to the movement vector.
- Allow A/D rudder input to yaw the plane on the ground even while taxiing slowly or standing still.
- Stop recentering the pilot look direction while the plane is on the ground.

## 1.0.2

- Restore local client prediction for the pilot's plane so the aircraft visibly moves while the HUD reports speed.
- Restore neutral centering for flight controls by gently pulling the pilot look input back to center.
- Add in-flight pitch auto-leveling so pitch and roll settle back toward neutral when the pilot is not holding input.

## 1.0.1

- Smooth plane yaw by keeping flight physics server-authoritative and rendering interpolated yaw/pitch/roll on the client.
- Replace instant yaw/roll changes with damped angular rates, clamped input, smoother engine response, quadratic lift, and speed-dependent drag.
- Reduce redundant synced data updates for throttle, roll, and rudder.
