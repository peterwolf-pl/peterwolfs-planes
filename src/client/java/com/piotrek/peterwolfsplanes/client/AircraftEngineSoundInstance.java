package com.piotrek.peterwolfsplanes.client;

import com.piotrek.peterwolfsplanes.PeterwolfsPlanesMod;
import com.piotrek.peterwolfsplanes.entity.PlaneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class AircraftEngineSoundInstance extends AbstractTickableSoundInstance {
	private static final float VOLUME_FADE = 0.18F;
	private static final float PITCH_FADE = 0.12F;

	private final PlaneEntity aircraft;

	public AircraftEngineSoundInstance(PlaneEntity aircraft) {
		super(
			aircraft.isHelicopter() ? PeterwolfsPlanesMod.HELICOPTER_ENGINE_SOUND : PeterwolfsPlanesMod.PLANE_ENGINE_SOUND,
			SoundSource.NEUTRAL,
			SoundInstance.createUnseededRandom()
		);
		this.aircraft = aircraft;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;
		this.pitch = aircraft.getEngineSoundPitch();
		this.x = aircraft.getX();
		this.y = aircraft.getY();
		this.z = aircraft.getZ();
		this.attenuation = SoundInstance.Attenuation.LINEAR;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public boolean canPlaySound() {
		return !this.aircraft.isSilent();
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();
		if (this.aircraft.isRemoved() || client.level == null || this.aircraft.level() != client.level) {
			this.stop();
			AircraftEngineSounds.stopped(this.aircraft.getId());
			return;
		}

		this.x = this.aircraft.getX();
		this.y = this.aircraft.getY();
		this.z = this.aircraft.getZ();

		float targetVolume = this.aircraft.getEngineSoundVolume();
		float targetPitch = this.aircraft.getEngineSoundPitch();
		this.volume = Mth.lerp(VOLUME_FADE, this.volume, targetVolume);
		this.pitch = Mth.lerp(PITCH_FADE, this.pitch, targetPitch);
		if (targetVolume <= 0.0F && this.volume < 0.01F) {
			this.volume = 0.0F;
		}
	}
}
