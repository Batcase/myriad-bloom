package com.myriadbloom.mixin;

import com.myriadbloom.world.event.StasisZone;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Skips block entity ticking inside a 时间凝滞 sphere, so furnaces, hoppers, spawners and the like stop
 * while time is frozen.
 */
@Mixin(Level.class)
public abstract class LevelBlockEntityStasisMixin {
    @Redirect(method = "tickBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void myriadbloom$skipFrozenBlockEntity(TickingBlockEntity ticker) {
        Level level = (Level) (Object) this;
        if (StasisZone.isActive(level) && StasisZone.isFrozen(level, ticker.getPos())) {
            return;
        }
        ticker.tick();
    }
}
