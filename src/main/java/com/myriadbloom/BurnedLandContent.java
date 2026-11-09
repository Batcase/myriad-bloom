package com.myriadbloom;

import com.myriadbloom.world.block.BurntCaramelBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** 燃尽之地的方块与世界内容。 */
public final class BurnedLandContent {
    /** 灰烬石：燃尽之地的基础岩石。 */
    public static final DeferredBlock<Block> ASH_STONE = MyriadBloom.BLOCKS.register("ash_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(1.2F, 6.0F)
                    .sound(SoundType.BASALT)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> ASH_STONE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ash_stone", ASH_STONE);

    /** 燃木：暗红色木头，火焰烧不掉。 */
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> BURNWOOD_LOG =
            MyriadBloom.BLOCKS.register("burnwood_log",
                    () -> new net.minecraft.world.level.block.RotatedPillarBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NETHER)
                                    .strength(2.0F, 6.0F)
                                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> BURNWOOD_LOG_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("burnwood_log", BURNWOOD_LOG);

    /** 灰烬砖：废墟的建材。 */
    public static final DeferredBlock<Block> ASH_BRICKS = MyriadBloom.BLOCKS.register("ash_bricks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> ASH_BRICKS_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ash_bricks", ASH_BRICKS);

    public static final DeferredBlock<Block> CRACKED_ASH_BRICKS = MyriadBloom.BLOCKS.register("cracked_ash_bricks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(1.2F, 6.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> CRACKED_ASH_BRICKS_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("cracked_ash_bricks", CRACKED_ASH_BRICKS);

    /** 焦糖传送门：右键进入燃尽之地。 */
    public static final DeferredBlock<BurntCaramelBlock> BURNT_CARAMEL =
            MyriadBloom.BLOCKS.register("burnt_caramel",
                    () -> new BurntCaramelBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .strength(0.6F)
                            .lightLevel(state -> 6)
                            .sound(SoundType.GLASS)));

    public static final DeferredItem<BlockItem> BURNT_CARAMEL_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("burnt_caramel", BURNT_CARAMEL);

    private BurnedLandContent() {
    }

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(ASH_STONE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(BURNT_CARAMEL_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(BURNWOOD_LOG_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ASH_BRICKS_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(CRACKED_ASH_BRICKS_ITEM);
    }
}
