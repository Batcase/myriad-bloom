package com.myriadbloom.world;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Remembers which frozen ocean arenas are still locked.
 *
 * <p>While an arena is sealed its floor of congealed ice cannot be broken - that is how the fight is kept
 * inside the arena until 极冰聚合物 dies, at which point {@link #release(BlockPos)} unlocks it.</p>
 */
public class ArenaGuard extends SavedData {
    private static final String DATA_NAME = "myriadbloom_ice_arenas";
    /** centre -> radius of the sealed floor. */
    private final Map<BlockPos, Integer> sealed = new HashMap<>();
    /** Patch cells that already have a built arena (bounded: a server cannot keep every cell forever). */
    private static final int MAX_BUILT_CELLS = 4096;
    private final java.util.Set<Long> builtCells = new java.util.LinkedHashSet<>();

    /** Evicts the oldest cell once the record grows past its cap. */
    private void trimBuiltCells() {
        while (this.builtCells.size() > MAX_BUILT_CELLS) {
            java.util.Iterator<Long> oldest = this.builtCells.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    public static ArenaGuard of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaGuard::new, ArenaGuard::load), DATA_NAME);
    }

    public static ArenaGuard load(CompoundTag tag, HolderLookup.Provider registries) {
        ArenaGuard data = new ArenaGuard();
        long[] built = tag.getLongArray("Built");
        for (long key : built) {
            data.builtCells.add(key);
        }
        ListTag list = tag.getList("Arenas", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.sealed.put(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")),
                    entry.getInt("R"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.sealed.forEach((pos, radius) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            entry.putInt("R", radius);
            list.add(entry);
        });
        tag.put("Arenas", list);
        tag.putLongArray("Built", this.builtCells.stream().mapToLong(Long::longValue).sorted().toArray());
        return tag;
    }

    public synchronized void seal(BlockPos centre, int radius) {
        this.sealed.put(centre.immutable(), radius);
        this.setDirty();
    }

    public synchronized boolean isBuilt(long cellKey) {
        return this.builtCells.contains(cellKey);
    }

    public synchronized void markBuilt(long cellKey) {
        if (this.builtCells.add(cellKey)) {
            this.trimBuiltCells();
            this.setDirty();
        }
    }

    /** Unlocks every arena whose centre is near the given position. */
    public synchronized void release(BlockPos near) {
        boolean changed = this.sealed.keySet().removeIf(centre -> centre.distSqr(near) <= 64 * 64);
        if (changed) {
            this.setDirty();
        }
    }

    /** True when the block sits on a sealed arena floor. */
    public synchronized boolean isSealed(BlockPos block) {
        for (Map.Entry<BlockPos, Integer> entry : this.sealed.entrySet()) {
            BlockPos centre = entry.getKey();
            int radius = entry.getValue();
            if (Math.abs(block.getY() - centre.getY()) <= 12
                    && Math.abs(block.getX() - centre.getX()) <= radius
                    && Math.abs(block.getZ() - centre.getZ()) <= radius) {
                return true;
            }
        }
        return false;
    }
}
