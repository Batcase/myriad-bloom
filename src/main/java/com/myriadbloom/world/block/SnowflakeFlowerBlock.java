package com.myriadbloom.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 雪花 (snowflake flower): a decorative snowflake plant.
 *
 * <p>Unlike vanilla flowers it also grows on snow layers and snow blocks, on top of grass and dirt -
 * but not on powder snow, ice, packed ice or blue ice. It can be crafted into sky blue dye.</p>
 */
public class SnowflakeFlowerBlock extends BushBlock {
    public static final com.mojang.serialization.MapCodec<SnowflakeFlowerBlock> CODEC =
            simpleCodec(SnowflakeFlowerBlock::new);

    public SnowflakeFlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT);
    }
}
