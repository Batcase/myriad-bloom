package com.myriadbloom;

import com.myriadbloom.api.MyriadBloomApi;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Content of the snow fortress boss encounter: the boss spawner, the giant chest it leaves behind and
 * the 时之沙 item.
 */
public final class BossContent {
    /** Marker block the boss wakes up from; it destroys itself when the ape spawns. */
    public static final DeferredBlock<Block> BOSS_SPAWNER = MyriadBloom.BLOCKS.register(
            "boss_spawner",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> BOSS_SPAWNER_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("boss_spawner", BOSS_SPAWNER);

    /** 巨木箱子: giant wood chest with a gold lock, a real 27 slot container like a chest. */
    public static final DeferredBlock<com.myriadbloom.world.block.GiantChestBlock> GIANT_CHEST =
            MyriadBloom.BLOCKS.register(
                    "giant_chest",
                    () -> new com.myriadbloom.world.block.GiantChestBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PODZOL)
                            .strength(2.5F)
                            .sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> GIANT_CHEST_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("giant_chest", GIANT_CHEST);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MyriadBloom.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<com.myriadbloom.world.block.GiantChestBlockEntity>>
            GIANT_CHEST_ENTITY = BLOCK_ENTITIES.register("giant_chest",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(com.myriadbloom.world.block.GiantChestBlockEntity::new, GIANT_CHEST.get())
                            .build(null));

    /** 时之沙: pale blue dust, no use yet, orange name. */
    /** 冰原核心: the frozen core left by the glacial conglomeration. */
    public static final DeferredItem<net.minecraft.world.item.Item> ICE_CORE = MyriadBloom.ITEMS.register(
            "ice_core", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** 极冰战利品: the glacial head, wearable and placeable. */
    public static final DeferredItem<com.myriadbloom.item.GlacialTrophyItem> GLACIAL_TROPHY =
            MyriadBloom.ITEMS.register("glacial_trophy",
                    () -> new com.myriadbloom.item.GlacialTrophyItem(
                            new net.minecraft.world.item.Item.Properties()));

    /** 战利品摆件: the placed head of either boss. */
    public static final DeferredBlock<com.myriadbloom.world.block.TrophyBlock> TROPHY =
            MyriadBloom.BLOCKS.register("trophy",
                    () -> new com.myriadbloom.world.block.TrophyBlock(
                            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                    .strength(0.4F)
                                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> TROPHY_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("trophy", TROPHY);

    public static final net.neoforged.neoforge.registries.DeferredHolder<
            net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<
                    com.myriadbloom.world.block.TrophyBlockEntity>> TROPHY_ENTITY =
            BLOCK_ENTITIES.register("trophy",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(com.myriadbloom.world.block.TrophyBlockEntity::new, TROPHY.get())
                            .build(null));


    public static final DeferredItem<TimeSandItem> TIME_SAND = MyriadBloom.ITEMS.register(
            "time_sand", () -> new TimeSandItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    /** 始源人战利品: the ape's skull, worn like a vanilla head. */
    public static final DeferredItem<com.myriadbloom.item.OriginApeTrophyItem> ORIGIN_APE_TROPHY =
            MyriadBloom.ITEMS.register("origin_ape_trophy",
                    () -> new com.myriadbloom.item.OriginApeTrophyItem(new Item.Properties()));

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(BOSS_SPAWNER_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(GIANT_CHEST_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(TIME_SAND);
        ModItems.CREATIVE_TAB_ITEMS.add(ICE_CORE);
        ModItems.CREATIVE_TAB_ITEMS.add(GLACIAL_TROPHY);
        ModItems.CREATIVE_TAB_ITEMS.add(TROPHY_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(ORIGIN_APE_TROPHY);
    }

    private BossContent() {
    }

    /** 时之沙: orange name, no function yet. */
    public static class TimeSandItem extends Item {
        public TimeSandItem(Properties properties) {
            super(properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return super.getName(stack).copy().withStyle(ChatFormatting.GOLD);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                    TooltipFlag flag) {
            tooltip.add(Component.translatable("item.myriadbloom.time_sand.desc")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
