package com.myriadbloom.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 雪兽 (snow beast): a white yak-like neutral animal of the frost realm.
 *
 * <p>Built on the vanilla goat so it inherits the goat model and the sure footed movement, but the
 * goat's ramming and hostility goals are removed: this is a placid grazer that only fights back when it
 * is attacked. 30 health, 2 attack damage, and cow-like drops (leather and its own meat).</p>
 */
public class SnowBeast extends Goat {
    public SnowBeast(EntityType<? extends Goat> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder beastAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // strip the goat behaviour: no ramming, no jumping at players, no screaming
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // neutral: it only retaliates against whatever hit it
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    /**
     * The vanilla goat shrinks to 70% while it is in the {@code long_jumping} pose. The snow beast never
     * jumps like that, and a shrunken hitbox is exactly what made some beasts impossible to hit, so the
     * size is pinned here.
     */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return net.minecraft.world.entity.EntityDimensions.scalable(1.6F, 1.9F)
                .scale(this.getAgeScale());
    }

    /** Never hostile on its own, even when the player gets close. */
    @Override
    public boolean canAttackType(net.minecraft.world.entity.EntityType<?> type) {
        return super.canAttackType(type);
    }
}
