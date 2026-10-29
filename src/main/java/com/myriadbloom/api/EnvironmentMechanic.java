package com.myriadbloom.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension point for content that wants to add rules to the Frost Realm itself
 * (temperature drain, aurora events, blizzards, custom mob behaviour, ...).
 *
 * <p>Mechanics are pure server-side callbacks: they never assume a client is present, so a
 * dedicated server behaves exactly like a single-player world.</p>
 */
public interface EnvironmentMechanic {
    Logger LOGGER = LoggerFactory.getLogger("myriadbloom/mechanic");

    /** Unique id, used for logging and for disabling a mechanic in config later on. */
    ResourceLocation id();

    /** Called once per server tick for every loaded Frost Realm level. */
    default void onLevelTick(ServerLevel level) {
    }

    /** Called once per server tick for every player standing in the Frost Realm. */
    default void onPlayerTick(ServerPlayer player) {
    }

    /** Called when a level of the mod's dimension is loaded (world start / dimension unload-reload). */
    default void onLevelLoad(ServerLevel level) {
    }

    /**
     * Called when a player logs out: mechanics that cache state per player UUID must drop it here,
     * otherwise the cache of a long running server keeps growing with every login.
     */
    default void forgetPlayer(java.util.UUID playerId) {
    }

    /** One line describing the size of this mechanic's caches, for {@code /myriadbloom cache_info}. */
    default String debugState() {
        return "";
    }

    /**
     * Registry of all mechanics. Registration is meant to happen during mod construction;
     * the list keeps insertion order so mechanics execute deterministically.
     */
    final class Registry {
        private static final Map<ResourceLocation, EnvironmentMechanic> MECHANICS = new LinkedHashMap<>();

        private Registry() {
        }

        public static synchronized void register(EnvironmentMechanic mechanic) {
            Objects.requireNonNull(mechanic, "mechanic");
            ResourceLocation id = Objects.requireNonNull(mechanic.id(), "mechanic id");
            if (MECHANICS.putIfAbsent(id, mechanic) != null) {
                throw new IllegalStateException("Duplicate Frost Realm mechanic id: " + id);
            }
        }

        public static synchronized boolean unregister(ResourceLocation id) {
            return MECHANICS.remove(id) != null;
        }

        public static synchronized List<EnvironmentMechanic> all() {
            return Collections.unmodifiableList(new ArrayList<>(MECHANICS.values()));
        }

        public static synchronized void tickLevel(ServerLevel level) {
            for (EnvironmentMechanic mechanic : MECHANICS.values()) {
                try {
                    mechanic.onLevelTick(level);
                } catch (Exception e) {
                    LOGGER.error("Frost Realm mechanic {} failed in onLevelTick", mechanic.id(), e);
                }
            }
        }

        public static synchronized void tickPlayer(ServerPlayer player) {
            for (EnvironmentMechanic mechanic : MECHANICS.values()) {
                try {
                    mechanic.onPlayerTick(player);
                } catch (Exception e) {
                    LOGGER.error("Frost Realm mechanic {} failed in onPlayerTick", mechanic.id(), e);
                }
            }
        }

        /** Broadcasts a logout so every caches keyed by player UUID can be cleaned. */
        public static synchronized void playerLeft(java.util.UUID playerId) {
            for (EnvironmentMechanic mechanic : MECHANICS.values()) {
                try {
                    mechanic.forgetPlayer(playerId);
                } catch (Exception e) {
                    LOGGER.error("Frost Realm mechanic {} failed in forgetPlayer", mechanic.id(), e);
                }
            }
        }

        public static synchronized void levelLoaded(ServerLevel level) {
            for (EnvironmentMechanic mechanic : MECHANICS.values()) {
                try {
                    mechanic.onLevelLoad(level);
                } catch (Exception e) {
                    LOGGER.error("Frost Realm mechanic {} failed in onLevelLoad", mechanic.id(), e);
                }
            }
        }
    }
}
