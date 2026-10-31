package com.myriadbloom.world.gen;

/**
 * 地貌边界扰动 (biome warping): a cheap, deterministic 2D value noise used to make patch borders
 * ragged instead of ruler straight.
 *
 * <p>Everything is a pure function of the coordinate, so worldgen, features and the biome source all
 * agree on the same border without storing anything.</p>
 */
public final class FrostWarp {
    private FrostWarp() {
    }

    private static double hash(int x, int z, long seed) {
        long h = x * 374761393L + z * 668265263L + seed * 1442695040888963407L;
        h ^= h >>> 13;
        h *= 1274126177L;
        h ^= h >>> 16;
        return ((h & 0xFFFFL) / 65535.0) * 2.0 - 1.0;
    }

    /** 平滑值噪声，返回 -1..1。 */
    public static double noise(double x, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sz = fz * fz * (3.0 - 2.0 * fz);
        double n00 = hash(x0, z0, seed);
        double n10 = hash(x0 + 1, z0, seed);
        double n01 = hash(x0, z0 + 1, seed);
        double n11 = hash(x0 + 1, z0 + 1, seed);
        double a = n00 + (n10 - n00) * sx;
        double b = n01 + (n11 - n01) * sx;
        return a + (b - a) * sz;
    }

    /** 把方块坐标沿边界方向偏移，用于让几何边界变得不规则。 */
    public static double offsetX(int blockX, int blockZ, double amplitude) {
        return noise(blockX / 48.0, blockZ / 48.0, 0x51ED) * amplitude
                + noise(blockX / 13.0, blockZ / 13.0, 0x9E37) * amplitude * 0.35;
    }

    public static double offsetZ(int blockX, int blockZ, double amplitude) {
        return noise(blockX / 48.0, blockZ / 48.0, 0x2F9A) * amplitude
                + noise(blockX / 13.0, blockZ / 13.0, 0x77C1) * amplitude * 0.35;
    }
}
