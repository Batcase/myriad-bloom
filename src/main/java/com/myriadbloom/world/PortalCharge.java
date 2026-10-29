package com.myriadbloom.world;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/**
 * Per-entity charging state for the frost portals.
 *
 * <p>A portal does not teleport instantly any more: the entity has to stay inside it while
 * snowflakes gather around it, and only when the charge completes does it travel. Stepping out
 * cancels the charge.</p>
 *
 * <p>{@code armed = false} means "this entity arrived through a portal and has not stepped off
 * yet", which stops it being bounced straight back. Server thread only.</p>
 */
public final class PortalCharge {
    /** Portals only ever charge a handful of entities at a time: a small LRU stops UUIDs piling up. */
    private static final int MAX_TRACKED = 512;
    private static final Map<UUID, State> STATES = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, State> eldest) {
            return size() > MAX_TRACKED;
        }
    };

    /** Number of tracked entities, for the cache report. */
    public static int size() {
        synchronized (STATES) {
            return STATES.size();
        }
    }

    /** Drops every charge state, e.g. when the server stops. */
    public static void clearCaches() {
        synchronized (STATES) {
            STATES.clear();
        }
    }

    public static State get(Entity entity) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(entity.getUUID(), ignored -> new State());
        }
    }

    /** Called after a successful trip: the entity must step off before it can charge again. */
    public static void markArrived(Entity entity) {
        State state = new State();
        state.armed = false;
        synchronized (STATES) {
            STATES.put(entity.getUUID(), state);
        }
    }

    /** Called when the entity leaves the portal, or after a completed charge. */
    public static void cancel(Entity entity) {
        synchronized (STATES) {
            STATES.remove(entity.getUUID());
        }
    }

    public static final class State {
        public int ticks;
        public boolean armed = true;
    }

    private PortalCharge() {
    }
}
