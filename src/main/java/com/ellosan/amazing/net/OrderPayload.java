package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: "I'd like to buy this, please." */
public record OrderPayload(String productId, int quantity) implements CustomPayload {
	public static final CustomPayload.Id<OrderPayload> ID = new CustomPayload.Id<>(AmazingMod.id("order"));

	public static final PacketCodec<RegistryByteBuf, OrderPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, OrderPayload::productId,
			PacketCodecs.VAR_INT, OrderPayload::quantity,
			OrderPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
