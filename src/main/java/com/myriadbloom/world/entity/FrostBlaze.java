package com.myriadbloom.world.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * 寒霜神 (frost blaze): a blaze made of ice.
 *
 * <p>The movement and attack rhythm are the vanilla blaze's (charge, four bursts, long recharge),
 * copied into {@link FrostAttackGoal} because the vanilla goal hardcodes a fireball. Differences:</p>
 *
 * <ul>
 *   <li>each burst fires <b>three ice bolts</b>, 6 damage and 0.3 knockback each</li>
 *   <li>snowflakes swirl around it while it hovers</li>
 *   <li>water under its feet freezes to ice instantly</li>
 *   <li>touching lava kills it instantly</li>
 * </ul>
 */
public class FrostBlaze extends Blaze {
    private int snowTimer;

    public FrostBlaze(EntityType<? extends Blaze> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // the vanilla goal only knows SmallFireball, replace it with the ice version
        this.goalSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().equals("BlazeAttackGoal"));
        this.goalSelector.addGoal(4, new FrostAttackGoal(this));
    }

    /**
     * 冰做的烈焰神当然不怕水：覆盖掉原版烈焰神的"遇水受伤 + 头顶冒烟"，
     * 否则它在水里会一边结冰一边冒烟，很出戏。
     */
    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (++this.snowTimer >= 2 && this.level() instanceof ServerLevel server) {
            this.snowTimer = 0;
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.6D, this.getZ(),
                    3, 0.55D, 0.7D, 0.55D, 0.01D);
        }
        BlockPos feet = this.blockPosition();
        BlockPos below = feet.below();
        if (com.myriadbloom.FrostConfig.FROST_BLAZE_FREEZES_WATER.get()) {
            freeze(feet);
            freeze(below);
        }
        if (com.myriadbloom.FrostConfig.FROST_BLAZE_LAVA_KILLS.get() && (isLava(feet) || isLava(below))) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 1.4F);
            this.hurt(this.damageSources().lava(), 1000.0F);
        }
    }

    private void freeze(BlockPos pos) {
        FluidState fluid = this.level().getFluidState(pos);
        if (fluid.is(Fluids.WATER) && fluid.isSource()) {
            this.level().setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
        }
    }

    private boolean isLava(BlockPos pos) {
        return this.level().getFluidState(pos).is(Fluids.LAVA)
                || this.level().getBlockState(pos).is(Blocks.LAVA);
    }

    /** Fires three ice bolts per burst. */
    private void shootBolts(LivingEntity target, double dx, double dy, double dz, double distSq) {
        double spread = Math.sqrt(Math.sqrt(distSq)) * 0.5D;
        int bolts = com.myriadbloom.FrostConfig.FROST_BLAZE_BOLTS.get();
        for (int i = 0; i < bolts; i++) {
            double ox = this.getRandom().triangle(dx, 2.297D * spread) * 0.08D;
            double oy = dy * 1.0D;
            double oz = this.getRandom().triangle(dz, 2.297D * spread) * 0.08D;
            FrostBolt bolt = new FrostBolt(this.level(), this);
            bolt.setPos(this.getX(), this.getY(0.5D) + 0.5D, this.getZ());
            bolt.shoot(ox, oy, oz, 1.6F, 0.5F);
            this.level().addFreshEntity(bolt);
        }
    }

    /** Copy of the vanilla blaze attack rhythm, but shooting ice bolts. */
    private static final class FrostAttackGoal extends Goal {
        private final FrostBlaze blaze;
        private int attackStep;
        private int attackTime;
        private int lastSeen;

        FrostAttackGoal(FrostBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.blaze.getTarget();
            return target != null && target.isAlive() && this.blaze.canAttack(target);
        }

        @Override
        public void start() {
            this.attackStep = 0;
        }

        @Override
        public void stop() {
            this.lastSeen = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.attackTime--;
            LivingEntity target = this.blaze.getTarget();
            if (target == null) {
                return;
            }
            boolean seen = this.blaze.getSensing().hasLineOfSight(target);
            if (seen) {
                this.lastSeen = 0;
            } else {
                this.lastSeen++;
            }
            double distSq = this.blaze.distanceToSqr(target);
            if (distSq < 4.0D) {
                if (!seen) {
                    return;
                }
                if (this.attackTime <= 0) {
                    this.attackTime = 20;
                    this.blaze.doHurtTarget(target);
                }
                this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
            } else if (distSq < 48.0D * 48.0D && seen) {
                double dx = target.getX() - this.blaze.getX();
                double dy = target.getY(0.5D) - this.blaze.getY(0.5D);
                double dz = target.getZ() - this.blaze.getZ();
                if (this.attackTime <= 0) {
                    this.attackStep++;
                    if (this.attackStep == 1) {
                        this.attackTime = 60;
                    } else if (this.attackStep <= 4) {
                        this.attackTime = 6;
                    } else {
                        this.attackTime = 100;
                        this.attackStep = 0;
                    }
                    if (this.attackStep > 1) {
                        if (!this.blaze.isSilent()) {
                            this.blaze.level().levelEvent(null, 1018, this.blaze.blockPosition(), 0);
                        }
                        this.blaze.shootBolts(target, dx, dy, dz, distSq);
                    }
                }
                this.blaze.getLookControl().setLookAt(target, 10.0F, 10.0F);
            } else if (this.lastSeen < 5) {
                this.blaze.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
            }
            super.tick();
        }
    }
}
