package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

public class VillagerPilotEntity extends PathfinderMob {
	/** Cooldown before spawning/mounting a plane again after dismounting. */
	private int planeCooldown = 0;

	public VillagerPilotEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.25D);
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.level().isClientSide()) {
			if (this.planeCooldown > 0) {
				this.planeCooldown--;
				return;
			}

			// If already riding a plane, nothing to do
			if (this.getVehicle() instanceof PlaneEntity) {
				return;
			}

			// Look for a nearby empty plane to board
			List<PlaneEntity> nearbyPlanes = this.level().getEntitiesOfClass(
				PlaneEntity.class,
				this.getBoundingBox().inflate(20.0D),
				plane -> !plane.isVehicle()
			);

			PlaneEntity nearestEmpty = nearbyPlanes.stream()
				.min((p1, p2) -> Double.compare(this.distanceToSqr(p1), this.distanceToSqr(p2)))
				.orElse(null);

			if (nearestEmpty != null) {
				if (this.distanceToSqr(nearestEmpty) < 4.0D) {
					this.startRiding(nearestEmpty);
					// Set a short cooldown in case startRiding fails
					if (!(this.getVehicle() instanceof PlaneEntity)) {
						this.planeCooldown = 20;
					}
				} else {
					this.getNavigation().moveTo(nearestEmpty, 1.25D);
				}
			} else {
				// No empty plane nearby - spawn one, then apply a cooldown so we
				// don't spawn another on the very next tick if startRiding fails.
				PlaneEntity plane;
				if (this.random.nextBoolean()) {
					plane = new PlaneEntity(PeterwolfsPlanesMod.PLANE_ENTITY, this.level());
				} else {
					plane = new TriplaneEntity(PeterwolfsPlanesMod.TRIPLANE_ENTITY, this.level());
				}
				plane.setPos(this.getX(), this.getY(), this.getZ());
				plane.setYRot(this.getYRot());
				this.level().addFreshEntity(plane);
				this.startRiding(plane);
				// Prevent any re-spawn for at least 2 seconds
				this.planeCooldown = 40;
			}
		}
	}
}
