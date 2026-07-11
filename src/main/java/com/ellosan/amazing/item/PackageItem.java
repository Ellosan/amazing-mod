package com.ellosan.amazing.item;

import com.ellosan.amazing.quest.QuestManager;
import com.ellosan.amazing.registry.ModComponents;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * A sealed Amazing package. Right-click to rip it open and receive whatever
 * is inside. Quest packages are instead hand-delivered to a villager by
 * right-clicking them while holding the package.
 */
public class PackageItem extends Item {

	public PackageItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		if (stack.contains(ModComponents.QUEST_TAG)) {
			if (!world.isClient) {
				player.sendMessage(Text.literal("This package belongs to a customer! Right-click a villager to deliver it.")
						.formatted(Formatting.GOLD), true);
			}
			return TypedActionResult.pass(stack);
		}

		List<ItemStack> contents = stack.get(ModComponents.PACKAGE_CONTENTS);
		if (contents == null || contents.isEmpty()) {
			return TypedActionResult.pass(stack);
		}

		if (!world.isClient) {
			world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,
					SoundCategory.PLAYERS, 1.0f, 0.8f);
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP,
					SoundCategory.PLAYERS, 0.8f, 0.6f);

			if (world instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.POOF,
						player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
				serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
						player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
			}

			for (ItemStack content : contents) {
				ItemStack copy = content.copy();
				// Toss the unboxed items up in a little fountain for flair.
				if (!player.giveItemStack(copy) && !copy.isEmpty()) {
					ItemEntity itemEntity = new ItemEntity(world,
							player.getX(), player.getY() + 1.2, player.getZ(), copy);
					itemEntity.setVelocity(world.random.nextGaussian() * 0.05, 0.25, world.random.nextGaussian() * 0.05);
					world.spawnEntity(itemEntity);
				}
			}

			player.sendMessage(Text.literal("Unboxed! Thank you for shopping with Amazing™")
					.formatted(Formatting.GOLD), true);
			stack.decrement(1);
		}

		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
		String questId = stack.get(ModComponents.QUEST_TAG);
		if (questId != null && entity instanceof VillagerEntity villager) {
			if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
				QuestManager.deliverQuestPackage(serverPlayer, villager, stack);
			}
			return ActionResult.success(player.getWorld().isClient);
		}
		return super.useOnEntity(stack, player, entity, hand);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		String addressee = stack.get(ModComponents.ADDRESSEE);
		if (addressee != null) {
			tooltip.add(Text.literal("Deliver to: " + addressee).formatted(Formatting.YELLOW));
		}
		if (stack.contains(ModComponents.QUEST_TAG)) {
			tooltip.add(Text.literal("Amazing courier quest package").formatted(Formatting.LIGHT_PURPLE));
			tooltip.add(Text.literal("Right-click a villager to deliver").formatted(Formatting.GRAY));
			return;
		}
		List<ItemStack> contents = stack.get(ModComponents.PACKAGE_CONTENTS);
		if (contents != null && !contents.isEmpty()) {
			tooltip.add(Text.literal("Contains:").formatted(Formatting.GRAY));
			for (ItemStack content : contents) {
				tooltip.add(Text.literal("  " + content.getCount() + "x ")
						.append(content.getName()).formatted(Formatting.DARK_GRAY));
			}
			tooltip.add(Text.literal("Right-click to unbox!").formatted(Formatting.GOLD));
		} else {
			tooltip.add(Text.literal("An empty collectible Amazing box.").formatted(Formatting.GRAY));
		}
	}
}
