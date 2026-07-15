package com.ellosan.amazing.net;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client → server: a banking operation from the ATM or phone.
 * Ops: withdraw, deposit_cash, sell_emeralds, prime, transfer.
 */
public record BankOpPayload(String op, int amount, String target) implements CustomPayload {
	public static final CustomPayload.Id<BankOpPayload> ID = new CustomPayload.Id<>(AmazingMod.id("bank_op"));

	public static final PacketCodec<RegistryByteBuf, BankOpPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, BankOpPayload::op,
			PacketCodecs.VAR_INT, BankOpPayload::amount,
			PacketCodecs.STRING, BankOpPayload::target,
			BankOpPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
