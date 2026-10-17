package com.piotrek.peterwolfsplanes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.entity.VillagerPilotEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

public class VillagerPilotRenderer extends MobRenderer<VillagerPilotEntity, VillagerPilotRenderState, VillagerPilotModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "textures/entity/villager_pilot.png");

	public VillagerPilotRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerPilotModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(VillagerPilotRenderState state) {
		return TEXTURE;
	}

	@Override
	public VillagerPilotRenderState createRenderState() {
		return new VillagerPilotRenderState();
	}

	@Override
	public void extractRenderState(VillagerPilotEntity entity, VillagerPilotRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.isUnhappy = false;
		state.pilotingPlane = false;
		state.planeYaw = 0.0F;
		state.planePitch = 0.0F;
		state.planeRoll = 0.0F;
		if (entity.getVehicle() instanceof PlaneEntity plane) {
			state.pilotingPlane = true;
			state.planeYaw = plane.getRenderYaw(partialTick);
			state.planePitch = plane.getVisualPitch(partialTick);
			state.planeRoll = plane.getRoll(partialTick);
			state.bodyRot = state.planeYaw;
			state.yRot = 0.0F;
			state.xRot = 0.0F;
		}
		state.villagerData = new VillagerData(
			net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
			net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
			1
		);
	}

	@Override
	protected void setupRotations(VillagerPilotRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, state.pilotingPlane ? state.planeYaw : bodyRot, entityScale);
		if (state.pilotingPlane) {
			// LivingEntityRenderer applies its coordinate flip after this hook.
			// These signs make the pilot inherit the same pitch/bank matrix as
			// the aircraft model while rotating around the cockpit seat.
			poseStack.rotateDegrees(Axis.XP, -state.planePitch);
			poseStack.rotateDegrees(Axis.ZP, -state.planeRoll);
		}
	}
}
