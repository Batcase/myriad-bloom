package com.myriadbloom.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Snow fortress as a real {@link Structure}.
 *
 * <p>It used to be a placed feature, which meant {@code /locate} could not find it. Registering it as
 * a structure gives it a structure set (spacing/separation), makes it show up in
 * {@code /locate structure myriadbloom:snow_fortress} and keeps the "half as common as a village"
 * density: villages use spacing 34, so 48 gives {@code (34/48)^2 = 0.5} of their rate.</p>
 *
 * <p>The actual block placement is delegated to {@link SnowFortressFeature#build}, which flattens the
 * site with a gradient and then raises the glacier ice igloo.</p>
 */
public class SnowFortressStructure extends Structure {
    public static final MapCodec<SnowFortressStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance))
                    .apply(instance, SnowFortressStructure::new));

    public SnowFortressStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return FrostStructures.SNOW_FORTRESS.get();
    }


    /** Same rule as the feature, but evaluated from the generator's heightmap (no level available here). */
    private static boolean isFlatSite(GenerationContext context, BlockPos origin) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int outer = SnowFortressFeature.DOME_FLAT_RADIUS;
        for (int dz = -outer; dz <= outer; dz += 3) {
            for (int dx = -outer; dx <= outer; dx += 3) {
                if (dx * dx + dz * dz > outer * outer) {
                    continue;
                }
                int height = context.chunkGenerator().getBaseHeight(origin.getX() + dx, origin.getZ() + dz,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                min = Math.min(min, height);
                max = Math.max(max, height);
                if (max - min > SnowFortressFeature.MAX_SLOPE) {
                    return false;
                }
            }
        }
        return max - min <= SnowFortressFeature.MAX_SLOPE;
    }

    /**
     * True when the congealed land reaches into the fortress footprint.
     *
     * <p>The dome is 42 blocks across, so a site in normal snowy plains can still have the congealed
     * biome bleeding into it. The biome source is available from the generation context, so the whole
     * footprint is sampled and such sites are skipped.</p>
     */
    private static boolean touchesCongealedLand(GenerationContext context, BlockPos origin) {
        net.minecraft.world.level.biome.BiomeSource source = context.chunkGenerator().getBiomeSource();
        net.minecraft.world.level.biome.Climate.Sampler sampler = context.randomState().sampler();
        int reach = SnowFortressFeature.APRON_RADIUS;
        for (int dz = -reach; dz <= reach; dz += 12) {
            for (int dx = -reach; dx <= reach; dx += 12) {
                if (dx * dx + dz * dz > reach * reach) {
                    continue;
                }
                int blockX = origin.getX() + dx;
                int blockZ = origin.getZ() + dz;
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                        source.getNoiseBiome(blockX >> 2, origin.getY() >> 2, blockZ >> 2, sampler);
                if (biome.is(com.myriadbloom.world.CongealedLandMechanic.BIOME)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Only the snowy plains of the realm may host a fortress - its terrain is flat and safe. */
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> SNOWY_PLAINS =
            net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.BIOME,
                    com.myriadbloom.api.MyriadBloomApi.id("frost_snowy_plains"));

    /**
     * True when the site is anything other than the realm's snowy plains.
     *
     * <p>The data driven biome list should already do this, but the structure is also placed where the
     * biome source remaps vanilla biomes, so the answer is checked again from the generation context.</p>
     */
    private static boolean notSnowyPlains(GenerationContext context, BlockPos origin) {
        net.minecraft.world.level.biome.BiomeSource source = context.chunkGenerator().getBiomeSource();
        net.minecraft.world.level.biome.Climate.Sampler sampler = context.randomState().sampler();
        // the site itself and a ring 32 blocks out must all be snowy plains, so the fortress never
        // straddles the border of the biome
        for (int dz = -32; dz <= 32; dz += 16) {
            for (int dx = -32; dx <= 32; dx += 16) {
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                        source.getNoiseBiome((origin.getX() + dx) >> 2, origin.getY() >> 2,
                                (origin.getZ() + dz) >> 2, sampler);
                if (!biome.is(SNOWY_PLAINS)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        // /locate ignores a rejected generation point, so a start has to exist wherever the placement
        // put one - otherwise the located spot would be empty. The feature adapts the ground instead.
        BlockPos origin = new BlockPos(x, y, z);
        if (notSnowyPlains(context, origin)) {
            // the fortress only ever stands on the realm's flat snowy plains
            return Optional.empty();
        }
        if (touchesCongealedLand(context, origin)) {
            // never raise a fortress inside or against the congealed land
            return Optional.empty();
        }
        BlockPos site = origin;
        return Optional.of(new GenerationStub(site,
                builder -> builder.addPiece(new SnowFortressPiece(site))));
    }
}
