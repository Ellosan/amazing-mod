package com.ellosan.amazing.item;

import com.ellosan.amazing.economy.BankManager;
import com.ellosan.amazing.net.OpenPhonePayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
 * The AmazingPhone X. Apps: Amazing (shop + order tracking), MineBank,
 * GPS, Quests, Radio, and good old-fashioned phone calls.
 */
public class PhoneItem extends Item {

	public PhoneItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
			world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(),
					SoundCategory.PLAYERS, 0.6f, 2.0f);
			BankManager.sync(serverPlayer);
			ServerPlayNetworking.send(serverPlayer, new OpenPhonePayload());
		}
		return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click to unlock").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("Amazing • MineBank • GPS • Quests • Radio").formatted(Formatting.GRAY));
	}
}
