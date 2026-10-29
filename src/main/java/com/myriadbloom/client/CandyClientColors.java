package com.myriadbloom.client;

import net.minecraft.world.phys.Vec3;

/**
 * The candy dimension's palette, shared by the sky mixin and the fog hook so both always agree.
 *
 * <p>The dimension's clock is frozen at midnight ({@code fixed_time = 18000}) and vanilla multiplies
 * the biome sky colour by the daylight factor, which is zero at midnight - so the sky would be black
 * no matter what the biome says. These values are therefore applied directly: a dusky rose that is
 * clearly pink but dark enough to still read as night (and to keep the vanilla star field subtle
 * instead of turning every faint star into a visible tile).</p>
 */
public final class CandyClientColors {
    public static final float SKY_RED = 0.58F;
    public static final float SKY_GREEN = 0.30F;
    public static final float SKY_BLUE = 0.42F;

    /** Linear-space sky/fog colour. */
    public static final Vec3 SKY = new Vec3(SKY_RED, SKY_GREEN, SKY_BLUE);

    private CandyClientColors() {
    }
}
