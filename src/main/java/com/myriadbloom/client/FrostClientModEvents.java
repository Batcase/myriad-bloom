package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Mod-bus client hooks: HUD layer registration. */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FrostClientModEvents {

    /** Custom cube model of 极冰聚合物. */
    public static final net.minecraft.client.model.geom.ModelLayerLocation GLACIAL_CONGLOMERATION =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.myriadbloom.MyriadBloom.MODID, "glacial_conglomeration"), "main");

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, MyriadBloomApi.id("warmth"), new WarmthOverlay());
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, MyriadBloomApi.id("thirst"), new ThirstOverlay());
        event.registerAbove(MyriadBloomApi.id("warmth"), MyriadBloomApi.id("heat"), new HeatOverlay());
        event.registerAbove(VanillaGuiLayers.HOTBAR, MyriadBloomApi.id("congealed_frost"), new CongealedOverlay());
        event.registerAbove(VanillaGuiLayers.HOTBAR, MyriadBloomApi.id("ice_ocean_dread"), new IceOceanOverlay());
    }

    /** Model layer of the lollipops on a candy cow's back. */
    public static final ModelLayerLocation CANDY_COW_LOLLIPOPS = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "candy_cow_lollipops"), "main");

    /** Model layer of the ten block tall origin ape. */
    /** Model layer of 时间维护者. */
    public static final ModelLayerLocation TIME_KEEPER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "time_keeper"), "main");

    public static final ModelLayerLocation SEALED_MAIDEN = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "sealed_maiden"), "main");

    public static final ModelLayerLocation ORIGIN_APE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "origin_ape"), "main");

    @SubscribeEvent
    public static void registerDimensionEffects(
            net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(com.myriadbloom.api.MyriadBloomApi.id("burned"), new BurnedSkyEffects());
        event.register(com.myriadbloom.api.MyriadBloomApi.id("time_void"), new TimeVoidSkyEffects());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CANDY_COW_LOLLIPOPS, CandyCowLollipopLayer::createLayer);
        event.registerLayerDefinition(OriginApeTrophyLayer.LAYER, OriginApeTrophyLayer::createBodyLayer);
        event.registerLayerDefinition(SEALED_MAIDEN, SealedMaidenModel::createBodyLayer);
        event.registerLayerDefinition(TIME_KEEPER, TimeKeeperModel::createBodyLayer);
        event.registerLayerDefinition(GlacialTrophyLayer.LAYER, GlacialTrophyLayer::createBodyLayer);
        event.registerLayerDefinition(GLACIAL_CONGLOMERATION, GlacialConglomerationModel::createBodyLayer);
        event.registerLayerDefinition(ORIGIN_APE, OriginApeModel::createBodyLayer);
    }

    private FrostClientModEvents() {
    }
}
