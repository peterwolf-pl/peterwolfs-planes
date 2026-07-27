package com.piotrek.peterwolfsplanes.mixin;

import com.piotrek.peterwolfsplanes.ParagliderStateAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerParagliderStateMixin implements ParagliderStateAccess {
	@Unique
	private static final EntityDataAccessor<Boolean> PETERWOLFS_PLANES_PARAGLIDER_DEPLOYED =
		SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void peterwolfsPlanes$defineParagliderState(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(PETERWOLFS_PLANES_PARAGLIDER_DEPLOYED, false);
	}

	@Override
	public boolean peterwolfsPlanes$isParagliderDeployed() {
		Player player = (Player) (Object) this;
		return player.getEntityData().get(PETERWOLFS_PLANES_PARAGLIDER_DEPLOYED);
	}

	@Override
	public void peterwolfsPlanes$setParagliderDeployed(boolean deployed) {
		Player player = (Player) (Object) this;
		player.getEntityData().set(PETERWOLFS_PLANES_PARAGLIDER_DEPLOYED, deployed);
	}
}
