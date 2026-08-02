package com.piotrek.peterwolfsplanes.api;

/**
 * Variable cargo mass for specialized aircraft.
 * Factor multiplies drag and divides thrust relative to an empty airframe.
 *
 * <p>1.0 = empty baseline. Values should change smoothly (e.g. water tank drain).
 */
public interface PlaneCargoMass {
	/**
	 * @return mass factor ≥ 0.5 (clamped by physics). Typical range 1.0–1.6.
	 */
	float getCargoMassFactor();
}
