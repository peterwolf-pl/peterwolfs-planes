package com.piotrek.peterwolfsplanes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlaneInputPayload(
	float throttle,
	float rudder,
	boolean combatMode,
	boolean fireGuns,
	boolean dropBomb
) implements CustomPacketPayload {
	public static final Identifier ID = Identifier.fromNamespaceAndPath("peterwolfs_planes", "plane_input");
	public static final CustomPacketPayload.Type<PlaneInputPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, PlaneInputPayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, PlaneInputPayload>() {
		@Override
		public PlaneInputPayload decode(RegistryFriendlyByteBuf buf) {
			return new PlaneInputPayload(
				buf.readFloat(),
				buf.readFloat(),
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readBoolean()
			);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, PlaneInputPayload value) {
			buf.writeFloat(value.throttle);
			buf.writeFloat(value.rudder);
			buf.writeBoolean(value.combatMode);
			buf.writeBoolean(value.fireGuns);
			buf.writeBoolean(value.dropBomb);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
