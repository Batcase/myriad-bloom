package com.myriadbloom.world.block;

import com.myriadbloom.world.FrostPortalFrame;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The unbreakable frame of a lit frost portal.
 *
 * <p>It is only "sealed" while the portal is actually running: as soon as the 2x2 portal surface is
 * gone (mined, blown up, removed by a command, or the frame itself was never completed), every frost
 * rock block turns back into a snow block so the structure can be cleaned up instead of leaving an
 * indestructible scar on the landscape.</p>
 */
public class FrostRockBlock extends Block {
    public FrostRockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        checkFrame(level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkFrame(level, pos);
    }

    private static void checkFrame(Level level, BlockPos pos) {
        if (level.isClientSide || FrostPortalFrame.isEditing()) {
            return;
        }
        if (!FrostPortalFrame.anyLitFrameAround(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.SNOW_BLOCK.defaultBlockState());
        }
    }
}
