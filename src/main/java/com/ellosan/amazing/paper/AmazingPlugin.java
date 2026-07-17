package com.ellosan.amazing.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Amazing™ for Paper — Earth's Blockiest Store as a server-side plugin.
 * Vanilla clients welcome: shop GUI, MineBank economy, courier deliveries,
 * quests, lodestone elevators, and the Amazing City world generator.
 */
public class AmazingPlugin extends JavaPlugin implements Listener {
	private Bank bank;
	private DeliveryService deliveries;
	private QuestService quests;
	private NamespacedKey barrelFilledKey;

	@Override
	public void onEnable() {
		this.bank = new Bank(this);
		this.deliveries = new DeliveryService(this);
		this.quests = new QuestService(this);
		this.barrelFilledKey = new NamespacedKey(this, "filled");

		this.getServer().getPluginManager().registerEvents(this, this);

		// Ambient courier visits to villagers.
		this.getServer().getScheduler().runTaskTimer(this, this.deliveries::tickAmbient, 20L * 90, 20L * 90);
		// Lodestone elevators: jump = up, sneak = down.
		this.getServer().getScheduler().runTaskTimer(this, this::tickElevators, 3L, 3L);

		this.getLogger().info("Amazing™ is open for business. Earth's Blockiest Store!");
	}

	@Override
	public void onDisable() {
		this.bank.save();
	}

	public Bank bank() {
		return this.bank;
	}

	public DeliveryService deliveries() {
		return this.deliveries;
	}

	public QuestService quests() {
		return this.quests;
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return new CityGenerator();
	}

	// ------------------------------------------------------------------
	// Commands
	// ------------------------------------------------------------------

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Players only.");
			return true;
		}

		switch (command.getName().toLowerCase()) {
			case "bank" -> BankGui.open(this, player);
			case "pay" -> {
				if (args.length != 2) {
					return false;
				}
				try {
					this.bank.transfer(player, this.getServer().getPlayerExact(args[0]),
							Integer.parseInt(args[1]));
				} catch (NumberFormatException e) {
					return false;
				}
			}
			case "amazing" -> {
				String sub = args.length > 0 ? args[0].toLowerCase() : "";
				switch (sub) {
					case "shop" -> ShopGui.open(this, player, 0);
					case "quest" -> player.sendMessage(this.quests.describe(player));
					case "balance" -> player.sendMessage(Component.text("[MineBank] Balance: $"
							+ this.bank.balance(player)
							+ (this.bank.hasPrime(player)
									? " • Prime ★ " + this.bank.primeDaysLeft(player) + "d" : " • No Prime"),
							NamedTextColor.GOLD));
					default -> {
						player.sendMessage(Component.text("— Amazing™, Earth's Blockiest Store —",
								NamedTextColor.GOLD));
						player.sendMessage(Component.text("/amazing shop — browse & order", NamedTextColor.YELLOW));
						player.sendMessage(Component.text("/amazing quest — your current quest", NamedTextColor.YELLOW));
						player.sendMessage(Component.text("/amazing balance — MineBank account", NamedTextColor.YELLOW));
						player.sendMessage(Component.text("/bank — e-banking menu, /pay <player> <amount>",
								NamedTextColor.YELLOW));
					}
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------
	// Listeners
	// ------------------------------------------------------------------

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getHolder() instanceof ShopGui gui) {
			gui.handleClick(event);
		} else if (event.getInventory().getHolder() instanceof BankGui gui) {
			gui.handleClick(event);
		}
	}

	@EventHandler
	public void onCourierClick(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (event.getRightClicked() instanceof Villager villager
				&& villager.getPersistentDataContainer()
						.has(this.deliveries.courierKey(), PersistentDataType.BYTE)) {
			event.setCancelled(true);
			this.quests.onCourierInteract(event.getPlayer(), villager);
		}
	}

	/** Warehouse barrels fill with random Amazing stock the first time they're opened. */
	@EventHandler
	public void onBarrelOpen(InventoryOpenEvent event) {
		if (!(event.getInventory().getHolder() instanceof Barrel barrel)) {
			return;
		}
		Location location = barrel.getLocation();
		if (!(location.getWorld().getGenerator() instanceof CityGenerator)
				|| !CityGenerator.isWarehouseCell(location.getBlockX(), location.getBlockZ())) {
			return;
		}
		if (barrel.getPersistentDataContainer().has(this.barrelFilledKey, PersistentDataType.BYTE)) {
			return;
		}
		barrel.getPersistentDataContainer().set(this.barrelFilledKey, PersistentDataType.BYTE, (byte) 1);
		barrel.update();

		int stacks = 1 + ThreadLocalRandom.current().nextInt(3);
		for (int i = 0; i < stacks; i++) {
			Catalog.Product product = Catalog.random(ThreadLocalRandom.current());
			barrel.getInventory().setItem(ThreadLocalRandom.current().nextInt(27),
					new ItemStack(product.material(), Math.min(product.count(), product.material().getMaxStackSize())));
		}
	}

	// ------------------------------------------------------------------
	// Lodestone elevators
	// ------------------------------------------------------------------

	private void tickElevators() {
		for (Player player : this.getServer().getOnlinePlayers()) {
			Location below = player.getLocation().subtract(0, 1, 0);
			if (below.getBlock().getType() != Material.LODESTONE) {
				continue;
			}
			int direction;
			if (player.getVelocity().getY() > 0.1) {
				direction = 1;
			} else if (player.isSneaking()) {
				direction = -1;
			} else {
				continue;
			}
			for (int dy = 2; dy <= 24; dy++) {
				Location pad = below.clone().add(0, direction * dy, 0);
				if (pad.getBlock().getType() != Material.LODESTONE) {
					continue;
				}
				if (!pad.clone().add(0, 1, 0).getBlock().isPassable()
						|| !pad.clone().add(0, 2, 0).getBlock().isPassable()) {
					continue;
				}
				Location destination = pad.clone().add(0.5, 1, 0.5);
				destination.setYaw(player.getLocation().getYaw());
				destination.setPitch(player.getLocation().getPitch());
				player.getWorld().spawnParticle(Particle.END_ROD,
						player.getLocation().add(0, 1, 0), 20, 0.3, 0.8, 0.3, 0.05);
				player.teleport(destination);
				player.getWorld().spawnParticle(Particle.END_ROD,
						destination.clone().add(0, 1, 0), 20, 0.3, 0.8, 0.3, 0.05);
				player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f,
						direction > 0 ? 1.6f : 1.2f);
				player.setFallDistance(0);
				break;
			}
		}
	}
}
