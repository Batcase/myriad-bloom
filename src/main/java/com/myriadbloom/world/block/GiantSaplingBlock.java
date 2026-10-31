package com.myriadbloom.world.block;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.world.gen.GiantTree;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 巨木树苗 (giant sapling): grows the giant tree.
 *
 * <p>Growth is driven directly (no TreeGrower), so the tree shape stays under our control: a 3x3 trunk
 * of {@code giant_log} with a wide crown of {@code giant_leaves}.</p>
 */
public class GiantSaplingBlock extends BushBlock {
    /** 0..2, so a few random ticks are needed before the tree appears. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);

    public static final MapCodec<GiantSaplingBlock> CODEC = simpleCodec(GiantSaplingBlock::new);

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    public GiantSaplingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) != 0) {
            return;
        }
        int stage = state.getValue(STAGE);
        if (stage < 2) {
            level.setBlock(pos, state.setValue(STAGE, stage + 1), 2);
        } else {
            GiantTree.grow(level, pos, random);
        }
    }
}
