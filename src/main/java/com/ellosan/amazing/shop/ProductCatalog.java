package com.ellosan.amazing.shop;

import com.ellosan.amazing.registry.ModItems;
import com.ellosan.amazing.shop.Product.Category;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The full Amazing catalog. Every listing has a stable id so orders can be
 * saved to disk and sent over the network as plain strings.
 */
public final class ProductCatalog {
	private static final List<Product> PRODUCTS = new ArrayList<>();
	private static final Map<String, Product> BY_ID = new HashMap<>();

	private static void add(String id, String name, Item item, int count, int price, Category category) {
		add(id, name, item, count, price, category, false);
	}

	private static void add(String id, String name, Item item, int count, int price, Category category, boolean prime) {
		Product product = new Product(id, name, item, count, price, category, prime);
		PRODUCTS.add(product);
		BY_ID.put(id, product);
	}

	static {
		// ----- Tools & DIY -----
		add("wood_pickaxe", "Amazing Basics Wooden Pickaxe", Items.WOODEN_PICKAXE, 1, 1, Category.TOOLS);
		add("stone_pickaxe", "Amazing Basics Stone Pickaxe", Items.STONE_PICKAXE, 1, 2, Category.TOOLS);
		add("iron_pickaxe", "IronWorks Pro Pickaxe", Items.IRON_PICKAXE, 1, 6, Category.TOOLS);
		add("diamond_pickaxe", "DiamondCo Ultra Pickaxe", Items.DIAMOND_PICKAXE, 1, 24, Category.TOOLS);
		add("iron_shovel", "IronWorks Pro Shovel", Items.IRON_SHOVEL, 1, 4, Category.TOOLS);
		add("diamond_shovel", "DiamondCo Ultra Shovel", Items.DIAMOND_SHOVEL, 1, 16, Category.TOOLS);
		add("iron_axe", "LumberJack 3000 Iron Axe", Items.IRON_AXE, 1, 6, Category.TOOLS);
		add("diamond_axe", "LumberJack 9000 Diamond Axe", Items.DIAMOND_AXE, 1, 24, Category.TOOLS);
		add("iron_hoe", "GreenThumb Iron Hoe", Items.IRON_HOE, 1, 3, Category.TOOLS);
		add("shears", "SnipSnap Precision Shears", Items.SHEARS, 1, 3, Category.TOOLS);
		add("flint_and_steel", "FireStarter Deluxe", Items.FLINT_AND_STEEL, 1, 3, Category.TOOLS);
		add("fishing_rod", "AnglerPro Fishing Rod", Items.FISHING_ROD, 1, 4, Category.TOOLS);
		add("compass", "WayFinder Compass", Items.COMPASS, 1, 5, Category.TOOLS);
		add("clock", "TickTock Wall Clock", Items.CLOCK, 1, 5, Category.TOOLS);
		add("spyglass", "FarSight Spyglass", Items.SPYGLASS, 1, 6, Category.TOOLS);
		add("bucket", "Amazing Basics Bucket", Items.BUCKET, 1, 4, Category.TOOLS);
		add("water_bucket", "Amazing Basics Bucket (Pre-Filled, Water)", Items.WATER_BUCKET, 1, 5, Category.TOOLS);
		add("lead", "WalkiesPro Lead 2-Pack", Items.LEAD, 2, 4, Category.TOOLS);
		add("name_tag", "LabelMate Name Tag", Items.NAME_TAG, 1, 12, Category.TOOLS);
		add("anvil", "HeavyDrop Anvil (Free Shipping!)", Items.ANVIL, 1, 20, Category.TOOLS);
		add("ladder", "StepUp Ladders 16-Pack", Items.LADDER, 16, 3, Category.TOOLS);
		add("torch", "GlowStick Torches 32-Pack", Items.TORCH, 32, 2, Category.TOOLS);
		add("lantern", "CozyGlow Lantern 4-Pack", Items.LANTERN, 4, 4, Category.TOOLS);
		add("scaffolding", "SafeClimb Scaffolding 24-Pack", Items.SCAFFOLDING, 24, 5, Category.TOOLS);

		// ----- Combat & Outdoors -----
		add("iron_sword", "KnightGuard Iron Sword", Items.IRON_SWORD, 1, 7, Category.COMBAT);
		add("diamond_sword", "KnightGuard Diamond Sword", Items.DIAMOND_SWORD, 1, 26, Category.COMBAT);
		add("bow", "LongShot Recurve Bow", Items.BOW, 1, 8, Category.COMBAT);
		add("crossbow", "LongShot Tactical Crossbow", Items.CROSSBOW, 1, 10, Category.COMBAT);
		add("arrows", "LongShot Arrows 32-Pack", Items.ARROW, 32, 4, Category.COMBAT);
		add("shield", "BlockIt Shield", Items.SHIELD, 1, 6, Category.COMBAT);
		add("iron_helmet", "KnightGuard Iron Helmet", Items.IRON_HELMET, 1, 8, Category.COMBAT);
		add("iron_chestplate", "KnightGuard Iron Chestplate", Items.IRON_CHESTPLATE, 1, 12, Category.COMBAT);
		add("iron_leggings", "KnightGuard Iron Leggings", Items.IRON_LEGGINGS, 1, 10, Category.COMBAT);
		add("iron_boots", "KnightGuard Iron Boots", Items.IRON_BOOTS, 1, 6, Category.COMBAT);
		add("diamond_helmet", "KnightGuard Diamond Helmet", Items.DIAMOND_HELMET, 1, 28, Category.COMBAT);
		add("diamond_chestplate", "KnightGuard Diamond Chestplate", Items.DIAMOND_CHESTPLATE, 1, 44, Category.COMBAT);
		add("diamond_leggings", "KnightGuard Diamond Leggings", Items.DIAMOND_LEGGINGS, 1, 38, Category.COMBAT);
		add("diamond_boots", "KnightGuard Diamond Boots", Items.DIAMOND_BOOTS, 1, 22, Category.COMBAT);
		add("tnt", "BoomCo TNT 4-Pack (Handle With Care)", Items.TNT, 4, 16, Category.COMBAT);
		add("ender_pearl", "WarpTech Ender Pearl 4-Pack", Items.ENDER_PEARL, 4, 14, Category.COMBAT);
		add("saddle", "RideEasy Saddle", Items.SADDLE, 1, 15, Category.COMBAT);
		add("campfire", "TrailBlaze Campfire Kit", Items.CAMPFIRE, 1, 3, Category.COMBAT);
		add("tent_bed", "TrailBlaze Sleeping Bed (Red)", Items.RED_BED, 1, 4, Category.COMBAT);

		// ----- Grocery -----
		add("bread", "BakeHouse Bread 6-Pack", Items.BREAD, 6, 2, Category.FOOD);
		add("apples", "OrchardFresh Apples 8-Pack", Items.APPLE, 8, 2, Category.FOOD);
		add("golden_apple", "OrchardFresh GOLDEN Apple", Items.GOLDEN_APPLE, 1, 18, Category.FOOD);
		add("steak", "PrimeCuts Steak 8-Pack", Items.COOKED_BEEF, 8, 4, Category.FOOD);
		add("cooked_chicken", "ClucksBBQ Roast Chicken 8-Pack", Items.COOKED_CHICKEN, 8, 3, Category.FOOD);
		add("cooked_porkchop", "PrimeCuts Porkchops 8-Pack", Items.COOKED_PORKCHOP, 8, 4, Category.FOOD);
		add("cooked_salmon", "OceanCatch Salmon 6-Pack", Items.COOKED_SALMON, 6, 3, Category.FOOD);
		add("baked_potato", "SpudBuds Baked Potatoes 12-Pack", Items.BAKED_POTATO, 12, 2, Category.FOOD);
		add("cookies", "GrandmaCraft Cookies 16-Pack", Items.COOKIE, 16, 3, Category.FOOD);
		add("cake", "GrandmaCraft Celebration Cake", Items.CAKE, 1, 6, Category.FOOD);
		add("pumpkin_pie", "GrandmaCraft Pumpkin Pie 4-Pack", Items.PUMPKIN_PIE, 4, 4, Category.FOOD);
		add("melon_slices", "JuicyBite Melon Slices 16-Pack", Items.MELON_SLICE, 16, 2, Category.FOOD);
		add("carrots", "OrchardFresh Carrots 12-Pack", Items.CARROT, 12, 2, Category.FOOD);
		add("golden_carrot", "OrchardFresh Golden Carrots 4-Pack", Items.GOLDEN_CARROT, 4, 10, Category.FOOD);
		add("milk", "MooJuice Fresh Milk", Items.MILK_BUCKET, 1, 3, Category.FOOD);
		add("sweet_berries", "BerryGood Sweet Berries 12-Pack", Items.SWEET_BERRIES, 12, 2, Category.FOOD);
		add("honey", "BuzzHive Honey Bottle 3-Pack", Items.HONEY_BOTTLE, 3, 4, Category.FOOD);
		add("mushroom_stew", "SoupreMe Mushroom Stew", Items.MUSHROOM_STEW, 1, 2, Category.FOOD);

		// ----- Home & Building -----
		add("oak_planks", "TimberTown Oak Planks 64-Pack", Items.OAK_PLANKS, 64, 3, Category.BLOCKS);
		add("spruce_planks", "TimberTown Spruce Planks 64-Pack", Items.SPRUCE_PLANKS, 64, 3, Category.BLOCKS);
		add("oak_logs", "TimberTown Oak Logs 32-Pack", Items.OAK_LOG, 32, 4, Category.BLOCKS);
		add("cobblestone", "RockSolid Cobblestone 64-Pack", Items.COBBLESTONE, 64, 2, Category.BLOCKS);
		add("stone", "RockSolid Smooth Stone 64-Pack", Items.STONE, 64, 3, Category.BLOCKS);
		add("stone_bricks", "RockSolid Stone Bricks 64-Pack", Items.STONE_BRICKS, 64, 4, Category.BLOCKS);
		add("bricks", "RedBake Bricks 32-Pack", Items.BRICKS, 32, 5, Category.BLOCKS);
		add("glass", "ClearView Glass 32-Pack", Items.GLASS, 32, 4, Category.BLOCKS);
		add("sand", "BeachDay Sand 64-Pack", Items.SAND, 64, 2, Category.BLOCKS);
		add("wool_white", "CozyHome White Wool 16-Pack", Items.WHITE_WOOL, 16, 3, Category.BLOCKS);
		add("terracotta", "DesertChic Terracotta 32-Pack", Items.TERRACOTTA, 32, 4, Category.BLOCKS);
		add("quartz_block", "LuxeLiving Quartz Block 16-Pack", Items.QUARTZ_BLOCK, 16, 10, Category.BLOCKS);
		add("bookshelf", "ReadMore Bookshelf 4-Pack", Items.BOOKSHELF, 4, 8, Category.BLOCKS);
		add("chest", "StoreIt Chest 4-Pack", Items.CHEST, 4, 3, Category.BLOCKS);
		add("barrel", "StoreIt Barrel 4-Pack", Items.BARREL, 4, 3, Category.BLOCKS);
		add("crafting_table", "MakerSpace Crafting Table", Items.CRAFTING_TABLE, 1, 1, Category.BLOCKS);
		add("furnace", "HotStuff Furnace 2-Pack", Items.FURNACE, 2, 2, Category.BLOCKS);
		add("smoker", "HotStuff Smoker", Items.SMOKER, 1, 4, Category.BLOCKS);
		add("blast_furnace", "HotStuff Blast Furnace", Items.BLAST_FURNACE, 1, 6, Category.BLOCKS);
		add("bed_white", "DreamSoft Bed (White)", Items.WHITE_BED, 1, 4, Category.BLOCKS);
		add("item_frame", "GalleryWall Item Frames 8-Pack", Items.ITEM_FRAME, 8, 4, Category.BLOCKS);
		add("painting", "GalleryWall Mystery Painting 3-Pack", Items.PAINTING, 3, 3, Category.BLOCKS);
		add("flower_pot", "GreenThumb Flower Pots 4-Pack", Items.FLOWER_POT, 4, 2, Category.BLOCKS);
		add("glowstone", "GlowUp Glowstone 8-Pack", Items.GLOWSTONE, 8, 8, Category.BLOCKS);
		add("sea_lantern", "GlowUp Sea Lantern 8-Pack", Items.SEA_LANTERN, 8, 12, Category.BLOCKS);

		// ----- Amazing Basics Tech (redstone) -----
		add("redstone_dust", "SparkWire Redstone Dust 32-Pack", Items.REDSTONE, 32, 5, Category.REDSTONE);
		add("redstone_torch", "SparkWire Redstone Torch 8-Pack", Items.REDSTONE_TORCH, 8, 3, Category.REDSTONE);
		add("repeater", "SparkWire Repeater 4-Pack", Items.REPEATER, 4, 6, Category.REDSTONE);
		add("comparator", "SparkWire Comparator 4-Pack", Items.COMPARATOR, 4, 8, Category.REDSTONE);
		add("piston", "PushPro Piston 4-Pack", Items.PISTON, 4, 8, Category.REDSTONE);
		add("sticky_piston", "PushPro Sticky Piston 4-Pack", Items.STICKY_PISTON, 4, 12, Category.REDSTONE);
		add("hopper", "FlowMaster Hopper 2-Pack", Items.HOPPER, 2, 10, Category.REDSTONE);
		add("dispenser", "LaunchIt Dispenser 2-Pack", Items.DISPENSER, 2, 8, Category.REDSTONE);
		add("dropper", "LaunchIt Dropper 2-Pack", Items.DROPPER, 2, 6, Category.REDSTONE);
		add("observer", "WatchDog Observer 4-Pack", Items.OBSERVER, 4, 10, Category.REDSTONE);
		add("lever", "ClickClack Levers 8-Pack", Items.LEVER, 8, 2, Category.REDSTONE);
		add("redstone_lamp", "GlowUp Redstone Lamp 4-Pack", Items.REDSTONE_LAMP, 4, 8, Category.REDSTONE);
		add("rail", "RailWay Rails 32-Pack", Items.RAIL, 32, 8, Category.REDSTONE);
		add("powered_rail", "RailWay Powered Rails 8-Pack", Items.POWERED_RAIL, 8, 12, Category.REDSTONE);
		add("minecart", "RailWay Minecart", Items.MINECART, 1, 6, Category.REDSTONE);
		add("daylight_detector", "SunSense Daylight Detector 2-Pack", Items.DAYLIGHT_DETECTOR, 2, 6, Category.REDSTONE);

		// ----- Garden & Pets -----
		add("wheat_seeds", "GreenThumb Wheat Seeds 24-Pack", Items.WHEAT_SEEDS, 24, 1, Category.FARMING);
		add("pumpkin_seeds", "GreenThumb Pumpkin Seeds 8-Pack", Items.PUMPKIN_SEEDS, 8, 2, Category.FARMING);
		add("melon_seeds", "GreenThumb Melon Seeds 8-Pack", Items.MELON_SEEDS, 8, 2, Category.FARMING);
		add("bone_meal", "GrowFast Bone Meal 24-Pack", Items.BONE_MEAL, 24, 4, Category.FARMING);
		add("oak_sapling", "TreeHugger Oak Saplings 6-Pack", Items.OAK_SAPLING, 6, 2, Category.FARMING);
		add("birch_sapling", "TreeHugger Birch Saplings 6-Pack", Items.BIRCH_SAPLING, 6, 2, Category.FARMING);
		add("poppy", "BloomBox Poppies 8-Pack", Items.POPPY, 8, 1, Category.FARMING);
		add("dandelion", "BloomBox Dandelions 8-Pack", Items.DANDELION, 8, 1, Category.FARMING);
		add("sugar_cane", "SweetStalk Sugar Cane 12-Pack", Items.SUGAR_CANE, 12, 2, Category.FARMING);
		add("cactus", "PricklyPal Cactus 4-Pack", Items.CACTUS, 4, 2, Category.FARMING);
		add("bamboo", "PandaSnack Bamboo 12-Pack", Items.BAMBOO, 12, 2, Category.FARMING);
		add("bone", "GoodBoy Dog Bones 8-Pack", Items.BONE, 8, 3, Category.FARMING);
		add("wheat", "FarmFresh Wheat 16-Pack", Items.WHEAT, 16, 2, Category.FARMING);
		add("egg", "ClucksFarm Eggs 16-Pack", Items.EGG, 16, 2, Category.FARMING);
		add("composter", "GreenThumb Composter", Items.COMPOSTER, 1, 2, Category.FARMING);
		add("beehive", "BuzzHive Starter Beehive", Items.BEEHIVE, 1, 8, Category.FARMING);

		// ----- Health & Alchemy -----
		add("brewing_stand", "AlchemyLab Brewing Stand", Items.BREWING_STAND, 1, 8, Category.BREWING);
		add("cauldron", "AlchemyLab Cauldron", Items.CAULDRON, 1, 6, Category.BREWING);
		add("glass_bottle", "AlchemyLab Glass Bottles 8-Pack", Items.GLASS_BOTTLE, 8, 2, Category.BREWING);
		add("nether_wart", "AlchemyLab Nether Wart 8-Pack", Items.NETHER_WART, 8, 8, Category.BREWING);
		add("blaze_powder", "AlchemyLab Blaze Powder 4-Pack", Items.BLAZE_POWDER, 4, 10, Category.BREWING);
		add("ghast_tear", "AlchemyLab Ghast Tear", Items.GHAST_TEAR, 1, 14, Category.BREWING);
		add("spider_eye", "AlchemyLab Spider Eyes 4-Pack", Items.SPIDER_EYE, 4, 3, Category.BREWING);
		add("gunpowder", "BoomCo Gunpowder 8-Pack", Items.GUNPOWDER, 8, 6, Category.BREWING);
		add("sugar", "SweetStalk Sugar 16-Pack", Items.SUGAR, 16, 2, Category.BREWING);
		add("golden_melon", "AlchemyLab Glistering Melon 2-Pack", Items.GLISTERING_MELON_SLICE, 2, 8, Category.BREWING);

		// ----- Prime Exclusives -----
		add("diamonds", "DiamondCo Certified Diamonds 3-Pack", Items.DIAMOND, 3, 20, Category.RARE, true);
		add("emerald_block", "InvestBlock Emerald Block", Items.EMERALD_BLOCK, 1, 10, Category.RARE, true);
		add("gold_ingots", "GoldRush Ingots 8-Pack", Items.GOLD_INGOT, 8, 12, Category.RARE, true);
		add("iron_ingots", "IronWorks Ingots 16-Pack", Items.IRON_INGOT, 16, 10, Category.RARE, true);
		add("obsidian", "VoidRock Obsidian 8-Pack", Items.OBSIDIAN, 8, 14, Category.RARE, true);
		add("ender_eye", "WarpTech Eye of Ender 2-Pack", Items.ENDER_EYE, 2, 18, Category.RARE, true);
		add("experience_bottle", "BrainBoost XP Bottles 8-Pack", Items.EXPERIENCE_BOTTLE, 8, 16, Category.RARE, true);
		add("totem", "SecondChance Totem of Undying", Items.TOTEM_OF_UNDYING, 1, 60, Category.RARE, true);
		add("elytra", "SkyGlide Elytra Wings", Items.ELYTRA, 1, 96, Category.RARE, true);
		add("netherite_scrap", "DeepForge Netherite Scrap", Items.NETHERITE_SCRAP, 1, 48, Category.RARE, true);
		add("shulker_box", "StoreIt INFINITE Shulker Box", Items.SHULKER_BOX, 1, 40, Category.RARE, true);
		add("amazing_van", "Amazing Delivery Van (Own One Today!)", ModItems.VAN, 1, 50, Category.RARE, true);
		add("prime_card", "Amazing Prime Membership Card", ModItems.PRIME_CARD, 1, 25, Category.RARE);
		add("empty_package", "Amazing Collectible Empty Box 4-Pack", ModItems.PACKAGE, 4, 1, Category.RARE);
	}

	private ProductCatalog() {
	}

	public static List<Product> all() {
		return PRODUCTS;
	}

	@Nullable
	public static Product byId(String id) {
		return BY_ID.get(id);
	}

	public static Product random(net.minecraft.util.math.random.Random random) {
		return PRODUCTS.get(random.nextInt(PRODUCTS.size()));
	}
}
