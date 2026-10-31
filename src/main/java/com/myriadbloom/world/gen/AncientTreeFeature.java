package com.myriadbloom.world.gen;

import com.myriadbloom.StormWoodlandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 古树 (ancient tree): the tree of the storm woodland.
 *
 * <p>Trunk diameter is 1 to 3 blocks. A diameter of three leaves the middle column as air, so the
 * trunk is a hollow ring. Every log position has a small chance of being replaced by plain ice
 * instead of ancient wood, which gives the tree its frozen, half-petrified look. Branches grow out of
 * the upper half of the trunk and end in small needle clusters.</p>
 */
public class AncientTreeFeature extends Feature<NoneFeatureConfiguration> {
    /** Chance that a would be log block is ice instead. */
    private static final float ICE_CHANCE = 0.08F;

    public AncientTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        if (baseY <= level.getMinBuildHeight() + 2) {
            return false;
        }
        // trees only grow on solid ground
        BlockPos below = new BlockPos(origin.getX(), baseY - 1, origin.getZ());
        if (level.getBlockState(below).isAir()) {
            return false;
        }

        BlockState wood = StormWoodlandContent.ANCIENT_WOOD.get().defaultBlockState();
        BlockState leaves = StormWoodlandContent.ANCIENT_LEAVES.get().defaultBlockState();
        BlockState ice = Blocks.ICE.defaultBlockState();

        int diameter = 1 + random.nextInt(3);
        int height = 10 + random.nextInt(7);
        boolean hollow = diameter == 3;

        // ---------------------------------------------------------------- trunk
        for (int y = 0; y < height; y++) {
            for (int[] offset : footprint(diameter)) {
                if (hollow && offset[0] == 0 && offset[1] == 0) {
                    // hollow centre: keep it open all the way to the crown
                    level.setBlock(origin.offset(offset[0], y, offset[1]), Blocks.AIR.defaultBlockState(), 3);
                    continue;
                }
                BlockState state = random.nextFloat() < ICE_CHANCE ? ice : wood;
                level.setBlock(origin.offset(offset[0], y, offset[1]), state, 3);
            }
        }

        // root flare: a skirt of wood one block up around the base
        for (int[] offset : footprint(diameter)) {
            for (int[] spread : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (random.nextFloat() < 0.5F) {
                    level.setBlock(origin.offset(offset[0] + spread[0], 0, offset[1] + spread[1]),
                            random.nextFloat() < ICE_CHANCE ? ice : wood, 3);
                }
            }
        }

        // ------------------------------------------------------------- branches
        int branches = 3 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            int at = (int) (height * (0.45D + random.nextDouble() * 0.4D));
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int length = 3 + random.nextInt(4);
            int x = origin.getX();
            int y = baseY + at;
            int z = origin.getZ();
            for (int step = 0; step < length; step++) {
                x += (int) Math.round(Math.cos(angle));
                z += (int) Math.round(Math.sin(angle));
                if (step % 3 == 2) {
                    y += 1;
                }
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.getBlockState(pos).isAir()) {
                    continue;
                }
                level.setBlock(pos, random.nextFloat() < ICE_CHANCE ? ice : wood, 3);
            }
            // needle cluster at the branch tip
            leafBall(level, random, new BlockPos(x, y, z), 2 + random.nextInt(2), leaves, ice);
        }

        // ---------------------------------------------------------------- crown
        BlockPos crown = new BlockPos(origin.getX(), origin.getY() + height, origin.getZ());
        leafBall(level, random, crown, 3 + random.nextInt(2), leaves, ice);
        for (int[] offset : footprint(diameter)) {
            leafBall(level, random, crown.offset(offset[0], -1, offset[1]), 2, leaves, ice);
        }
        return true;
    }

    /** Trunk footprint of a given diameter, relative to the origin block. */
    private static int[][] footprint(int diameter) {
        return switch (diameter) {
            case 2 -> new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};
            case 3 -> new int[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {0, 0}, {1, 0},
                    {-1, 1}, {0, 1}, {1, 1}};
            default -> new int[][]{{0, 0}};
        };
    }

    /** A ragged ball of leaves with the odd ice block frozen inside it. */
    private static void leafBall(WorldGenLevel level, RandomSource random, BlockPos centre, int radius,
                                 BlockState leaves, BlockState ice) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    double distance = dx * dx + dy * dy * 1.35D + dz * dz;
                    if (distance > radius * radius + 0.6D) {
                        continue;
                    }
                    // ragged edge
                    if (distance > (radius - 1) * (radius - 1) && random.nextFloat() < 0.35F) {
                        continue;
                    }
                    BlockPos pos = centre.offset(dx, dy, dz);
                    BlockState existing = level.getBlockState(pos);
                    if (!existing.isAir()) {
                        continue;
                    }
                    BlockState state = random.nextFloat() < ICE_CHANCE ? ice : leaves;
                    level.setBlock(pos, state, 3);
                }
            }
        }
    }
}
