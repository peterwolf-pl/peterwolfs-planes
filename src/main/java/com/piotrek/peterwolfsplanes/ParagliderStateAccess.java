package com.piotrek.peterwolfsplanes;

/**
 * Exposes the paraglider deployment flag stored in vanilla synced entity data.
 *
 * <p>Using entity data makes the flag part of player spawn/snapshot packets,
 * so replay recorders can reconstruct a flight even when recording starts
 * after the paraglider was deployed.</p>
 */
public interface ParagliderStateAccess {
	boolean peterwolfsPlanes$isParagliderDeployed();

	void peterwolfsPlanes$setParagliderDeployed(boolean deployed);
}
