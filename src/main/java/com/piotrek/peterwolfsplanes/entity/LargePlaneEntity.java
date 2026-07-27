package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class LargePlaneEntity extends PlaneEntity {
	public LargePlaneEntity(EntityType<? extends PlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.LARGE_PLANE_ITEM;
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction) {
		if (this.hasPassenger(passenger)) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			// Shift rider back by 15% along Z-axis (original Z offset was -0.5D, new is -0.5D * 1.15 = -0.575D)
			double rx = -Math.sin(yawRad) * -0.575D;
			double rz = Math.cos(yawRad) * -0.575D;
			moveFunction.accept(passenger, this.getX() + rx, this.getY() - 0.475D, this.getZ() + rz);
		}
	}
}
