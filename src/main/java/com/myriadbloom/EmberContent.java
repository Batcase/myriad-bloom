package com.myriadbloom;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 燃晶 content: the ember ore, the ember crystal, the ember core and the 极霜 (frost) armour set.
 *
 * <ul>
 *   <li>the ore is a slow heat source (+1 warmth per 40 s) and needs an iron pickaxe</li>
 *   <li>the crystal works as a heat source while held in either hand, same slow rate</li>
 *   <li>four crystals craft an ember core, which is the centre piece of every armour recipe</li>
 *   <li>the armour sits between iron and diamond: defence 3/7/6/3 and a durability multiplier of 20</li>
 * </ul>
 */
public final class EmberContent {
    public static final DeferredBlock<Block> EMBER_ORE = MyriadBloom.BLOCKS.register(
            "ember_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredItem<BlockItem> EMBER_ORE_ITEM =
            MyriadBloom.ITEMS.registerSimpleBlockItem("ember_ore", EMBER_ORE);

    /** 燃晶: a burning orange shard, only obtainable by blasting the ore. */
    public static final DeferredItem<Item> EMBER_CRYSTAL = MyriadBloom.ITEMS.register(
            "ember_crystal",
            () -> new Item(new Item.Properties()));

    /** 燃晶核心: steel-wrapped core, crafted from four crystals, centre of every armour piece. */
    public static final DeferredItem<Item> EMBER_CORE = MyriadBloom.ITEMS.register(
            "ember_core",
            () -> new Item(new Item.Properties()));

    /** Armour material registry. */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MyriadBloom.MODID);

    /**
     * 极霜 armour material: a bit tougher than iron, a bit more durable, still under diamond.
     * Defence 3/7/6/3 (iron 2/6/5/2, diamond 3/8/6/3), durability multiplier 20 (iron 15, diamond 33).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FROST_ARMOR_MATERIAL =
            ARMOR_MATERIALS.register("frost_armor", () -> new ArmorMaterial(
                    defence(),
                    12,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.FROST_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MyriadBloom.MODID, "frost_armor"))),
                    0.5F,
                    0.0F));

    public static final DeferredItem<ArmorItem> FROST_HELMET = armor("frost_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> FROST_CHESTPLATE = armor("frost_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> FROST_LEGGINGS = armor("frost_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> FROST_BOOTS = armor("frost_boots", ArmorItem.Type.BOOTS);

    private static Map<ArmorItem.Type, Integer> defence() {
        Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, 3);
        map.put(ArmorItem.Type.CHESTPLATE, 7);
        map.put(ArmorItem.Type.LEGGINGS, 6);
        map.put(ArmorItem.Type.BOOTS, 3);
        map.put(ArmorItem.Type.BODY, 9);
        return map;
    }

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return MyriadBloom.ITEMS.register(name, () -> new ArmorItem(FROST_ARMOR_MATERIAL, type,
                new Item.Properties().durability(type.getDurability(20))));
    }

    /** 覆雪矿镐: mines congealed ice as if it were stone. */
    public static final DeferredItem<net.minecraft.world.item.PickaxeItem> FROST_PICKAXE = MyriadBloom.ITEMS.register(
            "frost_pickaxe",
            () -> new net.minecraft.world.item.PickaxeItem(net.minecraft.world.item.Tiers.DIAMOND,
                    new Item.Properties().attributes(net.minecraft.world.item.PickaxeItem.createAttributes(
                            net.minecraft.world.item.Tiers.DIAMOND, 1.0F, -2.8F))));

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(EMBER_ORE_ITEM);
        ModItems.CREATIVE_TAB_ITEMS.add(EMBER_CRYSTAL);
        ModItems.CREATIVE_TAB_ITEMS.add(EMBER_CORE);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_HELMET);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_CHESTPLATE);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_LEGGINGS);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_BOOTS);
        ModItems.CREATIVE_TAB_ITEMS.add(FROST_PICKAXE);
    }

    private EmberContent() {
    }
}
