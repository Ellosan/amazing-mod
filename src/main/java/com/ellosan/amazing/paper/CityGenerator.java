package com.ellosan.amazing.paper;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.HeightMap;
import org.bukkit.Location;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Random;

/**
 * Amazing City™ for Paper — the same infinite road-grid city as the Fabric
 * mod, ported to Bukkit's generator API with vanilla blocks only: houses
 * with bedrooms/kitchens/living rooms, offices with lodestone elevators,
 * banks, cinemas, parks, and (rarely) Amazing warehouses full of stocked
 * barrels. Citizens included.
 *
 * <p>Enable per world in bukkit.yml:
 * <pre>worlds:
 *   world:
 *     generator: AmazingPaper</pre>
 */
public class CityGenerator extends ChunkGenerator {
	public static final int GROUND_Y = 64;
	private static final long LAYOUT_SALT = 0xA11A2196L;
	private static final int CELL = 32;
	private static final int ROAD = 2;

	enum CellType {
		HOUSES, TOWER, PARK, WAREHOUSE, OFFICE, BANK, CINEMA
	}

	static long cellSeed(int cellX, int cellZ) {
		long seed = LAYOUT_SALT ^ (cellX * 341873128712L + cellZ * 132897987541L);
		seed = seed * 6364136223846793005L + 1442695040888963407L;
		return seed ^ (seed >>> 31);
	}

	static CellType cellType(int cellX, int cellZ) {
		if (cellX == 0 && cellZ == 0) {
			return CellType.WAREHOUSE;
		}
		int roll = (int) Math.floorMod(cellSeed(cellX, cellZ), 100);
		if (roll < 3) return CellType.WAREHOUSE;
		if (roll < 7) return CellType.BANK;
		if (roll < 12) return CellType.CINEMA;
		if (roll < 24) return CellType.PARK;
		if (roll < 40) return CellType.OFFICE;
		if (roll < 52) return CellType.TOWER;
		return CellType.HOUSES;
	}

	/** True when the world position is inside a warehouse interior (for barrel loot). */
	public static boolean isWarehouseCell(int x, int z) {
		return cellType(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)) == CellType.WAREHOUSE;
	}

	// ------------------------------------------------------------------
	// Generation
	// ------------------------------------------------------------------

	@Override
	public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = chunkX * 16 + dx;
				int z = chunkZ * 16 + dz;
				data.setBlock(dx, data.getMinHeight(), dz, Material.BEDROCK);
				for (int y = data.getMinHeight() + 1; y < GROUND_Y; y++) {
					data.setBlock(dx, y, dz, Material.STONE);
				}
				data.setBlock(dx, GROUND_Y, dz, surface(x, z));
			}
		}

		// Build every cell whose footprint intersects this chunk.
		int minCellX = Math.floorDiv(chunkX * 16, CELL);
		int minCellZ = Math.floorDiv(chunkZ * 16, CELL);
		int maxCellX = Math.floorDiv(chunkX * 16 + 15, CELL);
		int maxCellZ = Math.floorDiv(chunkZ * 16 + 15, CELL);
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
				new CellBuilder(data, chunkX, chunkZ, cellX, cellZ).build();
			}
		}
	}

	private static Material surface(int x, int z) {
		int lx = Math.floorMod(x, CELL);
		int lz = Math.floorMod(z, CELL);
		boolean roadX = lx < ROAD || lx >= CELL - ROAD;
		boolean roadZ = lz < ROAD || lz >= CELL - ROAD;
		if (roadX || roadZ) {
			if (roadX && !roadZ && Math.floorMod(x, CELL) == 0 && Math.floorMod(z, 6) < 3) {
				return Material.YELLOW_CONCRETE;
			}
			if (roadZ && !roadX && Math.floorMod(z, CELL) == 0 && Math.floorMod(x, 6) < 3) {
				return Material.YELLOW_CONCRETE;
			}
			return Material.GRAY_CONCRETE;
		}
		if (lx < ROAD + 2 || lx >= CELL - ROAD - 2 || lz < ROAD + 2 || lz >= CELL - ROAD - 2) {
			return Material.SMOOTH_STONE;
		}
		return switch (cellType(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL))) {
			case PARK, HOUSES -> Material.GRASS_BLOCK;
			default -> Material.SMOOTH_STONE;
		};
	}

	@Override
	public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
		return GROUND_Y + 1;
	}

	@Override
	public boolean shouldGenerateNoise() {
		return false;
	}

	@Override
	public boolean shouldGenerateSurface() {
		return false;
	}

	@Override
	public boolean shouldGenerateCaves() {
		return false;
	}

	@Override
	public boolean shouldGenerateDecorations() {
		return false;
	}

	@Override
	public boolean shouldGenerateMobs() {
		return false;
	}

	@Override
	public boolean shouldGenerateStructures() {
		return false;
	}

	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return List.of(new CitizenPopulator());
	}

	// ------------------------------------------------------------------
	// Cell builder (chunk-bounded block placement)
	// ------------------------------------------------------------------

	private static final class CellBuilder {
		private final ChunkData data;
		private final int minX;
		private final int minZ;
		private final int originX;
		private final int originZ;
		private final long seed;
		private final CellType type;

		CellBuilder(ChunkData data, int chunkX, int chunkZ, int cellX, int cellZ) {
			this.data = data;
			this.minX = chunkX * 16;
			this.minZ = chunkZ * 16;
			this.originX = cellX * CELL;
			this.originZ = cellZ * CELL;
			this.seed = cellSeed(cellX, cellZ);
			this.type = cellType(cellX, cellZ);
		}

		void set(int x, int y, int z, Material material) {
			if (x >= this.minX && x < this.minX + 16 && z >= this.minZ && z < this.minZ + 16) {
				this.data.setBlock(x - this.minX, y, z - this.minZ, material);
			}
		}

		void set(int x, int y, int z, BlockData blockData) {
			if (x >= this.minX && x < this.minX + 16 && z >= this.minZ && z < this.minZ + 16) {
				this.data.setBlock(x - this.minX, y, z - this.minZ, blockData);
			}
		}

		void chair(int x, int y, int z, BlockFace facing) {
			Stairs stairs = (Stairs) Material.OAK_STAIRS.createBlockData();
			stairs.setFacing(facing);
			this.set(x, y, z, stairs);
		}

		void bed(int x, int y, int z) {
			Bed head = (Bed) Material.BLUE_BED.createBlockData();
			head.setPart(Bed.Part.HEAD);
			head.setFacing(BlockFace.SOUTH);
			Bed foot = (Bed) Material.BLUE_BED.createBlockData();
			foot.setPart(Bed.Part.FOOT);
			foot.setFacing(BlockFace.SOUTH);
			this.set(x, y, z + 1, head);
			this.set(x, y, z, foot);
		}

		void table(int x, int y, int z) {
			this.set(x, y, z, Material.OAK_FENCE);
			this.set(x, y + 1, z, Material.BROWN_CARPET);
		}

		void build() {
			switch (this.type) {
				case HOUSES -> this.house();
				case OFFICE -> this.office();
				case TOWER -> this.tower();
				case PARK -> this.park();
				case WAREHOUSE -> this.warehouse();
				case BANK -> this.bank();
				case CINEMA -> this.cinema();
			}
			// Street lamps at cell corners.
			for (int[] corner : new int[][] {{4, 4}, {CELL - 5, 4}, {4, CELL - 5}, {CELL - 5, CELL - 5}}) {
				int x = this.originX + corner[0];
				int z = this.originZ + corner[1];
				for (int dy = 1; dy <= 3; dy++) {
					this.set(x, GROUND_Y + dy, z, Material.COBBLESTONE_WALL);
				}
				this.set(x, GROUND_Y + 4, z, Material.SEA_LANTERN);
			}
		}

		private void shell(int x0, int z0, int width, int depth, int height,
				Material wall, Material accent, Material floor, Material roof, int windowEvery) {
			for (int dx = 0; dx < width; dx++) {
				for (int dz = 0; dz < depth; dz++) {
					int x = x0 + dx;
					int z = z0 + dz;
					boolean edge = dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1;
					boolean corner = (dx == 0 || dx == width - 1) && (dz == 0 || dz == depth - 1);
					this.set(x, GROUND_Y, z, floor);
					for (int dy = 1; dy <= height; dy++) {
						if (edge) {
							boolean window = windowEvery > 0 && dy >= 2 && dy <= 3 && !corner
									&& (dx % windowEvery == 1 || dz % windowEvery == 1);
							this.set(x, GROUND_Y + dy, z,
									corner ? accent : (window ? Material.GLASS : wall));
						} else {
							this.set(x, GROUND_Y + dy, z, Material.AIR);
						}
					}
					this.set(x, GROUND_Y + height + 1, z, roof);
				}
			}
		}

		private void door(int x, int z) {
			this.set(x, GROUND_Y + 1, z, Material.AIR);
			this.set(x, GROUND_Y + 2, z, Material.AIR);
		}

		private void house() {
			Material[][] palettes = {
					{Material.WHITE_TERRACOTTA, Material.DARK_OAK_PLANKS},
					{Material.BRICKS, Material.STONE},
					{Material.ORANGE_TERRACOTTA, Material.WHITE_TERRACOTTA},
					{Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE},
					{Material.SPRUCE_PLANKS, Material.COBBLESTONE},
					{Material.SANDSTONE, Material.SMOOTH_SANDSTONE},
					{Material.WHITE_CONCRETE, Material.BLUE_CONCRETE},
					{Material.MUD_BRICKS, Material.BRICKS},
			};
			Material[] palette = palettes[(int) Math.floorMod(this.seed, palettes.length)];
			boolean pool = Math.floorMod(this.seed >> 7, 2) == 0;
			int x0 = this.originX + 6;
			int z0 = this.originZ + 6;
			int width = 20;
			int depth = 14;

			this.shell(x0, z0, width, depth, 4, palette[0], palette[1],
					Material.OAK_PLANKS, Material.DARK_OAK_PLANKS, 4);
			int doorX = x0 + width / 2;
			this.door(doorX, z0 + depth - 1);

			// Interior partitions: hall cross.
			int wallX = x0 + 12;
			int wallZ = z0 + 7;
			for (int dz = 1; dz < depth - 1; dz++) {
				if (Math.abs(z0 + dz - wallZ) > 1) {
					for (int dy = 1; dy <= 3; dy++) {
						this.set(wallX, GROUND_Y + dy, z0 + dz, palette[0]);
					}
				}
			}
			for (int dx = 1; dx < width - 1; dx++) {
				if (Math.abs(x0 + dx - wallX) > 1 && Math.abs(x0 + dx - doorX) > 1) {
					for (int dy = 1; dy <= 3; dy++) {
						this.set(x0 + dx, GROUND_Y + dy, wallZ, palette[0]);
					}
				}
			}

			// Living room (SW): TV wall (black concrete), chairs, table.
			this.set(x0 + 5, GROUND_Y + 2, z0 + 10, Material.BLACK_CONCRETE);
			this.chair(x0 + 4, GROUND_Y + 1, z0 + 11, BlockFace.WEST);
			this.chair(x0 + 5, GROUND_Y + 1, z0 + 12, BlockFace.NORTH);
			this.table(x0 + 6, GROUND_Y + 1, z0 + 11);
			// Kitchen (NW).
			this.set(x0 + 3, GROUND_Y + 1, z0 + 2, Material.FURNACE);
			this.set(x0 + 4, GROUND_Y + 1, z0 + 2, Material.SMOKER);
			this.set(x0 + 5, GROUND_Y + 1, z0 + 2, Material.CRAFTING_TABLE);
			this.set(x0 + 6, GROUND_Y + 1, z0 + 2, Material.BARREL);
			// Bedroom (NE).
			this.bed(x0 + 15, GROUND_Y + 1, z0 + 2);
			this.set(x0 + 17, GROUND_Y + 1, z0 + 2, Material.CHEST);
			// Dining/office (SE).
			this.table(x0 + 15, GROUND_Y + 1, z0 + 10);
			this.chair(x0 + 14, GROUND_Y + 1, z0 + 10, BlockFace.EAST);
			this.chair(x0 + 16, GROUND_Y + 1, z0 + 10, BlockFace.WEST);
			this.set(x0 + 17, GROUND_Y + 1, z0 + 11, Material.BOOKSHELF);
			// Lights.
			this.set(doorX, GROUND_Y + 4, z0 + depth - 3, Material.SEA_LANTERN);
			this.set(x0 + 4, GROUND_Y + 4, z0 + 4, Material.SEA_LANTERN);

			// Yard: pool or garden.
			int yardZ = z0 + depth + 2;
			if (pool) {
				for (int dx = 0; dx < 6; dx++) {
					for (int dz = 0; dz < 4; dz++) {
						boolean rim = dx == 0 || dz == 0 || dx == 5 || dz == 3;
						this.set(x0 + 2 + dx, GROUND_Y, yardZ + dz,
								rim ? Material.SMOOTH_QUARTZ : Material.WATER);
					}
				}
			} else {
				for (int i = 0; i < 6; i++) {
					int fx = x0 + 2 + (int) Math.floorMod(this.seed >> (9 + i * 3), width - 4);
					this.set(fx, GROUND_Y + 1, yardZ + (i % 3),
							i % 2 == 0 ? Material.POPPY : Material.OXEYE_DAISY);
				}
			}
		}

		private void office() {
			int x0 = this.originX + 4;
			int z0 = this.originZ + 4;
			int size = 24;
			int floors = 3 + (int) Math.floorMod(this.seed >> 3, 3);
			int floorHeight = 4;
			Material facade = Math.floorMod(this.seed, 2) == 0
					? Material.LIGHT_GRAY_CONCRETE : Material.WHITE_CONCRETE;

			for (int f = 0; f < floors; f++) {
				int base = GROUND_Y + f * floorHeight;
				for (int dx = 0; dx < size; dx++) {
					for (int dz = 0; dz < size; dz++) {
						int x = x0 + dx;
						int z = z0 + dz;
						boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
						boolean corner = (dx == 0 || dx == size - 1) && (dz == 0 || dz == size - 1);
						this.set(x, base, z, f == 0 ? Material.SMOOTH_STONE : facade);
						for (int dy = 1; dy < floorHeight; dy++) {
							if (edge) {
								boolean band = dy >= 2 && !corner && (dx % 2 == 1 || dz % 2 == 1);
								this.set(x, base + dy, z, band ? Material.GLASS : facade);
							} else {
								this.set(x, base + dy, z, Material.AIR);
							}
						}
					}
				}
				// Lodestone elevator pads (jump = up, sneak = down).
				this.set(x0 + 2, base, z0 + 2, Material.LODESTONE);
				this.set(x0 + 3, base, z0 + 2, Material.LODESTONE);
				// Desks.
				if (f > 0) {
					for (int dz = 5; dz < size - 5; dz += 4) {
						for (int dx = 6; dx < size - 5; dx += 5) {
							this.table(x0 + dx, base + 1, z0 + dz);
							this.chair(x0 + dx, base + 1, z0 + dz + 1, BlockFace.NORTH);
						}
					}
					this.set(x0 + size / 2, base + floorHeight - 1, z0 + size / 2, Material.SEA_LANTERN);
				}
			}
			int roofY = GROUND_Y + floors * floorHeight;
			for (int dx = 0; dx < size; dx++) {
				for (int dz = 0; dz < size; dz++) {
					this.set(x0 + dx, roofY, z0 + dz, Material.GRAY_CONCRETE);
				}
			}
			int doorX = x0 + size / 2;
			this.door(doorX, z0 + size - 1);
			this.door(doorX + 1, z0 + size - 1);
			this.set(doorX, GROUND_Y + 3, z0 + size - 5, Material.SEA_LANTERN);
		}

		private void tower() {
			int x0 = this.originX + 9;
			int z0 = this.originZ + 9;
			int size = 14;
			int floors = 4 + (int) Math.floorMod(this.seed, 2);
			Material wall = Math.floorMod(this.seed, 2) == 0
					? Material.LIGHT_GRAY_CONCRETE : Material.CYAN_TERRACOTTA;
			for (int f = 0; f < floors; f++) {
				int base = GROUND_Y + f * 4;
				for (int dx = 0; dx < size; dx++) {
					for (int dz = 0; dz < size; dz++) {
						boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
						this.set(x0 + dx, base, z0 + dz, Material.SMOOTH_STONE);
						for (int dy = 1; dy < 4; dy++) {
							boolean window = dy == 2 && (dx % 2 == 1 || dz % 2 == 1);
							this.set(x0 + dx, base + dy, z0 + dz,
									edge ? (window ? Material.GLASS : wall) : Material.AIR);
						}
					}
				}
				this.set(x0 + size - 3, base, z0 + 2, Material.LODESTONE);
				this.set(x0 + 2, base + 1, z0 + 2, Material.SEA_LANTERN);
			}
			for (int dx = 0; dx < size; dx++) {
				for (int dz = 0; dz < size; dz++) {
					this.set(x0 + dx, GROUND_Y + floors * 4, z0 + dz, Material.SMOOTH_STONE);
				}
			}
			this.door(x0 + size / 2, z0 + size - 1);
		}

		private void park() {
			Random random = new Random(this.seed);
			for (int i = 0; i < 4; i++) {
				int x = this.originX + 8 + random.nextInt(16);
				int z = this.originZ + 8 + random.nextInt(16);
				int trunk = 4 + random.nextInt(2);
				for (int dy = 1; dy <= trunk; dy++) {
					this.set(x, GROUND_Y + dy, z, Material.OAK_LOG);
				}
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						for (int dy = trunk - 1; dy <= trunk + 1; dy++) {
							if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - trunk) <= 3) {
								this.set(x + dx, GROUND_Y + dy, z + dz, Material.OAK_LEAVES);
							}
						}
					}
				}
			}
			for (int i = 0; i < 8; i++) {
				this.set(this.originX + 6 + random.nextInt(20), GROUND_Y + 1,
						this.originZ + 6 + random.nextInt(20),
						i % 2 == 0 ? Material.POPPY : Material.DANDELION);
			}
			this.chair(this.originX + 15, GROUND_Y + 1, this.originZ + 15, BlockFace.SOUTH);
			this.chair(this.originX + 16, GROUND_Y + 1, this.originZ + 15, BlockFace.SOUTH);
		}

		private void warehouse() {
			int x0 = this.originX + 4;
			int z0 = this.originZ + 4;
			int size = 24;
			this.shell(x0, z0, size, size, 7, Material.GRAY_CONCRETE, Material.BLUE_CONCRETE,
					Material.SMOOTH_STONE, Material.LIGHT_GRAY_CONCRETE, 4);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = 1; dy <= 4; dy++) {
					this.set(x0 + size / 2 + dx, GROUND_Y + dy, z0 + size - 1, Material.AIR);
				}
			}
			// Shelf rows of stocked barrels (loot fills on first open).
			for (int row = 3; row < size - 3; row += 4) {
				for (int dz = 3; dz < size - 5; dz++) {
					this.set(x0 + row, GROUND_Y + 1, z0 + dz, Material.BARREL);
					if ((dz + row) % 3 != 0) {
						this.set(x0 + row, GROUND_Y + 2, z0 + dz, Material.BARREL);
					}
				}
			}
			for (int dx = 5; dx < size - 4; dx += 6) {
				for (int dz = 5; dz < size - 4; dz += 6) {
					this.set(x0 + dx, GROUND_Y + 6, z0 + dz, Material.SEA_LANTERN);
				}
			}
			for (int dx = 6; dx < size - 6; dx++) {
				this.set(x0 + dx, GROUND_Y + 5, z0 + size - 1, Material.ORANGE_CONCRETE);
			}
		}

		private void bank() {
			int x0 = this.originX + 6;
			int z0 = this.originZ + 8;
			this.shell(x0, z0, 20, 15, 5, Material.QUARTZ_BLOCK, Material.QUARTZ_PILLAR,
					Material.POLISHED_ANDESITE, Material.SMOOTH_QUARTZ, 3);
			// Gold trim.
			for (int dx = 0; dx < 20; dx++) {
				this.set(x0 + dx, GROUND_Y + 4, z0, Material.GOLD_BLOCK);
				this.set(x0 + dx, GROUND_Y + 4, z0 + 14, Material.GOLD_BLOCK);
			}
			int doorX = x0 + 10;
			this.door(doorX, z0 + 14);
			this.door(doorX + 1, z0 + 14);
			// Teller counter.
			for (int dx = 3; dx < 17; dx++) {
				this.table(x0 + dx, GROUND_Y + 1, z0 + 5);
			}
			// Vault with gold.
			for (int dx = 1; dx < 8; dx++) {
				for (int dy = 1; dy <= 3; dy++) {
					this.set(x0 + dx, GROUND_Y + dy, z0 + 3, Material.IRON_BLOCK);
				}
			}
			this.set(x0 + 4, GROUND_Y + 1, z0 + 3, Material.AIR);
			this.set(x0 + 4, GROUND_Y + 2, z0 + 3, Material.AIR);
			this.set(x0 + 2, GROUND_Y + 1, z0 + 1, Material.GOLD_BLOCK);
			this.set(x0 + 3, GROUND_Y + 1, z0 + 1, Material.GOLD_BLOCK);
			this.set(x0 + 6, GROUND_Y + 1, z0 + 1, Material.BARREL);
			this.set(doorX, GROUND_Y + 4, z0 + 10, Material.SEA_LANTERN);
		}

		private void cinema() {
			int x0 = this.originX + 4;
			int z0 = this.originZ + 6;
			int width = 24;
			int depth = 18;
			this.shell(x0, z0, width, depth, 6, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE,
					Material.RED_WOOL, Material.BLACK_CONCRETE, 0);
			// Screen wall (glowing).
			for (int dx = 6; dx < width - 6; dx++) {
				for (int dy = 2; dy <= 5; dy++) {
					this.set(x0 + dx, GROUND_Y + dy, z0 + 1,
							dy == 2 || dy == 5 ? Material.SMOOTH_QUARTZ : Material.SEA_LANTERN);
				}
			}
			// Seat rows.
			for (int row = 0; row < 4; row++) {
				int z = z0 + 6 + row * 2;
				for (int dx = 3; dx < width - 3; dx += 2) {
					if (Math.abs(dx - width / 2) < 2) {
						continue;
					}
					this.chair(x0 + dx, GROUND_Y + 1, z, BlockFace.NORTH);
				}
			}
			int doorX = x0 + width / 2;
			this.door(doorX, z0 + depth - 1);
			for (int dx = 4; dx < width - 4; dx++) {
				this.set(x0 + dx, GROUND_Y + 5, z0 + depth - 1, Material.ORANGE_CONCRETE);
			}
		}
	}

	// ------------------------------------------------------------------
	// Citizens
	// ------------------------------------------------------------------

	private static final class CitizenPopulator extends BlockPopulator {
		private static final String[] NAMES = {
				"Alex Doorstep", "Sam Porchly", "Casey Blocksworth", "Riley Lobbyist",
				"Jordan Stoop", "Morgan Mailbox", "Quinn Curbside", "Avery Awning"
		};

		@Override
		public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion region) {
			int cellX = Math.floorDiv(chunkX * 16, CELL);
			int cellZ = Math.floorDiv(chunkZ * 16, CELL);
			int originX = cellX * CELL;
			int originZ = cellZ * CELL;
			// Spawn once per cell, from the chunk containing the cell origin.
			if (Math.floorDiv(originX, 16) != chunkX || Math.floorDiv(originZ, 16) != chunkZ) {
				return;
			}
			CellType type = cellType(cellX, cellZ);
			if (type == CellType.WAREHOUSE) {
				return;
			}
			int count = type == CellType.OFFICE || type == CellType.TOWER ? 3 : 2;
			for (int i = 0; i < count; i++) {
				Location location = new Location(null,
						originX + 8 + random.nextInt(16) + 0.5, GROUND_Y + 1,
						originZ + 8 + random.nextInt(16) + 0.5);
				if (!region.isInRegion(location)) {
					continue;
				}
				if (region.spawnEntity(location, EntityType.VILLAGER) instanceof Villager citizen) {
					citizen.customName(Component.text(NAMES[random.nextInt(NAMES.length)],
							NamedTextColor.WHITE));
					citizen.setCustomNameVisible(true);
					citizen.setRemoveWhenFarAway(false);
				}
			}
		}
	}
}
