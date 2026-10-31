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
    /** 风暴林地 (storm woodland) holder, or {@code null} when it is not registered. */
    private final Holder<Biome> stormWoodland;
    private final ConcurrentMap<ResourceKey<Biome>, Holder<Biome>> remapCache = new ConcurrentHashMap<>();

    /** {@code myriadbloom:frost_lush_caves}; one position in five falls back to plains. */
    private static final ResourceLocation LUSH_CAVES =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_lush_caves");
    private static final ResourceLocation SNOWY_PLAINS =
            ResourceLocation.fromNamespaceAndPath(com.myriadbloom.MyriadBloom.MODID, "frost_snowy_plains");
    private static final ResourceLocation SNOWY_TAIGA =
            ResourceLocation.fromNamespaceAndPath(com.myriadbloom.MyriadBloom.MODID, "frost_snowy_taiga");
    private static final ResourceLocation SNOWY_GROVE =
            ResourceLocation.fromNamespaceAndPath(com.myriadbloom.MyriadBloom.MODID, "frost_grove");
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

    /** 风暴林地 (storm woodland): a plus shaped forest ringing the congealed land. */
    private static final ResourceLocation STORM_WOODLAND =
            ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_storm_woodland");

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
        this.stormWoodland = this.find(STORM_WOODLAND).orElse(null);
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
        Holder<Biome> mappedBiome = this.remapCache.computeIfAbsent(key.get(), this::snowyVersionOf);
        if (this.congealedLand != null && !isWaterBiome(mappedBiome)) {
            if (insideCongealedPatch(x, z)) {
                // 凝结之地只长在陆地上：海洋、河流、海滩上的区域不生成
                return this.congealedLand;
            }
            // the climate noise must never produce the congealed land on its own: outside the single
            // patch of the region the climate pick is replaced by the normal snowy biome
            if (original.is(CONGEALED_LAND)) {
                return this.fallback;
            }
        }
        if (this.congealedLand != null && !isWaterBiome(mappedBiome) && insideCongealedBuffer(x, z)) {
            // 凝结之地周围一圈缓冲区：不放别的特殊群系
            return mappedBiome;
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
        if (this.stormWoodland != null && !isWaterBiome(mappedBiome)
                && com.myriadbloom.world.gen.StormCross.insideCross(x * 4, z * 4)) {
            // the plus shaped storm woodland wraps the congealed land of its own region; oceans keep
            // their water biome so the forest never turns a frozen sea into a taiga
            return this.stormWoodland;
        }
        if (this.wishingPool != null && !isWaterBiome(mappedBiome)
                && com.myriadbloom.world.gen.WishingPoolFeature.insidePatch(x * 4, z * 4)) {
            return this.wishingPool;
        }
        if (this.frostedLand != null && isFlatPlains(key.get())
                && com.myriadbloom.world.gen.FrostWarp.noise(x * 4 / 700.0D, z * 4 / 700.0D, 0x3F0D) > 0.15D) {
            // 霜覆之地：用一大块噪声决定范围，边界是不规则的，不再是 128 格对齐的方块区
            return this.frostedLand;
        }
        // 海洋和河流太多了：七成的判定改写成雪原/雪林/雪棘，只留三成真正的水域
        if (isWaterBiome(mappedBiome)) {
            // 海洋改判成陆地时用平滑噪声画海岸线：边界是自然曲线，不再是 4x4 的方格锯齿
            double coast = com.myriadbloom.world.gen.FrostWarp.noise(x * 4 / 260.0D, z * 4 / 260.0D, 0x7A11L);
            if (coast > -0.02D) {
                double pick = com.myriadbloom.world.gen.FrostWarp.noise(x * 4 / 900.0D, z * 4 / 900.0D, 0x1166L);
                if (pick < -0.22D) {
                    return this.find(SNOWY_TAIGA).orElse(this.plains);
                }
                if (pick > 0.24D) {
                    return this.find(SNOWY_GROVE).orElse(this.plains);
                }
                return this.find(SNOWY_PLAINS).orElse(this.plains);
            }
        }
        Holder<Biome> mapped = mappedBiome;
        if (isLushCaves(mapped) && Math.floorMod(positionHash(x, y, z), 5) == 0) {
            // shrink the lush cave region by one fifth
            return this.plains;
        }
        return mapped;
    }

    /** 海洋、河流、海滩这类以水为主的群系：特殊结构一律不生成在上面。 */
    private static boolean isWaterBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            String path = key.location().getPath();
            return path.contains("ocean") || path.contains("river") || path.contains("beach");
        }).orElse(false);
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