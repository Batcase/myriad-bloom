package com.myriadbloom.world;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Maps an overworld biome id to the snowy biome that replaces it inside the Frost Realm.
 *
 * <p>Lookup order: explicit override ({@link #register}) &rarr; built-in vanilla table &rarr;
 * name-based heuristic. The heuristic is what keeps unknown/modded biomes from leaking warm
 * biomes into the realm: anything we cannot classify becomes snowy plains.</p>
 */
public final class FrostBiomeMap {
    private static final Map<ResourceLocation, ResourceLocation> OVERRIDES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> VANILLA = new LinkedHashMap<>();

    static {
        // --- flat / temperate -------------------------------------------------------------
        map("plains", "snowy_plains");
        map("sunflower_plains", "snowy_plains");
        map("meadow", "snowy_plains");
        map("mushroom_fields", "snowy_plains");
        map("dripstone_caves", "snowy_plains");
        // lush caves are kept in the realm; the biome source collapses ~1/5 of their positions back
        // to snowy plains so the cave region shrinks
        map("lush_caves", "lush_caves");
        map("deep_dark", "snowy_plains");
        map("swamp", "snowy_taiga");
        map("mangrove_swamp", "snowy_taiga");

        // --- forested ---------------------------------------------------------------------
        map("forest", "grove");
        map("flower_forest", "grove");
        map("birch_forest", "grove");
        map("old_growth_birch_forest", "grove");
        map("dark_forest", "grove");
        map("cherry_grove", "grove");
        map("grove", "grove");
        map("taiga", "snowy_taiga");
        map("old_growth_pine_taiga", "snowy_taiga");
        map("old_growth_spruce_taiga", "snowy_taiga");
        map("snowy_taiga", "snowy_taiga");
        map("jungle", "snowy_taiga");
        map("sparse_jungle", "snowy_taiga");
        map("bamboo_jungle", "snowy_taiga");

        // --- dry -> frozen desert ---------------------------------------------------------
        map("desert", "ice_spikes");
        map("badlands", "ice_spikes");
        map("eroded_badlands", "ice_spikes");
        map("wooded_badlands", "ice_spikes");
        map("savanna", "snowy_plains");
        map("savanna_plateau", "snowy_plains");
        map("windswept_savanna", "snowy_plains");

        // --- highlands --------------------------------------------------------------------
        map("windswept_hills", "snowy_slopes");
        map("windswept_gravelly_hills", "snowy_slopes");
        map("windswept_forest", "snowy_slopes");
        map("snowy_slopes", "snowy_slopes");
        map("stony_peaks", "frozen_peaks");
        map("jagged_peaks", "jagged_peaks");
        map("frozen_peaks", "frozen_peaks");

        // --- water & coast ----------------------------------------------------------------
        map("river", "frozen_river");
        map("frozen_river", "frozen_river");
        map("beach", "snowy_beach");
        map("snowy_beach", "snowy_beach");
        map("stony_shore", "snowy_beach");
        map("ocean", "frozen_ocean");
        map("deep_ocean", "frozen_ocean");
        map("warm_ocean", "frozen_ocean");
        map("lukewarm_ocean", "frozen_ocean");
        map("deep_lukewarm_ocean", "frozen_ocean");
        map("cold_ocean", "frozen_ocean");
        map("deep_cold_ocean", "frozen_ocean");
        map("frozen_ocean", "frozen_ocean");
        map("deep_frozen_ocean", "frozen_ocean");
    }

    private static void map(String from, String to) {
        VANILLA.put(ResourceLocation.withDefaultNamespace(from), ResourceLocation.withDefaultNamespace(to));
    }

    /** Adds or replaces an explicit remap. Used by addons for modded biomes. */
    public static synchronized void register(ResourceLocation from, ResourceLocation to) {
        OVERRIDES.put(from, to);
    }

    /** Resolves the snowy biome id that replaces {@code original}. Never returns {@code null}. */
    public static ResourceLocation resolve(ResourceLocation original) {
        ResourceLocation override;
        synchronized (FrostBiomeMap.class) {
            override = OVERRIDES.get(original);
        }
        if (override != null) {
            return override;
        }
        ResourceLocation vanilla = VANILLA.get(original);
        if (vanilla != null) {
            return vanilla;
        }
        return heuristic(original);
    }

    private static ResourceLocation heuristic(ResourceLocation original) {
        String path = original.getPath();
        if (path.contains("ocean")) {
            return ResourceLocation.withDefaultNamespace("frozen_ocean");
        }
        if (path.contains("river")) {
            return ResourceLocation.withDefaultNamespace("frozen_river");
        }
        if (path.contains("beach") || path.contains("shore") || path.contains("coast")) {
            return ResourceLocation.withDefaultNamespace("snowy_beach");
        }
        if (path.contains("peak") || path.contains("mountain") || path.contains("slope")) {
            return ResourceLocation.withDefaultNamespace("snowy_slopes");
        }
        if (path.contains("hill")) {
            return ResourceLocation.withDefaultNamespace("snowy_slopes");
        }
        if (path.contains("desert") || path.contains("badland") || path.contains("mesa") || path.contains("dune")) {
            return ResourceLocation.withDefaultNamespace("ice_spikes");
        }
        if (path.contains("forest") || path.contains("wood") || path.contains("jungle")
                || path.contains("swamp") || path.contains("grove") || path.contains("taiga")) {
            return ResourceLocation.withDefaultNamespace("grove");
        }
        if (path.contains("snow") || path.contains("ice") || path.contains("frozen") || path.contains("glacier")) {
            return ResourceLocation.withDefaultNamespace("snowy_plains");
        }
        return ResourceLocation.withDefaultNamespace("snowy_plains");
    }

    private FrostBiomeMap() {
    }
}
