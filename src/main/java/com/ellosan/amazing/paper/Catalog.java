package com.ellosan.amazing.paper;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The Amazing catalog for Paper — Material-based products, prices in dollars. */
public final class Catalog {
	public record Product(String id, String name, Material material, int count, int dollars, boolean prime) {
	}

	public static final List<Product> ALL = new ArrayList<>();
	private static final Map<String, Product> BY_ID = new HashMap<>();

	private static void add(String id, String name, Material material, int count, int dollars) {
		add(id, name, material, count, dollars, false);
	}

	private static void add(String id, String name, Material material, int count, int dollars, boolean prime) {
		Product product = new Product(id, name, material, count, dollars, prime);
		ALL.add(product);
		BY_ID.put(id, product);
	}

	static {
		add("iron_pickaxe", "IronWorks Pro Pickaxe", Material.IRON_PICKAXE, 1, 18);
		add("diamond_pickaxe", "DiamondCo Ultra Pickaxe", Material.DIAMOND_PICKAXE, 1, 72);
		add("iron_shovel", "IronWorks Pro Shovel", Material.IRON_SHOVEL, 1, 12);
		add("iron_axe", "LumberJack 3000 Iron Axe", Material.IRON_AXE, 1, 18);
		add("diamond_axe", "LumberJack 9000 Diamond Axe", Material.DIAMOND_AXE, 1, 72);
		add("shears", "SnipSnap Precision Shears", Material.SHEARS, 1, 9);
		add("flint_and_steel", "FireStarter Deluxe", Material.FLINT_AND_STEEL, 1, 9);
		add("fishing_rod", "AnglerPro Fishing Rod", Material.FISHING_ROD, 1, 12);
		add("compass", "WayFinder Compass", Material.COMPASS, 1, 15);
		add("clock", "TickTock Wall Clock", Material.CLOCK, 1, 15);
		add("spyglass", "FarSight Spyglass", Material.SPYGLASS, 1, 18);
		add("bucket", "Amazing Basics Bucket", Material.BUCKET, 1, 12);
		add("name_tag", "LabelMate Name Tag", Material.NAME_TAG, 1, 36);
		add("anvil", "HeavyDrop Anvil (Free Shipping!)", Material.ANVIL, 1, 60);
		add("torch", "GlowStick Torches 32-Pack", Material.TORCH, 32, 6);
		add("lantern", "CozyGlow Lantern 4-Pack", Material.LANTERN, 4, 12);
		add("ladder", "StepUp Ladders 16-Pack", Material.LADDER, 16, 9);
		add("scaffolding", "SafeClimb Scaffolding 24-Pack", Material.SCAFFOLDING, 24, 15);
		add("iron_sword", "KnightGuard Iron Sword", Material.IRON_SWORD, 1, 21);
		add("diamond_sword", "KnightGuard Diamond Sword", Material.DIAMOND_SWORD, 1, 78);
		add("bow", "LongShot Recurve Bow", Material.BOW, 1, 24);
		add("crossbow", "LongShot Tactical Crossbow", Material.CROSSBOW, 1, 30);
		add("arrows", "LongShot Arrows 32-Pack", Material.ARROW, 32, 12);
		add("shield", "BlockIt Shield", Material.SHIELD, 1, 18);
		add("iron_helmet", "KnightGuard Iron Helmet", Material.IRON_HELMET, 1, 24);
		add("iron_chestplate", "KnightGuard Iron Chestplate", Material.IRON_CHESTPLATE, 1, 36);
		add("iron_leggings", "KnightGuard Iron Leggings", Material.IRON_LEGGINGS, 1, 30);
		add("iron_boots", "KnightGuard Iron Boots", Material.IRON_BOOTS, 1, 18);
		add("diamond_chestplate", "KnightGuard Diamond Chestplate", Material.DIAMOND_CHESTPLATE, 1, 132);
		add("tnt", "BoomCo TNT 4-Pack (Handle With Care)", Material.TNT, 4, 48);
		add("ender_pearl", "WarpTech Ender Pearl 4-Pack", Material.ENDER_PEARL, 4, 42);
		add("saddle", "RideEasy Saddle", Material.SADDLE, 1, 45);
		add("bread", "BakeHouse Bread 6-Pack", Material.BREAD, 6, 6);
		add("apples", "OrchardFresh Apples 8-Pack", Material.APPLE, 8, 6);
		add("golden_apple", "OrchardFresh GOLDEN Apple", Material.GOLDEN_APPLE, 1, 54);
		add("steak", "PrimeCuts Steak 8-Pack", Material.COOKED_BEEF, 8, 12);
		add("cooked_chicken", "ClucksBBQ Roast Chicken 8-Pack", Material.COOKED_CHICKEN, 8, 9);
		add("baked_potato", "SpudBuds Baked Potatoes 12-Pack", Material.BAKED_POTATO, 12, 6);
		add("cookies", "GrandmaCraft Cookies 16-Pack", Material.COOKIE, 16, 9);
		add("cake", "GrandmaCraft Celebration Cake", Material.CAKE, 1, 18);
		add("golden_carrot", "OrchardFresh Golden Carrots 4-Pack", Material.GOLDEN_CARROT, 4, 30);
		add("sweet_berries", "BerryGood Sweet Berries 12-Pack", Material.SWEET_BERRIES, 12, 6);
		add("honey", "BuzzHive Honey Bottle 3-Pack", Material.HONEY_BOTTLE, 3, 12);
		add("oak_planks", "TimberTown Oak Planks 64-Pack", Material.OAK_PLANKS, 64, 9);
		add("oak_logs", "TimberTown Oak Logs 32-Pack", Material.OAK_LOG, 32, 12);
		add("cobblestone", "RockSolid Cobblestone 64-Pack", Material.COBBLESTONE, 64, 6);
		add("stone_bricks", "RockSolid Stone Bricks 64-Pack", Material.STONE_BRICKS, 64, 12);
		add("bricks", "RedBake Bricks 32-Pack", Material.BRICKS, 32, 15);
		add("glass", "ClearView Glass 32-Pack", Material.GLASS, 32, 12);
		add("wool_white", "CozyHome White Wool 16-Pack", Material.WHITE_WOOL, 16, 9);
		add("quartz_block", "LuxeLiving Quartz Block 16-Pack", Material.QUARTZ_BLOCK, 16, 30);
		add("bookshelf", "ReadMore Bookshelf 4-Pack", Material.BOOKSHELF, 4, 24);
		add("chest", "StoreIt Chest 4-Pack", Material.CHEST, 4, 9);
		add("barrel", "StoreIt Barrel 4-Pack", Material.BARREL, 4, 9);
		add("crafting_table", "MakerSpace Crafting Table", Material.CRAFTING_TABLE, 1, 3);
		add("furnace", "HotStuff Furnace 2-Pack", Material.FURNACE, 2, 6);
		add("bed_white", "DreamSoft Bed (White)", Material.WHITE_BED, 1, 12);
		add("item_frame", "GalleryWall Item Frames 8-Pack", Material.ITEM_FRAME, 8, 12);
		add("glowstone", "GlowUp Glowstone 8-Pack", Material.GLOWSTONE, 8, 24);
		add("sea_lantern", "GlowUp Sea Lantern 8-Pack", Material.SEA_LANTERN, 8, 36);
		add("redstone_dust", "SparkWire Redstone Dust 32-Pack", Material.REDSTONE, 32, 15);
		add("repeater", "SparkWire Repeater 4-Pack", Material.REPEATER, 4, 18);
		add("piston", "PushPro Piston 4-Pack", Material.PISTON, 4, 24);
		add("sticky_piston", "PushPro Sticky Piston 4-Pack", Material.STICKY_PISTON, 4, 36);
		add("hopper", "FlowMaster Hopper 2-Pack", Material.HOPPER, 2, 30);
		add("observer", "WatchDog Observer 4-Pack", Material.OBSERVER, 4, 30);
		add("rail", "RailWay Rails 32-Pack", Material.RAIL, 32, 24);
		add("powered_rail", "RailWay Powered Rails 8-Pack", Material.POWERED_RAIL, 8, 36);
		add("minecart", "RailWay Minecart", Material.MINECART, 1, 18);
		add("wheat_seeds", "GreenThumb Wheat Seeds 24-Pack", Material.WHEAT_SEEDS, 24, 3);
		add("bone_meal", "GrowFast Bone Meal 24-Pack", Material.BONE_MEAL, 24, 12);
		add("oak_sapling", "TreeHugger Oak Saplings 6-Pack", Material.OAK_SAPLING, 6, 6);
		add("brewing_stand", "AlchemyLab Brewing Stand", Material.BREWING_STAND, 1, 24);
		add("nether_wart", "AlchemyLab Nether Wart 8-Pack", Material.NETHER_WART, 8, 24);
		add("blaze_powder", "AlchemyLab Blaze Powder 4-Pack", Material.BLAZE_POWDER, 4, 30);
		add("diamonds", "DiamondCo Certified Diamonds 3-Pack", Material.DIAMOND, 3, 60, true);
		add("gold_ingots", "GoldRush Ingots 8-Pack", Material.GOLD_INGOT, 8, 36, true);
		add("iron_ingots", "IronWorks Ingots 16-Pack", Material.IRON_INGOT, 16, 30, true);
		add("obsidian", "VoidRock Obsidian 8-Pack", Material.OBSIDIAN, 8, 42, true);
		add("ender_eye", "WarpTech Eye of Ender 2-Pack", Material.ENDER_EYE, 2, 54, true);
		add("experience_bottle", "BrainBoost XP Bottles 8-Pack", Material.EXPERIENCE_BOTTLE, 8, 48, true);
		add("totem", "SecondChance Totem of Undying", Material.TOTEM_OF_UNDYING, 1, 180, true);
		add("elytra", "SkyGlide Elytra Wings", Material.ELYTRA, 1, 288, true);
		add("netherite_scrap", "DeepForge Netherite Scrap", Material.NETHERITE_SCRAP, 1, 144, true);
		add("shulker_box", "StoreIt INFINITE Shulker Box", Material.SHULKER_BOX, 1, 120, true);
	}

	private Catalog() {
	}

	public static Product byId(String id) {
		return BY_ID.get(id);
	}

	public static Product random(Random random) {
		return ALL.get(random.nextInt(ALL.size()));
	}
}
