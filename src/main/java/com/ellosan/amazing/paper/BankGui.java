package com.ellosan.amazing.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** The MineBank menu: balance, withdrawals, deposits, emerald sales, Prime. */
public class BankGui implements InventoryHolder {
	private final AmazingPlugin plugin;
	private Inventory inventory;

	private BankGui(AmazingPlugin plugin) {
		this.plugin = plugin;
	}

	public static void open(AmazingPlugin plugin, Player player) {
		BankGui gui = new BankGui(plugin);
		gui.inventory = Bukkit.createInventory(gui, 27,
				Component.text("MineBank™", NamedTextColor.AQUA, TextDecoration.BOLD));

		boolean prime = plugin.bank().hasPrime(player);
		gui.inventory.setItem(4, item(Material.EMERALD_BLOCK,
				"Balance: $" + plugin.bank().balance(player),
				prime ? "Prime ★ " + plugin.bank().primeDaysLeft(player) + " days left" : "No Prime subscription"));
		gui.inventory.setItem(10, item(Material.PAPER, "Withdraw $10", "Get cash banknotes"));
		gui.inventory.setItem(11, item(Material.PAPER, "Withdraw $50", "Get cash banknotes"));
		gui.inventory.setItem(12, item(Material.PAPER, "Withdraw $100", "Get cash banknotes"));
		gui.inventory.setItem(14, item(Material.CHEST, "Deposit all cash", "Banknotes in your inventory"));
		gui.inventory.setItem(15, item(Material.EMERALD, "Sell emeralds", "$" + Bank.EMERALD_SELL_PRICE + " each"));
		gui.inventory.setItem(16, item(Material.NETHER_STAR, "Subscribe Prime",
				"$" + Bank.PRIME_PRICE + " per 30 in-game days"));

		player.openInventory(gui.inventory);
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.6f, 1.8f);
	}

	private static ItemStack item(Material material, String name, String loreLine) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(Component.text(loreLine, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		item.setItemMeta(meta);
		return item;
	}

	public void handleClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		Bank bank = this.plugin.bank();
		switch (event.getRawSlot()) {
			case 10 -> bank.withdraw(player, 10);
			case 11 -> bank.withdraw(player, 50);
			case 12 -> bank.withdraw(player, 100);
			case 14 -> bank.depositCash(player);
			case 15 -> bank.sellEmeralds(player);
			case 16 -> bank.subscribePrime(player);
			default -> {
				return;
			}
		}
		open(this.plugin, player); // refresh balance display
	}

	@Override
	public Inventory getInventory() {
		return this.inventory;
	}
}
