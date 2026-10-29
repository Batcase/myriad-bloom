package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Detects a diamond thrown into the water of a snow frame and lights the portal.
 *
 * <p>There is no item-thrown event, so a throttled scan over the level's entities is used. Ten
 * ticks is both cheap (a single list walk per level) and fast enough that the diamond never
 * visibly bounces around before the portal opens.</p>
 */
public final class FrostPortalIgniter {
    private static final int SCAN_INTERVAL_TICKS = 10;

    public static void tickLevel(ServerLevel level) {
        if (!FrostConfig.ENABLE_PORTAL.get()) {
            return;
        }
        if (level.getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        // Collect first, ignite afterwards: igniting discards the item entity, which must not
        // happen while the level's entity list is being iterated.
        List<FrostPortalFrame.Frame> frames = new ArrayList<>();
        List<ItemEntity> triggers = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            if (!FrostPortalFrame.isIgnitionItem(item.getItem())) {
                continue;
            }
            Optional<FrostPortalFrame.Frame> frame = findFrameAround(level, item);
            if (frame.isPresent()) {
                frames.add(frame.get());
                triggers.add(item);
            }
        }

        for (int i = 0; i < frames.size(); i++) {
            FrostPortalFrame.ignite(level, frames.get(i), triggers.get(i));
        }
    }

    private static Optional<FrostPortalFrame.Frame> findFrameAround(ServerLevel level, ItemEntity item) {
        BlockPos pos = item.blockPosition();
        // Items float on the water surface, so check the occupied block and the one below it.
        for (BlockPos candidate : new BlockPos[] {pos, pos.below()}) {
            if (level.getBlockState(candidate).is(Blocks.WATER)) {
                Optional<FrostPortalFrame.Frame> frame = FrostPortalFrame.find(level, candidate);
                if (frame.isPresent()) {
                    return frame;
                }
            }
        }
        return Optional.empty();
    }

    private FrostPortalIgniter() {
    }
}
