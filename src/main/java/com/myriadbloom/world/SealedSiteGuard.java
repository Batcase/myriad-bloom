package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 已建成的封印之地记录：以区域中心为键，防止区块卸载后重复建造、刷出多个魔法阵。
 */
public final class SealedSiteGuard extends SavedData {
    private static final String DATA_NAME = MyriadBloom.MODID + "_sealed_sites";
    private final Set<Long> built = new HashSet<>();

    public static SealedSiteGuard of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SealedSiteGuard::new, SealedSiteGuard::load), DATA_NAME);
    }

    private static SealedSiteGuard load(CompoundTag tag, HolderLookup.Provider provider) {
        SealedSiteGuard guard = new SealedSiteGuard();
        ListTag list = tag.getList("built", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            guard.built.add(key(entry.getInt("x"), entry.getInt("z")));
        }
        return guard;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (long packed : this.built) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", (int) (packed >> 32));
            entry.putInt("z", (int) packed);
            list.add(entry);
        }
        tag.put("built", list);
        return tag;
    }

    public boolean isBuilt(int centreX, int centreZ) {
        return this.built.contains(key(centreX, centreZ));
    }

    public void markBuilt(int centreX, int centreZ) {
        if (this.built.add(key(centreX, centreZ))) {
            setDirty();
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
