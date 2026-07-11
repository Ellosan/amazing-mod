package com.ellosan.amazing.shop;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A single listing in the Amazing catalog.
 *
 * @param id       stable id used in network payloads and saved orders
 * @param name     marketing name shown in the catalog
 * @param item     the item that gets delivered
 * @param count    how many items one order contains
 * @param price    price in emeralds
 * @param category catalog department
 * @param prime    Prime-exclusive listings require an Amazing Prime Card in the inventory
 */
public record Product(String id, String name, Item item, int count, int price, Category category, boolean prime) {

	public ItemStack createStack() {
		return new ItemStack(item, count);
	}

	public enum Category {
		TOOLS("Tools & DIY"),
		COMBAT("Combat & Outdoors"),
		FOOD("Grocery"),
		BLOCKS("Home & Building"),
		REDSTONE("Amazing Basics Tech"),
		FARMING("Garden & Pets"),
		BREWING("Health & Alchemy"),
		RARE("Prime Exclusives");

		public final String displayName;

		Category(String displayName) {
			this.displayName = displayName;
		}
	}
}
