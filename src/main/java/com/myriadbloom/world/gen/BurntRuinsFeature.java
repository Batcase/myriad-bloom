package com.myriadbloom.world.gen;

import com.myriadbloom.BurnedLandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 焦黑废墟 (burnt ruins): fragments of collapsed walls built from ash bricks.
 *
 * <p>Only the outline survives, at a random height per column, with gaps and broken corners - enough
 * to read as "a building used to stand here".</p>
 */
public class BurntRuinsFeature extends Feature<NoneFeatureConfiguration> {
    public BurntRuinsFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState bricks = BurnedLandContent.ASH_BRICKS.get().defaultBlockState();
        BlockState cracked = BurnedLandContent.CRACKED_ASH_BRICKS.get().defaultBlockState();

        int width = 5 + random.nextInt(7);
        int depth = 5 + random.nextInt(7);
        int baseY = origin.getY();
        if (!level.getBlockState(origin.below()).is(BurnedLandContent.ASH_STONE.get())) {
            return false;
        }
        boolean placed = false;
        // 四面墙的残骸
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean edge = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                if (!edge) {
                    continue;
                }
                if (random.nextFloat() < 0.35F) {
                    continue;   // 缺口
                }
                int height = 1 + random.nextInt(4);
                for (int y = 0; y < height; y++) {
                    BlockPos pos = new BlockPos(origin.getX() + x, baseY + y, origin.getZ() + z);
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        continue;
                    }
                    level.setBlock(pos, random.nextFloat() < 0.3F ? cracked : bricks, 3);
                    placed = true;
                }
            }
        }
        // 角落的柱子
        if (placed) {
            for (int corner = 0; corner < 4; corner++) {
                int x = corner % 2 == 0 ? 0 : width - 1;
                int z = corner < 2 ? 0 : depth - 1;
                int height = 2 + random.nextInt(4);
                for (int y = 0; y < height; y++) {
                    BlockPos pos = new BlockPos(origin.getX() + x, baseY + y, origin.getZ() + z);
                    if (level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, bricks, 3);
                    }
                }
            }
            // 地面上的碎石
            for (int i = 0; i < 6; i++) {
                BlockPos pos = origin.offset(random.nextInt(width), 0, random.nextInt(depth));
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).is(BurnedLandContent.ASH_STONE.get())) {
                    level.setBlock(pos, random.nextBoolean() ? bricks : cracked, 3);
                }
            }
        }
        return placed;
    }
}
