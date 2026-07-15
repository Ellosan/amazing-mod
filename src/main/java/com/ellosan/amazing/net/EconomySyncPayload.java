package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

/** Server → client: bank balance, Prime days left, order tracking, quest info. */
public record EconomySyncPayload(int balance, int primeDaysLeft, List<String> orders, String quest)
		implements CustomPayload {
	public static final CustomPayload.Id<EconomySyncPayload> ID = new CustomPayload.Id<>(AmazingMod.id("economy_sync"));

	public static final PacketCodec<RegistryByteBuf, EconomySyncPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, EconomySyncPayload::balance,
			PacketCodecs.VAR_INT, EconomySyncPayload::primeDaysLeft,
			PacketCodecs.STRING.collect(PacketCodecs.toList()), EconomySyncPayload::orders,
			PacketCodecs.STRING, EconomySyncPayload::quest,
			EconomySyncPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
