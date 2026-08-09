package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParagliderModel extends EntityModel<AvatarRenderState> {
	private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
	/** Base height of each unit brake-line cube (pixel units). */
	private static final float BRAKE_LINE_BASE_LEN = 16.0f;
	/** Max tip rotation from full brake (subtle 8–15° band). */
	private static final float MAX_TIP_BEND_DEG = 12.0f;
	/** Max trailing-edge Z push (~7.5% of 16-unit chord). */
	private static final float MAX_TE_BACK = 1.2f;
	/** Max trailing-edge Y droop in pixel units. */
	private static final float MAX_TE_DOWN = 1.0f;

	private final ModelPart backpack;
	private final ModelPart canopy;
	private final ModelPart wingCenter;
	private final ModelPart wingMidLeft;
	private final ModelPart wingFarLeft;
	private final ModelPart wingMidRight;
	private final ModelPart wingFarRight;

	/** Main riser + 3 trailing-edge branches per side. */
	private final ModelPart[] leftBrakeLines;
	private final ModelPart[] rightBrakeLines;

	public ParagliderModel(ModelPart root) {
		super(root);
		this.backpack = root.getChild("backpack");
		this.canopy = root.getChild("canopy");
		this.wingCenter = this.canopy.getChild("wing_center");
		this.wingMidLeft = this.canopy.getChild("wing_mid_left");
		this.wingFarLeft = this.canopy.getChild("wing_far_left");
		this.wingMidRight = this.canopy.getChild("wing_mid_right");
		this.wingFarRight = this.canopy.getChild("wing_far_right");

		ModelPart brakes = root.getChild("brake_lines");
		this.leftBrakeLines = new ModelPart[] {
			brakes.getChild("left_main"),
			brakes.getChild("left_branch_a"),
			brakes.getChild("left_branch_b"),
			brakes.getChild("left_branch_c")
		};
		this.rightBrakeLines = new ModelPart[] {
			brakes.getChild("right_main"),
			brakes.getChild("right_branch_a"),
			brakes.getChild("right_branch_b"),
			brakes.getChild("right_branch_c")
		};
	}

	public ModelPart getBackpack() {
		return this.backpack;
	}

	public ModelPart getCanopy() {
		return this.canopy;
	}

	public ModelPart[] getLeftBrakeLines() {
		return this.leftBrakeLines;
	}

	public ModelPart[] getRightBrakeLines() {
		return this.rightBrakeLines;
	}

	/**
	 * Applies proportional wing-tip / trailing-edge deformation for steering
	 * visualization only. Does not affect flight physics.
	 */
	public void applyWingDeformation(float leftBrake, float rightBrake) {
		this.wingCenter.resetPose();
		this.wingMidLeft.resetPose();
		this.wingFarLeft.resetPose();
		this.wingMidRight.resetPose();
		this.wingFarRight.resetPose();

		deformSide(this.wingFarLeft, this.wingMidLeft, leftBrake, true);
		deformSide(this.wingFarRight, this.wingMidRight, rightBrake, false);
	}

	private static void deformSide(ModelPart tip, ModelPart mid, float brake, boolean left) {
		if (brake <= 0.001f) {
			return;
		}
		float amount = Mth.clamp(brake, 0.0f, 1.0f);
		// Negative xRot drops the trailing edge (+Z) downward (+Y in model space).
		float tipBend = -amount * MAX_TIP_BEND_DEG * DEG_TO_RAD;
		float midBend = tipBend * 0.4f;
		float sideSign = left ? -1.0f : 1.0f;
		// Extra tip droop without changing overall span.
		float tipExtraZ = sideSign * amount * 3.0f * DEG_TO_RAD;

		tip.xRot += tipBend;
		tip.zRot += tipExtraZ;
		tip.y += amount * MAX_TE_DOWN;
		tip.z += amount * MAX_TE_BACK;

		mid.xRot += midBend;
		mid.zRot += tipExtraZ * 0.35f;
		mid.y += amount * MAX_TE_DOWN * 0.4f;
		mid.z += amount * MAX_TE_BACK * 0.4f;
	}

	/**
	 * Places left/right brake lines from each hand up to the trailing edge near
	 * the wing tip. The active side is drawn tight (straight); the opposite side
	 * gets a very subtle mid-point sag. Lower endpoints follow the animated hands.
	 *
	 * @param bodySpaceHands hands already expressed in body-local pixel units
	 */
	public void updateBrakeLines(
		float handLeftX, float handLeftY, float handLeftZ,
		float handRightX, float handRightY, float handRightZ,
		float leftBrake, float rightBrake,
		boolean visible
	) {
		if (!visible) {
			setBrakeVisible(this.leftBrakeLines, false);
			setBrakeVisible(this.rightBrakeLines, false);
			return;
		}

		// Trailing-edge attachments near each wing tip (pixel units, body space).
		// Slightly track the same tip deformation so lines stay attached.
		float leftTeY = -38.5f + leftBrake * MAX_TE_DOWN;
		float leftTeZ = 7.0f + leftBrake * MAX_TE_BACK;
		float rightTeY = -38.5f + rightBrake * MAX_TE_DOWN;
		float rightTeZ = 7.0f + rightBrake * MAX_TE_BACK;

		float[][] leftTips = {
			{-30.5f, leftTeY, leftTeZ},
			{-27.0f, leftTeY + 0.2f, leftTeZ},
			{-23.5f, leftTeY, leftTeZ - 0.2f}
		};
		float[][] rightTips = {
			{30.5f, rightTeY, rightTeZ},
			{27.0f, rightTeY + 0.2f, rightTeZ},
			{23.5f, rightTeY, rightTeZ - 0.2f}
		};

		placeBrakeSide(
			this.leftBrakeLines,
			handLeftX, handLeftY, handLeftZ,
			leftTips,
			leftBrake,
			// Opposite slack when the other side is pulled harder.
			Math.max(0.0f, rightBrake - leftBrake) * 0.35f
		);
		placeBrakeSide(
			this.rightBrakeLines,
			handRightX, handRightY, handRightZ,
			rightTips,
			rightBrake,
			Math.max(0.0f, leftBrake - rightBrake) * 0.35f
		);
	}

	private static void placeBrakeSide(
		ModelPart[] lines,
		float handX, float handY, float handZ,
		float[][] tips,
		float tension,
		float extraSlack
	) {
		// Split point ~70% of the way from hand to the mean tip.
		float meanTipX = (tips[0][0] + tips[1][0] + tips[2][0]) / 3.0f;
		float meanTipY = (tips[0][1] + tips[1][1] + tips[2][1]) / 3.0f;
		float meanTipZ = (tips[0][2] + tips[1][2] + tips[2][2]) / 3.0f;

		float splitX = Mth.lerp(0.72f, handX, meanTipX);
		float splitY = Mth.lerp(0.72f, handY, meanTipY);
		float splitZ = Mth.lerp(0.72f, handZ, meanTipZ);

		// Subtle curve only when relaxed (no expensive rope sim).
		float slack = (1.0f - Mth.clamp(tension, 0.0f, 1.0f)) * 0.85f + extraSlack;
		if (slack > 0.001f) {
			// Sag slightly outward and a touch back so the idle line is not perfectly rigid.
			float outward = Math.signum(meanTipX == 0.0f ? 1.0f : meanTipX);
			splitX += outward * slack * 1.4f;
			splitY += slack * 1.1f;
			splitZ += slack * 0.6f;
		}

		// Main riser: hand → split (a touch thicker than branches / suspension)
		orientLine(lines[0], handX, handY, handZ, splitX, splitY, splitZ, 1.05f);
		// Three thinner branches: split → trailing edge
		for (int i = 0; i < 3; i++) {
			orientLine(
				lines[i + 1],
				splitX, splitY, splitZ,
				tips[i][0], tips[i][1], tips[i][2],
				0.75f
			);
		}
	}

	/**
	 * Places a unit-length (-Y) cube so it stretches from A to B in pixel units.
	 */
	private static void orientLine(
		ModelPart part,
		float ax, float ay, float az,
		float bx, float by, float bz,
		float thicknessScale
	) {
		float dx = bx - ax;
		float dy = by - ay;
		float dz = bz - az;
		float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 0.05f) {
			part.visible = false;
			return;
		}
		part.visible = true;
		part.x = ax;
		part.y = ay;
		part.z = az;
		part.xScale = thicknessScale;
		part.zScale = thicknessScale;
		part.yScale = len / BRAKE_LINE_BASE_LEN;

		// Align local -Y with (dx,dy,dz). rotationZYX: Z then Y then X.
		// First yaw around Y so the line's XZ matches, then pitch around X.
		// For a default -Y axis: use zRot / xRot pair that maps -Y → direction.
		float invLen = 1.0f / len;
		float dirX = dx * invLen;
		float dirY = dy * invLen;
		float dirZ = dz * invLen;

		// Build a rotation that takes (0, -1, 0) to (dirX, dirY, dirZ).
		Vector3f from = new Vector3f(0.0f, -1.0f, 0.0f);
		Vector3f to = new Vector3f(dirX, dirY, dirZ);
		Quaternionf q = new Quaternionf().rotationTo(from, to);
		Vector3f euler = new Vector3f();
		q.getEulerAnglesZYX(euler);
		part.zRot = euler.x;
		part.yRot = euler.y;
		part.xRot = euler.z;
	}

	private static void setBrakeVisible(ModelPart[] lines, boolean visible) {
		for (ModelPart line : lines) {
			line.visible = visible;
		}
	}

	/**
	 * Transforms a local point on an arm ModelPart into root pixel space
	 * (same units as part.x / cube coords).
	 */
	public static void transformArmPoint(ModelPart arm, float lx, float ly, float lz, Vector3f out) {
		out.set(lx * arm.xScale, ly * arm.yScale, lz * arm.zScale);
		new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).transform(out);
		out.add(arm.x, arm.y, arm.z);
	}

	/**
	 * Converts a root-space point into body-local space given the body's pose.
	 */
	public static void rootToBody(ModelPart body, Vector3f rootPoint, Vector3f out) {
		out.set(rootPoint.x - body.x, rootPoint.y - body.y, rootPoint.z - body.z);
		// Inverse of rotationZYX(body.z, body.y, body.x)
		new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot).conjugate().transform(out);
		if (body.xScale != 0.0f) {
			out.x /= body.xScale;
		}
		if (body.yScale != 0.0f) {
			out.y /= body.yScale;
		}
		if (body.zScale != 0.0f) {
			out.z /= body.zScale;
		}
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

		// 4 thin suspension lines from backpack up to the canopy.
		// ~0.28 thick so they read as lines, not beams.
		final float suspHalf = 0.14F;
		final float suspSize = 0.28F;
		// Line 1: Far Left (connects to x = -27)
		canopyPart.addOrReplaceChild("line_far_left",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-suspHalf, -50.1F, -suspHalf, suspSize, 50.1F, suspSize),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, 0.571F)
		);
		// Line 2: Mid Left (connects to x = -11)
		canopyPart.addOrReplaceChild("line_mid_left",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-suspHalf, -43.6F, -suspHalf, suspSize, 43.6F, suspSize),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, 0.256F)
		);
		// Line 3: Mid Right (connects to x = +11)
		canopyPart.addOrReplaceChild("line_mid_right",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-suspHalf, -43.6F, -suspHalf, suspSize, 43.6F, suspSize),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, -0.256F)
		);
		// Line 4: Far Right (connects to x = +27)
		canopyPart.addOrReplaceChild("line_far_right",
			CubeListBuilder.create()
				.texOffs(0, 64)
				.addBox(-suspHalf, -50.1F, -suspHalf, suspSize, 50.1F, suspSize),
			PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, -0.095F, 0.0F, -0.571F)
		);

		// Brake lines: unit-length cubes along -Y; transformed each frame from hands
		// to the trailing edge. Slightly thicker than suspension so they stay readable.
		PartDefinition brakeRoot = rootPart.addOrReplaceChild("brake_lines", CubeListBuilder.create(), PartPose.ZERO);
		addBrakeLinePart(brakeRoot, "left_main");
		addBrakeLinePart(brakeRoot, "left_branch_a");
		addBrakeLinePart(brakeRoot, "left_branch_b");
		addBrakeLinePart(brakeRoot, "left_branch_c");
		addBrakeLinePart(brakeRoot, "right_main");
		addBrakeLinePart(brakeRoot, "right_branch_a");
		addBrakeLinePart(brakeRoot, "right_branch_b");
		addBrakeLinePart(brakeRoot, "right_branch_c");

		return LayerDefinition.create(mesh, 256, 128);
	}

	private static void addBrakeLinePart(PartDefinition parent, String name) {
		// Thin dark line (~0.36); runtime x/z scale keeps main slightly thicker than branches.
		final float half = 0.18F;
		final float size = 0.36F;
		parent.addOrReplaceChild(name,
			CubeListBuilder.create()
				.texOffs(4, 64)
				.addBox(-half, -BRAKE_LINE_BASE_LEN, -half, size, BRAKE_LINE_BASE_LEN, size),
			PartPose.ZERO
		);
	}
}
