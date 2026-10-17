package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class WaterPlaneModel extends PlaneModel {
	public WaterPlaneModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// Main body (fuselage) - Front Engine and Open Cockpit
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

		// Single high-mounted wing split around a full-chord cockpit cutout. The
		// roots remain carried by the four cabane supports while the opening above
		// the pilot clears the complete first-person line of sight.
		body.addOrReplaceChild("upper_wing",
			CubeListBuilder.create()
				.texOffs(0, 84)
				.addBox(-54.0F, -10.0F, -6.0F, 47.0F, 2.0F, 12.0F)
				.texOffs(0, 84)
				.addBox(7.0F, -10.0F, -6.0F, 47.0F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Short cabane supports between the fuselage and the high wing.
		body.addOrReplaceChild("left_support_1",
			CubeListBuilder.create()
				.texOffs(224, 48)
				.addBox(-6.5F, -8.0F, -4.0F, 1.5F, 6.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("left_support_2",
			CubeListBuilder.create()
				.texOffs(224, 48)
				.addBox(-6.5F, -8.0F, 3.0F, 1.5F, 6.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_1",
			CubeListBuilder.create()
				.texOffs(228, 48)
				.addBox(5.0F, -8.0F, -4.0F, 1.5F, 6.0F, 1.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_support_2",
			CubeListBuilder.create()
				.texOffs(228, 48)
				.addBox(5.0F, -8.0F, 3.0F, 1.5F, 6.0F, 1.0F),
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
				.addBox(-1.5F, -18.0F, 35.0F, 3.0F, 15.0F, 9.0F),
			PartPose.ZERO
		);

		// Tail wings
		body.addOrReplaceChild("tail_wings",
			CubeListBuilder.create()
				.texOffs(130, 36)
				.addBox(-12.0F, -3.0F, 33.0F, 24.0F, 2.0F, 9.0F),
			PartPose.ZERO
		);

		// Propeller
		body.addOrReplaceChild("propeller",
			CubeListBuilder.create()
				.texOffs(212, 48)
				.addBox(-1.5F, -12.0F, -0.5F, 3.0F, 24.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, -16.5F)
		);

		// Floats & Struts instead of wheels
		body.addOrReplaceChild("left_float",
			CubeListBuilder.create()
				.texOffs(154, 94)
				// Main float body
				.addBox(-9.5F, 9.0F, -18.0F, 5.0F, 4.0F, 36.0F)
				// Front strut
				.addBox(-8.0F, 4.0F, -8.0F, 2.0F, 5.0F, 2.0F)
				// Back strut
				.addBox(-8.0F, 4.0F, 8.0F, 2.0F, 5.0F, 2.0F),
			PartPose.ZERO
		);

		body.addOrReplaceChild("right_float",
			CubeListBuilder.create()
				.texOffs(182, 94)
				// Main float body
				.addBox(4.5F, 9.0F, -18.0F, 5.0F, 4.0F, 36.0F)
				// Front strut
				.addBox(6.0F, 4.0F, -8.0F, 2.0F, 5.0F, 2.0F)
				// Back strut
				.addBox(6.0F, 4.0F, 8.0F, 2.0F, 5.0F, 2.0F),
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

		addControlStick(body);

		// --- Dedicated Emissive Aircraft Warning & Navigation Lights ---
		PartDefinition lights = rootPart.addOrReplaceChild("lights",
			CubeListBuilder.create(),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// Left Wingtip Nav Light (Port - Steady Aviation Red)
		lights.addOrReplaceChild("left_nav_light",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-55.0F, -9.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Left Strobe (Port - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("left_strobe",
			CubeListBuilder.create()
				.texOffs(16, 0)
				.addBox(-55.0F, -9.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Right Wingtip Nav Light (Starboard - Steady Aviation Green)
		lights.addOrReplaceChild("right_nav_light",
			CubeListBuilder.create()
				.texOffs(32, 0)
				.addBox(54.0F, -9.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Right Strobe (Starboard - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("right_strobe",
			CubeListBuilder.create()
				.texOffs(48, 0)
				.addBox(54.0F, -9.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Tail Aft Nav Light (Aft - Steady Aviation White)
		lights.addOrReplaceChild("tail_nav_light",
			CubeListBuilder.create()
				.texOffs(64, 0)
				.addBox(-0.5F, -4.0F, 44.0F, 1.0F, 1.4F, 1.0F),
			PartPose.ZERO
		);

		// Tail Strobe (Aft - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("tail_strobe",
			CubeListBuilder.create()
				.texOffs(80, 0)
				.addBox(-0.5F, -5.8F, 44.0F, 1.0F, 1.2F, 1.0F),
			PartPose.ZERO
		);

		// Anti-Collision Flashing Red Beacon (Top of Rudder Fin)
		lights.addOrReplaceChild("tail_beacon",
			CubeListBuilder.create()
				.texOffs(96, 0)
				.addBox(-0.75F, -19.6F, 38.5F, 1.5F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		return LayerDefinition.create(mesh, 256, 128);
	}
}
