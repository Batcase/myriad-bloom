package com.myriadbloom.world.entity;

import com.myriadbloom.FrostMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 冰弹 (ice bolt): the projectile of the frost blaze.
 *
 * <p>Deals 6 damage and a 0.3 knockback, leaves a snowflake trail, and shatters with a glass sound on
 * impact.</p>
 */
public class FrostBolt extends ThrowableProjectile {
    /** Damage of one bolt, default 6; read from the config when it hits. */
    public static final float DAMAGE = 6.0F;

    private static float damage() {
        return (float) (double) com.myriadbloom.FrostConfig.FROST_BLAZE_BOLT_DAMAGE.get();
    }

    private static double knockback() {
        return com.myriadbloom.FrostConfig.FROST_BLAZE_BOLT_KNOCKBACK.get();
    }

    public FrostBolt(EntityType<? extends FrostBolt> entityType, Level level) {
        super(entityType, level);
    }

    public FrostBolt(Level level, LivingEntity shooter) {
        super(FrostMobs.FROST_BOLT.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.ITEM_SNOWBALL,
                    this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        Entity target = result.getEntity();
        Entity shooter = this.getOwner();
        target.hurt(this.damageSources().thrown(this, shooter), damage());
        if (target instanceof LivingEntity living) {
            // LivingEntity#knockback subtracts the vector, so pass victim -> shooter
            Vec3 away = new Vec3(living.getX() - this.getX(), 0.0D, living.getZ() - this.getZ());
            if (away.lengthSqr() > 1.0E-4D) {
                away = away.normalize().scale(knockback() * 2.0D);
                living.knockback(knockback(), away.x, away.z);
            }
        }
        shatter();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        shatter();
    }

    private void shatter() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.02D);
            server.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.3F);
        }
        this.discard();
    }
}
