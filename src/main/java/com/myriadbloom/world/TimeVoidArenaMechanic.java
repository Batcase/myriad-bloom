package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 时之虚空的封闭场地：64x64 的时之边界平台，四周围上时之结界，顶上也封一块，形成一个盒子。
 *
 * <p>场地只在有玩家进入维度时建造一次，记录在 {@link Built} 里。</p>
 */
public final class TimeVoidArenaMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("time_void_arena");
    public static final ResourceKey<Level> TIME_VOID = ResourceKey.create(Registries.DIMENSION,
            MyriadBloomApi.id("time_void"));
    /** 平台半边长：64x64。 */
    public static final int HALF = 32;
    public static final int FLOOR_Y = 64;
    /** 墙高。 */
    public static final int WALL_HEIGHT = 24;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!level.dimension().equals(TIME_VOID)) {
            return;
        }
        if (level.players().isEmpty()) {
            return;
        }
        Built built = Built.of(level);
        if (built.done) {
            return;
        }
        build(level);
        built.done = true;
        built.setDirty();
        MyriadBloom.LOGGER.info("Time void arena built at {}", level.dimension().location());
    }

    private static void build(ServerLevel level) {
        var boundary = com.myriadbloom.TimeVoidContent.TIME_BOUNDARY.get().defaultBlockState();
        var barrier = com.myriadbloom.TimeVoidContent.TIME_BARRIER.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int ceilingY = FLOOR_Y + WALL_HEIGHT + 1;
        for (int x = -HALF; x < HALF; x++) {
            for (int z = -HALF; z < HALF; z++) {
                level.setBlock(pos.set(x, FLOOR_Y, z), boundary, 3);
                level.setBlock(pos.set(x, ceilingY, z), barrier, 3);
            }
        }
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + WALL_HEIGHT; y++) {
            for (int i = -HALF; i < HALF; i++) {
                level.setBlock(pos.set(i, y, -HALF), barrier, 3);
                level.setBlock(pos.set(i, y, HALF - 1), barrier, 3);
                level.setBlock(pos.set(-HALF, y, i), barrier, 3);
                level.setBlock(pos.set(HALF - 1, y, i), barrier, 3);
            }
        }
    }

    @Override
    public void forgetPlayer(UUID playerId) {
    }

    @Override
    public String debugState() {
        return "time_void_arena";
    }

    /** 记录场地是否已经建好。 */
    public static final class Built extends SavedData {
        private static final String NAME = MyriadBloom.MODID + "_time_void_arena";
        private boolean done;
        private static final Built EMPTY = new Built();

        private static Built load(CompoundTag tag, HolderLookup.Provider provider) {
            Built data = new Built();
            data.done = tag.getBoolean("done");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("done", this.done);
            return tag;
        }

        public static Built of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Built::new, Built::load), NAME);
        }
    }
}
