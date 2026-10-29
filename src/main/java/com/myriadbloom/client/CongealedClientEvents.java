package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.CongealedLandMechanic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client side mood of the 凝结之地: a dark, oppressive sky and much shorter view distance.
 *
 * <p>The sky colour itself is overridden in {@code ClientLevelSkyMixin}; this class handles the fog
 * colour and also exposes the "inside the congealed land" test for the HUD overlay.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CongealedClientEvents {
    public static final float FOG_RED = 0.06F;
    public static final float FOG_GREEN = 0.08F;
    public static final float FOG_BLUE = 0.14F;

    /** True when the camera is standing in the congealed land biome. */
    public static boolean isInside(ClientLevel level, Vec3 pos) {
        if (level == null || pos == null) {
            return false;
        }
        return level.getBiome(BlockPos.containing(pos)).is(CongealedLandMechanic.BIOME);
    }

    public static boolean isCameraInside() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        return camera != null && isInside(minecraft.level, camera.position());
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Entity camera = event.getCamera().getEntity();
        if (camera == null || !MyriadBloomApi.isMyriadBloom(camera.level())) {
            return;
        }
        if (!isInside((ClientLevel) camera.level(), event.getCamera().getPosition())) {
            return;
        }
        event.setRed(FOG_RED);
        event.setGreen(FOG_GREEN);
        event.setBlue(FOG_BLUE);
    }

    /** Blizzard visibility: pull the fog much closer while standing in the congealed land. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Entity camera = event.getCamera().getEntity();
        if (camera == null || !MyriadBloomApi.isMyriadBloom(camera.level())) {
            return;
        }
        if (!isInside((ClientLevel) camera.level(), event.getCamera().getPosition())) {
            return;
        }
        event.setNearPlaneDistance(8.0F);
        event.setFarPlaneDistance(96.0F);
        event.setCanceled(true);
    }

    private CongealedClientEvents() {
    }
}
