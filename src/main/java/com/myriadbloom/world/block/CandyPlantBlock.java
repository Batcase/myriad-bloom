package com.myriadbloom.world.block;

import com.myriadbloom.CandyContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decorative candy plant (sweet grass or candy grass).
 *
 * <p>Cross shaped, one block tall, only placeable on a sweet grass block - and deliberately not food:
 * sheep cannot eat these, they only eat the sweet grass block itself.</p>
 */
public class CandyPlantBlock extends BushBlock {
    public static final MapCodec<CandyPlantBlock> CODEC = simpleCodec(CandyPlantBlock::new);

    public CandyPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(CandyContent.SWEET_GRASS_BLOCK.get());
    }
}
