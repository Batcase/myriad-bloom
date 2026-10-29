package com.myriadbloom.world.block;

import com.myriadbloom.world.ThinIceMechanic;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 易碎冰块 (thin ice).
 *
 * <p>A living entity standing on it for three seconds breaks it into water; the water turns back into
 * thin ice after five minutes (handled by {@link ThinIceMechanic}). It is not collectable - digging or
 * blasting it never drops anything - but it is easy to break, glass hardness.</p>
 *
 * <p>Two entry points are needed: {@link #stepOn} fires the moment an entity walks onto the top face
 * (its bounding box starts exactly at the block's top, so {@code entityInside} never sees it), and
 * {@link #entityInside} covers entities whose box actually overlaps the block. Both only schedule the
 * countdown once - rescheduling every tick would push the tick into the future forever.</p>
 */
public class ThinIceBlock extends Block {
    /** Ticks a mob has to stand on the block before it gives way. */
    public static final int BREAK_TICKS = 60;

    public static final MapCodec<ThinIceBlock> CODEC = simpleCodec(ThinIceBlock::new);

    public ThinIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        scheduleBreak(level, pos, entity);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        scheduleBreak(level, pos, entity);
    }

    private void scheduleBreak(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.isSpectator() || !entity.isAlive()) {
            return;
        }
        if (level instanceof ServerLevel server && server.getBlockTicks().hasScheduledTick(pos, this)) {
            // the countdown is already running: keep it anchored to the first contact
            return;
        }
        level.scheduleTick(pos, this, com.myriadbloom.FrostConfig.THIN_ICE_BREAK_TICKS.get());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        if (!occupied(level, pos)) {
            // nobody left: the next stepOn/entityInside starts a fresh countdown
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        ThinIceMechanic.scheduleRestore(level, pos);
        // the server has already replaced the block, but the client keeps the entity standing on the
        // old collision until it gets a velocity update: nudge everything that stood on it so it drops
        // into the water immediately instead of hanging in the air until the player moves
        AABB above = new AABB(pos.getX(), pos.getY() + 0.6D, pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.6D, pos.getZ() + 1.0D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, above,
                entity -> !entity.isSpectator() && entity.isAlive())) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(motion.y, -0.25D), motion.z);
            entity.hurtMarked = true;
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                player.connection.teleport(player.getX(), player.getY() - 0.05D, player.getZ(),
                        player.getYRot(), player.getXRot());
            }
        }
    }

    /** True when a living entity stands on the top face or is inside the block. */
    private static boolean occupied(ServerLevel level, BlockPos pos) {
        AABB standing = new AABB(pos.getX(), pos.getY() + 0.85D, pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.45D, pos.getZ() + 1.0D);
        return !level.getEntitiesOfClass(Entity.class, standing,
                entity -> !entity.isSpectator() && entity.isAlive()).isEmpty();
    }
}
