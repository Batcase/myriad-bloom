package com.myriadbloom.world.block;

import com.myriadbloom.StormWoodlandContent;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 古树树苗 (ancient sapling).
 *
 * <p>Vanilla saplings grow through {@link TreeGrower}, which is fine for vanilla tree features but
 * resolves the configured feature through the registry on every attempt. This sapling keeps that path
 * for random growth, and overrides bonemeal with a direct, deterministic call into our own configured
 * feature, so a bone meal always produces a tree.</p>
 */
public class AncientSaplingBlock extends SaplingBlock {
    public AncientSaplingBlock(BlockBehaviour.Properties properties) {
        super(new TreeGrower("ancient", Optional.empty(),
                Optional.of(StormWoodlandContent.ANCIENT_TREE_FEATURE), Optional.empty()), properties);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, random);
    }

    /** Grows the tree at this position; the sapling is restored when the tree could not be placed. */
    public static boolean grow(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState sapling = StormWoodlandContent.ANCIENT_SAPLING.get().defaultBlockState();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
        boolean placed = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(StormWoodlandContent.ANCIENT_TREE_FEATURE)
                .map(holder -> holder.value().place(level,
                        level.getChunkSource().getGenerator(), random, pos))
                .orElse(false);
        if (!placed) {
            level.setBlock(pos, sapling, 4);
        }
        return placed;
    }
}
