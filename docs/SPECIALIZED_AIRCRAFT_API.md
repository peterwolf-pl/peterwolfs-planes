# Specialized Aircraft API

Small extension surface for other mods (e.g. Forest Fire water bomber) that subclass `PlaneEntity` / `LargePlaneEntity` without reimplementing flight.

## Independence

- **Peterwolf's Planes does not depend on Forest Fire** (or any other specialized aircraft mod).
- Optional content (water bomber, cargo, etc.) is owned by the other mod and only loads when Planes is present.
- Installing Planes alone never pulls in Forest Fire.

## Interfaces

### `com.piotrek.peterwolfsplanes.api.SpecializedPlaneControls`

```java
boolean usesCombatControls(); // default true if not implemented
```

When `false`:

* Planes client does **not** process V (combat) / B (bomb) while piloting this entity.
* Planes HUD hides dogfight weapon lines (specialized HUD may draw its own).

### `com.piotrek.peterwolfsplanes.api.PlaneCargoMass`

```java
float getCargoMassFactor(); // 1.0 = empty, >1.0 = heavier
```

Applied in `PlaneEntity` thrust/drag:

* thrust scales roughly as `1 / massFactor`
* base drag scales roughly as `massFactor`

Keep factors continuous (no step changes) so dumps feel smooth.

## Recommended integration pattern

1. Subclass `LargePlaneEntity` (or `PlaneEntity`).
2. Implement both interfaces.
3. Use your own C2S payload for specialized actions (do not overload `PlaneInputPayload` combat flags).
4. Synced entity data for tank / equipment state used by render + HUD.
5. Server remains authoritative for abilities (fill, dump, hose).

## Non-goals

* Full fluid capability API (Fluid API bridge is separate).
* Replacing client key registration — specialized mods register their own keys and only handle them when their entity is piloted.
