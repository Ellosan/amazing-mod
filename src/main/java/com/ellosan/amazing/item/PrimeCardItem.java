package com.ellosan.amazing.item;

import com.ellosan.amazing.net.OpenCatalogPayload;

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
 * The Amazing Prime Card. Right-click to browse the catalog from anywhere,
 * and carry it to unlock Prime Exclusive listings.
 */
public class PrimeCardItem extends Item {

	public PrimeCardItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
			world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
					SoundCategory.PLAYERS, 0.7f, 1.4f);
			ServerPlayNetworking.send(serverPlayer, new OpenCatalogPayload());
		}
		return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click to shop at Amazing™").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("Unlocks Prime Exclusive deals").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("\"Earth's Blockiest Store\"").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
