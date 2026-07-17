package com.ellosan.amazing.paper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MineBank™ for Paper: dollar accounts, PDC-tagged cash banknotes, and the
 * $20/30-day Prime subscription. Persists to accounts.json.
 */
public class Bank {
	public static final int STARTING_BALANCE = 250;
	public static final int PRIME_PRICE = 20;
	public static final long PRIME_DURATION_TICKS = 30L * 24000L;
	public static final int EMERALD_SELL_PRICE = 5;

	public static class Account {
		public int balance = STARTING_BALANCE;
		public long primeUntil;
		public String questType = "";
		public String questItem = "";
		public int questCount;
		public int questReward;
		public int questsCompleted;
	}

	private final AmazingPlugin plugin;
	private final NamespacedKey cashKey;
	private final File file;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Map<UUID, Account> accounts = new HashMap<>();

	public Bank(AmazingPlugin plugin) {
		this.plugin = plugin;
		this.cashKey = new NamespacedKey(plugin, "cash");
		this.file = new File(plugin.getDataFolder(), "accounts.json");
		this.load();
	}

	public Account account(UUID uuid) {
		return this.accounts.computeIfAbsent(uuid, ignored -> new Account());
	}

	public int balance(Player player) {
		return this.account(player.getUniqueId()).balance;
	}

	public boolean hasPrime(Player player) {
		return player.getGameMode() == org.bukkit.GameMode.CREATIVE
				|| this.account(player.getUniqueId()).primeUntil > player.getWorld().getGameTime();
	}

	public int primeDaysLeft(Player player) {
		long left = this.account(player.getUniqueId()).primeUntil - player.getWorld().getGameTime();
		return left <= 0 ? 0 : (int) Math.ceil(left / 24000.0);
	}

	public boolean charge(Player player, int amount) {
		Account account = this.account(player.getUniqueId());
		if (account.balance < amount) {
			return false;
		}
		account.balance -= amount;
		this.save();
		return true;
	}

	public void deposit(Player player, int amount) {
		this.account(player.getUniqueId()).balance += amount;
		this.save();
	}

	// ------------------------------------------------------------------
	// Cash + operations
	// ------------------------------------------------------------------

	public ItemStack createCash(int amount) {
		ItemStack cash = new ItemStack(Material.PAPER, amount);
		ItemMeta meta = cash.getItemMeta();
		meta.displayName(Component.text("MineBank Dollar", NamedTextColor.GREEN));
		meta.lore(List.of(Component.text("Worth $1 — deposit via /bank", NamedTextColor.GRAY),
				Component.text("\"In Blocks We Trust\"", NamedTextColor.DARK_GRAY)));
		meta.getPersistentDataContainer().set(this.cashKey, PersistentDataType.BYTE, (byte) 1);
		cash.setItemMeta(meta);
		return cash;
	}

	private boolean isCash(ItemStack stack) {
		return stack != null && stack.getType() == Material.PAPER && stack.hasItemMeta()
				&& stack.getItemMeta().getPersistentDataContainer().has(this.cashKey, PersistentDataType.BYTE);
	}

	public void withdraw(Player player, int amount) {
		amount = Math.min(amount, 256);
		if (amount <= 0 || !this.charge(player, amount)) {
			fail(player, "Insufficient funds.");
			return;
		}
		int remaining = amount;
		while (remaining > 0) {
			int size = Math.min(remaining, 64);
			var leftover = player.getInventory().addItem(this.createCash(size));
			leftover.values().forEach(stack ->
					player.getWorld().dropItemNaturally(player.getLocation(), stack));
			remaining -= size;
		}
		ok(player, "Withdrew $" + amount + ".");
	}

	public void depositCash(Player player) {
		int total = 0;
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			if (this.isCash(contents[i])) {
				total += contents[i].getAmount();
				player.getInventory().setItem(i, null);
			}
		}
		if (total == 0) {
			fail(player, "No MineBank Dollars to deposit.");
			return;
		}
		this.deposit(player, total);
		ok(player, "Deposited $" + total + ".");
	}

	public void sellEmeralds(Player player) {
		int total = 0;
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if (stack != null && stack.getType() == Material.EMERALD && !stack.hasItemMeta()) {
				total += stack.getAmount();
				player.getInventory().setItem(i, null);
			}
		}
		if (total == 0) {
			fail(player, "No emeralds to sell.");
			return;
		}
		this.deposit(player, total * EMERALD_SELL_PRICE);
		ok(player, "Sold " + total + " emeralds for $" + (total * EMERALD_SELL_PRICE) + ".");
	}

	public void subscribePrime(Player player) {
		if (!this.charge(player, PRIME_PRICE)) {
			fail(player, "Prime costs $" + PRIME_PRICE + " — insufficient funds.");
			return;
		}
		Account account = this.account(player.getUniqueId());
		long now = player.getWorld().getGameTime();
		account.primeUntil = Math.max(account.primeUntil, now) + PRIME_DURATION_TICKS;
		this.save();
		ok(player, "Prime active for " + this.primeDaysLeft(player) + " days. Enjoy free exclusives!");
	}

	public void transfer(Player from, Player to, int amount) {
		if (to == null || to == from) {
			fail(from, "That player is not online.");
			return;
		}
		if (amount <= 0 || !this.charge(from, amount)) {
			fail(from, "Insufficient funds.");
			return;
		}
		this.deposit(to, amount);
		ok(from, "Sent $" + amount + " to " + to.getName() + ".");
		to.sendMessage(Component.text("[MineBank] Received $" + amount + " from "
				+ from.getName() + ".", NamedTextColor.GREEN));
	}

	// ------------------------------------------------------------------
	// Persistence
	// ------------------------------------------------------------------

	private void load() {
		if (!this.file.exists()) {
			return;
		}
		try (FileReader reader = new FileReader(this.file)) {
			Type type = new TypeToken<Map<UUID, Account>>() {
			}.getType();
			Map<UUID, Account> loaded = this.gson.fromJson(reader, type);
			if (loaded != null) {
				this.accounts = loaded;
			}
		} catch (IOException e) {
			this.plugin.getLogger().warning("Could not read accounts.json: " + e.getMessage());
		}
	}

	public void save() {
		this.plugin.getDataFolder().mkdirs();
		try (FileWriter writer = new FileWriter(this.file)) {
			this.gson.toJson(this.accounts, writer);
		} catch (IOException e) {
			this.plugin.getLogger().warning("Could not save accounts.json: " + e.getMessage());
		}
	}

	static void ok(Player player, String message) {
		player.sendMessage(Component.text("[MineBank] " + message, NamedTextColor.GREEN));
	}

	static void fail(Player player, String message) {
		player.sendMessage(Component.text("[MineBank] " + message, NamedTextColor.RED));
	}
}
