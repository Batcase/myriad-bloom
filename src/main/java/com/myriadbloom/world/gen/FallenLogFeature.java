package com.myriadbloom.world.gen;

import com.myriadbloom.FrostedLandContent;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A fallen giant log lying on the snow of the frosted land.
 *
 * <ul>
 *   <li>cross section: a random, <b>not necessarily square</b> rectangle between 2x2 and 6x6</li>
 *   <li>length: 4-8 blocks</li>
 *   <li>bigger than 3x3: the log is a hollow tube, bark ring outside and empty air inside</li>
 *   <li>3x3 and smaller: solid bark with an ice core</li>
 *   <li>two logs never overlap: positions are remembered and a new log keeps its distance</li>
 * </ul>
 */
public class FallenLogFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_DIAMETER = 2;
    private static final int MAX_DIAMETER = 6;
    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 8;
    /** How far a single column of the trunk may stick out past the nominal end. */
    private static final int MAX_END_EXTENSION = 2;
    /** Distance two logs have to keep from each other. */
    private static final int MIN_DISTANCE = 12;

    /**
     * Logs placed during this session, used to keep them apart.
     *
     * <p>This used to be an unbounded list: every generated log appended a position and nothing ever
     * removed them, so a long world-generation session kept growing until the heap ran out. It is now
     * a bounded ring per dimension (oldest entries fall off) and is cleared when the server stops.</p>
     */
    private static final int MAX_TRACKED = 4096;
    private static final Map<ResourceKey<net.minecraft.world.level.Level>, Deque<BlockPos>> PLACED =
            new ConcurrentHashMap<>();

    /** Tracked log counts per dimension, for the cache report. */
    public static String cacheReport() {
        StringBuilder builder = new StringBuilder("felled_logs=");
        PLACED.forEach((dimension, placed) -> builder.append(dimension.location().getPath())
                .append(':').append(placed.size()).append(' '));
        return builder.toString().trim();
    }

    /** Frees the per-dimension buffers; called on server stop so a reload starts clean. */
    public static void clearCaches() {
        PLACED.clear();
    }

    public FallenLogFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public FallenLogFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int baseY = findGround(level, origin);
        if (baseY == Integer.MIN_VALUE) {
            return false;
        }
        if (!level.getLevel().isClientSide) {
            Deque<BlockPos> placed = PLACED.computeIfAbsent(level.getLevel().dimension(),
                    key -> new ArrayDeque<>());
            BlockPos centre = new BlockPos(origin.getX(), baseY, origin.getZ());
            synchronized (placed) {
                for (BlockPos previous : placed) {
                    if (previous.distSqr(centre) < (long) MIN_DISTANCE * MIN_DISTANCE) {
                        return false; // would overlap an existing log
                    }
                }
                placed.addLast(centre);
                while (placed.size() > MAX_TRACKED) {
                    placed.removeFirst();
                }
            }
        }

        int width = MIN_DIAMETER + random.nextInt(MAX_DIAMETER - MIN_DIAMETER + 1);
        int height = MIN_DIAMETER + random.nextInt(MAX_DIAMETER - MIN_DIAMETER + 1);
        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        boolean alongX = random.nextBoolean();
        boolean hollow = width > 3 || height > 3;

        // every lateral column gets its own length: both ends are ragged instead of a clean cut,
        // so two logs never look like the same extruded rectangle
        int[][] startExt = new int[width][height];
        int[][] endExt = new int[width][height];
        for (int across = 0; across < width; across++) {
            for (int up = 0; up < height; up++) {
                startExt[across][up] = random.nextInt(MAX_END_EXTENSION + 1);
                endExt[across][up] = random.nextInt(MAX_END_EXTENSION + 1);
            }
        }

        BlockState bark = FrostedLandContent.GIANT_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
        BlockState core = hollow ? Blocks.AIR.defaultBlockState() : Blocks.ICE.defaultBlockState();
        // the log's bottom rests exactly on the ground block
        int floor = baseY + 1;

        boolean placedAny = false;
        for (int across = 0; across < width; across++) {
            for (int up = 0; up < height; up++) {
                boolean onRing = up == 0 || up == height - 1 || across == 0 || across == width - 1;
                BlockState state = onRing ? bark : core;
                int from = -startExt[across][up];
                int to = length - 1 + endExt[across][up];
                for (int step = from; step <= to; step++) {
                    int along = step - length / 2;
                    int x = origin.getX() + (alongX ? along : across - width / 2);
                    int z = origin.getZ() + (alongX ? across - width / 2 : along);
                    BlockPos pos = new BlockPos(x, floor + up, z);
                    BlockState existing = level.getBlockState(pos);
                    if (onRing ? existing.canBeReplaced() : true) {
                        level.setBlock(pos, state, 2);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }

    /** Topmost solid block below the origin that has air above it, or {@link Integer#MIN_VALUE}. */
    private static int findGround(WorldGenLevel level, BlockPos origin) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos probe = origin.offset(0, dy, 0);
            if (!level.getBlockState(probe).isAir() && level.getBlockState(probe.above()).isAir()) {
                return probe.getY();
            }
        }
        return Integer.MIN_VALUE;
    }
}
