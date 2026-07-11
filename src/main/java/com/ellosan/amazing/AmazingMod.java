package com.ellosan.amazing;

import com.ellosan.amazing.command.AmazingCommand;
import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.net.ModNetworking;
import com.ellosan.amazing.registry.ModComponents;
import com.ellosan.amazing.registry.ModEntities;
import com.ellosan.amazing.registry.ModItemGroups;
import com.ellosan.amazing.registry.ModItems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmazingMod implements ModInitializer {
	public static final String MOD_ID = "amazing";
	public static final Logger LOGGER = LoggerFactory.getLogger("Amazing");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Amazing™ is open for business. Earth's Blockiest Store!");

		ModComponents.register();
		ModItems.register();
		ModEntities.register();
		ModItemGroups.register();
		ModNetworking.registerServer();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> AmazingCommand.register(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(DeliveryManager::tickServer);
	}
}
