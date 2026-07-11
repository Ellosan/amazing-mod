package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: open the Amazing catalog screen. */
public record OpenCatalogPayload() implements CustomPayload {
	public static final CustomPayload.Id<OpenCatalogPayload> ID = new CustomPayload.Id<>(AmazingMod.id("open_catalog"));

	public static final PacketCodec<RegistryByteBuf, OpenCatalogPayload> CODEC =
			PacketCodec.unit(new OpenCatalogPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
