package com.piotrek.peterwolfsplanes;

/**
 * Server-authoritative paraglider movement presets.
 *
 * <p>The descent ratio is expressed as vertical blocks lost per one horizontal
 * block travelled. Keeping speed and ratio together prevents client prediction
 * and server physics from assigning different sink rates to the same input.</p>
 */
public enum ParagliderFlightMode {
	CRUISE(0, 0.50D, 0.10D),
	SINGLE_W(1, 0.80D, 0.30D),
	DOUBLE_W(2, 1.10D, 0.50D),
	SPIRAL(3, 0.40D, 0.40D),
	DOUBLE_W_SPIRAL(4, 1.10D, 0.80D),
	/** Hold S — slow forward, shallow sink. */
	FLARE(5, 0.25D, 0.08D),
	/**
	 * Double-tap S lock — same shallow sink as flare, stays on until unlocked
	 * (double-tap S again) or deployment ends. Dive inputs (W / spirals) still override.
	 */
	LOCKED_FLARE(6, 0.25D, 0.05D);

	private final int networkId;
	private final double horizontalSpeed;
	private final double descentRatio;

	ParagliderFlightMode(int networkId, double horizontalSpeed, double descentRatio) {
		this.networkId = networkId;
		this.horizontalSpeed = horizontalSpeed;
		this.descentRatio = descentRatio;
	}

	public int networkId() {
		return networkId;
	}

	public double horizontalSpeed() {
		return horizontalSpeed;
	}

	public double descentRatio() {
		return descentRatio;
	}

	public double verticalSpeed() {
		return -horizontalSpeed * descentRatio;
	}

	public double descentPerTenHorizontalBlocks() {
		return descentRatio * 10.0D;
	}

	public boolean isFlareFamily() {
		return this == FLARE || this == LOCKED_FLARE;
	}

	public static ParagliderFlightMode fromNetworkId(int networkId) {
		for (ParagliderFlightMode mode : values()) {
			if (mode.networkId == networkId) {
				return mode;
			}
		}
		return CRUISE;
	}
}
