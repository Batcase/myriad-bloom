package com.myriadbloom.world.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The area frozen by a running 时间凝滞 (time stasis) event.
 *
 * <p>One zone per level at most, so the bookkeeping cannot grow. Worldgen/block-entity mixins and the
 * entity handlers all ask this class before letting anything tick, which keeps the frozen state in one
 * place instead of scattering flags through the code.</p>
 */
public final class StasisZone {
    private static final Map<ResourceKey<Level>, Zone> ZONES = new ConcurrentHashMap<>();

    private StasisZone() {
    }

    /** A frozen sphere around {@code centre} that lasts until {@code endGameTime}. */
    public record Zone(Vec3 centre, double radius, long endGameTime) {
        public double radiusSquared() {
            return this.radius * this.radius;
        }
    }

    public static void set(Level level, Vec3 centre, double radius, long endGameTime) {
        ZONES.put(level.dimension(), new Zone(centre, radius, endGameTime));
    }

    public static void clear(Level level) {
        ZONES.remove(level.dimension());
    }

    /** Short description of the live zones, for {@code /myriadbloom cache_info}. */
    public static String stateReport() {
        StringBuilder builder = new StringBuilder();
        ZONES.forEach((dimension, zone) -> builder.append(dimension.location().getPath())
                .append(':').append((int) zone.radius()).append(' '));
        return builder.toString().trim();
    }

    public static void clearAll() {
        ZONES.clear();
    }

    /** True while any zone is running in this level; cheap enough for the per tick mixin guards. */
    public static boolean isActive(Level level) {
        Zone zone = ZONES.get(level.dimension());
        return zone != null && level.getGameTime() < zone.endGameTime();
    }

    public static boolean isFrozen(Level level, Vec3 position) {
        Zone zone = ZONES.get(level.dimension());
        if (zone == null || level.getGameTime() >= zone.endGameTime()) {
            return false;
        }
        return position.distanceToSqr(zone.centre()) <= zone.radiusSquared();
    }

    public static boolean isFrozen(Level level, BlockPos pos) {
        Zone zone = ZONES.get(level.dimension());
        if (zone == null || level.getGameTime() >= zone.endGameTime()) {
            return false;
        }
        double dx = pos.getX() + 0.5D - zone.centre().x;
        double dy = pos.getY() + 0.5D - zone.centre().y;
        double dz = pos.getZ() + 0.5D - zone.centre().z;
        return dx * dx + dy * dy + dz * dz <= zone.radiusSquared();
    }
}
