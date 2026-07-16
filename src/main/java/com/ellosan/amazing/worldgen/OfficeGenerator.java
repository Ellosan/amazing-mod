package com.ellosan.amazing.worldgen;

import com.ellosan.amazing.block.LampBlock;
import com.ellosan.amazing.block.PcBlock;
import com.ellosan.amazing.registry.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

import static com.ellosan.amazing.worldgen.CityChunkGenerator.GROUND_Y;
import static com.ellosan.amazing.worldgen.CityChunkGenerator.set;

/**
 * Builds full office buildings: glass-band facades, a reception lobby with
 * ATMs, an escalator to the first floor, an express elevator shaft serving
 * every level, and per-floor workspaces.
 *
 * <p>Variant space: 5 facades x 3 heights x 3 floor layouts (open-plan desk
 * rows, cubicle grids, perimeter offices with a meeting table) =
 * <b>45 distinct offices</b>, derived from the cell seed.
 */
public final class OfficeGenerator {
	/** facade, accent */
	private static final BlockState[][] PALETTES = {
			{s(Blocks.LIGHT_GRAY_CONCRETE), s(Blocks.BLUE_CONCRETE)},
			{s(Blocks.WHITE_CONCRETE), s(Blocks.GRAY_CONCRETE)},
			{s(Blocks.GRAY_CONCRETE), s(Blocks.ORANGE_CONCRETE)},
			{s(Blocks.BLUE_TERRACOTTA), s(Blocks.WHITE_CONCRETE)},
			{s(Blocks.CYAN_TERRACOTTA), s(Blocks.YELLOW_CONCRETE)},
	};

	private static final int FLOOR_HEIGHT = 4;

	private OfficeGenerator() {
	}

	private static BlockState s(Block block) {
		return block.getDefaultState();
	}

	public static void build(Chunk chunk, long seed, int originX, int originZ) {
		BlockState[] palette = PALETTES[(int) Math.floorMod(seed, PALETTES.length)];
		BlockState facade = palette[0];
		BlockState accent = palette[1];
		int floors = 3 + (int) Math.floorMod(seed >> 3, 3);
		int layout = (int) Math.floorMod(seed >> 5, 3);

		int x0 = originX + 4;
		int z0 = originZ + 4;
		int size = 24;
		int y0 = GROUND_Y + 1;

		// ----- shell with glass window bands -----
		for (int dx = 0; dx < size; dx++) {
			for (int dz = 0; dz < size; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
				boolean corner = (dx == 0 || dx == size - 1) && (dz == 0 || dz == size - 1);

				for (int f = 0; f < floors; f++) {
					int base = y0 + f * FLOOR_HEIGHT;
					set(chunk, x, base - 1, z, f == 0 ? s(Blocks.SMOOTH_STONE) : facade); // slab
					for (int dy = 0; dy < FLOOR_HEIGHT - 1; dy++) {
						if (edge) {
							boolean band = dy >= 1 && !corner && (dx % 2 == 1 || dz % 2 == 1);
							set(chunk, x, base + dy, z, band ? s(Blocks.GLASS) : (corner ? accent : facade));
						} else {
							set(chunk, x, base + dy, z, AIR());
						}
					}
					if (edge) {
						set(chunk, x, base + FLOOR_HEIGHT - 1, z, corner ? accent : facade);
					} else {
						set(chunk, x, base + FLOOR_HEIGHT - 1, z, AIR());
					}
				}
			}
		}

		// ----- roof with AC units and accent rim -----
		int roofY = y0 + floors * FLOOR_HEIGHT - 1;
		for (int dx = 0; dx < size; dx++) {
			for (int dz = 0; dz < size; dz++) {
				set(chunk, x0 + dx, roofY, z0 + dz, s(Blocks.GRAY_CONCRETE));
			}
		}
		set(chunk, x0 + 5, roofY + 1, z0 + 5, s(Blocks.IRON_BLOCK));
		set(chunk, x0 + size - 6, roofY + 1, z0 + size - 6, s(Blocks.IRON_BLOCK));
		for (int dx = 0; dx < size; dx += 3) {
			set(chunk, x0 + dx, roofY + 1, z0, accent);
			set(chunk, x0 + dx, roofY + 1, z0 + size - 1, accent);
		}

		// ----- entrance (south) -----
		int doorX = x0 + size / 2;
		for (int dx = -1; dx <= 1; dx++) {
			set(chunk, doorX + dx, y0, z0 + size - 1, AIR());
			set(chunk, doorX + dx, y0 + 1, z0 + size - 1, AIR());
			set(chunk, doorX + dx, GROUND_Y, z0 + size, s(Blocks.SMOOTH_STONE));
		}

		// ----- lobby: reception, waiting chairs, ATMs -----
		set(chunk, doorX - 2, y0, z0 + size - 5, ModBlocks.TABLE.getDefaultState());
		set(chunk, doorX - 1, y0, z0 + size - 5, ModBlocks.TABLE.getDefaultState());
		set(chunk, doorX, y0, z0 + size - 5, ModBlocks.TABLE.getDefaultState());
		set(chunk, doorX - 1, y0, z0 + size - 6, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, doorX + 3, y0, z0 + size - 3, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.WEST));
		set(chunk, doorX + 3, y0, z0 + size - 4, ModBlocks.CHAIR.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.WEST));
		set(chunk, x0 + 2, y0, z0 + size - 3, ModBlocks.ATM.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.EAST));
		set(chunk, x0 + 2, y0, z0 + size - 5, ModBlocks.ATM.getDefaultState()
				.with(HorizontalFacingBlock.FACING, Direction.EAST));
		set(chunk, doorX, y0, z0 + size - 7, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));

		// ----- escalators: ground -> floor 1, along the east wall -----
		for (int i = 0; i < FLOOR_HEIGHT; i++) {
			int ex = x0 + size - 3;
			int ez = z0 + size - 4 - i;
			set(chunk, ex, y0 + i, ez, ModBlocks.ESCALATOR.getDefaultState()
					.with(HorizontalFacingBlock.FACING, Direction.NORTH));
			for (int clear = 1; clear <= 3; clear++) {
				set(chunk, ex, y0 + i + clear, ez, AIR());
			}
			// opening in the floor-1 slab above the run
			set(chunk, ex, y0 + FLOOR_HEIGHT - 1, ez, AIR());
		}

		// ----- express elevator shaft (NE corner, pad on every floor) -----
		for (int f = 0; f < floors; f++) {
			int base = y0 + f * FLOOR_HEIGHT;
			set(chunk, x0 + 2, base - 1, z0 + 2, ModBlocks.ELEVATOR.getDefaultState());
			set(chunk, x0 + 3, base - 1, z0 + 2, ModBlocks.ELEVATOR.getDefaultState());
			// keep the shaft column clear of floor slabs
			if (f > 0) {
				set(chunk, x0 + 2, base - 1, z0 + 3, s(Blocks.SMOOTH_STONE));
			}
		}

		// ----- per-floor workspaces -----
		for (int f = 1; f < floors; f++) {
			int base = y0 + f * FLOOR_HEIGHT;
			switch (layout) {
				case 0 -> openPlan(chunk, base, x0, z0, size);
				case 1 -> cubicles(chunk, base, x0, z0, size);
				default -> perimeterOffices(chunk, base, x0, z0, size);
			}
		}
	}

	/** Rows of desks: table + PC + chair, classic open plan. */
	private static void openPlan(Chunk chunk, int y, int x0, int z0, int size) {
		for (int dz = 5; dz < size - 5; dz += 4) {
			for (int dx = 6; dx < size - 5; dx += 4) {
				desk(chunk, y, x0 + dx, z0 + dz, Direction.SOUTH);
			}
		}
		set(chunk, x0 + size / 2, y, z0 + 3, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	/** Cubicle clusters with glass dividers. */
	private static void cubicles(Chunk chunk, int y, int x0, int z0, int size) {
		for (int dz = 5; dz < size - 6; dz += 6) {
			for (int dx = 6; dx < size - 6; dx += 6) {
				int x = x0 + dx;
				int z = z0 + dz;
				for (int i = 0; i < 4; i++) {
					set(chunk, x + i, y, z + 2, s(Blocks.LIGHT_GRAY_STAINED_GLASS));
				}
				desk(chunk, y, x, z, Direction.NORTH);
				desk(chunk, y, x + 3, z, Direction.NORTH);
			}
		}
	}

	/** Meeting table in the middle, desks around the edges. */
	private static void perimeterOffices(Chunk chunk, int y, int x0, int z0, int size) {
		int cx = x0 + size / 2;
		int cz = z0 + size / 2;
		for (int i = -2; i <= 2; i++) {
			set(chunk, cx + i, y, cz, ModBlocks.TABLE.getDefaultState());
			set(chunk, cx + i, y, cz - 2, ModBlocks.CHAIR.getDefaultState()
					.with(HorizontalFacingBlock.FACING, Direction.SOUTH));
			set(chunk, cx + i, y, cz + 2, ModBlocks.CHAIR.getDefaultState()
					.with(HorizontalFacingBlock.FACING, Direction.NORTH));
		}
		desk(chunk, y, x0 + 4, z0 + 4, Direction.EAST);
		desk(chunk, y, x0 + size - 5, z0 + 4, Direction.WEST);
		desk(chunk, y, x0 + 4, z0 + size - 5, Direction.EAST);
		set(chunk, cx, y, z0 + 4, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	/** A desk: table with a PC on top, chair behind it. sitDir = chair's view direction. */
	private static void desk(Chunk chunk, int y, int x, int z, Direction sitDir) {
		set(chunk, x, y, z, ModBlocks.TABLE.getDefaultState());
		set(chunk, x, y + 1, z, ModBlocks.PC.getDefaultState()
				.with(HorizontalFacingBlock.FACING, sitDir.getOpposite()).with(PcBlock.ON, (x + z) % 2 == 0));
		set(chunk, x - sitDir.getOffsetX(), y, z - sitDir.getOffsetZ(),
				ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, sitDir));
	}

	private static BlockState AIR() {
		return Blocks.AIR.getDefaultState();
	}
}
