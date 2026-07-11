package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: HONK! Broadcast the van horn to everyone nearby. */
public record HonkPayload() implements CustomPayload {
	public static final CustomPayload.Id<HonkPayload> ID = new CustomPayload.Id<>(AmazingMod.id("honk"));

	public static final PacketCodec<RegistryByteBuf, HonkPayload> CODEC = PacketCodec.unit(new HonkPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
