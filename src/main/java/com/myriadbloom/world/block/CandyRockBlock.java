package com.myriadbloom.world.block;

import com.myriadbloom.ModBlocks;
import com.myriadbloom.world.CandyPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The unbreakable frame of a lit candy portal.
 *
 * <p>Same contract as {@link FrostRockBlock}: it is only "sealed" while the portal surface is
 * actually there. As soon as no lit candy portal is left around it (mined away by a command, blown
 * up, or the frame was never completed), the block turns back into frost honey, which is breakable -
 * so a dead portal can always be taken apart instead of leaving an indestructible candy scar.</p>
 */
public class CandyRockBlock extends Block {
    public CandyRockBlock(Properties properties) {
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
        if (level.isClientSide || CandyPortalFrame.isEditing()) {
            return;
        }
        // Also keeps the landing platform of a floating arrival alive: it is part of the structure.
        if (!CandyPortalFrame.anyLitFrameNear(level, pos)) {
            level.setBlockAndUpdate(pos, ModBlocks.FROST_HONEY.get().defaultBlockState());
        }
    }
}
