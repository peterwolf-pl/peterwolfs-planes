package com.piotrek.peterwolfsplanes.client;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Recovers the visual deployment state in legacy Flashback recordings which
 * started after the one-time deployment payload had already been sent.
 */
final class FlashbackParagliderCompat {
	private static final Method IS_IN_REPLAY = findIsInReplayMethod();

	private FlashbackParagliderCompat() {
	}

	static boolean inferLegacyDeployment(Entity entity) {
		if (!isInReplay() || !(entity instanceof Player player)) {
			return false;
		}

		return player.getItemBySlot(EquipmentSlot.CHEST).is(PeterwolfsPlanesMod.PARAGLIDER_BACKPACK)
			&& !player.onGround()
			&& !player.isInWater()
			&& !player.isPassenger()
			&& !player.getAbilities().flying
			&& !player.isFallFlying()
			&& !player.isSpectator();
	}

	private static boolean isInReplay() {
		if (IS_IN_REPLAY == null) {
			return false;
		}

		try {
			return Boolean.TRUE.equals(IS_IN_REPLAY.invoke(null));
		} catch (IllegalAccessException | InvocationTargetException exception) {
			return false;
		}
	}

	private static Method findIsInReplayMethod() {
		if (!FabricLoader.getInstance().isModLoaded("flashback")) {
			return null;
		}

		try {
			return Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
		} catch (ClassNotFoundException | NoSuchMethodException exception) {
			return null;
		}
	}
}
