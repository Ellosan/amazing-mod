package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.item.CashItem;
import com.ellosan.amazing.item.PackageItem;
import com.ellosan.amazing.item.PhoneItem;
import com.ellosan.amazing.item.PrimeCardItem;
import com.ellosan.amazing.item.VanItem;

import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public class ModItems {
	public static final Item PACKAGE = new PackageItem(new Item.Settings().maxCount(16));
	public static final Item PRIME_CARD = new PrimeCardItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC));
	public static final Item VAN = new VanItem(() -> ModEntities.VAN, new Item.Settings().maxCount(1).rarity(Rarity.RARE));
	public static final Item CAR = new VanItem(() -> ModEntities.CAR, new Item.Settings().maxCount(1).rarity(Rarity.RARE));
	public static final Item PHONE = new PhoneItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON));
	public static final Item CASH = new CashItem(new Item.Settings());

	// Registered in ModEntities.register() after the entity types exist.
	public static SpawnEggItem workerSpawnEgg;
	public static SpawnEggItem citizenSpawnEgg;

	public static void register() {
		Registry.register(Registries.ITEM, AmazingMod.id("package"), PACKAGE);
		Registry.register(Registries.ITEM, AmazingMod.id("prime_card"), PRIME_CARD);
		Registry.register(Registries.ITEM, AmazingMod.id("van"), VAN);
		Registry.register(Registries.ITEM, AmazingMod.id("car"), CAR);
		Registry.register(Registries.ITEM, AmazingMod.id("phone"), PHONE);
		Registry.register(Registries.ITEM, AmazingMod.id("cash"), CASH);
	}
}
