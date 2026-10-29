package com.myriadbloom.world.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 始源人 (Origin Ape): the snow fortress boss.
 *
 * <p>A ten block tall white ape with red eyes. It appears from a boss spawner inside the fortress,
 * which breaks as it wakes up, and shows a boss bar while alive.</p>
 *
 * <ul>
 *   <li>200 health, 5 armour, 4 attack damage, 0.5 movement speed, 4 block reach</li>
 *   <li>normal attack: raises an arm and slams the target, once every 5 seconds</li>
 *   <li>with a small chance it charges for 2 seconds and then slams the ground, hitting everything
 *       within 4 blocks of the spot it picked when the charge started for 10 damage - the spot is
 *       fixed, so running away dodges it</li>
 *   <li>on death the body bursts into snowflakes and leaves a giant wood chest</li>
 * </ul>
 */
public class OriginApe extends Monster {
    public static final int CHARGE_TICKS = 40;
    private static final int ATTACK_INTERVAL = 100;
    private static final double SLAM_RADIUS = 4.0D;
    private static final float SLAM_DAMAGE = 10.0F;
    private static final double ATTACK_RANGE = 4.0D;

    /** 0 idle, 1 normal wind-up, 2 charge (skill), 3 skill slam, 4 normal punch. */
    private static final EntityDataAccessor<Integer> ANIM =
            SynchedEntityData.defineId(OriginApe.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS).setPlayBossMusic(false);

    private int attackCooldown;
    private int chargeTicks;
    private int animationTicks;
    private Vec3 slamTarget;

    public OriginApe(EntityType<? extends OriginApe> entityType, Level level) {
        super(entityType, level);
        this.xpReward = com.myriadbloom.FrostConfig.ORIGIN_APE_XP.get();
    }

    public static AttributeSupplier.Builder originApeAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM, 0);
    }

    /** 0 idle, 1 normal wind-up, 2 charging the slam, 3 skill slam, 4 normal punch. */
    public int getAnimState() {
        return this.entityData.get(ANIM);
    }

    /** Ticks the current animation has been running, used by the model. */
    public int getAnimationTicks() {
        return this.animationTicks;
    }

    private void setAnim(int state) {
        this.entityData.set(ANIM, state);
        this.animationTicks = 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ApeAttackGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.getAnimState() == 2) {
                this.animationTicks++;
            }
            return;
        }
        // snow drifts off the body while it charges
        if (this.getAnimState() == 2 && this.tickCount % 3 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.7D, this.getZ(),
                    6, 1.6D, 1.4D, 1.6D, 0.02D);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.animationTicks > 0) {
            this.animationTicks++;
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (com.myriadbloom.FrostConfig.ORIGIN_APE_BOSS_BAR.get()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        // super() assigns the starting health before the boss event field is initialised
        if (this.bossEvent != null) {
            this.bossEvent.setProgress(health / this.getMaxHealth());
        }
    }

    /** Bursts into snowflakes and leaves the giant chest behind. */
    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server && !this.isRemoved()) {
            // the whole body dissolves into snow
            for (int i = 0; i < 12; i++) {
                double y = this.getY() + this.getBbHeight() * (i / 11.0D);
                server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), y, this.getZ(),
                        18, 1.4D, 0.35D, 1.4D, 0.03D);
            }
            server.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.0D, this.getZ(),
                    40, 1.6D, 1.2D, 1.6D, 0.06D);
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SNOW_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);

            BlockPos chestPos = this.blockPosition();
            server.setBlockAndUpdate(chestPos,
                    com.myriadbloom.BossContent.GIANT_CHEST.get().defaultBlockState());
            if (server.getBlockEntity(chestPos) instanceof com.myriadbloom.world.block.GiantChestBlockEntity chest) {
                // the loot rolls the first time the chest is opened, exactly like a vanilla loot chest
                chest.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        com.myriadbloom.api.MyriadBloomApi.id("chests/origin_ape")), this.level().random.nextLong());
            }
        }
        this.bossEvent.removeAllPlayers();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    /** Raises an arm, then slams: normal attacks and the charged ground slam live here. */
    private static final class ApeAttackGoal extends Goal {
        private final OriginApe ape;
        private int raiseTicks;

        ApeAttackGoal(OriginApe ape) {
            this.ape = ape;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.ape.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || this.ape.chargeTicks > 0;
        }

        @Override
        public void stop() {
            this.ape.chargeTicks = 0;
            this.ape.setAnim(0);
            this.raiseTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = this.ape.getTarget();
            if (target == null) {
                return;
            }
            this.ape.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceSq = this.ape.distanceToSqr(target);

            // ---- charging the ground slam ------------------------------------------------
            if (this.ape.chargeTicks > 0) {
                this.ape.chargeTicks--;
                this.ape.setAnim(2);
                this.ape.getNavigation().stop();
                this.ape.getMoveControl().setWantedPosition(
                        this.ape.getX(), this.ape.getY(), this.ape.getZ(), 0.0D);
                if (this.ape.chargeTicks == 0) {
                    slam();
                }
                return;
            }

            // ---- normal wind-up -----------------------------------------------------------
            if (this.raiseTicks > 0) {
                this.raiseTicks--;
                if (this.raiseTicks == 0) {
                    // single target punch: only the target is hit, no area effect
                    this.ape.setAnim(4);
                    if (this.ape.distanceToSqr(target) <= ATTACK_RANGE * ATTACK_RANGE) {
                        target.hurt(this.ape.damageSources().mobAttack(this.ape),
                                (float) this.ape.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        if (this.ape.level() instanceof ServerLevel server) {
                            server.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.6F);
                            server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6D), target.getZ(),
                                    8, 0.3D, 0.3D, 0.3D, 0.05D);
                        }
                    }
                }
                return;
            }

            if (distanceSq > ATTACK_RANGE * ATTACK_RANGE) {
                this.ape.getNavigation().moveTo(target, 1.0D);
                return;
            }
            this.ape.getNavigation().stop();
            if (this.ape.attackCooldown > 0) {
                return;
            }
            this.ape.attackCooldown = ATTACK_INTERVAL;
            // small chance to spend the attack on the ground slam instead
            if (this.ape.getRandom().nextFloat() < 0.2F) {
                this.ape.slamTarget = target.position();
                this.ape.chargeTicks = CHARGE_TICKS;
                this.ape.setAnim(2);
            } else {
                this.raiseTicks = 10;
                this.ape.setAnim(1);
            }
        }

        /** Ten damage in four blocks around the fixed spot picked when the charge started. */
        private void slam() {
            Vec3 spot = this.ape.slamTarget;
            if (spot == null) {
                spot = this.ape.position();
            }
            AABB area = new AABB(spot.x - SLAM_RADIUS, spot.y - 3.0D, spot.z - SLAM_RADIUS,
                    spot.x + SLAM_RADIUS, spot.y + 4.0D, spot.z + SLAM_RADIUS);
            for (LivingEntity victim : this.ape.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (victim == this.ape || victim instanceof OriginApe) {
                    continue;
                }
                victim.hurt(this.ape.damageSources().mobAttack(this.ape), SLAM_DAMAGE);
            }
            this.ape.setAnim(3);
            if (this.ape.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.4D, spot.z,
                        6, SLAM_RADIUS * 0.4D, 0.2D, SLAM_RADIUS * 0.4D, 0.02D);
                server.sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y + 0.6D, spot.z,
                        60, SLAM_RADIUS * 0.5D, 0.4D, SLAM_RADIUS * 0.5D, 0.05D);
                server.playSound(null, spot.x, spot.y, spot.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6F, 0.55F);
            }
        }
    }

    // Mob import is used by the attribute builder
    private static Mob unused() {
        return null;
    }
}
