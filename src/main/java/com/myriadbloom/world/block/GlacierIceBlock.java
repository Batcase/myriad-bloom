package com.myriadbloom.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glacier ice (极冰): the material of the snow fortress.
 *
 * <p>Behaves like vanilla blue ice - slippery ({@code friction 0.989}), glassy sound, translucent,
 * never melts from light or warm biomes - with two differences:</p>
 *
 * <ul>
 *   <li>it drops nothing unless mined with silk touch (see the block loot table);</li>
 *   <li>lava or a magma block within {@value #MELT_RADIUS} blocks eventually vaporises it. The melt is
 *       <b>delayed</b> like vanilla ice melting in bright light: the block schedules a tick
 *       {@value #MELT_DELAY_MIN}-{@value #MELT_DELAY_MAX} ticks later, and only if it is still hot by
 *       then does it vanish - with a puff of vapour. It leaves no water behind.</li>
 * </ul>
 */
public class GlacierIceBlock extends Block {
    /** Lava within this many blocks (Chebyshev distance) starts the melt. */
    private static final int MELT_RADIUS = 3;
    private static final int MELT_DELAY_MIN = 20;
    private static final int MELT_DELAY_MAX = 80;

    public GlacierIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            scheduleMeltIfHot(serverLevel, pos, this);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            scheduleMeltIfHot(serverLevel, pos, this);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // catches lava that flowed close without touching this block (no neighbour update then)
        scheduleMeltIfHot(level, pos, this);
    }

    /** Schedules the delayed melt; safe to call repeatedly, the pending tick is not duplicated. */
    private static void scheduleMeltIfHot(ServerLevel level, BlockPos pos, Block block) {
        if (level.getBlockTicks().hasScheduledTick(pos, block)) {
            return;
        }
        if (isHot(level, pos)) {
            int delay = MELT_DELAY_MIN + level.random.nextInt(MELT_DELAY_MAX - MELT_DELAY_MIN + 1);
            level.scheduleTick(pos, block, delay);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isHot(level, pos)) {
            // the lava was removed before the timer ran out: stay frozen
            return;
        }
        level.removeBlock(pos, false);
        vaporise(level, pos);
    }

    /** White vapour burst plus a hiss, once the block is gone. */
    private static void vaporise(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 18, 0.35D, 0.35D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.35D, z, 10, 0.3D, 0.5D, 0.3D, 0.01D);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.3F);
    }

    private static boolean isHot(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -MELT_RADIUS; dy <= MELT_RADIUS; dy++) {
            for (int dx = -MELT_RADIUS; dx <= MELT_RADIUS; dx++) {
                for (int dz = -MELT_RADIUS; dz <= MELT_RADIUS; dz++) {
                    cursor.setWithOffset(pos, dx, dy, dz);
                    if (level.getFluidState(cursor).is(FluidTags.LAVA)
                            || level.getBlockState(cursor).is(Blocks.MAGMA_BLOCK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
