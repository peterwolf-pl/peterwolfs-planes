package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class ParagliderModel extends EntityModel<AvatarRenderState> {
	private final ModelPart backpack;
	private final ModelPart canopy;

	public ParagliderModel(ModelPart root) {
		super(root);
		this.backpack = root.getChild("backpack");
		this.canopy = root.getChild("canopy");
	}

	public ModelPart getBackpack() {
		return this.backpack;
	}

	public ModelPart getCanopy() {
		return this.canopy;
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// Backpack Pouch & Front Harness Straps / Shoulder Pads on player's torso
		rootPart.addOrReplaceChild("backpack",
			CubeListBuilder.create()
				// Main backpack pouch on player's back
				.texOffs(0, 0)
				.addBox(-4.0F, 1.0F, 2.0F, 8.0F, 10.0F, 4.0F)
				// Left front harness strap & shoulder pad
				.texOffs(32, 0)
				.addBox(-3.9F, 0.1F, -2.2F, 2.2F, 10.5F, 4.3F)
				// Right front harness strap & shoulder pad
				.texOffs(48, 0)
				.addBox(1.7F, 0.1F, -2.2F, 2.2F, 10.5F, 4.3F)
				// Front chest sternum cross-strap & golden buckle
				.texOffs(32, 16)
				.addBox(-2.5F, 4.5F, -2.4F, 5.0F, 1.5F, 0.6F),
			PartPose.ZERO
		);

		// Canopy overhead - 4 blocks (64 units) wide continuous curved arch
		PartDefinition canopyPart = rootPart.addOrReplaceChild("canopy",
			CubeListBuilder.create(),
			PartPose.ZERO
		);

		// Wing Sections (Continuous aerofoil arch)
		// Center section
		canopyPart.addOrReplaceChild("wing_center",
			CubeListBuilder.create()
				.texOffs(0, 16)
				.addBox(-10.0F, -42.0F, -8.0F, 20.0F, 3.0F, 16.0F),
			PartPose.ZERO
		);
		// Mid-left section (curved slightly down)
		canopyPart.addOrReplaceChild("wing_mid_left",
			CubeListBuilder.create()
				.texOffs(64, 16)
				.addBox(-12.0F, -1.5F, -8.0F, 12.0F, 3.0F, 16.0F),
			PartPose.offsetAndRotation(-10.0F, -40.5F, 0.0F, 0.0F, 0.0F, -0.087F)
		);
		// Far-left wingtip
		canopyPart.addOrReplaceChild("wing_far_left",
			CubeListBuilder.create()
				.texOffs(120, 16)
				.addBox(-10.0F, -1.5F, -8.0F, 10.0F, 3.0F, 16.0F),
			PartPose.offsetAndRotation(-22.0F, -39.5F, 0.0F, 0.0F, 0.0F, -0.209F)
		);
		// Mid-right section
		canopyPart.addOrReplaceChild("wing_mid_right",
			CubeListBuilder.create()
				.texOffs(64, 48)
				.addBox(0.0F, -1.5F, -8.0F, 12.0F, 3.0F, 16.0F),
			PartPose.offsetAndRotation(10.0F, -40.5F, 0.0F, 0.0F, 0.0F, 0.087F)
		);
		// Far-right wingtip
		canopyPart.addOrReplaceChild("wing_far_right",
			CubeListBuilder.create()
				.texOffs(120, 48)
				.addBox(0.0F, -1.5F, -8.0F, 10.0F, 3.0F, 16.0F),
			PartPose.offsetAndRotation(22.0F, -39.5F, 0.0F, 0.0F, 0.0F, 0.209F)
		);

		// 4 Suspension lines anchored at Backpack (0, 2, 4) and extending up to Canopy!
		// Line 1: Far Left (connects to x = -27)
		canopyPart.addOrReplaceChild("line_far_left",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-0.5F, -50.1F, -0.5F, 1.0F, 50.1F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, 0.571F)
		);
		// Line 2: Mid Left (connects to x = -11)
		canopyPart.addOrReplaceChild("line_mid_left",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-0.5F, -43.6F, -0.5F, 1.0F, 43.6F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, 0.256F)
		);
		// Line 3: Mid Right (connects to x = +11)
		canopyPart.addOrReplaceChild("line_mid_right",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-0.5F, -43.6F, -0.5F, 1.0F, 43.6F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, -0.256F)
		);
		// Line 4: Far Right (connects to x = +27)
		canopyPart.addOrReplaceChild("line_far_right",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-0.5F, -50.1F, -0.5F, 1.0F, 50.1F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, -0.571F)
		);

		return LayerDefinition.create(mesh, 256, 128);
	}
}
