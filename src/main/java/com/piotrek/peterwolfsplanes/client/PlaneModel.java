package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class PlaneModel extends EntityModel<PlaneRenderState> {
	private static final float CONTROL_STICK_PITCH_RAD = (float) Math.toRadians(12.0F);
	private static final float CONTROL_STICK_ROLL_RAD = (float) Math.toRadians(10.0F);

	protected final ModelPart mainBody;
	protected final ModelPart propeller;
	protected final ModelPart speedNeedle;
	protected final ModelPart altNeedle;
	protected final ModelPart controlStick;
	protected final ModelPart lights;
	protected final ModelPart leftStrobe;
	protected final ModelPart rightStrobe;
	protected final ModelPart tailStrobe;
	protected final ModelPart beaconLight;

	public PlaneModel(ModelPart root) {
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
				.addBox(-42.4F, -19.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Left Wingtip Strobe (Port - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("left_strobe",
			CubeListBuilder.create()
				.texOffs(16, 0)
				.addBox(-42.4F, -19.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Right Wingtip Nav Light (Starboard - Steady Aviation Green)
		lights.addOrReplaceChild("right_nav_light",
			CubeListBuilder.create()
				.texOffs(32, 0)
				.addBox(41.4F, -19.5F, -5.5F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);

		// Right Wingtip Strobe (Starboard - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("right_strobe",
			CubeListBuilder.create()
				.texOffs(48, 0)
				.addBox(41.4F, -19.5F, -2.0F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Tail Aft Nav Light (Aft - Steady Aviation White)
		lights.addOrReplaceChild("tail_nav_light",
			CubeListBuilder.create()
				.texOffs(64, 0)
				.addBox(-0.5F, -4.0F, 38.0F, 1.0F, 1.4F, 1.0F),
			PartPose.ZERO
		);

		// Tail Strobe (Aft - High-Intensity Flashing White Xenon)
		lights.addOrReplaceChild("tail_strobe",
			CubeListBuilder.create()
				.texOffs(80, 0)
				.addBox(-0.5F, -5.8F, 38.0F, 1.0F, 1.2F, 1.0F),
			PartPose.ZERO
		);

		// Tail Beacon Light (Rudder Fin Top - Flashing Aviation Red)
		lights.addOrReplaceChild("tail_beacon",
			CubeListBuilder.create()
				.texOffs(96, 0)
				.addBox(-0.75F, -19.6F, 33.5F, 1.5F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		return LayerDefinition.create(mesh, 256, 128);
	}

	public static void addControlStick(PartDefinition body) {
		body.addOrReplaceChild("control_stick_base",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.6F, -0.5F, -1.6F, 3.2F, 0.9F, 3.2F),
			PartPose.offset(0.0F, 5.0F, 2.2F)
		);
		body.addOrReplaceChild("control_stick",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.1F, -2.4F, -1.1F, 2.2F, 2.4F, 2.2F)
				.texOffs(0, 0)
				.addBox(-0.45F, -9.6F, -0.45F, 0.9F, 7.2F, 0.9F)
				.texOffs(248, 120)
				.addBox(-0.55F, -11.7F, -0.55F, 1.1F, 2.3F, 1.1F)
				.texOffs(248, 120)
				.addBox(-0.62F, -12.2F, -0.62F, 1.24F, 0.55F, 1.24F),
			PartPose.offset(0.0F, 4.6F, 2.2F)
		);
	}

	public static void animateControlStick(ModelPart stick, PlaneRenderState state) {
		float pitch = Mth.clamp(state.stickPitch, -1.0F, 1.0F);
		float roll = Mth.clamp(state.stickRoll, -1.0F, 1.0F);
		stick.xRot = pitch * CONTROL_STICK_PITCH_RAD;
		stick.zRot = roll * CONTROL_STICK_ROLL_RAD;
	}

	public static boolean isStrobeFlash(float ageInTicks) {
		// Standard aviation 24-tick cycle (1.2s): double-flash burst
		float cycle = ageInTicks % 24.0f;
		if (cycle < 0) cycle += 24.0f;
		return (cycle >= 0.0f && cycle < 1.8f) || (cycle >= 3.6f && cycle < 5.4f);
	}

	public static boolean isBeaconFlash(float ageInTicks) {
		// 20-tick cycle (1.0s): rhythmic warning pulse offset from strobes
		float cycle = ageInTicks % 20.0f;
		if (cycle < 0) cycle += 20.0f;
		return (cycle >= 9.0f && cycle < 13.0f);
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
		animateControlStick(this.controlStick, state);

		boolean strobeActive = state.lightsOn && isStrobeFlash(state.ageInTicks);
		boolean beaconActive = state.lightsOn && isBeaconFlash(state.ageInTicks);
		this.leftStrobe.visible = strobeActive;
		this.rightStrobe.visible = strobeActive;
		this.tailStrobe.visible = strobeActive;
		this.beaconLight.visible = beaconActive;
	}

	public ModelPart getLights() {
		return this.lights;
	}

	public ModelPart getMainBody() {
		return this.mainBody;
	}
}
