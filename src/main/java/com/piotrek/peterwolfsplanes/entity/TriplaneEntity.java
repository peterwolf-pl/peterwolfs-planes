package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class TriplaneEntity extends PlaneEntity {
	public TriplaneEntity(EntityType<? extends PlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.TRIPLANE_ITEM;
	}
}
