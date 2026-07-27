package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class LargeTwinEnginePlaneEntity extends LargePlaneEntity {
	public LargeTwinEnginePlaneEntity(EntityType<? extends PlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.LARGE_TWIN_ENGINE_PLANE_ITEM;
	}
}
