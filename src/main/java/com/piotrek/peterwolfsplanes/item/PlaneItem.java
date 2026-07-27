package com.piotrek.peterwolfsplanes.item;

import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public class PlaneItem extends Item {
	private final Function<Level, ? extends PlaneEntity> planeFactory;

	public PlaneItem(Properties properties, Function<Level, ? extends PlaneEntity> planeFactory) {
		super(properties);
		this.planeFactory = planeFactory;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		if (!world.isClientSide()) {
			BlockPos pos = context.getClickedPos();
			Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
			PlaneEntity plane = planeFactory.apply(world);
			if (plane != null) {
				plane.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
				plane.absSnapRotationTo(context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0f, 0.0f);
				world.addFreshEntity(plane);
				if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
					context.getItemInHand().shrink(1);
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.SUCCESS;
	}
}
