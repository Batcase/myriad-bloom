package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModAttachments;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;

/**
 * Warmth: the realm's survival stat.
 *
 * <ul>
 *   <li>standing within {@code heat_radius} blocks of a block tagged {@code #myriadbloom:heat_sources}
 *       refills one flame per second</li>
 *   <li>further away it cools down one flame every 40 seconds</li>
 *   <li>at 3 flames or less the player walks 50% slower</li>
 *   <li>at 0 flames the player takes freezing damage every 2 seconds, until death</li>
 *   <li>the {@code myriadbloom:heart_of_fire} armour enchantment slows the loss: 5% per level,
 *       summed over all four armour slots (100% = no loss at all)</li>
 * </ul>
 */
public final class WarmthMechanic implements EnvironmentMechanic {
    public static final int MAX_WARMTH = 10;
    /** Heat reserve cap, in percent. Only active while the full 极霜 armour set is worn. */
    public static final int MAX_HEAT = 100;
    /** The reserve refills this fast next to a real heat source (percent per second). */
    private static final int HEAT_REGEN_PER_SECOND = 20;
    /** ... and drains this slowly away from heat: 2% every 5 seconds. */
    private static final int HEAT_DRAIN_INTERVAL = 100;
    private static final int HEAT_DRAIN_AMOUNT = 2;
    public static final int SLOW_THRESHOLD = 3;

    private static final ResourceLocation ID = MyriadBloomApi.id("warmth");
    private static final ResourceLocation SLOW_ID = MyriadBloomApi.id("cold_slowdown");
    private static final AttributeModifier SLOW_MODIFIER =
            new AttributeModifier(SLOW_ID, -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, MyriadBloomApi.id("heat_sources"));
    private static final ResourceKey<Enchantment> HEART_OF_FIRE =
            ResourceKey.create(Registries.ENCHANTMENT, MyriadBloomApi.id("heart_of_fire"));

    /** The ember sources are slow: one flame every 40 seconds. */
    private static final int EMBER_TICKS = 800;
    private static final int SCAN_INTERVAL = 10;
    private static final int VERTICAL_SCAN = 3;
    private static final int DAMAGE_INTERVAL = 40;

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** Drops the cached per-player state: called when the player logs out, so it cannot pile up. */
    @Override
    public void forgetPlayer(UUID playerId) {
        this.states.remove(playerId);
    }

    @Override
    public String debugState() {
        return "warmth.states=" + this.states.size();
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!FrostConfig.WARMTH_ENABLED.get()) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            warmUp(player);
            return;
        }

        State state = this.states.computeIfAbsent(player.getUUID(), ignored -> new State());
        int warmth = player.getData(ModAttachments.WARMTH.get());

        // ---- advancement hooks: mod state the data driven triggers cannot read ----------
        if (warmth < 3) {
            // "迫近的寒意": the first time warmth falls into the danger zone
            AdvancementAward.award(player, "chill");
        }
        if (wearingAnyFrostArmour(player)) {
            // "温暖如春": wearing any piece of the frostbound armour
            AdvancementAward.award(player, "warm");
        }

        if (--state.scanTimer <= 0) {
            state.scanTimer = SCAN_INTERVAL;
            HeatScan scan = scanHeat(player);
            state.nearHeat = scan.fast();
            state.nearEmber = scan.ember() || holdsEmberCrystal(player);
            state.heatLevel = heartOfFireLevel(player);
        }

        double reduction = Math.min(1.0D, state.heatLevel * 0.05D);
        boolean fullSet = hasFullArmour(player);
        boolean congealed = player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getBiome(player.blockPosition())
                        .is(com.myriadbloom.world.CongealedLandMechanic.BIOME);
        int heat = player.getData(ModAttachments.HEAT.get());

        // ---- heat reserve: refills next to real heat, drains away from it ---------------
        if (fullSet) {
            boolean changed = false;
            if (state.nearHeat) {
                if (++state.regenTimer >= 20) {
                    state.regenTimer = 0;
                    if (heat < MAX_HEAT) {
                        heat = Math.min(MAX_HEAT, heat + HEAT_REGEN_PER_SECOND);
                        changed = true;
                    }
                }
            } else {
                state.regenTimer = 0;
                int heatInterval = congealed
                        ? HEAT_DRAIN_INTERVAL / com.myriadbloom.FrostConfig.CONGEALED_DRAIN_MULTIPLIER.get()
                        : HEAT_DRAIN_INTERVAL;
                if (heat > 0 && ++state.heatTimer >= heatInterval) {
                    state.heatTimer = 0;
                    heat = Math.max(0, heat - HEAT_DRAIN_AMOUNT);
                    changed = true;
                }
            }
            if (changed) {
                player.setData(ModAttachments.HEAT.get(), heat);
            }
        } else {
            state.regenTimer = 0;
            state.heatTimer = 0;
        }

        if (state.nearHeat) {
            if (++state.timer >= FrostConfig.WARMUP_TICKS.get()) {
                state.timer = 0;
                warmth = Math.min(MAX_WARMTH, warmth + 1);
                player.setData(ModAttachments.WARMTH.get(), warmth);
            }
        } else if (state.nearEmber) {
            // ember ore / ember crystal: one flame every EMBER_TICKS (40 s)
            if (++state.timer >= EMBER_TICKS) {
                state.timer = 0;
                warmth = Math.min(MAX_WARMTH, warmth + 1);
                player.setData(ModAttachments.WARMTH.get(), warmth);
            }
        } else if (fullSet && heat > 0) {
            // warmth is protected: the heat reserve pays instead
            state.timer = 0;
        } else if (reduction < 1.0D) {
            int interval = (int) Math.max(1L,
                    Math.round(FrostConfig.COOL_TICKS.get() / (1.0D - reduction)));
            if (fullSet) {
                // full frost armour: warmth drains at half speed
                interval *= 2;
            }
            if (player.hasEffect(com.myriadbloom.ModMobEffects.STORM_WIND)) {
                // 风暴棱镜的风暴：失温速度 +50%
                interval = Math.max(1, (int) Math.round(interval / 1.5D));
            }
            if (congealed) {
                // the congealed land drains warmth twice as fast
                interval = Math.max(1, interval / com.myriadbloom.FrostConfig.CONGEALED_DRAIN_MULTIPLIER.get());
            }
            if (++state.timer >= interval) {
                state.timer = 0;
                warmth = Math.max(0, warmth - 1);
                player.setData(ModAttachments.WARMTH.get(), warmth);
            }
        }

        applySlowdown(player, warmth <= SLOW_THRESHOLD);
        if (warmth <= 0 && player.tickCount % DAMAGE_INTERVAL == 0) {
            player.hurt(player.damageSources().freeze(), 1.0F);
        }
        if (warmth > 0 && state.heatLevel > 0) {
            // nothing extra: the enchantment only slows the loss
        }
    }

    private static void warmUp(ServerPlayer player) {
        if (player.getData(ModAttachments.WARMTH.get()) != MAX_WARMTH) {
            player.setData(ModAttachments.WARMTH.get(), MAX_WARMTH);
        }
    }

    private static void applySlowdown(ServerPlayer player, boolean slowed) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean present = speed.hasModifier(SLOW_ID);
        if (slowed && !present) {
            speed.addOrUpdateTransientModifier(SLOW_MODIFIER);
        } else if (!slowed && present) {
            speed.removeModifier(SLOW_ID);
        }
    }

    /** Total level of heart_of_fire across the four armour slots. */
    private static int heartOfFireLevel(ServerPlayer player) {
        Holder<Enchantment> holder = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(HEART_OF_FIRE)
                .orElse(null);
        return holder == null ? 0 : EnchantmentHelper.getEnchantmentLevel(holder, player);
    }

    /** Fast heat (campfire, lava, ...) or slow ember heat (ember ore). */
    private record HeatScan(boolean fast, boolean ember) {
    }

    private static HeatScan scanHeat(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int radius = FrostConfig.HEAT_RADIUS.get();
        int radiusSq = radius * radius;
        boolean ember = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -VERTICAL_SCAN; dy <= VERTICAL_SCAN; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(cursor).is(HEAT_SOURCES)) {
                        continue;
                    }
                    if (level.getBlockState(cursor).is(com.myriadbloom.EmberContent.EMBER_ORE.get())) {
                        ember = true; // slow source, needs 40 s
                    } else {
                        return new HeatScan(true, true); // a real fire wins
                    }
                }
            }
        }
        return new HeatScan(false, ember);
    }

    /** True when the player holds an ember crystal in either hand. */
    private static boolean holdsEmberCrystal(ServerPlayer player) {
        return player.getMainHandItem().is(com.myriadbloom.EmberContent.EMBER_CRYSTAL.get())
                || player.getOffhandItem().is(com.myriadbloom.EmberContent.EMBER_CRYSTAL.get());
    }

    /** True when all four armour slots are the frost armour pieces. */
    private static boolean hasFullArmour(ServerPlayer player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.myriadbloom.EmberContent.FROST_HELMET.get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(com.myriadbloom.EmberContent.FROST_CHESTPLATE.get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(com.myriadbloom.EmberContent.FROST_LEGGINGS.get())
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(com.myriadbloom.EmberContent.FROST_BOOTS.get());
    }

    /** True when at least one piece of the frostbound armour is worn (for the "温暖如春" advancement). */
    private static boolean wearingAnyFrostArmour(ServerPlayer player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.myriadbloom.EmberContent.FROST_HELMET.get())
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(com.myriadbloom.EmberContent.FROST_CHESTPLATE.get())
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(com.myriadbloom.EmberContent.FROST_LEGGINGS.get())
                || player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(com.myriadbloom.EmberContent.FROST_BOOTS.get());
    }

    private static final class State {
        private int timer;
        private int regenTimer;
        private int heatTimer;
        private int scanTimer;
        private boolean nearHeat;
        private boolean nearEmber;
        private int heatLevel;
    }
}
