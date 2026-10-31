package com.myriadbloom.world.gen;

import com.myriadbloom.FrostedLandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 巨树 (giant tree): a 3x3 trunk of giant wood with a broad leafy crown. */
public final class GiantTree {
    private static final int MIN_HEIGHT = 14;
    private static final int MAX_HEIGHT = 20;

    private GiantTree() {
    }

    /** Grows the tree at the sapling position, replacing the sapling. */
    public static void grow(ServerLevel level, BlockPos sapling, RandomSource random) {
        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        BlockState log = FrostedLandContent.GIANT_LOG.get().defaultBlockState();
        BlockState leaves = FrostedLandContent.GIANT_LEAVES.get().defaultBlockState();
        BlockPos base = sapling.below();
        // 树干 3x3
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    level.setBlock(base.offset(dx, y, dz), log, 3);
                }
            }
        }
        // 树冠：两层大叶团 + 顶部收窄
        int crownStart = height - 8;
        for (int y = crownStart; y < height + 4; y++) {
            int progress = y - crownStart;
            int radius = progress < 4 ? 6 : Math.max(2, 7 - progress);
            for (int dx = -radius; dx <= radius + 2; dx++) {
                for (int dz = -radius; dz <= radius + 2; dz++) {
                    double distance = Math.sqrt((dx - 1) * (dx - 1) + (dz - 1) * (dz - 1));
                    if (distance > radius + 0.5D) {
                        continue;
                    }
                    if (distance > radius - 1.0D && random.nextInt(3) != 0) {
                        continue;
                    }
                    BlockPos target = base.offset(dx, y, dz);
                    if (level.getBlockState(target).canBeReplaced()) {
                        level.setBlock(target, leaves, 3);
                    }
                }
            }
        }
        // 树根处清一圈雪
        for (int dx = -4; dx <= 5; dx++) {
            for (int dz = -4; dz <= 5; dz++) {
                BlockPos target = base.offset(dx, -1, dz);
                if (level.getBlockState(target).is(Blocks.SNOW)) {
                    level.setBlock(target, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
                }
            }
        }
    }
}
