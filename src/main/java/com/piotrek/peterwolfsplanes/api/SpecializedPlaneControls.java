package com.piotrek.peterwolfsplanes.api;

/**
 * Optional contract for plane variants that replace dogfight combat controls
 * with specialized equipment (water bomber, cargo, crop duster, etc.).
 *
 * <p>Implemented by entity subclasses. Checked by the Planes client HUD / input
 * loop; never requires registration.
 */
public interface SpecializedPlaneControls {
	/**
	 * @return {@code true} if the standard combat V / B / gun bindings apply.
	 *         {@code false} when this aircraft uses specialized bindings instead.
	 */
	boolean usesCombatControls();
}
