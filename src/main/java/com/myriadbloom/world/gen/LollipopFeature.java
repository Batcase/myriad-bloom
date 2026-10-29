package com.myriadbloom.world.gen;

import com.myriadbloom.CandyBlocks;
import com.myriadbloom.CandyContent;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.world.block.CandyColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Giant lollipops for the candy dimension: a candy corn stick topped with a two-tone candy head.
 *
 * <p>Five shapes are picked at random - a checkered ball, a target disc, a double ball, a wide flat
 * plate and a striped candy cane - and a head never uses the same colour twice, matching the spec's
 * "each candy uses only two colours". Placement is rare (a {@code rarity_filter} of 1 in 7 chunks),
 * so they read like trees on the hills instead of littering the landscape.</p>
 */
public class LollipopFeature extends Feature<NoneFeatureConfiguration> {
    private static final DyeColor[] PALETTE = {
            DyeColor.PINK, DyeColor.WHITE, DyeColor.LIGHT_BLUE, DyeColor.YELLOW,
            DyeColor.RED, DyeColor.LIME, DyeColor.PURPLE, DyeColor.ORANGE,
    };

    private static final int SHAPE_CANE = 4;

    public LollipopFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        DyeColor base = PALETTE[random.nextInt(PALETTE.length)];
        DyeColor accent = PALETTE[random.nextInt(PALETTE.length)];
        if (accent == base) {
            accent = base == DyeColor.WHITE ? DyeColor.PINK : DyeColor.WHITE;
        }

        int shape = random.nextInt(5);
        int height = 4 + random.nextInt(5);

        if (shape == SHAPE_CANE) {
            candyCane(level, origin, base, accent, height + 4);
            return true;
        }

        BlockState stick = CandyContent.CANDY_CORN_BLOCK.get().defaultBlockState();
        for (int i = 0; i < height; i++) {
            level.setBlock(origin.above(i), stick, 2);
        }
        BlockPos head = origin.above(height);

        switch (shape) {
            case 0 -> sphere(level, head, base, accent, 2 + random.nextInt(2), true);
            case 1 -> disc(level, head, base, accent, 3);
            case 2 -> {
                sphere(level, head, base, accent, 2, false);
                sphere(level, head.above(3), accent, base, 1, false);
            }
            default -> flatPlate(level, head, base, accent, 4 + random.nextInt(2));
        }
        return true;
    }

    /** Checkered ball; when {@code striped} the colours alternate by horizontal band. */
    private static void sphere(WorldGenLevel level, BlockPos centre, DyeColor outer, DyeColor inner,
                               int radius, boolean striped) {
        int radiusSq = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    boolean second = striped
                            ? Math.floorMod(dy, 2) == 0
                            : (dx + dy + dz) % 2 == 0;
                    level.setBlock(centre.offset(dx, dy, dz), candy(second ? inner : outer), 2);
                }
            }
        }
    }

    /** Flat target: a wide disc with a smaller disc of the second colour on top of it. */
    private static void disc(WorldGenLevel level, BlockPos centre, DyeColor outer, DyeColor inner, int radius) {
        for (int dy = 0; dy <= 1; dy++) {
            int r = dy == 0 ? radius : radius - 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    level.setBlock(centre.offset(dx, dy, dz), candy(outer), 2);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(centre.offset(dx, 1, dz), candy(inner), 2);
            }
        }
    }

    /**
     * Wide, flat plate: the "smashed lollipop" look. Two blocks thick with a big radius, an outer
     * ring in the base colour, the inner field in the accent colour and a small base-coloured core.
     */
    private static void flatPlate(WorldGenLevel level, BlockPos centre, DyeColor base, DyeColor accent,
                                  int radius) {
        int radiusSq = radius * radius;
        int ringSq = (radius - 1) * (radius - 1);
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq > radiusSq) {
                        continue;
                    }
                    DyeColor colour;
                    if (distSq > ringSq) {
                        colour = base;
                    } else if (distSq <= 1) {
                        colour = base;
                    } else {
                        colour = dy == 0 ? accent : base;
                    }
                    level.setBlock(centre.offset(dx, dy, dz), candy(colour), 2);
                }
            }
        }
    }

    /** Candy cane: a striped shaft with the top bent into a hook. */
    private static void candyCane(WorldGenLevel level, BlockPos origin, DyeColor base, DyeColor accent,
                                  int height) {
        for (int i = 0; i < height; i++) {
            level.setBlock(origin.above(i), candy(i / 2 % 2 == 0 ? base : accent), 2);
        }
        BlockPos top = origin.above(height);
        for (int dx = 1; dx <= 3; dx++) {
            level.setBlock(top.offset(dx, 0, 0), candy(dx / 2 % 2 == 0 ? accent : base), 2);
        }
        for (int dy = -1; dy >= -2; dy--) {
            level.setBlock(top.offset(3, dy, 0), candy(dy == -1 ? accent : base), 2);
        }
    }

    private static BlockState candy(DyeColor colour) {
        return CandyBlocks.candy(colour);
    }
}
