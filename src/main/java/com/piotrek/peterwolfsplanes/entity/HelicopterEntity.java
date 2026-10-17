package com.piotrek.peterwolfsplanes.entity;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class HelicopterEntity extends PlaneEntity {
	public HelicopterEntity(EntityType<? extends PlaneEntity> type, Level world) {
		super(type, world);
	}

	@Override
	public boolean isHelicopter() {
		return true;
	}

	@Override
	public Item getDropItem() {
		return PeterwolfsPlanesMod.HELICOPTER_ITEM;
	}

	@Override
	protected float getEmptyGroundVisualPitch() {
		return 0.0F;
	}

	@Override
	protected float[] getGunLateralOffsets() {
		return new float[] { -0.75F, 0.75F };
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction) {
		if (this.hasPassenger(passenger)) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			float pitchRad = (float) Math.toRadians(this.getXRot());
			float rollRad = (float) Math.toRadians(this.getRoll());
			Vec3 forward = this.calculateHeading(this.getYRot(), this.getXRot());
			Vec3 left = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
			Vec3 trueUp = new Vec3(
				-Math.sin(yawRad) * Math.sin(pitchRad),
				Math.cos(pitchRad),
				Math.cos(yawRad) * Math.sin(pitchRad)
			);
			Vec3 rolledUp = trueUp.scale(Math.cos(rollRad)).add(left.scale(Math.sin(rollRad)));
			Vec3 seatPosition = this.position()
				.add(forward.scale(-0.22D))
				.add(rolledUp.scale(-0.48D));
			moveFunction.accept(passenger, seatPosition.x, seatPosition.y, seatPosition.z);

			if (passenger instanceof Player player) {
				float rate = this.getYawRate();
				if (Math.abs(rate) > 0.001F) {
					player.yRotO = player.getYRot() - rate;
					player.yHeadRotO = player.getYHeadRot() - rate;
					player.yBodyRotO = player.yBodyRot - rate;
				}
			} else if (passenger instanceof VillagerPilotEntity villager) {
				villager.setYRot(this.getYRot());
				villager.setYBodyRot(this.getYRot());
				villager.setYHeadRot(this.getYRot());
				villager.yRotO = this.getRenderYawO();
				villager.yBodyRotO = this.getRenderYawO();
				villager.yHeadRotO = this.getRenderYawO();
			}
		}
	}
}
