package com.myriadbloom;

import com.myriadbloom.world.block.ThinIceBlock;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Content of the 结霜之地 (frosted land): the giant wood set - log, planks and the four tools.
 *
 * <p>Everything behaves like oak (same hardness, sound and wood tier), only the textures are a much
 * darker brown.</p>
 */
public final class FrostedLandContent {
    /** Fallen giant log: same properties as an oak log, darker texture. */
    public static final DeferredBlock<RotatedPillarBlock> GIANT_LOG = MyriadBloom.BLOCKS.register(
            "giant_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    /** 巨木树叶. */
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> GIANT_LEAVES =
            MyriadBloom.BLOCKS.register("giant_leaves",
                    () -> new net.minecraft.world.level.block.LeavesBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                                    .strength(0.2F)
                                    .randomTicks()
                                    .sound(net.minecraft.world.level.block.SoundType.GRASS)
                                    .noOcclusion()
                                    .isValidSpawn((state, level, pos, type) -> false)
                                    .isSuffocating((state, level, pos) -> false)
                                    .isViewBlocking((state, level, pos) -> false)
                                    .ignitedByLava()
                                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> GIANT_LEAVES_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("giant_leaves", GIANT_LEAVES);

    /** 巨木树苗. */
    public static final DeferredBlock<com.myriadbloom.world.block.GiantSaplingBlock> GIANT_SAPLING =
            MyriadBloom.BLOCKS.register("giant_sapling",
                    () -> new com.myriadbloom.world.block.GiantSaplingBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                                    .strength(0.0F)
                                    .randomTicks()
                                    .sound(net.minecraft.world.level.block.SoundType.GRASS)
                                    .noCollission()
                                    .instabreak()
                                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> GIANT_SAPLING_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("giant_sapling", GIANT_SAPLING);

    public static final DeferredItem<BlockItem> GIANT_LOG_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("giant_log", GIANT_LOG);

    public static final DeferredBlock<Block> GIANT_PLANKS = MyriadBloom.BLOCKS.register(
            "giant_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> GIANT_PLANKS_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("giant_planks", GIANT_PLANKS);

    /** Thin ice: breaks under a standing entity, refreezes later, never drops anything. */
    public static final DeferredBlock<ThinIceBlock> THIN_ICE = MyriadBloom.BLOCKS.register(
            "thin_ice",
            () -> new ThinIceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .randomTicks()));

    /** 凝结之冰: obsidian hard ice of the congealed land. */
    public static final DeferredBlock<com.myriadbloom.world.block.CongealedIceBlock> CONGEALED_ICE =
            MyriadBloom.BLOCKS.register("congealed_ice",
                    () -> new com.myriadbloom.world.block.CongealedIceBlock(
                            com.myriadbloom.world.block.CongealedIceBlock.createProperties()));

    public static final DeferredItem<BlockItem> CONGEALED_ICE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("congealed_ice", CONGEALED_ICE);

    /** 寒光灯: a sea lantern wannabe that lights but never warms. */
    public static final DeferredBlock<Block> FROST_LAMP = MyriadBloom.BLOCKS.register("frost_lamp",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.3F)
                    // 亮度 11：低于融冰阈值（>11），村庄里的冰不会被灯烤化
                    .lightLevel(state -> 11)
                    .sound(SoundType.GLASS)));

    public static final DeferredItem<BlockItem> FROST_LAMP_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_lamp", FROST_LAMP);

    /** 寒带苔石: mossy cobblestone with white moss instead of green. */
    public static final DeferredBlock<Block> FROST_MOSSY_COBBLESTONE = MyriadBloom.BLOCKS.register(
            "frost_mossy_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> FROST_MOSSY_COBBLESTONE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_mossy_cobblestone", FROST_MOSSY_COBBLESTONE);

    /** 许愿池: the wishing pool of its own tiny biome. */
    public static final DeferredBlock<com.myriadbloom.world.block.WishingPoolBlock> WISHING_POOL =
            MyriadBloom.BLOCKS.register("wishing_pool",
                    () -> new com.myriadbloom.world.block.WishingPoolBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WATER)
                            .strength(3.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> WISHING_POOL_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("wishing_pool", WISHING_POOL);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.myriadbloom.world.block.WishingPoolBlockEntity>>
            WISHING_POOL_ENTITY = BossContent.BLOCK_ENTITIES.register("wishing_pool",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(com.myriadbloom.world.block.WishingPoolBlockEntity::new, WISHING_POOL.get())
                            .build(null));

    /** 雪花: the decorative snowflake plant of the snowy plains. */
    public static final DeferredBlock<com.myriadbloom.world.block.SnowflakeFlowerBlock> SNOWFLAKE =
            MyriadBloom.BLOCKS.register(
                    "snowflake",
                    () -> new com.myriadbloom.world.block.SnowflakeFlowerBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final DeferredItem<BlockItem> SNOWFLAKE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("snowflake", SNOWFLAKE);

    /** Creative-only item: the block itself never drops when broken. */
    public static final DeferredItem<BlockItem> THIN_ICE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("thin_ice", THIN_ICE);

    /** Wooden tier tools in the giant wood colours: 59 durability, wood mining level. */
    public static final DeferredItem<SwordItem> GIANT_WOODEN_SWORD = MyriadBloom.ITEMS.register(
            "giant_wooden_sword",
            () -> new SwordItem(Tiers.WOOD, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 3.0F, -2.4F))));

    public static final DeferredItem<PickaxeItem> GIANT_WOODEN_PICKAXE = MyriadBloom.ITEMS.register(
            "giant_wooden_pickaxe",
            () -> new PickaxeItem(Tiers.WOOD, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(Tiers.WOOD, 1.0F, -2.8F))));

    public static final DeferredItem<AxeItem> GIANT_WOODEN_AXE = MyriadBloom.ITEMS.register(
            "giant_wooden_axe",
            () -> new AxeItem(Tiers.WOOD, new Item.Properties()
                    .attributes(AxeItem.createAttributes(Tiers.WOOD, 6.0F, -3.2F))));

    public static final DeferredItem<ShovelItem> GIANT_WOODEN_SHOVEL = MyriadBloom.ITEMS.register(
            "giant_wooden_shovel",
            () -> new ShovelItem(Tiers.WOOD, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(Tiers.WOOD, 1.5F, -3.0F))));

    /** Dropped by the carnivorous rabbit with a 10% base chance. */
    public static final DeferredItem<Item> FRENZIED_CORE = MyriadBloom.ITEMS.register(
            "frenzied_core",
            () -> new Item(new Item.Properties()));

    /** 时之祭坛：1.5 格高的祭坛。 */
    public static final DeferredBlock<com.myriadbloom.world.block.TimeAltarBlock> TIME_ALTAR =
            MyriadBloom.BLOCKS.register("time_altar",
                    () -> new com.myriadbloom.world.block.TimeAltarBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .mapColor(net.minecraft.world.level.material.MapColor.ICE)
                                    .strength(3.0F, 1200.0F)
                                    .lightLevel(state -> 7)
                                    .sound(net.minecraft.world.level.block.SoundType.AMETHYST)
                                    .noOcclusion()));

    /** 永不熄灭的炉心。 */
    public static final DeferredBlock<com.myriadbloom.world.block.EverburningCoreBlock> EVERBURNING_CORE =
            MyriadBloom.BLOCKS.register("everburning_core",
                    () -> new com.myriadbloom.world.block.EverburningCoreBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)
                                    .strength(2.0F, 6.0F)
                                    .lightLevel(state -> 15)
                                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> EVERBURNING_CORE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("everburning_core", EVERBURNING_CORE);

    public static final DeferredItem<BlockItem> TIME_ALTAR_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("time_altar", TIME_ALTAR);

    /** 迷宫锁孔。 */
    public static final DeferredBlock<com.myriadbloom.world.block.MazeLockBlock> MAZE_LOCK =
            MyriadBloom.BLOCKS.register("maze_lock",
                    () -> new com.myriadbloom.world.block.MazeLockBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_CYAN)
                                    .strength(-1.0F, 3600000.0F)
                                    .lightLevel(state -> 5)
                                    .sound(net.minecraft.world.level.block.SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> MAZE_LOCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("maze_lock", MAZE_LOCK);

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(TIME_ALTAR_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(MAZE_LOCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(EVERBURNING_CORE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_LOG_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_LEAVES_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_SAPLING_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_PLANKS_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_WOODEN_SWORD);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_WOODEN_PICKAXE);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_WOODEN_AXE);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_WOODEN_SHOVEL);
        ModItems.CREATIVE_TAB_ITEMS.add(FRENZIED_CORE);
        ModItems.CREATIVE_TAB_ITEMS.add(THIN_ICE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(SNOWFLAKE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(CONGEALED_ICE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(WISHING_POOL_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_LAMP_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_MOSSY_COBBLESTONE_ITEM);
    }

    private FrostedLandContent() {
    }
}
