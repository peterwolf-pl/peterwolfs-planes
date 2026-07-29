package com.piotrek.peterwolfsplanes;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Optional ridge-lift particle visualization, toggled per player via
 * {@code /planes liftparticles} or {@code /liftparticles}.
 */
public final class ParagliderLiftParticles {
	private static final Set<UUID> ENABLED = new HashSet<>();
	private static final int SCAN_INTERVAL = 10;
	private static final int SCAN_RADIUS = 12;
	private static final int SCAN_STEP = 4;

	private ParagliderLiftParticles() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (ENABLED.isEmpty()) {
				return;
			}
			if (server.getTickCount() % SCAN_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!ENABLED.contains(player.getUUID())) {
					continue;
				}
				if (!(player.level() instanceof ServerLevel level)) {
					continue;
				}
				spawnAround(level, player);
			}
		});
	}

	public static boolean isEnabled(ServerPlayer player) {
		return ENABLED.contains(player.getUUID());
	}

	public static boolean toggle(ServerPlayer player) {
		if (ENABLED.contains(player.getUUID())) {
			ENABLED.remove(player.getUUID());
			return false;
		}
		ENABLED.add(player.getUUID());
		return true;
	}

	public static void setEnabled(ServerPlayer player, boolean enabled) {
		if (enabled) {
			ENABLED.add(player.getUUID());
		} else {
			ENABLED.remove(player.getUUID());
		}
	}

	public static void clear(ServerPlayer player) {
		ENABLED.remove(player.getUUID());
	}

	private static void spawnAround(ServerLevel level, ServerPlayer player) {
		int cx = Mth.floor(player.getX());
		int cz = Mth.floor(player.getZ());

		// Local stream when the pilot is inside lift
		double localLift = ParagliderRidgeLift.sampleLift(level, player.getX(), player.getY(), player.getZ());
		if (localLift > 0.008D) {
			int count = 2 + (int) (localLift / ParagliderRidgeLift.MAX_LIFT * 6.0D);
			level.sendParticles(
				ParticleTypes.CLOUD,
				player.getX(), player.getY() + 0.2D, player.getZ(),
				count,
				0.55D, 0.35D, 0.55D,
				0.02D + localLift * 0.4D
			);
			level.sendParticles(
				ParticleTypes.END_ROD,
				player.getX(), player.getY(), player.getZ(),
				Math.max(1, count / 2),
				0.35D, 0.55D, 0.35D,
				0.01D + localLift * 0.25D
			);
		}

		// Sparse scan at pilot altitude — marks the lift band nearby without thrashing heightmaps
		double sampleY = player.getY();
		for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += SCAN_STEP) {
			for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += SCAN_STEP) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				int x = cx + dx;
				int z = cz + dz;
				double lift = ParagliderRidgeLift.sampleLift(level, x + 0.5D, sampleY, z + 0.5D);
				if (lift < 0.015D) {
					continue;
				}

				int n = 1 + (int) (lift / 0.07D);
				double rise = 0.04D + lift * 0.55D;
				level.sendParticles(
					ParticleTypes.CLOUD,
					x + 0.5D, sampleY, z + 0.5D,
					n,
					0.45D, 0.7D, 0.45D,
					rise
				);
				if (lift > 0.07D) {
					level.sendParticles(
						ParticleTypes.WHITE_ASH,
						x + 0.5D, sampleY - 0.2D, z + 0.5D,
						2,
						0.3D, 0.5D, 0.3D,
						rise * 0.55D
					);
				}
			}
		}
	}
}
