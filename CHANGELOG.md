# Changelog

## 1.2.5

### 🚁 Smooth Helicopter Tail Rotation & Anti-Torque Pedals
- **Eliminated Tail Rotation Jitter & Shudder**: Fixed sub-frame interpolation desynchronization during helicopter tail rotation (yawing with **A** / **D** anti-torque pedals).
- **Synchronized Passenger Rotation History**: Player and NPC passenger view angle histories (`yRotO`, `yHeadRotO`, `yBodyRotO`) are now maintained across tick boundaries in `positionRider`, preventing the 20-Hz camera shutter/jerk in both first-person and third-person perspectives.
- **Consistent Heading Interpolation**: Overrode `getYRot(partialTick)` and `getViewYRot(partialTick)` on `PlaneEntity` to reliably expose smoothed render yaw to all Minecraft camera and entity rendering subsystems.

## 1.2.4

### Minecraft 26.3
- Ported to **Minecraft 26.3** (Fabric Loader `0.19.5`, Fabric API `0.160.7+26.3`).
- Updated resource pack format to `97.1` and datapack format to `121.0`.
- Fixed runway chest loot table for 26.3 (`rolls` needs `minecraft:uniform`, `functions` → `modifier`) so **Create New World** no longer hangs on *Preparing for world creation…*.

## 1.2.3

### 🎬 Replay Playback & Interpolation (Flashback / ReplayMod)
- **Smooth Replay Motion**: Integrated Minecraft 26.2 `InterpolationHandler` into `PlaneEntity`, enabling smooth 3-tick sub-frame interpolation for all aircraft and helicopters during replay playback.
- **Replay Detection (`ReplayPlayback`)**: Aircraft now detect active Flashback (`com.moulberry.flashback.Flashback.isInReplay()`) and ReplayMod playback sessions.
- **Physics Deconfliction**: Suspended client-side physics and server-side corrections during replay playback (`ownsClientSimulation`), preventing the vertical bouncing/jittering ("skakanie") caused by client simulation fighting recorded packets.
- **NPC & Remote Interpolation**: Remote player aircraft and villager pilot patrol craft now also utilize `InterpolationHandler`, ensuring silky-smooth flight paths for observers.

## 1.2.2

### 🔊 Engine Sounds
- **Airplane engine loop**: Occupied fixed-wing aircraft now play a looping radial-engine rumble. Volume and pitch follow throttle (idle while boarded, louder and higher at full power). Large biplanes are slightly louder; the twin-engine variant is deeper.
- **Helicopter rotor loop**: Helicopters play a distinct rotor whoosh. Quiet spool-up on the ground with **W**, full rotor sound once airborne, with pitch shifting as collective changes.
- Engine audio is positional (heard from ~64 blocks) and plays for nearby players, including villager-piloted craft.
- **Helicopter landing**: Holding **S** now flares the descent near the ground but still settles onto the skids. All steering (cyclic and tail rotor) is locked while parked.

## 1.2.1

### 🛬 Vertical Descent-Speed Damping (Soft Landing)
- **Automatic Descent Braking**: When landing vertically with **S** (descend) in Simple Altitude Control mode, the helicopter automatically reduces its downward speed as it approaches the ground. Within 3 blocks of the surface the descent speed is scaled proportionally (down to a gentle `0.04 blocks/tick` minimum), ensuring a soft, controlled touchdown with no crash damage.
- **Liftoff Cursor Auto-Centering**: On takeoff, the pilot's look direction is automatically reset to straight-ahead/level, eliminating any leftover pitch or drift from pre-flight camera movement.

## 1.2.0

### 🚁 New Aircraft: The Helicopter (`peterwolfs_planes:helicopter`)
- **Vertical Takeoff and Landing (VTOL)**:
  - Takes off and lands vertically directly on heavy-duty tubular landing skids—no runway required!
  - Hover equilibrium at ~50% throttle, balancing gravity ($0.04\text{ blocks/tick}$) for a rock-solid stationary hover.
  - Throttle up (**W**) to climb; throttle down (**S**) to descend or initiate smooth autorotation landing.
- **Fuselage & Cockpit Visuals**:
  - Distinctive open minecart-style cockpit tub where the player sits with a clear forward view.
  - Heavy-duty tubular landing skids with front upturns, tuned with ground offset (`-1.625F`) so the bottom of the tubes rests flush with the ground surface.
  - Realistic cockpit instrumentation: functional analog speedometer and altimeter dials mounted on the dashboard.
  - Centrally mounted animated cyclic control stick directly in front of the pilot, dynamically reacting to pilot inputs and aircraft attitude.
  - High rotor mast with 4-blade horizontal main rotor (120 units wide) and vertical tail rotor, both spinning smoothly in real-time.
  - Aft engine deck, tail boom, and vertical tail fin.
- **Aviation Warning & Navigation Lighting (ICAO/FAA Compliant)**:
  - Port (left) red and starboard (right) green position lights.
  - Aft white navigation light on tail.
  - Synchronized high-intensity flashing white strobes and red anti-collision beacon on the vertical tail fin.
  - Emissive glowing layer (`RenderTypes.eyes`) active while the aircraft is occupied.
- **Crafting & World Integration**:
  - Survival crafting recipe using iron ingots, redstone, iron bars, and a minecart.
  - Added to creative inventory tab, starter kit (`/function peterwolfs_planes:kit`), and runway chest loot tables.

### 🎮 Helicopter Flight Controls & Aerodynamics
- **Instant Response (Zero Lag)**:
  - Removed slow low-pass smoothing (`0.12F`) on helicopter cyclic controls, replacing it with an immediate `0.60F` factor.
  - Eliminated camera fighting (`centerPilotLook`) on pitch, giving crisp 1:1 mouse steering with response times under 50 ms.
- **Exponential (Expo) Control Curve**:
  - Implemented an exponential response curve ($|x|^{1.85}$) for both pitch and roll cyclic controls.
  - Controls are gentle and precise near zero (neutral stick) for steady hovering and fine trimming, while smoothly ramping up to full authority at larger deflections.
- **Cyclic Stick Pitch (Mouse Forward / Backward)**:
  - **Mouse forward**: tilts cyclic stick forward, pitches nose down, and accelerates forward with strong horizontal thrust ($1.85\times$).
  - **Mouse backward**: pulls cyclic stick back, pitches nose up, and flares to brake forward momentum or fly backward.
  - **Cruise Attitude Hold**: pitch angle does not snap back to zero during forward flight, enabling continuous, comfortable cruising without endlessly dragging the mouse.
  - **Bounded Deflections**: clamped to realistic, safe limits (max $+16^\circ$ nose-down, $-12^\circ$ flare), preventing wild overshooting and runaway dives.
- **Cyclic Stick Roll & Lateral Flight (Mouse Left / Right)**:
  - Swapped left and right response: moving mouse left/right banks and propels the aircraft in the newly inverted direction.
  - Bank angle clamped to a safe $\pm 14^\circ$ envelope, maintaining stability without sudden tipping.
- **Anti-Torque Tail Rotor (Keys A & D)**:
  - Dedicated pedal control to rotate the tail boom and steer heading ("obrót ogona" / yaw authority) even while hovering motionless.
  - Keys **A** and **D** strictly govern tail rotor rotation without inducing roll.
- **Effortless Hover Stabilization**:
  - Simply looking towards the world horizon ($\pm 4^\circ$) automatically levels the aircraft back to $0.0^\circ$ pitch and engages a stable, motionless hover.
  - Deadband centering ensures easy, vibration-free hovering.
- **Aerodynamic Rotor Thrust & Translation Sink Physics**:
  - Tilting forward/backward (pitch) or sideways (roll) redirects rotor thrust away from the vertical axis, causing natural altitude loss (sink / opadanie) rather than climbing.
  - Forward, backward, and lateral horizontal airspeed induces aerodynamic sink ($v_{\text{horiz}} \cdot 0.024$).
  - Flying forward, backward, or strafing sideways naturally descends unless the pilot gently adds throttle (collective), exactly like real helicopters.
  - Aerodynamic parasite drag modeled for forward and lateral airflow.
- **Control Reset on Boarding**:
  - All control deflections (pitch, roll, rudder, cyclic stick, yaw rate, pitch rate) immediately reset to zero upon boarding the helicopter.

### 🎛️ Helicopter Simple Altitude Mode (Default) & Command Switcher
- **Simple Altitude Control (Default for Helicopters)**:
  - **W** (held): climbs smoothly upward (`Vertical: CLIMB % (W)`).
  - **S** (held): descends smoothly downward (`Vertical: DESCEND % (S)`).
  - **Neutral (releasing W / S)**: automatically locks and holds current altitude (`Vertical: ALT HOLD (W/S)`).
  - **Zero Maneuvering Sink**: pitching forward, backward, or banking sideways to fly and turn does **not** cause altitude loss—the helicopter maintains a steady, level flight altitude while cruising and maneuvering!
- **Realistic Mode Available**:
  - Realistic aerodynamic collective remains available where cyclic tilt and translational airspeed induce natural sink that must be compensated with collective pitch.
- **Commands & Switcher**:
  - `/helialt` or `/helicontrol` or `/planes helicopter altitude`: toggles between Simple and Realistic altitude modes.
  - `/helialt simple` or `/planes helicopter altitude simple`: sets altitude control to Simple mode.
  - `/helialt realistic` or `/planes helicopter altitude realistic`: enables realistic aerodynamic collective control.
  - Running while piloting a helicopter switches the current vehicle and updates player default; running outside updates default for future helicopters.

### 🛑 Ground Detection & Automatic Steering Lock
- **Automatic Ground State Recognition**:
  - The helicopter accurately detects when resting on the ground, helipad, or landing surface.
  - Pinned solidly to the terrain with zero jitter, zero micro-sliding, and zero uncontrolled drift.
- **Steering Disabled on Ground**:
  - While on the ground, cyclic pitch (forward/backward) and cyclic roll (sideways steering) are **completely disabled**.
  - Moving the mouse does not tilt the helicopter or cause accidental forward/backward sliding.
  - Airframe stays perfectly flat ($0.0^\circ$ pitch, $0.0^\circ$ roll) on its heavy-duty landing skids.
  - Tail rotor pedals (**A** / **D** keys) remain active, allowing pilots to steer heading in place to align on the helipad before takeoff.
  - HUD control stick indicator displays `GROUND` in amber and locks the cyclic crosshair centered at `(0, 0)`.
  - HUD status displays `Helicopter: ON GROUND (W to fly)`.
- **Vertical Liftoff Activation & Auto-Centering Cursor**:
  - Holding **W** lifts the helicopter strictly vertically into the air (`Takeoff: LIFTOFF % (W)`).
  - The moment the skids clear the ground, the helicopter switches into airborne flight mode (`isHelicopterAirborne = true`).
  - **Automatic Cursor Zeroing**: The instant liftoff occurs, the pilot's cursor/look direction (pitch $0.0^\circ$ at horizon, yaw straight ahead) and cyclic stick are automatically snapped to neutral center `(0, 0)`, preventing sudden lurches and starting flight in a stable hover.
  - Flight controls (forward/backward, sideways bank, cyclic stick) immediately activate once airborne.
- **Smooth Touchdown & Landing Lock**:
  - Upon descending to the surface and making ground contact with low/descending throttle, the helicopter smoothly touches down:
    - Pitch and roll are auto-leveled to $0.0^\circ$.
    - Flight steering immediately locks to prevent tipping over or sliding across blocks.
    - Airframe enters parked state (`PARKED`).

### 🕹️ HUD 2D Control Stick Indicator (Cyclic & Centering Guide)
- **Visual Control Stick Widget**:
  - Mounted directly on the right side of the cockpit HUD (docked seamlessly next to instruments).
  - Features an analog 2D crosshair arena displaying active cyclic stick position (Pitch on vertical axis, Roll on horizontal axis).
  - Instant visual feedback for flight controls without having to look down into the cockpit tub.
- **Effortless Center & Hover Locking**:
  - Center target box deadband illuminates in vibrant neon green (`[+]` / `CENTER`) when cyclic controls are centered.
  - Allows pilots using mouse steering to effortlessly find the neutral 0 trim position for a rock-solid, stationary hover.
  - Marker dot dynamically shifts color: bright neon green when centered, cyan during normal maneuvering, and red at extreme deflection limits.
- **Tail Rotor / Rudder Pedal Bar**:
  - Integrated 1D horizontal deflection bar displaying active anti-torque tail rotor / rudder pedal input (**A** / **D** keys).
- **Multi-Layout HUD Support**:
  - Supported across all three HUD modes: full instrumentation panel, compact mini HUD, and top-right docked layout.
  - In full combat HUD mode, provides real-time flight regime badges (`HOVER`, `CRUISE`, `MANVR`) and numerical pitch/roll degree readouts.

## 1.1.20

- Navigation lights, strobes, and the anti-collision beacon now light only while the aircraft is occupied (player or villager pilot).

## 1.1.19

- In the cockpit, **H** cycles instrument HUD layouts: full panel, compact mini, and a top-right strip.
- LIFT HUD (ridge climb/sink) only appears during paraglider flight; **H** toggles it there and no longer overlays the plane instruments.

## 1.1.18

- Hard landings now damage the airframe: sink rate above a gentle flare chips HUD HP, and a vertical smash can wreck the aircraft.
- Nose-down, heavy-bank, and very fast touchdowns increase impact severity; passengers take fall damage on the worst hits.
- Floatplanes tolerate a firmer water landing than wheeled aircraft, but slamming onto land still hurts.

## 1.1.17

- Add a cockpit control stick to every aircraft, with a floor mount and T-grip visible in first-person.
- Animate the stick from live pilot input: pull back to climb, push forward to descend, tilt left/right to bank, and blend in rudder for A/D turns.
- Villager-piloted aircraft drive the same stick from attitude so the cockpit still reads climb, descent, and bank.
- Invert stick deflection so it matches real control-column motion, and double the stick length.
- Lengthen the stick another 10%, cap it with a burgundy leather grip, and reduce stick throw so cockpit motion stays subtle.
- Reduce first-person cockpit head look to 10% of mouse offset so the camera stays on the aircraft while mouse flight controls stay at full authority.

## 1.1.16

- Add realistic international aviation warning and navigation lights (ICAO/FAA compliant) to all aircraft:
  - **Left Wingtip (Port)**: Steady Aviation Red position light + double-flash high-intensity white strobe.
  - **Right Wingtip (Starboard)**: Steady Aviation Green position light + double-flash high-intensity white strobe.
  - **Tail / Rudder (Aft)**: Steady Aviation White position light + tail strobe + flashing red anti-collision beacon on the vertical stabilizer.
- Dedicated emissive lighting rendering layer (`RenderTypes.eyes`) with fullbright illumination and shader bloom compatibility.
- Prevent villager pilot aircraft from prematurely entering `PARKED` state unless physically touchdown on runway ground surface (stops mid-air freezing on trees/obstacles).
- Add minimum cruise airspeed and positive forward velocity protection against aerodynamic stalls during NPC patrol and climb phases.

## 1.1.15

- Keep villager-piloted aircraft visually aligned with their synchronized flight heading instead of leaving the rendered fuselage at a stale yaw.
- Make the villager pilot follow the aircraft seat, pitch, and bank with a stable seated cockpit pose.
- After a randomized three-to-five-minute patrol, send villager pilots back to the nearest generated runway for an aligned approach, landing roll, braking, and parked stop.
- Add a dedicated pixel-art inventory texture for the Villager Pilot Spawn Egg.

## 1.1.14

- Rebuild the water plane as a single high-wing floatplane with short fuselage-mounted wing supports.
- Complete the opaque water-plane entity atlas so the fuselage, floats, and enlarged UV faces no longer render with transparent gaps.
- Keep the parked water plane level on its two full-length floats instead of applying the conventional tail-low two-wheel ground pose.
- Stop treating water contact as locked ground handling, restoring mouse-driven elevator and aileron authority while the floatplane accelerates and lifts off the water.
- Add deterministic ClientGameTest coverage for the parked high-wing silhouette and real mouse-driven pitch/roll response.
- Treat the water surface as float support while retaining live mouse controls, preventing stall pitch from tipping the aircraft onto its nose after landing and braking.
- Cut a full-chord cockpit opening into the high wing so its center section no longer blocks the first-person pilot view.
- Scale floatplane aileron authority with water speed so mouse input cannot roll a stationary aircraft, while control returns progressively during the takeoff run.
- Raise only the solid-ground render baseline by the exact float-bottom offset so the pontoons rest on terrain without changing their water immersion.
- Allow the Water Plane item to be deployed directly onto a targeted water surface while leaving land placement and other aircraft unchanged.

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
