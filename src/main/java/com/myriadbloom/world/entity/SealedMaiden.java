package com.myriadbloom.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 被封印的少女 (sealed maiden): a still figure inside the eternal ice.
 *
 * <p>No AI, no gravity, never despawns and cannot be hurt - she is part of the scenery until the seal
 * is broken.</p>
 */
public class SealedMaiden extends Mob {
    public SealedMaiden(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.setSilent(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder maidenAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
