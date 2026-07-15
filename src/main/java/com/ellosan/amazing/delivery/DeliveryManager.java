package com.ellosan.amazing.delivery;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.economy.BankManager;
import com.ellosan.amazing.entity.CitizenEntity;
import com.ellosan.amazing.entity.DeliveryVanEntity;
import com.ellosan.amazing.registry.ModComponents;
import com.ellosan.amazing.registry.ModEntities;
import com.ellosan.amazing.registry.ModItems;
import com.ellosan.amazing.shop.Product;
import com.ellosan.amazing.shop.ProductCatalog;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-side heart of Amazing: takes orders, charges emeralds, schedules
 * deliveries, dispatches vans, and keeps villagers supplied with ambient
 * NPC deliveries.
 */
public final class DeliveryManager {
	private static final int MAX_PENDING_PER_PLAYER = 5;
	private static final int RETRY_TICKS = 20 * 15;
	/** Roughly every 4 minutes per player, an NPC delivery happens nearby. */
	private static final int AMBIENT_CHECK_INTERVAL = 20 * 30;
	private static final float AMBIENT_CHANCE = 0.125f;

	private DeliveryManager() {
	}

	// ------------------------------------------------------------------
	// Ordering
	// ------------------------------------------------------------------

	/** Handles an order request coming from the catalog screen. */
	public static void placeOrder(ServerPlayerEntity player, String productId, int quantity) {
		Product product = ProductCatalog.byId(productId);
		quantity = MathHelper.clamp(quantity, 1, 8);

		if (product == null) {
			player.sendMessage(Text.literal("[Amazing] That listing is no longer available.")
					.formatted(Formatting.RED), false);
			return;
		}

		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AmazingWorldState state = AmazingWorldState.get(server);

		long pending = state.orders.stream().filter(order -> order.customer.equals(player.getUuid())).count();
		if (pending >= MAX_PENDING_PER_PLAYER) {
			player.sendMessage(Text.literal("[Amazing] You already have " + pending
					+ " deliveries on the way. Even we have limits!").formatted(Formatting.RED), false);
			return;
		}

		boolean creative = player.getAbilities().creativeMode;
		if (product.prime() && !BankManager.hasPrime(player)) {
			player.sendMessage(Text.literal("[Amazing] That's a Prime Exclusive! Subscribe to Prime ($"
					+ BankManager.PRIME_PRICE + "/month) in the MineBank app.")
					.formatted(Formatting.LIGHT_PURPLE), false);
			return;
		}

		int totalPrice = product.dollars() * quantity;
		if (!creative && !BankManager.charge(player, totalPrice)) {
			player.sendMessage(Text.literal("[Amazing] Insufficient funds! That costs $" + totalPrice
					+ " (balance: $" + BankManager.balance(player) + ").").formatted(Formatting.RED), false);
			return;
		}

		int deliveryTicks = 20 * (25 + player.getRandom().nextInt(30));
		state.orders.add(new AmazingWorldState.PendingOrder(player.getUuid(), productId, quantity, deliveryTicks));
		state.markDirty();
		BankManager.sync(player);

		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
				SoundCategory.PLAYERS, 0.7f, 1.8f);
		player.sendMessage(Text.literal("[Amazing] ").formatted(Formatting.GOLD)
				.append(Text.literal("Order confirmed: " + quantity + "x " + product.name()
						+ (creative ? " (free for creative customers)" : " ($" + totalPrice + ")")
						+ ". A van is on its way — ETA ~" + (deliveryTicks / 20) + "s!")
						.formatted(Formatting.YELLOW)), false);
	}

	// ------------------------------------------------------------------
	// Scheduling
	// ------------------------------------------------------------------

	public static void tickServer(MinecraftServer server) {
		AmazingWorldState state = AmazingWorldState.get(server);

		if (!state.orders.isEmpty()) {
			boolean dirty = false;
			Iterator<AmazingWorldState.PendingOrder> iterator = state.orders.iterator();
			while (iterator.hasNext()) {
				AmazingWorldState.PendingOrder order = iterator.next();
				order.ticksLeft--;
				if (order.ticksLeft > 0) {
					continue;
				}

				ServerPlayerEntity customer = server.getPlayerManager().getPlayer(order.customer);
				if (customer == null || customer.isRemoved()) {
					// Customer is offline — the van waits at the depot and retries.
					order.ticksLeft = RETRY_TICKS;
					dirty = true;
					continue;
				}

				dispatchOrder(customer, order);
				iterator.remove();
				dirty = true;
			}
			if (dirty) {
				state.markDirty();
			}
		}

		if (server.getTicks() % AMBIENT_CHECK_INTERVAL == 0) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.getRandom().nextFloat() < AMBIENT_CHANCE) {
					tryAmbientVillagerDelivery(player);
				}
			}
		}
	}

	private static void dispatchOrder(ServerPlayerEntity customer, AmazingWorldState.PendingOrder order) {
		Product product = ProductCatalog.byId(order.productId);
		if (product == null) {
			return;
		}

		List<ItemStack> contents = new ArrayList<>();
		for (int i = 0; i < order.quantity; i++) {
			int count = product.count();
			while (count > 0) {
				ItemStack stack = product.createStack();
				int size = Math.min(count, stack.getMaxCount());
				stack.setCount(size);
				contents.add(stack);
				count -= size;
			}
		}

		ItemStack packageStack = buildPackage(contents, customer.getName().getString());
		boolean sent = dispatchVan(customer.getServerWorld(), customer, customer.getBlockPos(), packageStack);

		if (sent) {
			customer.sendMessage(Text.literal("[Amazing] ").formatted(Formatting.GOLD)
					.append(Text.literal("Your delivery van has entered the area!")
							.formatted(Formatting.YELLOW)), true);
		} else {
			// No spot for a van (cave, ocean, roofed area) — express drone drop instead.
			if (!customer.giveItemStack(packageStack)) {
				customer.dropItem(packageStack, false);
			}
			customer.sendMessage(Text.literal("[Amazing] ").formatted(Formatting.GOLD)
					.append(Text.literal("No road access here, so we used our experimental drone. Package delivered!")
							.formatted(Formatting.YELLOW)), false);
		}
	}

	public static ItemStack buildPackage(List<ItemStack> contents, String addressee) {
		ItemStack packageStack = new ItemStack(ModItems.PACKAGE);
		packageStack.set(ModComponents.PACKAGE_CONTENTS, List.copyOf(contents));
		packageStack.set(ModComponents.ADDRESSEE, addressee);
		return packageStack;
	}

	/**
	 * Spawns a delivery van on the surface 25-40 blocks away from the customer.
	 * Returns false if no reasonable spawn spot exists.
	 */
	public static boolean dispatchVan(ServerWorld world, @Nullable Entity customer, BlockPos destination,
			ItemStack packageStack) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = world.random.nextDouble() * Math.PI * 2;
			double distance = 25 + world.random.nextDouble() * 15;
			int x = destination.getX() + (int) (Math.cos(angle) * distance);
			int z = destination.getZ() + (int) (Math.sin(angle) * distance);
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

			// Reject spawn points on wildly different terrain (cliffs, oceans).
			if (Math.abs(y - destination.getY()) > 20) {
				continue;
			}
			BlockPos spawnPos = new BlockPos(x, y, z);
			if (!world.getFluidState(spawnPos.down()).isEmpty()) {
				continue;
			}

			DeliveryVanEntity van = ModEntities.DELIVERY_VAN.create(world);
			if (van == null) {
				return false;
			}
			float yaw = (float) (MathHelper.atan2(destination.getZ() + 0.5 - (x + 0.5),
					destination.getX() + 0.5 - (x + 0.5)) * 180.0 / Math.PI) - 90.0f;
			van.refreshPositionAndAngles(x + 0.5, y + 0.5, z + 0.5, yaw, 0.0f);
			van.setDelivery(customer != null ? customer.getUuid() : null, destination, packageStack);

			if (!world.isSpaceEmpty(van, van.getBoundingBox())) {
				van.discard();
				continue;
			}

			world.spawnEntity(van);
			world.playSound(null, van.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
					SoundCategory.NEUTRAL, 0.5f, 0.7f);
			AmazingMod.LOGGER.debug("Dispatched Amazing van to {}", destination.toShortString());
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------
	// Ambient villager deliveries
	// ------------------------------------------------------------------

	private static void tryAmbientVillagerDelivery(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		List<LivingEntity> customers = world.getEntitiesByClass(LivingEntity.class,
				Box.of(player.getPos(), 96, 32, 96),
				entity -> entity.isAlive() && (entity instanceof VillagerEntity || entity instanceof CitizenEntity));
		if (customers.isEmpty()) {
			return;
		}

		LivingEntity lucky = customers.get(world.random.nextInt(customers.size()));
		Product product = ProductCatalog.random(world.random);
		ItemStack packageStack = buildPackage(List.of(product.createStack()), lucky.getName().getString());
		dispatchVan(world, lucky, lucky.getBlockPos(), packageStack);
	}
}
