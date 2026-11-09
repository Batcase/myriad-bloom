package com.myriadbloom.world.entity;

import com.myriadbloom.MyriadBloom;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 时间维护者 (Time Keeper): crystal mecha boss of the sealed void.
 *
 * <p>It never melees. It cycles through four scripted skills, each with a visible wind-up:</p>
 * <ul>
 *   <li>75% 重锤砸地: 2.5 s wind-up, red warning over a 2x2 area, 15 damage (halved while blocking,
 *       and blocking is disabled like an axe hit)</li>
 *   <li>18% 横扫: 1x4 sweep, 10 damage and knockback</li>
 *   <li>5% 时间停滞: everything but the boss is frozen for 2 s, then a light pillar hits each player
 *       for 20 damage</li>
 *   <li>2% 自我修复: heals 20% of max health over 2 s</li>
 * </ul>
 *
 * <p>The boss also carries a 100 point shield that refills 40 s after it breaks.</p>
 */
public class TimeKeeper extends Monster {
    public static final float WIDTH = 4.0F;
    public static final float HEIGHT = 6.0F;
    /** 护盾上限。 */
    public static final int MAX_SHIELD = 100;
    /** 护盾破碎后多少 tick 恢复：40 秒。 */
    private static final int SHIELD_REGEN_DELAY = 800;

    /** 动画状态：0 待机，1 举锤（技能1），2 横扫（技能2），3 时间停滞，4 修复。 */
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_SLAM = 1;
    public static final int ANIM_SWEEP = 2;
    public static final int ANIM_STASIS = 3;
    public static final int ANIM_HEAL = 4;

    private static final EntityDataAccessor<Integer> DATA_ANIM =
            SynchedEntityData.defineId(TimeKeeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIM_TICK =
            SynchedEntityData.defineId(TimeKeeper.class, EntityDataSerializers.INT);
    /** 护盾值（同步给客户端画第二条血条）。 */
    private static final EntityDataAccessor<Integer> DATA_SHIELD =
            SynchedEntityData.defineId(TimeKeeper.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)
            .setPlayBossMusic(false);
    /** 护盾条：原版白色 BOSS 条，无标题。 */
    private final ServerBossEvent shieldEvent = (ServerBossEvent) new ServerBossEvent(
            net.minecraft.network.chat.Component.empty(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS)
            .setPlayBossMusic(false);

    private int shield = MAX_SHIELD;
    private int shieldRegenAt = -1;
    private int cooldown = 60;
    private int animTick;
    private boolean animFired;
    private Vec3 slamTarget = Vec3.ZERO;
    private final List<Player> frozen = new java.util.ArrayList<>();

    public TimeKeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 800;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder keeperAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIM, ANIM_IDLE);
        builder.define(DATA_ANIM_TICK, 0);
        builder.define(DATA_SHIELD, MAX_SHIELD);
    }

    public int animState() {
        return this.entityData.get(DATA_ANIM);
    }

    public int animTickClient() {
        return this.entityData.get(DATA_ANIM_TICK);
    }

    public int shield() {
        return this.entityData.get(DATA_SHIELD);
    }

    @Override
    protected void registerGoals() {
        // scripted boss, no vanilla goals
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(WIDTH, HEIGHT);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            // 每 tick 把生命与护盾推给两条 BOSS 条
            this.bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F,
                    this.getHealth() / this.getMaxHealth())));
            this.shieldEvent.setProgress(Math.max(0.0F, Math.min(1.0F,
                    this.shield / (float) MAX_SHIELD)));
            // 场地上只留最早生成的那一只，其他直接移除，避免血条/护盾条互相串
            var others = this.level().getEntitiesOfClass(TimeKeeper.class,
                    this.getBoundingBox().inflate(64.0D), other -> other != this);
            for (TimeKeeper other : others) {
                // 保留最新生成的那一只（id 最大的），旧的清掉
                if (other.getId() < this.getId()) {
                    other.discard();
                }
            }
        }
        if (!this.level().isClientSide) {
            // 保证客户端看到的护盾值始终和服务器一致
            // 护盾恢复
            if (this.shield < MAX_SHIELD && this.shieldRegenAt > 0
                    && this.tickCount >= this.shieldRegenAt) {
                this.shield = MAX_SHIELD;
                this.entityData.set(DATA_SHIELD, MAX_SHIELD);
                this.shieldRegenAt = -1;
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.HOSTILE, 2.0F, 1.2F);
            }
            tickCombat();
        }
    }

    // ------------------------------------------------------------------ 战斗循环

    private void tickCombat() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Player target = server.getNearestPlayer(this, 48.0D);
        if (target == null) {
            this.entityData.set(DATA_ANIM, ANIM_IDLE);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.entityData.get(DATA_ANIM) != ANIM_IDLE) {
            this.animTick++;
            this.entityData.set(DATA_ANIM_TICK, this.animTick);
            runAnim(server, target);
            return;
        }
        if (--this.cooldown > 0) {
            return;
        }
        startSkill(server, target);
    }

    /** 按权重挑选技能。 */
    private void startSkill(ServerLevel server, Player target) {
        int roll = this.random.nextInt(100);
        this.animTick = 0;
        this.animFired = false;
        if (roll < 75) {
            this.slamTarget = target.position();
            this.entityData.set(DATA_ANIM, ANIM_SLAM);
        } else if (roll < 93) {
            this.entityData.set(DATA_ANIM, ANIM_SWEEP);
        } else if (roll < 98) {
            this.entityData.set(DATA_ANIM, ANIM_STASIS);
        } else {
            this.entityData.set(DATA_ANIM, ANIM_HEAL);
        }
        this.entityData.set(DATA_ANIM_TICK, 0);
        server.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    /** 推进当前技能。 */
    private void runAnim(ServerLevel server, Player target) {
        switch (this.entityData.get(DATA_ANIM)) {
            case ANIM_SLAM -> {
                // 2.5 秒 = 50 tick 的预警
                if (this.animTick < 50) {
                    warnArea(server, this.slamTarget, 1);
                    return;
                }
                if (!this.animFired) {
                    this.animFired = true;
                    impactSlam(server);
                }
                if (this.animTick > 60) {
                    endSkill(40);
                }
            }
            case ANIM_SWEEP -> {
                if (this.animTick < 16) {
                    return;
                }
                if (!this.animFired) {
                    this.animFired = true;
                    sweep(server, target);
                }
                if (this.animTick > 30) {
                    endSkill(50);
                }
            }
            case ANIM_STASIS -> {
                if (this.animTick < 40) {
                    if (this.animTick == 1) {
                        freezeAll(server);
                    }
                    return;
                }
                if (!this.animFired) {
                    this.animFired = true;
                    lightPillars(server);
                }
                if (this.animTick > 55) {
                    thawAll();
                    endSkill(60);
                }
            }
            case ANIM_HEAL -> {
                if (this.animTick < 40) {
                    healParticles(server);
                    return;
                }
                if (!this.animFired) {
                    this.animFired = true;
                    float heal = this.getMaxHealth() * 0.2F;
                    this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + heal));
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 3.0D, this.getZ(),
                            40, 2.5D, 3.0D, 2.5D, 0.05D);
                }
                if (this.animTick > 55) {
                    endSkill(45);
                }
            }
            default -> endSkill(40);
        }
    }

    private void endSkill(int ignored) {
        this.entityData.set(DATA_ANIM, ANIM_IDLE);
        this.entityData.set(DATA_ANIM_TICK, 0);
        this.animTick = 0;
        // 技能结束后 1~2 秒就重新随机（20~40 tick）
        this.cooldown = 20 + this.random.nextInt(21);
    }

    // ------------------------------------------------------------------ 技能 1：重锤砸地

    /** 在 2x2 区域刷红色预警。 */
    private void warnArea(ServerLevel server, Vec3 centre, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                server.sendParticles(ParticleTypes.CRIT,
                        centre.x + dx + 0.5D, centre.y + 0.1D, centre.z + dz + 0.5D,
                        4, 0.25D, 0.0D, 0.25D, 0.0D);
                server.sendParticles(ParticleTypes.FLAME,
                        centre.x + dx + 0.5D, centre.y + 0.05D, centre.z + dz + 0.5D,
                        1, 0.2D, 0.0D, 0.2D, 0.0D);
            }
        }
    }

    private void impactSlam(ServerLevel server) {
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.slamTarget.x, this.slamTarget.y + 0.5D, this.slamTarget.z, 1, 0, 0, 0, 0);
        server.playSound(null, this.slamTarget.x, this.slamTarget.y, this.slamTarget.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
        AABB area = new AABB(this.slamTarget.x - 1.0D, this.slamTarget.y - 1.0D, this.slamTarget.z - 1.0D,
                this.slamTarget.x + 1.0D, this.slamTarget.y + 2.5D, this.slamTarget.z + 1.0D);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive())) {
            float damage = 15.0F;
            if (victim instanceof Player player && player.isBlocking()) {
                damage *= 0.5F;
                // 类似斧头破盾
                player.getCooldowns().addCooldown(player.getUseItem().getItem(), 100);
                player.stopUsingItem();
                player.hurt(this.damageSources().mobAttack(this), damage);
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this), damage);
        }
    }

    // ------------------------------------------------------------------ 技能 2：横扫

    private void sweep(ServerLevel server, Player target) {
        Vec3 look = this.getLookAngle();
        Vec3 start = this.position().add(look.scale(1.5D));
        AABB area = new AABB(start.x - 2.0D, this.getY(), start.z - 2.0D,
                start.x + 2.0D, this.getY() + 4.0D, start.z + 2.0D);
        server.sendParticles(ParticleTypes.SWEEP_ATTACK, start.x, this.getY() + 2.0D, start.z,
                8, 1.2D, 0.8D, 1.2D, 0.0D);
        server.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.0F, 0.6F);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive())) {
            victim.hurt(this.damageSources().mobAttack(this), 10.0F);
            Vec3 push = victim.position().subtract(this.position()).normalize().scale(2.2D);
            victim.push(push.x, 0.4D, push.z);
            victim.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ 技能 3：时间停滞

    private void freezeAll(ServerLevel server) {
        this.frozen.clear();
        for (Player player : server.players()) {
            if (player.isAlive()) {
                this.frozen.add(player);
            }
        }
        // 用统一的时停机制：每 tick 强制静止，并画出脚底时钟
        var targets = new java.util.ArrayList<net.minecraft.world.entity.Entity>(this.frozen);
        var mobs = server.getEntities(this, this.getBoundingBox().inflate(25.0D),
                e -> e instanceof net.minecraft.world.entity.Mob && e.isAlive());
        targets.addAll(mobs);
        com.myriadbloom.world.TimeStopMechanic.freeze(targets, 60);
        MyriadBloom.LOGGER.info("time keeper stasis: froze {} targets", targets.size());
        server.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /** 每位玩家头顶降下光柱并造成 20 伤害。 */
    private void lightPillars(ServerLevel server) {
        for (Player player : this.frozen) {
            if (!player.isAlive()) {
                continue;
            }
            for (int y = 0; y < 8; y++) {
                server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + y, player.getZ(),
                        6, 0.25D, 0.25D, 0.25D, 0.0D);
            }
            server.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.HOSTILE, 1.5F, 1.6F);
            player.hurt(this.damageSources().magic(), 20.0F);
        }
    }

    private void thawAll() {
        for (Player player : this.frozen) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.JUMP);
        }
        this.frozen.clear();
    }

    // ------------------------------------------------------------------ 技能 4：修复

    private void healParticles(ServerLevel server) {
        if (this.animTick % 4 != 0) {
            return;
        }
        double angle = this.animTick * 0.4D;
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                this.getX() + Math.cos(angle) * 3.0D, this.getY() + 1.0D + this.animTick * 0.05D,
                this.getZ() + Math.sin(angle) * 3.0D, 2, 0.2D, 0.2D, 0.2D, 0.0D);
    }

    // ------------------------------------------------------------------ 护盾与伤害

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.shield > 0) {
            int absorbed = Math.min(this.shield, (int) Math.ceil(amount));
            MyriadBloom.LOGGER.info("keeper hurt {} -> shield {} (absorb {})", amount, this.shield, absorbed);
            this.shield -= absorbed;
            this.entityData.set(DATA_SHIELD, this.shield);
            this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.HOSTILE, 1.2F, 1.4F);
            if (this.shield <= 0) {
                this.shield = 0;
                this.entityData.set(DATA_SHIELD, 0);
                this.shieldRegenAt = this.tickCount + SHIELD_REGEN_DELAY;
                this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK,
                        SoundSource.HOSTILE, 2.0F, 0.8F);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        this.shieldEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
        this.shieldEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        this.bossEvent.removeAllPlayers();
        this.shieldEvent.removeAllPlayers();
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 3.0D, this.getZ(),
                    160, 3.0D, 4.0D, 3.0D, 0.25D);
            server.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.HOSTILE, 2.5F, 0.5F);
            // 3 秒后送玩家回去，并清理祭坛与冰山
            server.getServer().tell(new net.minecraft.server.TickTask(60, () -> finishFight(server)));
        }
        super.die(source);
    }

    /** 战斗结束：把玩家送回霜境原位，移除冰山/祭坛，原地生成战利品箱。 */
    private void finishFight(ServerLevel server) {
        var keeper = this;
        for (ServerPlayer player : java.util.List.copyOf(server.players())) {
            var data = player.getPersistentData();
            String dim = data.getString("myriadbloom_return_dim");
            if (dim.isEmpty()) {
                // 没有祭坛记录（例如手动召唤的 BOSS）：兜底传送到霜域出生点
                var frost = server.getServer().getLevel(
                        com.myriadbloom.api.MyriadBloomApi.FROST_REALM);
                if (frost != null) {
                    player.teleportTo(frost, 0.5D, frost.getSharedSpawnPos().getY() + 1.0D, 0.5D,
                            player.getYRot(), player.getXRot());
                }
            } else {
                var key = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(dim));
                ServerLevel target = server.getServer().getLevel(key);
                if (target != null) {
                    player.teleportTo(target,
                            data.getDouble("myriadbloom_return_x"),
                            data.getDouble("myriadbloom_return_y"),
                            data.getDouble("myriadbloom_return_z"),
                            data.getFloat("myriadbloom_return_yaw"),
                            data.getFloat("myriadbloom_return_pitch"));
                }
            }
            long altarPos = data.getLong("myriadbloom_altar_pos");
            String altarDim = data.getString("myriadbloom_altar_dim");
            if (altarPos != 0L && !altarDim.isEmpty()) {
                var key = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(altarDim));
                ServerLevel site = server.getServer().getLevel(key);
                if (site != null) {
                    clearSite(site, net.minecraft.core.BlockPos.of(altarPos));
                }
            }
            // 回到霜境后给予已充能的时之沙漏，并授予「时之歌」
            var charged = new net.minecraft.world.item.ItemStack(
                    com.myriadbloom.BossContent.CHARGED_HOURGLASS.get());
            if (!player.getInventory().add(charged)) {
                player.drop(charged, false);
            }
            com.myriadbloom.world.AdvancementAward.award(player, "timesong", "done");
            data.remove("myriadbloom_return_dim");
            data.remove("myriadbloom_altar_pos");
        }
        MyriadBloom.LOGGER.info("Time keeper defeated; players returned to the frost realm");
    }

    /** 清掉祭坛与周围的冰，并放一个空的战利品箱占位。 */
    private static void clearSite(ServerLevel level, net.minecraft.core.BlockPos altar) {
        int radius = 20;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 44; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    var pos = altar.offset(dx, dy, dz);
                    var state = level.getBlockState(pos);
                    boolean ice = state.is(net.minecraft.tags.BlockTags.ICE)
                            || state.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)
                            || state.is(net.minecraft.world.level.block.Blocks.PACKED_ICE)
                            || state.is(net.minecraft.world.level.block.Blocks.BLUE_ICE);
                    boolean altarBlock = state.is(com.myriadbloom.FrostedLandContent.TIME_ALTAR.get());
                    if (ice || altarBlock) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        // 箱子放在凝结之地中心（冰山被清除后的位置）
        int[] centre = com.myriadbloom.world.gen.CongealedLandFeature.patchCentre(
                altar.getX() >> 4, altar.getZ() >> 4);
        var chestPos = new net.minecraft.core.BlockPos(centre[0],
                level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        centre[0], centre[1]), centre[1]);
        level.setBlock(chestPos, com.myriadbloom.BossContent.GIANT_CHEST.get().defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos)
                instanceof com.myriadbloom.world.block.GiantChestBlockEntity chest) {
            chest.setLootTable(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.LOOT_TABLE,
                    com.myriadbloom.api.MyriadBloomApi.id("chests/time_keeper")), level.getRandom().nextLong());
        }
        level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5D, altar.getY() + 1.0D,
                altar.getZ() + 0.5D, 60, 2.0D, 2.0D, 2.0D, 0.1D);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        this.shieldEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
