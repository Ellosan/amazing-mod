package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: open the ATM screen. */
public record OpenAtmPayload() implements CustomPayload {
	public static final CustomPayload.Id<OpenAtmPayload> ID = new CustomPayload.Id<>(AmazingMod.id("open_atm"));

	public static final PacketCodec<RegistryByteBuf, OpenAtmPayload> CODEC = PacketCodec.unit(new OpenAtmPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
