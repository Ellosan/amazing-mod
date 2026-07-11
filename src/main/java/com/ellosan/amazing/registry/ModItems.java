package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.item.PackageItem;
import com.ellosan.amazing.item.PrimeCardItem;
import com.ellosan.amazing.item.VanItem;

import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public class ModItems {
	public static final Item PACKAGE = new PackageItem(new Item.Settings().maxCount(16));
	public static final Item PRIME_CARD = new PrimeCardItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
	public static final Item VAN = new VanItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));

	// Registered in ModEntities.register() after the entity type exists.
	public static SpawnEggItem workerSpawnEgg;

	public static void register() {
		Registry.register(Registries.ITEM, AmazingMod.id("package"), PACKAGE);
		Registry.register(Registries.ITEM, AmazingMod.id("prime_card"), PRIME_CARD);
		Registry.register(Registries.ITEM, AmazingMod.id("van"), VAN);
	}
}
