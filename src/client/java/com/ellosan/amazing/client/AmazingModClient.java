package com.ellosan.amazing.client;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.client.render.CitizenRenderer;
import com.ellosan.amazing.client.render.DeliveryWorkerRenderer;
import com.ellosan.amazing.client.render.VanEntityModel;
import com.ellosan.amazing.client.render.VanEntityRenderer;
import com.ellosan.amazing.client.screen.AtmScreen;
import com.ellosan.amazing.client.screen.CatalogScreen;
import com.ellosan.amazing.client.screen.PhoneScreen;
import com.ellosan.amazing.entity.VanEntity;
import com.ellosan.amazing.net.EconomySyncPayload;
import com.ellosan.amazing.net.HonkPayload;
import com.ellosan.amazing.net.OpenAtmPayload;
import com.ellosan.amazing.net.OpenCatalogPayload;
import com.ellosan.amazing.net.OpenPhonePayload;
import com.ellosan.amazing.registry.ModEntities;

import net.minecraft.client.render.entity.EmptyEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class AmazingModClient implements ClientModInitializer {
	public static final EntityModelLayer VAN_LAYER = new EntityModelLayer(AmazingMod.id("van"), "main");
	public static final EntityModelLayer WORKER_LAYER = new EntityModelLayer(AmazingMod.id("delivery_worker"), "main");

	public static final Identifier VAN_TEXTURE = AmazingMod.id("textures/entity/van.png");
	public static final Identifier DELIVERY_VAN_TEXTURE = AmazingMod.id("textures/entity/delivery_van.png");
	public static final Identifier CAR_TEXTURE = AmazingMod.id("textures/entity/car.png");

	private static KeyBinding catalogKey;
	private static KeyBinding honkKey;

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(VAN_LAYER, VanEntityModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(WORKER_LAYER,
				() -> TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.0f), 64, 32));

		EntityRendererRegistry.register(ModEntities.VAN,
				context -> new VanEntityRenderer(context, VAN_TEXTURE));
		EntityRendererRegistry.register(ModEntities.DELIVERY_VAN,
				context -> new VanEntityRenderer(context, DELIVERY_VAN_TEXTURE));
		EntityRendererRegistry.register(ModEntities.CAR,
				context -> new VanEntityRenderer(context, CAR_TEXTURE, 0.85f));
		EntityRendererRegistry.register(ModEntities.DELIVERY_WORKER, DeliveryWorkerRenderer::new);
		EntityRendererRegistry.register(ModEntities.CITIZEN, CitizenRenderer::new);
		EntityRendererRegistry.register(ModEntities.SEAT, EmptyEntityRenderer::new);

		catalogKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.amazing.catalog", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.amazing"));
		honkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.amazing.honk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.amazing"));

		ClientPlayNetworking.registerGlobalReceiver(OpenCatalogPayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new CatalogScreen())));
		ClientPlayNetworking.registerGlobalReceiver(OpenAtmPayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new AtmScreen())));
		ClientPlayNetworking.registerGlobalReceiver(OpenPhonePayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new PhoneScreen())));
		ClientPlayNetworking.registerGlobalReceiver(EconomySyncPayload.ID, (payload, context) ->
				context.client().execute(() -> {
					ClientEconomy.balance = payload.balance();
					ClientEconomy.primeDaysLeft = payload.primeDaysLeft();
					ClientEconomy.orders = payload.orders();
					ClientEconomy.quest = payload.quest();
				}));

		ClientTickEvents.END_CLIENT_TICK.register(AmazingModClient::onClientTick);
	}

	private static void onClientTick(MinecraftClient client) {
		while (catalogKey.wasPressed()) {
			if (client.player != null && client.currentScreen == null) {
				client.setScreen(new CatalogScreen());
			}
		}

		if (client.player == null) {
			return;
		}

		if (client.player.getVehicle() instanceof VanEntity van) {
			van.setInputs(
					client.options.forwardKey.isPressed(),
					client.options.backKey.isPressed(),
					client.options.leftKey.isPressed(),
					client.options.rightKey.isPressed(),
					client.options.jumpKey.isPressed());

			while (honkKey.wasPressed()) {
				ClientPlayNetworking.send(new HonkPayload());
			}

			// A humble chugging engine.
			float speed = Math.abs(van.getSpeed());
			if (speed > 0.05f && client.player.age % 6 == 0) {
				client.player.playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP,
						0.15f + speed * 0.4f, 0.5f + speed * 1.2f);
			}
		} else {
			// Drain any queued honks so they don't fire when re-entering a van.
			while (honkKey.wasPressed()) {
			}
		}
	}
}
