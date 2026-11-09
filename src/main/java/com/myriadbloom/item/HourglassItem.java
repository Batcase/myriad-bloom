package com.myriadbloom.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 时之沙漏（未充能）：放到凝结之地中央冰山前的祭坛上，就会被拉进时之虚空。
 */
public class HourglassItem extends Item {
    private final boolean charged;

    public HourglassItem(Properties properties, boolean charged) {
        super(properties);
        this.charged = charged;
    }

    public boolean charged() {
        return this.charged;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable(this.charged
                        ? "item.myriadbloom.hourglass.charged.desc"
                        : "item.myriadbloom.hourglass.desc")
                .withStyle(this.charged ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }
}
