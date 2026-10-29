package com.myriadbloom.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Biome source of the Candy Dimension.
 *
 * <p>Generation is delegated to the vanilla overworld {@code multi_noise} source - exactly the way
 * {@link FrostBiomeSource} mirrors the overworld for the frost realm - but the biome handed to the
 * chunk generator is always a candy biome, so the climate noise keeps shaping hills and valleys
 * while every column is painted with the candy biome's features (the candy conversion and the sugar
 * floor).</p>
 *
 * <p>Decoding is driven by {@code data/myriadbloom/dimension/candy.json}:</p>
 * <pre>{@code
 * "biome_source": {
 *   "type": "myriadbloom:candy_biome_source",
 *   "delegate": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
 *   "biomes": [ "myriadbloom:candy_plains" ]
 * }
 * }</pre>
 *
 * <p>Instances are queried from chunk-generation worker threads, so every field is immutable and no
 * mutable cache is used.</p>
 */
public final class CandyBiomeSource extends BiomeSource {
    /** Registered as {@code myriadbloom:candy_biome_source} in {@code Registries.BIOME_SOURCE}. */
    public static final MapCodec<CandyBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("delegate").forGetter(CandyBiomeSource::delegate),
                    RegistryCodecs.homogeneousList(Registries.BIOME)
                            .fieldOf("biomes")
                            .forGetter(CandyBiomeSource::biomes))
            .apply(instance, CandyBiomeSource::new));

    private final BiomeSource delegate;
    private final HolderSet<Biome> biomes;
    private final Holder<Biome> candyBiome;

    public CandyBiomeSource(BiomeSource delegate, HolderSet<Biome> biomes) {
        this.delegate = delegate;
        this.biomes = biomes;
        this.candyBiome = biomes.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "myriadbloom:candy_biome_source - 'biomes' must list at least one biome"));
    }

    public BiomeSource delegate() {
        return this.delegate;
    }

    public HolderSet<Biome> biomes() {
        return this.biomes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return this.candyBiome;
    }
}
