package com.myriadbloom.world.gen;

import com.myriadbloom.BurnedLandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 燃木树 (burnwood tree): a bare charred trunk with at most one short branch.
 *
 * <p>The branch always starts right next to the trunk, so it never floats. Fire is not guaranteed:
 * when it appears it sits on top of the trunk or on the branch tip, i.e. always supported by wood.</p>
 */
public class BurnTreeFeature extends Feature<NoneFeatureConfiguration> {
    public BurnTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        // 灰烬石、黑石、玄武岩上都能长；只要是实心的、不是岩浆就行
        var ground = level.getBlockState(base.below());
        if (ground.isAir() || ground.is(net.minecraft.world.level.block.Blocks.LAVA)
                || ground.is(net.minecraft.world.level.block.Blocks.FIRE)) {
            return false;
        }
        if (!level.getBlockState(base).isAir()) {
            return false;
        }
        BlockState log = BurnedLandContent.BURNWOOD_LOG.get().defaultBlockState();
        int height = 4 + random.nextInt(4);
        for (int y = 0; y < height; y++) {
            level.setBlock(base.above(y), log, 3);
        }

        // 至多一根分支：紧贴树干，向外延伸 1~2 格
        BlockPos branchTip = null;
        if (random.nextFloat() < 0.55F) {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int branchY = 1 + random.nextInt(Math.max(1, height - 2));
            int length = 1 + random.nextInt(2);
            BlockPos cursor = base.above(branchY).relative(direction);
            for (int i = 0; i < length; i++) {
                if (level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, log, 3);
                    branchTip = cursor;
                }
                cursor = cursor.relative(direction);
            }
        }

        // 火焰：随机出现，且一定落在木头上
        if (random.nextFloat() < 0.7F) {
            BlockPos top = base.above(height);
            if (level.getBlockState(top).isAir()) {
                level.setBlock(top, fireState(level, top, random), 3);
            }
        } else if (branchTip != null && random.nextFloat() < 0.5F) {
            BlockPos above = branchTip.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlock(above, fireState(level, above, random), 3);
            }
        }

        // 偶尔有倒下的树干
        if (random.nextInt(5) == 0) {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 2 + random.nextInt(3);
            for (int i = 1; i <= length; i++) {
                BlockPos fallen = base.relative(direction, i);
                if (level.getBlockState(fallen).isAir()
                        && level.getBlockState(fallen.below()).is(BurnedLandContent.ASH_STONE.get())) {
                    level.setBlock(fallen, log, 3);
                }
            }
        }
        return true;
    }

    private static BlockState fireState(WorldGenLevel level, BlockPos pos, RandomSource random) {
        return Blocks.FIRE.defaultBlockState();
    }
}
