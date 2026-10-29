package com.myriadbloom.world.gen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A marshmallow cloud in the candy dimension's sky.
 *
 * <p>Each cloud is a <b>single colour</b> and <b>entirely inside its own chunk</b>: it is placed near
 * the chunk middle and its radius is capped so it never crosses a chunk border. A feature may only
 * write into the chunk that is being generated; writing into a neighbour is dropped, which used to cut
 * clouds into hard 1-2 block tall walls along every chunk edge.</p>
 *
 * <p>Shapes and thickness vary per cloud: five silhouettes (round puff, puff with a lobe, two lobes,
 * elongated edge, small cluster) and a dome profile - the middle is up to five layers tall and the rim
 * drops to one. How the thickness is split between above and below the base line is random per cloud
 * (100%, 75%, 55% or 35% upwards), so some clouds are flat-bottomed domes while others bulge down and
 * puff out in both directions. Density stays low - one attempt per chunk with a 1/3 chance.</p>
 */
public class MarshmallowCloudFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_RADIUS = 3;
    private static final int MAX_RADIUS = 5;
    private static final int MAX_THICKNESS = 5;
    private static final int CHUNK_MARGIN = 2;
    /** Five silhouettes. */
    private static final int STYLE_COUNT = 5;

    public MarshmallowCloudFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public MarshmallowCloudFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int margin = radius + CHUNK_MARGIN;
        int centreX = minX + margin + random.nextInt(Math.max(1, 16 - 2 * margin));
        int centreZ = minZ + margin + random.nextInt(Math.max(1, 16 - 2 * margin));

        int style = random.nextInt(STYLE_COUNT);
        // how much of the thickness grows upwards: 1.0 is a classic flat-bottomed dome, low values
        // make the cloud bulge downwards as well
        float upRatio = switch (random.nextInt(4)) {
            case 0 -> 1.0F;
            case 1 -> 0.75F;
            case 2 -> 0.55F;
            default -> 0.35F;
        };
        boolean wispy = random.nextInt(4) == 0;                 // thin flat wisp
        int maxLayers = 2 + random.nextInt(MAX_THICKNESS - 1);  // 2..5, centre thickness
        int lobeX = (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(2));
        int lobeZ = random.nextInt(3) - 1;
        int lobeRadius = Math.max(2, radius - 1);
        int stretchX = 1 + random.nextInt(2);                    // elongated style stretch factors
        int stretchZ = 1 + random.nextInt(2);

        BlockState cloud = com.myriadbloom.MarshmallowContent.randomCloud(random);
        boolean placed = false;

        for (int dz = -radius - 1; dz <= radius + 1; dz++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                if (!inside(style, dx, dz, radius, lobeRadius, lobeX, lobeZ, stretchX, stretchZ)) {
                    continue;
                }
                int x = centreX + dx;
                int z = centreZ + dz;
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                    continue;
                }
                // dome profile: thick in the middle, thin at the rim, and the underside may sag
                double distance = Math.min(1.0D, Math.sqrt((double) (dx * dx + dz * dz)) / (radius + 0.5D));
                int layers = 1 + (int) Math.round((maxLayers - 1) * (1.0D - distance * distance));
                if (wispy) {
                    layers = Math.min(layers, 2);
                }
                int up = Math.max(1, Math.round(layers * upRatio));
                int down = layers - up;
                for (int layer = 0; layer < up; layer++) {
                    BlockPos pos = new BlockPos(x, origin.getY() + layer, z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, cloud, 2);
                        placed = true;
                    }
                }
                for (int layer = 1; layer <= down; layer++) {
                    BlockPos pos = new BlockPos(x, origin.getY() - layer, z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, cloud, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /** Silhouette test for the five cloud styles. */
    private static boolean inside(int style, int dx, int dz, int radius, int lobeRadius,
                                  int lobeX, int lobeZ, int stretchX, int stretchZ) {
        int disc = dx * dx + dz * dz;
        return switch (style) {
            case 0 -> disc <= radius * radius;                                      // round puff
            case 1 -> disc <= radius * radius                                       // puff + lobe
                    || (dx - lobeX) * (dx - lobeX) + (dz - lobeZ) * (dz - lobeZ) <= lobeRadius * lobeRadius;
            case 2 -> disc <= radius * radius                                       // two opposite lobes
                    || (dx - lobeX) * (dx - lobeX) + dz * dz <= (radius - 1) * (radius - 1)
                    || (dx + lobeX) * (dx + lobeX) + dz * dz <= (radius - 1) * (radius - 1);
            case 3 -> (dx * dx) / (double) (stretchX * stretchX)
                    + (dz * dz) / (double) (stretchZ * stretchZ) <= radius * radius; // stretched edge
            default -> disc <= (radius - 1) * (radius - 1)                           // small cluster
                    || (dx - lobeX) * (dx - lobeX) + (dz - lobeZ) * (dz - lobeZ) <= (radius - 1) * (radius - 1)
                    || (dx + lobeZ) * (dx + lobeZ) + (dz + lobeX) * (dz + lobeX) <= (radius - 2) * (radius - 2);
        };
    }
}
