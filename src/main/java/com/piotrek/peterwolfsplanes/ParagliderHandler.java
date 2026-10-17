package com.piotrek.peterwolfsplanes;

import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ParagliderHandler {
	private static final Set<Integer> DEPLOYED_PLAYERS = new HashSet<>();
	private static final Map<UUID, ParagliderFlightMode> FLIGHT_MODES = new HashMap<>();

	public static final double MIN_SINK_SPEED_MS = 1.2;

	public static boolean isDeployed(int entityId) {
		return DEPLOYED_PLAYERS.contains(entityId);
	}

	public static boolean isDeployed(Entity entity) {
		if (entity instanceof ParagliderStateAccess access) {
			return access.peterwolfsPlanes$isParagliderDeployed();
		}
		return isDeployed(entity.getId());
	}

	public static void setDeployed(int entityId, boolean deployed) {
		if (deployed) {
			DEPLOYED_PLAYERS.add(entityId);
		} else {
			DEPLOYED_PLAYERS.remove(entityId);
		}
	}

	public static void setDeployed(Entity entity, boolean deployed) {
		if (entity instanceof ParagliderStateAccess access) {
			access.peterwolfsPlanes$setParagliderDeployed(deployed);
		}
		setDeployed(entity.getId(), deployed);
	}

	public static void setFlightMode(ServerPlayer player, ParagliderFlightMode mode) {
		FLIGHT_MODES.put(player.getUUID(), mode == null ? ParagliderFlightMode.CRUISE : mode);
	}

	public static ParagliderFlightMode getFlightMode(ServerPlayer player) {
		return FLIGHT_MODES.getOrDefault(player.getUUID(), ParagliderFlightMode.CRUISE);
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
			boolean hasBackpack = chest.is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK.get());
			boolean previouslyDeployed = isDeployed(player);

			boolean canGlide = !player.onGround()
				&& !player.isInWater()
				&& !player.isPassenger()
				&& !player.getAbilities().flying
				&& !player.isFallFlying();

			boolean shouldDeploy = false;
			if (hasBackpack && canGlide) {
				if (player.fallDistance >= 4.0 || previouslyDeployed) {
					shouldDeploy = true;

					ParagliderFlightMode mode = getFlightMode(player);
					Vec3 look = player.getLookAngle();
					double horizLen = Math.sqrt(look.x * look.x + look.z * look.z);

					double forwardX = horizLen > 0.001 ? (look.x / horizLen) * mode.horizontalSpeed() : 0.0;
					double forwardZ = horizLen > 0.001 ? (look.z / horizLen) * mode.horizontalSpeed() : 0.0;

					double ridgeLift = ParagliderRidgeLift.sampleLift(player.level(), player.getX(), player.getY(), player.getZ());
					double vy = mode.verticalSpeed() + ridgeLift;

					player.setDeltaMovement(new Vec3(forwardX, vy, forwardZ));
					player.hurtMarked = true;
					player.fallDistance = 0.0;
				}
			}

			if (shouldDeploy != previouslyDeployed) {
				setDeployed(player, shouldDeploy);
				ParagliderSyncPayload payload = new ParagliderSyncPayload(player.getId(), shouldDeploy);
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
			}

			if (!shouldDeploy) {
				FLIGHT_MODES.remove(player.getUUID());
			}
		}
	}
}
