package com.piotrek.peterwolfsplanes;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Detects ridge / slope lift (prądy wznoszące) for paragliders.
 *
 * <p>A slope produces lift only when it is at least {@link #MIN_SIZE} blocks
 * high, long, and wide. Lift strength scales with relief (height) and
 * steepness.</p>
 */
public final class ParagliderRidgeLift {
	public static final int MIN_SIZE = 10;

	/** Max extra upward velocity from a huge cliff face (blocks / tick). */
	public static final double MAX_LIFT = 0.18D;
	/** Horizontal reach of the lift band past the face. */
	private static final double LIFT_BAND = 14.0D;
	/** How far above the ridge crest lift still works. */
	private static final double LIFT_CEILING = 22.0D;

	private static final int[][] FALL_DIRS = {
		{1, 0}, {-1, 0}, {0, 1}, {0, -1},
		{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
	};

	// Cheap tick cache — heightmaps are stable over short windows
	private static Level cachedLevel;
	private static int cachedTick = Integer.MIN_VALUE;
	private static int cachedBlockX;
	private static int cachedBlockY;
	private static int cachedBlockZ;
	private static double cachedLift;

	private ParagliderRidgeLift() {
	}

	/**
	 * @return additional vertical velocity in blocks/tick (always ≥ 0).
	 */
	public static double sampleLift(Level level, double x, double y, double z) {
		if (level == null) {
			return 0.0D;
		}

		int cx = Mth.floor(x);
		int cy = Mth.floor(y);
		int cz = Mth.floor(z);
		int tick = (int) (level.getGameTime() & 0x7FFFFFFF);
		// Reuse result for 4 ticks if still in the same block column/altitude band
		if (level == cachedLevel
			&& tick - cachedTick < 4
			&& cx == cachedBlockX
			&& cz == cachedBlockZ
			&& Math.abs(cy - cachedBlockY) <= 1) {
			return cachedLift;
		}

		double bestLift = 0.0D;

		for (int[] dir : FALL_DIRS) {
			int fdx = dir[0];
			int fdz = dir[1];
			// Perpendicular (right-hand) for width sampling
			int pdx = -fdz;
			int pdz = fdx;

			double lift = sampleFace(level, cx, cz, x, y, z, fdx, fdz, pdx, pdz);
			if (lift > bestLift) {
				bestLift = lift;
			}
		}

		cachedLevel = level;
		cachedTick = tick;
		cachedBlockX = cx;
		cachedBlockY = cy;
		cachedBlockZ = cz;
		cachedLift = bestLift;
		return bestLift;
	}

	private static double sampleFace(
		Level level,
		int cx,
		int cz,
		double px,
		double py,
		double pz,
		int fdx,
		int fdz,
		int pdx,
		int pdz
	) {
		// Profile heights along a 24-block fall line centered near the player
		final int half = 12;
		final int profileLen = half * 2 + 1;
		int[] heights = new int[profileLen];
		for (int i = 0; i < profileLen; i++) {
			int ox = (i - half) * fdx;
			int oz = (i - half) * fdz;
			// For diagonals, step still 1 in each axis → longer Euclidean; OK for size checks in blocks
			heights[i] = groundY(level, cx + ox, cz + oz);
		}

		// Search windows: length ≥ 10, height drop ≥ 10 (uphill index → downhill index)
		int bestDrop = 0;
		int bestStart = -1;
		int bestEnd = -1;
		for (int start = 0; start < profileLen; start++) {
			for (int end = start + MIN_SIZE; end < profileLen; end++) {
				int drop = heights[start] - heights[end];
				if (drop < MIN_SIZE) {
					continue;
				}
				int length = end - start;
				if (length < MIN_SIZE) {
					continue;
				}
				// Prefer taller / steeper faces
				if (drop > bestDrop || (drop == bestDrop && length < (bestEnd - bestStart))) {
					// Width check only for candidates better than current best
					if (hasMinWidth(level, cx, cz, half, start, end, fdx, fdz, pdx, pdz, drop)) {
						bestDrop = drop;
						bestStart = start;
						bestEnd = end;
					}
				}
			}
		}

		if (bestStart < 0) {
			return 0.0D;
		}

		int length = bestEnd - bestStart;
		int crestY = heights[bestStart];
		int footY = heights[bestEnd];
		int mid = (bestStart + bestEnd) / 2;

		// World position of mid-face and crest
		double midX = cx + (mid - half) * fdx;
		double midZ = cz + (mid - half) * fdz;
		double crestX = cx + (bestStart - half) * fdx;
		double crestZ = cz + (bestStart - half) * fdz;

		// Horizontal distance from player to the face line segment (mid is fine)
		double distToFace = Math.sqrt((px - midX) * (px - midX) + (pz - midZ) * (pz - midZ));
		if (distToFace > LIFT_BAND) {
			return 0.0D;
		}

		// Player should be roughly between foot and crest altitudes (plus ceiling above crest)
		if (py < footY - 2.0D || py > crestY + LIFT_CEILING) {
			return 0.0D;
		}

		// Prefer the air side of the slope: downhill of the crest along fall direction
		// Vector from crest to player dotted with fall dir should be ≥ 0 (on or past face)
		double alongFall = (px - crestX) * fdx + (pz - crestZ) * fdz;
		// Normalize for diagonals roughly
		double fallScale = Math.sqrt(fdx * fdx + fdz * fdz);
		alongFall /= fallScale;
		// Allow a little behind the crest (rotor side is weak / none)
		if (alongFall < -3.0D) {
			return 0.0D;
		}

		double steepness = bestDrop / (double) length; // blocks of rise per block of run
		double heightFactor = Mth.clamp(bestDrop / (double) MIN_SIZE, 1.0D, 4.0D);
		double steepFactor = Mth.clamp(steepness / 0.5D, 0.45D, 2.5D);
		double base = 0.035D * heightFactor * steepFactor;

		// Stronger near the face, weaker with distance
		double distFade = 1.0D - (distToFace / LIFT_BAND);
		// Stronger mid-band altitude (half way up the face), weaker very high above crest
		double faceFrac = Mth.clamp((py - footY) / Math.max(1.0D, bestDrop), 0.0D, 1.35D);
		double altShape = faceFrac <= 1.0D
			? 0.55D + 0.45D * Math.sin(faceFrac * Math.PI)
			: Mth.clamp(1.0D - (faceFrac - 1.0D) * 1.4D, 0.0D, 1.0D);

		return Mth.clamp(base * distFade * altShape, 0.0D, MAX_LIFT);
	}

	/**
	 * Width ≥ 10: at least 10 perpendicular offsets where the same fall line still drops ≥ 8.
	 */
	private static boolean hasMinWidth(
		Level level,
		int cx,
		int cz,
		int half,
		int start,
		int end,
		int fdx,
		int fdz,
		int pdx,
		int pdz,
		int mainDrop
	) {
		int needDrop = Math.max(8, mainDrop / 2);
		int good = 0;
		// Offsets -12..12 → can reach width 10 easily
		for (int w = -12; w <= 12; w++) {
			int upX = cx + (start - half) * fdx + w * pdx;
			int upZ = cz + (start - half) * fdz + w * pdz;
			int downX = cx + (end - half) * fdx + w * pdx;
			int downZ = cz + (end - half) * fdz + w * pdz;
			int drop = groundY(level, upX, upZ) - groundY(level, downX, downZ);
			if (drop >= needDrop) {
				good++;
				if (good >= MIN_SIZE) {
					return true;
				}
			}
		}
		return false;
	}

	private static int groundY(Level level, int x, int z) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
	}

	/** Debug / HUD helper: whether any lift is present. */
	public static boolean hasLift(Level level, double x, double y, double z) {
		return sampleLift(level, x, y, z) > 0.005D;
	}
}
