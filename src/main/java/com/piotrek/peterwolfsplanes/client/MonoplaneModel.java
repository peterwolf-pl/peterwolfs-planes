package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class MonoplaneModel extends EntityModel<PlaneRenderState> {
	private final ModelPart mainBody;
	private final ModelPart propeller;
	private final ModelPart speedNeedle;
	private final ModelPart altNeedle;
	private final ModelPart controlStick;
	private final ModelPart lights;
	private final ModelPart leftStrobe;
	private final ModelPart rightStrobe;
	private final ModelPart tailStrobe;
	private final ModelPart beaconLight;

	public MonoplaneModel(ModelPart root) {
		super(root.getChild("body"));
		this.mainBody = root.getChild("body");
		this.propeller = this.mainBody.getChild("propeller");
		this.speedNeedle = this.mainBody.getChild("speed_dial").getChild("speed_needle");
		this.altNeedle = this.mainBody.getChild("alt_dial").getChild("alt_needle");
		this.controlStick = this.mainBody.getChild("control_stick");
		this.lights = root.getChild("lights");
		this.leftStrobe = this.lights.getChild("left_strobe");
		this.rightStrobe = this.lights.getChild("right_strobe");
		this.tailStrobe = this.lights.getChild("tail_strobe");
		this.beaconLight = this.lights.getChild("tail_beacon");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// Main body (fuselage) - Aerodynamic Front Engine and Open Cockpit
		PartDefinition body = rootPart.addOrReplaceChild("body",
			CubeListBuilder.create()
				// Aerodynamic Engine Cowl
				.texOffs(0, 0)
				.addBox(-6.0F, -6.0F, -16.0F, 12.0F, 12.0F, 14.0F)
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

		// Single Main Wing (Monoplane) - Wide mid-wing design
		body.addOrReplaceChild("main_wing",
			CubeListBuilder.create()
				.texOffs(0, 68)
				.addBox(-45.0F, 3.0F, -6.0F, 90.0F, 2.0F, 12.0F),
			PartPose.ZERO
		);

		// Streamlined Tail boom
		body.addOrReplaceChild("tail_boom",
			CubeListBuilder.create()
				.texOffs(0, 98)
				.addBox(-3.0F, -3.0F, 12.0F, 6.0F, 6.0F, 26.0F),
			PartPose.ZERO
		);

		// Tail rudder
		body.addOrReplaceChild("rudder",
			CubeListBuilder.create()
				.texOffs(100, 36)
				.addBox(-1.5F, -18.0F, 32.0F, 3.0F, 15.0F, 8.0F),
			PartPose.ZERO
		);

		// Tail wings (Horizontal Stabilizer)
		body.addOrReplaceChild("tail_wings",
			CubeListBuilder.create()
				.texOffs(130, 36)
				.addBox(-14.0F, -3.0F, 30.0F, 28.0F, 2.0F, 8.0F),
			PartPose.ZERO
		);

		// Propeller - High Speed 3-Blade Design
		body.addOrReplaceChild("propeller",
			CubeListBuilder.create()
				.texOffs(146, 68)
				.addBox(-1.5F, -14.0F, -0.5F, 3.0F, 28.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, -16.5F)
		);

		// Wheels
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

		// Speedometer Dial & Needle on Dashboard
		PartDefinition speedDial = body.addOrReplaceChild("speed_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(-2.5F, 0.0F, -1.98F)
		);
		speedDial.addOrReplaceChild("speed_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		// Altimeter Dial & Needle on Dashboard
		PartDefinition altDial = body.addOrReplaceChild("alt_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(2.5F, 0.0F, -1.98F)
		);
		altDial.addOrReplaceChild("alt_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		PlaneModel.addControlStick(body);

		// --- Dedicated Emissive Aircraft Warning & Navigation Lights ---
		PartDefinition lights = rootPart.addOrReplaceChild("lights",
			CubeListBuilder.create(),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// Left Wingtip Nav Light (Port - Steady Aviation Red)
		lights.addOrReplaceChild("left_nav_light",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-46.0F, 3.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Left Strobe (Port - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("left_strobe",
			CubeListBuilder.create()
				.texOffs(16, 0)
				.addBox(-46.0F, 3.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Right Wingtip Nav Light (Starboard - Steady Aviation Green)
		lights.addOrReplaceChild("right_nav_light",
			CubeListBuilder.create()
				.texOffs(32, 0)
				.addBox(45.0F, 3.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Right Strobe (Starboard - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("right_strobe",
			CubeListBuilder.create()
				.texOffs(48, 0)
				.addBox(45.0F, 3.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Tail Aft Nav Light (Aft - Steady Aviation White)
		lights.addOrReplaceChild("tail_nav_light",
			CubeListBuilder.create()
				.texOffs(64, 0)
				.addBox(-0.5F, -4.0F, 40.0F, 1.0F, 1.4F, 1.0F),
			PartPose.ZERO
		);

		// Tail Strobe (Aft - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("tail_strobe",
			CubeListBuilder.create()
				.texOffs(80, 0)
				.addBox(-0.5F, -5.8F, 40.0F, 1.0F, 1.2F, 1.0F),
			PartPose.ZERO
		);

		// Tail Beacon Light (Rudder Fin Top - Flashing Aviation Red)
		lights.addOrReplaceChild("tail_beacon",
			CubeListBuilder.create()
				.texOffs(96, 0)
				.addBox(-0.75F, -19.6F, 35.5F, 1.5F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(PlaneRenderState state) {
		super.setupAnim(state);
		this.propeller.zRot = (float) Math.toRadians(state.propellerAngle);
		this.mainBody.xRot = (float) Math.toRadians(state.pitch);
		this.mainBody.zRot = (float) Math.toRadians(-state.roll);
		this.lights.visible = state.lightsOn;
		this.lights.xRot = this.mainBody.xRot;
		this.lights.zRot = this.mainBody.zRot;
		this.speedNeedle.zRot = (float) Math.toRadians(state.speed * 9.0f);
		this.altNeedle.zRot = (float) Math.toRadians(state.altitude * 3.6f);
		PlaneModel.animateControlStick(this.controlStick, state);

		boolean strobeActive = state.lightsOn && PlaneModel.isStrobeFlash(state.ageInTicks);
		boolean beaconActive = state.lightsOn && PlaneModel.isBeaconFlash(state.ageInTicks);
		this.leftStrobe.visible = strobeActive;
		this.rightStrobe.visible = strobeActive;
		this.tailStrobe.visible = strobeActive;
		this.beaconLight.visible = beaconActive;
	}

	public ModelPart getLights() {
		return this.lights;
	}
}
