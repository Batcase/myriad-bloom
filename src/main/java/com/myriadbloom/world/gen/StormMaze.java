package com.myriadbloom.world.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 凝结之地地下的四座迷宫，每条风暴林地的分支各一座，中央是第三个 BOSS 的巨型厅。
 *
 * <pre>
 *            [北迷宫]
 *                |
 *   [西迷宫] - [中央 BOSS 厅] - [东迷宫]
 *                |
 *            [南迷宫]
 * </pre>
 *
 * <p>每座迷宫是从中央向外铺的矩形网格，最外一排的中间格接地面螺旋入口，最内一排的中间格用短
 * 隧道接中央厅。所有数据都由区域中心决定，同一区域永远是同一套迷宫。</p>
 */
public final class StormMaze {
    /** 单格边长（通道 + 墙）。 */
    public static final int CELL = 11;
    /** 墙厚。 */
    public static final int WALL = 5;
    /** 通道净宽。 */
    public static final int CORRIDOR = CELL - WALL;
    /** 每座迷宫沿臂方向的格子数。 */
    public static final int CELLS_ALONG = 13;
    /** 每座迷宫横跨臂方向的格子数（奇数，中间有一列）。 */
    public static final int CELLS_ACROSS = 9;
    /** 一座迷宫沿臂方向的长度。 */
    public static final int LENGTH = CELLS_ALONG * CELL;
    /** 一座迷宫横跨臂方向的宽度。 */
    public static final int WIDTH = CELLS_ACROSS * CELL;
    /** 迷宫地面 y。 */
    public static final int FLOOR_Y = -24;
    /** 通道净高。 */
    public static final int HEIGHT = 5;
    /** 中央 BOSS 厅半径。 */
    public static final int BOSS_RADIUS = 26;
    /** 中央 BOSS 厅净高。 */
    public static final int BOSS_HEIGHT = 14;
    /** 中央厅到每座迷宫内端的间距。 */
    public static final int GAP = 34;
    /** 迷宫外端到入口竖井中心的距离。 */
    public static final int ENTRANCE_OFFSET = 12;

    /** 四条臂：0 东、1 南、2 西、3 北。 */
    public enum Arm {
        EAST(1, 0, 0, 1),
        SOUTH(0, 1, 1, 0),
        WEST(-1, 0, 0, 1),
        NORTH(0, -1, 1, 0);

        /** 沿臂方向（从中央指向外）。 */
        public final int alongX;
        public final int alongZ;
        /** 横跨臂方向。 */
        public final int acrossX;
        public final int acrossZ;

        Arm(int alongX, int alongZ, int acrossX, int acrossZ) {
            this.alongX = alongX;
            this.alongZ = alongZ;
            this.acrossX = acrossX;
            this.acrossZ = acrossZ;
        }

        public int id() {
            return ordinal();
        }

        public static Arm byId(int id) {
            return values()[Math.floorMod(id, 4)];
        }
    }

    /** 方块位置的用途。 */
    public enum Kind {
        OUTSIDE,
        WALL,
        OPEN,
        BOSS
    }

    /** 一座迷宫在世界里的位置与连接关系。 */
    public static final class ArmMaze {
        public final Arm arm;
        /** 格子 (0,0) 的世界角点。 */
        public final int baseX;
        public final int baseZ;
        /** local [u][v]：u 沿臂，v 横跨臂。 */
        public final boolean[][] openEast;
        public final boolean[][] openSouth;
        public final List<int[]> chests = new ArrayList<>();

        ArmMaze(Arm arm, int baseX, int baseZ) {
            this.arm = arm;
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.openEast = new boolean[CELLS_ALONG][CELLS_ACROSS];
            this.openSouth = new boolean[CELLS_ALONG][CELLS_ACROSS];
        }

        /** 格子 (u,v) 的世界角点。 */
        public int cellX(int u, int v) {
            return this.baseX + u * CELL * this.arm.alongX + v * CELL * this.arm.acrossX;
        }

        public int cellZ(int u, int v) {
            return this.baseZ + u * CELL * this.arm.alongZ + v * CELL * this.arm.acrossZ;
        }

        private int along(int worldX, int worldZ) {
            return (worldX - this.baseX) * this.arm.alongX + (worldZ - this.baseZ) * this.arm.alongZ;
        }

        private int across(int worldX, int worldZ) {
            return (worldX - this.baseX) * this.arm.acrossX + (worldZ - this.baseZ) * this.arm.acrossZ;
        }

        public int localU(int worldX, int worldZ) {
            return Math.floorMod(along(worldX, worldZ), CELL);
        }

        public int localV(int worldX, int worldZ) {
            return Math.floorMod(across(worldX, worldZ), CELL);
        }

        public int cellU(int worldX, int worldZ) {
            return Math.floorDiv(along(worldX, worldZ), CELL);
        }

        public int cellV(int worldX, int worldZ) {
            return Math.floorDiv(across(worldX, worldZ), CELL);
        }

        public boolean contains(int worldX, int worldZ) {
            int u = cellU(worldX, worldZ);
            int v = cellV(worldX, worldZ);
            return u >= 0 && u < CELLS_ALONG && v >= 0 && v < CELLS_ACROSS;
        }

        /** 该世界位置在这座迷宫里的用途。 */
        public Kind kindAt(int worldX, int worldZ) {
            if (!contains(worldX, worldZ)) {
                return Kind.OUTSIDE;
            }
            int u = cellU(worldX, worldZ);
            int v = cellV(worldX, worldZ);
            int ou = localU(worldX, worldZ);
            int ov = localV(worldX, worldZ);
            boolean corridorU = ou < CORRIDOR;
            boolean corridorV = ov < CORRIDOR;
            int midV = CELLS_ACROSS / 2;
            // 外侧入口：最外一排中间列，朝外那面打开
            if (v == midV && corridorV && u == CELLS_ALONG - 1 && ou >= CORRIDOR) {
                return Kind.OPEN;
            }
            // 内侧出口：最内一排中间列，朝中央厅那面打开
            if (v == midV && corridorV && u == 0 && ou < CORRIDOR) {
                return Kind.OPEN;
            }
            if (corridorU && corridorV) {
                return Kind.OPEN;
            }
            // 沿横跨方向连接（同一排的相邻格）
            if (ou >= CORRIDOR && corridorV && v + 1 < CELLS_ACROSS && this.openSouth[u][v]) {
                return Kind.OPEN;
            }
            // 沿臂方向连接（后一排的相邻格）
            if (ov >= CORRIDOR && corridorU && u + 1 < CELLS_ALONG && this.openEast[u][v]) {
                return Kind.OPEN;
            }
            return Kind.WALL;
        }
    }

    /** 整个区域的地下结构。 */
    public static final class Layout {
        public final int centreX;
        public final int centreZ;
        public final ArmMaze[] mazes = new ArmMaze[4];

        Layout(int centreX, int centreZ) {
            this.centreX = centreX;
            this.centreZ = centreZ;
            for (Arm arm : Arm.values()) {
                // 内端角点 = 中央厅外 GAP 处，横跨方向居中
                int innerX = centreX + arm.alongX * GAP;
                int innerZ = centreZ + arm.alongZ * GAP;
                int baseX = innerX - arm.acrossX * (WIDTH - CELL) / 2;
                int baseZ = innerZ - arm.acrossZ * (WIDTH - CELL) / 2;
                // 反向臂的 base 要落在最内端那一格的角上
                if (arm.alongX < 0) {
                    baseX = innerX - (LENGTH - CELL);
                }
                if (arm.alongZ < 0) {
                    baseZ = innerZ - (LENGTH - CELL);
                }
                this.mazes[arm.id()] = new ArmMaze(arm, baseX, baseZ);
            }
        }

        public boolean insideBossHall(int worldX, int worldZ) {
            int dx = worldX - this.centreX;
            int dz = worldZ - this.centreZ;
            return dx * dx + dz * dz <= BOSS_RADIUS * BOSS_RADIUS;
        }

        /** 方块位置属于哪座迷宫，没有则返回 null。 */
        public ArmMaze mazeAt(int worldX, int worldZ) {
            for (ArmMaze maze : this.mazes) {
                if (maze.contains(worldX, worldZ)) {
                    return maze;
                }
            }
            return null;
        }

        public Kind kindAt(int worldX, int worldZ) {
            if (insideBossHall(worldX, worldZ)) {
                return Kind.BOSS;
            }
            ArmMaze maze = mazeAt(worldX, worldZ);
            return maze == null ? Kind.OUTSIDE : maze.kindAt(worldX, worldZ);
        }

        /** 入口竖井的世界坐标（迷宫外端外侧）。 */
        public int[] entrance(Arm arm) {
            ArmMaze maze = this.mazes[arm.id()];
            int u = CELLS_ALONG;
            int v = CELLS_ACROSS / 2;
            int x = maze.cellX(u, v) + (CELL / 2) * arm.alongX + (CORRIDOR / 2) * arm.acrossX;
            int z = maze.cellZ(u, v) + (CELL / 2) * arm.alongZ + (CORRIDOR / 2) * arm.acrossZ;
            return new int[]{x + arm.alongX * ENTRANCE_OFFSET, z + arm.alongZ * ENTRANCE_OFFSET};
        }
    }

    private StormMaze() {
    }

    /** 生成整个区域的布局。 */
    public static Layout layout(int centreX, int centreZ) {
        Layout layout = new Layout(centreX, centreZ);
        for (ArmMaze maze : layout.mazes) {
            carve(maze, centreX * 31L + centreZ * 17L + maze.arm.id() * 7919L);
            placeRooms(maze);
        }
        return layout;
    }

    /** 递归回溯，把一座迷宫铺满。 */
    private static void carve(ArmMaze maze, long seed) {
        Random random = new Random(seed);
        boolean[][] visited = new boolean[CELLS_ALONG][CELLS_ACROSS];
        int[] stackU = new int[CELLS_ALONG * CELLS_ACROSS];
        int[] stackV = new int[CELLS_ALONG * CELLS_ACROSS];
        int top = 0;
        visited[0][CELLS_ACROSS / 2] = true;
        stackU[0] = 0;
        stackV[0] = CELLS_ACROSS / 2;
        top = 1;
        int[] du = {1, -1, 0, 0};
        int[] dv = {0, 0, 1, -1};
        while (top > 0) {
            int u = stackU[top - 1];
            int v = stackV[top - 1];
            int[] options = new int[4];
            int count = 0;
            for (int dir = 0; dir < 4; dir++) {
                int nu = u + du[dir];
                int nv = v + dv[dir];
                if (nu < 0 || nv < 0 || nu >= CELLS_ALONG || nv >= CELLS_ACROSS) {
                    continue;
                }
                if (visited[nu][nv]) {
                    continue;
                }
                options[count++] = dir;
            }
            if (count == 0) {
                top--;
                continue;
            }
            int dir = options[random.nextInt(count)];
            int nu = u + du[dir];
            int nv = v + dv[dir];
            if (dir == 0) {
                maze.openEast[u][v] = true;
            } else if (dir == 1) {
                maze.openEast[nu][nv] = true;
            } else if (dir == 2) {
                maze.openSouth[u][v] = true;
            } else {
                maze.openSouth[nu][nv] = true;
            }
            visited[nu][nv] = true;
            stackU[top] = nu;
            stackV[top] = nv;
            top++;
        }
        // 内端与外端的中间列一定连通
        int midV = CELLS_ACROSS / 2;
        for (int u = 0; u < CELLS_ALONG - 1; u++) {
            maze.openEast[u][midV] = true;
        }
    }

    /** 死胡同放箱子。 */
    private static void placeRooms(ArmMaze maze) {
        for (int u = 0; u < CELLS_ALONG; u++) {
            for (int v = 0; v < CELLS_ACROSS; v++) {
                int links = 0;
                if (u + 1 < CELLS_ALONG && maze.openEast[u][v]) {
                    links++;
                }
                if (u > 0 && maze.openEast[u - 1][v]) {
                    links++;
                }
                if (v + 1 < CELLS_ACROSS && maze.openSouth[u][v]) {
                    links++;
                }
                if (v > 0 && maze.openSouth[u][v - 1]) {
                    links++;
                }
                if (links == 1) {
                    int cx = maze.cellX(u, v) + (CORRIDOR / 2) * maze.arm.alongX
                            + (CORRIDOR / 2) * maze.arm.acrossX;
                    int cz = maze.cellZ(u, v) + (CORRIDOR / 2) * maze.arm.alongZ
                            + (CORRIDOR / 2) * maze.arm.acrossZ;
                    maze.chests.add(new int[]{cx, cz});
                }
            }
        }
    }
}
