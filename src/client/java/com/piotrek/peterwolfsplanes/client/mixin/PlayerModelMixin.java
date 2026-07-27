package com.piotrek.peterwolfsplanes.client.mixin;

import com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

	@Inject(
		method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
		at = @At("TAIL")
	)
	private void peterwolfsPlanes$applyParagliderPose(AvatarRenderState state, CallbackInfo ci) {
		if (!PeterwolfsPlanesClient.isParagliderVisuallyDeployed(state.id)) {
			return;
		}

		PlayerModel model = (PlayerModel) (Object) this;

		// Reset any walk, idle-bob, item-use, and attack offsets before applying
		// the fixed airborne harness pose.
		model.body.resetPose();
		model.leftArm.resetPose();
		model.rightArm.resetPose();
		model.leftLeg.resetPose();
		model.rightLeg.resetPose();

		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float pitchRadians = PeterwolfsPlanesClient.getParagliderPitchForEntity(state.id, partialTick) * DEG_TO_RAD;
		model.body.xRot = pitchRadians;
		model.head.xRot += pitchRadians;

		// Arms raised above the shoulders and spread into a Y shape.
		model.leftArm.xRot = -160.0f * DEG_TO_RAD;
		model.leftArm.yRot = 0.0f;
		model.leftArm.zRot = 22.0f * DEG_TO_RAD;
		model.rightArm.xRot = -160.0f * DEG_TO_RAD;
		model.rightArm.yRot = 0.0f;
		model.rightArm.zRot = -22.0f * DEG_TO_RAD;

		// Vanilla's seated passenger angles, used here without making the
		// player a passenger: legs are pulled forward and slightly apart.
		model.leftLeg.xRot = -80.0f * DEG_TO_RAD;
		model.leftLeg.yRot = -18.0f * DEG_TO_RAD;
		model.leftLeg.zRot = -4.5f * DEG_TO_RAD;
		model.rightLeg.xRot = -80.0f * DEG_TO_RAD;
		model.rightLeg.yRot = 18.0f * DEG_TO_RAD;
		model.rightLeg.zRot = 4.5f * DEG_TO_RAD;
	}
}
