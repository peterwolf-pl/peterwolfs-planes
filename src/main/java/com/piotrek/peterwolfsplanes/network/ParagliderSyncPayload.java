package com.piotrek.peterwolfsplanes.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ParagliderSyncPayload(int entityId, boolean deployed) implements CustomPacketPayload {
	public static final Identifier ID = Identifier.fromNamespaceAndPath("peterwolfs_planes", "paraglider_sync");
	public static final CustomPacketPayload.Type<ParagliderSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ParagliderSyncPayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, ParagliderSyncPayload>() {
		@Override
		public ParagliderSyncPayload decode(RegistryFriendlyByteBuf buf) {
			return new ParagliderSyncPayload(buf.readInt(), buf.readBoolean());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, ParagliderSyncPayload value) {
			buf.writeInt(value.entityId);
			buf.writeBoolean(value.deployed);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
