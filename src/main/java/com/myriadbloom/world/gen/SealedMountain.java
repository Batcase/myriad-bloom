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
 * 封印之地 (sealed site): a noisy cluster of eternal ice spikes at the centre of the congealed land.
 *
 * <p>中央一根明显更粗更高的冰刺，里面是空腔，少女浮在其中；四周是角度、距离、粗细、高度都被噪声打散的
 * 细冰刺，以及几根矮冰桩。每根刺都从自己脚下的地面长起，魔法阵悬在中央刺上方。</p>
 */
public final class SealedMountain {
    private static final int CORE_RADIUS = 5;
    private static final int CORE_HEIGHT = 30;
    private static final int CHAMBER_RADIUS = 3;
    private static final int CHAMBER_HEIGHT = 20;
    /** 外围冰刺数量。 */
    private static final int SATELLITES = 12;
    /** 每 tick 放置的列数。 */
    private static final int COLUMNS_PER_TICK = 260;

    private static final Map<Long, Job> JOBS = new ConcurrentHashMap<>();

    private SealedMountain() {
    }

    private record Column(int x, int z, int bottom, int top, int hollowTop) {
    }

    private static final class Job {
        final int centreX;
        final int centreZ;
        int ground = Integer.MIN_VALUE;
        List<Column> columns;
        int index;

        Job(int centreX, int centreZ) {
            this.centreX = centreX;
            this.centreZ = centreZ;
        }
    }

    public static void start(ServerLevel level, int x, int z) {
        JOBS.putIfAbsent(((long) x << 32) | (z & 0xFFFFFFFFL), new Job(x, z));
    }

    public static boolean isBuilding(int x, int z) {
        return JOBS.containsKey(((long) x << 32) | (z & 0xFFFFFFFFL));
    }

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
                if (!level.hasChunk(job.centreX >> 4, job.centreZ >> 4)) {
                    continue;
                }
                job.ground = Math.max(63, level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        job.centreX, job.centreZ));
                job.columns = plan(level, job.centreX, job.centreZ, job.ground);
            }
            if (job.columns == null) {
                continue;
            }
            int processed = 0;
            while (job.index < job.columns.size() && processed < COLUMNS_PER_TICK) {
                Column column = job.columns.get(job.index++);
                processed++;
                for (int y = column.bottom(); y <= column.top(); y++) {
                    level.setBlock(cursor.set(column.x(), y, column.z()), ice, 3);
                }
                if (column.hollowTop() > column.bottom()) {
                    for (int y = column.bottom() + 1; y <= column.hollowTop(); y++) {
                        level.setBlock(cursor.set(column.x(), y, column.z()), air, 3);
                    }
                }
            }
            if (job.index >= job.columns.size()) {
                finished.add(entry.getKey());
            }
        }
        for (Long key : finished) {
            Job job = JOBS.remove(key);
            if (job != null) {
                spawnContents(level, job);
            }
        }
    }

    /** 只造一根冰刺：比原版冰刺稍粗稍高，内部留空腔放少女。 */
    private static List<Column> plan(ServerLevel level, int centreX, int centreZ, int ground) {
        List<Column> columns = new ArrayList<>();
        addPillar(level, columns, centreX, centreZ, CORE_RADIUS, CORE_HEIGHT, ground,
                CHAMBER_RADIUS, ground + CHAMBER_HEIGHT);
        return columns;
    }

    /**
     * 一根冰刺：每列从自己脚下的地面长起；横截面用噪声扰动；顶端有轻微抖动。
     */
    private static void addPillar(ServerLevel level, List<Column> columns, int centreX, int centreZ,
                                  int radius, int height, int baseGround, int hollowRadius, int hollowTop) {
        int bound = radius + 1;
        for (int dx = -bound; dx <= bound; dx++) {
            for (int dz = -bound; dz <= bound; dz++) {
                int x = centreX + dx;
                int z = centreZ + dz;
                double distance = Math.sqrt(dx * dx + dz * dz);
                // 沿圆周采样噪声：同一方向上半径恒定，柱子内部不会漏洞
                double dirX = distance > 1.0E-4D ? dx / distance : 1.0D;
                double dirZ = distance > 1.0E-4D ? dz / distance : 0.0D;
                double edge = radius * (1.0D + 0.28D * FrostWarp.noise(
                        dirX * 1.7D + centreX * 0.05D, dirZ * 1.7D + centreZ * 0.05D, 0xA53CL));
                if (distance > edge) {
                    continue;
                }
                int columnGround = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x, z) - 1;
                int jitter = Math.floorMod(x * 31 + z * 17, 3) - 1;
                double ratio = Math.min(1.0D, distance / Math.max(1.0D, edge));
                int taper = (int) Math.round(Math.max(0.0D, ratio - 0.65D) / 0.35D * height * 0.5D);
                int top = columnGround + height - taper + jitter;
                boolean hollow = hollowRadius > 0 && distance <= hollowRadius;
                columns.add(new Column(x, z, columnGround, top, hollow ? hollowTop : 0));
            }
        }
    }

    private static void spawnContents(ServerLevel level, Job job) {
        if (job.ground == Integer.MIN_VALUE) {
            return;
        }
        SealedMaiden maiden = com.myriadbloom.FrostMobs.SEALED_MAIDEN.get().create(level);
        if (maiden != null) {
            maiden.moveTo(job.centreX + 0.5D, job.ground + 11.0D, job.centreZ + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(maiden);
        }
        // 兜底：附近已经有魔法阵就不再生成
        var existing = level.getEntitiesOfClass(MagicCircle.class,
                new net.minecraft.world.phys.AABB(job.centreX - 48, job.ground - 32, job.centreZ - 48,
                        job.centreX + 48, job.ground + 128, job.centreZ + 48));
        if (!existing.isEmpty()) {
            return;
        }
        MagicCircle circle = com.myriadbloom.FrostMobs.MAGIC_CIRCLE.get().create(level);
        if (circle != null) {
            circle.moveTo(job.centreX + 0.5D, job.ground + CORE_HEIGHT + 12.0D, job.centreZ + 0.5D, 0.0F, 0.0F);
            circle.setRadius(CongealedLandFeature.PATCH_RADIUS + CongealedLandFeature.MAX_WOBBLE);
            level.addFreshEntity(circle);
        }
    }
}
