package com.myriadbloom.world.gen;

import com.myriadbloom.FrostedLandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 风暴林地的螺旋入口: one of the four arms of the plus shaped forest hides a spiral shaft.
 *
 * <p>The position is derived from the same region hash as the cross itself, so each arm of each
 * region gets exactly one entrance at a fixed spot. The shaft is a helical stair inside a
 * congealed ice cylinder, ending in a round chamber - the doorway the underground maze will later
 * be attached to.</p>
 */
public class StormEntranceFeature extends Feature<NoneFeatureConfiguration> {
    /** Radius of the shaft. */
    private static final int SHAFT_RADIUS = 6;

    public StormEntranceFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int arm = StormCross.spiralArmAt(origin.getX(), origin.getZ());
        if (arm < 0) {
            // only the four chunks that really hold an entrance build anything
            return false;
        }
        int[] at = StormCross.spiralCentre(origin.getX(), origin.getZ(), arm);
        int centreX = at[0];
        int centreZ = at[1];
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centreX, centreZ);
        int bottom = Math.max(level.getMinBuildHeight() + 8, surface - StormCross.SPIRAL_DEPTH);
        BlockState wall = FrostedLandContent.CONGEALED_ICE.get().defaultBlockState();
        BlockState floor = Blocks.PACKED_ICE.defaultBlockState();
        BlockState step = Blocks.BLUE_ICE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // ------------------------------------------------------- shaft and walls
        for (int y = bottom - 1; y <= surface + 2; y++) {
            for (int dz = -SHAFT_RADIUS - 2; dz <= SHAFT_RADIUS + 2; dz++) {
                for (int dx = -SHAFT_RADIUS - 2; dx <= SHAFT_RADIUS + 2; dx++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    BlockPos pos = new BlockPos(centreX + dx, y, centreZ + dz);
                    if (distance <= SHAFT_RADIUS) {
                        // hollow shaft; the floor stays solid
                        level.setBlock(pos, y == bottom - 1 ? floor : air, 2);
                    } else if (distance <= SHAFT_RADIUS + 1.6D) {
                        level.setBlock(pos, wall, 2);
                    }
                }
            }
        }

        // --------------------------------------------------------- spiral stairs
        int steps = StormCross.SPIRAL_DEPTH * 4;
        for (int i = 0; i < steps; i++) {
            double angle = i * 0.24D;
            int y = surface - 1 - i / 4;
            int sx = centreX + (int) Math.round(Math.cos(angle) * (SHAFT_RADIUS - 1));
            int sz = centreZ + (int) Math.round(Math.sin(angle) * (SHAFT_RADIUS - 1));
            level.setBlock(new BlockPos(sx, y, sz), (i % 3 == 0) ? step : floor, 2);
            // headroom above the step
            level.setBlock(new BlockPos(sx, y + 1, sz), air, 2);
            level.setBlock(new BlockPos(sx, y + 2, sz), air, 2);
        }

        // ------------------------------------------------------- bottom chamber
        int chamberRadius = 8;
        for (int dz = -chamberRadius; dz <= chamberRadius; dz++) {
            for (int dx = -chamberRadius; dx <= chamberRadius; dx++) {
                for (int dy = 0; dy <= 5; dy++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > chamberRadius + 1) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(centreX + dx, bottom + dy, centreZ + dz);
                    if (dy == 0) {
                        level.setBlock(pos, floor, 2);
                    } else if (distance > chamberRadius - 1.2D) {
                        level.setBlock(pos, wall, 2);
                    } else {
                        level.setBlock(pos, air, 2);
                    }
                }
            }
        }

        // ---------------------------------------------------------- surface rim
        for (int dz = -SHAFT_RADIUS - 3; dz <= SHAFT_RADIUS + 3; dz++) {
            for (int dx = -SHAFT_RADIUS - 3; dx <= SHAFT_RADIUS + 3; dx++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        centreX + dx, centreZ + dz) - 1;
                BlockPos pos = new BlockPos(centreX + dx, y, centreZ + dz);
                if (distance > SHAFT_RADIUS + 1 && distance <= SHAFT_RADIUS + 3) {
                    level.setBlock(pos, wall, 2);
                }
            }
        }
        return true;
    }
}
