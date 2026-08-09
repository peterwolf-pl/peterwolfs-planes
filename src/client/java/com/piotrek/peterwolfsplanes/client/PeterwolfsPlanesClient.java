package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.peterwolfsplanes.ParagliderFlightMode;
import com.piotrek.peterwolfsplanes.ParagliderRidgeLift;
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
import net.minecraft.network.chat.Component;
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
	/** Left brake pull 0..1 from A / spiral-left (not from camera yaw). */
	public static float paragliderLeftBrake = 0.0f;
	/** Right brake pull 0..1 from D / spiral-right (not from camera yaw). */
	public static float paragliderRightBrake = 0.0f;
	private static float previousParagliderRoll = 0.0f;
	private static float previousParagliderPitch = 0.0f;
	private static float previousParagliderLeftBrake = 0.0f;
	private static float previousParagliderRightBrake = 0.0f;
	private static int wTapTimer = 0;
	private static boolean wasWDown = false;
	private static boolean isDoubleWTapped = false;

	private static int aTapTimer = 0;
	private static boolean wasADown = false;
	private static boolean isDoubleATapped = false;

	private static int dTapTimer = 0;
	private static boolean wasDDown = false;
	private static boolean isDoubleDTapped = false;

	/** Double-tap S locks minimum-sink (LOCKED_FLARE) until toggled off. */
	private static int sTapTimer = 0;
	private static boolean wasSDown = false;
	private static boolean isFlareLocked = false;

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
	/** Toggle ridge-lift strength HUD. Default: H */
	public static final KeyMapping TOGGLE_LIFT_HUD = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.peterwolfs_planes.toggle_lift_hud",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_H,
		PLANES_CATEGORY
	));

	private static boolean liftHudVisible = false;
	private static float lastSentThrottle = -1.0f;
	private static float lastSentRoll = 999.0f;
	private static boolean lastSentCombatMode = false;
	private static boolean lastSentFireGuns = false;
	private static ParagliderFlightMode lastSentParagliderMode;
	private static boolean combatModeActive = false;

	/** True when the plane uses dogfight V/B combat bindings (default). */
	private static boolean usesCombatControls(PlaneEntity plane) {
		if (plane instanceof com.piotrek.peterwolfsplanes.api.SpecializedPlaneControls specialized) {
			return specialized.usesCombatControls();
		}
		return true;
	}

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

	/**
	 * Smoothed left brake input (0..1) driven by the same A / double-A steering
	 * used for yaw and bank — not inferred from camera rotation alone.
	 */
	public static float getParagliderLeftBrakeForEntity(int entityId, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.player.getId() != entityId
			|| !isParagliderVisuallyDeployed(entityId)) {
			return 0.0f;
		}
		return Mth.lerp(partialTick, previousParagliderLeftBrake, paragliderLeftBrake);
	}

	/**
	 * Smoothed right brake input (0..1) driven by the same D / double-D steering
	 * used for yaw and bank — not inferred from camera rotation alone.
	 */
	public static float getParagliderRightBrakeForEntity(int entityId, float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.player.getId() != entityId
			|| !isParagliderVisuallyDeployed(entityId)) {
			return 0.0f;
		}
		return Mth.lerp(partialTick, previousParagliderRightBrake, paragliderRightBrake);
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
				boolean specializedPilot = client.player.getVehicle() instanceof PlaneEntity vehicle
					&& !usesCombatControls(vehicle);
				while (TOGGLE_LIFT_HUD.consumeClick()) {
					// Specialized aircraft (e.g. water bomber) reclaim H for equipment.
					if (specializedPilot) {
						continue;
					}
					liftHudVisible = !liftHudVisible;
					client.player.sendOverlayMessage(Component.translatable(
						liftHudVisible
							? "message.peterwolfs_planes.lift_hud_on"
							: "message.peterwolfs_planes.lift_hud_off"
					));
				}

				previousParagliderRoll = paragliderRoll;
				previousParagliderPitch = paragliderPitch;
				previousParagliderLeftBrake = paragliderLeftBrake;
				previousParagliderRightBrake = paragliderRightBrake;

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

						// Double S tap: toggle permanent minimum-sink (LOCKED_FLARE)
						if (sTapTimer > 0) sTapTimer--;
						if (keyDown && !wasSDown) {
							if (sTapTimer > 0) {
								isFlareLocked = !isFlareLocked;
								sTapTimer = 0;
								client.player.sendOverlayMessage(Component.translatable(
									isFlareLocked
										? "message.peterwolfs_planes.flare_locked"
										: "message.peterwolfs_planes.flare_unlocked"
								));
							} else {
								sTapTimer = 8;
							}
						}
						wasSDown = keyDown;

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
						} else if (isFlareLocked) {
							// Locked S: stay in minimum sink until double-S again
							flightMode = ParagliderFlightMode.LOCKED_FLARE;
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

						// Brake-line / hand / wing-tip visuals: drive from actual steering keys
						// (A/D and spiral double-taps), not from camera look alone.
						float targetLeftBrake = 0.0f;
						float targetRightBrake = 0.0f;
						if (spiralLeft) {
							targetLeftBrake = 1.0f;
						} else if (keyLeft) {
							targetLeftBrake = 0.62f;
						}
						if (spiralRight) {
							targetRightBrake = 1.0f;
						} else if (keyRight) {
							targetRightBrake = 0.62f;
						}
						paragliderLeftBrake = paragliderLeftBrake + (targetLeftBrake - paragliderLeftBrake) * 0.2f;
						paragliderRightBrake = paragliderRightBrake + (targetRightBrake - paragliderRightBrake) * 0.2f;

						if (flightMode == ParagliderFlightMode.DOUBLE_W_SPIRAL
							|| flightMode == ParagliderFlightMode.DOUBLE_W) {
							targetPitch = 35.0f;
						} else if (flightMode == ParagliderFlightMode.SPIRAL) {
							targetPitch = 25.0f;
						} else if (flightMode == ParagliderFlightMode.SINGLE_W) {
							targetPitch = 20.0f;
						} else if (flightMode.isFlareFamily()) {
							targetPitch = -18.0f;
						}
						paragliderPitch = paragliderPitch + (targetPitch - paragliderPitch) * 0.2f;

						double rad = Math.toRadians(yaw);
						double moveX = -Math.sin(rad) * flightMode.horizontalSpeed();
						double moveZ = Math.cos(rad) * flightMode.horizontalSpeed();

						double ridgeLift = ParagliderRidgeLift.sampleLift(
							client.player.level(),
							client.player.getX(),
							client.player.getY(),
							client.player.getZ()
						);
						client.player.setDeltaMovement(new Vec3(
							moveX,
							flightMode.verticalSpeed() + ridgeLift,
							moveZ
						));
						client.player.fallDistance = 0.0F;

						if (flightMode != lastSentParagliderMode) {
							lastSentParagliderMode = flightMode;
							ClientPlayNetworking.send(new ParagliderInputPayload(flightMode));
						}
					} else {
						paragliderRoll = 0.0f;
						paragliderPitch = 0.0f;
						paragliderLeftBrake = 0.0f;
						paragliderRightBrake = 0.0f;
						isDoubleWTapped = false;
						isFlareLocked = false;
						lastSentParagliderMode = null;
					}
				} else {
					paragliderRoll = 0.0f;
					paragliderPitch = 0.0f;
					paragliderLeftBrake = 0.0f;
					paragliderRightBrake = 0.0f;
					isDoubleWTapped = false;
					isFlareLocked = false;
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

					boolean combatControls = usesCombatControls(plane);

					// Edge-triggered combat arming / bomb release (skipped for specialized aircraft)
					if (combatControls) {
						while (TOGGLE_COMBAT.consumeClick()) {
							combatModeActive = !combatModeActive;
						}
					} else {
						// Drain combat key clicks so they do not accumulate while specialized controls run
						while (TOGGLE_COMBAT.consumeClick()) {
							// no-op
						}
						while (DROP_BOMB.consumeClick()) {
							// no-op — specialized mod handles B
						}
						combatModeActive = false;
					}

					boolean fireGuns = combatControls && combatModeActive && client.options.keyAttack.isDown();
					boolean dropBomb = false;
					if (combatControls) {
						while (DROP_BOMB.consumeClick()) {
							if (combatModeActive) {
								dropBomb = true;
							}
						}
					}

					plane.setThrottle(t);
					plane.setRudder(rudder);
					// Local prediction of combat flag for HUD
					plane.setCombatMode(combatControls && combatModeActive);

					boolean shouldSend = t != lastSentThrottle
						|| rudder != lastSentRoll
						|| (combatControls && combatModeActive != lastSentCombatMode)
						|| fireGuns != lastSentFireGuns
						|| dropBomb;
					if (shouldSend) {
						lastSentThrottle = t;
						lastSentRoll = rudder;
						lastSentCombatMode = combatControls && combatModeActive;
						lastSentFireGuns = fireGuns;
						ClientPlayNetworking.send(new PlaneInputPayload(
							t, rudder, combatControls && combatModeActive, fireGuns, dropBomb
						));
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
				previousParagliderLeftBrake = 0.0f;
				previousParagliderRightBrake = 0.0f;
				paragliderRoll = 0.0f;
				paragliderPitch = 0.0f;
				paragliderLeftBrake = 0.0f;
				paragliderRightBrake = 0.0f;
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

					boolean showCombatHud = usesCombatControls(plane);
					boolean combat = showCombatHud && plane.isCombatMode();
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
					int boxHeight = showCombatHud ? 93 : 58;
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
					if (showCombatHud) {
						graphicsExtractor.text(font, combatText, boxX + 8, boxY + 54, combatColor, false);
						graphicsExtractor.text(font, hpText, boxX + 8, boxY + 66, hpColor, false);
						graphicsExtractor.text(font, weaponsHint, boxX + 8, boxY + 78, 0xFFCCCCCC, false);
					}
				}

				// LIFT HUD (H) — climb / sink + ridge updraft
				if (liftHudVisible && client.player != null && client.level != null) {
					Font font = client.font;

					// Actual vertical speed of the player (blocks/tick → m/s)
					double vyBt = client.player.getDeltaMovement().y;
					double vyMs = vyBt * 20.0D;
					String trend;
					int trendColor;
					if (vyMs > 0.15D) {
						trend = "CLIMB"; // wznoszenie
						trendColor = 0xFF55FF55;
					} else if (vyMs < -0.15D) {
						trend = "SINK"; // opadanie
						trendColor = 0xFFFF5555;
					} else {
						trend = "LEVEL";
						trendColor = 0xFFCCCCCC;
					}
					String vsText = String.format("V/S: %s  %+.2f m/s  (%+.3f b/t)", trend, vyMs, vyBt);

					// Ridge updraft contribution (prąd wznoszący)
					double ridge = ParagliderRidgeLift.sampleLift(
						client.level,
						client.player.getX(),
						client.player.getY(),
						client.player.getZ()
					);
					double ridgeMs = ridge * 20.0D;
					String ridgeLabel;
					int ridgeColor;
					if (ridge < 0.008D) {
						ridgeLabel = "NONE";
						ridgeColor = 0xFF888888;
					} else if (ridge < 0.04D) {
						ridgeLabel = "WEAK";
						ridgeColor = 0xFF55FFFF;
					} else if (ridge < 0.09D) {
						ridgeLabel = "MED";
						ridgeColor = 0xFF55FF55;
					} else {
						ridgeLabel = "STRONG";
						ridgeColor = 0xFFFFFF55;
					}
					String ridgeText = String.format("Ridge: %s  %+.2f m/s", ridgeLabel, ridgeMs);

					String modeHint;
					if (isParagliderVisuallyDeployed(client.player.getId())) {
						modeHint = isFlareLocked ? "Min-sink LOCKED" : "Paraglider";
					} else {
						modeHint = "H hide · /liftparticles";
					}

					int screenWidth = client.getWindow().getGuiScaledWidth();
					int boxWidth = 200;
					int boxHeight = 62;
					int boxX = screenWidth - boxWidth - 8;
					int boxY = 8;
					int barW = boxWidth - 16;
					int barX = boxX + 8;
					int barY = boxY + 53;
					int barH = 4;

					graphicsExtractor.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x99000000);
					graphicsExtractor.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF55AAFF);
					graphicsExtractor.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF555555);
					graphicsExtractor.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF555555);
					graphicsExtractor.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF555555);

					graphicsExtractor.text(font, "LIFT", boxX + 8, boxY + 5, 0xFF55AAFF, false);
					graphicsExtractor.text(font, vsText, boxX + 8, boxY + 17, trendColor, false);
					graphicsExtractor.text(font, ridgeText, boxX + 8, boxY + 29, ridgeColor, false);
					graphicsExtractor.text(font, modeHint, boxX + 8, boxY + 41, 0xFFAAAAAA, false);

					// Centered V/S bar: left = sink (red), right = climb (green)
					// Scale: ±4 m/s full deflection
					double vsScale = 4.0D;
					double frac = Mth.clamp(vyMs / vsScale, -1.0D, 1.0D);
					int mid = barX + barW / 2;
					graphicsExtractor.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
					// zero marker
					graphicsExtractor.fill(mid - 1, barY - 1, mid + 1, barY + barH + 1, 0xFFAAAAAA);
					if (frac > 0.0D) {
						int fill = (int) (barW / 2.0D * frac);
						graphicsExtractor.fill(mid, barY, mid + fill, barY + barH, 0xFF55FF55);
					} else if (frac < 0.0D) {
						int fill = (int) (barW / 2.0D * -frac);
						graphicsExtractor.fill(mid - fill, barY, mid, barY + barH, 0xFFFF5555);
					}
				}
			}
		);
	}
}
