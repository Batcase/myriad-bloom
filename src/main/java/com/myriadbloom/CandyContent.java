package com.myriadbloom;

import com.myriadbloom.item.LollipopItem;
import com.myriadbloom.world.block.CandyPlantBlock;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Phase 3 candy content: the two edible sweets and the candy corn block.
 *
 * <p>Kept in its own holder class so the large {@link ModBlocks}/{@link ModItems} files stay
 * untouched; {@link #bootstrap()} is called from the mod constructor before the registry events,
 * which is also when it pushes the entries into the creative tab.</p>
 */
public final class CandyContent {
    /** Candy: half a shank, eaten twice as fast, and it costs half a flask of thirst. */
    public static final DeferredItem<Item> CANDY = MyriadBloom.ITEMS.register("candy",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1).saturationModifier(0.1F).fast().build())));

    /** 棒棒糖: sheared off a candy cow, restores one hunger point, a little thirst and 5 s resistance. */
    public static final DeferredItem<LollipopItem> CANDY_LOLLIPOP = MyriadBloom.ITEMS.register(
            "candy_lollipop",
            () -> new LollipopItem(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).fast().build())));

    /** Candy corn: a full shank, eaten twice as fast, also costs half a flask of thirst. */
    public static final DeferredItem<Item> CANDY_CORN = MyriadBloom.ITEMS.register("candy_corn",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.15F).fast().build())));

    /** Candy corn block: yellow, corn patterned, breaks back down into four candy corn. */
    public static final DeferredBlock<Block> CANDY_CORN_BLOCK = MyriadBloom.BLOCKS.register(
            "candy_corn_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> CANDY_CORN_BLOCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_corn_block", CANDY_CORN_BLOCK);

    /** Sweet grass block: the candy dimension's grass replacement (wafer bottom, pink grass top). */
    public static final DeferredBlock<Block> SWEET_GRASS_BLOCK = MyriadBloom.BLOCKS.register(
            "candy_grass_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)));

    public static final DeferredItem<BlockItem> SWEET_GRASS_BLOCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_grass_block", SWEET_GRASS_BLOCK);

    /** Sweet grass: decorative pink grass with small white specks. Not sheep food. */
    public static final DeferredBlock<CandyPlantBlock> SWEET_GRASS = MyriadBloom.BLOCKS.register(
            "sweet_grass",
            () -> new CandyPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final DeferredItem<BlockItem> SWEET_GRASS_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("sweet_grass", SWEET_GRASS);

    /** Candy grass: decorative flat spiral lollipop, one block tall. Not sheep food. */
    public static final DeferredBlock<CandyPlantBlock> CANDY_GRASS = MyriadBloom.BLOCKS.register(
            "candy_grass",
            () -> new CandyPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final DeferredItem<BlockItem> CANDY_GRASS_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_grass", CANDY_GRASS);

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_CORN);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_LOLLIPOP);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_CORN_BLOCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(SWEET_GRASS_BLOCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(SWEET_GRASS_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_GRASS_ITEM);
    }

    private CandyContent() {
    }
}
