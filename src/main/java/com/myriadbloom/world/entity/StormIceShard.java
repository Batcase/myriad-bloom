package com.myriadbloom.world.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 冰棱 (falling ice shard): the projectile of the storm prism's first skill.
 *
 * <p>It is spawned high above the target and falls at a constant speed, so the impact always happens
 * about 1.5 seconds after the cast - enough time to step aside.</p>
 */
public class StormIceShard extends Entity {
    /** Blocks per tick; 14 blocks of travel take about 30 ticks (1.5 s). */
    private static final double FALL_SPEED = 0.47D;
    private static final float DAMAGE = 8.0F;
    private static final double RADIUS = 1.0D;
    private StormPrism owner;

    public StormIceShard(EntityType<? extends StormIceShard> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(StormPrism owner) {
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.setPos(this.getX(), this.getY() - FALL_SPEED, this.getZ());
        server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                3, 0.3D, 0.3D, 0.3D, 0.01D);
        int floorY = server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(this.getX()), (int) Math.floor(this.getZ()));
        if (this.getY() <= floorY || this.tickCount > 60) {
            impact(server);
        }
    }

    private void impact(ServerLevel server) {
        Vec3 centre = this.position();
        server.sendParticles(ParticleTypes.ITEM_SNOWBALL, centre.x, centre.y + 0.5D, centre.z,
                30, 1.2D, 0.6D, 1.2D, 0.2D);
        server.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4F, 1.2F);
        AABB area = new AABB(centre, centre).inflate(RADIUS);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate.isAlive() && !(candidate instanceof StormPrism))) {
            entity.hurt(this.damageSources().freeze(), DAMAGE);
        }
        this.discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
