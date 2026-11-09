package com.myriadbloom.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * 燃尽之地的群系源：大部分是灰烬平原，按大尺度噪声在其中散布玄武岩三角洲。
 *
 * <p>{@code biomes} 的第一个是平原，第二个是三角洲。</p>
 */
public final class BurnedBiomeSource extends BiomeSource {
    public static final MapCodec<BurnedBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RegistryCodecs.homogeneousList(Registries.BIOME)
                            .fieldOf("biomes")
                            .forGetter(BurnedBiomeSource::biomes))
            .apply(instance, BurnedBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final Holder<Biome> plains;
    private final Holder<Biome> deltas;

    public BurnedBiomeSource(HolderSet<Biome> biomes) {
        List<Holder<Biome>> list = biomes.stream().toList();
        if (list.isEmpty()) {
            throw new IllegalArgumentException("myriadbloom:burned_biome_source needs at least one biome");
        }
        this.biomes = biomes;
        this.plains = list.get(0);
        this.deltas = list.size() > 1 ? list.get(1) : list.get(0);
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
        // 大尺度噪声：约三分之一的区域是玄武岩三角洲
        double noise = com.myriadbloom.world.gen.FrostWarp.noise(x * 4 / 380.0D, z * 4 / 380.0D, 0x8EE7L);
        return noise > 0.22D ? this.deltas : this.plains;
    }
}
