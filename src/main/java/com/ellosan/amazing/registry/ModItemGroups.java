package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class ModItemGroups {
	public static final ItemGroup AMAZING_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(ModItems.PACKAGE))
			.displayName(Text.translatable("itemGroup.amazing"))
			.entries((displayContext, entries) -> {
				entries.add(ModItems.PACKAGE);
				entries.add(ModItems.PHONE);
				entries.add(ModItems.CASH);
				entries.add(ModItems.PRIME_CARD);
				entries.add(ModItems.VAN);
				entries.add(ModItems.CAR);
				entries.add(ModBlocks.CHAIR);
				entries.add(ModBlocks.TABLE);
				entries.add(ModBlocks.LAMP);
				entries.add(ModBlocks.TV);
				entries.add(ModBlocks.ATM);
				entries.add(ModBlocks.PACKAGE_BLOCK);
				entries.add(ModItems.workerSpawnEgg);
				entries.add(ModItems.citizenSpawnEgg);
			})
			.build();

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, AmazingMod.id("amazing"), AMAZING_GROUP);
	}
}
