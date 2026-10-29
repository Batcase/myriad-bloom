package com.myriadbloom.world.gen;

import com.myriadbloom.CandyBlocks;
import com.myriadbloom.CandyContent;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.ModFluids;
import com.mojang.serialization.Codec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The big candy conversion: every column of the mirrored overworld terrain is walked and the vanilla
 * materials are swapped for candy ones:
 *
 * <ul>
 *   <li>stone family -&gt; {@code myriadbloom:candy_block} in one of sixteen colours,</li>
 *   <li>grass / dirt family -&gt; {@code myriadbloom:jelly_block} in one of sixteen colours,</li>
 *   <li>water and waterlogged blocks -&gt; {@code myriadbloom:syrup}, the dimension's fluid.</li>
 * </ul>
 *
 * <p>Colours are <b>large contiguous patches</b>, not per-block noise: a 2D Voronoi field with one
 * jittered centre per {@value #CANDY_CELL} blocks picks the colour of a whole area, so the hills read
 * as big red / blue / green regions with organic borders (and cliffs show vertical colour columns).</p>
 *
 * <p>A second pass reshapes the finished chunk:</p>
 * <ul>
 *   <li><b>valley clamp</b>: no column may sit more than two blocks below its highest neighbour, so
 *       the rivers are shallow candy streams instead of deep canyons;</li>
 *   <li><b>void seal</b>: the air between the sugar frost floor (Y &lt;= 6) and the bottom of the
 *       terrain mass is filled with candy blocks, overlapping the sugar layer, so the underground
 *       is solid instead of a hollow shell.</li>
 * </ul>
 *
 * <p>Performance matters here: this rewrites tens of thousands of blocks per chunk. The conversion
 * therefore writes straight into the chunk's {@link LevelChunkSection}s instead of going through
 * {@code WorldGenRegion#setBlock} (which pays a chunk lookup plus heightmap bookkeeping for every
 * single block). The swap never changes the block's motion-blocking or fluid state - candy and jelly
 * are solid like stone, syrup is a fluid like water - so the heightmaps stay correct, and the chunk's
 * light has not been computed yet at this generation stage. The resculpting pass touches far fewer
 * blocks and does go through {@code setBlock} so heightmaps stay right.</p>
 */
public class CandyTerrainFeature extends Feature<NoneFeatureConfiguration> {
    /** Edge length of the candy colour patches, in blocks. */
    private static final int CANDY_CELL = 72;
    /** Edge length of the jelly colour patches; the jelly layer is thinner, so slightly smaller. */
    private static final int JELLY_CELL = 52;
    /** How far a column may dip below its highest neighbour. */
    /** Hills are welcome below this height, peaks above it are squeezed. */
    private static final int SOFT_CAP = 78;
    /** Absolute skyline: nothing of the candy terrain is taller than this. */
    private static final int MAX_CAP = 96;
    /** How much of the height above SOFT_CAP is kept (0.3 = the peaks are flattened to a third). */
    private static final float PEAK_COMPRESSION = 0.3F;

    /** Squeezes tall mountains so the world stays hilly but never reaches absurd heights. */
    private static int compressTop(int top) {
        if (top <= SOFT_CAP) {
            return top;
        }
        int squeezed = SOFT_CAP + Math.round((top - SOFT_CAP) * PEAK_COMPRESSION);
        return Math.min(MAX_CAP, squeezed);
    }

    private static final int MAX_VALLEY_DEPTH = 2;

    /**
     * Colours handed out by world generation: the vivid candy hues only.
     *
     * <p>The full sixteen dye colours exist as block states (and stay available for building), but
     * terrain made of white / light grey / grey / black patches reads as washed-out stone at night,
     * which is exactly the "confetti but colourless" look this dimension should not have. Red,
     * orange, yellow, lime, green, cyan, light blue, blue, purple, magenta and pink give the
     * "large red area, large blue area" look that was asked for.</p>
     */
    private static final DyeColor[] WORLDGEN_COLORS = {
            DyeColor.RED,
            DyeColor.ORANGE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.GREEN,
            DyeColor.CYAN,
            DyeColor.LIGHT_BLUE,
            DyeColor.BLUE,
            DyeColor.PURPLE,
            DyeColor.MAGENTA,
            DyeColor.PINK,
    };

    /** Blocks of the stone family: replace with candy. */
    private static final Set<Block> STONE_LIKE = Set.of(
            Blocks.STONE,
            Blocks.DEEPSLATE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.TUFF,
            Blocks.CALCITE,
            Blocks.BASALT,
            Blocks.SMOOTH_BASALT,
            Blocks.DRIPSTONE_BLOCK,
            Blocks.SANDSTONE);

    /** Blocks of the soil family: replace with jelly. */
    private static final Set<Block> SOIL_LIKE = Set.of(
            Blocks.GRASS_BLOCK,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.PODZOL,
            Blocks.MYCELIUM,
            Blocks.MUD,
            Blocks.DIRT_PATH,
            Blocks.FARMLAND);

    /**
     * Loose, sand-like vanilla blocks: replace with sugar frost, the candy dimension's own "sand".
     * Vanilla sand and gravel would otherwise survive as grey-brown patches in the middle of the
     * candy landscape.
     */
    private static final Set<Block> SUGAR_LIKE = Set.of(
            Blocks.SAND,
            Blocks.GRAVEL,
            Blocks.CLAY,
            Blocks.SUSPICIOUS_SAND,
            Blocks.SUSPICIOUS_GRAVEL);

    /** The candy / jelly states per worldgen colour, the syrup source and the sugar frost block. */
    private static volatile BlockState[] candyStates;
    private static volatile BlockState[] jellyStates;
    private static volatile BlockState syrupState;
    private static volatile BlockState sugarState;
    /** Sweet grass block, replacing grass/dirt; jelly is now only a rare decoration pile. */
    private static volatile BlockState sweetGrassState;
    private static volatile BlockState sweetGrassPlantState;
    private static volatile BlockState candyGrassPlantState;

    public CandyTerrainFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public CandyTerrainFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /**
     * Resolves the registered states. Chunk generation runs on several worker threads, so the lazy
     * initialisation is synchronised and the arrays are published through volatile fields.
     */
    private static synchronized void ensureStates() {
        if (candyStates != null) {
            return;
        }
        DyeColor[] colors = WORLDGEN_COLORS;
        BlockState[] candy = new BlockState[colors.length];
        BlockState[] jelly = new BlockState[colors.length];
        for (int i = 0; i < colors.length; i++) {
            candy[i] = CandyBlocks.candy(colors[i]);
            jelly[i] = CandyBlocks.jelly(colors[i]);
        }
        candyStates = candy;
        jellyStates = jelly;
        syrupState = ModFluids.SYRUP_SOURCE.get().defaultFluidState().createLegacyBlock();
        sugarState = ModBlocks.SUGAR_FROST.get().defaultBlockState();
        sweetGrassState = CandyContent.SWEET_GRASS_BLOCK.get().defaultBlockState();
        sweetGrassPlantState = CandyContent.SWEET_GRASS.get().defaultBlockState();
        candyGrassPlantState = CandyContent.CANDY_GRASS.get().defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        ensureStates();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int minY = chunk.getMinBuildHeight();
        LevelChunkSection[] sections = chunk.getSections();

        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int baseY = minY + index * 16;
            // The band Y <= 6 belongs to CandyFloorFeature, which has already resculpted it.
            if (baseY + 15 <= CandyFloorFeature.SUGAR_MAX) {
                continue;
            }
            for (int localY = 0; localY < 16; localY++) {
                int worldY = baseY + localY;
                if (worldY <= CandyFloorFeature.SUGAR_MAX) {
                    continue;
                }
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (state.isAir()) {
                            continue;
                        }
                        int worldX = chunkMinX + localX;
                        int worldZ = chunkMinZ + localZ;
                        BlockState replacement = replacementFor(state, worldX, worldY, worldZ);
                        if (replacement != null) {
                            section.setBlockState(localX, localY, localZ, replacement, false);
                        }
                    }
                }
            }
        }

        resculpt(level, chunk, chunkMinX, chunkMinZ);
        return true;
    }

    /**
     * Column pass over the converted chunk: seals the void above the sugar floor and clamps valleys.
     */
    private static void resculpt(WorldGenLevel level, ChunkAccess chunk, int chunkMinX, int chunkMinZ) {
        int maxY = chunk.getMaxBuildHeight();
        int[][] surface = new int[16][16];
        int[][] bottom = new int[16][16];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = Integer.MIN_VALUE;
                int low = Integer.MIN_VALUE;
                boolean foundTop = false;
                for (int y = maxY - 1; y > CandyFloorFeature.SUGAR_MAX; y--) {
                    BlockState state = chunk.getBlockState(new BlockPos(chunkMinX + x, y, chunkMinZ + z));
                    boolean solid = !state.isAir();
                    if (!foundTop) {
                        if (solid) {
                            top = y;
                            foundTop = true;
                        }
                        continue;
                    }
                    if (!solid) {
                        low = y + 1;
                        break;
                    }
                }
                int capped = top == Integer.MIN_VALUE ? Integer.MIN_VALUE : compressTop(top);
                surface[z][x] = capped;
                bottom[z][x] = foundTop ? (low == Integer.MIN_VALUE ? top : low) : Integer.MIN_VALUE;
                if (top != Integer.MIN_VALUE && capped < top) {
                    // cut the peak down to the skyline and re-lay the candy surface on top of it
                    BlockState capFill = candyStates[voronoiColor(chunkMinX + x, chunkMinZ + z,
                            CANDY_CELL, 0x1F123BB5)];
                    for (int y = capped + 1; y <= top; y++) {
                        level.setBlock(new BlockPos(chunkMinX + x, y, chunkMinZ + z),
                                Blocks.AIR.defaultBlockState(), 2);
                    }
                    level.setBlock(new BlockPos(chunkMinX + x, capped, chunkMinZ + z), capFill, 2);
                }
            }
        }

        BlockState[] candy = candyStates;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int top = surface[z][x];
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                BlockState fill = candy[voronoiColor(chunkMinX + x, chunkMinZ + z, CANDY_CELL, 0x1F123BB5)];

                // 1) void between the sugar frost floor and the terrain mass -> solid candy
                int low = bottom[z][x];
                for (int y = CandyFloorFeature.SUGAR_MAX + 1; y < low; y++) {
                    BlockPos pos = new BlockPos(chunkMinX + x, y, chunkMinZ + z);
                    if (chunk.getBlockState(pos).isAir()) {
                        level.setBlock(pos, fill, 2);
                    }
                }

                // 2) shallow rivers: a column may not sit more than MAX_VALLEY_DEPTH below its
                //    highest neighbour inside the chunk
                int highest = top;
                for (int nz = -1; nz <= 1; nz++) {
                    for (int nx = -1; nx <= 1; nx++) {
                        int sx = x + nx;
                        int sz = z + nz;
                        if (sx < 0 || sx > 15 || sz < 0 || sz > 15) {
                            continue;
                        }
                        highest = Math.max(highest, surface[sz][sx]);
                    }
                }
                int floor = Math.min(highest - MAX_VALLEY_DEPTH, top + MAX_VALLEY_DEPTH);
                for (int y = top + 1; y <= floor; y++) {
                    level.setBlock(new BlockPos(chunkMinX + x, y, chunkMinZ + z), fill, 2);
                }

                // 3) surface decoration: rare jelly piles, otherwise candy plants
                decorate(level, chunkMinX + x, chunkMinZ + z, Math.max(top, floor));
            }
        }
    }

    /**
     * Puts a jelly pile (rare) or a candy plant (commoner) on a sweet grass surface. Both are purely
     * decorative; positions are hashed so a chunk always generates the same way.
     */
    private static void decorate(WorldGenLevel level, int x, int z, int surfaceY) {
        BlockPos groundPos = new BlockPos(x, surfaceY, z);
        if (!level.getBlockState(groundPos).is(CandyContent.SWEET_GRASS_BLOCK.get())) {
            return;
        }
        BlockPos above = groundPos.above();
        if (!level.getBlockState(above).isAir()) {
            return;
        }
        double roll = hash01(x, z, 0x51ED);
        if (roll < 0.015D) {
            BlockState[] jelly = jellyStates;
            BlockState pile = jelly[(int) (hash01(x, z, 0x7A3D) * jelly.length) % jelly.length];
            level.setBlock(above, pile, 2);
            if (hash01(x, z, 0x11C7) < 0.25D) {
                level.setBlock(above.above(), pile, 2);
            }
        } else if (roll < 0.35D) {
            BlockState plant = hash01(x, z, 0x3B9F) < 0.75D ? sweetGrassPlantState : candyGrassPlantState;
            level.setBlock(above, plant, 2);
        }
    }

    /** Stable 0..1 hash of a column, used so decoration is deterministic per position. */
    private static double hash01(int x, int z, int salt) {
        int hash = x * 0x9E3779B1 + z * 0x85EBCA77 + salt;
        hash ^= hash >>> 15;
        hash *= 0x2545F491;
        hash ^= hash >>> 13;
        return (hash & 0xFFFFFF) / (double) 0x1000000;
    }

    private static BlockState replacementFor(BlockState state, int x, int y, int z) {
        // Water first: it also covers waterlogged stairs, kelp, seagrass, coral, ...
        if (state.getFluidState().is(FluidTags.WATER)) {
            return syrupState;
        }
        Block block = state.getBlock();
        if (SUGAR_LIKE.contains(block)) {
            return sugarState;
        }
        if (STONE_LIKE.contains(block)) {
            return candyStates[voronoiColor(x, z, CANDY_CELL, 0x1F123BB5)];
        }
        if (SOIL_LIKE.contains(block)) {
            // grass / dirt are replaced by the sweet grass block; jelly is now only a rare decoration
            return sweetGrassState;
        }
        return null;
    }

    /**
     * Colour index of a block from a 2D Voronoi field: each cell edge of {@code cellSize} blocks gets
     * one jittered centre, and the colour of the nearest centre wins. That makes whole areas share a
     * colour instead of every block being independently random.
     */
    private static int voronoiColor(int x, int z, int cellSize, int salt) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        double px = x + 0.5D;
        double pz = z + 0.5D;
        double best = Double.MAX_VALUE;
        int color = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int gx = cellX + dx;
                int gz = cellZ + dz;
                int hash = mix(gx, gz, salt);
                // Jitter is limited to the middle half of the cell: fully random centres produce
                // the occasional tiny sliver cell, which reads as noise instead of a colour area.
                double centerX = gx * cellSize + (0.25D + 0.5D * ((hash & 0xFF) / 255.0D)) * cellSize;
                double centerZ = gz * cellSize + (0.25D + 0.5D * (((hash >>> 8) & 0xFF) / 255.0D)) * cellSize;
                double distance = (px - centerX) * (px - centerX) + (pz - centerZ) * (pz - centerZ);
                if (distance < best) {
                    best = distance;
                    color = ((hash >>> 16) & 0x7FFFFFFF) % WORLDGEN_COLORS.length;
                }
            }
        }
        return color;
    }

    /** Position hash used by the Voronoi field; stable across chunks and reloads. */
    private static int mix(int x, int z, int salt) {
        int hash = x * 0x9E3779B1 + z * 0x85EBCA77 + salt;
        hash ^= hash >>> 15;
        hash *= 0x2545F491;
        hash ^= hash >>> 13;
        return hash;
    }
}
