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
				entries.add(ModItems.PRIME_CARD);
				entries.add(ModItems.VAN);
				entries.add(ModItems.workerSpawnEgg);
			})
			.build();

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, AmazingMod.id("amazing"), AMAZING_GROUP);
	}
}
