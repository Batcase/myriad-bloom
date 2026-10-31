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
    /** 碎冰的最小/最大半径（格）。 */
    private static final int FLOW_ICE_MIN_RADIUS = 1;
    private static final int FLOW_ICE_MAX_RADIUS = 4;
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
                // 碎冰按 6x6 的"冰排"生成：每片有自己的大小、偏移和边缘缺口，
                // 所以近处有大块、远处有碎渣，不会出现整齐的格点感
                int cellX = Math.floorDiv(x, 6);
                int cellZ = Math.floorDiv(z, 6);
                long cellHash = (long) cellX * 341873128712L + (long) cellZ * 132897987541L;
                int localX = Math.floorMod(x, 6);
                int localZ = Math.floorMod(z, 6);
                int offsetX = Math.floorMod((int) (cellHash >> 8), 6);
                int offsetZ = Math.floorMod((int) (cellHash >> 16), 6);
                int radius = FLOW_ICE_MIN_RADIUS
                        + Math.floorMod((int) (cellHash >> 24), FLOW_ICE_MAX_RADIUS - FLOW_ICE_MIN_RADIUS + 1);
                boolean hasFloe = Math.floorMod(cellHash, 100) < (distance < PLATFORM_RADIUS + 14 ? 62 : 34);
                int dxLocal = Math.floorMod(localX - offsetX + 6, 6) - 2;
                int dzLocal = Math.floorMod(localZ - offsetZ + 6, 6) - 2;
                double floeDistance = Math.sqrt(dxLocal * dxLocal + dzLocal * dzLocal);
                // 边缘啃缺：hash 决定的少数方块被切掉，让轮廓不规则
                boolean bitten = Math.floorMod((int) (cellHash >> 32) + localX * 31 + localZ * 17, 11) == 0;
                boolean floe = hasFloe && floeDistance <= radius && !bitten;
                if (floe) {
                    level.setBlock(cursor.set(x, surface, z), random.nextInt(5) == 0 ? ice : packedIce, 2);
                } else {
                    level.setBlock(cursor.set(x, surface, z), water, 2);
                    // 少量拱起的冰块，位置和高度都随机
                    if (Math.floorMod((int) (cellHash >> 40) + localX * 7 + localZ * 13, 97) == 0) {
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
