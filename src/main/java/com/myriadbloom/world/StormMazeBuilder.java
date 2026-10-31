package com.myriadbloom.world;

import com.myriadbloom.FrostedLandContent;
import com.myriadbloom.world.gen.StormMaze;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 把 {@link StormMaze} 的布局写成方块。每个区块只处理落在自己范围内的那一部分，调用方负责记录
 * 哪些区块已经建过。
 */
public final class StormMazeBuilder {
    private static final BlockState WALL = FrostedLandContent.CONGEALED_ICE.get().defaultBlockState();
    private static final BlockState FLOOR = Blocks.PACKED_ICE.defaultBlockState();
    private static final BlockState STEP = Blocks.BLUE_ICE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    /** 入口竖井半径。 */
    public static final int SHAFT_RADIUS = 4;

    private StormMazeBuilder() {
    }

    /** 建出这个区块里的迷宫部分。 */
    public static void buildChunk(ServerLevel level, StormMaze.Layout layout, int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                buildColumn(level, layout, x, z);
            }
        }
        for (StormMaze.ArmMaze maze : layout.mazes) {
            buildChests(level, maze, minX, minZ);
        }
        if (chunkX == (layout.centreX >> 4) && chunkZ == (layout.centreZ >> 4)) {
            placeBossSocket(level, layout);
        }
        buildLinks(level, layout, minX, minZ, maxX, maxZ);
    }

    /** 一列：属于中央厅或某座迷宫时铺地板、空气、天花板。 */
    private static void buildColumn(ServerLevel level, StormMaze.Layout layout, int x, int z) {
        StormMaze.Kind kind = layout.kindAt(x, z);
        if (kind == StormMaze.Kind.OUTSIDE) {
            return;
        }
        boolean boss = kind == StormMaze.Kind.BOSS;
        int ceiling = StormMaze.FLOOR_Y + (boss ? StormMaze.BOSS_HEIGHT : StormMaze.HEIGHT) + 1;
        level.setBlock(new BlockPos(x, StormMaze.FLOOR_Y, z), FLOOR, 2);
        if (kind == StormMaze.Kind.WALL) {
            for (int y = StormMaze.FLOOR_Y + 1; y <= ceiling; y++) {
                level.setBlock(new BlockPos(x, y, z), WALL, 2);
            }
            return;
        }
        for (int y = StormMaze.FLOOR_Y + 1; y <= ceiling; y++) {
            level.setBlock(new BlockPos(x, y, z), AIR, 2);
        }
        level.setBlock(new BlockPos(x, ceiling, z), WALL, 2);
        if (boss) {
            // 八根冰柱围住大厅，中间留空给 BOSS
            int dx = x - layout.centreX;
            int dz = z - layout.centreZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx);
            if (distance > 18.0D && distance < 21.0D
                    && Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8) == 0) {
                for (int y = StormMaze.FLOOR_Y + 1; y <= StormMaze.FLOOR_Y + 11; y++) {
                    level.setBlock(new BlockPos(x, y, z), WALL, 2);
                }
            }
        }
    }

    /** 在中央厅正中放一块召唤核心：玩家走进大厅时由机制唤醒 BOSS。 */
    public static void placeBossSocket(ServerLevel level, StormMaze.Layout layout) {
        BlockPos pos = new BlockPos(layout.centreX, StormMaze.FLOOR_Y + 1, layout.centreZ);
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, com.myriadbloom.StormWoodlandContent.STORM_PRISM_SPAWNER.get()
                    .defaultBlockState(), 3);
        }
    }

    /** 死胡同里的箱子。 */
    private static void buildChests(ServerLevel level, StormMaze.ArmMaze maze, int minX, int minZ) {
        for (int[] chest : maze.chests) {
            if (chest[0] < minX || chest[0] > minX + 15 || chest[1] < minZ || chest[1] > minZ + 15) {
                continue;
            }
            BlockPos pos = new BlockPos(chest[0], StormMaze.FLOOR_Y + 1, chest[1]);
            if (!level.getBlockState(pos).isAir() || level.getBlockEntity(pos) instanceof ChestBlockEntity) {
                continue;
            }
            level.setBlock(pos, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.from2DDataValue(
                            Math.floorMod(chest[0] * 31 + chest[1] * 17, 4))), 3);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestEntity) {
                chestEntity.setLootTable(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE,
                        com.myriadbloom.api.MyriadBloomApi.id("chests/storm_maze")), 1L);
            }
        }
    }

    /** 四条入口竖井与外侧短隧道。 */
    private static void buildLinks(ServerLevel level, StormMaze.Layout layout,
                                   int minX, int minZ, int maxX, int maxZ) {
        for (StormMaze.Arm arm : StormMaze.Arm.values()) {
            int[] entrance = layout.entrance(arm);
            int shaftX = entrance[0];
            int shaftZ = entrance[1];
            boolean shaftChunk = maxX >= shaftX - SHAFT_RADIUS - 2 && minX <= shaftX + SHAFT_RADIUS + 2
                    && maxZ >= shaftZ - SHAFT_RADIUS - 2 && minZ <= shaftZ + SHAFT_RADIUS + 2;
            // the access tunnel lies in its own chunks: it must not be skipped just because this
            // chunk does not hold the shaft itself
            boolean tunnelChunk = tunnelTouches(layout, arm, shaftX, shaftZ, minX, minZ, maxX, maxZ);
            boolean innerChunk = innerTouches(layout, arm, minX, minZ, maxX, maxZ);
            if (!shaftChunk && !tunnelChunk && !innerChunk) {
                continue;
            }
            if (shaftChunk) {
                int surface = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        shaftX, shaftZ);
                buildShaft(level, shaftX, shaftZ, surface, minX, minZ, maxX, maxZ);
            }
            if (tunnelChunk) {
                buildTunnel(level, layout, arm, shaftX, shaftZ, minX, minZ, maxX, maxZ);
            }
            // 内侧：把迷宫内端接到中央 BOSS 厅
            if (innerChunk) {
                buildInnerLink(level, layout, arm, minX, minZ, maxX, maxZ);
            }
        }
    }


    /** 迷宫内端到中央厅的短通道是否碰到这个区块。 */
    private static boolean innerTouches(StormMaze.Layout layout, StormMaze.Arm arm,
                                        int minX, int minZ, int maxX, int maxZ) {
        int[] range = innerRange(layout, arm);
        return maxX >= range[0] && minX <= range[1] && maxZ >= range[2] && minZ <= range[3];
    }

    /** 内侧通道的世界范围：x0,x1,z0,z1。 */
    private static int[] innerRange(StormMaze.Layout layout, StormMaze.Arm arm) {
        StormMaze.ArmMaze maze = layout.mazes[arm.id()];
        int v = StormMaze.CELLS_ACROSS / 2;
        int x = maze.cellX(0, v) + (StormMaze.CORRIDOR / 2) * arm.acrossX;
        int z = maze.cellZ(0, v) + (StormMaze.CORRIDOR / 2) * arm.acrossZ;
        // 从迷宫内端沿臂方向往回（朝中央）延伸 GAP+4 格
        int innerX = layout.centreX + arm.alongX * (StormMaze.BOSS_RADIUS - 1);
        int innerZ = layout.centreZ + arm.alongZ * (StormMaze.BOSS_RADIUS - 1);
        int x0 = Math.min(x, innerX) - (arm.alongX != 0 ? 0 : StormMaze.CORRIDOR / 2);
        int x1 = Math.max(x, innerX) + (arm.alongX != 0 ? 0 : StormMaze.CORRIDOR / 2);
        int z0 = Math.min(z, innerZ) - (arm.alongZ != 0 ? 0 : StormMaze.CORRIDOR / 2);
        int z1 = Math.max(z, innerZ) + (arm.alongZ != 0 ? 0 : StormMaze.CORRIDOR / 2);
        return new int[]{x0, x1, z0, z1};
    }

    /** 挖通迷宫内端与中央厅之间的短通道。 */
    private static void buildInnerLink(ServerLevel level, StormMaze.Layout layout, StormMaze.Arm arm,
                                       int minX, int minZ, int maxX, int maxZ) {
        int[] range = innerRange(layout, arm);
        for (int x = Math.max(minX, range[0]); x <= Math.min(maxX, range[1]); x++) {
            for (int z = Math.max(minZ, range[2]); z <= Math.min(maxZ, range[3]); z++) {
                level.setBlock(new BlockPos(x, StormMaze.FLOOR_Y, z), FLOOR, 2);
                for (int y = StormMaze.FLOOR_Y + 1; y <= StormMaze.FLOOR_Y + StormMaze.HEIGHT; y++) {
                    level.setBlock(new BlockPos(x, y, z), AIR, 2);
                }
                level.setBlock(new BlockPos(x, StormMaze.FLOOR_Y + StormMaze.HEIGHT + 1, z), WALL, 2);
            }
        }
    }

    /** 该区块是否碰到某条臂的入口隧道。 */
    private static boolean tunnelTouches(StormMaze.Layout layout, StormMaze.Arm arm,
                                         int shaftX, int shaftZ,
                                         int minX, int minZ, int maxX, int maxZ) {
        StormMaze.ArmMaze maze = layout.mazes[arm.id()];
        int v = StormMaze.CELLS_ACROSS / 2;
        int u = StormMaze.CELLS_ALONG;
        int targetX = maze.cellX(u, v) + (StormMaze.CORRIDOR / 2) * arm.acrossX;
        int targetZ = maze.cellZ(u, v) + (StormMaze.CORRIDOR / 2) * arm.acrossZ;
        boolean alongX = arm.alongX != 0;
        int fromX = alongX ? Math.min(shaftX, targetX) : targetX - StormMaze.CORRIDOR / 2;
        int toX = alongX ? Math.max(shaftX, targetX) : targetX + StormMaze.CORRIDOR / 2;
        int fromZ = alongX ? targetZ - StormMaze.CORRIDOR / 2 : Math.min(shaftZ, targetZ);
        int toZ = alongX ? targetZ + StormMaze.CORRIDOR / 2 : Math.max(shaftZ, targetZ);
        return maxX >= fromX && minX <= toX && maxZ >= fromZ && minZ <= toZ;
    }

    /** 竖井：中央空心，外圈凝结之冰，内壁一圈从地表通到迷宫地面的螺旋台阶。 */
    private static void buildShaft(ServerLevel level, int centreX, int centreZ, int surface,
                                   int minX, int minZ, int maxX, int maxZ) {
        int top = surface + 2;
        // 1) 先把竖井挖空、砌上外壁
        for (int x = Math.max(minX, centreX - SHAFT_RADIUS - 1);
                x <= Math.min(maxX, centreX + SHAFT_RADIUS + 1); x++) {
            for (int z = Math.max(minZ, centreZ - SHAFT_RADIUS - 1);
                    z <= Math.min(maxZ, centreZ + SHAFT_RADIUS + 1); z++) {
                double distance = Math.sqrt((x - centreX) * (x - centreX)
                        + (z - centreZ) * (z - centreZ));
                for (int y = StormMaze.FLOOR_Y; y <= top; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (distance <= SHAFT_RADIUS - 1.0D) {
                        level.setBlock(pos, AIR, 2);
                    } else if (distance <= SHAFT_RADIUS) {
                        level.setBlock(pos, WALL, 2);
                    }
                }
            }
        }
        // 2) 再沿螺旋逐级放台阶：每级下降一格，每圈八级
        int steps = top - StormMaze.FLOOR_Y;
        for (int i = 0; i < steps; i++) {
            int y = top - i;
            double angle = i * (Math.PI * 2.0D / 8.0D);
            int x = centreX + (int) Math.round(Math.cos(angle) * (SHAFT_RADIUS - 1.0D));
            int z = centreZ + (int) Math.round(Math.sin(angle) * (SHAFT_RADIUS - 1.0D));
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                continue;
            }
            // 20% 概率缺一级，但不会连续缺两级，下去能跳、上来也还能爬
            if (missingStep(i, top)) {
                continue;
            }
            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);
            Direction facing = Math.abs(tangentX) > Math.abs(tangentZ)
                    ? (tangentX > 0 ? Direction.EAST : Direction.WEST)
                    : (tangentZ > 0 ? Direction.SOUTH : Direction.NORTH);
            BlockState stepState = com.myriadbloom.StormWoodlandContent.ICE_STEP.get()
                    .defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, facing);
            level.setBlock(new BlockPos(x, y, z), stepState, 3);
            // 台阶上方留出净空，免得踩上去顶头
            level.setBlock(new BlockPos(x, y + 1, z), AIR, 2);
            level.setBlock(new BlockPos(x, y + 2, z), AIR, 2);
        }
        level.setBlock(new BlockPos(centreX, StormMaze.FLOOR_Y, centreZ), FLOOR, 2);
    }

    /**
     * 这一级台阶是否缺失：每 5 级缺一级（约 20%），并且不会连续缺两级。
     *
     * <p>用固定哈希而不是随机数，所以同一个世界重建多少次都是同一段楼梯。</p>
     */
    private static boolean missingStep(int stepIndex, int top) {
        return stepHash(stepIndex, top) % 5 == 0 && stepHash(stepIndex - 1, top) % 5 != 0;
    }

    private static int stepHash(int stepIndex, int top) {
        int hash = stepIndex * 0x9E3779B1 + top * 0x85EBCA77;
        hash ^= hash >>> 15;
        hash *= 0x2545F491;
        hash ^= hash >>> 13;
        return Math.abs(hash);
    }

    /** 竖井底部到迷宫外端的短隧道。 */
    private static void buildTunnel(ServerLevel level, StormMaze.Layout layout, StormMaze.Arm arm,
                                    int shaftX, int shaftZ, int minX, int minZ, int maxX, int maxZ) {
        StormMaze.ArmMaze maze = layout.mazes[arm.id()];
        int v = StormMaze.CELLS_ACROSS / 2;
        int u = StormMaze.CELLS_ALONG;
        int targetX = maze.cellX(u, v) + (StormMaze.CORRIDOR / 2) * arm.acrossX;
        int targetZ = maze.cellZ(u, v) + (StormMaze.CORRIDOR / 2) * arm.acrossZ;
        boolean alongX = arm.alongX != 0;
        int fromX = alongX ? Math.min(shaftX, targetX) : targetX - StormMaze.CORRIDOR / 2;
        int toX = alongX ? Math.max(shaftX, targetX) : targetX + StormMaze.CORRIDOR / 2;
        int fromZ = alongX ? targetZ - StormMaze.CORRIDOR / 2 : Math.min(shaftZ, targetZ);
        int toZ = alongX ? targetZ + StormMaze.CORRIDOR / 2 : Math.max(shaftZ, targetZ);
        for (int x = Math.max(minX, fromX); x <= Math.min(maxX, toX); x++) {
            for (int z = Math.max(minZ, fromZ); z <= Math.min(maxZ, toZ); z++) {
                level.setBlock(new BlockPos(x, StormMaze.FLOOR_Y, z), FLOOR, 2);
                for (int y = StormMaze.FLOOR_Y + 1; y <= StormMaze.FLOOR_Y + StormMaze.HEIGHT; y++) {
                    level.setBlock(new BlockPos(x, y, z), AIR, 2);
                }
                level.setBlock(new BlockPos(x, StormMaze.FLOOR_Y + StormMaze.HEIGHT + 1, z), WALL, 2);
            }
        }
    }
}
