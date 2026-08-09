package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class WaterPlaneEntity extends LargePlaneEntity {
	public WaterPlaneEntity(EntityType<? extends LargePlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.WATER_PLANE_ITEM;
	}

	public double getWaterSurfaceY() {
		BlockPos pos = this.blockPosition();
		// At the exact surface the entity's block position is already air. Scan
		// down through the float depth first, then find the top water block.
		int scanDepth = 0;
		while (!this.level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && scanDepth < 3) {
			pos = pos.below();
			scanDepth++;
		}
		if (!this.level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
			return Double.NaN;
		}
		while (this.level().getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)) {
			pos = pos.above();
		}
		FluidState state = this.level().getFluidState(pos);
		if (state.is(net.minecraft.tags.FluidTags.WATER)) {
			float height = state.getHeight(this.level(), pos);
			return (double) pos.getY() + height;
		}
		return Double.NaN;
	}

	private boolean isOnWater() {
		double waterSurfaceY = this.getWaterSurfaceY();
		return this.isOnWater(waterSurfaceY);
	}

	private boolean isOnWater(double waterSurfaceY) {
		return !Double.isNaN(waterSurfaceY)
			&& this.getY() <= waterSurfaceY + 0.1D
			&& this.getY() >= waterSurfaceY - 1.5D;
	}

	@Override
	protected boolean hasSurfaceSupport() {
		return this.isOnWater();
	}

	@Override
	protected float getEmptyGroundVisualPitch() {
		// Two long floats support the airframe along its length, so it parks
		// level instead of resting tail-low like two-wheel landing gear.
		return 0.0F;
	}

	@Override
	public void tick() {
		super.tick();

		double waterSurfaceY = this.getWaterSurfaceY();
		if (this.isOnWater(waterSurfaceY)) {
			if (this.getY() < waterSurfaceY) {
				this.setPos(this.getX(), waterSurfaceY, this.getZ());
				Vec3 vel = this.getDeltaMovement();
				if (vel.y < 0.0D) {
					this.setDeltaMovement(new Vec3(vel.x, 0.0D, vel.z));
				}
			}
		}
	}
}
