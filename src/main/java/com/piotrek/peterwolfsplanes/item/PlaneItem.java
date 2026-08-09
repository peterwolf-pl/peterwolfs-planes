package com.piotrek.peterwolfsplanes.item;

import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public class PlaneItem extends Item {
	private final Function<Level, ? extends PlaneEntity> planeFactory;
	private final boolean canPlaceOnWater;

	public PlaneItem(Properties properties, Function<Level, ? extends PlaneEntity> planeFactory) {
		this(properties, planeFactory, false);
	}

	public PlaneItem(Properties properties, Function<Level, ? extends PlaneEntity> planeFactory, boolean canPlaceOnWater) {
		super(properties);
		this.planeFactory = planeFactory;
		this.canPlaceOnWater = canPlaceOnWater;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		Player player = context.getPlayer();
		Vec3 waterSpawnPos = canPlaceOnWater && player != null ? getWaterSpawnPos(world, player) : null;
		if (waterSpawnPos != null) {
			if (!world.isClientSide()) {
				spawnPlane(world, player, context.getItemInHand(), waterSpawnPos);
			}
			return InteractionResult.SUCCESS;
		}

		if (!world.isClientSide()) {
			BlockPos pos = context.getClickedPos();
			Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
			spawnPlane(world, player, context.getItemInHand(), spawnPos);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (!canPlaceOnWater) {
			return super.use(world, player, hand);
		}

		Vec3 spawnPos = getWaterSpawnPos(world, player);
		if (spawnPos == null) {
			return super.use(world, player, hand);
		}

		if (!world.isClientSide()) {
			spawnPlane(world, player, player.getItemInHand(hand), spawnPos);
		}
		return InteractionResult.SUCCESS;
	}

	private Vec3 getWaterSpawnPos(Level world, Player player) {
		BlockHitResult hitResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.WATER);
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return null;
		}

		BlockPos waterPos = hitResult.getBlockPos();
		FluidState fluidState = world.getFluidState(waterPos);
		if (!fluidState.is(FluidTags.WATER)) {
			return null;
		}

		double waterSurfaceY = waterPos.getY() + fluidState.getHeight(world, waterPos);
		return new Vec3(waterPos.getX() + 0.5, waterSurfaceY, waterPos.getZ() + 0.5);
	}

	private void spawnPlane(Level world, Player player, ItemStack itemStack, Vec3 spawnPos) {
		PlaneEntity plane = planeFactory.apply(world);
		if (plane == null) {
			return;
		}

		plane.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
		plane.absSnapRotationTo(player != null ? player.getYRot() : 0.0f, 0.0f);
		world.addFreshEntity(plane);
		if (player != null && !player.getAbilities().instabuild) {
			itemStack.shrink(1);
		}
	}
}
