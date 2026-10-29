package com.myriadbloom.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sugar frost: the candy dimension's white, sand-like ground material.
 *
 * <p>It is a {@link FallingBlock}, so it obeys gravity exactly like sand (the spec's "受影响于重力，
 * 可下落，可堆"). The floor feature places it as the realm's bottom layer, and because the bottom
 * bedrock row is always present there (see {@code CandyFloorFeature}) it does not sink when it is
 * generated.</p>
 */
public class SugarFrostBlock extends FallingBlock {
    /** Serialiser of the block (required by {@link FallingBlock}). */
    public static final MapCodec<SugarFrostBlock> CODEC = simpleCodec(SugarFrostBlock::new);

    /** Colour of the dust particles while the block falls. */
    private static final int DUST_COLOR = 0xF6EEF3;

    public SugarFrostBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return DUST_COLOR;
    }
}
