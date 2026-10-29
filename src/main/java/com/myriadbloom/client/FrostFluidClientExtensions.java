package com.myriadbloom.client;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client rendering of the syrup fluid: vanilla's water textures tinted pale pink.
 *
 * <p>Reusing {@code minecraft:block/water_still} / {@code water_flow} keeps the flowing animation,
 * the underwater overlay and the fog behaviour that water already has, so syrup only needs a tint
 * and no custom texture set.</p>
 */
@EventBusSubscriber(modid = MyriadBloom.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FrostFluidClientExtensions {
    /** ARGB tint: a soft bubblegum pink. */
    private static final int SYRUP_TINT = 0xFFF2A8C8;
    private static final ResourceLocation WATER_STILL =
            ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW =
            ResourceLocation.withDefaultNamespace("block/water_flow");

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public int getTintColor() {
                return SYRUP_TINT;
            }
        }, ModFluids.SYRUP_TYPE.get());
    }

    private FrostFluidClientExtensions() {
    }
}
