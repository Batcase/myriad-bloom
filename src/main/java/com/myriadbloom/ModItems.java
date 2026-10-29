package com.myriadbloom;

import com.myriadbloom.item.CandySyrupItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Item registry of Frost Realm.
 *
 * <p>New items go here (or in an addon using {@link com.myriadbloom.api.MyriadBloomApi#items()}).
 * Anything registered through {@link #register} is added to the mod's creative tab automatically,
 * which is why new content needs no changes to {@link ModCreativeTabs}.</p>
 */
public final class ModItems {
    /** Ordered list of items shown in the Frost Realm creative tab. */
    public static final List<Supplier<? extends Item>> CREATIVE_TAB_ITEMS = new ArrayList<>();

    /** Smelted from {@code myriadbloom:frost_ore}. */
    public static final DeferredItem<Item> FROST_INGOT =
            register("frost_ingot", () -> new Item(new Item.Properties()));

    /**
     * Candy syrup: the candy portal igniter. Bucket shaped and consumed on use; it is edible in
     * phase 2 of the spec, so for now it is a plain item with no food component.
     */
    public static final DeferredItem<CandySyrupItem> CANDY_SYRUP = register("candy_syrup",
            () -> new CandySyrupItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** Syrup bucket: places and picks up the candy dimension's fluid. */
    public static final DeferredItem<BucketItem> SYRUP_BUCKET = register("syrup_bucket",
            () -> new BucketItem(ModFluids.SYRUP_SOURCE.get(), new Item.Properties().stacksTo(1)));

    /** Registers an item and wires it into the creative tab. */
    public static <T extends Item> DeferredItem<T> register(String name, Supplier<? extends T> factory) {
        DeferredItem<T> holder = MyriadBloom.ITEMS.register(name, factory);
        CREATIVE_TAB_ITEMS.add(holder);
        return holder;
    }

    /** Forces static initialisation. Called from the mod constructor. */
    public static void bootstrap() {
        // Reserved for future content (crystals, lanterns, tools, ...).
    }

    private ModItems() {
    }
}
