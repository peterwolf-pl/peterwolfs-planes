package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piotrek.peterwolfsplanes.entity.WaterPlaneEntity;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class WaterPlaneRenderer extends EntityRenderer<WaterPlaneEntity, PlaneRenderState> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "textures/entity/water_plane.png");
	private static final float WATER_RENDER_Y_OFFSET = -1.5F;
	private static final float LAND_RENDER_Y_OFFSET = -27.0F / 16.0F;
	private final WaterPlaneModel model;

	public WaterPlaneRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 1.0F; // Larger shadow
		this.model = new WaterPlaneModel(context.bakeLayer(PeterwolfsPlanesClient.WATER_PLANE_MODEL_LAYER));
	}

	@Override
	public PlaneRenderState createRenderState() {
		return new PlaneRenderState();
	}

	@Override
	public void extractRenderState(WaterPlaneEntity entity, PlaneRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.pitch = entity.getVisualPitch(partialTick);
		state.yRot = entity.getRenderYaw(partialTick);
		state.roll = entity.getRoll(partialTick);
		state.propellerAngle = entity.getPropellerAngle(partialTick);
		state.speed = (float) entity.getInstrumentSpeedMetersPerSecond();
		state.altitude = (float) state.y;
		state.restingOnSolidGround = entity.onGround() && !entity.isRestingOnWater();
	}

	@Override
	public void submit(PlaneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
		super.submit(state, poseStack, collector, cameraState);
		poseStack.pushPose();
		poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		// The float bottoms are at model Y=27/16. Use that exact baseline on
		// solid ground, but preserve the existing water immersion unchanged.
		float renderYOffset = state.restingOnSolidGround ? LAND_RENDER_Y_OFFSET : WATER_RENDER_Y_OFFSET;
		poseStack.translate(0.0F, renderYOffset, 0.0F);
		
		this.model.setupAnim(state);
		collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		
		poseStack.popPose();
	}
}
