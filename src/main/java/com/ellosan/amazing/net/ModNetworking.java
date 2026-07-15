package com.ellosan.amazing.net;

import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.economy.BankManager;
import com.ellosan.amazing.entity.VanEntity;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Registers payload types and the server-side receivers. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void registerServer() {
		PayloadTypeRegistry.playC2S().register(OrderPayload.ID, OrderPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(HonkPayload.ID, HonkPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(BankOpPayload.ID, BankOpPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestSyncPayload.ID, RequestSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenCatalogPayload.ID, OpenCatalogPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EconomySyncPayload.ID, EconomySyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenAtmPayload.ID, OpenAtmPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenPhonePayload.ID, OpenPhonePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OrderPayload.ID, (payload, context) ->
				DeliveryManager.placeOrder(context.player(), payload.productId(), payload.quantity()));

		ServerPlayNetworking.registerGlobalReceiver(HonkPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player.getVehicle() instanceof VanEntity van) {
				van.getWorld().playSound(null, van.getBlockPos(),
						SoundEvents.GOAT_HORN_SOUNDS.get(0).value(), SoundCategory.NEUTRAL, 1.2f, 1.6f);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(RequestSyncPayload.ID, (payload, context) ->
				BankManager.sync(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(BankOpPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			switch (payload.op()) {
				case "withdraw" -> BankManager.withdrawCash(player, payload.amount());
				case "deposit_cash" -> BankManager.depositCash(player);
				case "sell_emeralds" -> BankManager.sellEmeralds(player);
				case "prime" -> BankManager.subscribePrime(player);
				case "transfer" -> BankManager.transfer(player, payload.target(), payload.amount());
				default -> {
				}
			}
		});
	}
}
