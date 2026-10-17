package com.piotrek.peterwolfsplanes.client;

import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.entity.Entity;

public final class AircraftEngineSounds {
	private static final Map<Integer, SoundInstance> PLAYING = new HashMap<>();

	private AircraftEngineSounds() {
	}

	public static void tick(Minecraft client) {
		if (client.level == null) {
			PLAYING.clear();
			return;
		}

		PLAYING.entrySet().removeIf(entry -> !client.getSoundManager().isActive(entry.getValue()));

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof PlaneEntity plane) || PLAYING.containsKey(plane.getId())) {
				continue;
			}
			AircraftEngineSoundInstance instance = new AircraftEngineSoundInstance(plane);
			SoundEngine.PlayResult result = client.getSoundManager().play(instance);
			if (result != SoundEngine.PlayResult.NOT_STARTED) {
				PLAYING.put(plane.getId(), instance);
			}
		}
	}

	static void stopped(int entityId) {
		PLAYING.remove(entityId);
	}
}
