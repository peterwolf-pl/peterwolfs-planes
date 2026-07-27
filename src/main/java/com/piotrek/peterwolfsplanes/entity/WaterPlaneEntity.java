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
		// Scan upwards to find the highest water block
		while (this.level().getFluidState(pos.above()).isSource() || this.level().getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)) {
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
		if (Double.isNaN(waterSurfaceY)) {
			return false;
		}
		return this.getY() <= waterSurfaceY + 0.1D;
	}

	@Override
	public boolean onGround() {
		return super.onGround() || this.isOnWater();
	}

	@Override
	public void tick() {
		super.tick();

		double waterSurfaceY = this.getWaterSurfaceY();
		if (!Double.isNaN(waterSurfaceY)) {
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
