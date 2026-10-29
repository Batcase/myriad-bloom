package com.myriadbloom.world.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 雪精灵 (snow sprite): the hostile flying spirit of the congealed land.
 *
 * <p>Modelled on the vanilla allay (same model, recoloured to snowflake blue with ice crystal wings)
 * but it is a blizzard hunter: exactly like the frenzied snow golem it turns hostile while it snows,
 * and the realm's permanent blizzard keeps that true. It flies freely, hits for 4 and carries 15
 * health and 2 armour. The congealed land's penalties only ever apply to players, so the sprite is
 * unaffected by the land it lives in.</p>
 */
public class SnowSprite extends Allay {
    /** Ticks between two melee hits. */
    private static final int ATTACK_COOLDOWN = 30;
    private int attackCooldown;

    public SnowSprite(EntityType<? extends Allay> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder spriteAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Hostile exactly while it snows, like the frenzied snow golem. */
    public boolean isFrenzied() {
        return this.level().isRaining();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // drop the allay behaviour (item fetching, duplication, dancing)
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpriteAttackGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true,
                false, candidate -> this.isFrenzied()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.level().isClientSide) {
            return;
        }
        // snow dust trails off the wings while it hunts
        if (this.isFrenzied() && this.tickCount % 4 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 0.4D, this.getZ(), 2, 0.3D, 0.25D, 0.3D, 0.01D);
        }
    }

    /** Flies to the target and slams into it for 4 damage. */
    private static final class SpriteAttackGoal extends Goal {
        private final SnowSprite sprite;

        SpriteAttackGoal(SnowSprite sprite) {
            this.sprite = sprite;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.sprite.getTarget();
            return target != null && target.isAlive() && this.sprite.isFrenzied();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.sprite.getTarget();
            if (target == null) {
                return;
            }
            this.sprite.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distanceSq = this.sprite.distanceToSqr(target);
            // hover a little above the target so the sprite reads as a flyer
            if (distanceSq > 2.6D) {
                double bob = Mth.sin(this.sprite.tickCount * 0.12F) * 0.25D;
                this.sprite.getMoveControl().setWantedPosition(
                        target.getX(), target.getY() + 0.9D + bob, target.getZ(), 1.1D);
                return;
            }
            this.sprite.getMoveControl().setWantedPosition(
                    target.getX(), target.getY() + 0.9D, target.getZ(), 1.1D);
            if (this.sprite.attackCooldown > 0) {
                return;
            }
            this.sprite.attackCooldown = ATTACK_COOLDOWN;
            float damage = (float) this.sprite.getAttributeValue(Attributes.ATTACK_DAMAGE);
            target.hurt(this.sprite.damageSources().mobAttack(this.sprite), damage);
            Vec3 shove = new Vec3(target.getX() - this.sprite.getX(), 0.25D, target.getZ() - this.sprite.getZ());
            if (shove.lengthSqr() > 1.0E-4D) {
                shove = shove.normalize().scale(0.25D);
                target.push(shove.x, shove.y, shove.z);
                target.hurtMarked = true;
            }
            if (this.sprite.level() instanceof ServerLevel server) {
                server.playSound(null, this.sprite.getX(), this.sprite.getY(), this.sprite.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.7F, 1.6F);
                server.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8D, target.getZ(),
                        10, 0.3D, 0.3D, 0.3D, 0.05D);
            }
        }
    }
}
