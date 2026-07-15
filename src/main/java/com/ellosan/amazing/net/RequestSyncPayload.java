package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: "refresh my economy/order/quest data" (e.g. when opening the phone). */
public record RequestSyncPayload() implements CustomPayload {
	public static final CustomPayload.Id<RequestSyncPayload> ID = new CustomPayload.Id<>(AmazingMod.id("request_sync"));

	public static final PacketCodec<RegistryByteBuf, RequestSyncPayload> CODEC =
			PacketCodec.unit(new RequestSyncPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
