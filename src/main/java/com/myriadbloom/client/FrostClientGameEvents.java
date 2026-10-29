package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Game-bus client hooks: the realm's blizzard look.
 *
 * <p>Visible range is cut to 70% ({@link #VISIBLE_RANGE_SCALE}) and the fog is pushed towards a cold
 * pale blue, which reads as a whiteout. This is purely visual and does not change what the server
 * sends, so it is safe on a dedicated server.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class FrostClientGameEvents {
    private static final float VISIBLE_RANGE_SCALE = 0.7F;
    private static final float BLIZZARD_RED = 0.72F;
    private static final float BLIZZARD_GREEN = 0.80F;
    private static final float BLIZZARD_BLUE = 0.90F;

    private static boolean inRealm(ViewportEvent event) {
        Entity camera = event.getCamera().getEntity();
        return camera != null && camera.level() != null
                && camera.level().dimension().equals(MyriadBloomApi.FROST_REALM);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!inRealm(event)) {
            return;
        }
        event.setFarPlaneDistance(event.getFarPlaneDistance() * VISIBLE_RANGE_SCALE);
        event.setNearPlaneDistance(event.getNearPlaneDistance() * VISIBLE_RANGE_SCALE);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inRealm(event)) {
            return;
        }
        event.setRed(BLIZZARD_RED);
        event.setGreen(BLIZZARD_GREEN);
        event.setBlue(BLIZZARD_BLUE);
    }

    private FrostClientGameEvents() {
    }
}
