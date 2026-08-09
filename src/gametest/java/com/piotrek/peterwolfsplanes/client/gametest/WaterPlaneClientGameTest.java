package com.piotrek.peterwolfsplanes.client.gametest;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.WaterPlaneEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("UnstableApiUsage")
public final class WaterPlaneClientGameTest implements FabricClientGameTest {
	private static final float CONTROL_RESPONSE_MIN = 1.0F;
	private static final float BRAKED_PITCH_MAX = 4.0F;
	private static final double BRAKED_SPEED_MAX = 0.08D;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(960, 700);
		int[] planeId = {-1};
		int[] waterBlockY = {0};

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.setUseConsistentSettings(true)
			.create()) {
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");

			// First prove the empty floatplane settles level on land.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = (ServerLevel) player.level();
				int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
				waterBlockY[0] = groundY;
				WaterPlaneEntity plane = new WaterPlaneEntity(PeterwolfsPlanesMod.WATER_PLANE_ENTITY, level);
				plane.setPos(0.5D, groundY + 0.1D, 0.5D);
				plane.setYRot(0.0F);
				plane.setXRot(-28.0F);
				plane.setDeltaMovement(Vec3.ZERO);
				level.addFreshEntity(plane);
				planeId[0] = plane.getId();

				player.teleportTo(0.5D, groundY + 2.0D, 7.5D);
				player.setYRot(180.0F);
				player.setXRot(15.0F);
			});

			context.waitFor(client -> {
				if (client.level == null || !(client.level.getEntity(planeId[0]) instanceof WaterPlaneEntity plane)) {
					return false;
				}
				return plane.onGround() && Math.abs(plane.getVisualPitch(1.0F)) < 1.0F;
			}, 200);
			singleplayer.getClientLevel().waitForChunksRender();
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				client.options.fov().set(80);
			});
			context.getInput().lookAt(180.0F, 15.0F);
			context.waitTicks(5);
			context.takeScreenshot("water-plane-parked-high-wing");

			// Then put the same plane directly on a water strip and drive its
			// elevator/aileron path through actual player mouse-look input. This
			// covers the former bug where water contact was reported as grounded.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = (ServerLevel) player.level();
				WaterPlaneEntity plane = (WaterPlaneEntity) level.getEntity(planeId[0]);
				if (plane == null) {
					throw new AssertionError("water plane disappeared before control test");
				}
				for (int x = -10; x <= 10; x++) {
					for (int z = -10; z <= 80; z++) {
						level.setBlockAndUpdate(new BlockPos(x, waterBlockY[0], z), Blocks.WATER.defaultBlockState());
					}
				}
				double planeY = waterBlockY[0] + 0.8D;
				plane.setPos(0.5D, planeY, 0.5D);
				plane.setOnGround(false);
				plane.setYRot(0.0F);
				plane.setXRot(0.0F);
				plane.setRoll(0.0F);
				plane.setThrottle(0.0F);
				plane.setDeltaMovement(Vec3.ZERO);
				player.teleportTo(0.5D, planeY, 0.5D);
				player.setYRot(0.0F);
				player.setXRot(0.0F);
				player.fallDistance = 0.0F;
				player.startRiding(plane);
			});

			context.waitFor(client -> client.player != null
				&& client.player.getVehicle() instanceof WaterPlaneEntity, 200);
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				WaterPlaneEntity plane = (WaterPlaneEntity) client.player.getVehicle();
				double planeY = waterBlockY[0] + 0.8D;
				plane.setPos(0.5D, planeY, 0.5D);
				plane.setOnGround(false);
				plane.setThrottle(0.0F);
				plane.setDeltaMovement(Vec3.ZERO);
				client.player.setPos(0.5D, planeY, 0.5D);
				client.player.fallDistance = 0.0F;
			});
			context.getInput().lookAt(0.0F, 0.0F);
			context.waitTicks(10);
			context.takeScreenshot("water-plane-cockpit-pov-cutout");

			// Accelerate on the water, then verify that real mouse look still
			// produces both elevator and aileron response.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				WaterPlaneEntity plane = (WaterPlaneEntity) player.level().getEntity(planeId[0]);
				plane.setThrottle(1.0F);
				plane.setDeltaMovement(new Vec3(0.0D, 0.05D, 0.8D));
			});
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				WaterPlaneEntity plane = (WaterPlaneEntity) client.player.getVehicle();
				plane.setThrottle(1.0F);
				plane.setDeltaMovement(new Vec3(0.0D, 0.05D, 0.8D));
			});
			context.getInput().lookAt(35.0F, -25.0F);
			context.waitTicks(20);
			context.takeScreenshot("water-plane-mouse-controls");

			float[] response = new float[2];
			context.runOnClient(client -> {
				if (client.player == null || !(client.player.getVehicle() instanceof WaterPlaneEntity plane)) {
					throw new AssertionError("player left the water plane during control test");
				}
				response[0] = plane.getRoll();
				response[1] = plane.getXRot();
			});
			if (Math.abs(response[0]) < CONTROL_RESPONSE_MIN || Math.abs(response[1]) < CONTROL_RESPONSE_MIN) {
				throw new AssertionError("mouse control response too small: roll=" + response[0] + ", pitch=" + response[1]);
			}

			// Recreate a nose-low water landing and brake with the real S binding.
			// The floats must settle without the airborne stall term tipping the
			// stopped aircraft onto its nose.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				WaterPlaneEntity plane = (WaterPlaneEntity) player.level().getEntity(planeId[0]);
				double planeY = waterBlockY[0] + 0.8D;
				plane.setPos(0.5D, planeY, 0.5D);
				plane.setOnGround(false);
				plane.setYRot(0.0F);
				plane.setXRot(12.0F);
				plane.setRoll(0.0F);
				plane.setThrottle(0.0F);
				plane.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.55D));
				player.setYRot(0.0F);
				player.setXRot(12.0F);
			});
			context.runOnClient(client -> {
				WaterPlaneEntity plane = (WaterPlaneEntity) client.player.getVehicle();
				double planeY = waterBlockY[0] + 0.8D;
				plane.setPos(0.5D, planeY, 0.5D);
				plane.setOnGround(false);
				plane.setYRot(0.0F);
				plane.setXRot(12.0F);
				plane.setRoll(0.0F);
				plane.setThrottle(0.0F);
				plane.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.55D));
				client.player.setYRot(0.0F);
				client.player.setXRot(12.0F);
			});
			context.getInput().lookAt(0.0F, 12.0F);
			context.getInput().holdKey(options -> options.keyDown);
			context.waitTicks(60);
			context.getInput().releaseKey(options -> options.keyDown);
			context.waitTicks(20);
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("water-plane-braked-level");

			double[] brakedState = new double[4];
			context.runOnClient(client -> {
				WaterPlaneEntity plane = (WaterPlaneEntity) client.player.getVehicle();
				brakedState[0] = plane.getXRot();
				brakedState[1] = plane.getRoll();
				brakedState[2] = plane.getDeltaMovement().horizontalDistance();
				brakedState[3] = plane.getThrottle();
			});
			if (Math.abs(brakedState[0]) > BRAKED_PITCH_MAX
				|| Math.abs(brakedState[1]) > BRAKED_PITCH_MAX
				|| brakedState[2] > BRAKED_SPEED_MAX
				|| brakedState[3] > -0.75D) {
				throw new AssertionError("water braking did not settle level: pitch=" + brakedState[0]
					+ ", roll=" + brakedState[1] + ", speed=" + brakedState[2] + ", brake=" + brakedState[3]);
			}
		}
	}
}
