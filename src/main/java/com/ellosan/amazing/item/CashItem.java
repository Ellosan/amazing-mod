package com.ellosan.amazing.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A crisp MineBank dollar. Legal tender across all biomes. */
public class CashItem extends Item {

	public CashItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Worth $" + stack.getCount() + " — deposit at any ATM").formatted(Formatting.GREEN));
		tooltip.add(Text.literal("\"In Blocks We Trust\"").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
