package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class LargeTwinEnginePlaneModel extends PlaneModel {
	private final ModelPart leftPropeller;
	private final ModelPart rightPropeller;

	public LargeTwinEnginePlaneModel(ModelPart root) {
		super(root);
		ModelPart bodyPart = root.getChild("body");
		this.leftPropeller = bodyPart.getChild("left_propeller");
		this.rightPropeller = bodyPart.getChild("right_propeller");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// Main body (fuselage) - Front Engine and Open Cockpit
		// Z dimensions and offsets scaled by 1.15 (rounded to nice integers/halves)
		PartDefinition body = rootPart.addOrReplaceChild("body",
			CubeListBuilder.create()
				// Front Engine Box
				.texOffs(0, 0)
				.addBox(-6.0F, -6.0F, -16.0F, 12.0F, 12.0F, 14.0F)
				// Cockpit Bottom Board
				.texOffs(100, 0)
				.addBox(-6.0F, 5.0F, -2.0F, 12.0F, 1.0F, 21.0F)
				// Cockpit Left Wall
				.texOffs(0, 36)
				.addBox(-6.0F, -3.0F, -2.0F, 1.0F, 8.0F, 21.0F)
				// Cockpit Right Wall
				.texOffs(50, 36)
				.addBox(5.0F, -3.0F, -2.0F, 1.0F, 8.0F, 21.0F)
				// Cockpit Back Wall
				.texOffs(180, 0)
				.addBox(-5.0F, -3.0F, 18.0F, 10.0F, 8.0F, 1.0F),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// Lower wing - 30% wider wingspan, original depth (Z) of 12.0F to match texture coordinates
		body.addOrReplaceChild("lower_wing",
			CubeListBuilder.create()
				.texOffs(0, 68)
				.addBox(-47.0F, 6.0F, -6.0F, 94.0F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Upper wing - 30% wider wingspan, original depth (Z) of 12.0F to match texture coordinates
		body.addOrReplaceChild("upper_wing",
			CubeListBuilder.create()
				.texOffs(0, 84)
				.addBox(-54.0F, -20.0F, -6.0F, 108.0F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Two additional engines under the upper wing
		// Left Engine Nacelle (using same texture offsets as the main engine for metallic details)
		body.addOrReplaceChild("left_engine",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-22.0F, -18.0F, -5.0F, 6.0F, 6.0F, 10.0F),
			PartPose.ZERO
		);

		// Right Engine Nacelle
		body.addOrReplaceChild("right_engine",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(16.0F, -18.0F, -5.0F, 6.0F, 6.0F, 10.0F),
			PartPose.ZERO
		);

		// Left Propeller (slightly smaller than main propeller) - moved to new texture offsets
		body.addOrReplaceChild("left_propeller",
			CubeListBuilder.create()
				.texOffs(212, 48)
				.addBox(-1.0F, -8.0F, -0.5F, 2.0F, 16.0F, 1.0F),
			PartPose.offset(-19.0F, -15.0F, -5.5F)
		);

		// Right Propeller - moved to new texture offsets
		body.addOrReplaceChild("right_propeller",
			CubeListBuilder.create()
				.texOffs(212, 48)
				.addBox(-1.0F, -8.0F, -0.5F, 2.0F, 16.0F, 1.0F),
			PartPose.offset(19.0F, -15.0F, -5.5F)
		);

		// Wing supports (left and right) - X scaled by 1.3, Z at original positions, moved to new texture offsets
		body.addOrReplaceChild("left_support_1",
			CubeListBuilder.create()
				.texOffs(224, 48)
				.addBox(-36.4F, -18.0F, -4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("left_support_2",
			CubeListBuilder.create()
				.texOffs(224, 48)
				.addBox(-36.4F, -18.0F, 4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_1",
			CubeListBuilder.create()
				.texOffs(228, 48)
				.addBox(35.1F, -18.0F, -4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_2",
			CubeListBuilder.create()
				.texOffs(228, 48)
				.addBox(35.1F, -18.0F, 4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);

		// Tail boom - original depth of 24.0F to fit texture size
		body.addOrReplaceChild("tail_boom",
			CubeListBuilder.create()
				.texOffs(0, 98)
				.addBox(-3.0F, -3.0F, 12.0F, 6.0F, 6.0F, 24.0F),
			PartPose.ZERO
		);

		// Tail rudder - Z scaled by 1.15
		body.addOrReplaceChild("rudder",
			CubeListBuilder.create()
				.texOffs(100, 36)
				.addBox(-1.5F, -18.0F, 35.0F, 3.0F, 15.0F, 9.0F),
			PartPose.ZERO
		);

		// Tail wings - Z scaled by 1.15
		body.addOrReplaceChild("tail_wings",
			CubeListBuilder.create()
				.texOffs(130, 36)
				.addBox(-12.0F, -3.0F, 33.0F, 24.0F, 2.0F, 9.0F),
			PartPose.ZERO
		);

		// Main Propeller - shifted Z forward to engine front, moved to new texture offsets
		body.addOrReplaceChild("propeller",
			CubeListBuilder.create()
				.texOffs(212, 48)
				.addBox(-1.5F, -12.0F, -0.5F, 3.0F, 24.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, -16.5F)
		);

		// Wheels - Z scaled by 1.15
		body.addOrReplaceChild("left_wheel",
			CubeListBuilder.create()
				.texOffs(154, 94)
				.addBox(-9.0F, 4.0F, -9.0F, 4.0F, 10.0F, 12.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_wheel",
			CubeListBuilder.create()
				.texOffs(182, 94)
				.addBox(5.0F, 4.0F, -9.0F, 4.0F, 10.0F, 12.0F),
			PartPose.ZERO
		);

		// Speedometer Dial & Needle on Dashboard (Z = -2.0F)
		PartDefinition speedDial = body.addOrReplaceChild("speed_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(-2.5F, 0.0F, -2.0F)
		);
		speedDial.addOrReplaceChild("speed_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		// Altimeter Dial & Needle on Dashboard (Z = -2.0F)
		PartDefinition altDial = body.addOrReplaceChild("alt_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(2.5F, 0.0F, -2.0F)
		);
		altDial.addOrReplaceChild("alt_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(PlaneRenderState state) {
		super.setupAnim(state);
		this.leftPropeller.zRot = (float) Math.toRadians(state.propellerAngle);
		this.rightPropeller.zRot = (float) Math.toRadians(state.propellerAngle);
	}
}
