package com.piotrek.peterwolfsplanes.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlaneEntity extends Entity {
	private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> RUDDER = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> PUSHER_ID = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.INT);

	private static final double GRAVITY = 0.04D;
	private static final double AIRFLOW_FOR_FULL_CONTROL = 0.45D;
	private static final double THRUST_FORCE = 0.042D;
	private static final double BASE_DRAG = 0.012D;
	private static final double SPEED_DRAG = 0.018D;
	private static final double LIFT_COEFFICIENT = 0.17D;
	private static final double MAX_LIFT = 0.085D;
	private static final float MAX_BANK = 65.0F;
	private static final float MAX_YAW_RATE = 3.0F;
	private static final float MAX_PITCH_RATE = 1.6F;
	private static final float ENGINE_RESPONSE = 0.025F;
	private static final float INPUT_SMOOTHING = 0.12F;
	private static final float PILOT_LOOK_CENTERING = 0.05F;
	private static final float GROUND_YAW_RATE = 1.75F;
	private static final float GROUND_YAW_RESPONSE = 0.18F;
	private static final float EMPTY_GROUND_VISUAL_PITCH = -12.0F;
	private static final float THROTTLE_IDLE_EPSILON = 1.0E-3F;
	private static final float GROUND_IDLE_ENGINE_EPSILON = 0.03F;
	private static final double GROUND_IDLE_HOLD_SPEED = 0.04D;
	private static final double GROUND_IDLE_VERTICAL_SPEED = 0.05D;
	private static final int GROUND_CONTACT_GRACE_TICKS = 6;

	private float propellerAngle = 0.0F;
	private float propellerAngleO = 0.0F;
	private float propellerSpeed = 0.0F;
	private float visualPitch = 0.0F;
	private float visualPitchO = 0.0F;
	private float visualRoll = 0.0F;
	private float visualRollO = 0.0F;
	private float smoothedRelativeYaw = 0.0F;
	private float smoothedRelativePitch = 0.0F;
	private float yawRate = 0.0F;
	private float pitchRate = 0.0F;
	private float enginePower = 0.0F;
	private int groundContactGraceTicks = 0;
	private int emptyGroundPoseTicks = 0;
	// Smooth yaw tracking for rendering interpolation (client-side only)
	private float renderYaw = Float.NaN;
	private float renderYawO = Float.NaN;

	// AI / Squadron pilot fields
	private PlaneEntity aiLeader = null;
	private Vec3 aiWaypoint = null;
	private float aiTargetRelativeYaw = 0.0F;
	private float aiTargetRelativePitch = 0.0F;
	private int aiSearchCooldown = 0;
	private int aiWaypointTicks = 0;

	// Solo villager pilot patrol (figure-8 after climb)
	private Vec3 aiPatrolCenter = null;
	private double aiPatrolPhase = 0.0;

	public void setAiLeader(PlaneEntity leader) {
		this.aiLeader = leader;
	}

	public PlaneEntity getAiLeader() {
		return this.aiLeader;
	}

	public PlaneEntity(EntityType<?> type, Level world) {
		super(type, world);
		this.blocksBuilding = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(THROTTLE, 0.0F);
		builder.define(ROLL, 0.0F);
		builder.define(RUDDER, 0.0F);
		builder.define(PUSHER_ID, -1);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setThrottle(input.getFloatOr("Throttle", 0.0F));
		this.setRoll(input.getFloatOr("Roll", 0.0F));
		this.setRudder(input.getFloatOr("Rudder", 0.0F));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Throttle", this.getThrottle());
		output.putFloat("Roll", this.getRoll());
		output.putFloat("Rudder", this.getRudder());
	}

	public int getPusherId() {
		return this.entityData.get(PUSHER_ID);
	}

	public void setPusherId(int id) {
		this.entityData.set(PUSHER_ID, id);
	}

	public boolean isBeingPushed() {
		return this.getPusherId() != -1;
	}

	public float getThrottle() {
		return this.entityData.get(THROTTLE);
	}

	public void setThrottle(float value) {
		if (!Float.isFinite(value)) {
			value = 0.0F;
		}
		value = Mth.clamp(value, -1.0F, 1.0F);
		if (Math.abs(value - this.entityData.get(THROTTLE)) > 1.0E-4F) {
			this.entityData.set(THROTTLE, value);
		}
	}

	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public float getRoll(float partialTick) {
		return Mth.rotLerp(partialTick, this.visualRollO, this.visualRoll);
	}

	public void setRoll(float value) {
		if (!Float.isFinite(value)) {
			value = 0.0F;
		}
		value = Mth.wrapDegrees(value);
		if (Math.abs(Mth.wrapDegrees(value - this.entityData.get(ROLL))) > 1.0E-3F) {
			this.entityData.set(ROLL, value);
		}
	}

	public float getRudder() {
		return this.entityData.get(RUDDER);
	}

	public void setRudder(float value) {
		if (!Float.isFinite(value)) {
			value = 0.0F;
		}
		value = Mth.clamp(value, -1.0F, 1.0F);
		if (Math.abs(value - this.entityData.get(RUDDER)) > 1.0E-4F) {
			this.entityData.set(RUDDER, value);
		}
	}

	public float getPropellerAngle() {
		return this.propellerAngle;
	}

	public float getPropellerAngle(float partialTick) {
		return Mth.rotLerp(partialTick, this.propellerAngleO, this.propellerAngle);
	}

	public float getVisualPitch(float partialTick) {
		return Mth.lerp(partialTick, this.visualPitchO, this.visualPitch);
	}

	public double getInstrumentSpeedMetersPerSecond() {
		Vec3 velocity = this.getDeltaMovement();
		if (this.shouldHoldGroundIdleInstruments(velocity)) {
			return 0.0D;
		}
		return velocity.length() * 20.0D;
	}

	public double getInstrumentVerticalSpeedMetersPerSecond() {
		Vec3 velocity = this.getDeltaMovement();
		if (this.shouldHoldGroundIdleInstruments(velocity)) {
			return 0.0D;
		}
		return velocity.y * 20.0D;
	}

	@Override
	public void tick() {
		// Snapshot pre-tick rotation for smooth rendering interpolation.
		// super.tick() copies yRot→yRotO and xRot→xRotO at tick start, but
		// our physics mutate yRot/xRot AFTER that copy, so the next frame's
		// yRotO would be one tick stale relative to what we wrote, causing a
		// one-tick jump every frame. We maintain our own renderYaw pair that
		// stays in sync with the physics writes.
		if (Float.isNaN(this.renderYaw)) {
			this.renderYaw = this.getYRot();
			this.renderYawO = this.getYRot();
		} else {
			this.renderYawO = this.renderYaw;
		}

		super.tick();

		if (this.level().isClientSide()) {
			if (this.isLocalClientAuthoritative()) {
				this.tickPhysics();
			}
			this.tickClientVisuals();
			return;
		}

		// When a player is piloting, isClientAuthoritative() returns true and
		// the server should not run physics — the client owns position.
		// We still run tickUnpilotedPhysics() when empty so the plane can
		// fall/coast on the server when nobody is aboard.
		if (!this.isClientAuthoritative()) {
			this.tickPhysics();
		}
	}

	private void tickClientVisuals() {
		this.visualPitchO = this.visualPitch;
		this.visualRollO = this.visualRoll;
		this.propellerAngleO = this.propellerAngle;
		float targetVisualPitch = this.getTargetVisualPitch();

		if (this.tickCount <= 1) {
			this.visualPitch = targetVisualPitch;
			this.visualPitchO = this.visualPitch;
			this.visualRoll = this.getRoll();
			this.visualRollO = this.visualRoll;
		} else {
			this.visualPitch = Mth.lerp(0.35F, this.visualPitch, targetVisualPitch);
			this.visualRoll = Mth.rotLerp(0.35F, this.visualRoll, this.getRoll());
		}

		float targetSpinSpeed = 0.0F;
		if (this.isVehicle()) {
			float throttle = Math.max(0.0F, this.getThrottle());
			targetSpinSpeed = 25.0F + throttle * 115.0F;
		}

		this.propellerSpeed = Mth.approach(this.propellerSpeed, targetSpinSpeed, 18.0F);
		this.propellerAngle += this.propellerSpeed;
		if (this.propellerAngle >= 360.0F) {
			this.propellerAngle %= 360.0F;
		} else if (this.propellerAngle < 0.0F) {
			this.propellerAngle = this.propellerAngle % 360.0F + 360.0F;
		}
	}

	private void tickNPCPilotedPhysics(Mob pilotMob, float currentThrottle) {
		Level world = this.level();
		Vec3 pos = this.position();

		// 1. Terrain avoidance and height check
		int currentX = (int) this.getX();
		int currentZ = (int) this.getZ();
		int height = world.getHeight(Heightmap.Types.MOTION_BLOCKING, currentX, currentZ);

		// Project forward 20 blocks in current heading direction
		Vec3 currentVelocity = this.getDeltaMovement();
		double vx = currentVelocity.x;
		double vz = currentVelocity.z;
		double headingLength = Math.sqrt(vx * vx + vz * vz);
		int aheadX = currentX;
		int aheadZ = currentZ;
		if (headingLength > 0.01D) {
			aheadX = (int) (this.getX() + (vx / headingLength) * 20.0D);
			aheadZ = (int) (this.getZ() + (vz / headingLength) * 20.0D);
		}
		int heightAhead = world.getHeight(Heightmap.Types.MOTION_BLOCKING, aheadX, aheadZ);
		int maxGround = Math.max(height, heightAhead);

		boolean lowAltitude = this.getY() < (double)(maxGround + 15);

		// 2. Leader & Squadron target lookup
		if (this.aiLeader != null && (!this.aiLeader.isAlive() || this.aiLeader.isRemoved())) {
			this.aiLeader = null;
		}

		// If no leader, scan for player plane nearby to follow
		if (this.aiLeader == null) {
			if (this.aiSearchCooldown-- <= 0) {
				this.aiSearchCooldown = 20; // check once per second
				List<PlaneEntity> nearbyPlanes = world.getEntitiesOfClass(
					PlaneEntity.class,
					this.getBoundingBox().inflate(120.0D),
					plane -> plane != this && plane.isAlive() && (plane.getFirstPassenger() instanceof Player)
				);
				if (!nearbyPlanes.isEmpty()) {
					this.aiLeader = nearbyPlanes.get(0);
				}
			}
		}

		Vec3 targetPos = null;
		float targetThrottle = 0.85F;

		if (this.aiLeader != null) {
			// Find our slot in the squadron to calculate formation offset
			List<PlaneEntity> followers = world.getEntitiesOfClass(
				PlaneEntity.class,
				this.getBoundingBox().inflate(150.0D),
				plane -> plane != this && plane.isAlive() && plane.getAiLeader() == this.aiLeader
			);
			// Sort them by entity ID to make slots deterministic
			followers.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
			int slot = 0;
			for (int i = 0; i < followers.size(); i++) {
				if (followers.get(i).getId() == this.getId()) {
					slot = i;
					break;
				}
				if (followers.get(i).getId() > this.getId()) {
					slot = i;
					break;
				}
				if (i == followers.size() - 1) {
					slot = followers.size();
				}
			}

			// Dynamic V-formation offsets
			int index = slot;
			double ox = (index % 2 == 0 ? -8.0D : 8.0D) * (index / 2 + 1);
			double oy = -2.0D * (index / 2 + 1);
			double oz = -12.0D * (index / 2 + 1);

			// Rotate offset by leader's rotation
			float leaderYawRad = (float) Math.toRadians(this.aiLeader.getYRot());
			Vec3 leaderForward = new Vec3(-Math.sin(leaderYawRad), 0.0D, Math.cos(leaderYawRad));
			Vec3 leaderRight = new Vec3(Math.cos(leaderYawRad), 0.0D, Math.sin(leaderYawRad));

			targetPos = this.aiLeader.position()
				.add(leaderRight.scale(ox))
				.add(0.0D, oy, 0.0D)
				.add(leaderForward.scale(oz));

			// Match leader speed, but apply proportional correction for distance
			double distToTarget = pos.distanceTo(targetPos);
			targetThrottle = this.aiLeader.getThrottle();
			if (distToTarget > 10.0D) {
				targetThrottle = Math.min(1.0F, targetThrottle + 0.2F);
			} else if (distToTarget < 3.0D) {
				targetThrottle = Math.max(0.1F, targetThrottle - 0.2F);
			}
		} else {
			// Solo villager pilot patrol:
			// 1. Climb (wznosić się) to safe altitude first
			// 2. Then level flight
			// 3. Fly figure-8 ("latac po 8") around a patrol center
			if (this.aiPatrolCenter == null) {
				double cx = this.getX();
				double cz = this.getZ();
				int cg = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) cx, (int) cz);
				this.aiPatrolCenter = new Vec3(cx, cg + 42.0D, cz);
				this.aiPatrolPhase = 0.0;
				this.aiWaypointTicks = 0;
			}

			// Periodically wander the patrol center so they don't stay in one spot forever
			if (++this.aiWaypointTicks > 900) {
				this.aiWaypointTicks = 0;
				double cx = this.aiPatrolCenter.x + (this.random.nextDouble() - 0.5D) * 90.0D;
				double cz = this.aiPatrolCenter.z + (this.random.nextDouble() - 0.5D) * 90.0D;
				int cg = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) cx, (int) cz);
				this.aiPatrolCenter = new Vec3(cx, cg + 42.0D, cz);
				this.aiPatrolPhase = this.random.nextDouble() * 6.2832;
			}

			this.aiPatrolPhase += 0.026; // traverse speed for nice figure-8 loops
			if (this.aiPatrolPhase > 12.566) {
				this.aiPatrolPhase -= 12.566;
			}

			double t = this.aiPatrolPhase;
			double a = 60.0; // size of the 8
			double s = Math.sin(t);
			double c = Math.cos(t);
			double d = 1.0 + s * s;
			double xOff = a * c / d;
			double zOff = a * s * c / d;

			double cruiseY = this.aiPatrolCenter.y;
			boolean needsClimb = this.getY() < (cruiseY - 6.0D);
			double tgtY = needsClimb ? (cruiseY + 6.0D) : cruiseY;

			targetPos = new Vec3(this.aiPatrolCenter.x + xOff, tgtY, this.aiPatrolCenter.z + zOff);
			targetThrottle = needsClimb ? 0.95F : 0.82F;
		}

		// Adjust target position for terrain avoidance / ensure climb
		if (lowAltitude || (targetPos != null && this.getY() < maxGround + 18.0D)) {
			targetPos = new Vec3(targetPos.x, Math.max(targetPos.y, maxGround + 28.0D), targetPos.z);
			targetThrottle = 1.0F;
		}

		// 3. Takeoff logic
		boolean grounded = this.resolveGroundHandling(this.onGround(), currentThrottle, currentVelocity);
		if (grounded && this.getY() < (double) (maxGround + 8)) {
			// Runway takeoff run: full throttle, keep steering straight
			this.setThrottle(1.0F);
			this.setRudder(0.0F);
			double speed = Math.max(0.0D, currentVelocity.length());
			if (speed < 0.45D) {
				this.aiTargetRelativeYaw = 0.0F;
				this.aiTargetRelativePitch = 0.0F;
			} else {
				this.aiTargetRelativeYaw = 0.0F;
				this.aiTargetRelativePitch = -25.0F; // Pitch up
			}
			this.tickPilotedPhysics(null, 1.0F, this.aiTargetRelativeYaw, this.aiTargetRelativePitch);
			return;
		}

		// 4. Cruising Flight AI Target steering
		Vec3 dir = targetPos.subtract(pos);
		double targetYaw = Math.toDegrees(Math.atan2(-dir.x, dir.z));
		double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		double targetPitch = Math.toDegrees(Math.atan2(-dir.y, horizontalDist));

		float yawDiff = Mth.wrapDegrees((float) targetYaw - this.getYRot());
		float pitchDiff = Mth.wrapDegrees((float) targetPitch - this.getXRot());

		// Clamp steering inputs
		this.aiTargetRelativeYaw = Mth.clamp(yawDiff, -45.0F, 45.0F);
		this.aiTargetRelativePitch = Mth.clamp(pitchDiff, -30.0F, 30.0F);

		// For villager pilots without a leader: force proper climb first (nose up),
		// then once at altitude they will naturally level (pitch ~0) and follow the figure-8 target.
		if (this.aiLeader == null) {
			int localGround = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) this.getX(), (int) this.getZ());
			if (this.getY() < localGround + 35.0D) {
				// Still climbing - command strong nose-up so they actually ascend instead of flying level/straight
				if (this.getY() < localGround + 18.0D) {
					this.aiTargetRelativePitch = -16.0F;
				} else {
					this.aiTargetRelativePitch = Math.min(this.aiTargetRelativePitch, -10.0F);
				}
				targetThrottle = 1.0F;
			}
		}

		this.setThrottle(targetThrottle);
		this.tickPilotedPhysics(null, targetThrottle, this.aiTargetRelativeYaw, this.aiTargetRelativePitch);
	}

	protected double getThrustForce() { return THRUST_FORCE; }
	protected double getBaseDrag() { return BASE_DRAG; }
	protected double getSpeedDrag() { return SPEED_DRAG; }
	protected double getAirflowForFullControl() { return AIRFLOW_FOR_FULL_CONTROL; }
	protected float getMaxBank() { return MAX_BANK; }
	protected float getMaxYawRate() { return MAX_YAW_RATE; }
	protected float getMaxPitchRate() { return MAX_PITCH_RATE; }

	private void tickPhysics() {
		if (this.isBeingPushed()) {
			Entity pusherEntity = this.level().getEntity(this.getPusherId());
			if (pusherEntity instanceof Player pusher && pusher.isAlive() && pusher.level() == this.level() && !pusher.isPassenger() && !this.isVehicle() && this.distanceToSqr(pusher) <= 49.0D) {
				this.tickWheelbarrowPushing(pusher);
				return;
			} else {
				this.setPusherId(-1);
			}
		}

		float currentThrottle = this.getThrottle();
		Entity passenger = this.getFirstPassenger();

		if (passenger instanceof Player pilot) {
			this.tickPilotedPhysics(pilot, currentThrottle, 0.0F, 0.0F);
			return;
		} else if (passenger instanceof Mob pilotMob) {
			this.tickNPCPilotedPhysics(pilotMob, currentThrottle);
			return;
		}

		this.tickUnpilotedPhysics();
	}

	private void tickWheelbarrowPushing(Player pusher) {
		this.setThrottle(0.0F);
		this.setRudder(0.0F);
		this.enginePower = 0.0F;

		float pusherYaw = pusher.getYRot();
		Vec3 pusherPos = pusher.position();
		float yawRad = (float) Math.toRadians(pusherYaw);

		Vec3 pusherForward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
		Vec3 tailTarget = pusherPos.add(pusherForward.scale(0.5D));

		// Position plane center ~1.4m ahead of tail along pusher heading
		Vec3 targetPos = tailTarget.add(pusherForward.scale(1.4D));

		Vec3 moveVec = targetPos.subtract(this.position());
		this.setPlaneYaw(pusherYaw);
		this.setXRot(-10.0F); // Lift tail in wheelbarrow pose
		this.setRoll(0.0F);

		this.setDeltaMovement(moveVec);
		this.move(MoverType.SELF, moveVec);
		this.setDeltaMovement(Vec3.ZERO);
	}

	private void tickPilotedPhysics(@Nullable Player pilot, float currentThrottle, float aiRelativeYaw, float aiRelativePitch) {
		Vec3 currentVelocity = this.getDeltaMovement();
		boolean grounded = this.resolveGroundHandling(this.onGround(), currentThrottle, currentVelocity);
		Vec3 heading = grounded ? this.calculateGroundHeading(this.getYRot()) : this.calculateHeading(this.getYRot(), this.getXRot());
		double speed = Math.max(0.0D, currentVelocity.dot(heading));
		float relativeYaw = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - this.getYRot()), -80.0F, 80.0F) : aiRelativeYaw;
		float relativePitch = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - this.getXRot()), -80.0F, 80.0F) : aiRelativePitch;
		float controlEfficiency = Mth.clamp((float) (speed / getAirflowForFullControl()), 0.0F, 1.0F);

		if (pilot != null && !grounded) {
			this.centerPilotLook(pilot, relativeYaw, relativePitch);
			relativeYaw = Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - this.getYRot()), -80.0F, 80.0F);
			relativePitch = Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - this.getXRot()), -80.0F, 80.0F);
		}

		this.enginePower = Mth.approach(this.enginePower, Math.max(0.0F, currentThrottle), ENGINE_RESPONSE);

		float targetYawRate;
		if (grounded) {
			this.smoothedRelativeYaw = Mth.approach(this.smoothedRelativeYaw, 0.0F, 4.0F);
			this.smoothedRelativePitch = Mth.approach(this.smoothedRelativePitch, 0.0F, 4.0F);
			this.pitchRate = Mth.approach(this.pitchRate, 0.0F, 0.35F);
			this.setRoll(Mth.approachDegrees(this.getRoll(), 0.0F, 4.0F));
			this.setXRot(Mth.approach(this.getXRot(), 0.0F, 3.0F));

			targetYawRate = this.getRudder() * GROUND_YAW_RATE;
		} else {
			this.smoothedRelativeYaw += (relativeYaw - this.smoothedRelativeYaw) * INPUT_SMOOTHING;
			this.smoothedRelativePitch += (relativePitch - this.smoothedRelativePitch) * INPUT_SMOOTHING;

			float targetRoll = Mth.clamp(this.smoothedRelativeYaw * 0.75F + this.getRudder() * 20.0F, -getMaxBank(), getMaxBank());
			float rollStep = 0.7F + 2.6F * controlEfficiency;
			this.setRoll(Mth.approachDegrees(this.getRoll(), targetRoll, rollStep));

			float pitchLeveling = -this.getXRot() * (0.012F + 0.018F * controlEfficiency);
			float targetPitchRate = Mth.clamp(-this.smoothedRelativePitch * 0.025F * controlEfficiency + pitchLeveling, -getMaxPitchRate(), getMaxPitchRate());
			this.pitchRate = Mth.approach(this.pitchRate, targetPitchRate, 0.16F);
			this.setXRot(Mth.clamp(this.getXRot() + this.pitchRate, -75.0F, 60.0F));

			float bankTurn = (float) Math.sin(Math.toRadians(this.getRoll())) * (1.45F * controlEfficiency);
			float rudderTurn = this.getRudder() * (0.35F + 1.15F * controlEfficiency);
			float coordinatedTurn = this.smoothedRelativeYaw * 0.006F * controlEfficiency;
			targetYawRate = Mth.clamp(bankTurn + rudderTurn + coordinatedTurn, -getMaxYawRate(), getMaxYawRate());
		}

		float yawResponse = grounded ? GROUND_YAW_RESPONSE : 0.22F;
		this.yawRate = Mth.clamp(Mth.approach(this.yawRate, targetYawRate, yawResponse), -getMaxYawRate(), getMaxYawRate());
		this.setPlaneYaw(this.getYRot() + this.yawRate);

		heading = grounded ? this.calculateGroundHeading(this.getYRot()) : this.calculateHeading(this.getYRot(), this.getXRot());
		speed = this.applyForces(speed, heading, currentThrottle, grounded);
		if (grounded && this.hasGroundIdlePower(currentThrottle) && speed < GROUND_IDLE_HOLD_SPEED) {
			speed = 0.0D;
		}

		double sink = 0.0D;
		if (!grounded) {
			double liftY = this.calculateLiftY(speed);
			double liftDeficit = liftY - GRAVITY;
			if (liftDeficit < 0.0D) {
				float stallPitch = (float) Mth.clamp(-liftDeficit * 34.0D, 0.0D, 2.2D);
				this.setXRot(Mth.clamp(this.getXRot() + stallPitch, -75.0F, 60.0F));
				sink = Mth.clamp(liftDeficit * 1.45D, -0.10D, 0.0D);
				heading = this.calculateHeading(this.getYRot(), this.getXRot());
			}
		}

		Vec3 movement = heading.scale(speed).add(0.0D, sink, 0.0D);
		if (grounded && speed == 0.0D && this.hasGroundIdlePower(currentThrottle)) {
			movement = Vec3.ZERO;
		}
		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);
		if (grounded && speed == 0.0D) {
			this.setDeltaMovement(Vec3.ZERO);
		}
	}

	private void centerPilotLook(Player pilot, float relativeYaw, float relativePitch) {
		float yawDelta = -relativeYaw * PILOT_LOOK_CENTERING;
		float pitchDelta = -relativePitch * PILOT_LOOK_CENTERING;

		pilot.setYRot(pilot.getYRot() + yawDelta);
		pilot.yRotO += yawDelta;
		pilot.setXRot(pilot.getXRot() + pitchDelta);
		pilot.xRotO += pitchDelta;
	}

	private double applyForces(double speed, Vec3 heading, float currentThrottle, boolean grounded) {
		double thrustForce = this.enginePower * getThrustForce();
		double gravityAssist = -GRAVITY * heading.y * 0.85D;
		double dragForce = speed * (getBaseDrag() + Math.min(speed, 1.6D) * getSpeedDrag());
		speed = Math.max(0.0D, speed + thrustForce + gravityAssist - dragForce);

		if (grounded) {
			double brakeAmount = currentThrottle < 0.0F ? 0.065D * -currentThrottle : 0.0D;
			double rollingResistance = 0.006D + Math.abs(this.getRudder()) * 0.0015D;
			speed = Math.max(0.0D, speed - brakeAmount - rollingResistance);
		}

		return speed;
	}

	private double calculateLiftY(double speed) {
		double liftAmount = Math.min(MAX_LIFT, speed * speed * LIFT_COEFFICIENT);
		float yawRad = (float) Math.toRadians(this.getYRot());
		float pitchRad = (float) Math.toRadians(this.getXRot());
		float rollRad = (float) Math.toRadians(this.getRoll());
		Vec3 unrolledUp = new Vec3(
			Math.sin(yawRad) * Math.sin(pitchRad),
			Math.cos(pitchRad),
			-Math.cos(yawRad) * Math.sin(pitchRad)
		);
		Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

		return unrolledUp.scale(Math.cos(rollRad)).add(right.scale(Math.sin(rollRad))).scale(liftAmount).y;
	}

	private Vec3 calculateHeading(float yaw, float pitch) {
		float yawRad = (float) Math.toRadians(yaw);
		float pitchRad = (float) Math.toRadians(pitch);

		return new Vec3(
			-Math.sin(yawRad) * Math.cos(pitchRad),
			-Math.sin(pitchRad),
			Math.cos(yawRad) * Math.cos(pitchRad)
		);
	}

	private Vec3 calculateGroundHeading(float yaw) {
		float yawRad = (float) Math.toRadians(yaw);

		return new Vec3(
			-Math.sin(yawRad),
			0.0D,
			Math.cos(yawRad)
		);
	}

	private float getTargetVisualPitch() {
		if (this.isBeingPushed()) {
			return -10.0F;
		}
		if (this.shouldUseEmptyGroundVisualPose()) {
			return EMPTY_GROUND_VISUAL_PITCH;
		}
		return this.getXRot();
	}

	private boolean shouldUseEmptyGroundVisualPose() {
		if (this.getFirstPassenger() != null) {
			this.emptyGroundPoseTicks = 0;
			return false;
		}

		Vec3 velocity = this.getDeltaMovement();
		boolean settled = this.isLowGroundIdleSpeed(velocity);
		if (this.onGround() && settled) {
			this.emptyGroundPoseTicks = GROUND_CONTACT_GRACE_TICKS;
			return true;
		}

		if (this.emptyGroundPoseTicks > 0 && settled) {
			this.emptyGroundPoseTicks--;
			return true;
		}

		this.emptyGroundPoseTicks = 0;
		return false;
	}

	private boolean resolveGroundHandling(boolean rawGrounded, float currentThrottle, Vec3 velocity) {
		if (rawGrounded) {
			this.groundContactGraceTicks = GROUND_CONTACT_GRACE_TICKS;
			return true;
		}

		if (this.groundContactGraceTicks > 0 && this.hasGroundIdlePower(currentThrottle) && this.isLowGroundIdleSpeed(velocity)) {
			this.groundContactGraceTicks--;
			return true;
		}

		this.groundContactGraceTicks = 0;
		return false;
	}

	private boolean shouldHoldGroundIdleInstruments(Vec3 velocity) {
		// Show zeroed instruments whenever the plane is sitting still on the
		// ground — this includes the case where the brake (negative throttle)
		// is held, which was previously excluded by the hasGroundIdlePower
		// check and caused HUD speed to flicker with the residual deltaMovement.
		return this.getFirstPassenger() instanceof Player
			&& (this.onGround() || this.groundContactGraceTicks > 0)
			&& this.isLowGroundIdleSpeed(velocity);
	}

	private boolean hasGroundIdlePower(float currentThrottle) {
		return Math.abs(currentThrottle) <= THROTTLE_IDLE_EPSILON && this.enginePower <= GROUND_IDLE_ENGINE_EPSILON;
	}

	private boolean isLowGroundIdleSpeed(Vec3 velocity) {
		double horizontalSpeedSqr = velocity.x * velocity.x + velocity.z * velocity.z;
		return horizontalSpeedSqr <= GROUND_IDLE_HOLD_SPEED * GROUND_IDLE_HOLD_SPEED
			&& Math.abs(velocity.y) <= GROUND_IDLE_VERTICAL_SPEED;
	}

	private void setPlaneYaw(float yaw) {
		float wrappedYaw = Mth.wrapDegrees(yaw);
		this.setYRot(wrappedYaw);
		this.setYHeadRot(wrappedYaw);
		this.setYBodyRot(wrappedYaw);
		// Keep our render-yaw in sync so interpolation uses the correct "new" value.
		this.renderYaw = wrappedYaw;
	}

	/**
	 * Returns the yaw angle for rendering, properly interpolated between the
	 * pre-tick and post-tick values that our physics wrote this tick.
	 * This avoids the one-tick stale-yRotO shudder that occurs when we call
	 * setYRot() after super.tick() has already snapshotted yRotO.
	 */
	public float getRenderYaw(float partialTick) {
		if (Float.isNaN(this.renderYawO)) {
			return this.renderYaw;
		}
		return Mth.rotLerp(partialTick, this.renderYawO, this.renderYaw);
	}

	private void tickUnpilotedPhysics() {
		this.enginePower = Mth.approach(this.enginePower, 0.0F, ENGINE_RESPONSE * 2.0F);
		this.yawRate = Mth.approach(this.yawRate, 0.0F, 0.35F);
		this.pitchRate = Mth.approach(this.pitchRate, 0.0F, 0.35F);
		this.smoothedRelativeYaw = Mth.approach(this.smoothedRelativeYaw, 0.0F, 4.0F);
		this.smoothedRelativePitch = Mth.approach(this.smoothedRelativePitch, 0.0F, 4.0F);

		if (this.onGround()) {
			this.groundContactGraceTicks = GROUND_CONTACT_GRACE_TICKS;
			this.setDeltaMovement(Vec3.ZERO);
			this.setXRot(Mth.approach(this.getXRot(), 0.0F, 3.0F));
		} else {
			Vec3 vel = this.getDeltaMovement().add(0.0D, -GRAVITY, 0.0D).scale(0.95D);
			this.setDeltaMovement(vel);
			this.move(MoverType.SELF, vel);
		}

		this.setThrottle(0.0F);
		this.setRoll(Mth.approachDegrees(this.getRoll(), 0.0F, 4.0F));
		this.setRudder(0.0F);
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		this.clampRotation(passenger);
	}

	protected void clampRotation(Entity passenger) {
		passenger.setYBodyRot(this.getYRot());
		float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
		float g = Mth.clamp(f, -80.0F, 80.0F);
		passenger.yRotO += g - f;
		passenger.setYRot(passenger.getYRot() + g - f);
	}

	@Override
	@Nullable
	public LivingEntity getControllingPassenger() {
		Entity passenger = this.getFirstPassenger();
		return passenger instanceof LivingEntity ? (LivingEntity) passenger : null;
	}

	/**
	 * Tells the server that position authority belongs to the client whenever
	 * a player is piloting. This suppresses the server-side physics tick and
	 * stops it from sending MoveVehiclePacket corrections that previously
	 * caused the rare "teleport jitter" seen while flying or sitting still.
	 */
	@Override
	public boolean isClientAuthoritative() {
		return this.getFirstPassenger() instanceof Player;
	}


	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction) {
		if (this.hasPassenger(passenger)) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			double rx = -Math.sin(yawRad) * -0.5D;
			double rz = Math.cos(yawRad) * -0.5D;
			moveFunction.accept(passenger, this.getX() + rx, this.getY() - 0.475D, this.getZ() + rz);
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public Item getDropItem() {
		return com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod.PLANE_ITEM;
	}

	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (this.isRemoved()) return false;
		if (this.getFirstPassenger() instanceof Player) return false;
		if (source.getEntity() instanceof Player player && !player.isSpectator()) {
			if (!player.getAbilities().instabuild) {
				this.spawnAtLocation(world, this.getDropItem());
			}
			this.discard();
			return true;
		}
		return false;
	}

	@Override
	public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
		if (player.isSecondaryUseActive()) {
			if (this.getPusherId() == player.getId()) {
				this.setPusherId(-1);
				return net.minecraft.world.InteractionResult.SUCCESS;
			}

			Vec3 heading = this.calculateGroundHeading(this.getYRot());
			Vec3 toPlayer = player.position().subtract(this.position());
			double dot = toPlayer.x * heading.x + toPlayer.z * heading.z;

			if (dot < -0.2D && !this.isVehicle() && !this.isBeingPushed()) {
				this.setPusherId(player.getId());
				return net.minecraft.world.InteractionResult.SUCCESS;
			}
			return net.minecraft.world.InteractionResult.PASS;
		} else {
			if (this.isBeingPushed()) {
				if (this.getPusherId() == player.getId()) {
					this.setPusherId(-1);
					return net.minecraft.world.InteractionResult.SUCCESS;
				}
				return net.minecraft.world.InteractionResult.PASS;
			}
			if (!this.level().isClientSide()) {
				return player.startRiding(this) ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
	}
}
