package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class MonoplaneEntity extends PlaneEntity {
	public MonoplaneEntity(EntityType<? extends PlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.MONOPLANE_ITEM;
	}

	@Override
	protected double getThrustForce() {
		return 0.1384D; // 2x top speed (~2.46 b/t / 49.2 m/s / 177 km/h)
	}

	@Override
	protected float getMaxYawRate() {
		return 3.5F;
	}

	@Override
	protected float getMaxPitchRate() {
		return 2.2F;
	}
}
