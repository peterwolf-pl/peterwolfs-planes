package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

public class ParagliderFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "textures/entity/paraglider.png");
	private final ParagliderModel paragliderModel;

	public ParagliderFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ParagliderModel model) {
		super(parent);
		this.paragliderModel = model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
		boolean isDeployed = PeterwolfsPlanesClient.isParagliderVisuallyDeployed(state.id);
		boolean hasBackpackEquipped = (state.chestEquipment != null && !state.chestEquipment.isEmpty() && state.chestEquipment.is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK));

		if (!hasBackpackEquipped && net.minecraft.client.Minecraft.getInstance().level != null) {
			net.minecraft.world.entity.Entity entity = net.minecraft.client.Minecraft.getInstance().level.getEntity(state.id);
			if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
				hasBackpackEquipped = living.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK);
			}
		}

		if (hasBackpackEquipped || isDeployed) {
			PlayerModel playerModel = this.getParentModel();

			poseStack.pushPose();
			// The player pose is applied by PlayerModelMixin before both the base
			// player model and this attached paraglider layer are submitted.
			playerModel.body.translateAndRotate(poseStack);

			net.minecraft.client.renderer.rendertype.RenderType renderType = RenderTypes.entityCutout(TEXTURE);

			// Render backpack attached to player's torso
			collector.submitModelPart(this.paragliderModel.getBackpack(), poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, null);

			// Render paraglider wing canopy and suspension lines when deployed
			if (isDeployed) {
				collector.submitModelPart(this.paragliderModel.getCanopy(), poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, null);
			}

			poseStack.popPose();
		}
	}
}
