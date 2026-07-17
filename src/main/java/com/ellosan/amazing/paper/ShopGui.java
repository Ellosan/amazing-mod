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

import java.util.ArrayList;
import java.util.List;

/**
 * The Amazing catalog as a paged chest GUI. Click a product to order it —
 * a courier will be dispatched with your package.
 */
public class ShopGui implements InventoryHolder {
	private static final int PAGE_SIZE = 45;

	private final AmazingPlugin plugin;
	private final int page;
	private Inventory inventory;

	private ShopGui(AmazingPlugin plugin, int page) {
		this.plugin = plugin;
		this.page = page;
	}

	public static void open(AmazingPlugin plugin, Player player, int page) {
		int maxPage = (Catalog.ALL.size() - 1) / PAGE_SIZE;
		page = Math.clamp(page, 0, maxPage);
		ShopGui gui = new ShopGui(plugin, page);
		gui.inventory = Bukkit.createInventory(gui, 54,
				Component.text("amazing", NamedTextColor.GOLD, TextDecoration.BOLD)
						.append(Component.text("  Earth's Blockiest Store  [" + (page + 1) + "/"
								+ (maxPage + 1) + "]", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

		boolean prime = plugin.bank().hasPrime(player);
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= Catalog.ALL.size()) {
				break;
			}
			Catalog.Product product = Catalog.ALL.get(index);
			ItemStack icon = new ItemStack(product.material(), Math.max(1, Math.min(product.count(), 64)));
			ItemMeta meta = icon.getItemMeta();
			meta.displayName(Component.text(product.name(), NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("$" + product.dollars() + (product.prime() ? "  [PRIME]" : ""),
					product.prime() && !prime ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GREEN)
					.decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Click to order — delivered by courier!", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
			meta.lore(lore);
			icon.setItemMeta(meta);
			gui.inventory.setItem(i, icon);
		}

		gui.inventory.setItem(45, navItem(Material.ARROW, "< Previous page"));
		gui.inventory.setItem(53, navItem(Material.ARROW, "Next page >"));
		gui.inventory.setItem(49, navItem(Material.EMERALD,
				"Balance: $" + plugin.bank().balance(player) + (prime ? "  •  PRIME ★" : "")));

		player.openInventory(gui.inventory);
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
	}

	private static ItemStack navItem(Material material, String label) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(label, NamedTextColor.YELLOW)
				.decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);
		return item;
	}

	public void handleClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		int slot = event.getRawSlot();
		if (slot == 45) {
			open(this.plugin, player, this.page - 1);
			return;
		}
		if (slot == 53) {
			open(this.plugin, player, this.page + 1);
			return;
		}
		if (slot < 0 || slot >= PAGE_SIZE) {
			return;
		}
		int index = this.page * PAGE_SIZE + slot;
		if (index >= Catalog.ALL.size()) {
			return;
		}
		player.closeInventory();
		this.plugin.deliveries().placeOrder(player, Catalog.ALL.get(index));
	}

	@Override
	public Inventory getInventory() {
		return this.inventory;
	}
}
