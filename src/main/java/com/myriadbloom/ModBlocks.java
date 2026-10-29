package com.myriadbloom;

import com.myriadbloom.world.block.CandyBlock;
import com.myriadbloom.world.block.CandyPortalBlock;
import com.myriadbloom.world.block.CandyRockBlock;
import com.myriadbloom.world.block.FrostPortalBlock;
import com.myriadbloom.world.block.FrostRockBlock;
import com.myriadbloom.world.block.JellyBlock;
import com.myriadbloom.world.block.SugarFrostBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Block registry of Frost Realm.
 *
 * <p>Content is grouped by dimension: the frost portal and its frame materials first, then the candy
 * dimension's blocks and the syrup fluid block. Every block that should be obtainable also gets a
 * {@link BlockItem} and is added to the creative tab.</p>
 */
public final class ModBlocks {

    /** 极冰聚合物召唤器: stone pedestal with a recess that accepts the origin ape trophy. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.myriadbloom.world.block.BossSummonerBlock>
            BOSS_SUMMONER = MyriadBloom.BLOCKS.register("boss_summoner",
            () -> new com.myriadbloom.world.block.BossSummonerBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.STONE)
                            .strength(4.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .sound(net.minecraft.world.level.block.SoundType.STONE)
                            .noOcclusion()));

    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem>
            BOSS_SUMMONER_ITEM = MyriadBloom.ITEMS.registerSimpleBlockItem("boss_summoner", BOSS_SUMMONER);
    // ------------------------------------------------------------------ frost realm

    /**
     * Portal surface block. Breakable, but drops nothing: mining it dismantles the portal, the rest
     * of the surface collapses and the frost rock frame reverts to snow.
     */
    public static final DeferredBlock<FrostPortalBlock> FROST_PORTAL = MyriadBloom.BLOCKS.register(
            "frost_portal",
            () -> new FrostPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .randomTicks()
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /**
     * Creative-only item so the portal surface can be placed by hand while testing.
     * It has no recipe, so it cannot be obtained in survival.
     */
    public static final DeferredItem<BlockItem> FROST_PORTAL_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_portal", FROST_PORTAL);

    /**
     * Frost ore: the realm's only resource ore. Needs an iron pickaxe or better
     * ({@code minecraft:needs_iron_tool}); smelts into a frost ingot.
     */
    public static final DeferredBlock<Block> FROST_ORE = MyriadBloom.BLOCKS.register(
            "frost_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> FROST_ORE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_ore", FROST_ORE);

    /**
     * Frost rock: what a lit portal's frame turns into. Blue obsidian, unbreakable
     * ({@code strength(-1, 3600000)} is the bedrock/barrier setup) and immune to the dragon and
     * wither through the vanilla block tags.
     */
    public static final DeferredBlock<Block> FROST_ROCK = MyriadBloom.BLOCKS.register(
            "frost_rock",
            () -> new FrostRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0F, 3600000.0F)
                    .randomTicks()
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> FROST_ROCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_rock", FROST_ROCK);

    // ------------------------------------------------------------------ candy dimension

    /**
     * Frost honey: orange candy wrapped in ice, and the frame material of the candy portal. Not
     * edible - the edible candy items are separate. Breakable, so an unlit frame can be taken apart.
     */
    public static final DeferredBlock<Block> FROST_HONEY = MyriadBloom.BLOCKS.register(
            "frost_honey",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.AMETHYST)));

    public static final DeferredItem<BlockItem> FROST_HONEY_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("frost_honey", FROST_HONEY);

    /**
     * Candy portal frame: what a lit candy portal's ring turns into. Unbreakable like frost rock, and
     * it reverts to frost honey as soon as no lit portal is left around it.
     */
    public static final DeferredBlock<CandyRockBlock> CANDY_ROCK = MyriadBloom.BLOCKS.register(
            "candy_rock",
            () -> new CandyRockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(-1.0F, 3600000.0F)
                    .randomTicks()
                    .sound(SoundType.AMETHYST)));

    public static final DeferredItem<BlockItem> CANDY_ROCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_rock", CANDY_ROCK);

    /**
     * The candy portal surface: a vertical 2x3 sheet with animated pink/white stripes. Like the frost
     * pool sheet it is breakable and drops nothing, which is how a candy portal is dismantled: break
     * the sheet, the candy rock frame reverts to frost honey and can be collected.
     */
    public static final DeferredBlock<CandyPortalBlock> CANDY_PORTAL = MyriadBloom.BLOCKS.register(
            "candy_portal",
            () -> new CandyPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .randomTicks()
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<BlockItem> CANDY_PORTAL_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_portal", CANDY_PORTAL);

    /** Sugar frost: white, sand-like, obeys gravity. The candy dimension's floor material. */
    public static final DeferredBlock<SugarFrostBlock> SUGAR_FROST = MyriadBloom.BLOCKS.register(
            "sugar_frost",
            () -> new SugarFrostBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F)
                    .sound(SoundType.SAND)));

    public static final DeferredItem<BlockItem> SUGAR_FROST_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("sugar_frost", SUGAR_FROST);

    /** Hard candy in sixteen colours; the candy dimension's stone replacement. */
    public static final DeferredBlock<CandyBlock> CANDY_BLOCK = MyriadBloom.BLOCKS.register(
            "candy_block",
            () -> new CandyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.AMETHYST)));

    public static final DeferredItem<BlockItem> CANDY_BLOCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("candy_block", CANDY_BLOCK);

    /** Jelly in sixteen colours, translucent; the candy dimension's grass/dirt replacement. */
    public static final DeferredBlock<JellyBlock> JELLY_BLOCK = MyriadBloom.BLOCKS.register(
            "jelly_block",
            () -> new JellyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.8F)
                    .friction(0.8F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> JELLY_BLOCK_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("jelly_block", JELLY_BLOCK);

    /**
     * The syrup fluid block. It has no item of its own (like water): it is placed and collected with
     * {@code myriadbloom:syrup_bucket}. The block state is invisible - the fluid is drawn by the
     * fluid renderer from {@link ModFluids#SYRUP_TYPE}.
     */
    public static final DeferredBlock<LiquidBlock> SYRUP = MyriadBloom.BLOCKS.register(
            "syrup",
            () -> new LiquidBlock(ModFluids.SYRUP_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    static {
        // Show the blocks in the creative tab (the syrup fluid block is deliberately excluded).
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_PORTAL_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_ORE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_ROCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_HONEY_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_ROCK_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(CANDY_PORTAL_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(SUGAR_FROST_ITEM);
        // candy_block / jelly_block are legacy single-block-with-colour-state versions: they stay
        // registered for old worlds but the tab offers the per-colour blocks from CandyBlocks instead
    }

    /** Forces static initialisation. Called from the mod constructor. */
    public static void bootstrap() {
        // Touching the class is enough; holders above are the registration.
    }

    private ModBlocks() {
    }
}
