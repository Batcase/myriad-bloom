package com.myriadbloom.world.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 时间魔法阵 (magic circle): a huge rotating seal that floats above the eternal ice.
 *
 * <p>Radius is stored on the entity so one entity can cover the whole congealed land. The clock in the
 * middle reads its hands from the world time, so it is a working clock, not a still image.</p>
 */
public class MagicCircle extends Entity {
    /** 半径（格）。 */
    private float radius = 96.0F;

    public MagicCircle(EntityType<? extends MagicCircle> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float radius() {
        return this.radius;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.radius = tag.getFloat("radius");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("radius", this.radius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D * 1024.0D;
    }
}
