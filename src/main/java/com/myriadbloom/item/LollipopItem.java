package com.myriadbloom.item;

import com.myriadbloom.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 棒棒糖 (lollipop): a candy on a yellow stick.
 *
 * <p>Eating it restores one hunger point, costs half a thirst unit (one half-flask) and grants
 * damage resistance for five seconds.</p>
 */
public class LollipopItem extends Item {
    private static final int THIRST_COST = 1;
    private static final int RESISTANCE_TICKS = 100;

    public LollipopItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof ServerPlayer player) {
            int thirst = player.getData(ModAttachments.THIRST.get());
            player.setData(ModAttachments.THIRST.get(), Math.max(0, thirst - THIRST_COST));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_TICKS, 0));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
        return result;
    }
}
