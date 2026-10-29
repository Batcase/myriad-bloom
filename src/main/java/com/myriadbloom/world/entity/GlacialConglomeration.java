package com.myriadbloom.world.entity;

import com.myriadbloom.world.ArenaGuard;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


/**
 * 极冰聚合物 (glacial conglomeration): the boss of the frozen ocean arena.
 *
 * <p>A giant slime made of ice. It hops towards its target like a slime, punches for 8 damage every two
 * seconds, and every eight seconds it leaps up, gathers snow into itself for three seconds and then
 * crashes down on the 4x4 area it is hovering above for 20 damage.</p>
 *
 * <p>Its hide soaks 40% of every hit, except from pickaxes - and a 覆雪之镐 deals 50% extra on top.</p>
 */
public class GlacialConglomeration extends Monster {
    public static final float WIDTH = 8.0F;
    public static final float HEIGHT = 8.0F;
    private static final float MELEE_DAMAGE = 8.0F;
    private static final int ATTACK_COOLDOWN = 40;      // 2 seconds
    private static final float SLAM_DAMAGE = 20.0F;
    private static final float SLAM_RADIUS = 2.0F;      // 4x4 area
    private static final int SKILL_COOLDOWN = 160;      // 8 seconds
    private static final int SKILL_WINDUP = 60;         // 3 seconds in the air

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS)
            .setPlayBossMusic(false);

    private int attackCooldown;
    private int skillCooldown = SKILL_COOLDOWN;
    private int windup;
    private Vec3 impactPos;

    public GlacialConglomeration(EntityType<? extends GlacialConglomeration> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1200;
    }

    public static AttributeSupplier.Builder glacialAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.ATTACK_DAMAGE, MELEE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // ------------------------------------------------------------------ AI ---------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new HopAttackGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Hops towards the target like a slime and punches when it is close enough. */
    private static final class HopAttackGoal extends Goal {
        private final GlacialConglomeration boss;

        HopAttackGoal(GlacialConglomeration boss) {
            this.boss = boss;
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.boss.getTarget() != null && this.boss.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.boss.getTarget();
            if (target == null) {
                return;
            }
            this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = this.boss.distanceTo(target);
            if (this.boss.onGround() && this.boss.windup <= 0 && distance < 24.0D) {
                Vec3 direction = new Vec3(target.getX() - this.boss.getX(), 0.0D,
                        target.getZ() - this.boss.getZ()).normalize();
                this.boss.setDeltaMovement(direction.x * 0.32D, 0.55D, direction.z * 0.32D);
            }
            if (this.boss.attackCooldown <= 0 && distance <= 9.0D && this.boss.hasLineOfSight(target)) {
                this.boss.doHurtTarget(target);
                this.boss.attackCooldown = ATTACK_COOLDOWN;
            }
        }
    }

    // ------------------------------------------------------------- tick logic ------------
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel server = (ServerLevel) this.level();

        if (this.windup > 0) {
            // ---- the leap: snow is pulled towards the boss, then it crashes down -------------
            this.windup--;
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            this.spawnConvergence(server);
            if (this.impactPos != null) {
                // rise cleanly to about three blocks up, hold, then slam
                float risen = (SKILL_WINDUP - this.windup) * 0.10F;
                double lift = Math.min(3.0D, risen);
                this.setPos(this.impactPos.x, this.impactPos.y + lift, this.impactPos.z);
            }
            if (this.windup == 0) {
                this.slam(server);
            }
            return;
        }

        if (this.skillCooldown > 0) {
            this.skillCooldown--;
        } else if (this.getTarget() != null && this.onGround()) {
            // ---- begin the leap -----------------------------------------------------------
            this.windup = SKILL_WINDUP;
            this.impactPos = this.position();
            this.skillCooldown = SKILL_COOLDOWN;
            this.setNoGravity(true);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 2.0F, 0.4F);
        }
    }

    /** Snowflakes spiral inwards towards the boss while it hangs in the air. */
    private void spawnConvergence(ServerLevel server) {
        float progress = 1.0F - (float) this.windup / SKILL_WINDUP;
        double radius = Mth.lerp(progress, 6.0D, 0.6D);
        double cx = this.getX();
        double cy = this.getY() + this.getBbHeight() * 0.5D;
        double cz = this.getZ();
        for (int i = 0; i < 8; i++) {
            double angle = (server.random.nextDouble() * 2.0D - 1.0D) * Math.PI;
            double height = (server.random.nextDouble() - 0.3D) * 3.0D;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            server.sendParticles(ParticleTypes.SNOWFLAKE, x, cy + height, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 20 damage in the 4x4 area under the boss, once it lands. */
    private void slam(ServerLevel server) {
        if (this.impactPos != null) {
            this.teleportTo(this.impactPos.x, this.impactPos.y, this.impactPos.z);
        }
        double cx = this.getX();
        double cz = this.getZ();
        double bottom = this.getY();
        AABB area = new AABB(cx - SLAM_RADIUS, bottom - 1.0D, cz - SLAM_RADIUS,
                cx + SLAM_RADIUS, bottom + 3.0D, cz + SLAM_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim == this || victim instanceof GlacialConglomeration) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), SLAM_DAMAGE)) {
                Vec3 push = victim.position().subtract(this.position()).normalize().scale(0.8D);
                victim.push(push.x, 0.4D, push.z);
            }
        }
        server.sendParticles(ParticleTypes.EXPLOSION, cx, bottom + 0.5D, cz, 12, 2.5D, 0.4D, 2.5D, 0.02D);
        server.sendParticles(ParticleTypes.SNOWFLAKE, cx, bottom + 0.8D, cz, 80, 3.0D, 0.6D, 3.0D, 0.12D);
        server.playSound(null, cx, bottom, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.4F, 0.5F);
        server.playSound(null, cx, bottom, cz, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.setNoGravity(false);
        this.impactPos = null;
    }

    // ------------------------------------------------------------- damage trait ----------
    @Override
    public boolean hurt(DamageSource source, float amount) {
        float scaled;
        if (source.getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (isPickaxe(weapon)) {
                // a pickaxe ignores the icy hide; the snow covered pickaxe cracks it wide open
                scaled = weapon.is(com.myriadbloom.EmberContent.FROST_PICKAXE.get()) ? amount * 1.5F : amount;
            } else {
                scaled = amount * 0.6F;
            }
        } else {
            scaled = amount * 0.6F;
        }
        return super.hurt(source, scaled);
    }

    /**
     * True for any pickaxe: the vanilla tag, the vanilla class, modded pickaxes (which usually extend
     * {@code PickaxeItem} or join the tag) and anything else that mines stone with its tool component.
     */
    private static boolean isPickaxe(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                || stack.getItem() instanceof net.minecraft.world.item.PickaxeItem
                || stack.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    }

    // ------------------------------------------------------------- boss bar --------------
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
    public void setHealth(float health) {
        super.setHealth(health);
        if (this.bossEvent != null) {
            this.bossEvent.setProgress(health / this.getMaxHealth());
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server) {
            ArenaGuard.of(server).release(this.blockPosition());
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.5F);
        }
        this.bossEvent.removeAllPlayers();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    // ------------------------------------------------------------- misc ------------------
    /** The boss is a slab of ice 8 blocks wide and tall. */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(WIDTH, HEIGHT);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    /** Never despawns, so the arena fight cannot be escaped by walking away. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
