package com.myriadbloom.world;

import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.EnvironmentMechanic;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.entity.FrostBlaze;
import com.myriadbloom.world.gen.CongealedLandFeature;
import com.myriadbloom.world.gen.StormMaze;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 凝结之地地下的四座迷宫。
 *
 * <p>迷宫横跨几百个区块，没法用世界生成特征（特征只能在当前区块写），所以改成逐区块填充：只填充
 * 已经生成的区块，每 tick 两三个，进度存在 {@link MazeGuard} 里。这样既不会卡住服务器，也不会
 * 因为强行生成区块触发看门狗。</p>
 *
 * <p>寒霜神不用刷怪笼：只要玩家在迷宫高度附近，就按固定间隔在附近通道里自然生成，并限制身边同时
 * 存在的数量。</p>
 */
public final class StormMazeMechanic implements EnvironmentMechanic {
    private static final ResourceLocation ID = MyriadBloomApi.id("storm_maze");
    /** 每 tick 填充多少区块。 */
    private static final int CHUNKS_PER_TICK = 2;
    /** 玩家距离区域中心多远才开始填充。 */
    private static final double ACTIVATION_RANGE = 1100.0D;
    /** 自然刷新间隔（tick）。 */
    private static final int SPAWN_INTERVAL = 60;
    /** 每次尝试的落点数。 */
    private static final int SPAWN_ATTEMPTS = 8;
    /** 身边同时存在的寒霜神上限。 */
    private static final int SPAWN_CAP = 12;
    private static final double SPAWN_MIN = 10.0D;
    private static final double SPAWN_MAX = 40.0D;

    private static final Set<Long> FORCED = ConcurrentHashMap.newKeySet();

    private final Map<Long, StormMaze.Layout> layouts = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> queues = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void onLevelTick(ServerLevel level) {
        if (!MyriadBloomApi.isMyriadBloom(level)) {
            return;
        }
        for (Long forced : FORCED) {
            int centreX = (int) (forced.longValue() >> 32);
            int centreZ = forced.intValue();
            StormMaze.Layout layout = this.layouts.computeIfAbsent(forced,
                    key -> StormMaze.layout(centreX, centreZ));
            if (tickQueue(level, forced, layout, 3)) {
                FORCED.remove(forced);
            }
        }
        ServerPlayer anchor = level.players().stream().findFirst().orElse(null);
        if (anchor == null) {
            return;
        }
        int[] centre = CongealedLandFeature.patchCentre(anchor.blockPosition().getX() >> 4,
                anchor.blockPosition().getZ() >> 4);
        double dx = anchor.getX() - centre[0];
        double dz = anchor.getZ() - centre[1];
        if (dx * dx + dz * dz > ACTIVATION_RANGE * ACTIVATION_RANGE) {
            return;
        }
        // 没有风暴林地的区域（比如落在海洋上被跳过）不要建迷宫
        // 只在该区域确实是风暴林地时建迷宫：落在海洋上的区域会被群系判定跳过。
        // 这里只查已加载的区块，避免为了判断群系而强行生成地形。
        long regionKey = regionKey(centre[0], centre[1]);
        StormMaze.Layout layout = this.layouts.computeIfAbsent(regionKey,
                key -> StormMaze.layout(centre[0], centre[1]));
        // 封印之山建在凝结之地中心，和风暴林地无关，所以先处理
        ensureSealedMountain(level, layout);
        com.myriadbloom.world.gen.SealedMountain.tick(level);
        // 迷宫只在确实是风暴林地的区域建
        if (!level.hasChunk(centre[0] >> 4, centre[1] >> 4)
                || !level.getBiome(new BlockPos(centre[0], StormMaze.FLOOR_Y + 8, centre[1]))
                        .unwrapKey()
                        .map(key -> key.location().getPath().equals("frost_storm_woodland"))
                        .orElse(false)) {
            return;
        }
        tickQueue(level, regionKey, layout, CHUNKS_PER_TICK);
        tickBossRoom(level, layout);
    }

    /** 凝结之地中心的封印之山：没有少女就建一次（幂等）。 */
    private void ensureSealedMountain(ServerLevel level, StormMaze.Layout layout) {
        var box = new net.minecraft.world.phys.AABB(
                layout.centreX - 12, StormMaze.FLOOR_Y, layout.centreZ - 12,
                layout.centreX + 12, 320, layout.centreZ + 12);
        if (!level.getEntitiesOfClass(com.myriadbloom.world.entity.SealedMaiden.class, box).isEmpty()) {
            return;
        }
        if (com.myriadbloom.world.gen.SealedMountain.isBuilding(layout.centreX, layout.centreZ)) {
            return;
        }
        if (level.hasChunk(layout.centreX >> 4, layout.centreZ >> 4)) {
            com.myriadbloom.world.gen.SealedMountain.start(level, layout.centreX, layout.centreZ);
        }
    }

    /** 玩家踏进中央厅时唤醒风暴棱镜，并击碎厅里的热源。 */
    private void tickBossRoom(ServerLevel level, StormMaze.Layout layout) {
        BlockPos socket = new BlockPos(layout.centreX, StormMaze.FLOOR_Y + 1, layout.centreZ);
        if (!level.getBlockState(socket).is(com.myriadbloom.StormWoodlandContent.STORM_PRISM_SPAWNER.get())) {
            return;
        }
        ServerPlayer intruder = level.players().stream()
                .filter(player -> player.distanceToSqr(layout.centreX + 0.5D,
                        StormMaze.FLOOR_Y + 1.0D, layout.centreZ + 0.5D) < 30.0D * 30.0D)
                .findFirst().orElse(null);
        if (intruder == null) {
            return;
        }
        com.myriadbloom.world.entity.StormPrism boss =
                com.myriadbloom.FrostMobs.STORM_PRISM.get().create(level);
        if (boss == null) {
            return;
        }
        boss.moveTo(layout.centreX + 0.5D, StormMaze.FLOOR_Y + 1.0D, layout.centreZ + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(boss);
        level.setBlock(socket, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                socket.getX() + 0.5D, socket.getY() + 0.5D, socket.getZ() + 0.5D, 60, 2.0D, 2.0D, 2.0D, 0.15D);
        level.playSound(null, socket, net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        shatterHeatSources(level, layout);
    }

    /** 被动：风暴一起，厅里的热源全部被摧毁。 */
    private void shatterHeatSources(ServerLevel level, StormMaze.Layout layout) {
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                com.myriadbloom.api.MyriadBloomApi.id("heat_sources"));
        for (int dx = -28; dx <= 28; dx++) {
            for (int dz = -28; dz <= 28; dz++) {
                if (dx * dx + dz * dz > 28 * 28) {
                    continue;
                }
                for (int dy = 0; dy <= 10; dy++) {
                    BlockPos pos = new BlockPos(layout.centreX + dx, StormMaze.FLOOR_Y + 1 + dy,
                            layout.centreZ + dz);
                    if (level.getBlockState(pos).is(tag)) {
                        level.destroyBlock(pos, false);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.3D, 0.3D, 0.3D,
                                0.02D);
                    }
                }
            }
        }
    }

    /** 在迷宫附近自然刷新寒霜神：没有刷怪笼。 */
    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!MyriadBloomApi.isMyriadBloom(player.serverLevel())) {
            return;
        }
        if (player.tickCount % SPAWN_INTERVAL != 0) {
            return;
        }
        // 只在地下迷宫的高度附近刷新
        if (player.getY() > StormMaze.FLOOR_Y + 40 || player.getY() < StormMaze.FLOOR_Y - 8) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int[] centre = CongealedLandFeature.patchCentre(player.blockPosition().getX() >> 4,
                player.blockPosition().getZ() >> 4);
        StormMaze.Layout layout = this.layouts.computeIfAbsent(regionKey(centre[0], centre[1]),
                key -> StormMaze.layout(centre[0], centre[1]));
        int nearby = level.getEntitiesOfClass(FrostBlaze.class,
                player.getBoundingBox().inflate(64.0D)).size();
        if (nearby >= SPAWN_CAP) {
            return;
        }
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = SPAWN_MIN + level.random.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (layout.kindAt(x, z) != StormMaze.Kind.OPEN) {
                continue;
            }
            int y = StormMaze.FLOOR_Y + 1;
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()
                    || !level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
                continue;
            }
            FrostBlaze blaze = com.myriadbloom.FrostMobs.FROST_BLAZE.get().create(level);
            if (blaze == null) {
                return;
            }
            blaze.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(blaze);
            return;
        }
    }

    /** 填一批区块，返回 true 表示这个区域已经全部填完。 */
    private boolean tickQueue(ServerLevel level, long regionKey, StormMaze.Layout layout, int budget) {
        List<Long> queue = this.queues.computeIfAbsent(regionKey, key -> buildQueue(layout));
        MazeGuard guard = MazeGuard.of(level);
        int built = 0;
        for (Long packed : queue) {
            if (built >= budget) {
                return false;
            }
            int cx = (int) (packed.longValue() >> 32);
            int cz = packed.intValue();
            if (guard.isBuilt(cx, cz)) {
                continue;
            }
            // 不在这里强行生成区块：生成一片风暴林地要好几秒，会触发看门狗
            if (!level.hasChunk(cx, cz)) {
                continue;
            }
            StormMazeBuilder.buildChunk(level, layout, cx, cz);
            guard.markBuilt(cx, cz);
            built++;
        }
        return true;
    }

    /** 迷宫、中央厅和四条入口会碰到的所有区块。 */
    public static List<Long> buildQueue(StormMaze.Layout layout) {
        Set<Long> chunks = new HashSet<>();
        int reach = StormMaze.GAP + StormMaze.LENGTH + 40;
        for (int x = layout.centreX - reach; x <= layout.centreX + reach; x += 16) {
            for (int z = layout.centreZ - reach; z <= layout.centreZ + reach; z += 16) {
                chunks.add(key(x >> 4, z >> 4));
            }
        }
        for (StormMaze.Arm arm : StormMaze.Arm.values()) {
            int[] entrance = layout.entrance(arm);
            for (int x = entrance[0] - 24; x <= entrance[0] + 24; x += 16) {
                for (int z = entrance[1] - 24; z <= entrance[1] + 24; z += 16) {
                    chunks.add(key(x >> 4, z >> 4));
                }
            }
        }
        List<Long> queue = new ArrayList<>(chunks);
        queue.sort((a, b) -> {
            int ax = (int) (a.longValue() >> 32);
            int az = a.intValue();
            int bx = (int) (b.longValue() >> 32);
            int bz = b.intValue();
            int da = Math.abs(ax - (layout.centreX >> 4)) + Math.abs(az - (layout.centreZ >> 4));
            int db = Math.abs(bx - (layout.centreX >> 4)) + Math.abs(bz - (layout.centreZ >> 4));
            return Integer.compare(da, db);
        });
        return queue;
    }

    /** 把某个区域排队，让机制在后台填完。 */
    public static void force(int centreX, int centreZ) {
        FORCED.add(regionKey(centreX, centreZ));
    }

    public static boolean isForced(int centreX, int centreZ) {
        return FORCED.contains(regionKey(centreX, centreZ));
    }

    /** 描述某个区域的迷宫，用于调试命令。 */
    public static String describe(ServerLevel level, int blockX, int blockZ) {
        int[] centre = CongealedLandFeature.patchCentre(blockX >> 4, blockZ >> 4);
        StormMaze.Layout layout = StormMaze.layout(centre[0], centre[1]);
        int chests = 0;
        for (StormMaze.ArmMaze maze : layout.mazes) {
            chests += maze.chests.size();
        }
        StringBuilder text = new StringBuilder();
        text.append("maze centre=").append(centre[0]).append(",").append(centre[1])
                .append(" mazes=4 size=").append(StormMaze.LENGTH).append("x").append(StormMaze.WIDTH)
                .append(" floorY=").append(StormMaze.FLOOR_Y)
                .append(" bossHall=radius ").append(StormMaze.BOSS_RADIUS)
                .append(" chests=").append(chests);
        for (StormMaze.Arm arm : StormMaze.Arm.values()) {
            int[] entrance = layout.entrance(arm);
            text.append(" | ").append(arm.name().toLowerCase()).append("Entrance=")
                    .append(entrance[0]).append(",").append(entrance[1]);
        }
        text.append(" builtChunks=").append(MazeGuard.of(level).size());
        return text.toString();
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static long regionKey(int centreX, int centreZ) {
        return ((long) centreX << 32) | (centreZ & 0xFFFFFFFFL);
    }

    @Override
    public void forgetPlayer(UUID playerId) {
        this.layouts.clear();
        this.queues.clear();
    }

    @Override
    public String debugState() {
        return "storm_maze.regions=" + this.layouts.size() + " queues=" + this.queues.size();
    }

    /** 已建区块的记录。 */
    public static final class MazeGuard extends SavedData {
        private static final String DATA_NAME = MyriadBloom.MODID + "_storm_maze";
        private final Set<Long> built = new HashSet<>();

        public static MazeGuard of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(MazeGuard::new, MazeGuard::load), DATA_NAME);
        }

        private static MazeGuard load(CompoundTag tag, HolderLookup.Provider provider) {
            MazeGuard guard = new MazeGuard();
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

        public boolean isBuilt(int chunkX, int chunkZ) {
            return this.built.contains(key(chunkX, chunkZ));
        }

        public void markBuilt(int chunkX, int chunkZ) {
            if (this.built.add(key(chunkX, chunkZ))) {
                setDirty();
            }
        }

        public int size() {
            return this.built.size();
        }
    }
}
