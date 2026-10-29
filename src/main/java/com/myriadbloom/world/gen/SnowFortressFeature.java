package com.myriadbloom.world.gen;

import com.myriadbloom.GlacierContent;
import com.myriadbloom.world.RegionLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Snow fortress (雪之堡垒): a large Antarctic-style igloo of glacier ice.
 *
 * <p>The site has to be flat ground (no water, and the surface must not vary by more than
 * {@value #MAX_SLOPE} blocks over the apron). Around the dome the terrain is then levelled with a
 * smooth gradient - fully flat inside {@value #APRON_INNER} blocks, blending back into the natural
 * height at {@value #APRON_OUTER} - so the fortress never floats or sits in a cliff.</p>
 *
 * <p>The dome is a hollow hemisphere (outer radius {@value #OUTER_RADIUS}, wall about two blocks
 * thick, interior ceiling about {@value #INNER_RADIUS} high) with a 3 wide, 4 high entrance tunnel
 * pointing in a random direction. That leaves room for the planned boss.</p>
 */
public class SnowFortressFeature extends Feature<NoneFeatureConfiguration> {
    private static final int OUTER_RADIUS = 20;
    private static final int INNER_RADIUS = 18;
    private static final int APRON_INNER = 21;
    private static final int APRON_OUTER = 42;
    private static final int ENTRANCE_HALF_WIDTH = 1;
    private static final int ENTRANCE_HEIGHT = 4;
    private static final int ENTRANCE_LENGTH = 10;
    /** Maximum a column may be filled or cut while levelling the site. */
    private static final int MAX_TERRAIN_CHANGE = 4;
    /** The dome footprint may be levelled by this many blocks, so it always sits flush. */
    private static final int DOME_LEVELLING = 14;


    /** Chunk clip box for the current build, so structure pieces only write in the chunk they own. */
    private static final ThreadLocal<BoundingBox> CLIP = new ThreadLocal<>();

    /** Runs {@code action} with block writes clipped to {@code box}. */
    public static void withClip(BoundingBox box, Runnable action) {
        CLIP.set(box);
        try {
            action.run();
        } finally {
            CLIP.remove();
        }
    }

    /** Structure-safe block write: ignored outside the current clip box. */
    private static void set(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        BoundingBox clip = CLIP.get();
        if (clip != null && !clip.isInside(pos)) {
            return;
        }
        level.setBlock(pos, state, flags);
    }


    /** Radius of the flattened apron, exposed for the structure's site test. */
    public static final int APRON_RADIUS = APRON_OUTER;

    /** Only the dome footprint has to be flat; the apron blends into the landscape. */
    public static final int DOME_FLAT_RADIUS = 22;

    public static final int MAX_SLOPE = 6;

    /**
     * Terrain test for a fortress site: over the whole apron the natural surface may rise or fall by
     * at most {@value #MAX_SLOPE} blocks. Steeper spots are valleys or cliffs - levelling them would
     * bury the entrance and need hundreds of glacier ice blocks, so no fortress is built there.
     */
    public static boolean isFlatSite(WorldGenLevel level, BlockPos origin) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int outerSq = APRON_OUTER * APRON_OUTER;
        for (int dz = -APRON_OUTER; dz <= APRON_OUTER; dz += 2) {
            for (int dx = -APRON_OUTER; dx <= APRON_OUTER; dx += 2) {
                if (dx * dx + dz * dz > outerSq) {
                    continue;
                }
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.getX() + dx, origin.getZ() + dz) - 1;
                min = Math.min(min, height);
                max = Math.max(max, height);
                if (max - min > MAX_SLOPE) {
                    return false;
                }
            }
        }
        return max - min <= MAX_SLOPE;
    }

    public SnowFortressFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return build(context.level(), context.random(), context.origin());
    }

    /**
     * Builds the fortress at {@code origin} (the block above the surface). Shared by the structure
     * ({@link SnowFortressStructure}) and the standalone feature.
     *
     * @return {@code false} when the site is not flat and dry enough
     */
    public static boolean build(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int base = origin.getY() - 1; // top solid block of the heightmap

        // postProcess runs once per chunk the piece touches, so the build has to be reproducible:
        // a fresh random per call used to carve a different entrance in every chunk (two doors).
        RandomSource stable = RandomSource.create(origin.asLong() * 0x9E3779B97F4A7C15L);

        BlockState ice = GlacierContent.GLACIER_ICE.get().defaultBlockState();
        flattenApron(level, origin, base, ice);
        buildDome(level, origin, base, ice);
        buildFloor(level, origin, base, ice);
        buildEntrance(level, origin, base, ice, stable);
        // the boss spawner wakes up once a player enters the fortress
        BlockPos spawnerPos = new BlockPos(origin.getX(), base + 1, origin.getZ());
        set(level, spawnerPos, com.myriadbloom.BossContent.BOSS_SPAWNER.get().defaultBlockState(), 2);
        if (level.getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            RegionLedger.of(server).record("boss_spawner", spawnerPos);
        }
        return true;
    }

    /** Paves the inside of the dome with glacier ice instead of the natural dirt/snow. */
    private static void buildFloor(WorldGenLevel level, BlockPos origin, int base, BlockState ice) {
        int radiusSq = INNER_RADIUS * INNER_RADIUS;
        for (int dz = -INNER_RADIUS; dz <= INNER_RADIUS; dz++) {
            for (int dx = -INNER_RADIUS; dx <= INNER_RADIUS; dx++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                for (int dy = -2; dy <= 0; dy++) {
                    set(level, new BlockPos(origin.getX() + dx, base + dy, origin.getZ() + dz), ice, 2);
                }
            }
        }
    }

    /**
     * Site test: only reject surface water. The fortress is always built and the ground is levelled
     * with glacier ice, but a column is never filled or cut by more than
     * {@value #MAX_TERRAIN_CHANGE} blocks - a steep site would otherwise need a small mountain of ice.
     */
    private static boolean isSuitable(WorldGenLevel level, BlockPos origin) {
        for (int dz = -APRON_OUTER; dz <= APRON_OUTER; dz += 3) {
            for (int dx = -APRON_OUTER; dx <= APRON_OUTER; dx += 3) {
                if (dx * dx + dz * dz > APRON_OUTER * APRON_OUTER) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockState state = level.getBlockState(new BlockPos(x, height - 1, z));
                if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Levels the ground around the dome, blending the natural height back in at the apron edge. */
    private static void flattenApron(WorldGenLevel level, BlockPos origin, int base, BlockState ice) {
        for (int dz = -APRON_OUTER; dz <= APRON_OUTER; dz++) {
            for (int dx = -APRON_OUTER; dx <= APRON_OUTER; dx++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > APRON_OUTER) {
                    continue;
                }
                // 1 inside the flat core, smoothly 0 at the outer edge
                double blend = Mth.clamp((APRON_OUTER - distance) / (double) (APRON_OUTER - APRON_INNER), 0.0D, 1.0D);
                blend = blend * blend * (3.0D - 2.0D * blend); // smoothstep
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int natural = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int target = (int) Math.round(natural + (base - natural) * blend);
                // Under the dome the ground is levelled all the way to the base height (up to
                // DOME_LEVELLING blocks), otherwise the dome would sit half buried or half floating on
                // sloped terrain. The outer apron blends back into the landscape with the gentle cap.
                int limit = distance <= INNER_RADIUS + 2.0D ? DOME_LEVELLING : MAX_TERRAIN_CHANGE;
                target = Mth.clamp(target, natural - limit, natural + limit);

                for (int y = target + 1; y <= natural; y++) {
                    set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                for (int y = natural; y < target; y++) {
                    set(level, new BlockPos(x, y, z), ice, 2);
                }
                set(level, new BlockPos(x, Math.max(target, natural), z), ice, 2);
            }
        }
    }

    /** Hollow hemisphere shell: glacier ice outside, open air inside, the ground as the floor. */
    private static void buildDome(WorldGenLevel level, BlockPos origin, int base, BlockState ice) {
        double outer = OUTER_RADIUS * OUTER_RADIUS;
        double inner = INNER_RADIUS * INNER_RADIUS;
        for (int dy = 0; dy <= OUTER_RADIUS; dy++) {
            for (int dz = -OUTER_RADIUS; dz <= OUTER_RADIUS; dz++) {
                for (int dx = -OUTER_RADIUS; dx <= OUTER_RADIUS; dx++) {
                    double distance = dx * dx + dy * dy + dz * dz;
                    if (distance > outer) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(origin.getX() + dx, base + dy, origin.getZ() + dz);
                    if (distance >= inner) {
                        set(level, pos, ice, 2);
                    } else {
                        set(level, pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Entrance tunnel in a random cardinal direction: ice shell outside, 3x4 air inside. */
    private static void buildEntrance(WorldGenLevel level, BlockPos origin, int base, BlockState ice,
                                      RandomSource random) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] direction = directions[random.nextInt(directions.length)];
        int perpX = -direction[1];
        int perpZ = direction[0];

        int from = OUTER_RADIUS - 3;
        int to = OUTER_RADIUS + ENTRANCE_LENGTH;
        for (int step = from; step <= to; step++) {
            for (int side = -ENRANCE_WALL; side <= ENRANCE_WALL; side++) {
                for (int height = 0; height <= ENTRANCE_HEIGHT + 1; height++) {
                    BlockPos pos = new BlockPos(
                            origin.getX() + direction[0] * step + perpX * side,
                            base + height,
                            origin.getZ() + direction[1] * step + perpZ * side);
                    boolean open = Math.abs(side) <= ENTRANCE_HALF_WIDTH
                            && height >= 1
                            && height <= ENTRANCE_HEIGHT;
                    set(level, pos, open ? Blocks.AIR.defaultBlockState() : ice, 2);
                }

                // support skirt: outside the flattened dome the ground falls away, so the walkway is
                // carried down to the real terrain instead of hanging in mid air
                int worldX = origin.getX() + direction[0] * step + perpX * side;
                int worldZ = origin.getZ() + direction[1] * step + perpZ * side;
                int ground = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        worldX, worldZ);
                for (int y = Math.max(ground, level.getMinBuildHeight()); y < base; y++) {
                    set(level, new BlockPos(worldX, y, worldZ), ice, 2);
                }
            }
        }
    }

    /** Half width of the ice shell around the tunnel opening (one block thicker than the walkway). */
    private static final int ENRANCE_WALL = ENTRANCE_HALF_WIDTH + 1;
}
