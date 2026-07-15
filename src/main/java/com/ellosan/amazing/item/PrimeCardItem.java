package com.ellosan.amazing.item;

import com.ellosan.amazing.economy.BankManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * An Amazing Prime gift card. Right-click to redeem 30 days of Prime.
 * (Ongoing membership costs $20/month via the MineBank app.)
 */
public class PrimeCardItem extends Item {

	public PrimeCardItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
			BankManager.addPrimeTime(serverPlayer);
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
					SoundCategory.PLAYERS, 0.7f, 1.4f);
			player.sendMessage(Text.literal("[Amazing] Prime card redeemed! Prime active for "
					+ BankManager.primeDaysLeft(serverPlayer) + " days.").formatted(Formatting.LIGHT_PURPLE), false);
			stack.decrement(1);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click to redeem 30 days of Prime").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("Unlocks Prime Exclusive deals").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("\"Earth's Blockiest Store\"").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
