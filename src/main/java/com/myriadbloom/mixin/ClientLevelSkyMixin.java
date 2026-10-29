package com.myriadbloom.mixin;

import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.client.CandyClientColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the candy dimension its dark pink sky.
 *
 * <p>NeoForge has a viewport event for the fog colour but none for the sky colour: vanilla computes
 * it in {@link ClientLevel#getSkyColor(Vec3, float)} from the biome colour times the daylight
 * factor. Since the candy dimension is frozen at midnight that factor is zero, so the biome colour
 * can never show; the sky has to be overridden outright. Only this dimension is touched.</p>
 *
 * <p>This class targets a client-only class, so it is listed under {@code "client"} in
 * {@code myriadbloom.mixins.json} and never loads on a dedicated server.</p>
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void myriadbloom$candySkyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (MyriadBloomApi.isCandy(level)) {
            cir.setReturnValue(CandyClientColors.SKY);
        }
        if (com.myriadbloom.client.CongealedClientEvents.isInside(level, pos)) {
            // the congealed land has an almost black, suffocating sky
            cir.setReturnValue(new Vec3(0.03D, 0.04D, 0.08D));
        }
    }
}
