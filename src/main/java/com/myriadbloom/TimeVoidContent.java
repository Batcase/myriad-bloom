package com.myriadbloom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 时之虚空的方块。
 *
 * <p>{@code time_boundary} 是脚下那块几乎透明的平台，{@code time_barrier} 围成整个盒子的墙。</p>
 */
public final class TimeVoidContent {
    /** 时之边界：几乎透明、无法破坏。 */
    public static final DeferredBlock<Block> TIME_BOUNDARY = MyriadBloom.BLOCKS.register("time_boundary",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> TIME_BOUNDARY_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("time_boundary", TIME_BOUNDARY);

    /** 时之结界：围成盒子的墙。 */
    public static final DeferredBlock<Block> TIME_BARRIER = MyriadBloom.BLOCKS.register("time_barrier",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 9)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> TIME_BARRIER_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("time_barrier", TIME_BARRIER);

    private TimeVoidContent() {
    }

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(TIME_BOUNDARY_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(TIME_BARRIER_ITEM);
    }
}
