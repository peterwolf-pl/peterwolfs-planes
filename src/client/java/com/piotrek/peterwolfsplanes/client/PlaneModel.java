package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class PlaneModel extends EntityModel<PlaneRenderState> {
	private final ModelPart mainBody;
	private final ModelPart propeller;
	private final ModelPart speedNeedle;
	private final ModelPart altNeedle;

	public PlaneModel(ModelPart root) {
		super(root);
		this.mainBody = root.getChild("body");
		this.propeller = this.mainBody.getChild("propeller");
		this.speedNeedle = this.mainBody.getChild("speed_dial").getChild("speed_needle");
		this.altNeedle = this.mainBody.getChild("alt_dial").getChild("alt_needle");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// Main body (fuselage) - Front Engine and Open Cockpit
		PartDefinition body = rootPart.addOrReplaceChild("body",
			CubeListBuilder.create()
				// Front Engine Box
				.texOffs(0, 0)
				.addBox(-6.0F, -6.0F, -14.0F, 12.0F, 12.0F, 12.0F)
				// Cockpit Bottom Board
				.texOffs(100, 0)
				.addBox(-6.0F, 5.0F, -2.0F, 12.0F, 1.0F, 18.0F)
				// Cockpit Left Wall
				.texOffs(0, 36)
				.addBox(-6.0F, -3.0F, -2.0F, 1.0F, 8.0F, 18.0F)
				// Cockpit Right Wall
				.texOffs(50, 36)
				.addBox(5.0F, -3.0F, -2.0F, 1.0F, 8.0F, 18.0F)
				// Cockpit Back Wall
				.texOffs(180, 0)
				.addBox(-5.0F, -3.0F, 15.0F, 10.0F, 8.0F, 1.0F),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// Lower wing
		body.addOrReplaceChild("lower_wing",
			CubeListBuilder.create()
				.texOffs(0, 68)
				.addBox(-36.0F, 6.0F, -6.0F, 72.0F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Upper wing
		body.addOrReplaceChild("upper_wing",
			CubeListBuilder.create()
				.texOffs(0, 84)
				.addBox(-41.4F, -20.0F, -6.0F, 82.8F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Wing supports (left and right)
		body.addOrReplaceChild("left_support_1",
			CubeListBuilder.create()
				.texOffs(190, 68)
				.addBox(-28.0F, -18.0F, -4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("left_support_2",
			CubeListBuilder.create()
				.texOffs(190, 68)
				.addBox(-28.0F, -18.0F, 4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_1",
			CubeListBuilder.create()
				.texOffs(194, 68)
				.addBox(27.0F, -18.0F, -4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_2",
			CubeListBuilder.create()
				.texOffs(194, 68)
				.addBox(27.0F, -18.0F, 4.0F, 1.0F, 24.0F, 1.0F),
			PartPose.ZERO
		);

		// Tail boom
		body.addOrReplaceChild("tail_boom",
			CubeListBuilder.create()
				.texOffs(0, 98)
				.addBox(-3.0F, -3.0F, 12.0F, 6.0F, 6.0F, 24.0F),
			PartPose.ZERO
		);

		// Tail rudder
		body.addOrReplaceChild("rudder",
			CubeListBuilder.create()
				.texOffs(100, 36)
				.addBox(-1.5F, -18.0F, 30.0F, 3.0F, 15.0F, 8.0F),
			PartPose.ZERO
		);

		// Tail wings
		body.addOrReplaceChild("tail_wings",
			CubeListBuilder.create()
				.texOffs(130, 36)
				.addBox(-12.0F, -3.0F, 28.0F, 24.0F, 2.0F, 8.0F),
			PartPose.ZERO
		);

		// Propeller
		body.addOrReplaceChild("propeller",
			CubeListBuilder.create()
				.texOffs(146, 68)
				.addBox(-1.5F, -12.0F, -0.5F, 3.0F, 24.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, -14.5F)
		);

		// Wheels - larger 4x10x10 for better ground presence
		body.addOrReplaceChild("left_wheel",
			CubeListBuilder.create()
				.texOffs(154, 94)
				.addBox(-9.0F, 4.0F, -8.0F, 4.0F, 10.0F, 10.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_wheel",
			CubeListBuilder.create()
				.texOffs(182, 94)
				.addBox(5.0F, 4.0F, -8.0F, 4.0F, 10.0F, 10.0F),
			PartPose.ZERO
		);

		// Speedometer Dial & Needle on Dashboard (Z = -2.0F on Engine Box)
		PartDefinition speedDial = body.addOrReplaceChild("speed_dial",
			CubeListBuilder.create()
				.texOffs(98, 2) // METAL_LIGHT color
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(-2.5F, 0.0F, -1.98F)
		);
		speedDial.addOrReplaceChild("speed_needle",
			CubeListBuilder.create()
				.texOffs(56, 20) // PROP_RED color
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		// Altimeter Dial & Needle on Dashboard
		PartDefinition altDial = body.addOrReplaceChild("alt_dial",
			CubeListBuilder.create()
				.texOffs(98, 2) // METAL_LIGHT color
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(2.5F, 0.0F, -1.98F)
		);
		altDial.addOrReplaceChild("alt_needle",
			CubeListBuilder.create()
				.texOffs(56, 20) // PROP_RED color
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(PlaneRenderState state) {
		super.setupAnim(state);
		this.propeller.zRot = (float) Math.toRadians(state.propellerAngle);
		this.mainBody.xRot = (float) Math.toRadians(state.pitch);
		this.mainBody.zRot = (float) Math.toRadians(-state.roll);
		this.speedNeedle.zRot = (float) Math.toRadians(state.speed * 9.0f);
		this.altNeedle.zRot = (float) Math.toRadians(state.altitude * 3.6f);
	}
}
