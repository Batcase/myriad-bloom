package com.myriadbloom.world.block;

import com.myriadbloom.BossContent;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Inventory of the giant chest.
 *
 * <p>A normal 27 slot container that supports loot tables, exactly like a vanilla chest: the boss puts
 * its loot table on the chest when it dies, and the table is rolled the first time somebody opens it.
 * The GUI is the vanilla chest screen (three rows), so no custom screen code is needed.</p>
 */
public class GiantChestBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SLOTS = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public GiantChestBlockEntity(BlockPos pos, BlockState state) {
        super(BossContent.GIANT_CHEST_ENTITY.get(), pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.myriadbloom.giant_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        // unpackLootTable is a no-op once the table has been rolled
        if (!player.isSpectator()) {
            this.unpackLootTable(player);
        }
        super.startOpen(player);
    }

    /** Vanilla chests use the same menu type for three rows; kept here so the container is explicit. */
    public static MenuType<?> menuType() {
        return MenuType.GENERIC_9x3;
    }

    /** Convenience so callers do not need the Container cast. */
    public Container asContainer() {
        return this;
    }
}
