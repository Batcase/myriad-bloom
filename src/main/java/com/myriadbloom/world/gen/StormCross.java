package com.myriadbloom.world.gen;

/**
 * 风暴林地（十字形）的几何。
 *
 * <p>十字的宽度按「迷宫 + 约 2/5 余地」定：一座迷宫横跨臂方向 99 格，十字宽 312 格，两侧各留约
 * 106 格，不会把迷宫贴死在林地边缘。</p>
 *
 * <p>四条臂各有一座迷宫（见 {@link StormMaze}），入口就在那座迷宫外端的外侧。</p>
 */
public final class StormCross {
    /** 每条臂的半宽。十字总宽 = 2 * 这个值。 */
    public static final int ARM_HALF_WIDTH = 156;
    /** 从中心到臂尖的长度。 */
    public static final int ARM_LENGTH = 700;
    /** 世界生成阶段先挖的入口竖井深度（剩下的由迷宫构建器补完）。 */
    public static final int SPIRAL_DEPTH = 44;
    /** 入口竖井半径。 */
    public static final int SPIRAL_RADIUS = 5;

    private StormCross() {
    }

    /** 臂边界的低频噪声，让十字不是直边。 */
    private static double borderNoise(int x, int z, int seed) {
        double a = Math.sin(x * 0.0121D + seed * 1.7D) * 18.0D;
        double b = Math.sin(z * 0.0143D + seed * 2.3D) * 16.0D;
        double c = Math.sin((x + z) * 0.0071D + seed * 0.9D) * 22.0D;
        double d = Math.sin((x - z) * 0.0211D + seed * 3.1D) * 9.0D;
        return a + b + c + d;
    }

    /** 该方块是否落在本区域的十字形风暴林地里。 */
    public static boolean insideCross(int blockX, int blockZ) {
        int[] centre = CongealedLandFeature.patchCentre(blockX >> 4, blockZ >> 4);
        // 先用噪声把坐标扭一下，十字的边就不再是直尺边
        int warpedX = blockX + (int) Math.round(FrostWarp.offsetX(blockX, blockZ, 26.0D));
        int warpedZ = blockZ + (int) Math.round(FrostWarp.offsetZ(blockX, blockZ, 26.0D));
        int dx = Math.abs(warpedX - centre[0]);
        int dz = Math.abs(warpedZ - centre[1]);
        int seed = (centre[0] & 0xFF) * 31 + (centre[1] & 0xFF) * 17;
        double wobble = borderNoise(blockX, blockZ, seed);
        double halfWidth = ARM_HALF_WIDTH + wobble;
        double length = ARM_LENGTH + wobble * 1.4D;
        // 十字：一条宽横杠或一条宽竖杠
        return (dz <= halfWidth && dx <= length) || (dx <= halfWidth && dz <= length);
    }

    /**
     * 第 {@code arm} 条臂的入口竖井中心：0 东、1 南、2 西、3 北。
     *
     * <p>位置由那座迷宫的外端决定，所以竖井一定落在最外一排的中间通道上。</p>
     */
    public static int[] spiralCentre(int blockX, int blockZ, int arm) {
        int[] centre = CongealedLandFeature.patchCentre(blockX >> 4, blockZ >> 4);
        StormMaze.Layout layout = StormMaze.layout(centre[0], centre[1]);
        return layout.entrance(StormMaze.Arm.byId(arm));
    }

    /** 该方块所在区块是不是某个入口竖井，返回臂编号，否则 -1。 */
    public static int spiralArmAt(int blockX, int blockZ) {
        for (StormMaze.Arm arm : StormMaze.Arm.values()) {
            int[] at = spiralCentre(blockX, blockZ, arm.id());
            if ((blockX >> 4) == (at[0] >> 4) && (blockZ >> 4) == (at[1] >> 4)) {
                return arm.id();
            }
        }
        return -1;
    }
}
