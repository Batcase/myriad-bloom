package com.myriadbloom.world;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Turns broken thin ice back into ice after five minutes.
 *
 * <p>Water cannot carry a scheduled tick, so the pending restores live in a {@link SavedData} that is
 * walked once every two seconds. Pending restores survive a reload, so leaving the area (or the world)
 * does not leave a permanent hole in the lake.</p>
 */
public class ThinIceMechanic implements EnvironmentMechanic {
    /** Five minutes in ticks. */
    public static final int RESTORE_TICKS = 6000;
    private static final String DATA_NAME = "myriadbloom_thin_ice";

    @Override
    public ResourceLocation id() {
        return MyriadBloomApi.id("thin_ice");
    }

    /** Remembers that the water at {@code pos} should freeze over again in five minutes. */
    public static void scheduleRestore(ServerLevel level, BlockPos pos) {
        PendingRestores.get(level).schedule(pos,
                level.getGameTime() + com.myriadbloom.FrostConfig.THIN_ICE_RESTORE_TICKS.get());
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % 40L != 0L) {
            return;
        }
        PendingRestores.get(level).restoreDue(level);
    }

    /** Saved pending restores of one level. */
    public static final class PendingRestores extends SavedData {
        /** A leaked restore entry would keep a position alive forever: cap the map and age it out. */
        private static final int MAX_PENDING_RESTORES = 8192;
        private final Map<BlockPos, Long> pending = new java.util.LinkedHashMap<>();

        public static PendingRestores get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(PendingRestores::new, PendingRestores::load), DATA_NAME);
        }

        private static PendingRestores load(CompoundTag tag, HolderLookup.Provider registries) {
            PendingRestores data = new PendingRestores();
            ListTag entries = tag.getList("Pending", Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                data.pending.put(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")),
                        entry.getLong("Time"));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag entries = new ListTag();
            this.pending.forEach((pos, time) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("X", pos.getX());
                entry.putInt("Y", pos.getY());
                entry.putInt("Z", pos.getZ());
                entry.putLong("Time", time);
                entries.add(entry);
            });
            tag.put("Pending", entries);
            return tag;
        }

        synchronized void schedule(BlockPos pos, long when) {
            this.pending.put(pos.immutable(), when);
            while (this.pending.size() > MAX_PENDING_RESTORES) {
                java.util.Iterator<java.util.Map.Entry<BlockPos, Long>> oldest = this.pending.entrySet().iterator();
                oldest.next();
                oldest.remove();
            }
            this.setDirty();
        }

        synchronized void restoreDue(ServerLevel level) {
            if (this.pending.isEmpty()) {
                return;
            }
            long now = level.getGameTime();
            List<BlockPos> due = new ArrayList<>();
            this.pending.forEach((pos, time) -> {
                if (now >= time) {
                    due.add(pos);
                }
            });
            if (due.isEmpty()) {
                return;
            }
            for (BlockPos pos : due) {
                this.pending.remove(pos);
                if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.WATER)) {
                    level.setBlockAndUpdate(pos, FrostedLandContent.THIN_ICE.get().defaultBlockState());
                }
            }
            this.setDirty();
        }
    }
}
