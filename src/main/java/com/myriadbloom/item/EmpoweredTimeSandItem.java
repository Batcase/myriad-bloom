package com.myriadbloom.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 赋能的时之沙：金色词条，时之沙的强化版。 */
public class EmpoweredTimeSandItem extends Item {
    public EmpoweredTimeSandItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.myriadbloom.empowered_time_sand")
                .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.myriadbloom.empowered_time_sand.desc")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
