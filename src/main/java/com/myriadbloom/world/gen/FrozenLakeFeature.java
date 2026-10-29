package com.myriadbloom.world.gen;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.world.LakePopulator;
import com.myriadbloom.world.RegionLedger;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A frozen lake of the 结霜之地: a shallow basin dug <b>into</b> the ground, filled with water, thin ice
 * on the surface but open water in the middle, seagrass on the bottom and a couple of cod in the water.
 *
 * <p>Every column is carved relative to its own ground height, so the ice always replaces the top block
 * (grass, snow, dirt) instead of being laid on top of it. Columns whose ground is more than
 * {@value #MAX_RIM_STEP} blocks away from the centre are skipped, which keeps the shore tidy on slopes.</p>
 */
public class FrozenLakeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_RADIUS = 5;
    private static final int MAX_RADIUS = 8;
    private static final int MIN_DEPTH = 3;
    private static final int MAX_DEPTH = 4;
    /** Middle 3x3 stays unfrozen. */
    private static final int OPEN_WATER = 1;
    /** A column is only dug when its ground is this close to the centre column's ground. */
    private static final int MAX_RIM_STEP = 2;

    public FrozenLakeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public FrozenLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int centre = groundY(level, origin.getX(), origin.getZ(), origin.getY());
        if (centre == Integer.MIN_VALUE) {
            return false;
        }
        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int depth = MIN_DEPTH + random.nextInt(MAX_DEPTH - MIN_DEPTH + 1);
        int radiusSq = radius * radius;

        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState thinIce = FrostedLandContent.THIN_ICE.get().defaultBlockState();
        boolean placed = false;

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int ground = groundY(level, x, z, centre);
                if (ground == Integer.MIN_VALUE || Math.abs(ground - centre) > MAX_RIM_STEP) {
                    continue;
                }
                // dig the basin: sand floor, water above it, all inside the original ground
                for (int y = ground - depth; y < ground; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            y == ground - depth ? Blocks.SAND.defaultBlockState() : water, 2);
                }
                // the surface block itself becomes ice/water, so no grass stays under the lid
                boolean openMiddle = Math.abs(dx) <= OPEN_WATER && Math.abs(dz) <= OPEN_WATER;
                level.setBlock(new BlockPos(x, ground, z), openMiddle ? water : thinIce, 2);
                // and anything that still pokes out above the rim is cleared
                for (int y = ground + 1; y <= centre + 2; y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                if (random.nextInt(3) == 0) {
                    level.setBlock(new BlockPos(x, ground - depth + 1, z), Blocks.SEAGRASS.defaultBlockState(), 2);
                }
                placed = true;
            }
        }

        if (placed && level.getLevel() instanceof ServerLevel server) {
            RegionLedger.of(server).record("lake", origin);
            for (int i = 0; i < 1 + random.nextInt(3); i++) {
                int dx = random.nextInt(radius * 2 + 1) - radius;
                int dz = random.nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                // never spawn entities on a worldgen thread (chunk load deadlock); queue instead
                LakePopulator.queue(server,
                        new BlockPos(origin.getX() + dx, centre - 1, origin.getZ() + dz));
            }
        }
        return placed;
    }

    /** First solid block that has air above, searched around {@code aroundY}. */
    private static int groundY(WorldGenLevel level, int x, int z, int aroundY) {
        for (int y = aroundY + 3; y >= aroundY - 6; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
