package com.ellosan.amazing.worldgen;

import com.ellosan.amazing.block.LampBlock;
import com.ellosan.amazing.block.TvBlock;
import com.ellosan.amazing.entity.CitizenEntity;
import com.ellosan.amazing.entity.DeliveryWorkerEntity;
import com.ellosan.amazing.registry.ModBlocks;
import com.ellosan.amazing.registry.ModEntities;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Amazing City™ — a procedural city world. The city is an infinite grid of
 * 32x32 cells separated by roads; each cell deterministically becomes
 * houses, an apartment tower, a park, or an Amazing warehouse stacked with
 * packages. Citizens live in the houses; couriers staff the warehouses.
 *
 * <p>Ground level is y=64. Everything is derived from (cellX, cellZ, seed)
 * hashes, so chunks generate independently and identically every time.
 */
public class CityChunkGenerator extends ChunkGenerator {
	public static final MapCodec<CityChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
			instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource))
					.apply(instance, CityChunkGenerator::new));

	public static final int GROUND_Y = 64;
	/** City layout is position-hashed with a fixed salt so chunks are fully independent. */
	private static final long LAYOUT_SALT = 0xA11A2196L;
	private static final int CELL = 32;
	/** Road band: local coords 0,1 and 30,31 (4 wide across the cell seam). */
	private static final int ROAD = 2;

	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final BlockState STONE = Blocks.STONE.getDefaultState();
	private static final BlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
	private static final BlockState GRASS = Blocks.GRASS_BLOCK.getDefaultState();
	private static final BlockState ROAD_BLOCK = Blocks.GRAY_CONCRETE.getDefaultState();
	private static final BlockState ROAD_LINE = Blocks.YELLOW_CONCRETE.getDefaultState();
	private static final BlockState SIDEWALK = Blocks.SMOOTH_STONE.getDefaultState();

	private enum CellType {
		HOUSES,
		TOWER,
		PARK,
		WAREHOUSE
	}

	public CityChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	// ------------------------------------------------------------------
	// Cell layout
	// ------------------------------------------------------------------

	private static long cellSeed(long worldSeed, int cellX, int cellZ) {
		long seed = worldSeed ^ (cellX * 341873128712L + cellZ * 132897987541L);
		seed = seed * 6364136223846793005L + 1442695040888963407L;
		return seed ^ (seed >>> 31);
	}

	private static CellType cellType(long worldSeed, int cellX, int cellZ) {
		if (cellX == 0 && cellZ == 0) {
			return CellType.WAREHOUSE; // spawn cell always has a warehouse landmark
		}
		long hash = cellSeed(worldSeed, cellX, cellZ);
		int roll = (int) Math.floorMod(hash, 100);
		if (roll < 8) {
			return CellType.WAREHOUSE;
		}
		if (roll < 22) {
			return CellType.PARK;
		}
		if (roll < 40) {
			return CellType.TOWER;
		}
		return CellType.HOUSES;
	}

	// ------------------------------------------------------------------
	// Terrain
	// ------------------------------------------------------------------

	@Override
	public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
			StructureAccessor structureAccessor, Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		long worldSeed = LAYOUT_SALT;
		BlockPos.Mutable pos = new BlockPos.Mutable();

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = chunkPos.getStartX() + dx;
				int z = chunkPos.getStartZ() + dz;

				// Base terrain column.
				chunk.setBlockState(pos.set(x, chunk.getBottomY(), z), BEDROCK, false);
				for (int y = chunk.getBottomY() + 1; y < GROUND_Y; y++) {
					chunk.setBlockState(pos.set(x, y, z), STONE, false);
				}
				chunk.setBlockState(pos.set(x, GROUND_Y, z), this.surfaceBlock(worldSeed, x, z), false);
			}
		}

		this.buildCellFeatures(chunk, worldSeed);
		return CompletableFuture.completedFuture(chunk);
	}

	private BlockState surfaceBlock(long worldSeed, int x, int z) {
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		boolean roadX = lx < ROAD || lx >= CELL - ROAD;
		boolean roadZ = lz < ROAD || lz >= CELL - ROAD;

		if (roadX || roadZ) {
			// Dashed center lines along each road axis.
			if (roadX && !roadZ && Math.floorMod(x, CELL) == 0 && Math.floorMod(z, 6) < 3) {
				return ROAD_LINE;
			}
			if (roadZ && !roadX && Math.floorMod(z, CELL) == 0 && Math.floorMod(x, 6) < 3) {
				return ROAD_LINE;
			}
			return ROAD_BLOCK;
		}
		// Sidewalk ring just inside the road.
		if (lx < ROAD + 2 || lx >= CELL - ROAD - 2 || lz < ROAD + 2 || lz >= CELL - ROAD - 2) {
			return SIDEWALK;
		}
		CellType type = cellType(worldSeed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
		return switch (type) {
			case PARK, HOUSES -> GRASS;
			case TOWER, WAREHOUSE -> SIDEWALK;
		};
	}

	// ------------------------------------------------------------------
	// Buildings
	// ------------------------------------------------------------------

	/** Places above-ground features whose footprints intersect this chunk. */
	private void buildCellFeatures(Chunk chunk, long worldSeed) {
		ChunkPos chunkPos = chunk.getPos();
		int minCellX = Math.floorDiv(chunkPos.getStartX(), CELL);
		int minCellZ = Math.floorDiv(chunkPos.getStartZ(), CELL);
		int maxCellX = Math.floorDiv(chunkPos.getEndX(), CELL);
		int maxCellZ = Math.floorDiv(chunkPos.getEndZ(), CELL);

		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
				this.buildCell(chunk, worldSeed, cellX, cellZ);
			}
		}
	}

	private void buildCell(Chunk chunk, long worldSeed, int cellX, int cellZ) {
		long seed = cellSeed(worldSeed, cellX, cellZ);
		Random random = Random.create(seed);
		int originX = cellX * CELL;
		int originZ = cellZ * CELL;
		CellType type = cellType(worldSeed, cellX, cellZ);

		switch (type) {
			case HOUSES -> {
				// Four 11x11 lots, one house each (some lots stay gardens).
				buildHouse(chunk, random, originX + 5, originZ + 5, seed);
				buildHouse(chunk, random, originX + 17, originZ + 5, seed >> 8);
				buildHouse(chunk, random, originX + 5, originZ + 17, seed >> 16);
				buildHouse(chunk, random, originX + 17, originZ + 17, seed >> 24);
			}
			case TOWER -> buildTower(chunk, random, originX + 9, originZ + 9, seed);
			case PARK -> buildPark(chunk, random, originX, originZ, seed);
			case WAREHOUSE -> buildWarehouse(chunk, originX, originZ, seed);
		}

		// Street lamps at the four sidewalk corners of every cell.
		placeStreetLamp(chunk, originX + 4, originZ + 4);
		placeStreetLamp(chunk, originX + CELL - 5, originZ + 4);
		placeStreetLamp(chunk, originX + 4, originZ + CELL - 5);
		placeStreetLamp(chunk, originX + CELL - 5, originZ + CELL - 5);

		// ATM on some corners (every cell whose hash divides by 3).
		if (Math.floorMod(seed, 3) == 0) {
			set(chunk, originX + 6, GROUND_Y + 1, originZ + 4,
					ModBlocks.ATM.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		}
	}

	private void buildHouse(Chunk chunk, Random random, int x0, int z0, long variantSeed) {
		if (Math.floorMod(variantSeed, 5) == 0) {
			return; // empty garden lot
		}
		int size = 10;
		int height = 5;
		int y0 = GROUND_Y + 1;
		BlockState wall = switch ((int) Math.floorMod(variantSeed >> 3, 4)) {
			case 0 -> Blocks.WHITE_TERRACOTTA.getDefaultState();
			case 1 -> Blocks.BRICKS.getDefaultState();
			case 2 -> Blocks.ORANGE_TERRACOTTA.getDefaultState();
			default -> Blocks.LIGHT_GRAY_TERRACOTTA.getDefaultState();
		};
		BlockState floor = Blocks.OAK_PLANKS.getDefaultState();
		BlockState roof = Blocks.DARK_OAK_PLANKS.getDefaultState();

		for (int dx = 0; dx < size; dx++) {
			for (int dz = 0; dz < size; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;

				set(chunk, x, GROUND_Y, z, floor);
				for (int dy = 0; dy < height; dy++) {
					if (edge) {
						boolean window = dy == 2 && (dx % 3 == 1 || dz % 3 == 1)
								&& !(dx == 0 && dz == 0) && !(dx == size - 1 && dz == size - 1);
						set(chunk, x, y0 + dy, z, window ? Blocks.GLASS.getDefaultState() : wall);
					} else {
						set(chunk, x, y0 + dy, z, AIR);
					}
				}
				set(chunk, x, y0 + height, z, roof);
			}
		}

		// Door opening facing south.
		int doorX = x0 + size / 2;
		int doorZ = z0 + size - 1;
		set(chunk, doorX, y0, doorZ, AIR);
		set(chunk, doorX, y0 + 1, doorZ, AIR);

		// Furnishings: bed, table + chair, lamp, TV. Amazing Home™ approved.
		set(chunk, x0 + 2, y0, z0 + 3,
				Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.SOUTH));
		set(chunk, x0 + 2, y0, z0 + 2,
				Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.SOUTH));
		set(chunk, x0 + size - 3, y0, z0 + 2, ModBlocks.TABLE.getDefaultState());
		set(chunk, x0 + size - 4, y0, z0 + 2,
				ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		set(chunk, x0 + 2, y0, z0 + size - 3, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
		set(chunk, x0 + size - 3, y0, z0 + size - 3,
				ModBlocks.TV.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH)
						.with(TvBlock.ON, random.nextBoolean()));
	}

	private void buildTower(Chunk chunk, Random random, int x0, int z0, long variantSeed) {
		int size = 14;
		int floors = 3 + (int) Math.floorMod(variantSeed, 3);
		int floorHeight = 4;
		BlockState wall = Math.floorMod(variantSeed, 2) == 0
				? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState()
				: Blocks.WHITE_CONCRETE.getDefaultState();

		for (int dx = 0; dx < size; dx++) {
			for (int dz = 0; dz < size; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;

				for (int floor = 0; floor < floors; floor++) {
					int base = GROUND_Y + floor * floorHeight;
					set(chunk, x, base, z, Blocks.SMOOTH_STONE.getDefaultState());
					for (int dy = 1; dy < floorHeight; dy++) {
						if (edge) {
							boolean window = dy == 2 && (dx % 2 == 1 || dz % 2 == 1);
							set(chunk, x, base + dy, z,
									window ? Blocks.GLASS.getDefaultState() : wall);
						} else {
							set(chunk, x, base + dy, z, AIR);
						}
					}
				}
				set(chunk, x, GROUND_Y + floors * floorHeight, z, Blocks.SMOOTH_STONE.getDefaultState());
			}
		}

		// Lobby door + a lamp inside each floor.
		int doorX = x0 + size / 2;
		set(chunk, doorX, GROUND_Y + 1, z0 + size - 1, AIR);
		set(chunk, doorX, GROUND_Y + 2, z0 + size - 1, AIR);
		for (int floor = 0; floor < floors; floor++) {
			int base = GROUND_Y + floor * floorHeight;
			set(chunk, x0 + 2, base + 1, z0 + 2, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
			set(chunk, x0 + size - 3, base + 1, z0 + size - 3,
					ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
		}
	}

	private void buildPark(Chunk chunk, Random random, int originX, int originZ, long seed) {
		// A few trees, flowers, and a bench (chairs, of course).
		for (int i = 0; i < 5; i++) {
			int x = originX + 6 + (int) Math.floorMod(seed >> (i * 7), 20);
			int z = originZ + 6 + (int) Math.floorMod(seed >> (i * 11 + 3), 20);
			int trunkHeight = 4 + (i % 2);
			for (int dy = 1; dy <= trunkHeight; dy++) {
				set(chunk, x, GROUND_Y + dy, z, Blocks.OAK_LOG.getDefaultState());
			}
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int dy = trunkHeight - 1; dy <= trunkHeight + 1; dy++) {
						if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - trunkHeight) <= 3
								&& !(dx == 0 && dz == 0 && dy <= trunkHeight)) {
							set(chunk, x + dx, GROUND_Y + dy, z + dz, Blocks.OAK_LEAVES.getDefaultState());
						}
					}
				}
			}
		}
		for (int i = 0; i < 10; i++) {
			int x = originX + 5 + (int) Math.floorMod(seed >> (i * 5), 22);
			int z = originZ + 5 + (int) Math.floorMod(seed >> (i * 3 + 13), 22);
			set(chunk, x, GROUND_Y + 1,  z, (i % 2 == 0 ? Blocks.POPPY : Blocks.DANDELION).getDefaultState());
		}
		set(chunk, originX + 15, GROUND_Y + 1, originZ + 15,
				ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		set(chunk, originX + 16, GROUND_Y + 1, originZ + 15,
				ModBlocks.CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
	}

	private void buildWarehouse(Chunk chunk, int originX, int originZ, long seed) {
		int x0 = originX + 4;
		int z0 = originZ + 4;
		int size = 24;
		int height = 8;
		int y0 = GROUND_Y + 1;
		BlockState wall = Blocks.GRAY_CONCRETE.getDefaultState();
		BlockState trim = Blocks.BLUE_CONCRETE.getDefaultState();

		for (int dx = 0; dx < size; dx++) {
			for (int dz = 0; dz < size; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;

				set(chunk, x, GROUND_Y, z, Blocks.SMOOTH_STONE.getDefaultState());
				for (int dy = 0; dy < height; dy++) {
					if (edge) {
						BlockState state = dy == height - 2 ? trim
								: (dy >= 3 && dy <= 4 && (dx % 4 == 2 || dz % 4 == 2)
										? Blocks.GLASS.getDefaultState() : wall);
						set(chunk, x, y0 + dy, z, state);
					} else {
						set(chunk, x, y0 + dy, z, AIR);
					}
				}
				set(chunk, x, y0 + height, z, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
			}
		}

		// Big loading-dock opening on the south face.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = 0; dy < 4; dy++) {
				set(chunk, x0 + size / 2 + dx, y0 + dy, z0 + size - 1, AIR);
			}
		}

		// Shelf rows stacked with Amazing packages.
		for (int row = 3; row < size - 3; row += 4) {
			for (int dz = 3; dz < size - 5; dz++) {
				int x = x0 + row;
				int z = z0 + dz;
				set(chunk, x, y0, z, ModBlocks.PACKAGE_BLOCK.getDefaultState());
				if ((dz + row) % 3 != 0) {
					set(chunk, x, y0 + 1, z, ModBlocks.PACKAGE_BLOCK.getDefaultState());
				}
				if ((dz + row) % 4 == 0) {
					set(chunk, x, y0 + 2, z, ModBlocks.PACKAGE_BLOCK.getDefaultState());
				}
			}
		}

		// Interior lighting.
		for (int dx = 5; dx < size - 4; dx += 6) {
			for (int dz = 5; dz < size - 4; dz += 6) {
				set(chunk, x0 + dx, y0 + height - 1, z0 + dz,
						ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
			}
		}

		// Smile on the front wall, in blue trim... we tried.
		for (int dx = 6; dx < size - 6; dx++) {
			set(chunk, x0 + dx, y0 + 5, z0 + size - 1, Blocks.ORANGE_CONCRETE.getDefaultState());
		}
	}

	private void placeStreetLamp(Chunk chunk, int x, int z) {
		for (int dy = 1; dy <= 3; dy++) {
			set(chunk, x, GROUND_Y + dy, z, Blocks.COBBLESTONE_WALL.getDefaultState());
		}
		set(chunk, x, GROUND_Y + 4, z, ModBlocks.LAMP.getDefaultState().with(LampBlock.LIT, true));
	}

	/** Sets a block only when the position falls inside this chunk. */
	private static void set(Chunk chunk, int x, int y, int z, BlockState state) {
		ChunkPos pos = chunk.getPos();
		if (x >= pos.getStartX() && x <= pos.getEndX() && z >= pos.getStartZ() && z <= pos.getEndZ()) {
			chunk.setBlockState(new BlockPos(x, y, z), state, false);
		}
	}

	// ------------------------------------------------------------------
	// Entities
	// ------------------------------------------------------------------

	@Override
	public void populateEntities(ChunkRegion region) {
		ChunkPos center = region.getCenterPos();
		long worldSeed = LAYOUT_SALT;

		// Spawn NPCs once per cell, from the chunk holding the cell origin.
		int cellX = Math.floorDiv(center.getStartX(), CELL);
		int cellZ = Math.floorDiv(center.getStartZ(), CELL);
		int originX = cellX * CELL;
		int originZ = cellZ * CELL;
		if (Math.floorDiv(originX, 16) != center.x || Math.floorDiv(originZ, 16) != center.z) {
			return;
		}

		CellType type = cellType(worldSeed, cellX, cellZ);
		Random random = Random.create(cellSeed(worldSeed, cellX, cellZ));

		if (type == CellType.HOUSES || type == CellType.TOWER || type == CellType.PARK) {
			int count = type == CellType.TOWER ? 3 : 2;
			for (int i = 0; i < count; i++) {
				CitizenEntity citizen = ModEntities.CITIZEN.create(region.toServerWorld());
				if (citizen != null) {
					citizen.refreshPositionAndAngles(originX + 8 + random.nextInt(16), GROUND_Y + 1,
							originZ + 8 + random.nextInt(16), random.nextFloat() * 360.0f, 0.0f);
					citizen.initialize(region, region.getLocalDifficulty(citizen.getBlockPos()),
							net.minecraft.entity.SpawnReason.CHUNK_GENERATION, null);
					region.spawnEntity(citizen);
				}
			}
		} else if (type == CellType.WAREHOUSE) {
			for (int i = 0; i < 2; i++) {
				DeliveryWorkerEntity worker = ModEntities.DELIVERY_WORKER.create(region.toServerWorld());
				if (worker != null) {
					worker.refreshPositionAndAngles(originX + 10 + random.nextInt(12), GROUND_Y + 1,
							originZ + 26 + random.nextInt(4), random.nextFloat() * 360.0f, 0.0f);
					region.spawnEntity(worker);
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Boilerplate
	// ------------------------------------------------------------------

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
			StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public int getWorldHeight() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return GROUND_Y - 1;
	}

	@Override
	public int getMinimumY() {
		return -64;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return GROUND_Y + 1;
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		BlockState[] states = new BlockState[MathHelper.clamp(GROUND_Y + 1 - world.getBottomY(), 1, world.getHeight())];
		for (int i = 0; i < states.length; i++) {
			states[i] = STONE;
		}
		return new VerticalBlockSample(world.getBottomY(), states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
	}
}
