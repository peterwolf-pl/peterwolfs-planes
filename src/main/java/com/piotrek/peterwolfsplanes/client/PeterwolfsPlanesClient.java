package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.peterwolfsplanes.ParagliderFlightMode;
import com.piotrek.peterwolfsplanes.ParagliderRidgeLift;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.network.ParagliderInputPayload;
import com.piotrek.peterwolfsplanes.network.PlaneInputPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;

import com.piotrek.peterwolfsplanes.ParagliderHandler;
import com.piotrek.peterwolfsplanes.network.ParagliderSyncPayload;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PeterwolfsPlanesMod.MOD_ID, dist = Dist.CLIENT)
public class PeterwolfsPlanesClient {
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
	private static final float NORMAL_BRAKE_PULL = 0.78f;
	private static final float SPIRAL_BRAKE_PULL = 1.0f;
	private static final float BRAKE_PULL_SMOOTHING = 0.22f;
	private static final float BRAKE_RELEASE_SMOOTHING = 0.14f;
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
	public static final ModelLayerLocation HELICOPTER_MODEL_LAYER = new ModelLayerLocation(
		Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "helicopter"),
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
	public static final KeyMapping TOGGLE_COMBAT = new KeyMapping(
		"key.peterwolfs_planes.toggle_combat",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_V,
		PLANES_CATEGORY
	);
	/** Drop a lit TNT bomb while combat mode is armed. Default: B */
	public static final KeyMapping DROP_BOMB = new KeyMapping(
		"key.peterwolfs_planes.drop_bomb",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_B,
		PLANES_CATEGORY
	);
	/** Toggle ridge-lift strength HUD. Default: H */
	public static final KeyMapping TOGGLE_LIFT_HUD = new KeyMapping(
		"key.peterwolfs_planes.toggle_lift_hud",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_H,
		PLANES_CATEGORY
	);

	private static boolean liftHudVisible = false;
	private static float lastSentThrottle = -1.0f;
	private static float lastSentRoll = 999.0f;
	private static boolean lastSentCombatMode = false;
	private static boolean lastSentFireGuns = false;
	private static ParagliderFlightMode lastSentParagliderMode;
	private static boolean combatModeActive = false;
	private static boolean lastSentHelicopterAirborne = false;

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
	 * Smoothed right brake input (0..1) driven by the same D / double-D steering.
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

	public PeterwolfsPlanesClient(IEventBus modBus) {
		modBus.addListener(PeterwolfsPlanesClient::registerRenderers);
		modBus.addListener(PeterwolfsPlanesClient::registerLayerDefinitions);
		modBus.addListener(PeterwolfsPlanesClient::addLayers);
		modBus.addListener(PeterwolfsPlanesClient::registerKeys);
		modBus.addListener(PeterwolfsPlanesClient::registerGuiLayers);
		modBus.addListener(PeterwolfsPlanesClient::registerClientPayloads);
		NeoForge.EVENT_BUS.addListener(PeterwolfsPlanesClient::onClientTick);
	}

	private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(PeterwolfsPlanesMod.PLANE_ENTITY.get(), PlaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.LARGE_PLANE_ENTITY.get(), LargePlaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.LARGE_TWIN_ENGINE_PLANE_ENTITY.get(), LargeTwinEnginePlaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.TRIPLANE_ENTITY.get(), TriplaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.WATER_PLANE_ENTITY.get(), WaterPlaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.MONOPLANE_ENTITY.get(), MonoplaneRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.HELICOPTER_ENTITY.get(), HelicopterRenderer::new);
		event.registerEntityRenderer(PeterwolfsPlanesMod.VILLAGER_PILOT_ENTITY.get(), VillagerPilotRenderer::new);
	}

	private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(PLANE_MODEL_LAYER, PlaneModel::createLayerDefinition);
		event.registerLayerDefinition(LARGE_PLANE_MODEL_LAYER, LargePlaneModel::createLayerDefinition);
		event.registerLayerDefinition(LARGE_TWIN_ENGINE_PLANE_MODEL_LAYER, LargeTwinEnginePlaneModel::createLayerDefinition);
		event.registerLayerDefinition(TRIPLANE_MODEL_LAYER, TriplaneModel::createLayerDefinition);
		event.registerLayerDefinition(WATER_PLANE_MODEL_LAYER, WaterPlaneModel::createLayerDefinition);
		event.registerLayerDefinition(MONOPLANE_MODEL_LAYER, MonoplaneModel::createLayerDefinition);
		event.registerLayerDefinition(HELICOPTER_MODEL_LAYER, HelicopterModel::createLayerDefinition);
		event.registerLayerDefinition(PARAGLIDER_MODEL_LAYER, ParagliderModel::createLayerDefinition);
	}

	private static void addLayers(EntityRenderersEvent.AddLayers event) {
		for (PlayerModelType skin : PlayerModelType.values()) {
			AvatarRenderer<?> playerRenderer = event.getPlayerRenderer(skin);
			if (playerRenderer != null) {
				ParagliderModel paragliderModel = new ParagliderModel(event.getContext().bakeLayer(PARAGLIDER_MODEL_LAYER));
				playerRenderer.addLayer(new ParagliderFeatureRenderer(playerRenderer, paragliderModel));
			}
		}
	}

	private static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(TOGGLE_COMBAT);
		event.register(DROP_BOMB);
		event.register(TOGGLE_LIFT_HUD);
	}

	private static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(PeterwolfsPlanesMod.id("hud_overlay"), PeterwolfsPlanesClient::renderHud);
	}

	private static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
		event.register(ParagliderSyncPayload.TYPE, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			Entity entity = client.level == null ? null : client.level.getEntity(payload.entityId());
			if (entity == null) {
				ParagliderHandler.setDeployed(payload.entityId(), payload.deployed());
			} else {
				ParagliderHandler.setDeployed(entity, payload.deployed());
			}
		});
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		AircraftEngineSounds.tick(client);
		if (client.player != null) {
			boolean specializedPilot = client.player.getVehicle() instanceof PlaneEntity vehicle
				&& !usesCombatControls(vehicle);
			while (TOGGLE_LIFT_HUD.consumeClick()) {
				// Specialized aircraft (e.g. water bomber) reclaim H for equipment.
				if (specializedPilot) {
					continue;
				}
				if (isParagliderVisuallyDeployed(client.player.getId())) {
					liftHudVisible = !liftHudVisible;
					client.player.displayClientMessage(Component.translatable(
						liftHudVisible
							? "message.peterwolfs_planes.lift_hud_on"
							: "message.peterwolfs_planes.lift_hud_off"
					), true);
				}
			}

			previousParagliderRoll = paragliderRoll;
			previousParagliderPitch = paragliderPitch;
			previousParagliderLeftBrake = paragliderLeftBrake;
			previousParagliderRightBrake = paragliderRightBrake;

			if (client.player.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK.get())) {
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
							client.player.displayClientMessage(Component.translatable(
								isFlareLocked
									? "message.peterwolfs_planes.flare_locked"
									: "message.peterwolfs_planes.flare_unlocked"
							), true);
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
						targetRoll = 25.0f;
					} else if (keyRight) {
						yaw += 3.0f;
						targetRoll = -25.0f;
					}

					client.player.setYRot(yaw);
					paragliderRoll = paragliderRoll + (targetRoll - paragliderRoll) * 0.2f;

					float targetLeftBrake = 0.0f;
					float targetRightBrake = 0.0f;
					if (spiralLeft) {
						targetLeftBrake = SPIRAL_BRAKE_PULL;
					} else if (spiralRight) {
						targetRightBrake = SPIRAL_BRAKE_PULL;
					} else {
						if (keyLeft) {
							targetLeftBrake = NORMAL_BRAKE_PULL;
						}
						if (keyRight) {
							targetRightBrake = NORMAL_BRAKE_PULL;
						}
					}
					paragliderLeftBrake = approachBrake(paragliderLeftBrake, targetLeftBrake);
					paragliderRightBrake = approachBrake(paragliderRightBrake, targetRightBrake);

					if (flightMode == ParagliderFlightMode.DOUBLE_W || flightMode == ParagliderFlightMode.DOUBLE_W_SPIRAL) {
						targetPitch = -15.0f;
					} else if (flightMode == ParagliderFlightMode.SINGLE_W) {
						targetPitch = -5.0f;
					} else if (flightMode == ParagliderFlightMode.LOCKED_FLARE || flightMode == ParagliderFlightMode.FLARE) {
						targetPitch = 12.0f;
					}
					paragliderPitch = paragliderPitch + (targetPitch - paragliderPitch) * 0.2f;

					if (lastSentParagliderMode != flightMode) {
						lastSentParagliderMode = flightMode;
						ClientPacketDistributor.sendToServer(new ParagliderInputPayload(flightMode));
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
				if (plane.isHelicopter() && plane.isSimpleAltitudeControl()) {
					if (keyUp) {
						t = Mth.approach(t, 1.0f, 0.08f);
					} else if (keyDown) {
						t = Mth.approach(t, -1.0f, 0.08f);
					} else {
						t = Mth.approach(t, 0.0f, 0.12f);
					}
				} else {
					if (keyUp) {
						t = Math.min(1.0f, t + 0.02f);
					} else if (keyDown) {
						t = Math.max(-1.0f, t - 0.02f);
					}
				}

				float rudder = 0.0f;
				if (keyLeft) rudder -= 1.0f;
				if (keyRight) rudder += 1.0f;

				boolean combatControls = usesCombatControls(plane);
				if (combatControls) {
					while (TOGGLE_COMBAT.consumeClick()) {
						combatModeActive = !combatModeActive;
						client.player.displayClientMessage(Component.translatable(
							combatModeActive ? "message.peterwolfs_planes.combat_mode_on" : "message.peterwolfs_planes.combat_mode_off"
						), true);
					}
				} else {
					combatModeActive = false;
				}

				boolean fireGuns = combatControls && combatModeActive && client.options.keyAttack.isDown();

				boolean dropBomb = false;
				if (combatControls) {
					while (DROP_BOMB.consumeClick()) {
						if (combatModeActive) {
							dropBomb = true;
						} else {
							client.player.displayClientMessage(Component.translatable("message.peterwolfs_planes.combat_mode_required"), true);
						}
					}
				}

				plane.setThrottle(t);
				plane.setRudder(rudder);
				plane.setCombatMode(combatControls && combatModeActive);

				boolean heliAirborne = plane.isHelicopterAirborne();
				if (plane.isHelicopter()) {
					if (keyUp && !heliAirborne && t > 0.05f) {
						heliAirborne = true;
					}
				}

				if (Math.abs(t - lastSentThrottle) > 0.001f
					|| Math.abs(rudder - lastSentRoll) > 0.001f
					|| (combatControls && combatModeActive != lastSentCombatMode)
					|| (combatControls && fireGuns != lastSentFireGuns)
					|| dropBomb
					|| (plane.isHelicopter() && heliAirborne != lastSentHelicopterAirborne)) {
					lastSentThrottle = t;
					lastSentRoll = rudder;
					lastSentCombatMode = combatControls && combatModeActive;
					lastSentFireGuns = combatControls && fireGuns;
					lastSentHelicopterAirborne = heliAirborne;
					ClientPacketDistributor.sendToServer(new PlaneInputPayload(
						t, rudder, combatControls && combatModeActive, fireGuns, dropBomb, heliAirborne
					));
				}
			} else {
				lastSentThrottle = -1.0f;
				lastSentRoll = 999.0f;
				lastSentCombatMode = false;
				lastSentFireGuns = false;
				lastSentHelicopterAirborne = false;
				combatModeActive = false;
			}
		}
	}

	private static float approachBrake(float current, float target) {
		float step = target > current ? BRAKE_PULL_SMOOTHING : BRAKE_RELEASE_SMOOTHING;
		return current + (target - current) * step;
	}

	private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && client.player.getVehicle() instanceof PlaneEntity plane) {
			Font font = client.font;

			double speedMs = plane.getInstrumentSpeedMetersPerSecond();
			double speedKmh = speedMs * 3.6;
			double altitude = plane.getY();
			double vsMs = plane.getInstrumentVerticalSpeedMetersPerSecond();

			String speedText = String.format("Speed: %.1f m/s (%.1f km/h)", speedMs, speedKmh);
			String altText = String.format("Altitude: %.1f m", altitude);
			String vsText = String.format("V/S: %+.1f m/s", vsMs);

			float throttle = plane.getThrottle();
			String throttleText;
			int throttleColor = 0xFFFFFFFF;
			if (plane.isHelicopter() && plane.isSimpleAltitudeControl()) {
				if (!plane.isHelicopterAirborne()) {
					if (throttle > 0.05f) {
						throttleText = String.format("Takeoff: LIFTOFF %.0f%% (W)", throttle * 100.0);
						throttleColor = 0xFF55FF55;
					} else {
						throttleText = "Helicopter: ON GROUND (W to fly)";
						throttleColor = 0xFFFFAA00;
					}
				} else {
					if (throttle > 0.05f) {
						throttleText = String.format("Vertical: CLIMB %.0f%% (W)", throttle * 100.0);
						throttleColor = 0xFF55FF55;
					} else if (throttle < -0.05f) {
						throttleText = String.format("Vertical: DESCEND %.0f%% (S)", -throttle * 100.0);
						throttleColor = 0xFFFF5555;
					} else {
						throttleText = "Vertical: ALT HOLD (W/S)";
						throttleColor = 0xFF55FFFF;
					}
				}
			} else if (plane.isHelicopter() && !plane.isHelicopterAirborne()) {
				if (throttle > 0.35f) {
					throttleText = String.format("Takeoff: SPOOL UP %.0f%% (W)", throttle * 100.0);
					throttleColor = 0xFF55FF55;
				} else {
					throttleText = "Helicopter: ON GROUND (W to fly)";
					throttleColor = 0xFFFFAA00;
				}
			} else {
				if (throttle > 0.0f) {
					throttleText = String.format("Throttle: %.0f%%", throttle * 100.0);
					throttleColor = 0xFF55FFFF;
				} else if (throttle < 0.0f) {
					throttleText = String.format("Brake: %.0f%%", -throttle * 100.0);
					throttleColor = 0xFFFF5555;
				} else {
					throttleText = "Throttle: Idle";
					throttleColor = 0xFFFFAA00;
				}
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

			int vsColor = 0xFFCCCCCC;
			if (vsMs > 0.2) {
				vsColor = 0xFF55FF55;
			} else if (vsMs < -0.2) {
				vsColor = 0xFFFF5555;
			}

			int screenWidth = client.getWindow().getGuiScaledWidth();
			int screenHeight = client.getWindow().getGuiScaledHeight();
			int boxWidth = 168;
			int boxHeight = showCombatHud ? 93 : 58;
			int stickWidth = 54;
			int totalWidth = boxWidth + 4 + stickWidth;
			int boxX = (screenWidth - totalWidth) / 2;
			int boxY = (screenHeight - boxHeight) / 2 + (int) (screenHeight * 0.18);
			graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000);

			graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF555555);
			graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF555555);
			graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF555555);
			graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF555555);

			graphics.drawString(font, speedText, boxX + 8, boxY + 6, 0xFFFFFFFF, false);
			graphics.drawString(font, altText, boxX + 8, boxY + 18, 0xFFFFFFFF, false);
			graphics.drawString(font, vsText, boxX + 8, boxY + 30, vsColor, false);
			graphics.drawString(font, throttleText, boxX + 8, boxY + 42, throttleColor, false);
			if (showCombatHud) {
				graphics.drawString(font, combatText, boxX + 8, boxY + 54, combatColor, false);
				graphics.drawString(font, hpText, boxX + 8, boxY + 66, hpColor, false);
				graphics.drawString(font, weaponsHint, boxX + 8, boxY + 78, 0xFFCCCCCC, false);
			}

			renderStickIndicator(graphics, font, plane, client, boxX + boxWidth + 4, boxY, stickWidth, boxHeight);
		}

		if (liftHudVisible && client.player != null && client.level != null && isParagliderVisuallyDeployed(client.player.getId())) {
			Font font = client.font;

			double vyBt = client.player.getDeltaMovement().y;
			double vyMs = vyBt * 20.0D;
			String trend;
			int trendColor;
			if (vyMs > 0.15D) {
				trend = "CLIMB";
				trendColor = 0xFF55FF55;
			} else if (vyMs < -0.15D) {
				trend = "SINK";
				trendColor = 0xFFFF5555;
			} else {
				trend = "LEVEL";
				trendColor = 0xFFCCCCCC;
			}
			String vsText = String.format("V/S: %s  %+.2f m/s  (%+.3f b/t)", trend, vyMs, vyBt);

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

			String modeHint = isFlareLocked ? "Min-sink LOCKED" : "Paraglider";

			int screenWidth = client.getWindow().getGuiScaledWidth();
			int boxWidth = 200;
			int boxHeight = 62;
			int boxX = screenWidth - boxWidth - 8;
			int boxY = 8;
			int barW = boxWidth - 16;
			int barX = boxX + 8;
			int barY = boxY + 53;
			int barH = 4;

			graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x99000000);
			graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFF55AAFF);
			graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF555555);
			graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF555555);
			graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF555555);

			graphics.drawString(font, "LIFT", boxX + 8, boxY + 5, 0xFF55AAFF, false);
			graphics.drawString(font, vsText, boxX + 8, boxY + 17, trendColor, false);
			graphics.drawString(font, ridgeText, boxX + 8, boxY + 29, ridgeColor, false);
			graphics.drawString(font, modeHint, boxX + 8, boxY + 41, 0xFFAAAAAA, false);

			double vsScale = 4.0D;
			double frac = Mth.clamp(vyMs / vsScale, -1.0D, 1.0D);
			int mid = barX + barW / 2;
			graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
			graphics.fill(mid - 1, barY - 1, mid + 1, barY + barH + 1, 0xFFAAAAAA);
			if (frac > 0.0D) {
				int fill = (int) (barW / 2.0D * frac);
				graphics.fill(mid, barY, mid + fill, barY + barH, 0xFF55FF55);
			} else if (frac < 0.0D) {
				int fill = (int) (barW / 2.0D * -frac);
				graphics.fill(mid - fill, barY, mid, barY + barH, 0xFFFF5555);
			}
		}
	}

	private static void renderStickIndicator(
		GuiGraphics graphics,
		Font font,
		PlaneEntity plane,
		Minecraft client,
		int x,
		int y,
		int w,
		int h
	) {
		Player pilot = client.player;
		float stickDeflectionX = 0.0F;
		float stickDeflectionY = 0.0F;

		if (pilot != null) {
			float relPitch = Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - plane.getXRot()), -80.0F, 80.0F);
			float relYaw = Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - plane.getYRot()), -80.0F, 80.0F);

			if (plane.isHelicopter()) {
				if (!plane.isHelicopterAirborne()) {
					stickDeflectionX = 0.0F;
					stickDeflectionY = 0.0F;
				} else {
					stickDeflectionX = Mth.clamp((relYaw / 20.0F) + (plane.getRoll() / 14.0F), -1.0F, 1.0F);
					stickDeflectionY = Mth.clamp((plane.getXRot() / 16.0F) - (relPitch / 20.0F), -1.0F, 1.0F);
				}
			} else {
				float rudder = plane.getRudder();
				stickDeflectionX = Mth.clamp((relYaw / 30.0F) + (rudder * 0.5F) + (plane.getRoll() / 45.0F), -1.0F, 1.0F);
				stickDeflectionY = Mth.clamp((plane.getXRot() / 30.0F) - (relPitch / 30.0F), -1.0F, 1.0F);
			}
		}

		graphics.fill(x, y, x + w, y + h, 0x88000000);
		graphics.fill(x, y, x + w, y + 1, 0xFF555555);
		graphics.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
		graphics.fill(x, y, x + 1, y + h, 0xFF555555);
		graphics.fill(x + w - 1, y, x + w, y + h, 0xFF555555);

		boolean centered = Math.abs(stickDeflectionX) <= 0.16F && Math.abs(stickDeflectionY) <= 0.16F;

		String label;
		int labelColor;
		if (plane.isHelicopter() && !plane.isHelicopterAirborne()) {
			label = "GROUND";
			labelColor = 0xFFFFAA00;
		} else if (centered) {
			label = "CENTER";
			labelColor = 0xFF55FF55;
		} else {
			label = plane.isHelicopter() ? "CYCLIC" : "STICK";
			labelColor = 0xFFAAAAAA;
		}
		int labelX = x + (w - font.width(label)) / 2;
		graphics.drawString(font, label, labelX, y + 4, labelColor, false);

		int cx = x + w / 2;
		int cy = h >= 80 ? y + 28 : y + 31;
		int rx = 18;
		int ry = 14;

		graphics.fill(cx - rx, cy - ry, cx + rx + 1, cy + ry + 1, 0x44000000);
		graphics.fill(cx - rx, cy - ry, cx + rx + 1, cy - ry + 1, 0x33AAAAAA);
		graphics.fill(cx - rx, cy + ry, cx + rx + 1, cy + ry + 1, 0x33AAAAAA);
		graphics.fill(cx - rx, cy - ry, cx - rx + 1, cy + ry + 1, 0x33AAAAAA);
		graphics.fill(cx + rx, cy - ry, cx + rx + 1, cy + ry + 1, 0x33AAAAAA);

		graphics.fill(cx - rx + 2, cy, cx + rx - 1, cy + 1, 0x44888888);
		graphics.fill(cx, cy - ry + 2, cx + 1, cy + ry - 1, 0x44888888);

		int targetR = 3;
		int boxBorderColor = centered ? 0xFF55FF55 : 0x55888888;
		if (centered) {
			graphics.fill(cx - targetR, cy - targetR, cx + targetR + 1, cy + targetR + 1, 0x5500AA00);
		}
		graphics.fill(cx - targetR, cy - targetR, cx + targetR + 1, cy - targetR + 1, boxBorderColor);
		graphics.fill(cx - targetR, cy + targetR, cx + targetR + 1, cy + targetR + 1, boxBorderColor);
		graphics.fill(cx - targetR, cy - targetR, cx - targetR + 1, cy + targetR + 1, boxBorderColor);
		graphics.fill(cx + targetR, cy - targetR, cx + targetR + 1, cy + targetR + 1, boxBorderColor);

		int px = cx + Math.round(stickDeflectionX * rx);
		int py = cy - Math.round(stickDeflectionY * ry);
		px = Mth.clamp(px, cx - rx + 1, cx + rx - 1);
		py = Mth.clamp(py, cy - ry + 1, cy + ry - 1);

		int dotColor;
		if (centered) {
			dotColor = 0xFF55FF55;
		} else if (Math.abs(stickDeflectionX) > 0.75F || Math.abs(stickDeflectionY) > 0.75F) {
			dotColor = 0xFFFF5555;
		} else {
			dotColor = 0xFF55FFFF;
		}
		graphics.fill(px - 1, py - 1, px + 2, py + 2, dotColor);
		graphics.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);

		if (h >= 80) {
			boolean isHover = plane.isHelicopter() && plane.isHelicopterAirborne() && Math.abs(plane.getXRot()) < 4.0F && Math.abs(plane.getRoll()) < 4.0F && centered;
			String modeBadge = plane.isHelicopter()
				? (!plane.isHelicopterAirborne() ? "PARKED" : (isHover ? "HOVER" : (plane.getInstrumentSpeedMetersPerSecond() > 3.0 ? "CRUISE" : "MANVR")))
				: (plane.getInstrumentSpeedMetersPerSecond() > 3.0 ? "CRUISE" : "MANVR");
			int modeColor = (plane.isHelicopter() && !plane.isHelicopterAirborne()) ? 0xFFFFAA00 : (isHover ? 0xFF55FF55 : 0xFF55FFFF);
			graphics.drawString(font, modeBadge, x + (w - font.width(modeBadge)) / 2, y + 48, modeColor, false);

			String pText = String.format("P %+.0f°", plane.getXRot());
			String rText = String.format("R %+.0f°", plane.getRoll());
			graphics.drawString(font, pText, x + 6, y + 58, 0xFFCCCCCC, false);
			graphics.drawString(font, rText, x + 6, y + 68, 0xFFCCCCCC, false);
		}

		int barY = y + h - 6;
		int barW = 36;
		int barLeft = x + (w - barW) / 2;
		graphics.fill(barLeft, barY, barLeft + barW, barY + 2, 0x44555555);
		graphics.fill(cx, barY - 1, cx + 1, barY + 3, 0x88AAAAAA);
		float rudderDeflection = Mth.clamp(plane.getRudder(), -1.0F, 1.0F);
		int markerX = cx + Math.round(rudderDeflection * (barW / 2.0F - 2.0F));
		graphics.fill(markerX - 1, barY - 2, markerX + 2, barY + 4, 0xFF55FF55);
	}
}
