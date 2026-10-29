package com.myriadbloom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 雪兽 items: the raw cut and the cooked cut.
 *
 * <p>The cooked cut restores satiety <b>and</b> a couple of warmth flames, which is what makes it worth
 * carrying in the frost realm. Warmth lives in a data attachment, so the bonus is applied when the
 * eating action finishes server side.</p>
 */
public final class SnowBeastContent {
    /** Satiety restored by the cooked cut, in hunger points (2 per drumstick icon). */
    public static final int COOKED_NUTRITION = 4;

    public static final net.neoforged.neoforge.registries.DeferredItem<Item> RAW_MEAT =
            MyriadBloom.ITEMS.register("snow_beast_meat",
                    () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(1).saturationModifier(0.1F).build())));

    public static final net.neoforged.neoforge.registries.DeferredItem<WarmthFoodItem> COOKED_MEAT =
            MyriadBloom.ITEMS.register("snow_beast_cooked_meat",
                    () -> new WarmthFoodItem(new Item.Properties().food(new FoodProperties.Builder()
                            .nutrition(COOKED_NUTRITION).saturationModifier(0.6F).build()), 2));

    public static void bootstrap() {
        ModItems.CREATIVE_TAB_ITEMS.add(RAW_MEAT);
        ModItems.CREATIVE_TAB_ITEMS.add(COOKED_MEAT);
    }

    private SnowBeastContent() {
    }

    /** Food that also warms the player up by {@code warmth} flames. */
    public static class WarmthFoodItem extends Item {
        private final int warmth;

        public WarmthFoodItem(Properties properties, int warmth) {
            super(properties);
            this.warmth = warmth;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
            ItemStack result = super.finishUsingItem(stack, level, living);
            if (!level.isClientSide && living instanceof Player player) {
                int current = player.getData(ModAttachments.WARMTH);
                int max = com.myriadbloom.world.WarmthMechanic.MAX_WARMTH;
                player.setData(ModAttachments.WARMTH, Math.min(max, current + this.warmth));
            }
            return result;
        }
    }
}
