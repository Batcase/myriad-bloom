package com.myriadbloom.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 冰霜之钥：开启风暴林地迷宫入口的锁孔。 */
public class FrostKeyItem extends Item {
    public FrostKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.myriadbloom.frost_key.desc")
                .withStyle(ChatFormatting.AQUA));
    }
}
