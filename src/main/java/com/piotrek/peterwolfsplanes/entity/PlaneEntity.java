package com.piotrek.peterwolfsplanes.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.compat.ReplayPlayback;
import java.util.List;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlaneEntity extends Entity {
	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
	private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> RUDDER = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> PUSHER_ID = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> COMBAT_MODE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> COMBAT_HEALTH = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> SIMPLE_ALTITUDE_CONTROL = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> HELICOPTER_AIRBORNE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.BOOLEAN);
	public static boolean defaultHelicopterSimpleAltitude = true;

	private static final float MAX_COMBAT_HEALTH = 40.0F;
	private static final int GUN_COOLDOWN_TICKS = 2;
	private static final int BOMB_COOLDOWN_TICKS = 20;
	private static final int BOMB_FUSE_TICKS = 40;
	private static final float GUN_MUZZLE_VELOCITY = 4.25F;
	private static final float GUN_SPREAD = 0.85F;
	private static final double GUN_BASE_DAMAGE = 3.0D;

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
	private static final int LANDING_IMPACT_COOLDOWN_TICKS = 20;
	private static final int MIN_AIRBORNE_TICKS_FOR_SYNC_LANDING = 8;
	private static final double LANDING_SAFE_SINK = 0.18D;
	private static final double LANDING_FATAL_SINK = 0.70D;
	private static final double LANDING_WATER_SAFE_SINK = 0.22D;
	private static final double LANDING_WATER_FATAL_SINK = 0.85D;
	private static final double LANDING_TELEPORT_SINK = 4.0D;
	/** Distance (blocks) above ground at which descent-speed damping kicks in during vertical landing. */
	private static final double HELI_LANDING_BUFFER = 4.0D;
	/** Gentle sink kept near the ground so S still completes a landing instead of hovering forever. */
	private static final double HELI_MIN_DESCENT_SPEED = 0.03D;

	// NPC / villager pilot flight AI
	/** Horizontal speed (blocks/tick) required before rotation / liftoff. */
	private static final double AI_TAKEOFF_SPEED = 0.42D;
	/** Absolute plane pitch (degrees): negative = nose up / climb. */
	private static final float AI_CLIMB_PITCH = -24.0F;
	private static final float AI_STEEP_CLIMB_PITCH = -32.0F;
	private static final float AI_CRUISE_PITCH = -2.0F;
	/** Target AGL for solo patrol cruise. */
	private static final double AI_CRUISE_AGL = 42.0D;
	/** Minimum clearance above terrain / obstacles. */
	private static final double AI_MIN_CLEARANCE = 14.0D;
	private static final double AI_HARD_CLEARANCE = 7.0D;
	/** Stronger pitch authority for AI than player look-follow. */
	private static final float AI_PITCH_GAIN = 0.12F;
	private static final float AI_MAX_PITCH_RATE = 2.4F;
	private static final int AI_RETURN_MIN_TICKS = 20 * 60 * 3;
	private static final int AI_RETURN_RANDOM_TICKS = 20 * 60 * 2;
	private static final int AI_RUNWAY_SEARCH_RETRY_TICKS = 20 * 20;
	private static final int AI_RUNWAY_SEARCH_RADIUS_CHUNKS = 96;
	private static final int AI_LANDING_PATROL = 0;
	private static final int AI_LANDING_APPROACH = 1;
	private static final int AI_LANDING_LINEUP = 2;
	private static final int AI_LANDING_FINAL = 3;
	private static final int AI_LANDING_ROLLOUT = 4;
	private static final int AI_LANDING_PARKED = 5;
	private static final double AI_APPROACH_CLEARANCE = 28.0D;
	private static final double AI_LINEUP_CLEARANCE = 9.0D;
	private static final double AI_APPROACH_EXTRA_DISTANCE = 72.0D;
	private static final double AI_LANDING_ALIGNMENT_DISTANCE = 22.0D;
	private static final double AI_PARK_SPEED = 0.045D;
	private static final double AI_MIN_FLIGHT_AIRSPEED = 0.44D;
	private static final double AI_MIN_FORWARD_VELOCITY = 0.38D;

	private float propellerAngle = 0.0F;
	private float propellerAngleO = 0.0F;
	private float propellerSpeed = 0.0F;
	private float visualPitch = 0.0F;
	private float visualPitchO = 0.0F;
	private float visualRoll = 0.0F;
	private float visualRollO = 0.0F;
	private float smoothedRelativeYaw = 0.0F;
	private float smoothedRelativePitch = 0.0F;
	private float heldHelicopterPitch = 0.0F;
	private float yawRate = 0.0F;
	private float pitchRate = 0.0F;
	private float enginePower = 0.0F;
	private int groundContactGraceTicks = 0;
	private int emptyGroundPoseTicks = 0;
	private int gunCooldownTicks = 0;
	private int bombCooldownTicks = 0;
	private boolean pendingGunFire = false;
	private boolean pendingBombDrop = false;
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
	/** When true, next tickPilotedPhysics treats NPC as airborne (takeoff rotation). */
	private boolean aiForceAirborne = false;
	/** Absolute desired XRot for NPC (negative = climb). Only used when pilot == null. */
	private float aiDesiredPitch = 0.0F;
	/** Randomized three-to-five-minute patrol timer before the pilot returns home. */
	private int aiReturnCountdown = -1;
	private int aiRunwaySearchCooldown = 0;
	private int aiLandingPhase = AI_LANDING_PATROL;
	private Vec3 aiRunwayCenter = null;
	private Vec3 aiRunwayDirection = null;
	private double aiRunwayHalfLength = 24.0D;
	private int aiLandingPhaseTicks = 0;
	private int landingImpactCooldown = 0;
	private double lastSyncY = Double.NaN;
	private boolean lastSyncAirborne = false;
	private int syncAirborneTicks = 0;
	private double peakSyncSink = 0.0D;

	public void setAiLeader(PlaneEntity leader) {
		this.aiLeader = leader;
	}

	public PlaneEntity getAiLeader() {
		return this.aiLeader;
	}

	public void beginNpcFlight() {
		if (this.level().isClientSide()) {
			return;
		}
		this.aiLeader = null;
		this.aiPatrolCenter = null;
		this.aiPatrolPhase = 0.0D;
		this.aiReturnCountdown = -1;
		this.aiRunwaySearchCooldown = 0;
		this.aiLandingPhase = AI_LANDING_PATROL;
		this.aiRunwayCenter = null;
		this.aiRunwayDirection = null;
		this.aiLandingPhaseTicks = 0;
	}

	public PlaneEntity(EntityType<?> type, Level world) {
		super(type, world);
		this.blocksBuilding = true;
	}

	@Override
	public InterpolationHandler getInterpolation() {
		return this.interpolation;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(THROTTLE, 0.0F);
		builder.define(ROLL, 0.0F);
		builder.define(RUDDER, 0.0F);
		builder.define(PUSHER_ID, -1);
		builder.define(COMBAT_MODE, false);
		builder.define(COMBAT_HEALTH, MAX_COMBAT_HEALTH);
		builder.define(SIMPLE_ALTITUDE_CONTROL, defaultHelicopterSimpleAltitude);
		builder.define(HELICOPTER_AIRBORNE, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setThrottle(input.getFloatOr("Throttle", 0.0F));
		this.setRoll(input.getFloatOr("Roll", 0.0F));
		this.setRudder(input.getFloatOr("Rudder", 0.0F));
		this.setCombatMode(input.getBooleanOr("CombatMode", false));
		this.setCombatHealth(input.getFloatOr("CombatHealth", MAX_COMBAT_HEALTH));
		this.setSimpleAltitudeControl(input.getBooleanOr("SimpleAltitudeControl", defaultHelicopterSimpleAltitude));
		this.setHelicopterAirborne(input.getBooleanOr("HelicopterAirborne", false));
		this.aiReturnCountdown = input.getIntOr("AiReturnCountdown", -1);
		this.aiLandingPhase = Mth.clamp(input.getIntOr("AiLandingPhase", AI_LANDING_PATROL), AI_LANDING_PATROL, AI_LANDING_PARKED);
		if (input.getBooleanOr("HasAiRunway", false)) {
			this.aiRunwayCenter = new Vec3(
				input.getDoubleOr("AiRunwayX", 0.0D),
				input.getDoubleOr("AiRunwayY", 0.0D),
				input.getDoubleOr("AiRunwayZ", 0.0D)
			);
			this.aiRunwayDirection = new Vec3(
				input.getDoubleOr("AiRunwayDirectionX", 0.0D),
				0.0D,
				input.getDoubleOr("AiRunwayDirectionZ", 1.0D)
			).normalize();
			this.aiRunwayHalfLength = Math.max(8.0D, input.getDoubleOr("AiRunwayHalfLength", 24.0D));
		} else if (this.aiLandingPhase != AI_LANDING_PATROL) {
			this.aiLandingPhase = AI_LANDING_PATROL;
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Throttle", this.getThrottle());
		output.putFloat("Roll", this.getRoll());
		output.putFloat("Rudder", this.getRudder());
		output.putBoolean("CombatMode", this.isCombatMode());
		output.putFloat("CombatHealth", this.getCombatHealth());
		output.putBoolean("SimpleAltitudeControl", this.isSimpleAltitudeControl());
		output.putBoolean("HelicopterAirborne", this.isHelicopterAirborne());
		output.putInt("AiReturnCountdown", this.aiReturnCountdown);
		output.putInt("AiLandingPhase", this.aiLandingPhase);
		boolean hasAiRunway = this.aiRunwayCenter != null && this.aiRunwayDirection != null;
		output.putBoolean("HasAiRunway", hasAiRunway);
		if (hasAiRunway) {
			output.putDouble("AiRunwayX", this.aiRunwayCenter.x);
			output.putDouble("AiRunwayY", this.aiRunwayCenter.y);
			output.putDouble("AiRunwayZ", this.aiRunwayCenter.z);
			output.putDouble("AiRunwayDirectionX", this.aiRunwayDirection.x);
			output.putDouble("AiRunwayDirectionZ", this.aiRunwayDirection.z);
			output.putDouble("AiRunwayHalfLength", this.aiRunwayHalfLength);
		}
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

	/** Engine loop volume for all clients. Uses synced occupancy/throttle, not local enginePower. */
	public float getEngineSoundVolume() {
		if (!this.isVehicle()) {
			return 0.0F;
		}
		float gain = this.getEngineSoundVolumeMultiplier();
		if (this.isHelicopter()) {
			float throttle = Mth.clamp(this.getThrottle(), -1.0F, 1.0F);
			if (!this.isHelicopterAirborne()) {
				float spool = Mth.clamp(throttle, 0.0F, 1.0F);
				return (0.16F + spool * 0.42F) * gain;
			}
			return (0.58F + Mth.clamp(Math.abs(throttle), 0.0F, 1.0F) * 0.42F) * gain;
		}
		float throttle = Mth.clamp(this.getThrottle(), 0.0F, 1.0F);
		return (0.30F + throttle * 0.70F) * gain;
	}

	public float getEngineSoundPitch() {
		if (this.isHelicopter()) {
			float throttle = Mth.clamp(this.getThrottle(), -1.0F, 1.0F);
			if (!this.isHelicopterAirborne()) {
				float spool = Mth.clamp(throttle, 0.0F, 1.0F);
				return 0.72F + spool * 0.22F;
			}
			return 0.94F + throttle * 0.16F;
		}
		float throttle = Mth.clamp(this.getThrottle(), 0.0F, 1.0F);
		return this.getEngineSoundPitchBase() + throttle * 0.36F;
	}

	protected float getEngineSoundVolumeMultiplier() {
		return 1.0F;
	}

	protected float getEngineSoundPitchBase() {
		return 0.84F;
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

	public boolean isCombatMode() {
		return this.entityData.get(COMBAT_MODE);
	}

	public void setCombatMode(boolean enabled) {
		if (this.entityData.get(COMBAT_MODE) != enabled) {
			this.entityData.set(COMBAT_MODE, enabled);
		}
	}

	public float getCombatHealth() {
		return this.entityData.get(COMBAT_HEALTH);
	}

	public float getMaxCombatHealth() {
		return MAX_COMBAT_HEALTH;
	}

	public void setCombatHealth(float health) {
		if (!Float.isFinite(health)) {
			health = MAX_COMBAT_HEALTH;
		}
		health = Mth.clamp(health, 0.0F, MAX_COMBAT_HEALTH);
		if (Math.abs(health - this.entityData.get(COMBAT_HEALTH)) > 1.0E-3F) {
			this.entityData.set(COMBAT_HEALTH, health);
		}
	}

	public boolean isSimpleAltitudeControl() {
		return this.entityData.get(SIMPLE_ALTITUDE_CONTROL);
	}

	public void setSimpleAltitudeControl(boolean simple) {
		if (this.entityData.get(SIMPLE_ALTITUDE_CONTROL) != simple) {
			this.entityData.set(SIMPLE_ALTITUDE_CONTROL, simple);
		}
	}

	public boolean isHelicopterAirborne() {
		return this.entityData.get(HELICOPTER_AIRBORNE);
	}

	public void setHelicopterAirborne(boolean airborne) {
		boolean wasAirborne = this.entityData.get(HELICOPTER_AIRBORNE);
		if (wasAirborne != airborne) {
			this.entityData.set(HELICOPTER_AIRBORNE, airborne);
			if (!wasAirborne && airborne) {
				Entity passenger = this.getFirstPassenger();
				this.zeroPilotCursor(passenger instanceof Player player ? player : null);
			}
		}
	}

	public void zeroPilotCursor(@Nullable Player pilot) {
		if (pilot != null) {
			pilot.setXRot(0.0F);
			pilot.xRotO = 0.0F;
			pilot.setYRot(this.getYRot());
			pilot.yRotO = this.getYRot();
			pilot.setYBodyRot(this.getYRot());
			pilot.setYHeadRot(this.getYRot());
		}
		this.heldHelicopterPitch = 0.0F;
		this.smoothedRelativePitch = 0.0F;
		this.smoothedRelativeYaw = 0.0F;
		this.pitchRate = 0.0F;
		this.yawRate = 0.0F;
		this.setRoll(0.0F);
		this.setXRot(0.0F);
	}

	public boolean isHelicopterTouchingGround() {
		if (this.onGround() || this.verticalCollisionBelow || this.hasSurfaceSupport()) {
			return true;
		}
		return !this.level().noCollision(this, this.getBoundingBox().expandTowards(0.0D, -0.06D, 0.0D));
	}

	/**
	 * Returns the vertical distance (blocks) from the bottom of this helicopter's
	 * bounding box to the nearest solid surface below, up to {@code maxScan} blocks.
	 * Returns {@code maxScan} when no ground is found within that range.
	 */
	private double getDistanceToGround(double maxScan) {
		double bottomY = this.getBoundingBox().minY;
		// Step downward in small increments looking for a collision
		double step = 0.25D;
		double checked = 0.0D;
		while (checked < maxScan) {
			double nextStep = Math.min(step, maxScan - checked);
			checked += nextStep;
			if (!this.level().noCollision(this, this.getBoundingBox().move(0.0D, -(checked), 0.0D))) {
				return checked - nextStep; // distance to top of colliding block
			}
		}
		return maxScan;
	}

	/**
	 * Slows a downward velocity as the helicopter nears the surface, but always keeps
	 * a minimum sink so holding S still completes touchdown.
	 */
	private double dampHelicopterDescent(double vy) {
		if (vy >= -1.0E-4D) {
			return vy;
		}
		double distToGround = this.getDistanceToGround(HELI_LANDING_BUFFER + 1.0D);
		if (distToGround >= HELI_LANDING_BUFFER) {
			return vy;
		}
		double factor = Mth.clamp(distToGround / HELI_LANDING_BUFFER, 0.0D, 1.0D);
		factor *= factor;
		double sink = Mth.lerp(factor, HELI_MIN_DESCENT_SPEED, Math.abs(vy));
		return -sink;
	}

	public void applyCombatInput(boolean combatMode, boolean fireGuns, boolean dropBomb) {
		boolean wasCombat = this.isCombatMode();
		this.setCombatMode(combatMode);
		if (combatMode != wasCombat && this.getFirstPassenger() instanceof Player pilot) {
			pilot.displayClientMessage(
				Component.translatable(combatMode
					? "message.peterwolfs_planes.combat_armed"
					: "message.peterwolfs_planes.combat_safe"),
				true
			);
			if (!this.level().isClientSide()) {
				this.level().playSound(
					null,
					this.getX(), this.getY(), this.getZ(),
					combatMode ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.NOTE_BLOCK_BASS.value(),
					SoundSource.PLAYERS,
					0.7F,
					combatMode ? 1.4F : 0.7F
				);
			}
		}

		if (!combatMode) {
			this.pendingGunFire = false;
			this.pendingBombDrop = false;
			return;
		}

		this.pendingGunFire = fireGuns;
		if (dropBomb) {
			this.pendingBombDrop = true;
		}
	}

	private void tickCombatWeapons() {
		if (this.gunCooldownTicks > 0) {
			this.gunCooldownTicks--;
		}
		if (this.bombCooldownTicks > 0) {
			this.bombCooldownTicks--;
		}

		if (!(this.getFirstPassenger() instanceof Player pilot) || !this.isCombatMode()) {
			this.pendingGunFire = false;
			this.pendingBombDrop = false;
			return;
		}

		if (this.pendingGunFire && this.gunCooldownTicks <= 0) {
			this.fireMachineGuns(pilot);
			this.gunCooldownTicks = GUN_COOLDOWN_TICKS;
		}

		if (this.pendingBombDrop && this.bombCooldownTicks <= 0) {
			if (this.dropBomb(pilot)) {
				this.bombCooldownTicks = BOMB_COOLDOWN_TICKS;
			}
			this.pendingBombDrop = false;
		} else if (this.pendingBombDrop && this.bombCooldownTicks > 0) {
			this.pendingBombDrop = false;
		}
	}

	private void fireMachineGuns(Player pilot) {
		Level world = this.level();
		if (world.isClientSide()) {
			return;
		}

		Vec3 heading = this.calculateHeading(this.getYRot(), this.getXRot());
		Vec3 right = this.calculateRightVector(this.getYRot());
		Vec3 planeVelocity = this.getDeltaMovement();
		// Twin guns: slightly outboard of the nose / wing root
		float[] lateralOffsets = this.getGunLateralOffsets();

		for (float lateral : lateralOffsets) {
			Vec3 muzzle = this.position()
				.add(heading.scale(1.65D))
				.add(right.scale(lateral))
				.add(0.0D, 0.35D, 0.0D);

			Arrow arrow = new Arrow(world, pilot, new ItemStack(Items.ARROW), null);
			arrow.setPos(muzzle.x, muzzle.y, muzzle.z);
			arrow.setOwner(pilot);
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
			arrow.setBaseDamage(GUN_BASE_DAMAGE);
			arrow.setCritArrow(true);
			arrow.shoot(heading.x, heading.y, heading.z, GUN_MUZZLE_VELOCITY, GUN_SPREAD);
			// Carry plane speed so shots land where the pilot is aiming in a dogfight
			arrow.setDeltaMovement(arrow.getDeltaMovement().add(planeVelocity));
			world.addFreshEntity(arrow);
		}

		world.playSound(
			null,
			this.getX(), this.getY(), this.getZ(),
			SoundEvents.FIREWORK_ROCKET_BLAST,
			SoundSource.PLAYERS,
			0.55F,
			1.35F + world.getRandom().nextFloat() * 0.25F
		);
	}

	protected float[] getGunLateralOffsets() {
		return new float[] { -0.55F, 0.55F };
	}

	private boolean dropBomb(Player pilot) {
		Level world = this.level();
		if (world.isClientSide()) {
			return false;
		}

		if (!this.consumeTnt(pilot)) {
			pilot.displayClientMessage(Component.translatable("message.peterwolfs_planes.need_tnt"), true);
			return false;
		}

		Vec3 heading = this.calculateHeading(this.getYRot(), this.getXRot());
		Vec3 dropPos = this.position()
			.add(heading.scale(-0.35D))
			.add(0.0D, -0.55D, 0.0D);

		PrimedTnt bomb = new PrimedTnt(world, dropPos.x, dropPos.y, dropPos.z, pilot);
		bomb.setFuse(BOMB_FUSE_TICKS);
		// Inherit most of the plane velocity so the charge falls away cleanly
		Vec3 bombVelocity = this.getDeltaMovement().scale(0.85D).add(0.0D, -0.15D, 0.0D);
		bomb.setDeltaMovement(bombVelocity);
		world.addFreshEntity(bomb);

		world.playSound(
			null,
			this.getX(), this.getY(), this.getZ(),
			SoundEvents.TNT_PRIMED,
			SoundSource.PLAYERS,
			1.0F,
			0.9F
		);
		return true;
	}

	private boolean consumeTnt(Player pilot) {
		if (pilot.hasInfiniteMaterials()) {
			return true;
		}

		for (int i = 0; i < pilot.getInventory().getContainerSize(); i++) {
			ItemStack stack = pilot.getInventory().getItem(i);
			if (stack.is(Items.TNT)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private Vec3 calculateRightVector(float yaw) {
		float yawRad = (float) Math.toRadians(yaw);
		return new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
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
			if (this.ownsClientSimulation()) {
				this.tickPhysics();
			} else {
				this.getInterpolation().interpolate();
			}
			// Remote/NPC aircraft receive yRot through entity interpolation rather
			// than setPlaneYaw(). Mirror that synchronized value into the renderer's
			// interpolation pair so the fuselage cannot remain at an old heading
			// while its passenger attachment point follows the live entity yaw.
			this.renderYaw = this.getYRot();
			this.tickClientVisuals();
			return;
		}

		if (ReplayPlayback.isActive()) {
			return;
		}

		// When a player is piloting, isClientAuthoritative() returns true and
		// the server should not run physics — the client owns position.
		// We still run tickUnpilotedPhysics() when empty so the plane can
		// fall/coast on the server when nobody is aboard.
		if (this.landingImpactCooldown > 0) {
			this.landingImpactCooldown--;
		}
		if (!this.isClientAuthoritative()) {
			this.tickPhysics();
		} else {
			this.tickLandingImpactFromSyncedPosition();
		}

		// Combat weapons always resolve server-side (entity spawn + inventory).
		this.tickCombatWeapons();
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
		} else if (this.isHelicopter()) {
			this.visualPitch = Mth.lerp(0.70F, this.visualPitch, targetVisualPitch);
			this.visualRoll = Mth.rotLerp(0.70F, this.visualRoll, this.getRoll());
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
		Vec3 currentVelocity = this.getDeltaMovement();
		this.aiForceAirborne = false;

		int currentX = Mth.floor(this.getX());
		int currentZ = Mth.floor(this.getZ());
		int groundHere = world.getHeight(Heightmap.Types.MOTION_BLOCKING, currentX, currentZ);
		double agl = this.getY() - groundHere;

		if (this.aiReturnCountdown < 0) {
			this.aiReturnCountdown = AI_RETURN_MIN_TICKS + this.random.nextInt(AI_RETURN_RANDOM_TICKS + 1);
		}

		if (this.aiLandingPhase == AI_LANDING_PATROL) {
			if (this.aiReturnCountdown > 0) {
				this.aiReturnCountdown--;
			} else if (world instanceof ServerLevel serverLevel) {
				if (this.aiRunwaySearchCooldown > 0) {
					this.aiRunwaySearchCooldown--;
				} else if (!this.beginRunwayReturn(serverLevel)) {
					this.aiRunwaySearchCooldown = AI_RUNWAY_SEARCH_RETRY_TICKS;
				}
			}
		}

		if (this.aiLandingPhase != AI_LANDING_PATROL
			&& this.tickRunwayLanding(groundHere, agl)) {
			return;
		}

		// Heading for forward scans (use yaw if nearly stopped)
		float yawRad = (float) Math.toRadians(this.getYRot());
		double hx = -Math.sin(yawRad);
		double hz = Math.cos(yawRad);
		double speedHoriz = Math.sqrt(currentVelocity.x * currentVelocity.x + currentVelocity.z * currentVelocity.z);

		// Multi-range obstacle / terrain scan ahead + left/right
		ObstacleScan scan = this.scanObstaclesAhead(world, hx, hz);

		// ── Leader / squadron ──────────────────────────────────────────
		if (this.aiLeader != null && (!this.aiLeader.isAlive() || this.aiLeader.isRemoved())) {
			this.aiLeader = null;
		}
		if (this.aiLeader == null) {
			if (this.aiSearchCooldown-- <= 0) {
				this.aiSearchCooldown = 20;
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

		Vec3 targetPos;
		float targetThrottle = 0.88F;
		float desiredPitch = AI_CRUISE_PITCH;
		float desiredYawRateCmd = 0.0F; // via relative yaw → bank

		if (this.aiLeader != null) {
			List<PlaneEntity> followers = world.getEntitiesOfClass(
				PlaneEntity.class,
				this.getBoundingBox().inflate(150.0D),
				plane -> plane != this && plane.isAlive() && plane.getAiLeader() == this.aiLeader
			);
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

			int index = slot;
			double ox = (index % 2 == 0 ? -8.0D : 8.0D) * (index / 2 + 1);
			double oy = -2.0D * (index / 2 + 1);
			double oz = -12.0D * (index / 2 + 1);

			float leaderYawRad = (float) Math.toRadians(this.aiLeader.getYRot());
			Vec3 leaderForward = new Vec3(-Math.sin(leaderYawRad), 0.0D, Math.cos(leaderYawRad));
			Vec3 leaderRight = new Vec3(Math.cos(leaderYawRad), 0.0D, Math.sin(leaderYawRad));

			targetPos = this.aiLeader.position()
				.add(leaderRight.scale(ox))
				.add(0.0D, oy, 0.0D)
				.add(leaderForward.scale(oz));

			// Keep formation above terrain
			int slotGround = world.getHeight(
				Heightmap.Types.MOTION_BLOCKING,
				Mth.floor(targetPos.x),
				Mth.floor(targetPos.z)
			);
			if (targetPos.y < slotGround + AI_MIN_CLEARANCE) {
				targetPos = new Vec3(targetPos.x, slotGround + AI_MIN_CLEARANCE + 4.0D, targetPos.z);
			}

			double distToTarget = pos.distanceTo(targetPos);
			targetThrottle = Math.max(0.55F, this.aiLeader.getThrottle());
			if (distToTarget > 10.0D) {
				targetThrottle = Math.min(1.0F, targetThrottle + 0.25F);
			} else if (distToTarget < 3.0D) {
				targetThrottle = Math.max(0.35F, targetThrottle - 0.15F);
			}
		} else {
			// Solo patrol: climb to cruise AGL, then figure-8
			if (this.aiPatrolCenter == null) {
				this.aiPatrolCenter = new Vec3(this.getX(), groundHere + AI_CRUISE_AGL, this.getZ());
				this.aiPatrolPhase = 0.0;
				this.aiWaypointTicks = 0;
			}

			if (++this.aiWaypointTicks > 900) {
				this.aiWaypointTicks = 0;
				double cx = this.aiPatrolCenter.x + (this.random.nextDouble() - 0.5D) * 90.0D;
				double cz = this.aiPatrolCenter.z + (this.random.nextDouble() - 0.5D) * 90.0D;
				int cg = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) cx, (int) cz);
				this.aiPatrolCenter = new Vec3(cx, cg + AI_CRUISE_AGL, cz);
				this.aiPatrolPhase = this.random.nextDouble() * 6.2832;
			}

			// Keep patrol altitude above local high ground
			int patrolGround = world.getHeight(
				Heightmap.Types.MOTION_BLOCKING,
				Mth.floor(this.aiPatrolCenter.x),
				Mth.floor(this.aiPatrolCenter.z)
			);
			double cruiseY = Math.max(this.aiPatrolCenter.y, patrolGround + AI_CRUISE_AGL);
			this.aiPatrolCenter = new Vec3(this.aiPatrolCenter.x, cruiseY, this.aiPatrolCenter.z);

			this.aiPatrolPhase += 0.026;
			if (this.aiPatrolPhase > 12.566) {
				this.aiPatrolPhase -= 12.566;
			}

			double t = this.aiPatrolPhase;
			double a = 60.0;
			double s = Math.sin(t);
			double c = Math.cos(t);
			double d = 1.0 + s * s;
			double xOff = a * c / d;
			double zOff = a * s * c / d;

			boolean needsClimb = agl < AI_CRUISE_AGL - 6.0D || this.getY() < cruiseY - 6.0D;
			double tgtY = needsClimb ? cruiseY + 8.0D : cruiseY;
			targetPos = new Vec3(this.aiPatrolCenter.x + xOff, tgtY, this.aiPatrolCenter.z + zOff);
			targetThrottle = needsClimb ? 1.0F : 0.85F;
		}

		// ── Obstacle avoidance: raise target, climb, and/or turn ────────
		double safeY = Math.max(targetPos.y, scan.maxTerrainAhead + AI_MIN_CLEARANCE + 6.0D);
		if (scan.imminentCollision || agl < AI_HARD_CLEARANCE || scan.minClearanceAhead < AI_HARD_CLEARANCE) {
			safeY = Math.max(safeY, scan.maxTerrainAhead + AI_MIN_CLEARANCE + 16.0D);
			targetThrottle = 1.0F;
		}
		targetPos = new Vec3(targetPos.x, safeY, targetPos.z);

		// Turn toward freer side when blocked ahead
		if (scan.imminentCollision || scan.minClearanceAhead < AI_MIN_CLEARANCE) {
			if (scan.leftClearance > scan.rightClearance + 2.0D) {
				desiredYawRateCmd = -35.0F; // bank left (negative relative yaw → left)
			} else if (scan.rightClearance > scan.leftClearance + 2.0D) {
				desiredYawRateCmd = 35.0F;
			} else {
				// Both bad — pick random side once per second-ish
				desiredYawRateCmd = (this.tickCount / 40) % 2 == 0 ? -40.0F : 40.0F;
			}
			targetThrottle = 1.0F;
		}

		// Desired absolute pitch from altitude error + obstacles
		double altError = targetPos.y - this.getY();
		if (scan.imminentCollision || scan.minClearanceAhead < AI_HARD_CLEARANCE || agl < AI_HARD_CLEARANCE) {
			desiredPitch = AI_STEEP_CLIMB_PITCH;
			targetThrottle = 1.0F;
		} else if (altError > 8.0D || agl < AI_MIN_CLEARANCE || scan.minClearanceAhead < AI_MIN_CLEARANCE) {
			// Strong climb with meaningful vertical speed (~0.15–0.25 b/t at cruise speed)
			desiredPitch = AI_CLIMB_PITCH;
			targetThrottle = Math.max(targetThrottle, 0.95F);
		} else if (altError > 2.0D) {
			desiredPitch = Mth.clamp((float) (-8.0D - altError * 0.8D), AI_STEEP_CLIMB_PITCH, -6.0F);
			targetThrottle = Math.max(targetThrottle, 0.9F);
		} else if (altError < -10.0D && agl > AI_CRUISE_AGL) {
			// Gentle descent only with plenty of clearance
			desiredPitch = Mth.clamp((float) (4.0D - altError * 0.15D), 2.0F, 12.0F);
		} else {
			// Path pitch toward target, but never dive into terrain
			Vec3 dir = targetPos.subtract(pos);
			double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
			float pathPitch = horizontalDist > 0.5D
				? (float) Math.toDegrees(Math.atan2(-dir.y, horizontalDist))
				: AI_CRUISE_PITCH;
			// pathPitch uses atan2(-dy,h): positive dy (target above) → negative pitch (nose up). Good.
			desiredPitch = Mth.clamp(pathPitch, AI_CLIMB_PITCH, 8.0F);
			if (desiredPitch > 0.0F && (agl < AI_MIN_CLEARANCE + 8.0D || scan.minClearanceAhead < AI_MIN_CLEARANCE + 4.0D)) {
				desiredPitch = AI_CRUISE_PITCH;
			}
		}

		// Yaw toward target when not hard-avoiding
		Vec3 toTarget = targetPos.subtract(pos);
		double targetYaw = Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
		float yawDiff = Mth.wrapDegrees((float) targetYaw - this.getYRot());
		if (desiredYawRateCmd == 0.0F) {
			this.aiTargetRelativeYaw = Mth.clamp(yawDiff, -40.0F, 40.0F);
		} else {
			this.aiTargetRelativeYaw = desiredYawRateCmd;
		}

		this.aiDesiredPitch = desiredPitch;
		// Keep legacy field for any external readers
		this.aiTargetRelativePitch = desiredPitch;

		// ── Takeoff: accelerate on ground, then rotate and climb ────────
		boolean grounded = this.resolveGroundHandling(this.onGround(), currentThrottle, currentVelocity);
		boolean nearGround = grounded || agl < 2.5D;

		if (nearGround && agl < 8.0D) {
			this.setThrottle(1.0F);
			this.setRudder(Mth.clamp(this.aiTargetRelativeYaw / 40.0F, -1.0F, 1.0F));

			if (speedHoriz < AI_TAKEOFF_SPEED && grounded) {
				// Ground roll — stay level, build speed; allow light steering
				this.aiDesiredPitch = 0.0F;
				this.aiForceAirborne = false;
				this.tickPilotedPhysics(null, 1.0F, this.aiTargetRelativeYaw * 0.35F, 0.0F);
			} else {
				// Rotation / initial climb: force airborne physics so pitch is not zeroed
				this.aiDesiredPitch = AI_STEEP_CLIMB_PITCH;
				this.aiForceAirborne = true;
				this.setOnGround(false);
				this.groundContactGraceTicks = 0;
				this.tickPilotedPhysics(null, 1.0F, this.aiTargetRelativeYaw * 0.5F, 0.0F);
			}
			return;
		}

		this.setThrottle(targetThrottle);
		this.setRudder(0.0F);
		this.tickPilotedPhysics(null, targetThrottle, this.aiTargetRelativeYaw, 0.0F);
	}

	private boolean beginRunwayReturn(ServerLevel world) {
		BlockPos located = world.findNearestMapStructure(
			PeterwolfsPlanesMod.RUNWAY_STRUCTURES_TAG,
			this.blockPosition(),
			AI_RUNWAY_SEARCH_RADIUS_CHUNKS,
			false
		);
		if (located == null) {
			return false;
		}

		Registry<Structure> structures = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Structure runway = structures.getValue(PeterwolfsPlanesMod.RUNWAY_STRUCTURE_KEY);
		if (runway == null) {
			return false;
		}

		// The locate result points into the runway's start chunk. Load that one
		// chunk so an already-generated structure can expose its rotated bounding
		// box; the long axis then gives us the real landing direction.
		StructureStart start = world.structureManager().getStartForStructure(
			SectionPos.of(located),
			runway,
			world.getChunk(located.getX() >> 4, located.getZ() >> 4)
		);
		if (start == null || !start.isValid()) {
			start = world.structureManager().startsForStructure(SectionPos.of(located), runway).stream()
				.filter(StructureStart::isValid)
				.min((a, b) -> Double.compare(
					a.getBoundingBox().getCenter().distSqr(located),
					b.getBoundingBox().getCenter().distSqr(located)
				))
				.orElse(null);
		}
		if (start == null || !start.isValid()) {
			return false;
		}

		BoundingBox box = start.getBoundingBox();
		double centerX = (box.minX() + box.maxX() + 1) * 0.5D;
		double centerZ = (box.minZ() + box.maxZ() + 1) * 0.5D;
		int surfaceY = world.getHeight(
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			Mth.floor(centerX),
			Mth.floor(centerZ)
		);
		Vec3 center = new Vec3(centerX, surfaceY + 0.05D, centerZ);
		boolean runwayAlongX = box.getXSpan() >= box.getZSpan();
		Vec3 axis = runwayAlongX ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 0.0D, 1.0D);
		double halfLength = Math.max(8.0D, (runwayAlongX ? box.getXSpan() : box.getZSpan()) * 0.5D);
		double approachDistance = halfLength + AI_APPROACH_EXTRA_DISTANCE;
		Vec3 approachForPositiveAxis = center.subtract(axis.scale(approachDistance));
		Vec3 approachForNegativeAxis = center.add(axis.scale(approachDistance));
		Vec3 direction = this.horizontalDistanceSqr(this.position(), approachForPositiveAxis)
			<= this.horizontalDistanceSqr(this.position(), approachForNegativeAxis)
			? axis
			: axis.scale(-1.0D);

		this.aiLeader = null;
		this.aiRunwayCenter = center;
		this.aiRunwayDirection = direction;
		this.aiRunwayHalfLength = halfLength;
		this.aiLandingPhase = AI_LANDING_APPROACH;
		this.aiLandingPhaseTicks = 0;
		return true;
	}

	private boolean tickRunwayLanding(int groundHere, double agl) {
		if (this.aiRunwayCenter == null || this.aiRunwayDirection == null) {
			this.aiLandingPhase = AI_LANDING_PATROL;
			this.aiRunwaySearchCooldown = AI_RUNWAY_SEARCH_RETRY_TICKS;
			return false;
		}

		this.aiLandingPhaseTicks++;
		Vec3 direction = this.aiRunwayDirection;
		float runwayYaw = this.yawToward(direction);

		if (this.aiLandingPhase == AI_LANDING_PARKED) {
			// If not on actual ground/surface or height above terrain is significant,
			// abort parked mode and resume patrol flight to prevent mid-air hovering.
			if (!this.onGround() && !this.hasSurfaceSupport() && agl > 1.5D) {
				this.aiLandingPhase = AI_LANDING_PATROL;
				this.aiReturnCountdown = 20 * 60; // 1 min before retry
				return false;
			}
			this.enginePower = Mth.approach(this.enginePower, 0.0F, ENGINE_RESPONSE * 2.0F);
			this.setThrottle(0.0F);
			this.setRudder(0.0F);
			this.setRoll(Mth.approachDegrees(this.getRoll(), 0.0F, 4.0F));
			this.setXRot(Mth.approach(this.getXRot(), 0.0F, 3.0F));
			this.setDeltaMovement(Vec3.ZERO);
			return true;
		}

		if (this.aiLandingPhase == AI_LANDING_ROLLOUT
			|| (this.aiLandingPhase == AI_LANDING_FINAL && this.onGround() && Math.abs(this.getY() - this.aiRunwayCenter.y) <= 3.0D)) {
			this.aiLandingPhase = AI_LANDING_ROLLOUT;
			this.aiDesiredPitch = 0.0F;
			float yawError = Mth.wrapDegrees(runwayYaw - this.getYRot());
			this.setThrottle(-1.0F);
			this.setRudder(Mth.clamp(yawError / 25.0F, -0.55F, 0.55F));
			this.tickPilotedPhysics(null, -1.0F, Mth.clamp(yawError, -12.0F, 12.0F), 0.0F);
			
			boolean onSolidRunwaySurface = (this.onGround() || this.hasSurfaceSupport())
				&& agl <= 1.25D
				&& Math.abs(this.getY() - this.aiRunwayCenter.y) <= 3.0D;
			
			if (onSolidRunwaySurface && this.getDeltaMovement().horizontalDistance() <= AI_PARK_SPEED) {
				this.aiLandingPhase = AI_LANDING_PARKED;
				this.aiLandingPhaseTicks = 0;
				this.setThrottle(0.0F);
				this.setRudder(0.0F);
				this.setRoll(0.0F);
				this.setXRot(0.0F);
				this.setDeltaMovement(Vec3.ZERO);
			} else if (!onSolidRunwaySurface && this.aiLandingPhaseTicks > 20 * 12) {
				// Abort false rollout in mid-air and go around for a fresh approach
				this.aiRunwayDirection = direction.scale(-1.0D);
				this.aiLandingPhase = AI_LANDING_APPROACH;
				this.aiLandingPhaseTicks = 0;
			}
			return true;
		}

		if (this.aiLandingPhase == AI_LANDING_APPROACH) {
			double approachDistance = this.aiRunwayHalfLength + AI_APPROACH_EXTRA_DISTANCE;
			Vec3 approach = this.aiRunwayCenter.subtract(direction.scale(approachDistance));
			int approachGround = this.level().getHeight(
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Mth.floor(approach.x),
				Mth.floor(approach.z)
			);
			double targetY = Math.max(
				approachGround + AI_APPROACH_CLEARANCE,
				this.aiRunwayCenter.y + AI_APPROACH_CLEARANCE
			);
			float yawRad = (float) Math.toRadians(this.getYRot());
			ObstacleScan scan = this.scanObstaclesAhead(this.level(), -Math.sin(yawRad), Math.cos(yawRad));
			targetY = Math.max(targetY, scan.maxTerrainAhead + AI_MIN_CLEARANCE + 6.0D);
			Vec3 target = new Vec3(approach.x, targetY, approach.z);
			if (this.onGround() && this.getDeltaMovement().horizontalDistance() >= AI_TAKEOFF_SPEED) {
				this.aiForceAirborne = true;
				this.setOnGround(false);
				this.groundContactGraceTicks = 0;
			}
			this.flyNpcToward(target, scan.imminentCollision ? 1.0F : 0.82F, -28.0F, 8.0F, 35.0F);

			if (this.horizontalDistanceSqr(this.position(), target) <= AI_LANDING_ALIGNMENT_DISTANCE * AI_LANDING_ALIGNMENT_DISTANCE
				&& Math.abs(this.getY() - targetY) <= 12.0D) {
				this.aiLandingPhase = AI_LANDING_LINEUP;
				this.aiLandingPhaseTicks = 0;
			}
			return true;
		}

		if (this.aiLandingPhase == AI_LANDING_LINEUP) {
			Vec3 lineupPoint = this.aiRunwayCenter.subtract(direction.scale(this.aiRunwayHalfLength + 18.0D));
			Vec3 target = new Vec3(
				lineupPoint.x,
				this.aiRunwayCenter.y + AI_LINEUP_CLEARANCE,
				lineupPoint.z
			);
			this.flyNpcToward(target, 0.64F, -12.0F, 12.0F, 22.0F);
			float lineupYawError = Math.abs(Mth.wrapDegrees(runwayYaw - this.getYRot()));
			if (this.horizontalDistanceSqr(this.position(), target) <= AI_LANDING_ALIGNMENT_DISTANCE * AI_LANDING_ALIGNMENT_DISTANCE
				&& lineupYawError <= 14.0F) {
				this.aiLandingPhase = AI_LANDING_FINAL;
				this.aiLandingPhaseTicks = 0;
			} else if (this.aiLandingPhaseTicks > 20 * 45) {
				this.aiRunwayDirection = direction.scale(-1.0D);
				this.aiLandingPhase = AI_LANDING_APPROACH;
				this.aiLandingPhaseTicks = 0;
			}
			return true;
		}

		// Final approach: point the nose at the far end, but compute descent
		// against a touchdown point in the first third of the strip. This keeps
		// the aircraft lined up along the runway instead of homing on its center.
		Vec3 touchdown = this.aiRunwayCenter.subtract(direction.scale(this.aiRunwayHalfLength * 0.55D));
		double heightAboveRunway = this.getY() - this.aiRunwayCenter.y;
		double alongRunway = this.position().subtract(this.aiRunwayCenter).dot(direction);
		if ((alongRunway > this.aiRunwayHalfLength + 18.0D && heightAboveRunway > 1.0D)
			|| this.aiLandingPhaseTicks > 20 * 60) {
			// Missed or unstable approach: use the nearer end for a fresh circuit
			// instead of diving beyond the generated strip.
			this.aiRunwayDirection = direction.scale(-1.0D);
			this.aiLandingPhase = AI_LANDING_APPROACH;
			this.aiLandingPhaseTicks = 0;
			return true;
		}
		double distanceToTouchdown = Math.sqrt(this.horizontalDistanceSqr(this.position(), touchdown));
		float desiredPitch = (float) Math.toDegrees(Math.atan2(
			Math.max(0.0D, heightAboveRunway),
			Math.max(18.0D, distanceToTouchdown)
		));
		desiredPitch = Mth.clamp(desiredPitch, -2.0F, 11.0F);
		if (heightAboveRunway < 3.0D) {
			desiredPitch = Mth.clamp(desiredPitch, 1.5F, 4.0F);
		}

		float yawError = Mth.wrapDegrees(runwayYaw - this.getYRot());
		this.aiDesiredPitch = desiredPitch;
		this.aiTargetRelativePitch = desiredPitch;
		this.aiTargetRelativeYaw = Mth.clamp(yawError, -18.0F, 18.0F);
		float landingThrottle = distanceToTouchdown > 55.0D ? 0.56F : (heightAboveRunway > 5.0D ? 0.46F : 0.34F);
		this.setThrottle(landingThrottle);
		this.setRudder(Mth.clamp(yawError / 35.0F, -0.35F, 0.35F));
		this.tickPilotedPhysics(null, landingThrottle, this.aiTargetRelativeYaw, 0.0F);
		double runwayYDiff = Math.abs(this.getY() - this.aiRunwayCenter.y);
		boolean nearRunwayAltitude = runwayYDiff <= 3.0D;
		if ((this.onGround() && nearRunwayAltitude) || (agl < 1.0D && nearRunwayAltitude)) {
			this.aiLandingPhase = AI_LANDING_ROLLOUT;
			this.aiLandingPhaseTicks = 0;
		}
		return true;
	}

	private void flyNpcToward(Vec3 target, float throttle, float minPitch, float maxPitch, float maxYawCommand) {
		Vec3 delta = target.subtract(this.position());
		double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float targetYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
		float yawError = Mth.wrapDegrees(targetYaw - this.getYRot());
		float targetPitch = horizontalDistance > 0.5D
			? (float) Math.toDegrees(Math.atan2(-delta.y, horizontalDistance))
			: 0.0F;
		this.aiDesiredPitch = Mth.clamp(targetPitch, minPitch, maxPitch);
		this.aiTargetRelativePitch = this.aiDesiredPitch;
		this.aiTargetRelativeYaw = Mth.clamp(yawError, -maxYawCommand, maxYawCommand);
		this.setThrottle(throttle);
		this.setRudder(0.0F);
		this.tickPilotedPhysics(null, throttle, this.aiTargetRelativeYaw, 0.0F);
	}

	private float yawToward(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	private double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double dx = first.x - second.x;
		double dz = first.z - second.z;
		return dx * dx + dz * dz;
	}

	/**
	 * Forward / side terrain + solid-block scan for NPC pilots.
	 */
	private ObstacleScan scanObstaclesAhead(Level world, double hx, double hz) {
		ObstacleScan scan = new ObstacleScan();
		double px = this.getX();
		double py = this.getY();
		double pz = this.getZ();
		double rightX = hz;
		double rightZ = -hx;

		double minClear = Double.MAX_VALUE;
		double maxTerrain = Double.NEGATIVE_INFINITY;
		boolean imminent = false;

		int[] distances = { 6, 12, 18, 24, 32 };
		for (int dist : distances) {
			int ax = Mth.floor(px + hx * dist);
			int az = Mth.floor(pz + hz * dist);
			int terrain = world.getHeight(Heightmap.Types.MOTION_BLOCKING, ax, az);
			maxTerrain = Math.max(maxTerrain, terrain);
			double clear = py - terrain;
			minClear = Math.min(minClear, clear);

			// Solid blocks at flight altitude (trees, buildings, cliffs)
			int sampleY = Mth.floor(py);
			if (world.getBlockState(new BlockPos(ax, sampleY, az)).isSolidRender()
				|| world.getBlockState(new BlockPos(ax, sampleY + 1, az)).isSolidRender()
				|| world.getBlockState(new BlockPos(ax, sampleY - 1, az)).isSolidRender()) {
				imminent = true;
				minClear = Math.min(minClear, -2.0D);
			}

			if (dist <= 12 && clear < AI_HARD_CLEARANCE) {
				imminent = true;
			}
		}

		// Side freeness for turn choice
		double leftClear = 0.0D;
		double rightClear = 0.0D;
		for (int dist = 10; dist <= 22; dist += 6) {
			int lx = Mth.floor(px + hx * dist - rightX * 10.0D);
			int lz = Mth.floor(pz + hz * dist - rightZ * 10.0D);
			int rx = Mth.floor(px + hx * dist + rightX * 10.0D);
			int rz = Mth.floor(pz + hz * dist + rightZ * 10.0D);
			leftClear += py - world.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
			rightClear += py - world.getHeight(Heightmap.Types.MOTION_BLOCKING, rx, rz);
		}

		scan.minClearanceAhead = minClear == Double.MAX_VALUE ? 99.0D : minClear;
		scan.maxTerrainAhead = maxTerrain == Double.NEGATIVE_INFINITY ? py - 20.0D : maxTerrain;
		scan.leftClearance = leftClear;
		scan.rightClearance = rightClear;
		scan.imminentCollision = imminent;
		return scan;
	}

	private static final class ObstacleScan {
		double minClearanceAhead = 99.0D;
		double maxTerrainAhead = 0.0D;
		double leftClearance = 0.0D;
		double rightClearance = 0.0D;
		boolean imminentCollision = false;
	}

	protected double getThrustForce() { return THRUST_FORCE; }
	protected double getBaseDrag() { return BASE_DRAG; }
	protected double getSpeedDrag() { return SPEED_DRAG; }
	protected double getAirflowForFullControl() { return AIRFLOW_FOR_FULL_CONTROL; }
	protected float getMaxBank() { return MAX_BANK; }
	protected float getMaxYawRate() { return MAX_YAW_RATE; }
	protected float getMaxPitchRate() { return MAX_PITCH_RATE; }

	/**
	 * Cargo mass relative to empty airframe. 1.0 = empty.
	 * Specialized planes may implement {@link com.piotrek.peterwolfsplanes.api.PlaneCargoMass}.
	 */
	protected float getCargoMassFactor() {
		if (this instanceof com.piotrek.peterwolfsplanes.api.PlaneCargoMass cargo) {
			float factor = cargo.getCargoMassFactor();
			if (!Float.isFinite(factor)) {
				return 1.0F;
			}
			return Mth.clamp(factor, 0.5F, 3.0F);
		}
		return 1.0F;
	}

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

	public boolean isHelicopter() {
		return false;
	}

	private void tickPilotedPhysics(@Nullable Player pilot, float currentThrottle, float aiRelativeYaw, float aiRelativePitch) {
		Vec3 currentVelocity = this.getDeltaMovement();
		boolean grounded = this.resolveGroundHandling(this.onGround(), currentThrottle, currentVelocity);
		if (this.isHelicopter()) {
			float relYaw = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - this.getYRot()), -80.0F, 80.0F) : aiRelativeYaw;
			float relPitch = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - this.getXRot()), -80.0F, 80.0F) : aiRelativePitch;
			this.tickHelicopterFlight(pilot, currentThrottle, grounded, currentVelocity, relYaw, relPitch);
			return;
		}
		// Floats can support an aircraft without turning it into a ground-locked
		// vehicle. Keep mouse flight controls active, but do not apply airborne
		// stall/sink forces while the airframe is resting on that surface.
		boolean surfaceSupported = grounded || this.hasSurfaceSupport();
		// NPC takeoff rotation: leave ground handling so pitch-up is not zeroed
		if (pilot == null && this.aiForceAirborne) {
			grounded = false;
			surfaceSupported = false;
			this.groundContactGraceTicks = 0;
		}

		boolean npc = pilot == null;
		Vec3 heading = grounded ? this.calculateGroundHeading(this.getYRot()) : this.calculateHeading(this.getYRot(), this.getXRot());
		double speed = Math.max(0.0D, currentVelocity.dot(heading));
		float relativeYaw = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - this.getYRot()), -80.0F, 80.0F) : aiRelativeYaw;
		float relativePitch = pilot != null ? Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - this.getXRot()), -80.0F, 80.0F) : aiRelativePitch;
		float controlEfficiency = Mth.clamp((float) (speed / getAirflowForFullControl()), 0.0F, 1.0F);
		// NPCs need control authority during takeoff even if airflow is low
		if (npc) {
			controlEfficiency = Math.max(controlEfficiency, 0.55F);
		}

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

			// NPC ground steering via rudder + slight yaw from relative yaw command
			float groundSteer = this.getRudder();
			if (npc && Math.abs(relativeYaw) > 1.0F) {
				groundSteer = Mth.clamp(relativeYaw / 40.0F, -1.0F, 1.0F);
			}
			targetYawRate = groundSteer * GROUND_YAW_RATE;
		} else {
			this.smoothedRelativeYaw += (relativeYaw - this.smoothedRelativeYaw) * INPUT_SMOOTHING;
			this.smoothedRelativePitch += (relativePitch - this.smoothedRelativePitch) * INPUT_SMOOTHING;

			float surfaceRollAuthority = surfaceSupported && !grounded ? controlEfficiency : 1.0F;
			float targetRoll = Mth.clamp(
				(this.smoothedRelativeYaw * 0.75F + this.getRudder() * 20.0F) * surfaceRollAuthority,
				-getMaxBank(),
				getMaxBank()
			);
			float rollStep = 0.7F + 2.6F * controlEfficiency;
			this.setRoll(Mth.approachDegrees(this.getRoll(), targetRoll, rollStep));

			float targetPitchRate;
			if (npc) {
				// Absolute pitch command (aiDesiredPitch): negative = climb with real vertical speed
				float pitchError = this.aiDesiredPitch - this.getXRot();
				targetPitchRate = Mth.clamp(pitchError * AI_PITCH_GAIN, -AI_MAX_PITCH_RATE, AI_MAX_PITCH_RATE);
				// Reduce auto-level fight while AI is climbing
				if (this.aiDesiredPitch < -5.0F) {
					// no leveling pull toward 0
				} else {
					targetPitchRate += -this.getXRot() * 0.02F * controlEfficiency;
				}
			} else {
				float pitchLeveling = -this.getXRot() * (0.012F + 0.018F * controlEfficiency);
				targetPitchRate = Mth.clamp(
					-this.smoothedRelativePitch * 0.025F * controlEfficiency + pitchLeveling,
					-getMaxPitchRate(),
					getMaxPitchRate()
				);
			}
			float pitchApproach = npc ? 0.28F : 0.16F;
			float maxRate = npc ? AI_MAX_PITCH_RATE : getMaxPitchRate();
			this.pitchRate = Mth.approach(this.pitchRate, targetPitchRate, pitchApproach);
			this.pitchRate = Mth.clamp(this.pitchRate, -maxRate, maxRate);
			this.setXRot(Mth.clamp(this.getXRot() + this.pitchRate, -75.0F, 60.0F));

			float bankTurn = (float) Math.sin(Math.toRadians(this.getRoll())) * (1.45F * controlEfficiency);
			float rudderTurn = this.getRudder() * (0.35F + 1.15F * controlEfficiency);
			float coordinatedTurn = this.smoothedRelativeYaw * 0.006F * controlEfficiency;
			// NPCs: stronger yaw from bank/command so they can dodge obstacles
			if (npc) {
				bankTurn *= 1.25F;
				coordinatedTurn = this.smoothedRelativeYaw * 0.02F * controlEfficiency;
			}
			targetYawRate = Mth.clamp(bankTurn + rudderTurn + coordinatedTurn, -getMaxYawRate(), getMaxYawRate());
		}

		float yawResponse = grounded ? GROUND_YAW_RESPONSE : (npc ? 0.32F : 0.22F);
		this.yawRate = Mth.clamp(Mth.approach(this.yawRate, targetYawRate, yawResponse), -getMaxYawRate(), getMaxYawRate());
		this.setPlaneYaw(this.getYRot() + this.yawRate);

		heading = grounded ? this.calculateGroundHeading(this.getYRot()) : this.calculateHeading(this.getYRot(), this.getXRot());
		speed = this.applyForces(speed, heading, currentThrottle, surfaceSupported);

		// Anti-stall and minimum airspeed protection for NPC aircraft in flight
		boolean inAirborneNpcFlight = npc && !surfaceSupported
			&& this.aiLandingPhase != AI_LANDING_ROLLOUT
			&& this.aiLandingPhase != AI_LANDING_PARKED;

		if (inAirborneNpcFlight) {
			if (speed < AI_MIN_FLIGHT_AIRSPEED) {
				speed = AI_MIN_FLIGHT_AIRSPEED;
			}
			if (this.enginePower < 0.65F) {
				this.enginePower = 0.65F;
			}
		}

		if (grounded && this.hasGroundIdlePower(currentThrottle) && speed < GROUND_IDLE_HOLD_SPEED) {
			speed = 0.0D;
		}

		double sink = 0.0D;
		if (!surfaceSupported) {
			double liftY = this.calculateLiftY(speed);
			double liftDeficit = liftY - GRAVITY;
			if (liftDeficit < 0.0D) {
				// NPCs climbing hard: less stall nose-down punishment so they keep ascending
				float stallPitch = (float) Mth.clamp(-liftDeficit * (npc && this.aiDesiredPitch < -8.0F ? 12.0D : 34.0D), 0.0D, 2.2D);
				this.setXRot(Mth.clamp(this.getXRot() + stallPitch, -75.0F, 60.0F));
				sink = Mth.clamp(liftDeficit * (npc ? 0.50D : 1.45D), -0.10D, 0.0D);
				heading = this.calculateHeading(this.getYRot(), this.getXRot());
			}
		}

		Vec3 movement = heading.scale(speed).add(0.0D, sink, 0.0D);

		// Guarantee positive forward movement and minimum climb rate for NPC in flight
		if (inAirborneNpcFlight) {
			if (movement.horizontalDistance() < AI_MIN_FORWARD_VELOCITY) {
				float yawRad = (float) Math.toRadians(this.getYRot());
				Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
				movement = new Vec3(forward.x * AI_MIN_FORWARD_VELOCITY, movement.y, forward.z * AI_MIN_FORWARD_VELOCITY);
			}
			if (this.aiDesiredPitch <= -10.0F) {
				double minClimb = this.aiDesiredPitch <= -24.0F ? 0.22D : 0.14D;
				if (movement.y < minClimb) {
					movement = new Vec3(movement.x, minClimb, movement.z);
				}
			}
		}

		if (grounded && speed == 0.0D && this.hasGroundIdlePower(currentThrottle)) {
			movement = Vec3.ZERO;
		}
		boolean airborneBeforeMove = !this.onGround() && !this.hasSurfaceSupport();
		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);
		this.evaluateLandingAfterMove(movement, airborneBeforeMove);
		if (grounded && speed == 0.0D) {
			this.setDeltaMovement(Vec3.ZERO);
		}

		this.aiForceAirborne = false;
	}

	private void centerPilotLook(Player pilot, float relativeYaw, float relativePitch) {
		float yawDelta = -relativeYaw * PILOT_LOOK_CENTERING;
		float pitchDelta = -relativePitch * PILOT_LOOK_CENTERING;

		pilot.setYRot(pilot.getYRot() + yawDelta);
		pilot.yRotO += yawDelta;
		pilot.setXRot(pilot.getXRot() + pitchDelta);
		pilot.xRotO += pitchDelta;
	}

	private double applyForces(double speed, Vec3 heading, float currentThrottle, boolean surfaceSupported) {
		double mass = Math.max(0.5D, this.getCargoMassFactor());
		double thrustForce = this.enginePower * getThrustForce() / mass;
		double gravityAssist = -GRAVITY * heading.y * 0.85D * mass;
		double dragForce = speed * (getBaseDrag() * mass + Math.min(speed, 1.6D) * getSpeedDrag() * Math.sqrt(mass));
		speed = Math.max(0.0D, speed + thrustForce + gravityAssist - dragForce);

		if (surfaceSupported) {
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

	protected Vec3 calculateHeading(float yaw, float pitch) {
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
			return this.getEmptyGroundVisualPitch();
		}
		return this.getXRot();
	}

	/**
	 * Empty parked attitude. Conventional two-wheel aircraft rest tail-low;
	 * subclasses with level landing gear or floats can override this.
	 */
	protected float getEmptyGroundVisualPitch() {
		return EMPTY_GROUND_VISUAL_PITCH;
	}

	/**
	 * Non-solid support such as floats resting on water. It suppresses airborne
	 * stall/sink behavior and enables braking drag without disabling flight
	 * controls as the normal grounded state does.
	 */
	protected boolean hasSurfaceSupport() {
		return false;
	}

	private boolean shouldUseEmptyGroundVisualPose() {
		if (this.getFirstPassenger() != null) {
			this.emptyGroundPoseTicks = 0;
			return false;
		}

		Vec3 velocity = this.getDeltaMovement();
		boolean settled = this.isLowGroundIdleSpeed(velocity);
		if ((this.onGround() || this.hasSurfaceSupport()) && settled) {
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

	private void tickHelicopterFlight(@Nullable Player pilot, float currentThrottle, boolean grounded, Vec3 currentVelocity, float relativeYaw, float relativePitch) {
		float targetEnginePower;
		if (this.isSimpleAltitudeControl()) {
			targetEnginePower = !this.isHelicopterAirborne() && currentThrottle <= 0.05F ? 0.0F : 0.85F;
		} else {
			targetEnginePower = Math.max(0.0F, currentThrottle);
		}
		this.enginePower = Mth.approach(this.enginePower, targetEnginePower, ENGINE_RESPONSE);

		if (!this.isHelicopterAirborne()) {
			// If ground fell away beneath the helicopter, become airborne immediately
			if (!this.isHelicopterTouchingGround()) {
				this.setHelicopterAirborne(true);
				this.zeroPilotCursor(pilot);
				return;
			}

			// HELICOPTER ON GROUND: all flight steering is locked (cyclic + tail rotor).
			this.heldHelicopterPitch = 0.0F;
			this.smoothedRelativeYaw = 0.0F;
			this.smoothedRelativePitch = 0.0F;
			this.pitchRate = 0.0F;
			this.yawRate = 0.0F;
			this.setRudder(0.0F);
			this.setRoll(0.0F);
			this.setXRot(0.0F);

			// TAKEOFF / LIFTOFF:
			// Only after takeoff into the air does flight steering activate!
			boolean canLiftOff = this.isSimpleAltitudeControl()
				? currentThrottle > 0.05F
				: (currentThrottle > 0.35F && this.enginePower > 0.45F);

			if (canLiftOff) {
				double liftVy = this.isSimpleAltitudeControl()
					? Math.max(0.08D, currentThrottle * 0.45D)
					: (this.enginePower - 0.45F) * 0.45D;
				Vec3 liftMovement = new Vec3(0.0D, liftVy, 0.0D);
				this.setDeltaMovement(liftMovement);
				this.move(MoverType.SELF, liftMovement);

				if (!this.isHelicopterTouchingGround()) {
					this.setHelicopterAirborne(true);
					this.zeroPilotCursor(pilot);
				}
			} else {
				// Firmly pin to ground
				Vec3 groundPin = new Vec3(0.0D, -0.04D, 0.0D);
				this.setDeltaMovement(Vec3.ZERO);
				this.move(MoverType.SELF, groundPin);
				this.setDeltaMovement(Vec3.ZERO);
			}
			return;
		}

		if (pilot != null) {
			relativeYaw = Mth.clamp(Mth.wrapDegrees(pilot.getYRot() - this.getYRot()), -80.0F, 80.0F);
			relativePitch = Mth.clamp(Mth.wrapDegrees(pilot.getXRot() - this.getXRot()), -80.0F, 80.0F);
		}

		// Fast smoothing (0.60F) completely eliminates the 1-second delay between forward and backward movements
		float heliSmoothing = 0.60F;
		this.smoothedRelativeYaw += (relativeYaw - this.smoothedRelativeYaw) * heliSmoothing;
		this.smoothedRelativePitch += (relativePitch - this.smoothedRelativePitch) * heliSmoothing;

		// Cyclic Stick Pitch:
		// Mouse forward: relativePitch < 0 -> increases pitch (nose down -> forward thrust).
		// Mouse backward: relativePitch > 0 -> decreases pitch (nose up -> flare / reverse).
		// Pitch attitude is held for continuous cruise, but effortlessly stabilizable into hover!
		if (pilot != null) {
			// Stabilization Assist: when pilot looks near the horizon, auto-level to 0.0 for a rock-solid hover
			boolean aimingNearHorizon = Math.abs(pilot.getXRot()) < 4.0F;
			boolean stickNearCenter = Math.abs(relativePitch) < 4.5F;
			if (aimingNearHorizon && stickNearCenter) {
				this.heldHelicopterPitch = Mth.approach(this.heldHelicopterPitch, 0.0F, 0.75F);
			} else {
				float pitchInput = -relativePitch;
				if (Math.abs(pitchInput) < 0.8F) {
					pitchInput = 0.0F;
				}
				// Exponential curve: significantly reduced sensitivity near 0 for smooth hovering and micro-corrections
				float expoPitch = applyExpo(pitchInput, 32.0F, 1.85F);
				float pitchDelta = expoPitch * 0.045F;
				this.heldHelicopterPitch = Mth.clamp(this.heldHelicopterPitch + pitchDelta, -12.0F, 16.0F);

				// Snap to zero if near zero and centered
				if (Math.abs(this.heldHelicopterPitch) < 0.8F && Math.abs(pitchInput) < 2.0F) {
					this.heldHelicopterPitch = Mth.approach(this.heldHelicopterPitch, 0.0F, 0.25F);
				}
			}
			this.setXRot(Mth.approach(this.getXRot(), this.heldHelicopterPitch, 2.5F));
		} else {
			this.heldHelicopterPitch = Mth.approach(this.heldHelicopterPitch, 0.0F, 0.8F);
			this.setXRot(Mth.approach(this.getXRot(), 0.0F, 1.5F));
		}

		// Cyclic Stick Roll: swapped left and right per user request
		float targetRoll;
		if (pilot == null) {
			targetRoll = 0.0F;
		} else {
			// Exponential curve: lower sensitivity near 0 for smooth hovering and micro-corrections
			float expoYaw = applyExpo(this.smoothedRelativeYaw, 32.0F, 1.85F);
			targetRoll = Mth.clamp(expoYaw * 0.40F, -14.0F, 14.0F);
		}
		this.setRoll(Mth.approachDegrees(this.getRoll(), targetRoll, 2.5F));

		// Anti-Torque Tail Rotor: keys A and D rotate the tail ("obrót ogona")
		float tailRotorCommand = this.getRudder() * 4.2F;
		this.yawRate = Mth.approach(this.yawRate, tailRotorCommand, 0.35F);
		this.setPlaneYaw(this.getYRot() + this.yawRate);
		if (pilot != null && Math.abs(this.yawRate) > 0.001F) {
			pilot.setYRot(pilot.getYRot() + this.yawRate);
			pilot.setYHeadRot(pilot.getYHeadRot() + this.yawRate);
			pilot.setYBodyRot(pilot.yBodyRot + this.yawRate);
		}

		float yawRad = (float) Math.toRadians(this.getYRot());
		float pitchRad = (float) Math.toRadians(this.getXRot());
		float rollRad = (float) Math.toRadians(this.getRoll());

		Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
		Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

		double sinPitch = Math.sin(pitchRad);
		double sinRoll = Math.sin(rollRad);

		double newVx;
		double newVy;
		double newVz;
		double horizontalSpeed = currentVelocity.horizontalDistance();
		double horizDrag = 0.038D + 0.012D * horizontalSpeed;

		if (this.isSimpleAltitudeControl()) {
			// SIMPLE ALTITUDE CONTROL (Default for helicopters):
			// W = climb, S = descend, release W/S = auto-altitude hold (0 vertical speed).
			// Forward/backward steering (pitch) and sideways steering (roll) do NOT cause descending!
			double targetVy;
			if (pilot != null) {
				if (currentThrottle > 0.05F) {
					targetVy = currentThrottle * 0.45D;
				} else if (currentThrottle < -0.05F) {
					targetVy = currentThrottle * 0.35D;
				} else {
					targetVy = 0.0D;
				}
			} else {
				targetVy = -0.20D;
			}

			newVy = Mth.approach((float) currentVelocity.y, (float) targetVy, 0.05F);
			if (targetVy < 0.0D) {
				newVy = this.dampHelicopterDescent(newVy);
			}

			// Forward & lateral thrust with steady authority
			double horizThrustBase = 0.065D;
			Vec3 forwardThrust = forward.scale(sinPitch * horizThrustBase * 1.85D);
			Vec3 lateralThrust = left.scale(-sinRoll * horizThrustBase * 1.45D);

			newVx = currentVelocity.x * (1.0D - horizDrag) + forwardThrust.x + lateralThrust.x;
			newVz = currentVelocity.z * (1.0D - horizDrag) + forwardThrust.z + lateralThrust.z;
		} else {
			// REALISTIC AERODYNAMIC MODE:
			// Manual collective throttle + tilt redirects rotor thrust and induces translation sink
			double hoverThrust = GRAVITY; // 0.04D
			double maxThrust = 0.092D;
			double idleThrust = 0.012D; // gentle autorotation descent

			double totalRotorThrust;
			if (currentThrottle < 0.0F) {
				totalRotorThrust = Math.max(0.0D, idleThrust * (1.0F + currentThrottle));
			} else if (this.enginePower <= 0.5F) {
				float f = this.enginePower / 0.5F;
				totalRotorThrust = Mth.lerp(f, idleThrust, hoverThrust);
			} else {
				float f = (this.enginePower - 0.5F) / 0.5F;
				totalRotorThrust = Mth.lerp(f, hoverThrust, maxThrust);
			}

			double pitchTiltLoss = Math.abs(sinPitch) * 0.75D;
			double rollTiltLoss = Math.abs(sinRoll) * 0.75D;
			double tiltFactor = Math.max(0.15D, (1.0D - pitchTiltLoss) * (1.0D - rollTiltLoss));

			double translationSink = horizontalSpeed * 0.024D;
			double verticalLift = Math.max(0.0D, totalRotorThrust * tiltFactor - translationSink);

			Vec3 forwardThrust = forward.scale(sinPitch * totalRotorThrust * 1.85D);
			Vec3 lateralThrust = left.scale(-sinRoll * totalRotorThrust * 1.45D);

			double vertDrag = 0.03D;
			newVx = currentVelocity.x * (1.0D - horizDrag) + forwardThrust.x + lateralThrust.x;
			newVy = currentVelocity.y * (1.0D - vertDrag) + verticalLift - GRAVITY;
			newVz = currentVelocity.z * (1.0D - horizDrag) + forwardThrust.z + lateralThrust.z;
			if (newVy < 0.0D && currentThrottle < -0.05F) {
				newVy = this.dampHelicopterDescent(newVy);
			}
		}

		double hSpeedSqr = newVx * newVx + newVz * newVz;
		if (hSpeedSqr > 1.69D) {
			double scale = 1.3D / Math.sqrt(hSpeedSqr);
			newVx *= scale;
			newVz *= scale;
		}
		newVy = Mth.clamp(newVy, -0.75D, 0.75D);

		Vec3 movement = new Vec3(newVx, newVy, newVz);
		boolean airborneBeforeMove = !this.onGround();
		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);
		this.evaluateLandingAfterMove(movement, airborneBeforeMove);

		// TOUCHDOWN / LANDING:
		float groundCutoff = this.isSimpleAltitudeControl() ? 0.05F : 0.35F;
		boolean touchingGround = this.isHelicopterTouchingGround();
		if (touchingGround && movement.y <= 0.05D && currentThrottle <= groundCutoff) {
			this.setHelicopterAirborne(false);
			this.resetHelicopterControls();
			this.setDeltaMovement(Vec3.ZERO);
		}
	}

	private static float applyExpo(float input, float maxInputRange, float exponent) {
		if (Math.abs(input) < 1.0E-4F) {
			return 0.0F;
		}
		float norm = Mth.clamp(input / maxInputRange, -1.0F, 1.0F);
		float sign = Math.signum(norm);
		return sign * (float) Math.pow(Math.abs(norm), exponent) * maxInputRange;
	}

	private boolean resolveGroundHandling(boolean rawGrounded, float currentThrottle, Vec3 velocity) {
		if (this.isHelicopter()) {
			return !this.isHelicopterAirborne();
		}
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
		if (this.isHelicopter()) {
			return !this.isHelicopterAirborne();
		}
		// Show zeroed instruments whenever the plane is sitting still on the
		// ground — this includes the case where the brake (negative throttle)
		// is held, which was previously excluded by the hasGroundIdlePower
		// check and caused HUD speed to flicker with the residual deltaMovement.
		return this.getFirstPassenger() instanceof Player
			&& (this.onGround() || this.hasSurfaceSupport() || this.groundContactGraceTicks > 0)
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

	public float getYawRate() {
		return this.yawRate;
	}

	public float getRenderYawO() {
		return Float.isNaN(this.renderYawO) ? this.getYRot() : this.renderYawO;
	}

	@Override
	public float getYRot(float partialTick) {
		return this.getRenderYaw(partialTick);
	}

	@Override
	public float getViewYRot(float partialTick) {
		return this.getRenderYaw(partialTick);
	}

	@Override
	public float getXRot(float partialTick) {
		return this.getVisualPitch(partialTick);
	}

	@Override
	public float getViewXRot(float partialTick) {
		return this.getVisualPitch(partialTick);
	}

	/**
	 * Returns the yaw angle for rendering, properly interpolated between the
	 * pre-tick and post-tick values that our physics wrote this tick.
	 * This avoids the one-tick stale-yRotO shudder that occurs when we call
	 * setYRot() after super.tick() has already snapshotted yRotO.
	 */
	public float getRenderYaw(float partialTick) {
		if (Float.isNaN(this.renderYaw)) {
			return this.getYRot();
		}
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

		if (this.onGround() || this.hasSurfaceSupport()) {
			this.groundContactGraceTicks = GROUND_CONTACT_GRACE_TICKS;
			this.setDeltaMovement(Vec3.ZERO);
			this.setXRot(Mth.approach(this.getXRot(), 0.0F, 3.0F));
			if (this.isHelicopter()) {
				this.setHelicopterAirborne(false);
				this.resetHelicopterControls();
			}
		} else {
			Vec3 vel = this.getDeltaMovement().add(0.0D, -GRAVITY, 0.0D).scale(0.95D);
			boolean airborneBeforeMove = !this.onGround() && !this.hasSurfaceSupport();
			this.setDeltaMovement(vel);
			this.move(MoverType.SELF, vel);
			this.evaluateLandingAfterMove(vel, airborneBeforeMove);
			if (this.isHelicopter() && (this.onGround() || this.verticalCollisionBelow || this.hasSurfaceSupport())) {
				this.setHelicopterAirborne(false);
				this.resetHelicopterControls();
				this.setDeltaMovement(Vec3.ZERO);
			}
		}

		this.setThrottle(0.0F);
		this.setRoll(Mth.approachDegrees(this.getRoll(), 0.0F, 4.0F));
		this.setRudder(0.0F);
	}

	public void resetHelicopterControls() {
		this.heldHelicopterPitch = 0.0F;
		this.setXRot(0.0F);
		this.setRoll(0.0F);
		this.pitchRate = 0.0F;
		this.yawRate = 0.0F;
		this.smoothedRelativePitch = 0.0F;
		this.smoothedRelativeYaw = 0.0F;
		this.setRudder(0.0F);
		if (this.isHelicopterTouchingGround()) {
			this.setHelicopterAirborne(false);
		}
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (this.isHelicopter() && passenger instanceof Player) {
			this.resetHelicopterControls();
			if (!this.isHelicopterTouchingGround()) {
				this.setHelicopterAirborne(true);
			} else {
				this.setHelicopterAirborne(false);
			}
		}
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
		if (ReplayPlayback.isActive()) {
			return false;
		}
		return this.getFirstPassenger() instanceof Player;
	}

	@Override
	protected boolean isLocalClientAuthoritative() {
		if (ReplayPlayback.isActive()) {
			return false;
		}
		return super.isLocalClientAuthoritative();
	}

	private boolean ownsClientSimulation() {
		return this.level().isClientSide() && this.isLocalClientAuthoritative() && !ReplayPlayback.isActive();
	}


	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction) {
		if (this.hasPassenger(passenger)) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			float pitchRad = (float) Math.toRadians(this.getXRot());
			float rollRad = (float) Math.toRadians(this.getRoll());
			Vec3 forward = this.calculateHeading(this.getYRot(), this.getXRot());
			Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
			Vec3 unrolledUp = new Vec3(
				Math.sin(yawRad) * Math.sin(pitchRad),
				Math.cos(pitchRad),
				-Math.cos(yawRad) * Math.sin(pitchRad)
			);
			Vec3 rolledUp = unrolledUp.scale(Math.cos(rollRad)).add(right.scale(Math.sin(rollRad)));
			Vec3 seatPosition = this.position()
				.add(forward.scale(-0.5D))
				.add(rolledUp.scale(-0.475D));
			moveFunction.accept(passenger, seatPosition.x, seatPosition.y, seatPosition.z);

			if (passenger instanceof Player player) {
				if (this.isHelicopter() && Math.abs(this.yawRate) > 0.001F) {
					player.yRotO = player.getYRot() - this.yawRate;
					player.yHeadRotO = player.getYHeadRot() - this.yawRate;
					player.yBodyRotO = player.yBodyRot - this.yawRate;
				}
			} else if (passenger instanceof VillagerPilotEntity villager) {
				villager.setYRot(this.getYRot());
				villager.setYBodyRot(this.getYRot());
				villager.setYHeadRot(this.getYRot());
				villager.yRotO = this.getRenderYawO();
				villager.yBodyRotO = this.getRenderYawO();
				villager.yHeadRotO = this.getRenderYawO();
			}
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public Item getDropItem() {
		return com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod.PLANE_ITEM.get();
	}

	/**
	 * Vertical speed (blocks/tick downward) a landing can absorb without airframe damage.
	 */
	protected double getLandingSafeSinkRate(boolean waterSurface) {
		return waterSurface ? LANDING_WATER_SAFE_SINK : LANDING_SAFE_SINK;
	}

	/**
	 * Vertical speed that destroys the airframe on impact when attitude is level.
	 */
	protected double getLandingFatalSinkRate(boolean waterSurface) {
		return waterSurface ? LANDING_WATER_FATAL_SINK : LANDING_FATAL_SINK;
	}

	private void evaluateLandingAfterMove(Vec3 impactVelocity, boolean airborneBefore) {
		if (this.level().isClientSide() || !airborneBefore) {
			return;
		}
		boolean supportedNow = this.onGround() || this.verticalCollisionBelow || this.hasSurfaceSupport();
		if (!supportedNow) {
			return;
		}
		boolean waterSurface = this.hasSurfaceSupport() && !this.onGround();
		this.applyLandingImpact(-impactVelocity.y, impactVelocity.horizontalDistance(), waterSurface);
	}

	/**
	 * Player-piloted aircraft skip server physics, but vehicle packets still update
	 * position and {@code onGround}. Reconstruct sink rate from the Y delta.
	 */
	private void tickLandingImpactFromSyncedPosition() {
		boolean supported = this.onGround() || this.hasSurfaceSupport();
		double y = this.getY();
		if (!Double.isFinite(this.lastSyncY)) {
			this.lastSyncY = y;
			this.lastSyncAirborne = !supported;
			this.syncAirborneTicks = this.lastSyncAirborne ? 1 : 0;
			return;
		}

		double dy = y - this.lastSyncY;
		this.lastSyncY = y;

		if (!supported) {
			this.syncAirborneTicks++;
			this.peakSyncSink = Math.max(this.peakSyncSink, -dy);
			this.lastSyncAirborne = true;
			return;
		}

		if (this.lastSyncAirborne && this.syncAirborneTicks >= MIN_AIRBORNE_TICKS_FOR_SYNC_LANDING) {
			double sink = Math.max(this.peakSyncSink, -dy);
			boolean waterSurface = this.hasSurfaceSupport() && !this.onGround();
			this.applyLandingImpact(sink, this.getDeltaMovement().horizontalDistance(), waterSurface);
		}
		this.syncAirborneTicks = 0;
		this.peakSyncSink = 0.0D;
		this.lastSyncAirborne = false;
	}

	private void applyLandingImpact(double sinkRate, double horizontalSpeed, boolean waterSurface) {
		if (!(this.level() instanceof ServerLevel world) || this.isRemoved()) {
			return;
		}
		if (this.landingImpactCooldown > 0 || !Double.isFinite(sinkRate)) {
			return;
		}
		if (sinkRate <= 0.0D || sinkRate >= LANDING_TELEPORT_SINK) {
			return;
		}

		double safe = this.getLandingSafeSinkRate(waterSurface);
		if (sinkRate <= safe) {
			return;
		}

		double fatal = Math.max(safe + 0.05D, this.getLandingFatalSinkRate(waterSurface));
		double severity = Mth.clamp((sinkRate - safe) / (fatal - safe), 0.0D, 1.0D);
		double attitude = this.getLandingAttitudeMultiplier(horizontalSpeed);
		float damage = (float) (severity * MAX_COMBAT_HEALTH * attitude);
		if (damage < 0.5F) {
			return;
		}

		this.landingImpactCooldown = LANDING_IMPACT_COOLDOWN_TICKS;
		float newHealth = this.getCombatHealth() - damage;
		this.setCombatHealth(newHealth);

		float volume = (float) (0.6D + severity * 0.8D);
		world.playSound(
			null,
			this.getX(), this.getY(), this.getZ(),
			severity >= 0.55D ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.ANVIL_LAND,
			SoundSource.NEUTRAL,
			volume,
			severity >= 0.55D ? 1.15F : 0.75F
		);
		world.sendParticles(
			severity >= 0.55D ? ParticleTypes.EXPLOSION : ParticleTypes.CLOUD,
			this.getX(), this.getY() + 0.35D, this.getZ(),
			severity >= 0.55D ? 2 : 8,
			0.6D, 0.2D, 0.6D,
			0.02D
		);

		float passengerDamage = (float) Math.max(0.0D, (sinkRate - safe - 0.08D) * 16.0D * attitude);
		if (passengerDamage >= 1.0F) {
			DamageSource fall = this.damageSources().fall();
			for (Entity passenger : List.copyOf(this.getPassengers())) {
				if (passenger instanceof LivingEntity living) {
					living.hurtServer(world, fall, passengerDamage);
				}
			}
		}

		if (damage >= 4.0F) {
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof Player player) {
					player.displayClientMessage(Component.translatable(
						newHealth <= 0.0F
							? "message.peterwolfs_planes.crash_landing"
							: "message.peterwolfs_planes.hard_landing"
					), true);
				}
			}
		}

		if (newHealth <= 0.0F) {
			this.destroyFromCombat(world);
		}
	}

	private double getLandingAttitudeMultiplier(double horizontalSpeed) {
		float noseDown = Math.max(0.0F, this.getXRot());
		float bank = Math.abs(this.getRoll());
		double attitude = 1.0D
			+ Mth.clamp(noseDown / 50.0F, 0.0F, 1.0F) * 0.85D
			+ Mth.clamp((bank - 18.0F) / 50.0F, 0.0F, 1.0F) * 0.55D;
		if (horizontalSpeed > 0.85D) {
			attitude += Mth.clamp((horizontalSpeed - 0.85D) / 0.8D, 0.0D, 0.4D);
		}
		return attitude;
	}

	@Override
	public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
		if (this.isRemoved()) {
			return false;
		}

		boolean combatDamage = source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION);
		boolean pilotedByPlayer = this.getFirstPassenger() instanceof Player;

		// Empty planes: player left-click still recovers the item (pickup).
		if (!pilotedByPlayer && !combatDamage && source.getEntity() instanceof Player player && !player.isSpectator()) {
			if (!player.getAbilities().instabuild) {
				this.spawnAtLocation(world, this.getDropItem());
			}
			this.discard();
			return true;
		}

		// Dogfight damage: arrows, machine-gun fire, and TNT can hit occupied aircraft.
		if (combatDamage) {
			// Ignore friendly fire from the pilot's own munitions
			Entity attacker = source.getEntity();
			if (attacker != null && attacker.getVehicle() == this) {
				return false;
			}
			if (attacker != null && this.hasPassenger(attacker)) {
				return false;
			}

			float newHealth = this.getCombatHealth() - amount;
			this.setCombatHealth(newHealth);
			world.playSound(
				null,
				this.getX(), this.getY(), this.getZ(),
				SoundEvents.IRON_GOLEM_HURT,
				SoundSource.NEUTRAL,
				0.8F,
				1.2F
			);

			if (newHealth <= 0.0F) {
				this.destroyFromCombat(world);
			}
			return true;
		}

		// Piloted aircraft remain immune to melee / generic non-combat hits.
		return false;
	}

	private void destroyFromCombat(ServerLevel world) {
		for (Entity passenger : List.copyOf(this.getPassengers())) {
			passenger.stopRiding();
			passenger.setDeltaMovement(this.getDeltaMovement().add(
				(this.random.nextDouble() - 0.5D) * 0.4D,
				0.35D,
				(this.random.nextDouble() - 0.5D) * 0.4D
			));
		}

		world.sendParticles(
			ParticleTypes.EXPLOSION_EMITTER,
			this.getX(), this.getY() + 0.5D, this.getZ(),
			1, 0.0D, 0.0D, 0.0D, 0.0D
		);
		world.playSound(
			null,
			this.getX(), this.getY(), this.getZ(),
			SoundEvents.GENERIC_EXPLODE.value(),
			SoundSource.NEUTRAL,
			1.5F,
			0.85F
		);
		this.discard();
	}

	@Override
	public net.minecraft.world.InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 location, net.minecraft.world.InteractionHand hand) {
		return this.interact(player, hand);
	}

	@Override
	public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
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
				boolean started = player.startRiding(this);
				if (started && this.isHelicopter()) {
					this.resetHelicopterControls();
				}
				return started ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
			}
			if (this.isHelicopter()) {
				this.resetHelicopterControls();
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
	}
}
