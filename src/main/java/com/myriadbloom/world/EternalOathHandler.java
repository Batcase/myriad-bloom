package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 【永不磨灭的誓言】：背包里带着骑士之誓时死亡，增益效果不会消失，倒计时继续。
 */
@EventBusSubscriber(modid = MyriadBloom.MODID)
public final class EternalOathHandler {
    private static final Map<UUID, List<MobEffectInstance>> STASH = new ConcurrentHashMap<>();
    /** 死亡时被誓言保下来的物品。 */
    private static final Map<UUID, List<ItemStack>> KEPT_ITEMS = new ConcurrentHashMap<>();

    private EternalOathHandler() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!carriesOath(player)) {
            return;
        }
        // 带誓言的物品不会因为死亡掉落：从背包里取出暂存，复活时放回
        var oathHolder = oathHolder(player);
        if (oathHolder == null) {
            return;
        }
        List<ItemStack> kept = new java.util.ArrayList<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getEnchantmentLevel(oathHolder) > 0) {
                kept.add(stack.copy());
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (!kept.isEmpty()) {
            KEPT_ITEMS.put(player.getUUID(), kept);
        }

        // 只保留增益效果（原版 isBeneficial 判定），负面效果照常被死亡清除
        List<MobEffectInstance> beneficial = player.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().isBeneficial())
                .toList();
        if (beneficial.isEmpty()) {
            return;
        }
        STASH.put(player.getUUID(), beneficial);
        MyriadBloom.LOGGER.info("Eternal oath kept {} beneficial effects for {}",
                beneficial.size(), player.getGameProfile().getName());


    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        List<ItemStack> keptItems = KEPT_ITEMS.remove(event.getOriginal().getUUID());
        if (keptItems != null && !keptItems.isEmpty()) {
            for (ItemStack stack : keptItems) {
                if (!event.getEntity().getInventory().add(stack)) {
                    event.getEntity().drop(stack, false);
                }
            }
        }

        List<MobEffectInstance> kept = STASH.remove(event.getOriginal().getUUID());
        if (kept == null) {
            return;
        }
        for (MobEffectInstance effect : kept) {
            if (effect.getEffect().value().isBeneficial()) {
                event.getEntity().addEffect(new MobEffectInstance(effect));
            }
        }
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> oathHolder(
            Player player) {
        return player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(com.myriadbloom.api.MyriadBloomApi.id("eternal_oath")).orElse(null);
    }

    private static boolean carriesOath(Player player) {
        var holder = oathHolder(player);
        if (holder == null) {
            return false;
        }
        for (var stack : player.getInventory().items) {
            if (stack.getEnchantmentLevel(holder) > 0) {
                return true;
            }
        }
        return false;
    }
}
