package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

public final class VillagerPilotModel extends VillagerModel {
	private final ModelPart arms;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public VillagerPilotModel(ModelPart root) {
		super(root);
		this.arms = root.getChild("arms");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	@Override
	public void setupAnim(VillagerRenderState state) {
		super.setupAnim(state);
		if (state instanceof VillagerPilotRenderState pilotState && pilotState.pilotingPlane) {
			// Stable cockpit pose: knees forward and the joined villager arms
			// lowered toward the controls instead of a standing/walking silhouette.
			this.rightLeg.xRot = -1.32F;
			this.leftLeg.xRot = -1.32F;
			this.rightLeg.yRot = 0.10F;
			this.leftLeg.yRot = -0.10F;
			this.arms.xRot = -1.08F;
		}
	}
}
