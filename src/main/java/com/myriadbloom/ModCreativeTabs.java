package com.myriadbloom;

import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Creative tabs. The main tab is filled from {@link ModItems#CREATIVE_TAB_ITEMS}, so new items show
 * up automatically once registered through {@link ModItems#register}; enchanted books for the mod's
 * own enchantments are appended explicitly.
 */
public final class ModCreativeTabs {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = MyriadBloom.CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.myriadbloom.main"))
                    .icon(() -> new ItemStack(Blocks.SNOW_BLOCK))
                    .displayItems((parameters, output) -> {
                        for (var supplier : ModItems.CREATIVE_TAB_ITEMS) {
                            Item item = supplier.get();
                            output.accept(item);
                        }
                        for (int level = 1; level <= 5; level++) {
                            output.accept(heartOfFireBook(parameters.holders(), level));
                        }
                    })
                    .build());

    /**
     * Enchanted book carrying {@code myriadbloom:heart_of_fire} at the given level, so the book can
     * be pulled straight out of the creative inventory.
     */
    public static ItemStack heartOfFireBook(HolderLookup.Provider registries, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, MyriadBloomApi.id("heart_of_fire"));
        registries.lookup(Registries.ENCHANTMENT)
                .flatMap(lookup -> lookup.get(key))
                .ifPresent(holder -> {
                    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    mutable.set(holder, level);
                    stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                });
        return stack;
    }

    public static void bootstrap() {
    }

    private ModCreativeTabs() {
    }
}
