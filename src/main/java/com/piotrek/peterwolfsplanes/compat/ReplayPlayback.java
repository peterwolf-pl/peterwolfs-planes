package com.piotrek.peterwolfsplanes.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Detects ReplayMod / Flashback playback so aircraft can stop simulating and
 * follow recorded (interpolated) positions instead of fighting the replay.
 */
public final class ReplayPlayback {
	private static final Method REPLAYMOD_HANDLER = findReplayModHandler();
	private static final Method FLASHBACK_IN_REPLAY = findFlashbackInReplay();

	private ReplayPlayback() {}

	public static boolean isActive() {
		return isReplayModPlaying() || isFlashbackPlaying();
	}

	private static boolean isReplayModPlaying() {
		if (REPLAYMOD_HANDLER == null) {
			return false;
		}
		try {
			Object instance = REPLAYMOD_HANDLER.getDeclaringClass().getField("instance").get(null);
			if (instance == null) {
				return false;
			}
			return REPLAYMOD_HANDLER.invoke(instance) != null;
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static boolean isFlashbackPlaying() {
		if (FLASHBACK_IN_REPLAY == null) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(FLASHBACK_IN_REPLAY.invoke(null));
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static Method findReplayModHandler() {
		if (!FabricLoader.getInstance().isModLoaded("replaymod")) {
			return null;
		}
		try {
			return Class.forName("com.replaymod.replay.ReplayModReplay").getMethod("getReplayHandler");
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static Method findFlashbackInReplay() {
		if (!FabricLoader.getInstance().isModLoaded("flashback")) {
			return null;
		}
		try {
			return Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}
}
