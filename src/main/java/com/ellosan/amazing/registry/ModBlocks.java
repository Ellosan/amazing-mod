package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.block.AtmBlock;
import com.ellosan.amazing.block.CinemaScreenBlock;
import com.ellosan.amazing.block.ConsoleBlock;
import com.ellosan.amazing.block.ElevatorBlock;
import com.ellosan.amazing.block.EscalatorBlock;
import com.ellosan.amazing.block.PcBlock;
import com.ellosan.amazing.block.ChairBlock;
import com.ellosan.amazing.block.LampBlock;
import com.ellosan.amazing.block.PackageBlock;
import com.ellosan.amazing.block.TableBlock;
import com.ellosan.amazing.block.TvBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public class ModBlocks {
	public static final Block CHAIR = new ChairBlock(AbstractBlock.Settings.create()
			.strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque());
	public static final Block TABLE = new TableBlock(AbstractBlock.Settings.create()
			.strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque());
	public static final Block LAMP = new LampBlock(AbstractBlock.Settings.create()
			.strength(0.6f).sounds(BlockSoundGroup.LANTERN).nonOpaque()
			.luminance(state -> state.get(LampBlock.LIT) ? 14 : 0));
	public static final Block TV = new TvBlock(AbstractBlock.Settings.create()
			.strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque()
			.luminance(state -> state.get(TvBlock.ON) ? 10 : 0));
	public static final Block ATM = new AtmBlock(AbstractBlock.Settings.create()
			.strength(3.0f).requiresTool().sounds(BlockSoundGroup.METAL).nonOpaque());
	public static final Block PACKAGE_BLOCK = new PackageBlock(AbstractBlock.Settings.create()
			.strength(0.5f).sounds(BlockSoundGroup.WOOD));
	public static final Block PC = new PcBlock(AbstractBlock.Settings.create()
			.strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque()
			.luminance(state -> state.get(PcBlock.ON) ? 8 : 0));
	public static final Block CONSOLE = new ConsoleBlock(AbstractBlock.Settings.create()
			.strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque().ticksRandomly()
			.luminance(state -> state.get(ConsoleBlock.ON) ? 6 : 0));
	public static final Block CINEMA_SCREEN = new CinemaScreenBlock(AbstractBlock.Settings.create()
			.strength(1.5f).sounds(BlockSoundGroup.GLASS).luminance(state -> 12));
	public static final Block ELEVATOR = new ElevatorBlock(AbstractBlock.Settings.create()
			.strength(2.0f).sounds(BlockSoundGroup.METAL).luminance(state -> 5));
	public static final Block ESCALATOR = new EscalatorBlock(AbstractBlock.Settings.create()
			.strength(2.0f).sounds(BlockSoundGroup.METAL).nonOpaque());

	public static void register() {
		registerWithItem("chair", CHAIR);
		registerWithItem("table", TABLE);
		registerWithItem("lamp", LAMP);
		registerWithItem("tv", TV);
		registerWithItem("atm", ATM);
		registerWithItem("package_block", PACKAGE_BLOCK);
		registerWithItem("pc", PC);
		registerWithItem("console", CONSOLE);
		registerWithItem("cinema_screen", CINEMA_SCREEN);
		registerWithItem("elevator", ELEVATOR);
		registerWithItem("escalator", ESCALATOR);
	}

	private static void registerWithItem(String name, Block block) {
		Registry.register(Registries.BLOCK, AmazingMod.id(name), block);
		Registry.register(Registries.ITEM, AmazingMod.id(name), new BlockItem(block, new Item.Settings()));
	}
}
