package com.myriadbloom.world.gen;

import com.myriadbloom.world.entity.MagicCircle;
import com.myriadbloom.world.entity.SealedMaiden;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 封印之山 (sealed mountain): a cone of eternal ice at the centre of the congealed land.
 *
 * <p>建造是渐进式的：每次 tick 只处理少量柱子，避免一次性写入十万格方块卡住服务器。建完之后
 * 才放下浮空的少女和山顶的魔法阵。</p>
 */
public final class SealedMountain {
    /** 底面半径。 */
    public static final int RADIUS = 26;
    /** 高度。 */
    public static final int HEIGHT = 30;
    /** 中央空腔半径。 */
    public static final int CHAMBER = 5;
    /** 每 tick 处理的柱子数。 */
    private static final int COLUMNS_PER_TICK = 320;

    /** 正在建造的山：区域键 -> 任务。 */
    private static final Map<Long, Job> JOBS = new ConcurrentHashMap<>();

    private SealedMountain() {
    }

    /** 一次成型的任务：记录进度。 */
    private static final class Job {
        final int centreX;
        final int centreZ;
        int ground = Integer.MIN_VALUE;
        int index;
        int summit = Integer.MIN_VALUE;

        Job(int centreX, int centreZ) {
            this.centreX = centreX;
            this.centreZ = centreZ;
        }
    }

    /** 排队建山；同一区域只会有一个任务。 */
    public static void start(ServerLevel level, int x, int z) {
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        if (JOBS.containsKey(key)) {
            return;
        }
        JOBS.put(key, new Job(x, z));
    }

    public static boolean isBuilding(int x, int z) {
        return JOBS.containsKey(((long) x << 32) | (z & 0xFFFFFFFFL));
    }

    /** 每个 tick 推进所有在建的山。 */
    public static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) {
            return;
        }
        var ice = com.myriadbloom.StormWoodlandContent.ETERNAL_ICE.get().defaultBlockState();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<Long> finished = new ArrayList<>();
        for (Map.Entry<Long, Job> entry : JOBS.entrySet()) {
            Job job = entry.getValue();
            if (job.ground == Integer.MIN_VALUE) {
                // 区块没加载就读高度会得到 -64，所以等加载后再取
                if (!level.hasChunk(job.centreX >> 4, job.centreZ >> 4)) {
                    continue;
                }
                job.ground = Math.max(63, level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        job.centreX, job.centreZ));
                job.summit = job.ground + HEIGHT + 4;
            }
            int processed = 0;
            int span = RADIUS * 2 + 1;
            while (processed < COLUMNS_PER_TICK) {
                int total = span * span;
                if (job.index >= total) {
                    finished.add(entry.getKey());
                    break;
                }
                int dx = job.index % span - RADIUS;
                int dz = job.index / span - RADIUS;
                job.index++;
                processed++;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > RADIUS) {
                    continue;
                }
                int columnX = job.centreX + dx;
                int columnZ = job.centreZ + dz;
                int groundHere = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        columnX, columnZ) - 1;
                // 每列给一点哈希噪声，让锥面起伏、不完美对称
                int hash = (columnX * 374761393 + columnZ * 668265263) ^ (job.centreX * 31 + job.centreZ);
                hash = (hash ^ (hash >>> 13)) * 1274126177;
                double wobble = ((hash >>> 8) & 0xFF) / 255.0D - 0.5D;
                double ratio = Math.min(1.0D, distance / RADIUS);
                int topOffset = (int) Math.round(HEIGHT * (1.0D - Math.pow(ratio, 1.15D))
                        + wobble * 3.0D * (1.0D - ratio));
                int topY = job.ground + topOffset;
                for (int y = groundHere; y <= topY; y++) {
                    level.setBlock(cursor.set(columnX, y, columnZ), ice, 3);
                }
                if (distance <= CHAMBER) {
                    for (int y = groundHere + 1; y <= job.ground + 22; y++) {
                        level.setBlock(cursor.set(columnX, y, columnZ), air, 3);
                    }
                }
            }
            // 建完后放少女与魔法阵
            boolean done = job.index >= (RADIUS * 2 + 1) * (RADIUS * 2 + 1);
            if (done && !finished.contains(entry.getKey())) {
                finished.add(entry.getKey());
            }
        }
        for (Long key : finished) {
            Job job = JOBS.remove(key);
            if (job == null) {
                continue;
            }
            spawnContents(level, job);
        }
    }

    /** 少女浮在空腔中央，魔法阵悬在锥顶。 */
    private static void spawnContents(ServerLevel level, Job job) {
        SealedMaiden maiden = com.myriadbloom.FrostMobs.SEALED_MAIDEN.get().create(level);
        if (maiden != null) {
            maiden.moveTo(job.centreX + 0.5D, job.ground + 12.0D, job.centreZ + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(maiden);
        }
        MagicCircle circle = com.myriadbloom.FrostMobs.MAGIC_CIRCLE.get().create(level);
        if (circle != null) {
            circle.moveTo(job.centreX + 0.5D, job.summit, job.centreZ + 0.5D, 0.0F, 0.0F);
            circle.setRadius(CongealedLandFeature.PATCH_RADIUS + CongealedLandFeature.MAX_WOBBLE);
            level.addFreshEntity(circle);
        }
    }
}
