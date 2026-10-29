package com.myriadbloom.world.event;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Rolls random events.
 *
 * <p>Every {@code random_event_interval_ticks} ticks each realm level rolls once: with
 * {@code random_event_chance} a random event from {@link RandomEvents#ALL} starts around a random player,
 * provided that event lists this dimension. Only one event per level runs at a time.</p>
 */
public final class RandomEventManager implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("random_events");

    /** Running event per dimension; at most one, so this map is bounded by the level count. */
    private final Map<ResourceKey<Level>, ActiveRandomEvent> running = new HashMap<>();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!FrostConfig.RANDOM_EVENTS_ENABLED.get()) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        ActiveRandomEvent current = this.running.get(dimension);
        if (current != null) {
            if (current.isFinished(level)) {
                current.stop(level);
                this.running.remove(dimension);
                MyriadBloom.LOGGER.info("Random event {} ended in {}", current.type().id(), dimension.location());
            } else {
                current.tick(level);
                return;
            }
        }

        int interval = FrostConfig.RANDOM_EVENT_INTERVAL_TICKS.get();
        if (interval <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }
        if (level.random.nextDouble() > FrostConfig.RANDOM_EVENT_CHANCE.get()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        RandomEventType type = RandomEventType.pick(RandomEvents.ALL, dimension, level.random);
        if (type == null) {
            return;
        }
        ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        int duration = type.randomDuration(level.random);
        ActiveRandomEvent event = new ActiveRandomEvent(type, anchor.position(),
                level.getGameTime() + duration);
        this.running.put(dimension, event);
        event.start(level);
        MyriadBloom.LOGGER.info("Random event {} started in {} for {} ticks", type.id(),
                dimension.location(), duration);
    }

    /** Drops every running event, used on server stop so nothing leaks across worlds. */
    @Override
    public void forgetPlayer(java.util.UUID playerId) {
        // nothing is keyed by player
    }

    @Override
    public String debugState() {
        return "random_events=" + this.running.size() + " stasis=" + com.myriadbloom.world.event.StasisZone.stateReport();
    }

    /** Registry of live managers, so commands can drive them. */
    private static final java.util.List<RandomEventManager> MANAGERS = new java.util.ArrayList<>();

    public RandomEventManager() {
        MANAGERS.add(this);
    }

    /** Hands a manually started event to the manager so it ticks and ends like a rolled one. */
    public static void setRunning(ServerLevel level, ActiveRandomEvent event) {
        for (RandomEventManager manager : MANAGERS) {
            manager.running.put(level.dimension(), event);
        }
    }

    public static boolean stopRunning(ServerLevel level) {
        boolean stopped = false;
        for (RandomEventManager manager : MANAGERS) {
            ActiveRandomEvent event = manager.running.remove(level.dimension());
            if (event != null) {
                event.stop(level);
                stopped = true;
            }
        }
        return stopped;
    }

    /** Rolls once right now at the given spot, ignoring the player requirement (used by commands). */
    public static boolean rollNow(ServerLevel level, net.minecraft.world.phys.Vec3 centre) {
        RandomEventType type = RandomEventType.pick(RandomEvents.ALL, level.dimension(), level.random);
        if (type == null) {
            return false;
        }
        int duration = type.randomDuration(level.random);
        ActiveRandomEvent event = new ActiveRandomEvent(type, centre, level.getGameTime() + duration);
        setRunning(level, event);
        event.start(level);
        MyriadBloom.LOGGER.info("Random event {} started in {} for {} ticks (manual roll)", type.id(),
                level.dimension().location(), duration);
        return true;
    }

    /** Cancels a running event, e.g. when the level unloads. */
    public void cancel(ServerLevel level) {
        ActiveRandomEvent event = this.running.remove(level.dimension());
        if (event != null) {
            event.stop(level);
        }
    }
}
