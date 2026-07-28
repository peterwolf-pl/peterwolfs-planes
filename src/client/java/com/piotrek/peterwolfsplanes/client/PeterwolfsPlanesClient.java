package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.peterwolfsplanes.ParagliderFlightMode;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.network.ParagliderInputPayload;
import com.piotrek.peterwolfsplanes.network.PlaneInputPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.piotrek.peterwolfsplanes.ParagliderHandler;
import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

public class PeterwolfsPlanesClient implements ClientModInitializer {
	public static float paragliderRoll = 0.0f;
	public static float paragliderPitch = 0.0f;
	private static float previousParagliderRoll = 0.0f;
	private static float previousParagliderPitch = 0.0f;
	private static int wTapTimer = 0;
	private static boolean wasWDown = false;
	private static boolean isDoubleWTapped = false;

	private static int aTapTimer = 0;
	private static boolean wasADown = false;
	private static boolean isDoubleATapped = false;

	private static int dTapTimer = 0;
	private static boolean wasDDown = false;
	private static boolean isDoubleDTapped = false;

	public static final ModelLayerLocation PLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "plane"),
		"main"
	);
	public static final ModelLayerLocation LARGE_PLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "large_plane"),
		"main"
	);
	public static final ModelLayerLocation LARGE_TWIN_ENGINE_PLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "large_twin_engine_plane"),
		"main"
	);
	public static final ModelLayerLocation TRIPLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "triplane"),
		"main"
	);
	public static final ModelLayerLocation WATER_PLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "water_plane"),
		"main"
	);
	public static final ModelLayerLocation MONOPLANE_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "monoplane"),
		"main"
	);
	public static final ModelLayerLocation PARAGLIDER_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "paraglider"),
		"main"
	);

	private static final KeyMapping.Category PLANES_CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "main")
	);

	/** Toggle dogfight combat mode while piloting. Default: V */
	public static final KeyMapping TOGGLE_COMBAT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.peterwolfs_planes.toggle_combat",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_V,
		PLANES_CATEGORY
	));
	/** Drop a lit TNT bomb while combat mode is armed. Default: B */
	public static final KeyMapping DROP_BOMB = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.peterwolfs_planes.drop_bomb",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_B,
		PLANES_CATEGORY
	));

	private static float lastSentThrottle = -1.0f;
	private static float lastSentRoll = 999.0f;
	private static boolean lastSentCombatMode = false;
	private static boolean lastSentFireGuns = false;
	private static ParagliderFlightMode lastSentParagliderMode;
	private static boolean combatModeActive = false;

	public static boolean isParagliderVisuallyDeployed(int entityId) {
		Minecraft client = Minecraft.getInstance();
		Entity entity = client.level == null ? null : client.level.getEntity(entityId);
		if (entity == null) {
			return ParagliderHandler.isDeployed(entityId);
		}
		return ParagliderHandler.isDeployed(entity) || FlashbackParagliderCompat.inferLegacyDeployment(entity);
	}

	public static float getParagliderRollForEntity(int entityId, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.player.getId() != entityId
			|| !isParagliderVisuallyDeployed(entityId)) {
			return 0.0f;
		}
		return Mth.lerp(partialTick, previousParagliderRoll, paragliderRoll);
	}

	public static float getParagliderPitchForEntity(int entityId, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.player.getId() != entityId
			|| !isParagliderVisuallyDeployed(entityId)) {
			return 0.0f;
		}
		return Mth.lerp(partialTick, previousParagliderPitch, paragliderPitch);
	}

	@Override
	public void onInitializeClient() {
		// Register Entity Renderer
		EntityRendererRegistry.register(PeterwolfsPlanesMod.PLANE_ENTITY, PlaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.LARGE_PLANE_ENTITY, LargePlaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.LARGE_TWIN_ENGINE_PLANE_ENTITY, LargeTwinEnginePlaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.TRIPLANE_ENTITY, TriplaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.WATER_PLANE_ENTITY, WaterPlaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.MONOPLANE_ENTITY, MonoplaneRenderer::new);
		EntityRendererRegistry.register(PeterwolfsPlanesMod.VILLAGER_PILOT_ENTITY, VillagerPilotRenderer::new);

		// Register Model Layer
		ModelLayerRegistry.registerModelLayer(PLANE_MODEL_LAYER, PlaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(LARGE_PLANE_MODEL_LAYER, LargePlaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(LARGE_TWIN_ENGINE_PLANE_MODEL_LAYER, LargeTwinEnginePlaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(TRIPLANE_MODEL_LAYER, TriplaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(WATER_PLANE_MODEL_LAYER, WaterPlaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(MONOPLANE_MODEL_LAYER, MonoplaneModel::createLayerDefinition);
		ModelLayerRegistry.registerModelLayer(PARAGLIDER_MODEL_LAYER, ParagliderModel::createLayerDefinition);

		// Register Paraglider Player Feature Renderer
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof AvatarRenderer playerRenderer) {
				ParagliderModel paragliderModel = new ParagliderModel(context.bakeLayer(PARAGLIDER_MODEL_LAYER));
				registrationHelper.register(new ParagliderFeatureRenderer(playerRenderer, paragliderModel));
			}
		});

		// Client S2C Receiver for Paraglider Sync
		ClientPlayNetworking.registerGlobalReceiver(ParagliderSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				Entity entity = context.client().level == null
					? null
					: context.client().level.getEntity(payload.entityId());
				if (entity == null) {
					ParagliderHandler.setDeployed(payload.entityId(), payload.deployed());
				} else {
					ParagliderHandler.setDeployed(entity, payload.deployed());
				}
			});
		});

		// Client Input & Prediction Loop
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				previousParagliderRoll = paragliderRoll;
				previousParagliderPitch = paragliderPitch;

				if (client.player.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK)) {
					if (isParagliderVisuallyDeployed(client.player.getId())) {
						boolean keyUp = client.options.keyUp.isDown();       // W
						boolean keyDown = client.options.keyDown.isDown();   // S
						boolean keyLeft = client.options.keyLeft.isDown();   // A
						boolean keyRight = client.options.keyRight.isDown(); // D

						// Double W tap detection
						if (wTapTimer > 0) wTapTimer--;
						if (keyUp && !wasWDown) {
							if (wTapTimer > 0) {
								isDoubleWTapped = true;
							} else {
								wTapTimer = 8; // ~400ms window
							}
						}
						wasWDown = keyUp;
						if (!keyUp) {
							isDoubleWTapped = false;
						}

						// Double A tap detection (Spiral Dive Left)
						if (aTapTimer > 0) aTapTimer--;
						if (keyLeft && !wasADown) {
							if (aTapTimer > 0) {
								isDoubleATapped = true;
							} else {
								aTapTimer = 8;
							}
						}
						wasADown = keyLeft;
						if (!keyLeft) {
							isDoubleATapped = false;
						}

						// Double D tap detection (Spiral Dive Right)
						if (dTapTimer > 0) dTapTimer--;
						if (keyRight && !wasDDown) {
							if (dTapTimer > 0) {
								isDoubleDTapped = true;
							} else {
								dTapTimer = 8;
							}
						}
						wasDDown = keyRight;
						if (!keyRight) {
							isDoubleDTapped = false;
						}

						boolean spiralLeft = isDoubleATapped && keyLeft;
						boolean spiralRight = isDoubleDTapped && keyRight;
						boolean doubleWDive = isDoubleWTapped && keyUp;
						boolean spiralDive = spiralLeft || spiralRight;

						ParagliderFlightMode flightMode;
						if (doubleWDive && spiralDive) {
							flightMode = ParagliderFlightMode.DOUBLE_W_SPIRAL;
						} else if (spiralDive) {
							flightMode = ParagliderFlightMode.SPIRAL;
						} else if (doubleWDive) {
							flightMode = ParagliderFlightMode.DOUBLE_W;
						} else if (keyUp) {
							flightMode = ParagliderFlightMode.SINGLE_W;
						} else if (keyDown) {
							flightMode = ParagliderFlightMode.FLARE;
						} else {
							flightMode = ParagliderFlightMode.CRUISE;
						}

						float yaw = client.player.getYRot();
						float targetRoll = 0.0f;
						float targetPitch = 0.0f;

						if (spiralLeft) {
							yaw -= 8.0f;
							targetRoll = 45.0f;
						} else if (spiralRight) {
							yaw += 8.0f;
							targetRoll = -45.0f;
						} else if (keyLeft) {
							yaw -= 3.0f;
							targetRoll = 25.0f; // Inverted roll for A (tilt into turn)
						} else if (keyRight) {
							yaw += 3.0f;
							targetRoll = -25.0f; // Inverted roll for D (tilt into turn)
						}

						client.player.setYRot(yaw);
						paragliderRoll = paragliderRoll + (targetRoll - paragliderRoll) * 0.2f;

						if (flightMode == ParagliderFlightMode.DOUBLE_W_SPIRAL
							|| flightMode == ParagliderFlightMode.DOUBLE_W) {
							targetPitch = 35.0f;
						} else if (flightMode == ParagliderFlightMode.SPIRAL) {
							targetPitch = 25.0f;
						} else if (flightMode == ParagliderFlightMode.SINGLE_W) {
							targetPitch = 20.0f;
						} else if (flightMode == ParagliderFlightMode.FLARE) {
							targetPitch = -18.0f;
						}
						paragliderPitch = paragliderPitch + (targetPitch - paragliderPitch) * 0.2f;

						double rad = Math.toRadians(yaw);
						double moveX = -Math.sin(rad) * flightMode.horizontalSpeed();
						double moveZ = Math.cos(rad) * flightMode.horizontalSpeed();

						client.player.setDeltaMovement(new Vec3(moveX, flightMode.verticalSpeed(), moveZ));
						client.player.fallDistance = 0.0F;

						if (flightMode != lastSentParagliderMode) {
							lastSentParagliderMode = flightMode;
							ClientPlayNetworking.send(new ParagliderInputPayload(flightMode));
						}
					} else {
						paragliderRoll = 0.0f;
						paragliderPitch = 0.0f;
						isDoubleWTapped = false;
						lastSentParagliderMode = null;
					}
				} else {
					paragliderRoll = 0.0f;
					paragliderPitch = 0.0f;
					isDoubleWTapped = false;
					lastSentParagliderMode = null;
				}

				if (client.player.getVehicle() instanceof PlaneEntity plane) {
					boolean keyUp = client.options.keyUp.isDown();
					boolean keyDown = client.options.keyDown.isDown();
					boolean keyLeft = client.options.keyLeft.isDown();
					boolean keyRight = client.options.keyRight.isDown();

					float t = plane.getThrottle();
					if (keyUp) {
						t = Math.min(1.0f, t + 0.02f);
					} else if (keyDown) {
						t = Math.max(-1.0f, t - 0.02f);
					}

					float rudder = 0.0f;
					if (keyLeft) {
						rudder = -1.0f;
					} else if (keyRight) {
						rudder = 1.0f;
					}

					// Edge-triggered combat arming / bomb release
					while (TOGGLE_COMBAT.consumeClick()) {
						combatModeActive = !combatModeActive;
					}

					boolean fireGuns = combatModeActive && client.options.keyAttack.isDown();
					boolean dropBomb = false;
					while (DROP_BOMB.consumeClick()) {
						if (combatModeActive) {
							dropBomb = true;
						}
					}

					plane.setThrottle(t);
					plane.setRudder(rudder);
					// Local prediction of combat flag for HUD
					plane.setCombatMode(combatModeActive);

					boolean shouldSend = t != lastSentThrottle
						|| rudder != lastSentRoll
						|| combatModeActive != lastSentCombatMode
						|| fireGuns != lastSentFireGuns
						|| dropBomb;
					if (shouldSend) {
						lastSentThrottle = t;
						lastSentRoll = rudder;
						lastSentCombatMode = combatModeActive;
						lastSentFireGuns = fireGuns;
						ClientPlayNetworking.send(new PlaneInputPayload(t, rudder, combatModeActive, fireGuns, dropBomb));
					}
				} else {
					lastSentThrottle = -1.0f;
					lastSentRoll = 999.0f;
					lastSentCombatMode = false;
					lastSentFireGuns = false;
					combatModeActive = false;
				}
			} else {
				previousParagliderRoll = 0.0f;
				previousParagliderPitch = 0.0f;
				paragliderRoll = 0.0f;
				paragliderPitch = 0.0f;
				lastSentParagliderMode = null;
			}
		});

		// HUD overlay for plane instrumentation
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "hud_overlay"),
			(graphicsExtractor, deltaTracker) -> {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null && client.player.getVehicle() instanceof PlaneEntity plane) {
					Font font = client.font;

					// Get physics values
					double speedMs = plane.getInstrumentSpeedMetersPerSecond();
					double speedKmh = speedMs * 3.6;
					double altitude = plane.getY();
					double vsMs = plane.getInstrumentVerticalSpeedMetersPerSecond();

					// Format strings
					String speedText = String.format("Speed: %.1f m/s (%.1f km/h)", speedMs, speedKmh);
					String altText = String.format("Altitude: %.1f m", altitude);
					String vsText = String.format("V/S: %+.1f m/s", vsMs);

					float throttle = plane.getThrottle();
					String throttleText;
					int throttleColor = 0xFFFFFFFF;
					if (throttle > 0.0f) {
						throttleText = String.format("Throttle: %.0f%%", throttle * 100.0);
						throttleColor = 0xFF55FFFF; // cyan
					} else if (throttle < 0.0f) {
						throttleText = String.format("Brake: %.0f%%", -throttle * 100.0);
						throttleColor = 0xFFFF5555; // red
					} else {
						throttleText = "Throttle: Idle";
						throttleColor = 0xFFFFAA00; // orange
					}

					boolean combat = plane.isCombatMode();
					String combatText = combat ? "Combat: ARMED" : "Combat: SAFE";
					int combatColor = combat ? 0xFFFF5555 : 0xFFAAAAAA;
					float hp = plane.getCombatHealth();
					float maxHp = plane.getMaxCombatHealth();
					String hpText = String.format("Airframe: %.0f/%.0f", hp, maxHp);
					int hpColor = hp > maxHp * 0.5f ? 0xFF55FF55 : (hp > maxHp * 0.25f ? 0xFFFFAA00 : 0xFFFF5555);
					String weaponsHint = combat
						? "Guns: LMB  Bomb: B (TNT)"
						: "Press V for dogfight";

					// Determine V/S color: Green for climb (> 0.2), Red for sink (< -0.2), Gray/white otherwise
					int vsColor = 0xFFCCCCCC; // light gray
					if (vsMs > 0.2) {
						vsColor = 0xFF55FF55; // light green
					} else if (vsMs < -0.2) {
						vsColor = 0xFFFF5555; // light red
					}

					// Draw transparent background box for instrumentation (sleek glassmorphic box)
					// Box coordinates: centered horizontally, lowered by 20% vertically from the center
					int screenWidth = client.getWindow().getGuiScaledWidth();
					int screenHeight = client.getWindow().getGuiScaledHeight();
					int boxWidth = 168;
					int boxHeight = 93;
					int boxX = (screenWidth - boxWidth) / 2;
					int boxY = (screenHeight - boxHeight) / 2 + (int) (screenHeight * 0.18);
					graphicsExtractor.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000); // 50% opacity black

					// Draw border
					graphicsExtractor.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF555555); // Top border
					graphicsExtractor.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF555555); // Bottom border
					graphicsExtractor.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF555555); // Left border
					graphicsExtractor.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF555555); // Right border

					// Draw texts inside the box
					graphicsExtractor.text(font, speedText, boxX + 8, boxY + 6, 0xFFFFFFFF, false);
					graphicsExtractor.text(font, altText, boxX + 8, boxY + 18, 0xFFFFFFFF, false);
					graphicsExtractor.text(font, vsText, boxX + 8, boxY + 30, vsColor, false);
					graphicsExtractor.text(font, throttleText, boxX + 8, boxY + 42, throttleColor, false);
					graphicsExtractor.text(font, combatText, boxX + 8, boxY + 54, combatColor, false);
					graphicsExtractor.text(font, hpText, boxX + 8, boxY + 66, hpColor, false);
					graphicsExtractor.text(font, weaponsHint, boxX + 8, boxY + 78, 0xFFCCCCCC, false);
				}
			}
		);
	}
}
