package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client → server: a message sent from the phone's Chat app.
 * Empty target = broadcast to everyone; otherwise a DM to that player.
 */
public record PhoneChatPayload(String target, String message) implements CustomPayload {
	public static final CustomPayload.Id<PhoneChatPayload> ID = new CustomPayload.Id<>(AmazingMod.id("phone_chat"));

	public static final PacketCodec<RegistryByteBuf, PhoneChatPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PhoneChatPayload::target,
			PacketCodecs.STRING, PhoneChatPayload::message,
			PhoneChatPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
