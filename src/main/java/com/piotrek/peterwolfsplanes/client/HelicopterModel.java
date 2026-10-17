package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class HelicopterModel extends EntityModel<PlaneRenderState> {
	private final ModelPart mainBody;
	private final ModelPart mainRotor;
	private final ModelPart tailRotor;
	private final ModelPart speedNeedle;
	private final ModelPart altNeedle;
	private final ModelPart controlStick;
	private final ModelPart lights;
	private final ModelPart leftStrobe;
	private final ModelPart rightStrobe;
	private final ModelPart tailStrobe;
	private final ModelPart beaconLight;

	public HelicopterModel(ModelPart root) {
		super(root.getChild("body"));
		this.mainBody = root.getChild("body");
		this.mainRotor = this.mainBody.getChild("main_rotor");
		this.tailRotor = this.mainBody.getChild("tail_rotor");
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

		// Main fuselage / body
		PartDefinition body = rootPart.addOrReplaceChild("body",
			CubeListBuilder.create()
				// === OPEN MINECART-STYLE COCKPIT TUB ===
				// Floor
				.texOffs(100, 0)
				.addBox(-7.0F, 5.0F, -10.0F, 14.0F, 1.0F, 20.0F)
				// Front Wall
				.texOffs(0, 0)
				.addBox(-8.0F, -3.0F, -11.0F, 16.0F, 8.0F, 2.0F)
				// Back Wall
				.texOffs(180, 0)
				.addBox(-8.0F, -3.0F, 9.0F, 16.0F, 8.0F, 2.0F)
				// Left Wall
				.texOffs(0, 36)
				.addBox(-8.0F, -3.0F, -9.0F, 2.0F, 8.0F, 18.0F)
				// Right Wall
				.texOffs(50, 36)
				.addBox(6.0F, -3.0F, -9.0F, 2.0F, 8.0F, 18.0F)

				// === ENGINE DECK (Mounted behind the open cockpit tub) ===
				.texOffs(0, 70)
				.addBox(-6.0F, -4.0F, 11.0F, 12.0F, 9.0F, 14.0F)

				// === HIGH ROTOR MAST (Rises high above player's head) ===
				.texOffs(56, 70)
				.addBox(-1.5F, -22.0F, 16.0F, 3.0F, 18.0F, 3.0F),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// === MAIN ROTOR ASSEMBLY (Large 120-wide 4-blade rotor spinning at Y = -22.5F) ===
		PartDefinition mainRotor = body.addOrReplaceChild("main_rotor",
			CubeListBuilder.create()
				// Rotor Hub
				.texOffs(70, 70)
				.addBox(-3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 6.0F)
				// Primary Blade Pair (X-axis, 120 wide)
				.texOffs(0, 96)
				.addBox(-60.0F, -0.5F, -1.5F, 120.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, -22.5F, 17.5F)
		);
		mainRotor.addOrReplaceChild("cross_blade",
			CubeListBuilder.create()
				.texOffs(0, 96)
				.addBox(-60.0F, -0.5F, -1.5F, 120.0F, 1.0F, 3.0F),
			PartPose.rotation(0.0F, (float) (Math.PI / 2.0), 0.0F)
		);

		// === TAIL BOOM & EMPENNAGE ===
		// Tail Boom extending back
		body.addOrReplaceChild("tail_boom",
			CubeListBuilder.create()
				.texOffs(100, 36)
				.addBox(-2.5F, -3.0F, 25.0F, 5.0F, 5.0F, 32.0F),
			PartPose.ZERO
		);

		// Large Vertical Tail Fin
		body.addOrReplaceChild("vertical_fin",
			CubeListBuilder.create()
				.texOffs(176, 36)
				.addBox(-1.0F, -19.0F, 50.0F, 2.0F, 21.0F, 7.0F),
			PartPose.ZERO
		);

		// Horizontal Tail Fin
		body.addOrReplaceChild("horizontal_fin",
			CubeListBuilder.create()
				.texOffs(198, 36)
				.addBox(-10.0F, -2.0F, 44.0F, 20.0F, 1.0F, 6.0F),
			PartPose.ZERO
		);

		// Tail Rotor Assembly (spinning vertically on fin side)
		body.addOrReplaceChild("tail_rotor",
			CubeListBuilder.create()
				.texOffs(74, 10)
				.addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F)
				.texOffs(82, 10)
				.addBox(-0.25F, -10.0F, -1.0F, 0.5F, 20.0F, 2.0F),
			PartPose.offset(-1.8F, -10.0F, 53.0F)
		);

		// === HEAVY-DUTY LANDING SKIDS ===
		// Left Skid Struts
		body.addOrReplaceChild("left_skid_strut_f",
			CubeListBuilder.create()
				.texOffs(100, 74)
				.addBox(-9.5F, 5.0F, -6.0F, 2.5F, 5.0F, 2.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("left_skid_strut_r",
			CubeListBuilder.create()
				.texOffs(100, 74)
				.addBox(-9.5F, 5.0F, 14.0F, 2.5F, 5.0F, 2.0F),
			PartPose.ZERO
		);
		// Left Skid Tube (long with upturned front)
		body.addOrReplaceChild("left_skid_tube",
			CubeListBuilder.create()
				.texOffs(110, 74)
				.addBox(-10.5F, 10.0F, -16.0F, 2.0F, 2.0F, 42.0F)
				.texOffs(110, 74)
				.addBox(-10.5F, 7.5F, -19.0F, 2.0F, 3.0F, 3.0F),
			PartPose.ZERO
		);

		// Right Skid Struts
		body.addOrReplaceChild("right_skid_strut_f",
			CubeListBuilder.create()
				.texOffs(100, 74)
				.addBox(7.0F, 5.0F, -6.0F, 2.5F, 5.0F, 2.0F),
			PartPose.ZERO
		);
		body.addOrReplaceChild("right_skid_strut_r",
			CubeListBuilder.create()
				.texOffs(100, 74)
				.addBox(7.0F, 5.0F, 14.0F, 2.5F, 5.0F, 2.0F),
			PartPose.ZERO
		);
		// Right Skid Tube
		body.addOrReplaceChild("right_skid_tube",
			CubeListBuilder.create()
				.texOffs(110, 74)
				.addBox(8.5F, 10.0F, -16.0F, 2.0F, 2.0F, 42.0F)
				.texOffs(110, 74)
				.addBox(8.5F, 7.5F, -19.0F, 2.0F, 3.0F, 3.0F),
			PartPose.ZERO
		);

		// === DASHBOARD & DIALS (Mounted on inside of front wall) ===
		PartDefinition speedDial = body.addOrReplaceChild("speed_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(-3.0F, 0.0F, -8.9F)
		);
		speedDial.addOrReplaceChild("speed_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		PartDefinition altDial = body.addOrReplaceChild("alt_dial",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.5F, -1.5F, -0.05F, 3.0F, 3.0F, 0.1F),
			PartPose.offset(3.0F, 0.0F, -8.9F)
		);
		altDial.addOrReplaceChild("alt_needle",
			CubeListBuilder.create()
				.texOffs(56, 20)
				.addBox(-0.25F, -1.2F, -0.1F, 0.5F, 1.2F, 0.1F),
			PartPose.offset(0.0F, 0.0F, -0.1F)
		);

		// === CYCLIC CONTROL STICK (Placed right in front of the player's seat) ===
		body.addOrReplaceChild("control_stick_base",
			CubeListBuilder.create()
				.texOffs(98, 2)
				.addBox(-1.6F, -0.5F, -1.6F, 3.2F, 0.9F, 3.2F),
			PartPose.offset(0.0F, 5.0F, -2.5F)
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
			PartPose.offset(0.0F, 4.6F, -2.5F)
		);

		// === DEDICATED EMISSIVE LIGHTS ===
		PartDefinition lights = rootPart.addOrReplaceChild("lights",
			CubeListBuilder.create(),
			PartPose.offset(0.0F, 14.0F, 0.0F)
		);

		// Left Skid Nav Light & Strobe
		lights.addOrReplaceChild("left_nav_light",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-11.0F, 9.5F, -6.0F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);
		lights.addOrReplaceChild("left_strobe",
			CubeListBuilder.create()
				.texOffs(16, 0)
				.addBox(-11.0F, 9.5F, -2.5F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Right Skid Nav Light & Strobe
		lights.addOrReplaceChild("right_nav_light",
			CubeListBuilder.create()
				.texOffs(32, 0)
				.addBox(10.0F, 9.5F, -6.0F, 1.0F, 1.0F, 3.0F),
			PartPose.ZERO
		);
		lights.addOrReplaceChild("right_strobe",
			CubeListBuilder.create()
				.texOffs(48, 0)
				.addBox(10.0F, 9.5F, -2.5F, 1.0F, 1.0F, 2.0F),
			PartPose.ZERO
		);

		// Tail Strobe
		lights.addOrReplaceChild("tail_strobe",
			CubeListBuilder.create()
				.texOffs(80, 0)
				.addBox(-0.5F, -5.0F, 57.5F, 1.0F, 1.2F, 1.0F),
			PartPose.ZERO
		);

		// Tail Beacon Light
		lights.addOrReplaceChild("tail_beacon",
			CubeListBuilder.create()
				.texOffs(96, 0)
				.addBox(-0.75F, -20.5F, 52.0F, 1.5F, 1.2F, 2.0F),
			PartPose.ZERO
		);

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(PlaneRenderState state) {
		super.setupAnim(state);
		this.mainRotor.yRot = (float) Math.toRadians(state.propellerAngle);
		this.tailRotor.xRot = (float) Math.toRadians(state.propellerAngle * 2.5F);
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
