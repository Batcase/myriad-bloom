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
 * 许愿池 landmark: a small round platform in the middle of its biome with the wishing pool block at
 * the very centre, plus a few decorative ice pillars around it.
 *
 * <p>The biome itself is a tiny patch produced by {@code FrostBiomeSource}; this feature only runs in
 * the chunk that holds the patch centre, so exactly one pool is built per patch.</p>
 */
public class WishingPoolFeature extends Feature<NoneFeatureConfiguration> {
    /** One pool per this many blocks of world, and the patch is 6x6 chunks wide. */
    public static final int REGION_BLOCKS = 4096;
    public static final int PATCH_RADIUS = 48;
    private static final int MARGIN = 1024;

    public WishingPoolFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public WishingPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Centre of the wish patch that owns this chunk (very sparse: one per 2048 blocks). */
    public static int[] patchCentre(int chunkX, int chunkZ) {
        int regionChunks = REGION_BLOCKS >> 4;
        int regionX = Math.floorDiv(chunkX, regionChunks);
        int regionZ = Math.floorDiv(chunkZ, regionChunks);
        long hash = regionX * 668265263L + regionZ * 2147483647L + 0x1F1F1FL;
        int span = REGION_BLOCKS - 2 * MARGIN;
        int offsetX = MARGIN + (int) (Math.floorMod(hash, span) >> 1) * 2;
        int offsetZ = MARGIN + (int) (Math.floorMod(hash >> 20, span) >> 1) * 2;
        return new int[]{regionX * REGION_BLOCKS + offsetX, regionZ * REGION_BLOCKS + offsetZ};
    }

    /** True inside the (round) wish patch of the region that owns this block. */
    public static boolean insidePatch(int blockX, int blockZ) {
        int[] centre = patchCentre(blockX >> 4, blockZ >> 4);
        int dx = blockX - centre[0];
        int dz = blockZ - centre[1];
        return dx * dx + dz * dz <= PATCH_RADIUS * PATCH_RADIUS;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int[] centre = patchCentre(chunkX, chunkZ);
        if (chunkX != (centre[0] >> 4) || chunkZ != (centre[1] >> 4)) {
            return false;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre[0], centre[1]) - 1;
        ground = Math.max(level.getMinBuildHeight() + 4, Math.min(ground, level.getMaxBuildHeight() - 8));
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
        BlockState rock = com.myriadbloom.FrostedLandContent.FROST_MOSSY_COBBLESTONE.get().defaultBlockState();
        BlockState lamp = com.myriadbloom.FrostedLandContent.FROST_LAMP.get().defaultBlockState();
        int radius = 7;

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int x = centre[0] + dx;
                int z = centre[1] + dz;
                boolean rim = dx * dx + dz * dz > (radius - 2) * (radius - 2);
                BlockState floor = rim ? snow : (random.nextInt(8) == 0 ? lamp : rock);
                level.setBlock(new BlockPos(x, ground, z), floor, 2);
                level.setBlock(new BlockPos(x, ground - 1, z), ice, 2);
                for (int clear = 1; clear <= 4; clear++) {
                    BlockPos above = new BlockPos(x, ground + clear, z);
                    if (!level.getBlockState(above).isAir()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // four decorative pillars around the pool
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 2.0D * i + Math.PI / 4.0D;
            int px = centre[0] + (int) Math.round(Math.cos(angle) * 5.0D);
            int pz = centre[1] + (int) Math.round(Math.sin(angle) * 5.0D);
            int height = 3 + random.nextInt(2);
            for (int y = 1; y <= height; y++) {
                level.setBlock(new BlockPos(px, ground + y, pz), ice, 2);
            }
            level.setBlock(new BlockPos(px, ground + height + 1, pz), snow, 2);
        }
        // the well: a 3x3 diorite rim two blocks high around a 1x1 column of water that has
        // depth, so an offering really falls in and sinks before the wish is granted
        // the well is sunk into the platform: pool block at ground-3, water at ground-2 and ground-1,
        // so the water surface sits one block below the rim and offerings can fall in
        level.setBlock(new BlockPos(centre[0], ground - 3, centre[1]),
                FrostedLandContent.WISHING_POOL.get().defaultBlockState(), 2);
        for (int dy = -2; dy <= -1; dy++) {
            level.setBlock(new BlockPos(centre[0], ground + dy, centre[1]),
                    Blocks.WATER.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(centre[0], ground, centre[1]), Blocks.AIR.defaultBlockState(), 2);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    level.setBlock(new BlockPos(centre[0], ground - 1, centre[1]), ice, 2);
                    continue;
                }
                // the rim is flush with the platform: the water sits in a hole, not behind a wall
                BlockState wall = random.nextInt(8) == 0 ? lamp : rock;
                level.setBlock(new BlockPos(centre[0] + dx, ground, centre[1] + dz), wall, 2);
                level.setBlock(new BlockPos(centre[0] + dx, ground - 1, centre[1] + dz), rock, 2);
            }
        }
        return true;
    }
}
