package com.myriadbloom.world;

import com.myriadbloom.CandyContent;
import com.myriadbloom.FrostConfig;
import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Thirst: the candy dimension's survival stat.
 *
 * <p>Thirst drains while the player is in the candy realm; at zero the player takes poison and magic
 * damage. Drinking or eating sweet things refills it, while raw candy makes it worse.</p>
 */
public final class ThirstMechanic implements EnvironmentMechanic {
    public static final int MAX_THIRST = 20;

    private static final ResourceLocation ID = MyriadBloomApi.id("thirst");
    private static final int DAMAGE_INTERVAL = 40;

    private final Map<UUID, Integer> timers = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** Drops the cached drink timer of a player that logged out. */
    @Override
    public void forgetPlayer(UUID playerId) {
        this.timers.remove(playerId);
    }

    @Override
    public String debugState() {
        return "thirst.timers=" + this.timers.size();
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!FrostConfig.THIRST_ENABLED.get() || !MyriadBloomApi.isCandy(player.serverLevel())) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            setThirst(player, MAX_THIRST);
            return;
        }
        int thirst = player.getData(ModAttachments.THIRST.get());
        int interval = Math.max(MAX_THIRST, FrostConfig.THIRST_TICKS_PER_UNIT.get());
        int timer = this.timers.merge(player.getUUID(), 1, Integer::sum);
        if (timer >= interval) {
            this.timers.put(player.getUUID(), 0);
            if (thirst > 0) {
                thirst--;
                setThirst(player, thirst);
            }
        }
        if (thirst <= 0 && player.tickCount % DAMAGE_INTERVAL == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true, true));
            player.hurt(player.damageSources().magic(), 1.0F);
        }
    }

    /** Refills (or drains) thirst when the player finishes a drink or a snack. */
    public static void onConsumed(ServerPlayer player, ItemStack stack) {
        if (!FrostConfig.THIRST_ENABLED.get() || !MyriadBloomApi.isCandy(player.serverLevel())) {
            return;
        }
        int thirst = player.getData(ModAttachments.THIRST.get());
        if (stack.is(CandyContent.CANDY.get()) || stack.is(CandyContent.CANDY_CORN.get())) {
            // raw candy makes the thirst worse
            setThirst(player, thirst - 1);
            return;
        }
        int amount = restoreAmount(stack);
        if (amount <= 0) {
            return;
        }
        setThirst(player, Math.min(MAX_THIRST, thirst + amount));
    }

    private static int restoreAmount(ItemStack stack) {
        if (stack.is(Items.POTION)) {
            return 8;
        }
        if (stack.is(Items.APPLE)) {
            return 2;
        }
        if (stack.is(Items.BEETROOT_SOUP) || stack.is(Items.MUSHROOM_STEW)) {
            return 4;
        }
        return 0;
    }

    private static void setThirst(ServerPlayer player, int value) {
        int clamped = Math.max(0, Math.min(MAX_THIRST, value));
        if (player.getData(ModAttachments.THIRST.get()) != clamped) {
            player.setData(ModAttachments.THIRST.get(), clamped);
        }
    }
}
