package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Biome source of the Frost Realm.
 *
 * <p>Terrain shape, height and climate noise stay <b>identical to the overworld</b> because
 * generation is delegated to a vanilla overworld biome source; only the biome that is finally
 * handed to the chunk generator is swapped for its snowy counterpart. That gives a true mirror
 * world (same rivers, same mountains, same coastlines) without shipping a copy of the worldgen
 * data.</p>
 *
 * <p>Decoding is driven by {@code data/myriadbloom/dimension/permafrost.json}:</p>
 * <pre>{@code
 * "biome_source": {
 *   "type": "myriadbloom:frost_biome_source",
 *   "delegate": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
 *   "snowy_biomes": [ "minecraft:snowy_plains", "minecraft:ice_spikes", ... ]
 * }
 * }</pre>
 *
 * <p>Instances are queried from chunk-generation worker threads, so the remap cache must be
 * concurrent and every field immutable.</p>
 */
public final class FrostBiomeSource extends BiomeSource {
    /** Registered as {@code myriadbloom:frost_biome_source} in {@code Registries.BIOME_SOURCE}. */
    public static final MapCodec<FrostBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("delegate").forGetter(FrostBiomeSource::delegate),
                    RegistryCodecs.homogeneousList(Registries.BIOME)
                            .fieldOf("snowy_biomes")
                            .forGetter(FrostBiomeSource::snowyBiomes))
            .apply(instance, FrostBiomeSource::new));

    private final BiomeSource delegate;
    private final HolderSet<Biome> snowyBiomes;
    private final Holder<Biome> fallback;
    /** Biome used where the lush cave region is collapsed (the 1/5 reduction). */
    private final Holder<Biome> plains;
    /** Frosted land holder, or {@code null} when the biome is not registered. */
    private final Holder<Biome> frostedLand;
    /** Congealed land holder, or {@code null} when the biome is not registered. */
    private final Holder<Biome> congealedLand;
    /** Wish pool biome holder (very rare), or {@code null} when it is not registered. */
    private final Holder<Biome> wishingPool;
    /** 冰洋 (ice ocean) holder, or {@code null} when it is not registered. */
    private final Holder<Biome> iceOcean;
    private final ConcurrentMap<ResourceKey<Biome>, Holder<Biome>> remapCache = new ConcurrentHashMap<>();

    /** {@code myriadbloom:frost_lush_caves}; one position in five falls back to plains. */
    private static final ResourceLocation LUSH_CAVES =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_lush_caves");
    private static final ResourceLocation PLAINS =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_snowy_plains");
    /** 凝结之地: one patch per 16x16 chunk region, never touching another patch. */
    private static final ResourceLocation WISHING_POOL =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_wishing_pool");

    private static final ResourceLocation CONGEALED_LAND =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_congealed");

    /** Scattered flat biome that replaces one in six plains areas. */
    private static final ResourceLocation FROSTED_LAND =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frosted_land");

    /** 冰洋 (ice ocean): a 9x9 chunk patch that only ever replaces frozen ocean. */
    private static final ResourceLocation ICE_OCEAN =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_ice_ocean");

    public FrostBiomeSource(BiomeSource delegate, HolderSet<Biome> snowyBiomes) {
        this.delegate = delegate;
        this.snowyBiomes = snowyBiomes;
        this.fallback = snowyBiomes.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "myriadbloom:frost_biome_source - 'snowy_biomes' must list at least one biome"));
        this.plains = this.find(PLAINS).orElse(this.fallback);
        this.frostedLand = this.find(FROSTED_LAND).orElse(null);
        this.congealedLand = this.find(CONGEALED_LAND).orElse(null);
        this.wishingPool = this.find(WISHING_POOL).orElse(null);
        this.iceOcean = this.find(ICE_OCEAN).orElse(null);
    }

    public BiomeSource delegate() {
        return this.delegate;
    }

    public HolderSet<Biome> snowyBiomes() {
        return this.snowyBiomes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.snowyBiomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> original = this.delegate.getNoiseBiome(x, y, z, sampler);
        Optional<ResourceKey<Biome>> key = original.unwrapKey();
        if (key.isEmpty()) {
            // Should never happen for registry-backed biomes, but never crash worldgen over it.
            return this.fallback;
        }
        if (this.wishingPool != null
                && com.myriadbloom.world.gen.WishingPoolFeature.insidePatch(x * 4, z * 4)) {
            return this.wishingPool;
        }
        if (this.congealedLand != null) {
            if (insideCongealedPatch(x, z)) {
                return this.congealedLand;
            }
            // the climate noise must never produce the congealed land on its own: outside the single
            // patch of the region the climate pick is replaced by the normal snowy biome
            if (original.is(CONGEALED_LAND)) {
                return this.fallback;
            }
        }
        if (insideCongealedBuffer(x, z)) {
            // no other special biome may touch the congealed patch: a ring around it stays normal
            Holder<Biome> mapped = this.remapCache.computeIfAbsent(key.get(), this::snowyVersionOf);
            return mapped;
        }
        if (this.iceOcean != null) {
            // "frozen ocean" in the delegate sense: the remapped snowy biome is the frozen ocean
            Holder<Biome> snowy = this.remapCache.computeIfAbsent(key.get(), this::snowyVersionOf);
            if ((isFrozenOcean(snowy) || isFrozenOcean(this.delegate.getNoiseBiome(x, y, z, sampler)))
                    && com.myriadbloom.world.IceOceanArena.insidePatch(x * 4, z * 4)) {
                // 冰洋 only ever replaces frozen ocean, and only inside its own 9x9 chunk patch
                return this.iceOcean;
            }
        }
        if (this.frostedLand != null && isFlatPlains(key.get())
                && Math.floorMod(positionHash(x >> 11, 1, z >> 11), 3) == 0) {
            // whole 128x128 chunk clusters (2048x2048 blocks) of plains become the flat frosted land,
            // so the biome reads as a region instead of a handful of small patches
            return this.frostedLand;
        }
        Holder<Biome> mapped = this.remapCache.computeIfAbsent(key.get(), this::snowyVersionOf);
        if (isLushCaves(mapped) && Math.floorMod(positionHash(x, y, z), 5) == 0) {
            // shrink the lush cave region by one fifth
            return this.plains;
        }
        return mapped;
    }

    /** True when the block position falls inside the single congealed patch of its region. */
    /** Buffer ring around the congealed patch where no other special biome is allowed. */
    private static boolean insideCongealedBuffer(int quartX, int quartZ) {
        return com.myriadbloom.world.gen.CongealedLandFeature.insideBuffer(quartX * 4, quartZ * 4);
    }

    private static boolean insideCongealedPatch(int quartX, int quartZ) {
        // getNoiseBiome works in quart coordinates (4 blocks per unit), the patch maths is in blocks
        int x = quartX * 4;
        int z = quartZ * 4;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return com.myriadbloom.world.gen.CongealedLandFeature.insidePatch(x, z);
    }

    private static boolean isFlatPlains(ResourceKey<Biome> key) {
        ResourceLocation id = key.location();
        return id.equals(ResourceLocation.withDefaultNamespace("plains"))
                || id.equals(ResourceLocation.withDefaultNamespace("meadow"));
    }

    private static boolean isFrozenOcean(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(key -> {
                    ResourceLocation id = key.location();
                    return id.equals(ResourceLocation.withDefaultNamespace("frozen_ocean"))
                            || id.equals(ResourceLocation.withDefaultNamespace("deep_frozen_ocean"))
                            || id.equals(ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_frozen_ocean"));
                })
                .orElse(false);
    }

    private static boolean isLushCaves(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().equals(LUSH_CAVES)).orElse(false);
    }

    /** Stable per-position hash, so the same block always resolves to the same biome. */
    private static int positionHash(int x, int y, int z) {
        int hash = x * 0x9E3779B1 + y * 0x85EBCA77 + z * 0xC2B2AE3D;
        hash ^= hash >>> 15;
        hash *= 0x2545F491;
        hash ^= hash >>> 13;
        return hash;
    }

    private Holder<Biome> snowyVersionOf(ResourceKey<Biome> original) {
        ResourceLocation target = FrostBiomeMap.resolve(original.location());
        // Prefer the mod's own biome copies (they strip the vanilla ores and add the frost ore).
        ResourceLocation custom = ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_" + target.getPath());
        Optional<Holder<Biome>> customHolder = find(custom);
        if (customHolder.isPresent()) {
            return customHolder.get();
        }
        return find(target).orElse(this.fallback);
    }

    private Optional<Holder<Biome>> find(ResourceLocation target) {
        return this.snowyBiomes.stream()
                .filter(holder -> holder.unwrapKey()
                        .map(key -> key.location().equals(target))
                        .orElse(false))
                .findFirst();
    }
}