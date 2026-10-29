package com.myriadbloom.world.gen;

import com.myriadbloom.FrostedLandContent;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Shapes the 凝结之地: the surface becomes congealed ice, ice spikes rise around the edges and the
 * middle stays a wide open flat area reserved for the boss building.
 *
 * <p>The patch is one per 16x16 chunk region (see {@code FrostBiomeSource}); this feature recomputes
 * the same centre from the chunk position so it always agrees with the biome map.</p>
 */
public class CongealedLandFeature extends Feature<NoneFeatureConfiguration> {
    /** Half size of the biome patch, in blocks. */
    public static final int PATCH_RADIUS = 96;
    /** Ring around the patch that no other special biome may enter. */
    public static final int BUFFER_RADIUS = 24;
    /** Maximum outward wobble of the irregular patch outline. */
    public static final int MAX_WOBBLE = 36;
    /** Half size of the flat, spike free middle reserved for the boss building. */
    public static final int CENTRE_CLEARING = 18;
    /** Region size in blocks: 128x128 chunks. */
    public static final int REGION_BLOCKS = 2048;
    /** Region centre stays at least this many blocks away from the region border. */
    private static final int MARGIN = 640;

    public CongealedLandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public CongealedLandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Centre of the congealed patch that owns this chunk, derived from the region hash. */
    public static int[] patchCentre(int chunkX, int chunkZ) {
        // one patch per REGION_BLOCKS area - must be derived from the same constant, otherwise the
        // offsets of neighbouring regions overlap and two patches end up next to each other
        int regionChunks = REGION_BLOCKS >> 4;
        int regionX = Math.floorDiv(chunkX, regionChunks);
        int regionZ = Math.floorDiv(chunkZ, regionChunks);
        long hash = regionX * 341873128712L + regionZ * 132897987541L + 0x5EEDL;
        int offsetX = MARGIN + (int) (Math.floorMod(hash, REGION_BLOCKS - 2 * MARGIN) >> 1) * 2;
        int offsetZ = MARGIN + (int) (Math.floorMod(hash >> 16, REGION_BLOCKS - 2 * MARGIN) >> 1) * 2;
        return new int[]{regionX * REGION_BLOCKS + offsetX, regionZ * REGION_BLOCKS + offsetZ};
    }

    /**
     * True when the block is inside the patch: an irregular blob, not a square. The radius is wobbled
     * with two low frequency sines of the angle around the centre, so the border reads as a natural
     * biome edge instead of a straight cut.
     */
    public static boolean insidePatch(int blockX, int blockZ) {
        int[] centre = patchCentre(blockX >> 4, blockZ >> 4);
        int dx = blockX - centre[0];
        int dz = blockZ - centre[1];
        double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (distance > PATCH_RADIUS + MAX_WOBBLE) {
            return false;
        }
        double angle = Math.atan2(dz, dx);
        double seed = centre[0] * 0.0173D + centre[1] * 0.0311D;
        double wobble = 22.0D * Math.sin(3.0D * angle + seed)
                + 10.0D * Math.sin(7.0D * angle + seed * 2.0D)
                + 4.0D * Math.sin(11.0D * angle + seed * 3.0D)
                // block level jitter: a hash of the position, so the border is ragged instead of a
                // clean curve - real biome borders are never smooth
                + 7.0D * (((blockX * 73856093) ^ (blockZ * 19349663)) & 15) / 15.0D * 2.0D - 7.0D;
        return distance <= PATCH_RADIUS + wobble;
    }

    /** True inside the patch or its buffer ring. */
    public static boolean insideBuffer(int blockX, int blockZ) {
        int[] centre = patchCentre(blockX >> 4, blockZ >> 4);
        int dx = blockX - centre[0];
        int dz = blockZ - centre[1];
        double reach = PATCH_RADIUS + BUFFER_RADIUS + MAX_WOBBLE;
        return dx * dx + dz * dz <= reach * reach;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int[] centre = patchCentre(chunkX, chunkZ);
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        BlockState ice = FrostedLandContent.CONGEALED_ICE.get().defaultBlockState();
        int sandHeight = 0;
        int columns = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                sandHeight += level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, minX + x, minZ + z);
                columns++;
            }
        }
        int baseHeight = sandHeight / Math.max(1, columns);

        boolean placed = false;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int worldX = minX + x;
                int worldZ = minZ + z;
                // the biome is sampled per quart cell, so a whole chunk can be "congealed" while its
                // corners are not: only convert columns that really sit inside the blob, otherwise the
                // ice field would end in a chunk aligned straight line
                if (!insidePatch(worldX, worldZ)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ) - 1;
                boolean inCentre = Math.abs(worldX - centre[0]) <= CENTRE_CLEARING
                        && Math.abs(worldZ - centre[1]) <= CENTRE_CLEARING;

                // 1) the surface becomes congealed ice (two blocks deep). Anything that pokes out of the
                //    ground - exposed ore, dirt, snow, ice spikes from other features - is swallowed too,
                //    so no ore can ever sit on top of the ice.
                for (int dy = 2; dy >= -6; dy--) {
                    int y = top + dy;
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(ice.getBlock())) {
                        continue;
                    }
                    level.setBlock(pos, ice, 2);
                }

                // 2) the middle is levelled to the chunk average, up to six blocks
                if (inCentre) {
                    int target = baseHeight;
                    for (int y = Math.max(target, top); y > Math.min(target, top); y--) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (y > target) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        } else {
                            level.setBlock(pos, ice, 2);
                        }
                    }
                    level.setBlock(new BlockPos(worldX, target, worldZ), ice, 2);
                    placed = true;
                    continue;
                }

                // 3) ice spikes outside the clearing
                long hash = (long) worldX * 341873128712L + (long) worldZ * 132897987541L;
                if (Math.floorMod(hash, 26) != 0) {
                    continue;
                }
                int height = 3 + (int) Math.floorMod(hash >> 8, 8);
                boolean thick = Math.floorMod(hash >> 20, 5) == 0;
                int radius = thick ? 2 : 0;
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int reach = height - Math.abs(dx) - Math.abs(dz);
                        for (int y = 1; y <= reach; y++) {
                            level.setBlock(new BlockPos(worldX + dx, top + y, worldZ + dz), ice, 2);
                        }
                    }
                }
                placed = true;
            }
        }

        // the chunk holding the middle of the patch also builds the boss altar
        if (chunkX == (centre[0] >> 4) && chunkZ == (centre[1] >> 4)) {
            buildAltar(level, centre[0], baseHeight, centre[1], ice);
            placed = true;
        }
        return placed;
    }

    /**
     * 冰封祭坛: the frozen altar in the middle of the congealed land.
     *
     * <p>A round congealed ice platform, a step, eight ice pillars and a raised dais carrying the boss
     * spawner - the same marker the snow fortress uses, so the origin ape wakes up the same way here.</p>
     */
    private void buildAltar(WorldGenLevel level, int centreX, int groundY, int centreZ, BlockState ice) {
        BlockState blueIce = Blocks.BLUE_ICE.defaultBlockState();
        BlockState ember = com.myriadbloom.EmberContent.EMBER_ORE == null ? ice
                : com.myriadbloom.EmberContent.EMBER_ORE.get().defaultBlockState();
        int radius = 13;
        for (int dz = -radius - 1; dz <= radius + 1; dz++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + 1) {
                    continue;
                }
                int x = centreX + dx;
                int z = centreZ + dz;
                // step ring, one block lower than the platform
                int y = distance > radius - 0.5D ? groundY - 1 : groundY;
                level.setBlock(new BlockPos(x, y, z), distance > radius - 2.5D ? blueIce : ice, 2);
                // clear the air above the platform
                for (int clear = 1; clear <= 9; clear++) {
                    if (!level.getBlockState(new BlockPos(x, y + clear, z)).isAir()) {
                        level.setBlock(new BlockPos(x, y + clear, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // eight pillars around the platform
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            int px = centreX + (int) Math.round(Math.cos(angle) * 10.0D);
            int pz = centreZ + (int) Math.round(Math.sin(angle) * 10.0D);
            int height = 7 + (i % 2) * 2;
            for (int y = 1; y <= height; y++) {
                level.setBlock(new BlockPos(px, groundY + y, pz), ice, 2);
            }
            level.setBlock(new BlockPos(px, groundY + height + 1, pz), blueIce, 2);
            level.setBlock(new BlockPos(px, groundY + height + 2, pz), ember, 2);
        }
        // raised dais with the boss spawner on top
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                if (dx * dx + dz * dz > 9) {
                    continue;
                }
                level.setBlock(new BlockPos(centreX + dx, groundY + 1, centreZ + dz), ice, 2);
                level.setBlock(new BlockPos(centreX + dx, groundY + 2, centreZ + dz), blueIce, 2);
            }
        }
        // the congealed land is intentionally empty for now: no boss spawner here, only a frozen
        // altar core. The origin ape only ever appears from the snow fortress spawner.
        level.setBlock(new BlockPos(centreX, groundY + 3, centreZ), blueIce, 2);
        level.setBlock(new BlockPos(centreX, groundY + 4, centreZ), ice, 2);
        level.setBlock(new BlockPos(centreX, groundY + 5, centreZ), blueIce, 2);
    }
}
