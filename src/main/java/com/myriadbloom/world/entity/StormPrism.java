package com.myriadbloom.world.entity;

import com.myriadbloom.ModMobEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 风暴棱镜 (storm prism): the third boss, waiting in the centre of the four frost mazes.
 *
 * <p>It floats instead of walking: it drifts towards its target for a while, then stops dead for four
 * seconds and casts one of three skills. It never melees - its body deals no damage at all.</p>
 *
 * <ul>
 *   <li><b>被动</b>: while it lives, a storm fills the hall: players get a random headed 20% slow and
 *       warmth drains 50% faster; every heat source in the room is shattered when it appears.</li>
 *   <li><b>技能 1 (70%)</b>: an ice block falls from the ceiling onto the target, 1.5 s later it hits
 *       for 8 damage.</li>
 *   <li><b>技能 2 (29%)</b>: a shove for 5 damage, doubled when the target is stopped by a wall.</li>
 *   <li><b>技能 3 (1%)</b>: everything nearby is frozen for 3 s, taking 2 damage per second and 5 at
 *       the end.</li>
 * </ul>
 */
public class StormPrism extends Monster {
    public static final float WIDTH = 6.0F;
    public static final float HEIGHT = 6.0F;
    /** How long it drifts towards the target before stopping. */
    private static final int MOVE_TICKS = 100;
    /** How long it stays still, casting. */
    private static final int STOP_TICKS = 80;
    /** When in the stop window the skill actually fires. */
    private static final int CAST_DELAY = 24;
    private static final float SKILL1_DAMAGE = 8.0F;
    private static final float SKILL2_DAMAGE = 5.0F;
    private static final float SKILL2_WALL_DAMAGE = 10.0F;
    private static final float SKILL3_TICK_DAMAGE = 2.0F;
    private static final float SKILL3_END_DAMAGE = 5.0F;
    private static final int SKILL3_TICKS = 60;
    private static final int STORM_RADIUS = 40;

    private enum Phase {
        MOVING,
        STOPPING
    }

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10)
            .setPlayBossMusic(false);

    private Phase phase = Phase.MOVING;
    private int phaseTicks;
    private boolean castDone;
    private boolean knockbackPending;
    private UUID knockbackTarget;
    private Vec3 knockbackStart = Vec3.ZERO;
    private int knockbackTicks;
    private int freezeTicks;
    private final Map<UUID, Integer> frozen = new HashMap<>();

    public StormPrism(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder prismAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // no goals at all: the state machine below drives the whole fight
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(WIDTH, HEIGHT);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        keepAfloat();
        applyStorm();
        tickKnockback();
        tickFreeze();

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            target = this.findNearestPlayer();
            this.setTarget(target);
        }
        if (++this.phaseTicks >= (this.phase == Phase.MOVING ? MOVE_TICKS : STOP_TICKS)) {
            this.phaseTicks = 0;
            this.phase = this.phase == Phase.MOVING ? Phase.STOPPING : Phase.MOVING;
            this.castDone = false;
        }
        if (this.phase == Phase.MOVING) {
            driftTowards(target);
        } else if (!this.castDone && this.phaseTicks >= CAST_DELAY && target != null) {
            this.castDone = true;
            this.castSkill(target);
        }
    }

    /** Hovers at a fixed height above the floor instead of walking. */
    private void keepAfloat() {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, 0.0D, motion.z);
        this.fallDistance = 0.0F;
    }

    /** The passive storm: a 20% slow and faster warmth loss for everyone in the room. */
    private void applyStorm() {
        for (ServerPlayer player : ((ServerLevel) this.level()).getPlayers(
                player -> player.distanceToSqr(this) < STORM_RADIUS * STORM_RADIUS)) {
            player.addEffect(new MobEffectInstance(ModMobEffects.STORM_WIND, 40, 0,
                    false, false, false));
        }
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 3.0D, this.getZ(),
                    6, 2.5D, 1.5D, 2.5D, 0.02D);
        }
    }

    private LivingEntity findNearestPlayer() {
        Player player = this.level().getNearestPlayer(this, 48.0D);
        return player;
    }

    private void driftTowards(LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 direction = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize().scale(0.14D);
        this.setDeltaMovement(direction.x, 0.0D, direction.z);
        this.hasImpulse = true;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    /** Picks a skill by weight: 70 / 29 / 1. */
    private void castSkill(LivingEntity target) {
        int roll = this.random.nextInt(100);
        if (roll < 70) {
            castIceBlock(target);
        } else if (roll < 99) {
            castShove(target);
        } else {
            castFreeze();
        }
    }

    // ------------------------------------------------------------------ skill 1

    private void castIceBlock(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        StormIceShard shard = com.myriadbloom.FrostMobs.STORM_ICE_SHARD.get().create(server);
        if (shard == null) {
            return;
        }
        shard.moveTo(target.getX(), target.getY() + 14.0D, target.getZ(), 0.0F, 0.0F);
        shard.setOwner(this);
        server.addFreshEntity(shard);
        server.playSound(null, this.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.2F, 1.6F);
    }

    // ------------------------------------------------------------------ skill 2

    private void castShove(LivingEntity target) {
        Vec3 direction = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        }
        direction = direction.normalize().scale(2.1D);
        target.push(direction.x, 0.35D, direction.z);
        target.hurtMarked = true;
        this.knockbackPending = true;
        this.knockbackTarget = target.getUUID();
        this.knockbackStart = target.position();
        this.knockbackTicks = 0;
        if (this.level() instanceof ServerLevel server) {
            server.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.HOSTILE, 1.4F, 1.0F);
        }
    }

    /** Resolves the shove once the target has stopped: hitting a wall doubles the damage. */
    private void tickKnockback() {
        if (!this.knockbackPending || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (++this.knockbackTicks < 10) {
            return;
        }
        Player player = server.getPlayerByUUID(this.knockbackTarget);
        if (player == null) {
            this.knockbackPending = false;
            return;
        }
        boolean stopped = player.getDeltaMovement().horizontalDistanceSqr() < 0.0025D;
        if (!stopped && this.knockbackTicks < 40) {
            return;
        }
        double travelled = player.position().distanceTo(this.knockbackStart);
        boolean hitWall = travelled < 1.2D;
        float damage = hitWall ? SKILL2_WALL_DAMAGE : SKILL2_DAMAGE;
        player.hurt(this.damageSources().mobAttack(this), damage);
        server.sendParticles(hitWall ? ParticleTypes.EXPLOSION : ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.05D);
        this.knockbackPending = false;
    }

    // ------------------------------------------------------------------ skill 3

    private void castFreeze() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(14.0D), candidate -> candidate != this && candidate.isAlive())) {
            this.frozen.put(entity.getUUID(), SKILL3_TICKS);
        }
        server.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /** Holds every frozen entity still, ticks damage, and finishes with a burst. */
    private void tickFreeze() {
        if (this.frozen.isEmpty() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : this.frozen.entrySet()) {
            var entity = server.getEntity(entry.getKey());
            int left = entry.getValue() - 1;
            if (entity == null || !entity.isAlive()) {
                done.add(entry.getKey());
                continue;
            }
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, motion.y, 0.0D);
            entity.hurtMarked = true;
            if (left % 20 == 0 && left > 0) {
                entity.hurt(this.damageSources().freeze(), SKILL3_TICK_DAMAGE);
            }
            if (left <= 0) {
                entity.hurt(this.damageSources().mobAttack(this), SKILL3_END_DAMAGE);
                done.add(entry.getKey());
            } else {
                entry.setValue(left);
            }
        }
        done.forEach(this.frozen::remove);
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && this.getTarget() == null) {
            this.setTarget(attacker);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        this.bossEvent.removeAllPlayers();
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 3.0D, this.getZ(),
                    120, 3.0D, 3.0D, 3.0D, 0.2D);
            server.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
            if (source.getEntity() instanceof ServerPlayer killer) {
                com.myriadbloom.world.AdvancementAward.award(killer, "storm_prism_slain");
            }
            spawnLootChest(server);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
                                   BlockPos pos) {
        // it never falls
    }

    /** 死亡原地生成巨木箱，并填入风暴棱镜的战利品表。 */
    private void spawnLootChest(net.minecraft.server.level.ServerLevel server) {
        net.minecraft.core.BlockPos base = this.blockPosition();
        for (int dy = 0; dy >= -4; dy--) {
            net.minecraft.core.BlockPos candidate = base.offset(0, dy, 0);
            if (server.getBlockState(candidate).canBeReplaced()
                    && server.getBlockState(candidate.below()).isSolid()) {
                server.setBlock(candidate, com.myriadbloom.BossContent.GIANT_CHEST.get().defaultBlockState(), 3);
                if (server.getBlockEntity(candidate)
                        instanceof com.myriadbloom.world.block.GiantChestBlockEntity chest) {
                    chest.setLootTable(net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.LOOT_TABLE,
                                    com.myriadbloom.api.MyriadBloomApi.id("chests/storm_prism")),
                            server.getRandom().nextLong());
                }
                return;
            }
        }
    }
}
