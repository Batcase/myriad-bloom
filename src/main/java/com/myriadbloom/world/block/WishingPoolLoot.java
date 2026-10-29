package com.myriadbloom.world.block;

import com.myriadbloom.EmberContent;
import com.myriadbloom.ModItems;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Reward tables of the wishing pool.
 *
 * <p>Each offering tier has its own list, ordered from the rarest prize to the most common one. A wish
 * always spits out three of them, picked with weights that shrink quickly towards the top of the list,
 * so a diamond has a real but small chance of handing out the snow pickaxe or a netherite ingot.</p>
 *
 * <p>Time sand is deliberately <b>not</b> here: it only comes from the origin ape's chest, because it is
 * the material for the late game hourglass.</p>
 */
public final class WishingPoolLoot {
    public static final int PICKS_PER_WISH = 3;

    /** One entry of a tier: the prize and its relative weight (lower = rarer). */
    private record Entry(Supplier<ItemStack> stack, int weight) {
    }

    private WishingPoolLoot() {
    }

    private static Entry of(ItemStack stack, int weight) {
        return new Entry(() -> stack.copy(), weight);
    }

    private static Entry of(net.minecraft.world.item.Item item, int count, int weight) {
        return of(new ItemStack(item, count), weight);
    }

    private static Entry of(java.util.function.Supplier<ItemStack> supplier, int weight) {
        return new Entry(supplier, weight);
    }

    private static Supplier<ItemStack> book(ServerLevel level, int enchantmentLevel) {
        return () -> {
            ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var holder = enchantments.getOrThrow(
                    ResourceKey.create(Registries.ENCHANTMENT, MyriadBloomApi.id("heart_of_fire")));
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mutable.set(holder, enchantmentLevel);
            stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
            return stack;
        };
    }

    /** Random piece of the frostbound armour set. */
    private static Supplier<ItemStack> frostArmour(RandomSource random) {
        int pick = random.nextInt(4);
        return switch (pick) {
            case 0 -> () -> new ItemStack(EmberContent.FROST_HELMET.get());
            case 1 -> () -> new ItemStack(EmberContent.FROST_CHESTPLATE.get());
            case 2 -> () -> new ItemStack(EmberContent.FROST_LEGGINGS.get());
            default -> () -> new ItemStack(EmberContent.FROST_BOOTS.get());
        };
    }

    /** Ordered prize list of one tier; 0 is the rarest. */
    private static List<Entry> tier(ServerLevel level, int rarity, RandomSource random) {
        List<Entry> list = new ArrayList<>();
        switch (rarity) {
            case 0 -> {   // 钻石
                list.add(of(() -> new ItemStack(EmberContent.FROST_PICKAXE.get()), 1));
                list.add(of(Items.ENCHANTED_GOLDEN_APPLE, 1, 2));
                list.add(of(Items.NETHERITE_INGOT, 1, 3));
                list.add(of(frostArmour(random), 4));
                list.add(of(book(level, 4), 6));
                list.add(of(book(level, 3), 9));
                list.add(of(Items.DIAMOND, 2, 14));
                list.add(of(Items.EMERALD, 4, 20));
                list.add(of(ModItems.FROST_INGOT.get(), 3, 26));
                list.add(of(Items.GOLD_INGOT, 6, 34));
                list.add(of(Items.REDSTONE, 8, 44));
                list.add(of(Items.BAKED_POTATO, 6, 54));
            }
            case 1 -> {   // 燃晶
                list.add(of(Items.ENCHANTED_GOLDEN_APPLE, 1, 1));
                list.add(of(Items.NETHERITE_INGOT, 1, 2));
                list.add(of(book(level, 3), 5));
                list.add(of(Items.DIAMOND, 1, 10));
                list.add(of(Items.EMERALD, 3, 18));
                list.add(of(ModItems.FROST_INGOT.get(), 2, 26));
                list.add(of(Items.GOLD_INGOT, 4, 34));
                list.add(of(Items.REDSTONE, 8, 44));
                list.add(of(Items.BAKED_POTATO, 5, 54));
            }
            case 2 -> {   // 青金石
                list.add(of(Items.NETHERITE_INGOT, 1, 1));
                list.add(of(book(level, 2), 4));
                list.add(of(Items.DIAMOND, 1, 10));
                list.add(of(Items.EMERALD, 2, 18));
                list.add(of(ModItems.FROST_INGOT.get(), 2, 28));
                list.add(of(Items.GOLD_INGOT, 3, 36));
                list.add(of(Items.REDSTONE, 6, 46));
                list.add(of(Items.BAKED_POTATO, 4, 56));
            }
            case 3 -> {   // 金锭
                list.add(of(book(level, 1), 3));
                list.add(of(Items.DIAMOND, 1, 8));
                list.add(of(Items.EMERALD, 2, 16));
                list.add(of(ModItems.FROST_INGOT.get(), 2, 26));
                list.add(of(Items.GOLD_INGOT, 3, 36));
                list.add(of(Items.REDSTONE, 6, 46));
                list.add(of(Items.BAKED_POTATO, 4, 56));
            }
            case 4 -> {   // 霜锭
                list.add(of(Items.EMERALD, 1, 6));
                list.add(of(ModItems.FROST_INGOT.get(), 2, 18));
                list.add(of(Items.GOLD_INGOT, 2, 28));
                list.add(of(Items.REDSTONE, 4, 40));
                list.add(of(Items.BAKED_POTATO, 3, 50));
                list.add(of(Items.IRON_INGOT, 3, 60));
            }
            default -> {  // 铁锭
                list.add(of(Items.GOLD_INGOT, 1, 10));
                list.add(of(Items.REDSTONE, 2, 24));
                list.add(of(Items.BAKED_POTATO, 2, 36));
                list.add(of(Items.IRON_INGOT, 2, 48));
                list.add(of(Items.BREAD, 2, 58));
            }
        }
        return list;
    }

    /** Rolls the three prizes of one wish. */
    public static List<ItemStack> roll(ServerLevel level, int rarity, RandomSource random) {
        List<Entry> pool = tier(level, rarity, random);
        List<ItemStack> result = new ArrayList<>(PICKS_PER_WISH);
        for (int pick = 0; pick < PICKS_PER_WISH && !pool.isEmpty(); pick++) {
            int total = 0;
            for (Entry entry : pool) {
                total += entry.weight();
            }
            int roll = random.nextInt(total);
            int index = 0;
            for (int i = 0; i < pool.size(); i++) {
                roll -= pool.get(i).weight();
                if (roll < 0) {
                    index = i;
                    break;
                }
            }
            Entry chosen = pool.remove(index);
            result.add(chosen.stack().get());
        }
        return result;
    }
}
