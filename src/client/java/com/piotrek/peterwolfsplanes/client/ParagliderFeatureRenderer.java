package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;

public class ParagliderFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "textures/entity/paraglider.png");
	/** Dark graphite/red tint for brake lines (ARGB). */
	private static final int BRAKE_LINE_TINT = 0xFF3A1818;
	/** First-person: keep lines present but very restrained so they do not block view. */
	private static final int BRAKE_LINE_TINT_FIRST_PERSON = 0x55301010;

	private final ParagliderModel paragliderModel;
	private final Vector3f scratchA = new Vector3f();
	private final Vector3f scratchB = new Vector3f();

	public ParagliderFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ParagliderModel model) {
		super(parent);
		this.paragliderModel = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
		boolean isDeployed = PeterwolfsPlanesClient.isParagliderVisuallyDeployed(state.id);
		boolean hasBackpackEquipped = (state.chestEquipment != null && !state.chestEquipment.isEmpty() && state.chestEquipment.is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK));

		if (!hasBackpackEquipped && Minecraft.getInstance().level != null) {
			net.minecraft.world.entity.Entity entity = Minecraft.getInstance().level.getEntity(state.id);
			if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
				hasBackpackEquipped = living.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK);
			}
		}

		if (hasBackpackEquipped || isDeployed) {
			PlayerModel playerModel = this.getParentModel();
			Minecraft client = Minecraft.getInstance();
			float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

			float leftBrake = PeterwolfsPlanesClient.getParagliderLeftBrakeForEntity(state.id, partialTick);
			float rightBrake = PeterwolfsPlanesClient.getParagliderRightBrakeForEntity(state.id, partialTick);

			boolean firstPersonSelf = client.player != null
				&& client.player.getId() == state.id
				&& client.options.getCameraType().isFirstPerson();

			poseStack.pushPose();
			// The player pose is applied by PlayerModelMixin before both the base
			// player model and this attached paraglider layer are submitted.
			playerModel.body.translateAndRotate(poseStack);

			RenderType renderType = RenderTypes.entityCutout(TEXTURE);

			// Render backpack attached to player's torso
			collector.submitModelPart(this.paragliderModel.getBackpack(), poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, null);

			// Render paraglider wing canopy and suspension lines when deployed
			if (isDeployed) {
				this.paragliderModel.applyWingDeformation(leftBrake, rightBrake);
				collector.submitModelPart(this.paragliderModel.getCanopy(), poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, null);

				// Hand positions from the already-posed arms, converted into body space
				// so lower brake endpoints track the animated hands without clipping the torso.
				// Local (1, 10, 0) ≈ palm center at the end of a standard arm cube.
				ParagliderModel.transformArmPoint(playerModel.leftArm, 1.0f, 10.0f, 0.0f, this.scratchA);
				ParagliderModel.rootToBody(playerModel.body, this.scratchA, this.scratchB);
				float handLX = this.scratchB.x;
				float handLY = this.scratchB.y;
				float handLZ = this.scratchB.z;

				ParagliderModel.transformArmPoint(playerModel.rightArm, -1.0f, 10.0f, 0.0f, this.scratchA);
				ParagliderModel.rootToBody(playerModel.body, this.scratchA, this.scratchB);
				float handRX = this.scratchB.x;
				float handRY = this.scratchB.y;
				float handRZ = this.scratchB.z;

				// Nudge slightly forward of the palm so lines clear the arms/harness.
				handLZ -= 1.2f;
				handRZ -= 1.2f;

				this.paragliderModel.updateBrakeLines(
					handLX, handLY, handLZ,
					handRX, handRY, handRZ,
					leftBrake, rightBrake,
					true
				);

				// First-person: thinner + translucent so lines stay readable without
				// blocking the pilot's view; third-person uses a solid dark tint.
				RenderType brakeRenderType = firstPersonSelf
					? RenderTypes.entityTranslucent(TEXTURE)
					: renderType;
				int brakeTint = firstPersonSelf ? BRAKE_LINE_TINT_FIRST_PERSON : BRAKE_LINE_TINT;
				float thicknessMul = firstPersonSelf ? 0.55f : 1.0f;
				for (var line : this.paragliderModel.getLeftBrakeLines()) {
					if (line.visible) {
						line.xScale *= thicknessMul;
						line.zScale *= thicknessMul;
						collector.submitModelPart(
							line, poseStack, brakeRenderType, packedLight, OverlayTexture.NO_OVERLAY,
							null, brakeTint, null, 0
						);
					}
				}
				for (var line : this.paragliderModel.getRightBrakeLines()) {
					if (line.visible) {
						line.xScale *= thicknessMul;
						line.zScale *= thicknessMul;
						collector.submitModelPart(
							line, poseStack, brakeRenderType, packedLight, OverlayTexture.NO_OVERLAY,
							null, brakeTint, null, 0
						);
					}
				}
			}

			poseStack.popPose();
		}
	}
}
