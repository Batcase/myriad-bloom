package com.myriadbloom.world.block;

import com.myriadbloom.CandyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Legacy coloured jelly: one block with a {@code color} state.
 *
 * <p>Kept registered so worlds generated before the per-colour blocks existed still load. New builds
 * should use {@code myriadbloom:<colour>_jelly_block} instead - those are registered one block per
 * colour, which is what makes them obtainable from the creative inventory. Pick block on the legacy
 * block redirects to the matching per-colour block.</p>
 */
public class JellyBlock extends JellyPhysicsBlock {
    public JellyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CandyColors.COLOR, DyeColor.PINK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CandyColors.COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(CandyColors.COLOR, CandyColors.random(context.getLevel().getRandom()));
    }

    /** Pick block returns the per-colour block, not the legacy single item. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(CandyBlocks.JELLY.get(state.getValue(CandyColors.COLOR)).get());
    }
}
