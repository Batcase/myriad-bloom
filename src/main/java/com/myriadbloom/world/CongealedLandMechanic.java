package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biome;

/**
 * The 凝结之地 (congealed land) entry punishment.
 *
 * <p>Inside that biome the player is slowed by 50%, takes 0.5 damage per second and drains warmth and
 * heat twice as fast (the drain multiplier lives in {@link WarmthMechanic}).</p>
 *
 * <p><b>Entry condition hook:</b> {@link #meetsEntryRequirement(ServerPlayer)} is the placeholder for
 * the "if the player qualifies, no damage" rule - it returns {@code false} for now, so everybody takes
 * the damage until the rule is defined.</p>
 */
public class CongealedLandMechanic implements EnvironmentMechanic {
    public static final double DAMAGE_PER_SECOND = 0.5D;
    /** Fraction of movement speed removed. */
    public static final float SPEED_PENALTY = -0.5F;
    /** Drain multiplier applied to warmth and heat. */
    public static final int DRAIN_MULTIPLIER = 2;

    public static final ResourceKey<Biome> BIOME =
            ResourceKey.create(Registries.BIOME, MyriadBloomApi.id("frost_congealed"));
    private static final ResourceLocation SLOW_ID = MyriadBloomApi.id("congealed_slowdown");
    private static AttributeModifier slowModifier() {
        return new AttributeModifier(SLOW_ID, com.myriadbloom.FrostConfig.CONGEALED_SPEED_PENALTY.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("congealed_land");
    }

    /** True when the player is inside the congealed land. */
    public static boolean isInside(ServerPlayer player) {
        return player.serverLevel().getBiome(player.blockPosition()).is(BIOME);
    }

    /**
     * Entry condition placeholder: once the rule is defined this returns true for players that are
     * allowed to walk in without the damage (the slow and the doubled drain still apply).
     */
    public static boolean meetsEntryRequirement(ServerPlayer player) {
        // TODO(entry condition): fill in the real rule
        return false;
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!MyriadBloomApi.isMyriadBloom(player.serverLevel())) {
            return;
        }
        boolean inside = isInside(player);
        // environmental effects are survival only: creative and spectator keep their speed
        boolean affected = inside && !player.isCreative() && !player.isSpectator();
        applySlowdown(player, affected);
        if (!affected) {
            return;
        }
        if (!com.myriadbloom.FrostConfig.CONGEALED_PUNISHMENT_ENABLED.get()) {
            return;
        }
        if (!meetsEntryRequirement(player) && player.tickCount % 20 == 0) {
            player.hurt(player.damageSources().freeze(),
                    (float) (double) com.myriadbloom.FrostConfig.CONGEALED_DAMAGE_PER_SECOND.get());
        }
    }

    private static void applySlowdown(ServerPlayer player, boolean slowed) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean present = speed.hasModifier(SLOW_ID);
        if (slowed) {
            // rebuild the modifier so a config change takes effect immediately
            if (present) {
                speed.removeModifier(SLOW_ID);
            }
            speed.addOrUpdateTransientModifier(slowModifier());
        } else if (present) {
            speed.removeModifier(SLOW_ID);
        }
    }
}
