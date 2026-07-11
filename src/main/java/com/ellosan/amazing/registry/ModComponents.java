package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;

import com.mojang.serialization.Codec;

import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.List;

/**
 * Data components attached to Amazing items.
 */
public class ModComponents {
	/** The items sealed inside an Amazing package. */
	public static final ComponentType<List<ItemStack>> PACKAGE_CONTENTS = ComponentType.<List<ItemStack>>builder()
			.codec(ItemStack.CODEC.listOf())
			.packetCodec(ItemStack.PACKET_CODEC.collect(PacketCodecs.toList()))
			.build();

	/** Marks a package as belonging to a courier quest. Value = quest id. */
	public static final ComponentType<String> QUEST_TAG = ComponentType.<String>builder()
			.codec(Codec.STRING)
			.packetCodec(PacketCodecs.STRING)
			.build();

	/** The customer this package is addressed to (shown on the label). */
	public static final ComponentType<String> ADDRESSEE = ComponentType.<String>builder()
			.codec(Codec.STRING)
			.packetCodec(PacketCodecs.STRING)
			.build();

	public static void register() {
		Registry.register(Registries.DATA_COMPONENT_TYPE, AmazingMod.id("package_contents"), PACKAGE_CONTENTS);
		Registry.register(Registries.DATA_COMPONENT_TYPE, AmazingMod.id("quest_tag"), QUEST_TAG);
		Registry.register(Registries.DATA_COMPONENT_TYPE, AmazingMod.id("addressee"), ADDRESSEE);
	}
}
