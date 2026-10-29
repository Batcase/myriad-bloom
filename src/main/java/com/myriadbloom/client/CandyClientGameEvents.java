package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Game-bus client hooks for the candy dimension: the dark pink fog.
 *
 * <p>Distance fog is untouched (the candy dimension is a normal-horizon world, unlike the frost
 * realm's blizzard whiteout); only the colour is forced, so the horizon melts into the pink night
 * sky instead of vanilla's dark blue.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CandyClientGameEvents {
    private static boolean inCandy(ViewportEvent event) {
        Entity camera = event.getCamera().getEntity();
        return camera != null && camera.level() != null && MyriadBloomApi.isCandy(camera.level());
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inCandy(event)) {
            return;
        }
        event.setRed(CandyClientColors.SKY_RED);
        event.setGreen(CandyClientColors.SKY_GREEN);
        event.setBlue(CandyClientColors.SKY_BLUE);
    }

    private CandyClientGameEvents() {
    }
}
