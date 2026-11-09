package com.myriadbloom.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/** 骑士之誓的材质等级：耐久略高于铁，其余按铁级。 */
public final class KnightOathTier implements Tier {
    public static final KnightOathTier INSTANCE = new KnightOathTier();

    private KnightOathTier() {
    }

    @Override
    public int getUses() {
        return 320;
    }

    @Override
    public float getSpeed() {
        return Tiers.IRON.getSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_IRON_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.IRON.getEnchantmentValue();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("myriadbloom", "frost_ingot")));
    }
}
