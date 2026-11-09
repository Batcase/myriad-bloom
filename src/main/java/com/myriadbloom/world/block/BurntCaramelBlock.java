package com.myriadbloom.world.block;

import com.myriadbloom.world.BurnedTeleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 焦糖传送门 (caramel portal): a burnt caramel block that sends the player to the burned land.
 *
 * <p>Right click to travel. The destination is placed on the surface at the same horizontal
 * coordinates, with a small search for a spot that is not inside lava.</p>
 */
public class BurntCaramelBlock extends Block {
    public BurntCaramelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                              BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ServerLevel target = serverPlayer.serverLevel().getServer().getLevel(BurnedTeleport.BURNED);
        if (target == null) {
            return InteractionResult.FAIL;
        }
        BurnedTeleport.send(serverPlayer, target);
        level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }
}
