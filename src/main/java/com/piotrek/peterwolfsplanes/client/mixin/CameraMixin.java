package com.piotrek.peterwolfsplanes.client.mixin;

import com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
	private static final float COCKPIT_LOOK_SCALE = 0.10F;

	@ModifyArgs(
		method = "alignWithEntity(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;setRotation(FF)V"
		)
	)
	private void peterwolfsPlanes$dampFirstPersonCockpitLook(Args args) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.getCameraEntity() != client.player
			|| !client.options.getCameraType().isFirstPerson()
			|| !(client.player.getVehicle() instanceof PlaneEntity plane)) {
			return;
		}

		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float planeYaw = plane.getRenderYaw(partialTick);
		float planePitch = plane.getVisualPitch(partialTick);
		float lookYaw = args.get(0);
		float lookPitch = args.get(1);
		args.set(0, planeYaw + Mth.wrapDegrees(lookYaw - planeYaw) * COCKPIT_LOOK_SCALE);
		args.set(1, planePitch + (lookPitch - planePitch) * COCKPIT_LOOK_SCALE);
	}

	@ModifyArg(
		method = "setRotation(FF)V",
		at = @At(
			value = "INVOKE",
			target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
			remap = false
		),
		index = 2
	)
	private float peterwolfsPlanes$applyParagliderRoll(float vanillaRoll) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null
			|| client.getCameraEntity() != client.player
			|| !client.options.getCameraType().isFirstPerson()
			|| !PeterwolfsPlanesClient.isParagliderVisuallyDeployed(client.player.getId())) {
			return vanillaRoll;
		}

		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float rollDegrees = PeterwolfsPlanesClient.getParagliderRollForEntity(client.player.getId(), partialTick);
		return vanillaRoll + rollDegrees * Mth.DEG_TO_RAD;
	}
}
