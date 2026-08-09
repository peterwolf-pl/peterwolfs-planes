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

	/** Arms raised to the brake handles in the neutral harness pose. */
	private static final float NEUTRAL_ARM_X_DEG = -160.0f;
	/** How far a full brake lowers the hand (degrees of xRot toward horizontal). */
	private static final float BRAKE_LOWER_DEG = 38.0f;
	private static final float NEUTRAL_ARM_Z_DEG = 22.0f;

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

		float leftBrake = PeterwolfsPlanesClient.getParagliderLeftBrakeForEntity(state.id, partialTick);
		float rightBrake = PeterwolfsPlanesClient.getParagliderRightBrakeForEntity(state.id, partialTick);

		// Arms at the brake handles; the active steering hand lowers smoothly
		// and proportionally to the A/D (or spiral) brake input.
		model.leftArm.xRot = (NEUTRAL_ARM_X_DEG + leftBrake * BRAKE_LOWER_DEG) * DEG_TO_RAD;
		model.leftArm.yRot = 0.0f;
		model.leftArm.zRot = (NEUTRAL_ARM_Z_DEG - leftBrake * 6.0f) * DEG_TO_RAD;
		model.rightArm.xRot = (NEUTRAL_ARM_X_DEG + rightBrake * BRAKE_LOWER_DEG) * DEG_TO_RAD;
		model.rightArm.yRot = 0.0f;
		model.rightArm.zRot = (-NEUTRAL_ARM_Z_DEG + rightBrake * 6.0f) * DEG_TO_RAD;

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
