package com.myriadbloom.world.gen;

import com.myriadbloom.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Builds the bottom of the candy dimension: a random bedrock band at {@code Y -3..0} (same density
 * curve as the frost realm's {@code FrostFloorFeature}) and an undulating sugar-frost floor filling
 * the gaps above it up to a per-column height of at most {@code Y 6}.
 *
 * <p>Runs as a {@code raw_generation} feature, i.e. after the noise terrain and its own bedrock have
 * been generated, which is what lets it carve the vanilla layers away. The band {@code -4..6} is
 * cleared first: the sugar floor is what the player digs into, so it should not be buried inside the
 * mirrored overworld's stone.</p>
 *
 * <p>Like the conversion feature it writes into the chunk's sections directly, which keeps the
 * generation cost low: the heightmap only records the highest non-air block per column, and the band
 * this feature rewrites is far below the surface, so the heightmaps stay valid.</p>
 */
public class CandyFloorFeature extends Feature<NoneFeatureConfiguration> {
    /** Highest Y of the band this feature owns; everything at or below is resculpted. */
    public static final int VOID_TOP = -4;
    /** Lowest bedrock layer. */
    public static final int BEDROCK_MIN = -3;
    /** Highest bedrock layer of the random band. */
    public static final int BEDROCK_MAX = 0;
    /** The sugar-frost floor never rises above this Y. */
    public static final int SUGAR_MAX = 6;

    public CandyFloorFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public CandyFloorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
        int minY = chunk.getMinBuildHeight();
        LevelChunkSection[] sections = chunk.getSections();
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState sugar = ModBlocks.SUGAR_FROST.get().defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkMinX + localX;
                int worldZ = chunkMinZ + localZ;
                int target = sugarHeight(worldX, worldZ);
                for (int y = VOID_TOP; y <= SUGAR_MAX; y++) {
                    LevelChunkSection section = sections[(y - minY) >> 4];
                    int localY = y & 15;
                    BlockState state = air;
                    if (y >= BEDROCK_MIN && y <= BEDROCK_MAX && random.nextFloat() < bedrockChance(y)) {
                        state = bedrock;
                    } else if (y >= BEDROCK_MIN && y <= target) {
                        state = sugar;
                    }
                    if (!section.getBlockState(localX, localY, localZ).equals(state)) {
                        section.setBlockState(localX, localY, localZ, state, false);
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

    /**
     * Height of the sugar-frost floor for a column: a slow two-wave ripple inside
     * {@code BEDROCK_MAX..SUGAR_MAX}, so the floor undulates instead of being a flat slab.
     */
    public static int sugarHeight(int x, int z) {
        double wave = Math.sin(x * 0.11D) * Math.cos(z * 0.09D) * 2.0D
                + Math.sin((x + z) * 0.05D) * 1.4D;
        return Mth.clamp((int) Math.round(3.0D + wave), BEDROCK_MAX, SUGAR_MAX);
    }
}
