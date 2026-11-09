package com.myriadbloom.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/** 骑士之誓：细长的骑士剑，自带【永不磨灭的誓言】。 */
public class KnightOathItem extends SwordItem {
    public KnightOathItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.myriadbloom.knight_oath.desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("enchantment.myriadbloom.eternal_oath.desc")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        var frostIngot = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("myriadbloom", "frost_ingot"));
        return repair.is(frostIngot) || super.isValidRepairItem(stack, repair);
    }
}
