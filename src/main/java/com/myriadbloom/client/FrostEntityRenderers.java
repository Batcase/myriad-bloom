package com.myriadbloom.client;

import com.myriadbloom.FrostMobs;
import com.myriadbloom.MyriadBloom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client renderers for the mod's entities. */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FrostEntityRenderers {

    /** Adds the trophy skull layer to both player models (classic and slim). */
    @net.neoforged.bus.api.SubscribeEvent
    public static void addLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new OriginApeTrophyLayer(renderer, event.getEntityModels()));
            }
        }
    }
    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        // the golem throws plain vanilla snowballs, so no projectile renderer is needed here
        event.registerEntityRenderer(FrostMobs.FRENZIED_SNOW_GOLEM.get(), FrenziedSnowGolemRenderer::new);
        event.registerEntityRenderer(FrostMobs.CARNIVOROUS_RABBIT.get(), CarnivorousRabbitRenderer::new);
        event.registerEntityRenderer(FrostMobs.CANDY_COW.get(), CandyCowRenderer::new);
        event.registerEntityRenderer(FrostMobs.FROST_BLAZE.get(), FrostBlazeRenderer::new);
        event.registerEntityRenderer(FrostMobs.FROST_BOLT.get(), FrostBoltRenderer::new);
        event.registerEntityRenderer(FrostMobs.ORIGIN_APE.get(), OriginApeRenderer::new);
        event.registerEntityRenderer(FrostMobs.SNOW_SPRITE.get(), SnowSpriteRenderer::new);
        event.registerEntityRenderer(FrostMobs.SNOW_BEAST.get(), SnowBeastRenderer::new);
        event.registerEntityRenderer(FrostMobs.GLACIAL_CONGLOMERATION.get(), GlacialConglomerationRenderer::new);
    }

    private FrostEntityRenderers() {
    }
}
