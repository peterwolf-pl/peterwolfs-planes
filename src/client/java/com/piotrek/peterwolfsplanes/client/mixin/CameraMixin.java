package com.piotrek.peterwolfsplanes.client.mixin;

import com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
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
