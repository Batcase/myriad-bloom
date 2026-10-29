package com.myriadbloom.world.entity;

import com.myriadbloom.api.MyriadBloomApi;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A snow golem that turns hostile while it is snowing.
 *
 * <p>The realm is permanently in a blizzard ({@code LevelWeatherMixin} forces {@code getRainLevel} to
 * 1.0), so {@link #isFrenzied()} is true there and the golem hunts players. Once the planned boss is
 * defeated and the blizzard is switched off, {@code level.isRaining()} becomes false and the very same
 * entity calms down again - no extra state, no extra code.</p>
 *
 * <ul>
 *   <li>20 health, 32 block follow range</li>
 *   <li>throws plain <b>vanilla</b> snowballs; 1 damage and knockback are added on impact by
 *       {@code FrostEvents#onProjectileImpact}</li>
 *   <li>hunts players, but fights back at whatever hurt it (a zombie angered by a stray snowball)</li>
 *   <li><b>flees from heat</b>: a campfire, lava or magma block within {@value #HEAT_RADIUS} blocks
 *       makes it drop its target and run away</li>
 * </ul>
 */
public class FrenziedSnowGolem extends SnowGolem {
    /** Radius searched for heat sources such as campfires, lava and magma blocks. */
    private static final int HEAT_RADIUS = 6;
    private static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, MyriadBloomApi.id("heat_sources"));

    private int retaliationCooldown;
    private int heatScanTimer;
    private BlockPos cachedHeat;

    public FrenziedSnowGolem(EntityType<? extends SnowGolem> entityType, Level level) {
        super(entityType, level);
    }

    /** Attributes for the hostile variant: vanilla golem speed, ten hearts, long sight. */
    public static AttributeSupplier.Builder frenziedAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Hostile exactly while it snows; the realm's permanent blizzard keeps this true. */
    public boolean isFrenzied() {
        return this.level().isRaining();
    }

    /** Heat source the golem is currently running from, or {@code null}. Refreshed server side. */
    public BlockPos nearbyHeat() {
        return this.cachedHeat;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // The inherited RangedAttackGoal only ever fires at whatever the vanilla goal system picked
        // and refused to shoot at non-player targets we target ourselves, so it is replaced with an
        // explicit one that throws at *our* target (player or a mob that attacked us).
        this.goalSelector.removeAllGoals(goal -> goal instanceof RangedAttackGoal);
        //   0 - run away from heat (highest priority: fire beats everything)
        //   1 - throw at the current target
        //   2 - walk into throwing range
        this.goalSelector.addGoal(0, new FleeHeatGoal(this));
        this.goalSelector.addGoal(1, new ThrowAtTargetGoal(this));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.15D, 8.0F));
        // Explicit target priorities:
        //   0 - fight back against whoever hit us
        //   1 - hunt players while it snows
        //   2 - the vanilla behaviour, pick fights with hostile mobs
        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true,
                false, candidate -> this.isFrenzied()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true,
                false, candidate -> candidate instanceof Enemy));
    }

    /**
     * The realm is permanently "raining" thanks to the weather mixin, and vanilla snow golems take
     * drown damage in rain as well as in water - without this override the golem would melt away
     * within seconds of spawning.
     */
    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    /** Walking on water freezes the block underneath, leaving a frozen trail. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            BlockPos below = this.blockPosition().below();
            if (this.level().getFluidState(below).is(FluidTags.WATER)) {
                this.level().setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
            }
        }
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (--this.heatScanTimer <= 0) {
            this.heatScanTimer = 10;
            this.cachedHeat = scanForHeat();
        }

        if (this.cachedHeat != null) {
            // fire scares it off completely: no target, no retaliation, just run (FleeHeatGoal moves)
            this.setTarget(null);
            this.retaliationCooldown = 0;
            return;
        }

        // Retaliation wins over the player hunt: if a mob (a zombie hit by a stray snowball, ...)
        // damaged us within the last few seconds, turn on it and shoot right away. Vanilla clears
        // lastHurtByMob after a short delay, so the golem returns to hunting players afterwards.
        LivingEntity attacker = this.getLastHurtByMob();
        if (attacker != null && attacker != this && !(attacker instanceof Player)
                && attacker.isAlive() && this.canAttack(attacker)
                && this.distanceToSqr(attacker) <= 32.0D * 32.0D) {
            if (this.getTarget() != attacker) {
                this.setTarget(attacker);
            }
            if (--this.retaliationCooldown <= 0) {
                this.retaliationCooldown = 20;
                this.performRangedAttack(attacker, 1.0F);
            }
        } else {
            this.retaliationCooldown = 0;
        }

        if (!this.isFrenzied() && this.getTarget() instanceof Player) {
            // the snow stopped: forget the player
            this.setTarget(null);
        }
    }

    private BlockPos scanForHeat() {
        BlockPos center = this.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -HEAT_RADIUS; dx <= HEAT_RADIUS; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -HEAT_RADIUS; dz <= HEAT_RADIUS; dz++) {
                    cursor.setWithOffset(center, dx, dy, dz);
                    if (this.level().getBlockState(cursor).is(HEAT_SOURCES)) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** Throws a vanilla snowball at whatever this golem currently targets. */
    private static final class ThrowAtTargetGoal extends Goal {
        private static final double RANGE = 12.0D;
        private final FrenziedSnowGolem golem;
        private int cooldown;

        ThrowAtTargetGoal(FrenziedSnowGolem golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.golem.getTarget();
            return target != null && target.isAlive()
                    && this.golem.distanceToSqr(target) <= RANGE * RANGE
                    && this.golem.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.cooldown = 10;
        }

        @Override
        public void tick() {
            LivingEntity target = this.golem.getTarget();
            if (target == null) {
                return;
            }
            this.golem.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.cooldown <= 0) {
                this.cooldown = 20;
                this.golem.performRangedAttack(target, 1.0F);
            }
        }
    }

    /** Runs away from the nearest heat source until none is in range. */
    private static final class FleeHeatGoal extends Goal {
        private final FrenziedSnowGolem golem;
        private int repath;

        FleeHeatGoal(FrenziedSnowGolem golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.golem.nearbyHeat() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.golem.nearbyHeat() != null;
        }

        @Override
        public void start() {
            this.golem.setTarget(null);
            this.repath = 0;
        }

        @Override
        public void tick() {
            BlockPos heat = this.golem.nearbyHeat();
            if (heat == null) {
                return;
            }
            if (--this.repath > 0) {
                return;
            }
            this.repath = 10;
            Vec3 away = new Vec3(this.golem.getX() - (heat.getX() + 0.5D), 0.0D,
                    this.golem.getZ() - (heat.getZ() + 0.5D));
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize().scale(10.0D);
            this.golem.getNavigation().moveTo(this.golem.getX() + away.x, this.golem.getY(),
                    this.golem.getZ() + away.z, 1.5D);
        }
    }
}
