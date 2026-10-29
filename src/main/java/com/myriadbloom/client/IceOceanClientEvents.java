package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.FrostBiomeKeys;
import com.myriadbloom.world.entity.GlacialConglomeration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 冰洋 (ice ocean) atmosphere: while 极冰聚合物 still lives, the patch closes in around the player.
 *
 * <p>Visibility drops to a couple dozen blocks, the fog turns a cold glacier blue and slowly breathes,
 * and a dark vignette pulses at the screen edges. All of it is driven by one cached test - standing in
 * the ice ocean biome with a living boss nearby - so the per frame hooks stay allocation free.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class IceOceanClientEvents {
    /** How far away the boss is still felt, in blocks. Covers a whole patch from its edge. */
    private static final double BOSS_RANGE = 220.0D;
    private static final float FOG_RED = 0.56F;
    private static final float FOG_GREEN = 0.72F;
    private static final float FOG_BLUE = 0.82F;

    private static boolean fogActive;
    private static float pulse;

    private IceOceanClientEvents() {
    }

    /** True while the player stands in the ice ocean with an uncleared boss in the patch. */
    public static boolean isFogActive() {
        return fogActive;
    }

    public static float pulse() {
        return pulse;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        fogActive = compute(minecraft);
        // a slow breath, roughly one cycle every six seconds, so the fog feels alive
        pulse = (Mth.sin(minecraft.level == null ? 0.0F : minecraft.level.getGameTime() * 0.018F) + 1.0F) * 0.5F;
    }

    private static boolean compute(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        if (level == null || camera == null || !MyriadBloomApi.isMyriadBloom(level)) {
            return false;
        }
        if (!com.myriadbloom.FrostConfig.ICE_OCEAN_DREAD.get()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(camera.position());
        if (!level.getBiome(pos).is(FrostBiomeKeys.ICE_OCEAN)) {
            return false;
        }
        return !level.getEntitiesOfClass(GlacialConglomeration.class,
                camera.getBoundingBox().inflate(BOSS_RANGE)).isEmpty();
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!fogActive) {
            return;
        }
        // slightly darker on the exhale, which reads as pressure rather than plain fog
        float shade = 0.94F - 0.10F * pulse;
        event.setRed(FOG_RED * shade);
        event.setGreen(FOG_GREEN * shade);
        event.setBlue(FOG_BLUE * shade);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!fogActive) {
            return;
        }
        Entity camera = event.getCamera().getEntity();
        if (camera == null || !isInsideCamera(camera.level(), event.getCamera().getPosition())) {
            return;
        }
        // the breath also squeezes the view: 32 -> 22 blocks
        event.setNearPlaneDistance(3.0F + 3.0F * pulse);
        event.setFarPlaneDistance(32.0F - 10.0F * pulse);
        event.setCanceled(true);
    }

    private static boolean isInsideCamera(net.minecraft.world.level.Level level, Vec3 position) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return false;
        }
        return clientLevel.getBiome(BlockPos.containing(position)).is(FrostBiomeKeys.ICE_OCEAN);
    }
}
