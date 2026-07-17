package com.ellosan.amazing.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Amazing side hustles on Paper: supply runs (bring stock to any courier)
 * and express reviews (order something and receive it). Paid in dollars.
 */
public class QuestService {
	private record SupplyRequest(Material material, int count, int reward) {
	}

	private static final List<SupplyRequest> SUPPLY_REQUESTS = List.of(
			new SupplyRequest(Material.WHEAT, 16, 18),
			new SupplyRequest(Material.PAPER, 12, 15),
			new SupplyRequest(Material.LEATHER, 6, 21),
			new SupplyRequest(Material.IRON_INGOT, 8, 30),
			new SupplyRequest(Material.COAL, 16, 18),
			new SupplyRequest(Material.OAK_LOG, 24, 18),
			new SupplyRequest(Material.STRING, 12, 15),
			new SupplyRequest(Material.SWEET_BERRIES, 12, 15),
			new SupplyRequest(Material.COPPER_INGOT, 12, 18));

	private final AmazingPlugin plugin;

	public QuestService(AmazingPlugin plugin) {
		this.plugin = plugin;
	}

	public void onCourierInteract(Player player, Villager courier) {
		Bank.Account account = this.plugin.bank().account(player.getUniqueId());

		if (!account.questType.isEmpty()) {
			if (account.questType.equals("supply")) {
				this.tryCompleteSupply(player, courier, account);
			} else {
				say(player, courier, "Order something from /amazing shop and receive it! ($"
						+ account.questReward + ")");
			}
			return;
		}

		// Offer a new quest.
		if (ThreadLocalRandom.current().nextBoolean()) {
			SupplyRequest request = SUPPLY_REQUESTS.get(
					ThreadLocalRandom.current().nextInt(SUPPLY_REQUESTS.size()));
			account.questType = "supply";
			account.questItem = request.material().name();
			account.questCount = request.count();
			account.questReward = request.reward();
			say(player, courier, "The warehouse is running low! Bring me " + request.count() + "x "
					+ request.material().name().toLowerCase().replace('_', ' ')
					+ " and I'll pay you $" + request.reward + ".");
		} else {
			account.questType = "express";
			account.questReward = 25;
			say(player, courier, "Corporate wants five-star reviews! Order anything from /amazing shop "
					+ "and receive it. $25 for your trouble.");
		}
		this.plugin.bank().save();
		player.sendMessage(Component.text("New Amazing quest! Check it with /amazing quest",
				NamedTextColor.LIGHT_PURPLE));
	}

	private void tryCompleteSupply(Player player, Villager courier, Bank.Account account) {
		Material material = Material.matchMaterial(account.questItem);
		if (material == null) {
			account.questType = "";
			this.plugin.bank().save();
			return;
		}
		if (!player.getInventory().containsAtLeast(new ItemStack(material), account.questCount)) {
			say(player, courier, "Not enough yet — I need " + account.questCount + "x "
					+ material.name().toLowerCase().replace('_', ' ') + ".");
			return;
		}
		player.getInventory().removeItem(new ItemStack(material, account.questCount));
		courier.swingMainHand();
		say(player, courier, "Perfect, the warehouse thanks you!");
		this.complete(player, account);
	}

	public void onDeliveryReceived(Player player) {
		Bank.Account account = this.plugin.bank().account(player.getUniqueId());
		if (account.questType.equals("express")) {
			this.complete(player, account);
		}
	}

	private void complete(Player player, Bank.Account account) {
		int reward = account.questReward;
		account.questType = "";
		account.questItem = "";
		account.questCount = 0;
		account.questReward = 0;
		account.questsCompleted++;
		this.plugin.bank().deposit(player, reward);

		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
		player.sendMessage(Component.text("[Amazing] ", NamedTextColor.GOLD)
				.append(Component.text("Quest complete! +$" + reward + " paid to your account. ("
						+ account.questsCompleted + " done)", NamedTextColor.GREEN)));
	}

	public Component describe(Player player) {
		Bank.Account account = this.plugin.bank().account(player.getUniqueId());
		if (account.questType.isEmpty()) {
			return Component.text("No active quest. Find an Amazing courier and ask for work!",
					NamedTextColor.GRAY);
		}
		String text = account.questType.equals("supply")
				? "Supply run: bring " + account.questCount + "x "
						+ account.questItem.toLowerCase().replace('_', ' ') + " to any Amazing courier."
				: "Express review: order from /amazing shop and receive the delivery.";
		return Component.text(text + " Reward: $" + account.questReward + ".", NamedTextColor.YELLOW);
	}

	private static void say(Player player, Villager courier, String message) {
		player.sendMessage(Component.text("<" + DeliveryService.plainName(courier) + "> ", NamedTextColor.AQUA)
				.append(Component.text(message, NamedTextColor.WHITE)));
		player.playSound(courier.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.2f);
	}
}
