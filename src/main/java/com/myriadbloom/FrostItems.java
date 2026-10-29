package com.myriadbloom;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Gem-like items of the mod.
 *
 * <p>Kept in its own holder class so {@link ModItems} stays focused on the frost-metal line.</p>
 */
public final class FrostItems {
    /** Ice crystal: dropped by frenzied snow golems, rhombus shaped, sky blue. */
    public static final DeferredItem<Item> ICE_CRYSTAL = MyriadBloom.ITEMS.register("ice_crystal",
            () -> new Item(new Item.Properties()));

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(ICE_CRYSTAL);
    }

    private FrostItems() {
    }
}
