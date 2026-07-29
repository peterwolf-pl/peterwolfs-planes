package com.piotrek.peterwolfsplanes;

import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ParagliderHandler {
	private static final Set<Integer> DEPLOYED_PLAYERS = new HashSet<>();
	private static final Map<UUID, ParagliderFlightMode> FLIGHT_MODES = new HashMap<>();

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

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
				boolean wearing = chest.is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK);
				boolean currentlyDeployed = isDeployed(player);

				boolean airborne = !player.onGround()
					&& !player.isInWater()
					&& !player.isPassenger()
					&& !player.getAbilities().flying
					&& !player.isFallFlying();

				boolean shouldBeDeployed = false;

				if (wearing && airborne) {
					if (player.fallDistance >= 4.0F || currentlyDeployed) {
						shouldBeDeployed = true;

						ParagliderFlightMode mode = getFlightMode(player);
						Vec3 look = player.getLookAngle();
						double horizLen = Math.sqrt(look.x * look.x + look.z * look.z);
						double moveX = horizLen > 0.001D ? (look.x / horizLen) * mode.horizontalSpeed() : 0.0D;
						double moveZ = horizLen > 0.001D ? (look.z / horizLen) * mode.horizontalSpeed() : 0.0D;

						double ridgeLift = ParagliderRidgeLift.sampleLift(
							player.level(), player.getX(), player.getY(), player.getZ()
						);
						double vertical = mode.verticalSpeed() + ridgeLift;

						player.setDeltaMovement(new Vec3(moveX, vertical, moveZ));

						player.hurtMarked = true;
						player.fallDistance = 0.0F;
					}
				}

				if (shouldBeDeployed != currentlyDeployed) {
					setDeployed(player, shouldBeDeployed);
					ParagliderSyncPayload payload = new ParagliderSyncPayload(player.getId(), shouldBeDeployed);
					ServerPlayNetworking.send(player, payload);
					for (ServerPlayer tracking : PlayerLookup.tracking(player)) {
						ServerPlayNetworking.send(tracking, payload);
					}
				}

				if (!shouldBeDeployed) {
					FLIGHT_MODES.remove(player.getUUID());
				}
			}
		});
	}
}
