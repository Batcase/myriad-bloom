package com.myriadbloom.world.gen;

import com.myriadbloom.api.MyriadBloomApi;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Turns villages inside the Frost Realm into frozen ruins.
 *
 * <p>Blocks are <b>replaced</b>, not covered: every placed block of a building has a
 * {@code probability} chance of becoming ice, so walls, floors and roofs turn into a mix of their
 * original material and ice. Air, structure voids and jigsaw markers are never touched (replacing a
 * jigsaw block would break structure generation).</p>
 *
 * <p>The same processor thins out the population: villager entity markers are dropped with
 * {@code villager_removal_chance} probability, which is how "90% fewer villagers" is implemented -
 * villagers in villages are entity markers baked into the templates, not spawned by a spawner.</p>
 *
 * <p>Both effects are gated on the dimension, so overworld villages stay completely vanilla even
 * though the village template pools are overridden globally.</p>
 */
public class FrostyVillageProcessor extends StructureProcessor {
    public static final MapCodec<FrostyVillageProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability")
                            .orElse(0.4F).forGetter(FrostyVillageProcessor::probability),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("villager_removal_chance")
                            .orElse(0.9F).forGetter(FrostyVillageProcessor::villagerRemovalChance))
            .apply(instance, FrostyVillageProcessor::new));

    /** Registered as {@code myriadbloom:frosty_village}. */
    public static final StructureProcessorType<FrostyVillageProcessor> TYPE = () -> CODEC;

    private final float probability;
    private final float villagerRemovalChance;

    public FrostyVillageProcessor(float probability, float villagerRemovalChance) {
        this.probability = probability;
        this.villagerRemovalChance = villagerRemovalChance;
    }

    public float probability() {
        return this.probability;
    }

    public float villagerRemovalChance() {
        return this.villagerRemovalChance;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }

    @Override
    public StructureTemplate.StructureBlockInfo process(LevelReader level, BlockPos pos, BlockPos pivot,
                                                        StructureTemplate.StructureBlockInfo original,
                                                        StructureTemplate.StructureBlockInfo current,
                                                        StructurePlaceSettings settings,
                                                        StructureTemplate template) {
        if (current == null || !inMyriadBloom(level)) {
            return current;
        }
        BlockState state = current.state();
        if (state.isAir() || state.is(Blocks.STRUCTURE_VOID) || state.is(Blocks.JIGSAW)) {
            return current;
        }
        // 村庄的火把/灯笼亮度太高（14/15），会把附近被冻结的方块烤化：
        // 统一换成亮度 11 的霜灯，冰就不会融化
        if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return new StructureTemplate.StructureBlockInfo(current.pos(),
                    com.myriadbloom.FrostedLandContent.FROST_LAMP.get().defaultBlockState(), null);
        }
        if (randomAt(current.pos()).nextFloat() < this.probability) {
            return new StructureTemplate.StructureBlockInfo(current.pos(), Blocks.ICE.defaultBlockState(), null);
        }
        return current;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos pos,
                                                              StructureTemplate.StructureEntityInfo raw,
                                                              StructureTemplate.StructureEntityInfo current,
                                                              StructurePlaceSettings settings,
                                                              StructureTemplate template) {
        if (current == null || !inMyriadBloom(level)) {
            return current;
        }
        CompoundTag nbt = current.nbt;
        if (nbt != null && nbt.contains("id")) {
            String id = nbt.getString("id");
            boolean villager = "minecraft:villager".equals(id) || "minecraft:zombie_villager".equals(id);
            if (villager && randomAt(current.blockPos).nextFloat() < this.villagerRemovalChance) {
                return null;
            }
        }
        return current;
    }

    /** Deterministic per-position randomness, so a chunk regenerated later looks the same. */
    private static RandomSource randomAt(BlockPos pos) {
        return RandomSource.create(pos.asLong() * 0x9E3779B97F4A7C15L + 0x5DEECE66DL);
    }

    private static boolean inMyriadBloom(LevelReader level) {
        if (level instanceof WorldGenLevel worldGenLevel) {
            return worldGenLevel.getLevel().dimension().equals(MyriadBloomApi.FROST_REALM);
        }
        return false;
    }
}
