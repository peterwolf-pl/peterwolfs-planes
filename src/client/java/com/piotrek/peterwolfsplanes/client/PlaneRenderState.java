package com.piotrek.peterwolfsplanes.client;

import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PlaneRenderState extends EntityRenderState {
	public float pitch;
	public float yRot;
	public float roll;
	public float propellerAngle;
	public float speed;
	public float altitude;
	public boolean restingOnSolidGround;
	public float ageInTicks;
	/** -1..1, negative = pull back / climb, positive = push forward / descend. */
	public float stickPitch;
	/** -1..1, negative = left, positive = right. */
	public float stickRoll;
	/** Nav lights, strobes, and beacon are on only while the aircraft is occupied. */
	public boolean lightsOn;

	public void extractControlStick(PlaneEntity entity, float partialTick) {
		this.lightsOn = entity.isVehicle();
		LivingEntity pilot = entity.getControllingPassenger();
		float rudder = entity.getRudder();
		if (pilot instanceof Player) {
			float relativePitch = Mth.clamp(Mth.wrapDegrees(pilot.getXRot(partialTick) - entity.getXRot(partialTick)), -80.0F, 80.0F);
			float relativeYaw = Mth.clamp(Mth.wrapDegrees(pilot.getYRot(partialTick) - entity.getYRot(partialTick)), -80.0F, 80.0F);
			if (entity.isHelicopter()) {
				if (!entity.isHelicopterAirborne()) {
					this.stickPitch = 0.0F;
					this.stickRoll = 0.0F;
				} else {
					this.stickPitch = Mth.clamp(this.pitch / 16.0F - relativePitch / 20.0F, -1.0F, 1.0F);
					this.stickRoll = Mth.clamp(-relativeYaw / 20.0F, -1.0F, 1.0F);
				}
			} else {
				this.stickPitch = Mth.clamp(relativePitch / 45.0F, -1.0F, 1.0F);
				this.stickRoll = Mth.clamp(relativeYaw / 45.0F + rudder * 0.65F, -1.0F, 1.0F);
			}
		} else {
			if (entity.isHelicopter() && !entity.isHelicopterAirborne()) {
				this.stickPitch = 0.0F;
				this.stickRoll = 0.0F;
			} else {
				this.stickPitch = Mth.clamp(this.pitch / 16.0F, -1.0F, 1.0F);
				this.stickRoll = Mth.clamp(-this.roll / 20.0F + rudder * 0.65F, -1.0F, 1.0F);
			}
		}
	}
}
