package com.piotrek.peterwolfsplanes.client;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.VillagerPilotEntity;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

public class VillagerPilotRenderer extends MobRenderer<VillagerPilotEntity, VillagerRenderState, VillagerModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(PeterwolfsPlanesMod.MOD_ID, "textures/entity/villager_pilot.png");

	public VillagerPilotRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	@Override
	public void extractRenderState(VillagerPilotEntity entity, VillagerRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.isUnhappy = false;
		state.villagerData = new VillagerData(
			net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
			net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
			1
		);
	}
}
