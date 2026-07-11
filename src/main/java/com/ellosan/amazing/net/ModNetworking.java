package com.ellosan.amazing.net;

import com.ellosan.amazing.delivery.DeliveryManager;
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
		PayloadTypeRegistry.playS2C().register(OpenCatalogPayload.ID, OpenCatalogPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OrderPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			DeliveryManager.placeOrder(player, payload.productId(), payload.quantity());
		});

		ServerPlayNetworking.registerGlobalReceiver(HonkPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player.getVehicle() instanceof VanEntity van) {
				van.getWorld().playSound(null, van.getBlockPos(),
						SoundEvents.GOAT_HORN_SOUNDS.get(0).value(), SoundCategory.NEUTRAL, 1.2f, 1.6f);
			}
		});
	}
}
