package com.myriadbloom.world.block;

import com.myriadbloom.CandyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 棉花糖 (marshmallow): the candy sky's cloud blocks.
 *
 * <p>Wool-like (0.8 hardness, hand breakable, no tool), but <b>silent to walk on</b> and it melts the
 * moment water touches it: the block vanishes and turns into a candy item.</p>
 */
public class MarshmallowBlock extends Block {
    /**
     * Step sound is empty so walking on the clouds is silent; the other sounds stay cloth like.
     */
    public static final SoundType SILENT_STEP = new SoundType(
            1.0F, 1.0F,
            SoundEvents.WOOL_BREAK,
            SoundEvents.EMPTY,
            SoundEvents.WOOL_PLACE,
            SoundEvents.WOOL_HIT,
            SoundEvents.WOOL_FALL);

    public MarshmallowBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            checkWater(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            checkWater(level, pos);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkWater(level, pos);
    }

    /** Water anywhere next to the block melts it into candy. */
    private void checkWater(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            melt(level, pos);
            return;
        }
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                melt(level, pos);
                return;
            }
        }
    }

    private static void melt(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        if (level instanceof ServerLevel server) {
            Block.popResource(server, pos, new ItemStack(CandyContent.CANDY.get()));
            Vec3 centre = Vec3.atCenterOf(pos);
            server.playSound(null, centre.x, centre.y, centre.z, SoundEvents.WOOL_BREAK,
                    SoundSource.BLOCKS, 0.6F, 1.4F);
        }
    }
}
