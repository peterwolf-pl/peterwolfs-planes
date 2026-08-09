package com.piotrek.peterwolfsplanes.client.gametest;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("UnstableApiUsage")
public final class ParagliderVisualClientGameTest implements FabricClientGameTest {
	private static final float ACTIVE_BRAKE_MIN = 0.50f;
	private static final float RELEASED_BRAKE_MAX = 0.03f;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(800, 700);

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.setUseConsistentSettings(true)
			.create()) {
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK));
				player.teleportTo(0.5D, 200.0D, 0.5D);
				player.setYRot(0.0f);
				player.setXRot(0.0f);
				player.fallDistance = 5.0f;
				player.setDeltaMovement(Vec3.ZERO);
			});

			context.waitFor(client -> client.player != null
				&& client.player.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK)
				&& PeterwolfsPlanesClient.isParagliderVisuallyDeployed(client.player.getId()), 200);
			singleplayer.getClientLevel().waitForChunksRender();

			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				client.options.fov().set(95);
			});
			context.getInput().lookAt(0.0f, -6.0f);
			context.waitTicks(8);

			assertBrakeState(
				"neutral",
				PeterwolfsPlanesClient.paragliderLeftBrake,
				PeterwolfsPlanesClient.paragliderRightBrake,
				RELEASED_BRAKE_MAX
			);
			context.takeScreenshot("paraglider-neutral");

			context.getInput().holdKey(options -> options.keyLeft);
			context.waitTicks(14);
			assertTurningSide("left", PeterwolfsPlanesClient.paragliderLeftBrake, PeterwolfsPlanesClient.paragliderRightBrake);
			context.takeScreenshot("paraglider-left-turn");

			context.getInput().releaseKey(options -> options.keyLeft);
			context.waitTicks(30);
			assertBrakeState(
				"released after left turn",
				PeterwolfsPlanesClient.paragliderLeftBrake,
				PeterwolfsPlanesClient.paragliderRightBrake,
				RELEASED_BRAKE_MAX
			);
			context.takeScreenshot("paraglider-released");

			context.getInput().holdKey(options -> options.keyRight);
			context.waitTicks(14);
			assertTurningSide("right", PeterwolfsPlanesClient.paragliderRightBrake, PeterwolfsPlanesClient.paragliderLeftBrake);
			context.takeScreenshot("paraglider-right-turn");
			context.getInput().releaseKey(options -> options.keyRight);
		}
	}

	private static void assertTurningSide(String side, float activeBrake, float oppositeBrake) {
		if (activeBrake < ACTIVE_BRAKE_MIN) {
			throw new AssertionError(side + " brake did not follow the actual steering input: " + activeBrake);
		}
		if (oppositeBrake > RELEASED_BRAKE_MAX) {
			throw new AssertionError(side + " turn also pulled the opposite brake: " + oppositeBrake);
		}
	}

	private static void assertBrakeState(String state, float leftBrake, float rightBrake, float maximum) {
		if (leftBrake > maximum || rightBrake > maximum) {
			throw new AssertionError(state + " brakes were not neutral: left=" + leftBrake + ", right=" + rightBrake);
		}
	}
}
