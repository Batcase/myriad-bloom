package com.myriadbloom.world.block;

import com.myriadbloom.BossContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 迷宫锁孔：堵在风暴林地迷宫入口上方，用冰霜之钥才能打开。
 */
public class MazeLockBlock extends Block {
    public MazeLockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BossContent.FROST_KEY.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        stack.shrink(1);
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.2F, 1.4F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 40, 0.6D, 0.6D, 0.6D, 0.05D);
        }
        return ItemInteractionResult.SUCCESS;
    }
}
