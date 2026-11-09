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
                renderer.addLayer(new SnowRabbitCarryLayer(renderer, event.getEntityModels()));
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
        event.registerEntityRenderer(FrostMobs.SNOW_RABBIT.get(), SnowRabbitRenderer::new);
        event.registerEntityRenderer(FrostMobs.STORM_PRISM.get(), StormPrismRenderer::new);
        // 风暴棱镜的冰锥：原来没有注册渲染器，生成时会因 EntityRenderer 为 null 而崩溃
        event.registerEntityRenderer(FrostMobs.STORM_ICE_SHARD.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(FrostMobs.SEALED_MAIDEN.get(), SealedMaidenRenderer::new);
        event.registerEntityRenderer(FrostMobs.MAGIC_CIRCLE.get(), MagicCircleRenderer::new);
        event.registerEntityRenderer(FrostMobs.TIME_KEEPER.get(), TimeKeeperRenderer::new);
        event.registerBlockEntityRenderer(com.myriadbloom.BossContent.TROPHY_ENTITY.get(),
                TrophyRenderer::new);
    }

    private FrostEntityRenderers() {
    }
}
