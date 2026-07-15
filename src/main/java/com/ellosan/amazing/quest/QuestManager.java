package com.ellosan.amazing.quest;

import com.ellosan.amazing.delivery.AmazingWorldState;
import com.ellosan.amazing.economy.BankManager;
import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.entity.DeliveryWorkerEntity;
import com.ellosan.amazing.registry.ModComponents;
import com.ellosan.amazing.registry.ModItems;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.UUID;

/**
 * Amazing side hustles. Couriers hand out three kinds of quests:
 *
 * <ul>
 *   <li><b>Courier</b> — hand-deliver a quest package to any villager.</li>
 *   <li><b>Supply</b> — bring requested stock back to any Amazing courier.</li>
 *   <li><b>Express</b> — place an order and receive the delivery.</li>
 * </ul>
 *
 * Every third completed quest earns a milestone bonus (a Prime Card, then
 * your very own van).
 */
public final class QuestManager {
	public static final String TYPE_COURIER = "courier";
	public static final String TYPE_SUPPLY = "supply";
	public static final String TYPE_EXPRESS = "express";

	private record SupplyRequest(Item item, int count, int reward) {
	}

	private static final List<SupplyRequest> SUPPLY_REQUESTS = List.of(
			new SupplyRequest(Items.WHEAT, 16, 6),
			new SupplyRequest(Items.PAPER, 12, 5),
			new SupplyRequest(Items.LEATHER, 6, 7),
			new SupplyRequest(Items.IRON_INGOT, 8, 10),
			new SupplyRequest(Items.COAL, 16, 6),
			new SupplyRequest(Items.OAK_LOG, 24, 6),
			new SupplyRequest(Items.STRING, 12, 5),
			new SupplyRequest(Items.HONEY_BOTTLE, 2, 8),
			new SupplyRequest(Items.SWEET_BERRIES, 12, 5),
			new SupplyRequest(Items.COPPER_INGOT, 12, 6));

	private QuestManager() {
	}

	// ------------------------------------------------------------------
	// Interacting with couriers
	// ------------------------------------------------------------------

	public static void onWorkerInteract(ServerPlayerEntity player, DeliveryWorkerEntity worker) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AmazingWorldState state = AmazingWorldState.get(server);
		AmazingWorldState.QuestData data = state.questData(player.getUuid());

		if (data.hasActiveQuest()) {
			if (data.activeType.equals(TYPE_SUPPLY)) {
				tryCompleteSupplyQuest(player, worker, state, data);
			} else {
				say(player, worker, questReminder(data));
			}
			return;
		}

		offerQuest(player, worker, state, data);
		state.markDirty();
	}

	private static void offerQuest(ServerPlayerEntity player, DeliveryWorkerEntity worker,
			AmazingWorldState state, AmazingWorldState.QuestData data) {
		Random random = player.getRandom();
		int roll = random.nextInt(data.questsCompleted == 0 ? 2 : 3);

		switch (roll) {
			case 0 -> { // Courier quest
				String questId = UUID.randomUUID().toString().substring(0, 8);
				data.activeType = TYPE_COURIER;
				data.questId = questId;
				data.reward = 20 + random.nextInt(21);

				ItemStack questPackage = new ItemStack(ModItems.PACKAGE);
				questPackage.set(ModComponents.QUEST_TAG, questId);
				questPackage.set(ModComponents.ADDRESSEE, "any villager");
				if (!player.giveItemStack(questPackage)) {
					player.dropItem(questPackage, false);
				}

				say(player, worker, "We're swamped! Take this package and hand it to any villager for me. "
						+ "Pay is $" + data.reward + ".");
			}
			case 1 -> { // Supply quest
				SupplyRequest request = SUPPLY_REQUESTS.get(random.nextInt(SUPPLY_REQUESTS.size()));
				data.activeType = TYPE_SUPPLY;
				data.targetItemId = Registries.ITEM.getId(request.item()).toString();
				data.targetCount = request.count();
				data.reward = request.reward() * 3;

				say(player, worker, "The warehouse is running low on stock. Bring me " + request.count()
						+ "x " + request.item().getName().getString() + " and I'll pay you $"
						+ data.reward + ".");
			}
			default -> { // Express quest
				data.activeType = TYPE_EXPRESS;
				data.reward = 25;

				say(player, worker, "Corporate wants five-star reviews! Place any order from our catalog "
						+ "(press O or use a Prime Card) and receive the delivery. $" + data.reward
						+ " for your trouble.");
			}
		}

		player.sendMessage(Text.literal("New Amazing quest! Check it anytime with /amazing quest")
				.formatted(Formatting.LIGHT_PURPLE), false);
	}

	private static String questReminder(AmazingWorldState.QuestData data) {
		return switch (data.activeType) {
			case TYPE_COURIER -> "Still holding my package? Right-click any villager with it! ("
					+ "$" + data.reward + ")";
			case TYPE_SUPPLY -> "Got my " + data.targetCount + "x " + targetItemName(data)
					+ " yet? Bring them to me! (" + "$" + data.reward + ")";
			case TYPE_EXPRESS -> "Order something from the catalog (press O) and receive it! ("
					+ "$" + data.reward + ")";
			default -> "Have a great Amazing day!";
		};
	}

	public static String targetItemName(AmazingWorldState.QuestData data) {
		Identifier id = Identifier.tryParse(data.targetItemId);
		if (id == null) {
			return data.targetItemId;
		}
		return Registries.ITEM.get(id).getName().getString();
	}

	// ------------------------------------------------------------------
	// Completion paths
	// ------------------------------------------------------------------

	private static void tryCompleteSupplyQuest(ServerPlayerEntity player, DeliveryWorkerEntity worker,
			AmazingWorldState state, AmazingWorldState.QuestData data) {
		Identifier id = Identifier.tryParse(data.targetItemId);
		Item item = id != null ? Registries.ITEM.get(id) : null;
		if (item == null) {
			data.clearActive();
			state.markDirty();
			return;
		}

		PlayerInventory inventory = player.getInventory();
		int have = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(item)) {
				have += stack.getCount();
			}
		}

		if (have < data.targetCount) {
			say(player, worker, "That's not enough yet — I need " + data.targetCount + "x "
					+ item.getName().getString() + " (you have " + have + ").");
			return;
		}

		int remaining = data.targetCount;
		for (int i = 0; i < inventory.size() && remaining > 0; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(item)) {
				int take = Math.min(remaining, stack.getCount());
				stack.decrement(take);
				remaining -= take;
			}
		}

		worker.swingHand(net.minecraft.util.Hand.MAIN_HAND);
		say(player, worker, "Perfect, the warehouse thanks you!");
		completeQuest(player, state, data);
	}

	/** Called when a quest package is handed to a villager or citizen. */
	public static void deliverQuestPackage(ServerPlayerEntity player, LivingEntity customer, ItemStack stack) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AmazingWorldState state = AmazingWorldState.get(server);
		AmazingWorldState.QuestData data = state.questData(player.getUuid());

		String questId = stack.get(ModComponents.QUEST_TAG);
		if (!data.activeType.equals(TYPE_COURIER) || questId == null || !questId.equals(data.questId)) {
			player.sendMessage(Text.literal("[Amazing] This package isn't part of your current route.")
					.formatted(Formatting.RED), true);
			return;
		}

		stack.decrement(1);
		if (player.getWorld() instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
					customer.getX(), customer.getY() + 1.5, customer.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
		}
		customer.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);

		completeQuest(player, state, data);
	}

	/** Called when any ordered package reaches the player, for express quests. */
	public static void onDeliveryReceived(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AmazingWorldState state = AmazingWorldState.get(server);
		AmazingWorldState.QuestData data = state.questData(player.getUuid());
		if (data.activeType.equals(TYPE_EXPRESS)) {
			completeQuest(player, state, data);
		}
	}

	private static void completeQuest(ServerPlayerEntity player, AmazingWorldState state,
			AmazingWorldState.QuestData data) {
		int reward = data.reward;
		data.questsCompleted++;
		int completed = data.questsCompleted;
		data.clearActive();
		state.markDirty();

		BankManager.deposit(player, reward);

		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
				SoundCategory.PLAYERS, 0.8f, 1.2f);
		player.sendMessage(Text.literal("[Amazing] ").formatted(Formatting.GOLD)
				.append(Text.literal("Quest complete! +$" + reward + " paid to your MineBank account. ("
						+ completed + " quests done)").formatted(Formatting.GREEN)), false);

		// Milestone bonuses.
		if (completed == 3) {
			ItemStack card = new ItemStack(ModItems.PRIME_CARD);
			if (!player.giveItemStack(card)) {
				player.dropItem(card, false);
			}
			player.sendMessage(Text.literal("[Amazing] MILESTONE: You've earned a free Prime Card! Welcome to the family.")
					.formatted(Formatting.LIGHT_PURPLE), false);
		} else if (completed == 6) {
			ItemStack van = new ItemStack(ModItems.VAN);
			if (!player.giveItemStack(van)) {
				player.dropItem(van, false);
			}
			player.sendMessage(Text.literal("[Amazing] MILESTONE: Employee of the month! Here's your very own Amazing van.")
					.formatted(Formatting.LIGHT_PURPLE), false);
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private static void say(ServerPlayerEntity player, DeliveryWorkerEntity worker, String message) {
		player.sendMessage(Text.literal("<" + worker.getName().getString() + "> ")
				.formatted(Formatting.AQUA)
				.append(Text.literal(message).formatted(Formatting.WHITE)), false);
		worker.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, 1.0f, 1.2f);
	}

	public static Text describeQuest(AmazingWorldState.QuestData data) {
		if (!data.hasActiveQuest()) {
			return Text.literal("No active quest. Find an Amazing courier and ask for work!")
					.formatted(Formatting.GRAY);
		}
		String description = switch (data.activeType) {
			case TYPE_COURIER -> "Courier run: deliver the quest package to any villager (right-click them).";
			case TYPE_SUPPLY -> "Supply run: bring " + data.targetCount + "x " + targetItemName(data)
					+ " to any Amazing courier.";
			case TYPE_EXPRESS -> "Express review: place a catalog order and receive the delivery.";
			default -> data.activeType;
		};
		return Text.literal(description + " Reward: $" + data.reward + ".")
				.formatted(Formatting.YELLOW);
	}
}
