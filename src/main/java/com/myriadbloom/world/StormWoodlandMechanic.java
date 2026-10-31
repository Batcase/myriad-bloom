package com.myriadbloom.world;

import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.entity.SnowRabbit;
import com.myriadbloom.world.gen.StormCross;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rules of the 风暴林地 (storm woodland).
 *
 * <p>The forest itself is hostile to trespassers: anyone who has not yet killed the Glacial
 * Conglomeration is blinded and slowly frozen while inside it. Leaving the biome lifts both effects
 * immediately. The same mechanic drives the snow rabbit riding on a player's head: while the synced
 * timer runs the player keeps a small jump boost and slow falling, and when it expires the rabbit
 * jumps off and turns wild again.</p>
 */
public final class StormWoodlandMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("storm_woodland");
    private static final ResourceLocation STORM_WOODLAND =
            ResourceLocation.fromNamespaceAndPath(com.myriadbloom.MyriadBloom.MODID, "frost_storm_woodland");
    /** Damage interval while the player is trespassing, in ticks. */
    private static final int DAMAGE_INTERVAL = 40;
    /** Players currently suffering the forest's curse, so the effects can be cleared on leaving. */
    private final Set<UUID> cursed = ConcurrentHashMap.newKeySet();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        tickCarriedRabbit(player);
        if (!MyriadBloomApi.isMyriadBloom(player.serverLevel())) {
            return;
        }
        boolean inside = insideStormWoodland(player);
        if (!inside) {
            if (this.cursed.remove(player.getUUID())) {
                // leaving the biome lifts the curse at once
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            return;
        }
        if (hasSlainBoss(player)) {
            this.cursed.remove(player.getUUID());
            return;
        }
        this.cursed.add(player.getUUID());
        // short durations, refreshed every tick: the moment the player steps out they fade away
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
        if (player.tickCount % DAMAGE_INTERVAL == 0) {
            player.hurt(player.damageSources().freeze(), 1.0F);
        }
    }

    private static boolean insideStormWoodland(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.location().equals(STORM_WOODLAND))
                .orElse(false);
    }

    /** The storm woodland only tolerates players who already defeated the Glacial Conglomeration. */
    private static boolean hasSlainBoss(ServerPlayer player) {
        var holder = com.myriadbloom.world.AdvancementAward.holder(player, "giant");
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void tickCarriedRabbit(ServerPlayer player) {
        int ticks = player.getData(ModAttachments.SNOW_RABBIT_CARRY.get());
        if (ticks <= 0) {
            return;
        }
        if (ticks % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
        }
        int left = ticks - 1;
        if (left <= 0) {
            player.setData(ModAttachments.SNOW_RABBIT_CARRY.get(), 0);
            player.removeEffect(MobEffects.JUMP);
            player.removeEffect(MobEffects.SLOW_FALLING);
            if (player.level() instanceof ServerLevel server) {
                SnowRabbit.releaseNear(server, player);
            }
        } else {
            player.setData(ModAttachments.SNOW_RABBIT_CARRY.get(), left);
        }
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        this.cursed.remove(playerId);
    }

    @Override
    public String debugState() {
        return "storm_woodland.cursed=" + this.cursed.size();
    }
}
