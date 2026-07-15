package com.ellosan.amazing.economy;

import com.ellosan.amazing.delivery.AmazingWorldState;
import com.ellosan.amazing.net.EconomySyncPayload;
import com.ellosan.amazing.quest.QuestManager;
import com.ellosan.amazing.registry.ModItems;
import com.ellosan.amazing.shop.Product;
import com.ellosan.amazing.shop.ProductCatalog;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * MineBank™ — Amazing's in-house bank. Every player has an account holding
 * dollars; cash exists as banknote items via ATMs; Prime is a $20
 * subscription that lasts 30 in-game days.
 */
public final class BankManager {
	public static final int PRIME_PRICE = 20;
	public static final long PRIME_DURATION_TICKS = 30L * 24000L;
	public static final int EMERALD_SELL_PRICE = 5;

	private BankManager() {
	}

	private static AmazingWorldState.BankAccount account(ServerPlayerEntity player) {
		return AmazingWorldState.get(player.getServer()).account(player.getUuid());
	}

	private static void dirty(ServerPlayerEntity player) {
		AmazingWorldState.get(player.getServer()).markDirty();
		sync(player);
	}

	public static int balance(ServerPlayerEntity player) {
		return account(player).balance;
	}

	public static boolean hasPrime(ServerPlayerEntity player) {
		return player.getAbilities().creativeMode
				|| account(player).primeUntil > player.getServerWorld().getTime();
	}

	public static int primeDaysLeft(ServerPlayerEntity player) {
		long left = account(player).primeUntil - player.getServerWorld().getTime();
		return left <= 0 ? 0 : (int) Math.ceil(left / 24000.0);
	}

	/** Withdraws from the account. Returns false when funds are insufficient. */
	public static boolean charge(ServerPlayerEntity player, int amount) {
		AmazingWorldState.BankAccount account = account(player);
		if (account.balance < amount) {
			return false;
		}
		account.balance -= amount;
		dirty(player);
		return true;
	}

	public static void deposit(ServerPlayerEntity player, int amount) {
		account(player).balance += amount;
		dirty(player);
	}

	// ------------------------------------------------------------------
	// ATM / e-banking operations (validated server-side)
	// ------------------------------------------------------------------

	public static void withdrawCash(ServerPlayerEntity player, int amount) {
		amount = Math.min(amount, 64 * 4);
		if (amount <= 0 || !charge(player, amount)) {
			fail(player, "Insufficient funds.");
			return;
		}
		int remaining = amount;
		while (remaining > 0) {
			int size = Math.min(remaining, 64);
			ItemStack cash = new ItemStack(ModItems.CASH, size);
			if (!player.giveItemStack(cash)) {
				player.dropItem(cash, false);
			}
			remaining -= size;
		}
		ok(player, "Withdrew $" + amount + ".");
	}

	public static void depositCash(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		int total = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(ModItems.CASH)) {
				total += stack.getCount();
				stack.setCount(0);
			}
		}
		if (total == 0) {
			fail(player, "No cash to deposit.");
			return;
		}
		deposit(player, total);
		ok(player, "Deposited $" + total + ".");
	}

	public static void sellEmeralds(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();
		int total = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(Items.EMERALD)) {
				total += stack.getCount();
				stack.setCount(0);
			}
		}
		if (total == 0) {
			fail(player, "No emeralds to sell.");
			return;
		}
		deposit(player, total * EMERALD_SELL_PRICE);
		ok(player, "Sold " + total + " emeralds for $" + (total * EMERALD_SELL_PRICE) + ".");
	}

	public static void subscribePrime(ServerPlayerEntity player) {
		if (!charge(player, PRIME_PRICE)) {
			fail(player, "Prime costs $" + PRIME_PRICE + " — insufficient funds.");
			return;
		}
		addPrimeTime(player);
		ok(player, "Prime active for " + primeDaysLeft(player) + " more days. Enjoy free exclusives!");
	}

	/** Extends Prime by 30 in-game days (used by subscription and Prime Card). */
	public static void addPrimeTime(ServerPlayerEntity player) {
		AmazingWorldState.BankAccount account = account(player);
		long now = player.getServerWorld().getTime();
		account.primeUntil = Math.max(account.primeUntil, now) + PRIME_DURATION_TICKS;
		dirty(player);
	}

	public static void transfer(ServerPlayerEntity player, String targetName, int amount) {
		MinecraftServer server = player.getServer();
		ServerPlayerEntity target = server != null ? server.getPlayerManager().getPlayer(targetName) : null;
		if (target == null || target == player) {
			fail(player, "Player \"" + targetName + "\" is not online.");
			return;
		}
		if (amount <= 0 || !charge(player, amount)) {
			fail(player, "Insufficient funds.");
			return;
		}
		deposit(target, amount);
		ok(player, "Sent $" + amount + " to " + targetName + ".");
		target.sendMessage(Text.literal("[MineBank] Received $" + amount + " from "
				+ player.getName().getString() + ".").formatted(Formatting.GREEN), false);
	}

	// ------------------------------------------------------------------
	// Client sync
	// ------------------------------------------------------------------

	/** Pushes balance, Prime status, order tracking and quest info to the client. */
	public static void sync(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AmazingWorldState state = AmazingWorldState.get(server);

		List<String> orders = new ArrayList<>();
		for (AmazingWorldState.PendingOrder order : state.orders) {
			if (order.customer.equals(player.getUuid())) {
				Product product = ProductCatalog.byId(order.productId);
				String name = product != null ? product.name() : order.productId;
				orders.add(order.quantity + "x " + name + " — ETA " + Math.max(0, order.ticksLeft / 20) + "s");
			}
		}

		String quest = QuestManager.describeQuest(state.questData(player.getUuid())).getString();

		ServerPlayNetworking.send(player, new EconomySyncPayload(
				balance(player), primeDaysLeft(player), orders, quest));
	}

	private static void ok(ServerPlayerEntity player, String message) {
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
				SoundCategory.PLAYERS, 0.5f, 1.6f);
		player.sendMessage(Text.literal("[MineBank] " + message).formatted(Formatting.GREEN), false);
	}

	private static void fail(ServerPlayerEntity player, String message) {
		player.sendMessage(Text.literal("[MineBank] " + message).formatted(Formatting.RED), false);
	}
}
