package com.myriadbloom.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 霜之哀伤：冰构成的长剑，附带【亡灵序曲】。 */
public class FrostmourneItem extends SwordItem {
    public FrostmourneItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.myriadbloom.frostmourne.desc")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("enchantment.myriadbloom.requiem.desc")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
