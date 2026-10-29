package com.myriadbloom.world.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The single block-state property shared by {@link CandyBlock} and {@link JellyBlock}: one of the
 * sixteen dye colours.
 *
 * <p>Vanilla ships sixteen separate blocks per coloured family (16 wool blocks, 16 stained glass
 * blocks, ...). A candy world needs all sixteen colours of two families but none of the extra block
 * types, so here the colour is a block state instead: {@code myriadbloom:candy_block[color=red]}.
 * That keeps the registries small and lets the worldgen pick a colour per block without a lookup
 * table of sixteen blocks.</p>
 */
public final class CandyColors {
    /** {@code color=white} ... {@code color=black}; {@link DyeColor} serialises to the vanilla names. */
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    /** All sixteen values, in vanilla's dye order (white first, black last). */
    public static final DyeColor[] VALUES = DyeColor.values();

    /** Picks a colour from the world's random source (used when a block is placed by hand). */
    public static DyeColor random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    private CandyColors() {
    }
}
