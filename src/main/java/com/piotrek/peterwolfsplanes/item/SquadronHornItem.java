package com.piotrek.peterwolfsplanes.item;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import com.piotrek.peterwolfsplanes.entity.TriplaneEntity;
import com.piotrek.peterwolfsplanes.entity.VillagerPilotEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SquadronHornItem extends Item {
	public SquadronHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			SoundEvents.RAID_HORN,
			SoundSource.PLAYERS,
			2.0F,
			1.0F
		);

		if (!level.isClientSide()) {
			float yaw = player.getYRot();
			double yawRad = Math.toRadians(yaw);
			Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
			Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

			// Leader position is 20 blocks in front of the player, elevated slightly
			Vec3 leaderPos = player.position().add(forward.scale(20.0D)).add(0.0D, 8.0D, 0.0D);

			// We will spawn 5 planes in a V-formation
			// Offsets: Leader(0,0,0), Left1(-8, -2, -12), Right1(8, -2, -12), Left2(-16, -4, -24), Right2(16, -4, -24)
			Vec3[] offsets = {
				new Vec3(0, 0, 0),         // Leader
				new Vec3(-8, -2, -12),     // Follower 1 (Left)
				new Vec3(8, -2, -12),      // Follower 2 (Right)
				new Vec3(-16, -4, -24),    // Follower 3 (Left outer)
				new Vec3(16, -4, -24)      // Follower 4 (Right outer)
			};

			PlaneEntity leaderPlane = null;

			for (int i = 0; i < 5; i++) {
				Vec3 offset = offsets[i];
				Vec3 spawnPos = leaderPos
					.add(right.scale(offset.x))
					.add(0.0D, offset.y, 0.0D)
					.add(forward.scale(offset.z));

				// 60% chance of standard plane, 40% chance of triplane
				PlaneEntity plane;
				if (level.getRandom().nextFloat() < 0.6F) {
					plane = new PlaneEntity(PeterwolfsPlanesMod.PLANE_ENTITY, level);
				} else {
					plane = new TriplaneEntity(PeterwolfsPlanesMod.TRIPLANE_ENTITY, level);
				}

				plane.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
				plane.setYRot(yaw);
				plane.setXRot(0.0F);
				plane.setDeltaMovement(forward.scale(0.6D)); // Start already flying forward at takeoff speed
				plane.setThrottle(0.85F);
				level.addFreshEntity(plane);

				// Spawn pilot
				VillagerPilotEntity pilot = new VillagerPilotEntity(PeterwolfsPlanesMod.VILLAGER_PILOT_ENTITY, level);
				pilot.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
				pilot.setYRot(yaw);
				level.addFreshEntity(pilot);
				pilot.startRiding(plane);

				if (i == 0) {
					leaderPlane = plane;
					// If player is piloting a plane, leader follows player's plane!
					if (player.getVehicle() instanceof PlaneEntity playerPlane) {
						leaderPlane.setAiLeader(playerPlane);
					}
				} else {
					plane.setAiLeader(leaderPlane);
				}
			}

			player.getCooldowns().addCooldown(stack, 100); // 5 second cooldown
		}

		return InteractionResult.SUCCESS;
	}
}
