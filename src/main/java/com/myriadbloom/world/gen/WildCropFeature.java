package com.myriadbloom.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A patch of wild crops in the 结霜之地: a small tilled field with beetroot, carrots, wheat or
 * potatoes growing on it.
 *
 * <p>Vanilla has no "wild crop" feature (the crop patches are placed by village farms), so this
 * feature builds the farmland and the crops itself. Only grass and dirt columns with air above are
 * converted, the patch is a rough circle of radius 1-3.</p>
 */
public class WildCropFeature extends Feature<NoneFeatureConfiguration> {
    private static final Block[] CROPS = {Blocks.BEETROOTS, Blocks.CARROTS, Blocks.WHEAT, Blocks.POTATOES};
    private static final int MAX_RADIUS = 3;

    public WildCropFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public WildCropFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int surface = findGround(level, origin);
        if (surface == Integer.MIN_VALUE) {
            return false;
        }

        Block cropBlock = CROPS[random.nextInt(CROPS.length)];
        int radius = 1 + random.nextInt(MAX_RADIUS);
        int radiusSq = radius * radius;
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7);
        boolean placed = false;

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                BlockPos ground = new BlockPos(origin.getX() + dx, surface, origin.getZ() + dz);
                BlockState state = level.getBlockState(ground);
                if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.DIRT)) {
                    continue;
                }
                if (!level.getBlockState(ground.above()).isAir()) {
                    continue;
                }
                level.setBlock(ground, farmland, 2);
                BlockState crop = cropBlock.defaultBlockState();
                if (crop.hasProperty(CropBlock.AGE)) {
                    crop = crop.setValue(CropBlock.AGE, random.nextInt(4) + 4); // mostly grown
                }
                level.setBlock(ground.above(), crop, 2);
                placed = true;
            }
        }
        return placed;
    }

    /** Topmost grass/dirt block below the origin, or {@link Integer#MIN_VALUE}. */
    private static int findGround(WorldGenLevel level, BlockPos origin) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos probe = origin.offset(0, dy, 0);
            BlockState state = level.getBlockState(probe);
            if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))
                    && level.getBlockState(probe.above()).isAir()) {
                return probe.getY();
            }
        }
        return Integer.MIN_VALUE;
    }
}
