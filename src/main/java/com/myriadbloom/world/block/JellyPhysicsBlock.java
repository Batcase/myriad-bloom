package com.myriadbloom.world.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * The physics of jelly, without the colour block state.
 *
 * <p>Jelly blocks used to be a single block with a {@code color} property. Builders could not get a
 * specific colour from the creative inventory that way, so every colour is now its own registered
 * block; this class carries the shared behaviour:</p>
 *
 * <ul>
 *   <li>translucent rendering comes from the block models ({@code render_type: translucent});</li>
 *   <li>walking on it costs 20% movement speed ({@link #getSpeedFactor()} = 0.8);</li>
 *   <li>landing on it bounces you back up.</li>
 * </ul>
 */
public class JellyPhysicsBlock extends Block {
    public JellyPhysicsBlock(Properties properties) {
        super(properties);
    }

    /** -20% movement speed while walking on jelly. */
    @Override
    public float getSpeedFactor() {
        return 0.8F;
    }

    /** Springy landing: falling onto jelly bounces the entity back up. */
    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            return;
        }
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < -0.08D) {
            entity.setDeltaMovement(movement.x, -movement.y * 0.55D, movement.z);
            entity.resetFallDistance();
        }
    }
}
