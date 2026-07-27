package com.piotrek.peterwolfsplanes.network;

import com.piotrek.peterwolfsplanes.ParagliderFlightMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ParagliderInputPayload(ParagliderFlightMode mode) implements CustomPacketPayload {
	public static final Identifier ID = Identifier.fromNamespaceAndPath("peterwolfs_planes", "paraglider_input");
	public static final CustomPacketPayload.Type<ParagliderInputPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ParagliderInputPayload> CODEC =
		new StreamCodec<RegistryFriendlyByteBuf, ParagliderInputPayload>() {
			@Override
			public ParagliderInputPayload decode(RegistryFriendlyByteBuf buf) {
				return new ParagliderInputPayload(ParagliderFlightMode.fromNetworkId(buf.readUnsignedByte()));
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, ParagliderInputPayload value) {
				buf.writeByte(value.mode().networkId());
			}
		};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
