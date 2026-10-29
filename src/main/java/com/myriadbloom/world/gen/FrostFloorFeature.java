package com.myriadbloom.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Carves the bottom of the realm: everything below {@link #VOID_TOP} becomes air (void), and the
 * four layers {@link #BEDROCK_MIN}..{@link #BEDROCK_MAX} become randomly generated bedrock, densest
 * at the bottom, so the lowest standable layer is {@link #BEDROCK_MAX}.
 *
 * <p>Runs as a {@code raw_generation} feature, i.e. after the noise terrain and its own bottom
 * bedrock have been placed, which is what lets it remove vanilla's deepslate/bedrock.</p>
 */
public class FrostFloorFeature extends Feature<NoneFeatureConfiguration> {
    /** Everything at or below this Y is void. */
    public static final int VOID_TOP = -34;
    /** Lowest bedrock layer. */
    public static final int BEDROCK_MIN = -33;
    /** Highest bedrock layer: the walkable floor. */
    public static final int BEDROCK_MAX = -30;

    public FrostFloorFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public FrostFloorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                for (int y = minY; y <= VOID_TOP; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }

                for (int y = BEDROCK_MIN; y <= BEDROCK_MAX; y++) {
                    cursor.set(x, y, z);
                    if (random.nextFloat() < bedrockChance(y)) {
                        level.setBlock(cursor, Blocks.BEDROCK.defaultBlockState(), 2);
                    } else if (!level.getBlockState(cursor).isAir()) {
                        // leave the terrain above the bedrock layers untouched
                        continue;
                    }
                }
            }
        }
        return true;
    }

    /** Density of the random bedrock band: solid floor, thinning towards the top. */
    public static float bedrockChance(int y) {
        return switch (y) {
            case BEDROCK_MIN -> 1.0F;
            case BEDROCK_MIN + 1 -> 0.7F;
            case BEDROCK_MIN + 2 -> 0.4F;
            default -> 0.15F;
        };
    }
}
