package com.piotrek.peterwolfsplanes.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
		method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
		at = @At("TAIL")
	)
	private void peterwolfsPlanes$bankParagliderPilot(
		AvatarRenderState state,
		PoseStack poseStack,
		float bodyRot,
		float entityScale,
		CallbackInfo ci
	) {
		if (!PeterwolfsPlanesClient.isParagliderVisuallyDeployed(state.id)) {
			return;
		}

		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float rollDegrees = PeterwolfsPlanesClient.getParagliderRollForEntity(state.id, partialTick);
		if (Math.abs(rollDegrees) > 0.01f) {
			poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
		}
	}
}
