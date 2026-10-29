package com.myriadbloom.world;

import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

/**
 * Spawns the cod of a frozen lake on the main thread.
 *
 * <p>Worldgen threads must not spawn entities: {@code EntityType#spawn} needs the chunk, which makes a
 * worldgen worker wait for the server thread that is itself waiting for the chunk - a deadlock that
 * shows up as a watchdog crash. The lake feature only queues the position here, and the level tick
 * spawns the fish once the chunk is loaded and the server thread owns it.</p>
 */
public class LakePopulator implements EnvironmentMechanic {
    /** Upper bound of queued fish spawns per dimension, so an ocean of generated lakes cannot pile up. */
    private static final int MAX_PENDING = 4096;
    private static final Map<ResourceKey<Level>, Queue<BlockPos>> PENDING = new ConcurrentHashMap<>();

    /** Queue sizes per dimension, for the cache report. */
    public static String cacheReport() {
        StringBuilder builder = new StringBuilder("lake.queued=");
        PENDING.forEach((dimension, queue) -> builder.append(dimension.location().getPath())
                .append(':').append(queue.size()).append(' '));
        return builder.toString().trim();
    }

    /** Frees the queues on server stop. */
    public static void clearCaches() {
        PENDING.clear();
    }

    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("lake_populator");
    }

    /** Called from worldgen: only touches a concurrent queue, never the level. */
    public static void queue(ServerLevel level, BlockPos waterPos) {
        Queue<BlockPos> queue = PENDING.computeIfAbsent(level.dimension(),
                key -> new ConcurrentLinkedQueue<>());
        if (queue.size() >= MAX_PENDING) {
            // the queue is only a convenience for spawning fish: dropping an entry is harmless
            return;
        }
        queue.add(waterPos.immutable());
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        Queue<BlockPos> queue = PENDING.get(level.dimension());
        if (queue == null || queue.isEmpty() || level.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos pos;
        while ((pos = queue.peek()) != null) {
            if (!level.isLoaded(pos)) {
                return;
            }
            queue.poll();
            EntityType.COD.spawn(level, pos, MobSpawnType.STRUCTURE);
            if (level.getRandom().nextBoolean()) {
                EntityType.COD.spawn(level, pos.offset(1, 0, 0), MobSpawnType.STRUCTURE);
            }
        }
    }
}
