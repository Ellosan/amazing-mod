package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: open the AmazingPhone screen. */
public record OpenPhonePayload() implements CustomPayload {
	public static final CustomPayload.Id<OpenPhonePayload> ID = new CustomPayload.Id<>(AmazingMod.id("open_phone"));

	public static final PacketCodec<RegistryByteBuf, OpenPhonePayload> CODEC = PacketCodec.unit(new OpenPhonePayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
