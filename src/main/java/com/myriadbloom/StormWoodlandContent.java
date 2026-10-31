package com.myriadbloom;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Blocks of the 风暴林地 (storm woodland): the ancient wood and its leaves.
 *
 * <p>Both are ordinary wood tier blocks; the trees that use them are built by
 * {@link com.myriadbloom.world.gen.AncientTreeFeature}.</p>
 */
public final class StormWoodlandContent {
    /** 古木 (ancient wood): a pale, ice bleached log. */
    public static final DeferredBlock<RotatedPillarBlock> ANCIENT_WOOD = MyriadBloom.BLOCKS.register(
            "ancient_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> ANCIENT_WOOD_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ancient_wood", ANCIENT_WOOD);

    /** 古树树叶 (ancient leaves): frost covered needles. */
    public static final DeferredBlock<LeavesBlock> ANCIENT_LEAVES = MyriadBloom.BLOCKS.register(
            "ancient_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> ANCIENT_LEAVES_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ancient_leaves", ANCIENT_LEAVES);

    /** The configured ancient tree, grown from a sapling. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> ANCIENT_TREE_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "ancient_tree"));

    /** 古树树苗 (ancient sapling): grows into an ancient tree, same drop chance as any vanilla sapling. */
    public static final DeferredBlock<com.myriadbloom.world.block.AncientSaplingBlock> ANCIENT_SAPLING =
            MyriadBloom.BLOCKS.register("ancient_sapling",
            () -> new com.myriadbloom.world.block.AncientSaplingBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .noCollission()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                            .offsetType(BlockBehaviour.OffsetType.XZ)));

    public static final DeferredItem<BlockItem> ANCIENT_SAPLING_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ancient_sapling", ANCIENT_SAPLING);

    /** 冰台阶 (ice step): the stair block used by the maze entrances. */
    public static final DeferredBlock<com.myriadbloom.world.block.IceStepBlock> ICE_STEP =
            MyriadBloom.BLOCKS.register("ice_step",
                    () -> new com.myriadbloom.world.block.IceStepBlock(
                            net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.ICE)
                                    .strength(0.6F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((state, level, pos, type) -> false)
                                    .isRedstoneConductor((state, level, pos) -> false)
                                    .isSuffocating((state, level, pos) -> false)
                                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> ICE_STEP_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ice_step", ICE_STEP);

    /** 风暴棱镜的召唤核心，摆在中央 BOSS 厅正中。 */
    public static final DeferredBlock<com.myriadbloom.world.block.StormPrismSpawnerBlock>
            STORM_PRISM_SPAWNER = MyriadBloom.BLOCKS.register("storm_prism_spawner",
                    () -> new com.myriadbloom.world.block.StormPrismSpawnerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_CYAN)
                                    .strength(3.0F, 1200.0F)
                                    .lightLevel(state -> 15)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> STORM_PRISM_SPAWNER_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("storm_prism_spawner", STORM_PRISM_SPAWNER);

    /** 永恒之冰：几乎透明的冰，无法破坏。 */
    public static final DeferredBlock<com.myriadbloom.world.block.EternalIceBlock> ETERNAL_ICE =
            MyriadBloom.BLOCKS.register("eternal_ice",
                    () -> new com.myriadbloom.world.block.EternalIceBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.ICE)
                                    .strength(-1.0F, 3600000.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((state, level, pos, type) -> false)
                                    .isRedstoneConductor((state, level, pos) -> false)
                                    .isSuffocating((state, level, pos) -> false)
                                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> ETERNAL_ICE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("eternal_ice", ETERNAL_ICE);

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(ANCIENT_WOOD_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ANCIENT_LEAVES_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ANCIENT_SAPLING_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ICE_STEP_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(STORM_PRISM_SPAWNER_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ETERNAL_ICE_ITEM);
    }

    private StormWoodlandContent() {
    }
}
