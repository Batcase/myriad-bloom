package com.myriadbloom.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.Holder;

/**
 * 始源人战利品 (origin ape trophy): the ape's skull, worn like a vanilla head.
 *
 * <p>Equippable in the head slot, a single stack, no lore, and the name is printed in yellow. The worn
 * model is a shrunken copy of the ape head rendered by {@code OriginApeTrophyLayer}.</p>
 */
public class OriginApeTrophyItem extends Item implements Equipable {
    public OriginApeTrophyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_TURTLE;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.YELLOW);
    }
}
