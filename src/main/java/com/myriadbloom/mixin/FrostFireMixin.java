package com.myriadbloom.mixin;

import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wild fire in the frost realm needs fuel.
 *
 * <p>Fire that appears directly on the ground - snow, stone, dirt, ice - goes out immediately, while
 * fire lit on or next to a flammable block (wood, planks, coal blocks, ...) behaves like vanilla fire.
 * That keeps naturally spawned fire (lava lakes, lightning) from burning for ever on bare rock.</p>
 *
 * <p>The check runs both when the block is placed and when its first scheduled tick fires, so it also
 * covers fire that is placed through paths that do not call {@code onPlace}.</p>
 */
@Mixin(FireBlock.class)
public abstract class FrostFireMixin {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$requireFuelOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                               boolean movedByPiston, CallbackInfo ci) {
        if (level.isClientSide || !(level instanceof ServerLevel server) || !MyriadBloomApi.isMyriadBloom(server)) {
            return;
        }
        if (!hasFuel(level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$requireFuelOnTick(BlockState state, ServerLevel level, BlockPos pos,
                                              net.minecraft.util.RandomSource random, CallbackInfo ci) {
        if (!MyriadBloomApi.isMyriadBloom(level)) {
            return;
        }
        if (!hasFuel(level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    /** Fire survives when a neighbouring block can burn. */
    private static boolean hasFuel(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            BlockState neighbourState = level.getBlockState(neighbour);
            if (neighbourState.isFlammable(level, neighbour, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }
}
