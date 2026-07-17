package com.ellosan.amazing.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Courier deliveries for Paper: orders are fulfilled by a uniformed courier
 * NPC (a renamed villager) who walks from down the street to the customer
 * and hands over a shulker package with the goods inside.
 */
public class DeliveryService {
	private static final String[] COURIER_NAMES = {
			"Driver Dave", "Speedy Sam", "Parcel Pat", "Boxer Bella", "Turbo Tina",
			"Hasty Hans", "Zippy Zoe", "Courier Carl", "Dash Dana", "Miles Munroe"
	};

	private final AmazingPlugin plugin;
	private final NamespacedKey courierKey;

	public DeliveryService(AmazingPlugin plugin) {
		this.plugin = plugin;
		this.courierKey = new NamespacedKey(plugin, "courier");
	}

	public NamespacedKey courierKey() {
		return this.courierKey;
	}

	public void placeOrder(Player player, Catalog.Product product) {
		boolean creative = player.getGameMode() == org.bukkit.GameMode.CREATIVE;
		if (product.prime() && !this.plugin.bank().hasPrime(player)) {
			player.sendMessage(Component.text("[Amazing] That's a Prime Exclusive! Subscribe in /bank ($"
					+ Bank.PRIME_PRICE + "/month).", NamedTextColor.LIGHT_PURPLE));
			return;
		}
		if (!creative && !this.plugin.bank().charge(player, product.dollars())) {
			player.sendMessage(Component.text("[Amazing] Insufficient funds! That costs $" + product.dollars()
					+ " (balance: $" + this.plugin.bank().balance(player) + ").", NamedTextColor.RED));
			return;
		}

		int delaySeconds = 20 + ThreadLocalRandom.current().nextInt(25);
		player.sendMessage(Component.text("[Amazing] ", NamedTextColor.GOLD)
				.append(Component.text("Order confirmed: " + product.name()
						+ (creative ? " (free for creative customers)" : " ($" + product.dollars() + ")")
						+ ". Courier ETA ~" + delaySeconds + "s!", NamedTextColor.YELLOW)));
		player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (player.isOnline()) {
					dispatchCourier(player, buildPackage(product), true);
				}
			}
		}.runTaskLater(this.plugin, delaySeconds * 20L);
	}

	public ItemStack buildPackage(Catalog.Product product) {
		ItemStack box = new ItemStack(Material.BROWN_SHULKER_BOX);
		BlockStateMeta meta = (BlockStateMeta) box.getItemMeta();
		meta.displayName(Component.text("Amazing Package", NamedTextColor.GOLD));
		meta.lore(List.of(Component.text("Contains: " + product.name(), NamedTextColor.GRAY),
				Component.text("Place & open to unbox!", NamedTextColor.YELLOW)));
		ShulkerBox state = (ShulkerBox) meta.getBlockState();
		int remaining = product.count();
		while (remaining > 0) {
			int size = Math.min(remaining, product.material().getMaxStackSize());
			state.getInventory().addItem(new ItemStack(product.material(), size));
			remaining -= size;
		}
		meta.setBlockState(state);
		box.setItemMeta(meta);
		return box;
	}

	/** Spawns a courier down the street who walks over and delivers the package. */
	public void dispatchCourier(Player customer, ItemStack packageStack, boolean giveDirectly) {
		Location target = customer.getLocation();
		Location spawn = findStreetSpawn(target);

		Villager courier = customer.getWorld().spawn(spawn, Villager.class, npc -> {
			npc.customName(Component.text(COURIER_NAMES[ThreadLocalRandom.current()
					.nextInt(COURIER_NAMES.length)] + " ⚡ Amazing", NamedTextColor.GOLD));
			npc.setCustomNameVisible(true);
			npc.setProfession(Villager.Profession.LEATHERWORKER);
			npc.setVillagerLevel(5);
			npc.setRemoveWhenFarAway(false);
			npc.setInvulnerable(true);
			npc.getPersistentDataContainer().set(this.courierKey, PersistentDataType.BYTE, (byte) 1);
		});
		customer.getWorld().playSound(spawn, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.7f);

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if (!courier.isValid() || !customer.isOnline()) {
					courier.remove();
					this.cancel();
					return;
				}
				this.ticks += 10;
				courier.getPathfinder().moveTo(customer, 1.1);

				if (courier.getLocation().distanceSquared(customer.getLocation()) < 6.5 || this.ticks > 20 * 60) {
					// Hand over the goods.
					if (giveDirectly) {
						customer.getInventory().addItem(packageStack).values().forEach(left ->
								customer.getWorld().dropItemNaturally(customer.getLocation(), left));
					} else {
						customer.getWorld().dropItemNaturally(courier.getLocation(), packageStack);
					}
					customer.sendMessage(Component.text("[Amazing] ", NamedTextColor.GOLD)
							.append(Component.text(plainName(courier) + " delivered your package. Enjoy!",
									NamedTextColor.YELLOW)));
					customer.getWorld().playSound(customer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
					customer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
							courier.getLocation().add(0, 1.5, 0), 12, 0.4, 0.4, 0.4);
					courier.swingMainHand();
					plugin.quests().onDeliveryReceived(customer);
					walkAwayAndDespawn(courier);
					this.cancel();
				}
			}
		}.runTaskTimer(this.plugin, 10L, 10L);
	}

	/** The courier jogs off down the street, then vanishes in a puff. */
	private void walkAwayAndDespawn(Villager courier) {
		Location away = courier.getLocation().add(
				ThreadLocalRandom.current().nextInt(-20, 21), 0, ThreadLocalRandom.current().nextInt(-20, 21));
		courier.getPathfinder().moveTo(away, 1.2);
		new BukkitRunnable() {
			@Override
			public void run() {
				if (courier.isValid()) {
					courier.getWorld().spawnParticle(Particle.CLOUD,
							courier.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
					courier.getWorld().playSound(courier.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
					courier.remove();
				}
			}
		}.runTaskLater(this.plugin, 20L * 8);
	}

	private static Location findStreetSpawn(Location target) {
		Random random = ThreadLocalRandom.current();
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 18 + random.nextDouble() * 10;
			int x = target.getBlockX() + (int) (Math.cos(angle) * distance);
			int z = target.getBlockZ() + (int) (Math.sin(angle) * distance);
			int y = target.getWorld().getHighestBlockYAt(x, z) + 1;
			if (Math.abs(y - target.getBlockY()) <= 12) {
				return new Location(target.getWorld(), x + 0.5, y, z + 0.5);
			}
		}
		return target.clone().add(6, 0, 6);
	}

	/** Ambient deliveries: a courier pays a nearby villager a visit. */
	public void tickAmbient() {
		for (Player player : this.plugin.getServer().getOnlinePlayers()) {
			if (ThreadLocalRandom.current().nextFloat() >= 0.12f) {
				continue;
			}
			player.getWorld().getNearbyEntitiesByType(Villager.class, player.getLocation(), 48).stream()
					.filter(villager -> !villager.getPersistentDataContainer()
							.has(this.courierKey, PersistentDataType.BYTE))
					.findAny()
					.ifPresent(lucky -> {
						lucky.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
								lucky.getLocation().add(0, 1.5, 0), 10, 0.4, 0.4, 0.4);
						lucky.getWorld().playSound(lucky.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
					});
		}
	}

	static String plainName(Villager courier) {
		return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
				.serialize(courier.customName() == null ? Component.text("Courier") : courier.customName());
	}
}
