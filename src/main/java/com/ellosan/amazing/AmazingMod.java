package com.ellosan.amazing;

import com.ellosan.amazing.command.AmazingCommand;
import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.economy.BankManager;
import com.ellosan.amazing.entity.CitizenEntity;
import com.ellosan.amazing.net.ModNetworking;
import com.ellosan.amazing.quest.QuestManager;
import com.ellosan.amazing.registry.ModBlocks;
import com.ellosan.amazing.registry.ModComponents;
import com.ellosan.amazing.registry.ModEntities;
import com.ellosan.amazing.registry.ModItemGroups;
import com.ellosan.amazing.registry.ModItems;
import com.ellosan.amazing.worldgen.CityChunkGenerator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
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
		ModBlocks.register();
		ModItems.register();
		ModEntities.register();
		ModItemGroups.register();
		ModNetworking.registerServer();

		Registry.register(Registries.CHUNK_GENERATOR, id("city"), CityChunkGenerator.CODEC);

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> AmazingCommand.register(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(DeliveryManager::tickServer);

		ServerPlayConnectionEvents.JOIN.register(
				(handler, sender, server) -> BankManager.sync(handler.getPlayer()));

		// Quest packages are handed over BEFORE the villager opens its trade
		// screen (or the citizen starts chatting) — this is what makes
		// courier quests actually deliverable.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!player.getStackInHand(hand).contains(ModComponents.QUEST_TAG)) {
				return ActionResult.PASS;
			}
			if (!(entity instanceof VillagerEntity) && !(entity instanceof CitizenEntity)) {
				return ActionResult.PASS;
			}
			if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
				QuestManager.deliverQuestPackage(serverPlayer, (LivingEntity) entity,
						player.getStackInHand(hand));
			}
			return ActionResult.success(world.isClient);
		});
	}
}
