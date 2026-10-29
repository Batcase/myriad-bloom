package com.myriadbloom.mixin;

import com.myriadbloom.world.FrostWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the Frost Realm its own permanent snowstorm without touching the overworld's weather.
 *
 * <p>Everything weather related in vanilla goes through these two getters:
 * {@code isRaining()} is {@code getRainLevel(1) > 0.2}, {@code isThundering()} is
 * {@code getThunderLevel(1) > 0.2}, and sky darkening, precipitation ticking and the client's
 * rain/snow renderer all read them as well. Patching them per dimension is therefore enough to make
 * the storm local: {@code /weather clear} cannot end it, and the overworld stays sunny.</p>
 */
@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$getRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (FrostWeather.isBlizzard((Level) (Object) this)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$rainAt(net.minecraft.core.BlockPos pos,
                                   CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (!com.myriadbloom.api.MyriadBloomApi.isMyriadBloom(level) || !level.canSeeSky(pos)) {
            return;
        }
        // the congealed land is always caught in the blizzard, whatever the level wide weather says
        if (level.getBiome(pos).is(com.myriadbloom.world.CongealedLandMechanic.BIOME)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$getThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (FrostWeather.isBlizzard((Level) (Object) this)) {
            cir.setReturnValue(1.0F);
        }
    }
}
