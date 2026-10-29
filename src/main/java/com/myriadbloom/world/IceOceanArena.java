package com.myriadbloom.world;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the 冰洋 (ice ocean) boss arena: a round congealed ice platform ringed by sea water and drifting
 * ice floes, with the boss summoner standing on the central block.
 *
 * <p>The arena is <b>not</b> part of world generation. Probing biomes from a worldgen feature is unsafe
 * (it crashes with "Requested chunk unavailable during world generation"), so the arena is built by
 * {@code IceOceanArenaMechanic} the first time a player walks into an ice ocean patch. The patch itself
 * is still pure worldgen: {@link #insidePatch(int, int)} describes a 9x9 chunk cell.</p>
 */
public final class IceOceanArena {
    /** 9 chunks: the size of one ice ocean patch. */
    public static final int CELL = 9 * 16;
    /** Radius of the congealed ice platform. */
    public static final int PLATFORM_RADIUS = 26;
    /** Radius of the congealed ice floor locked while the boss lives. */
    public static final int SEAL_RADIUS = 40;

    private IceOceanArena() {
    }

    private static long hash(int cellX, int cellZ) {
        long h = cellX * 341873128712L + cellZ * 132897987541L + 0x1CE0CEAL;
        h ^= h >> 29;
        h *= 0x5DEECE66DL;
        h ^= h >> 32;
        return h;
    }

    /** True when this block lies inside a marked ice ocean patch. */
    public static boolean insidePatch(int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        if (Math.floorMod(hash(cellX, cellZ), 4) != 0L) {
            return false;
        }
        int centreX = cellX * CELL + CELL / 2;
        int centreZ = cellZ * CELL + CELL / 2;
        double dx = blockX - centreX;
        double dz = blockZ - centreZ;
        double angle = Math.atan2(dz, dx);
        // a round blob that fills most of the cell, wobbled so the edge is not a perfect circle
        double radius = 62.0D + 5.0D * Math.sin(6.0D * angle) + 3.0D * Math.sin(11.0D * angle);
        return Math.sqrt(dx * dx + dz * dz) <= radius;
    }

    /** Cell key used by {@link ArenaGuard} to remember which patches already have an arena. */
    public static long cellKey(int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        return ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    /** Centre of the patch that owns this block position. */
    public static int[] centreOf(int blockX, int blockZ) {
        int cellX = Math.floorDiv(blockX, CELL);
        int cellZ = Math.floorDiv(blockZ, CELL);
        return new int[]{cellX * CELL + CELL / 2, cellZ * CELL + CELL / 2};
    }

    /**
     * Builds the arena centred on a congealed ice platform. Idempotent: a patch is only ever built once.
     *
     * @return true when this call actually built the arena
     */
    public static boolean build(ServerLevel level, int centreX, int centreZ) {
        ArenaGuard guard = ArenaGuard.of(level);
        long key = cellKey(centreX, centreZ);
        if (guard.isBuilt(key)) {
            return false;
        }
        guard.markBuilt(key);

        RandomSource random = level.random;
        int surface = level.getSeaLevel();
        BlockState ice = FrostedLandContent.CONGEALED_ICE.get().defaultBlockState();
        BlockState packedIce = Blocks.PACKED_ICE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // ---- the round platform of congealed ice ---------------------------------------------
        for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
            for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
                if (dx * dx + dz * dz > PLATFORM_RADIUS * PLATFORM_RADIUS) {
                    continue;
                }
                int x = centreX + dx;
                int z = centreZ + dz;
                level.setBlock(cursor.set(x, surface, z), ice, 2);
                // the boss is 8 blocks tall and leaps another 3, so clear plenty of air above the slab
                for (int y = surface + 1; y <= surface + 20; y++) {
                    level.setBlock(cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        // ---- water ring with drifting floes ---------------------------------------------------
        int outer = PLATFORM_RADIUS + 34;
        for (int dz = -outer; dz <= outer; dz++) {
            for (int dx = -outer; dx <= outer; dx++) {
                int x = centreX + dx;
                int z = centreZ + dz;
                if (!insidePatch(x, z)) {
                    continue;
                }
                double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (distance <= PLATFORM_RADIUS) {
                    continue;
                }
                for (int y = surface - 3; y < surface; y++) {
                    level.setBlock(cursor.set(x, y, z), water, 2);
                }
                long floeHash = (long) x * 341873128712L + (long) z * 132897987541L;
                // sparse floes: roughly 1 in 7 blocks near the platform, 1 in 13 further out
                boolean floe = Math.floorMod(floeHash, distance < PLATFORM_RADIUS + 14 ? 7 : 13) == 0;
                if (floe) {
                    level.setBlock(cursor.set(x, surface, z), random.nextInt(4) == 0 ? ice : packedIce, 2);
                } else {
                    level.setBlock(cursor.set(x, surface, z), water, 2);
                    // rare raised ice, only outside the platform ring
                    if (Math.floorMod(floeHash >> 8, 90) == 0) {
                        for (int y = surface + 1; y <= surface + 2 + random.nextInt(2); y++) {
                            level.setBlock(cursor.set(x, y, z), packedIce, 2);
                        }
                    }
                }
            }
        }

        // ---- the summoner in the middle -------------------------------------------------------
        BlockPos summoner = new BlockPos(centreX, surface + 1, centreZ);
        level.setBlock(summoner.below(), ice, 2);
        level.setBlock(summoner, ModBlocks.BOSS_SUMMONER.get().defaultBlockState(), 2);
        return true;
    }
}
