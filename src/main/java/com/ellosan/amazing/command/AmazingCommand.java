package com.ellosan.amazing.command;

import com.ellosan.amazing.delivery.AmazingWorldState;
import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.net.OpenCatalogPayload;
import com.ellosan.amazing.quest.QuestManager;
import com.ellosan.amazing.shop.ProductCatalog;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /amazing — shop, orders, quest, quest abandon
 */
public final class AmazingCommand {
	private AmazingCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("amazing")
				.executes(context -> help(context.getSource()))
				.then(CommandManager.literal("shop").executes(context -> {
					ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
					ServerPlayNetworking.send(player, new OpenCatalogPayload());
					return 1;
				}))
				.then(CommandManager.literal("orders").executes(context -> {
					ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
					AmazingWorldState state = AmazingWorldState.get(player.getServer());
					var mine = state.orders.stream()
							.filter(order -> order.customer.equals(player.getUuid()))
							.toList();
					if (mine.isEmpty()) {
						player.sendMessage(Text.literal("[Amazing] No deliveries on the way. Time to shop!")
								.formatted(Formatting.GOLD), false);
					} else {
						player.sendMessage(Text.literal("[Amazing] Your deliveries:")
								.formatted(Formatting.GOLD), false);
						for (var order : mine) {
							var product = ProductCatalog.byId(order.productId);
							String name = product != null ? product.name() : order.productId;
							player.sendMessage(Text.literal("  • " + order.quantity + "x " + name
									+ " — ETA " + Math.max(0, order.ticksLeft / 20) + "s")
									.formatted(Formatting.YELLOW), false);
						}
					}
					return 1;
				}))
				.then(CommandManager.literal("quest")
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							AmazingWorldState state = AmazingWorldState.get(player.getServer());
							player.sendMessage(QuestManager.describeQuest(state.questData(player.getUuid())), false);
							return 1;
						})
						.then(CommandManager.literal("abandon").executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							AmazingWorldState state = AmazingWorldState.get(player.getServer());
							AmazingWorldState.QuestData data = state.questData(player.getUuid());
							if (data.hasActiveQuest()) {
								data.clearActive();
								state.markDirty();
								player.sendMessage(Text.literal("[Amazing] Quest abandoned. HR is disappointed.")
										.formatted(Formatting.GRAY), false);
							} else {
								player.sendMessage(Text.literal("[Amazing] You have no active quest.")
										.formatted(Formatting.GRAY), false);
							}
							return 1;
						})))
				.then(CommandManager.literal("balance").executes(context -> {
					ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
					player.sendMessage(Text.literal("[Amazing] Wallet: " + DeliveryManager.countEmeralds(player)
							+ " emeralds" + (DeliveryManager.hasPrimeCard(player) ? " • Prime member ✔" : ""))
							.formatted(Formatting.GOLD), false);
					return 1;
				})));
	}

	private static int help(ServerCommandSource source) {
		source.sendFeedback(() -> Text.literal("— Amazing™, Earth's Blockiest Store —").formatted(Formatting.GOLD), false);
		source.sendFeedback(() -> Text.literal("/amazing shop — browse the catalog (or press O)").formatted(Formatting.YELLOW), false);
		source.sendFeedback(() -> Text.literal("/amazing orders — track your deliveries").formatted(Formatting.YELLOW), false);
		source.sendFeedback(() -> Text.literal("/amazing quest — view or abandon your quest").formatted(Formatting.YELLOW), false);
		source.sendFeedback(() -> Text.literal("/amazing balance — check your emerald wallet").formatted(Formatting.YELLOW), false);
		return 1;
	}
}
