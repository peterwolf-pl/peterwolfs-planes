# Changelog

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
