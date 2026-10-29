package com.myriadbloom.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-level record of "big regions": a 16x16 chunk area (256x256 blocks) and every notable structure
 * that was placed inside it. The data is saved with the world so a future in-game map can point at
 * the buildings, and it is also what enforces the per region fortress cap.
 *
 * <p>One ledger per level: the frost realm tracks fortresses, the candy dimension tracks lollipops.
 * Regions are addressed by a packed (regionX, regionZ) long.</p>
 */
public class RegionLedger extends SavedData {
    /** Region edge in chunks: 16x16 chunks. */
    public static final int REGION_CHUNKS = 16;
    private static final int REGION_BLOCKS = REGION_CHUNKS * 16;
    private static final String FILE_NAME = "myriadbloom_regions";
    /** At most this many snow fortresses may share one region. */
    public static final int MAX_FORTRESSES_PER_REGION = 9;

    /** One recorded building: its kind ("fortress", "lollipop", ...) and position. */
    public record Entry(String kind, BlockPos pos) {
    }

    private final Map<Long, List<Entry>> regions = new HashMap<>();

    public static RegionLedger of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RegionLedger::new, RegionLedger::load), FILE_NAME);
    }

    private static RegionLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        RegionLedger ledger = new RegionLedger();
        ListTag regionsTag = tag.getList("Regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < regionsTag.size(); i++) {
            CompoundTag regionTag = regionsTag.getCompound(i);
            long key = regionTag.getLong("Key");
            ListTag entriesTag = regionTag.getList("Entries", Tag.TAG_COMPOUND);
            List<Entry> entries = new ArrayList<>(entriesTag.size());
            for (int j = 0; j < entriesTag.size(); j++) {
                CompoundTag entryTag = entriesTag.getCompound(j);
                entries.add(new Entry(entryTag.getString("Kind"),
                        new BlockPos(entryTag.getInt("X"), entryTag.getInt("Y"), entryTag.getInt("Z"))));
            }
            ledger.regions.put(key, entries);
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag regionsTag = new ListTag();
        this.regions.forEach((key, entries) -> {
            CompoundTag regionTag = new CompoundTag();
            regionTag.putLong("Key", key);
            ListTag entriesTag = new ListTag();
            for (Entry entry : entries) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("Kind", entry.kind());
                entryTag.putInt("X", entry.pos().getX());
                entryTag.putInt("Y", entry.pos().getY());
                entryTag.putInt("Z", entry.pos().getZ());
                entriesTag.add(entryTag);
            }
            regionTag.put("Entries", entriesTag);
            regionsTag.add(regionTag);
        });
        tag.put("Regions", regionsTag);
        return tag;
    }

    /** Packed region key of a position. */
    public static long regionKey(BlockPos pos) {
        int regionX = Math.floorDiv(pos.getX(), REGION_BLOCKS);
        int regionZ = Math.floorDiv(pos.getZ(), REGION_BLOCKS);
        return ((long) regionX & 0xFFFFFFFFL) << 32 | (regionZ & 0xFFFFFFFFL);
    }

    /** Region corner position, for the future map. */
    public static BlockPos regionCorner(long key) {
        int regionX = (int) (key >> 32);
        int regionZ = (int) key;
        return new BlockPos(regionX * REGION_BLOCKS, 0, regionZ * REGION_BLOCKS);
    }

    public int count(String kind, BlockPos pos) {
        List<Entry> entries = this.regions.get(regionKey(pos));
        if (entries == null) {
            return 0;
        }
        int count = 0;
        for (Entry entry : entries) {
            if (entry.kind().equals(kind)) {
                count++;
            }
        }
        return count;
    }

    public boolean canPlaceFortress(BlockPos pos) {
        return count("fortress", pos) < MAX_FORTRESSES_PER_REGION;
    }

    public synchronized void record(String kind, BlockPos pos) {
        this.regions.computeIfAbsent(regionKey(pos), key -> new ArrayList<>()).add(new Entry(kind, pos.immutable()));
        this.setDirty();
    }

    /** Every recorded position of one kind, across all regions. */
    public synchronized List<Entry> allEntries(String kind) {
        List<Entry> result = new ArrayList<>();
        for (List<Entry> entries : this.regions.values()) {
            for (Entry entry : entries) {
                if (entry.kind().equals(kind)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /** Recorded buildings of one kind in the region containing {@code pos}. */
    public List<Entry> entriesIn(String kind, BlockPos pos) {
        List<Entry> entries = this.regions.get(regionKey(pos));
        if (entries == null) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.kind().equals(kind)) {
                result.add(entry);
            }
        }
        return result;
    }

    private RegionLedger() {
    }
}
