package com.myriadbloom.mixin;

import com.myriadbloom.world.event.StasisZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops random block ticks inside a 时间凝滞 sphere: crops stop growing, snow stops forming, ice stops
 * melting and fluids stop spreading while time is frozen.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelStasisMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$skipFrozenChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo callback) {
        Level level = (Level) (Object) this;
        if (!StasisZone.isActive(level)) {
            return;
        }
        int centreX = chunk.getPos().getMinBlockX() + 8;
        int centreZ = chunk.getPos().getMinBlockZ() + 8;
        if (StasisZone.isFrozen(level, new net.minecraft.core.BlockPos(centreX, level.getSeaLevel(), centreZ))) {
            callback.cancel();
        }
    }
}
