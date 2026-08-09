package com.piotrek.peterwolfsplanes.entity;

import net.minecraft.core.BlockPos;
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
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlaneEntity extends Entity {
	private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> RUDDER = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> PUSHER_ID = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> COMBAT_MODE = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> COMBAT_HEALTH = SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);

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
		builder.define(COMBAT_MODE, false);
		builder.define(COMBAT_HEALTH, MAX_COMBAT_HEALTH);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setThrottle(input.getFloatOr("Throttle", 0.0F));
		this.setRoll(input.getFloatOr("Roll", 0.0F));
		this.setRudder(input.getFloatOr("Rudder", 0.0F));
		this.setCombatMode(input.getBooleanOr("CombatMode", false));
		this.setCombatHealth(input.getFloatOr("CombatHealth", MAX_COMBAT_HEALTH));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("Throttle", this.getThrottle());
		output.putFloat("Roll", this.getRoll());
		output.putFloat("Rudder", this.getRudder());
		output.putBoolean("CombatMode", this.isCombatMode());
		output.putFloat("CombatHealth", this.getCombatHealth());
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

	/**
	 * Applies pilot combat inputs from the client. Weapon spawning runs server-side.
	 */
	public void applyCombatInput(boolean combatMode, boolean fireGuns, boolean dropBomb) {
		boolean wasCombat = this.isCombatMode();
		this.setCombatMode(combatMode);
		if (combatMode != wasCombat && this.getFirstPassenger() instanceof Player pilot) {
			pilot.sendOverlayMessage(
				Component.translatable(combatMode
					? "message.peterwolfs_planes.combat_armed"
					: "message.peterwolfs_planes.combat_safe")
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
			pilot.sendOverlayMessage(Component.translatable("message.peterwolfs_planes.need_tnt"));
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

	private void tickPilotedPhysics(@Nullable Player pilot, float currentThrottle, float aiRelativeYaw, float aiRelativePitch) {
		Vec3 currentVelocity = this.getDeltaMovement();
		boolean grounded = this.resolveGroundHandling(this.onGround(), currentThrottle, currentVelocity);
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
				sink = Mth.clamp(liftDeficit * (npc ? 0.85D : 1.45D), -0.10D, 0.0D);
				heading = this.calculateHeading(this.getYRot(), this.getXRot());
			}
		}

		Vec3 movement = heading.scale(speed).add(0.0D, sink, 0.0D);

		// Guarantee a minimum climb rate for NPC when commanding nose-up and flying
		if (npc && !grounded && this.aiDesiredPitch <= -12.0F && speed > 0.25D) {
			double minClimb = this.aiDesiredPitch <= -28.0F ? 0.22D : 0.14D;
			if (movement.y < minClimb) {
				movement = new Vec3(movement.x, minClimb, movement.z);
			}
		}

		if (grounded && speed == 0.0D && this.hasGroundIdlePower(currentThrottle)) {
			movement = Vec3.ZERO;
		}
		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);
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

		if (this.onGround() || this.hasSurfaceSupport()) {
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
