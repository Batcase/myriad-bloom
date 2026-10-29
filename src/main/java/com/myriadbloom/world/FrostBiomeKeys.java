package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/** Registry keys of the mod's own biomes. */
public final class FrostBiomeKeys {
    public static final ResourceKey<Biome> ICE_OCEAN = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_ice_ocean"));

    private FrostBiomeKeys() {
    }
}
