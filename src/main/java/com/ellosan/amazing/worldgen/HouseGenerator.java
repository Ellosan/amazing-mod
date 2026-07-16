package com.ellosan.amazing.worldgen;

import com.ellosan.amazing.block.LampBlock;
import com.ellosan.amazing.block.PcBlock;
import com.ellosan.amazing.block.TvBlock;
import com.ellosan.amazing.registry.ModBlocks;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

import static com.ellosan.amazing.worldgen.CityChunkGenerator.GROUND_Y;
import static com.ellosan.amazing.worldgen.CityChunkGenerator.set;

/**
 * Builds full family homes: entry hall, living room (TV + console + couch),
 * dining room, kitchen, bedrooms, office/den upstairs, plus optional pools,
 * balconies and two garden styles.
 *
 * <p>Variant space: 12 palettes x 2 floor-counts x 3 roof styles x pool
 * on/off x 2 garden styles x mirrored layouts = <b>576 distinct houses</b>,
 * all derived deterministically from the cell seed.
 */
public final class HouseGenerator {
	/** wall, accent, roof, floor */
	private static final BlockState[][] PALETTES = {
			{s(Blocks.WHITE_TERRACOTTA), s(Blocks.DARK_OAK_PLANKS), s(Blocks.DARK_OAK_PLANKS), s(Blocks.OAK_PLANKS)},
			{s(Blocks.BRICKS), s(Blocks.STONE), s(Blocks.SMOOTH_STONE), s(Blocks.OAK_PLANKS)},
			{s(Blocks.ORANGE_TERRACOTTA), s(Blocks.WHITE_TERRACOTTA), s(Blocks.DARK_OAK_PLANKS), s(Blocks.SPRUCE_PLANKS)},
			{s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.GRAY_CONCRETE), s(Blocks.SPRUCE_PLANKS)},
			{s(Blocks.SPRUCE_PLANKS), s(Blocks.COBBLESTONE), s(Blocks.DARK_OAK_PLANKS), s(Blocks.SPRUCE_PLANKS)},
			{s(Blocks.SANDSTONE), s(Blocks.SMOOTH_SANDSTONE), s(Blocks.SMOOTH_SANDSTONE), s(Blocks.BIRCH_PLANKS)},
			{s(Blocks.WHITE_CONCRETE), s(Blocks.BLUE_CONCRETE), s(Blocks.BLUE_TERRACOTTA), s(Blocks.BIRCH_PLANKS)},
			{s(Blocks.RED_TERRACOTTA), s(Blocks.WHITE_TERRACOTTA), s(Blocks.BROWN_TERRACOTTA), s(Blocks.OAK_PLANKS)},
			{s(Blocks.MUD_BRICKS), s(Blocks.BRICKS), s(Blocks.BRICKS), s(Blocks.OAK_PLANKS)},
			{s(Blocks.GRAY_TERRACOTTA), s(Blocks.WHITE_CONCRETE), s(Blocks.CYAN_TERRACOTTA), s(Blocks.DARK_OAK_PLANKS)},
			{s(Blocks.QUARTZ_BLOCK), s(Blocks.SMOOTH_QUARTZ), s(Blocks.SMOOTH_QUARTZ), s(Blocks.BIRCH_PLANKS)},
			{s(Blocks.DARK_OAK_PLANKS), s(Blocks.WHITE_TERRACOTTA), s(Blocks.DARK_OAK_PLANKS), s(Blocks.OAK_PLANKS)},
	};

	private static final int FLOOR_HEIGHT = 4;

	private HouseGenerator() {
	}

	private static BlockState s(Block block) {
		return block.getDefaultState();
	}

	/**
	 * Builds one house + yard filling a city cell. House footprint 20x14 at
	 * (origin+6, origin+6); yard with garden/pool to the south.
	 */
	public static void build(Chunk chunk, long seed, int originX, int originZ) {
		BlockState[] palette = PALETTES[(int) Math.floorMod(seed, PALETTES.length)];
		BlockState wall = palette[0];
		BlockState accent = palette[1];
		BlockState roof = palette[2];
		BlockState floor = palette[3];

		boolean twoFloors = Math.floorMod(seed >> 4, 2) == 0;
		int roofStyle = (int) Math.floorMod(seed >> 5, 3);
		boolean pool = Math.floorMod(seed >> 7, 2) == 0;
		boolean hedgeGarden = Math.floorMod(seed >> 8, 2) == 0;
		boolean mirrored = Math.floorMod(seed >> 9, 2) == 0;
		boolean balcony = twoFloors && Math.floorMod(seed >> 10, 2) == 0;

		int x0 = originX + 6;
		int z0 = originZ + 6;
		int width = 20;
		int depth = 14;
		int floors = twoFloors ? 2 : 1;
		int y0 = GROUND_Y + 1;

		// ----- shell -----
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < depth; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;

				set(chunk, x, GROUND_Y, z, floor);
				for (int f = 0; f < floors; f++) {
					int base = y0 + f * FLOOR_HEIGHT;
					for (int dy = 0; dy < FLOOR_HEIGHT; dy++) {
						if (edge) {
							boolean window = dy >= 1 && dy <= 2 && (dx % 4 == 2 || dz % 4 == 2)
									&& dx != 0 == (dz != 0); // skip corners
							boolean corner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
							set(chunk, x, base + dy, z,
									corner ? accent : (window ? s(Blocks.GLASS) : wall));
						} else {
							set(chunk, x, base + dy, z, AIR());
						}
					}
					if (f < floors - 1) {
						set(chunk, x, base + FLOOR_HEIGHT, z, floor); // upper floor slab
					}
				}
			}
		}

		// ----- roof -----
		int roofY = y0 + floors * FLOOR_HEIGHT;
		for (int dx = 0; dx < width; dx++) {
			for (int dz = 0; dz < depth; dz++) {
				set(chunk, x0 + dx, roofY, z0 + dz, roof);
			}
		}
		switch (roofStyle) {
			case 0 -> { // parapet rim
				for (int dx = 0; dx < width; dx++) {
					set(chunk, x0 + dx, roofY + 1, z0, accent);
					set(chunk, x0 + dx, roofY + 1, z0 + depth - 1, accent);
				}
				for (int dz = 0; dz < depth; dz++) {
					set(chunk, x0, roofY + 1, z0 + dz, accent);
					set(chunk, x0 + width - 1, roofY + 1, z0 + dz, accent);
				}
			}
			case 1 -> { // stepped pyramid cap
				for (int inset = 1; inset <= 2; inset++) {
					for (int dx = inset; dx < width - inset; dx++) {
						for (int dz = inset; dz < depth - inset; dz++) {
							set(chunk, x0 + dx, roofY + inset, z0 + dz, roof);
						}
					}
				}
			}
			default -> { // overhang eaves
				for (int dx = -1; dx <= width; dx++) {
					set(chunk, x0 + dx, roofY, z0 - 1, roof);
					set(chunk, x0 + dx, roofY, z0 + depth, roof);
				}
				for (int dz = -1; dz <= depth; dz++) {
					set(chunk, x0 - 1, roofY, z0 + dz, roof);
					set(chunk, x0 + width, roofY, z0 + dz, roof);
				}
			}
		}

		// ----- front door (south) + porch -----
		int doorX = x0 + width / 2 + (mirrored ? -2 : 2);
		set(chunk, doorX, y0, z0 + depth - 1, AIR());
		set(chunk, doorX, y0 + 1, z0 + depth - 1, AIR());
		set(chunk, doorX, GROUND_Y, z0 + depth, s(Blocks.SMOOTH_STONE));
		set(chunk, doorX, GROUND_Y, z0 + depth + 1, s(Blocks.SMOOTH_STONE));

		// ----- interior partitions: quadrants around a hall cross -----
		int wallX = x0 + (mirrored ? 7 : 12); // north-south internal wall
		int wallZ = z0 + 7;                   // east-west internal wall
		for (int dz = 1; dz < depth - 1; dz++) {
			if (Math.abs(z0 + dz - wallZ) > 1) { // door gap at crossing
				set(chunk, wallX, y0, z0 + dz, wall);
				set(chunk, wallX, y0 + 1, z0 + dz, wall);
				set(chunk, wallX, y0 + 2, z0 + dz, wall);
			}
		}
		for (int dx = 1; dx < width - 1; dx++) {
			int x = x0 + dx;
			if (Math.abs(x - wallX) > 1 && Math.abs(x - doorX) > 1) {
				set(chunk, x, y0, wallZ, wall);
				set(chunk, x, y0 + 1, wallZ, wall);
				set(chunk, x, y0 + 2, wallZ, wall);
			}
		}

		// Quadrant anchor points (west/east of wallX, north/south of wallZ).
		int westX = (x0 + 2 + wallX) / 2;
		int eastX = (wallX + x0 + width - 3) / 2 + 1;
		int northZ = z0 + 3;
		int southZ = z0 + depth - 4;
		int livingX = mirrored ? eastX : westX;
		int diningX = mirrored ? westX : eastX;

		furnishLiving(chunk, y0, livingX, southZ);
		furnishDining(chunk, y0, diningX, southZ);
		furnishKitchen(chunk, y0, mirrored ? eastX : westX, northZ);
		furnishBedroom(chunk, y0, mirrored ? westX : eastX, northZ, 1);
		set(chunk, doorX, y0, z0 + depth - 3, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));

		// ----- second floor: bedrooms + office + den, stairs, balcony -----
		if (twoFloors) {
			int base2 = y0 + FLOOR_HEIGHT;
			// stairwell opening + stairs along the hall
			for (int i = 0; i < 4; i++) {
				int sx = wallX + (mirrored ? 2 : -2);
				int sz = wallZ - 1 + i;
				set(chunk, sx, y0 + i, sz, s(Blocks.OAK_STAIRS).with(StairsBlock.FACING, Direction.SOUTH));
				for (int clear = y0 + i + 1; clear <= base2 + 1; clear++) {
					set(chunk, sx, clear, sz, AIR());
				}
			}
			furnishBedroom(chunk, base2, westX, northZ, 2);
			furnishOffice(chunk, base2, eastX, northZ);
			furnishDen(chunk, base2, mirrored ? eastX : westX, southZ);

			if (balcony) {
				for (int dx = 4; dx < width - 4; dx++) {
					set(chunk, x0 + dx, base2 - 1, z0 + depth, floor);
					set(chunk, x0 + dx, base2 - 1, z0 + depth + 1, floor);
					set(chunk, x0 + dx, base2, z0 + depth + 2, s(Blocks.OAK_FENCE));
				}
				set(chunk, x0 + 4, base2, z0 + depth, s(Blocks.OAK_FENCE));
				set(chunk, x0 + width - 5, base2, z0 + depth, s(Blocks.OAK_FENCE));
				// balcony door
				set(chunk, doorX, base2, z0 + depth - 1, AIR());
				set(chunk, doorX, base2 + 1, z0 + depth - 1, AIR());
			}
		}

		// ----- yard: garden path + pool or flowers -----
		int yardZ0 = z0 + depth + 1;
		int yardZ1 = originZ + 27;
		for (int z = yardZ0; z <= yardZ1; z++) {
			set(chunk, doorX, GROUND_Y, z, s(Blocks.GRAVEL));
		}
		if (pool) {
			int px = x0 + (mirrored ? 12 : 2);
			for (int dx = 0; dx < 6; dx++) {
				for (int dz = 0; dz < 4; dz++) {
					boolean rim = dx == 0 || dz == 0 || dx == 5 || dz == 3;
					set(chunk, px + dx, GROUND_Y, yardZ0 + dz + 1,
							rim ? s(Blocks.SMOOTH_QUARTZ) : Blocks.WATER.getDefaultState());
				}
			}
			set(chunk, px + 6, GROUND_Y + 1, yardZ0 + 2,
					ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
			set(chunk, px + 6, GROUND_Y + 1, yardZ0 + 3,
					ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
		} else if (hedgeGarden) {
			for (int dx = 2; dx < width - 2; dx += 2) {
				set(chunk, x0 + dx, GROUND_Y + 1, yardZ1, s(Blocks.OAK_LEAVES));
			}
			set(chunk, x0 + 2, GROUND_Y + 1, yardZ0 + 1, s(Blocks.OAK_LEAVES));
			set(chunk, x0 + width - 3, GROUND_Y + 1, yardZ0 + 1, s(Blocks.OAK_LEAVES));
		} else {
			for (int i = 0; i < 8; i++) {
				int fx = x0 + 1 + (int) Math.floorMod(seed >> (11 + i * 3), width - 2);
				int fz = yardZ0 + (int) Math.floorMod(seed >> (13 + i * 2), Math.max(1, yardZ1 - yardZ0));
				if (fx != doorX) {
					set(chunk, fx, GROUND_Y + 1, fz,
							(i % 2 == 0 ? Blocks.POPPY : Blocks.OXEYE_DAISY).getDefaultState());
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Rooms
	// ------------------------------------------------------------------

	private static void furnishLiving(Chunk chunk, int y, int cx, int cz) {
		set(chunk, cx, y, cz, ModBlocks.TV.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.NORTH).with(TvBlock.ON, true));
		set(chunk, cx - 1, y, cz, ModBlocks.CONSOLE.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.NORTH));
		set(chunk, cx - 1, y, cz - 2, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, cx, y, cz - 2, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, cx + 1, y, cz - 2, ModBlocks.TABLE.getDefaultState());
		set(chunk, cx + 2, y, cz, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
		set(chunk, cx, GROUND_Y, cz - 1, Blocks.RED_WOOL.getDefaultState());
	}

	private static void furnishDining(Chunk chunk, int y, int cx, int cz) {
		set(chunk, cx, y, cz - 1, ModBlocks.TABLE.getDefaultState());
		set(chunk, cx + 1, y, cz - 1, ModBlocks.TABLE.getDefaultState());
		set(chunk, cx - 1, y, cz - 1, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.EAST));
		set(chunk, cx + 2, y, cz - 1, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.WEST));
		set(chunk, cx, y, cz - 2, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, cx + 1, y, cz, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.NORTH));
		set(chunk, cx - 2, y, cz, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	private static void furnishKitchen(Chunk chunk, int y, int cx, int cz) {
		set(chunk, cx - 1, y, cz - 1, Blocks.FURNACE.getDefaultState());
		set(chunk, cx, y, cz - 1, Blocks.SMOKER.getDefaultState());
		set(chunk, cx + 1, y, cz - 1, Blocks.CRAFTING_TABLE.getDefaultState());
		set(chunk, cx + 2, y, cz - 1, Blocks.BARREL.getDefaultState());
		set(chunk, cx - 2, y, cz - 1, Blocks.CAULDRON.getDefaultState());
		set(chunk, cx, y, cz + 1, ModBlocks.TABLE.getDefaultState());
	}

	private static void furnishBedroom(Chunk chunk, int y, int cx, int cz, int beds) {
		for (int i = 0; i < beds; i++) {
			int bx = cx + i * 2;
			set(chunk, bx, y, cz, Blocks.BLUE_BED.getDefaultState()
					.with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.SOUTH));
			set(chunk, bx, y, cz - 1, Blocks.BLUE_BED.getDefaultState()
					.with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.SOUTH));
		}
		set(chunk, cx - 2, y, cz, Blocks.CHEST.getDefaultState());
		set(chunk, cx - 2, y, cz - 1, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	private static void furnishOffice(Chunk chunk, int y, int cx, int cz) {
		set(chunk, cx, y, cz - 1, ModBlocks.TABLE.getDefaultState());
		set(chunk, cx, y + 1, cz - 1, ModBlocks.PC.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH).with(PcBlock.ON, true));
		set(chunk, cx, y, cz, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.NORTH));
		set(chunk, cx + 2, y, cz - 1, Blocks.BOOKSHELF.getDefaultState());
		set(chunk, cx - 2, y, cz - 1, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	private static void furnishDen(Chunk chunk, int y, int cx, int cz) {
		set(chunk, cx - 1, y, cz, Blocks.BOOKSHELF.getDefaultState());
		set(chunk, cx + 1, y, cz, Blocks.BOOKSHELF.getDefaultState());
		set(chunk, cx, y, cz, ModBlocks.TV.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.NORTH).with(TvBlock.ON, false));
		set(chunk, cx, y, cz - 2, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, cx - 2, y, cz - 2, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	private static BlockState AIR() {
		return Blocks.AIR.getDefaultState();
	}
}
