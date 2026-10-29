package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.world.block.CandyPortalBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The candy portal frame, straight from the nether portal blueprint (skeleton laid out as the
 * player sees it, {@code #} = frost honey / candy frame):
 *
 * <pre>
 *   # # # #     4 wide, 5 tall outer ring   -> 14 frame blocks
 *   # . . #     interior 2 wide, 3 tall     ->  6 portal blocks
 *   # . . #
 *   # . . #
 *   # # # #
 * </pre>
 *
 * <p>The portal stands upright, so the plane's normal is horizontal (axis {@code x} or {@code z}).
 * The frame is built from {@code myriadbloom:frost_honey} and is lit by using candy syrup on an
 * interior block; ignition swaps the interior for {@code myriadbloom:candy_portal} and the ring for
 * the unbreakable {@code myriadbloom:candy_rock}. As with the frost pool, validation is pure
 * server-side block-state inspection (no block entity), so it behaves identically in single-player
 * and on a dedicated server.</p>
 */
public final class CandyPortalFrame {
    /** Interior width (horizontal, in the portal plane). */
    public static final int INTERIOR_WIDTH = 2;
    /** Interior height (vertical). */
    public static final int INTERIOR_HEIGHT = 3;

    private static final Direction.Axis[] HORIZONTAL_AXES = {Direction.Axis.X, Direction.Axis.Z};

    /** A validated frame: {@code min} is the lowest/leftmost interior block. */
    public record Frame(BlockPos min, Direction.Axis axis) {
        /** Interior block at offset (u along the plane, v up). */
        public BlockPos interior(int u, int v) {
            return at(this.min, this.axis, u, v);
        }

        /** The six interior (portal surface) positions. */
        public List<BlockPos> interiorBlocks() {
            List<BlockPos> list = new ArrayList<>(INTERIOR_WIDTH * INTERIOR_HEIGHT);
            for (int u = 0; u < INTERIOR_WIDTH; u++) {
                for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                    list.add(interior(u, v));
                }
            }
            return list;
        }

        /** The fourteen ring positions. */
        public List<BlockPos> ringBlocks() {
            List<BlockPos> list = new ArrayList<>();
            for (int u = -1; u <= INTERIOR_WIDTH; u++) {
                for (int v = -1; v <= INTERIOR_HEIGHT; v++) {
                    if (isRing(u, v)) {
                        list.add(at(this.min, this.axis, u, v));
                    }
                }
            }
            return list;
        }
    }

    private static boolean isRing(int u, int v) {
        return u == -1 || u == INTERIOR_WIDTH || v == -1 || v == INTERIOR_HEIGHT;
    }

    /**
     * Position inside the frame plane: {@code u} runs along the horizontal in-plane axis, {@code v}
     * is height.
     *
     * <ul>
     *   <li>{@link Direction.Axis#X}: the plane is YZ, so u = z and v = y.</li>
     *   <li>{@link Direction.Axis#Z}: the plane is XY, so u = x and v = y.</li>
     * </ul>
     */
    public static BlockPos at(BlockPos min, Direction.Axis axis, int u, int v) {
        return offset(min, axis, u, v);
    }

    /** {@code base} moved by {@code u} along the in-plane horizontal axis and {@code v} up. */
    public static BlockPos offset(BlockPos base, Direction.Axis axis, int u, int v) {
        return switch (axis) {
            case X -> base.offset(0, v, u);
            case Z -> base.offset(u, v, 0);
            default -> base.offset(u, 0, v);
        };
    }

    // ------------------------------------------------------------------ validation

    /** A frame ring is built from frost honey; once lit it is unbreakable candy rock. */
    private static boolean isRingMaterial(BlockState state) {
        return state.is(ModBlocks.FROST_HONEY.get()) || state.is(ModBlocks.CANDY_ROCK.get());
    }

    /** The interior of a frame must be free for ignition: six air blocks. */
    private static boolean isInteriorEmpty(Level level, BlockPos min, Direction.Axis axis) {
        for (int u = 0; u < INTERIOR_WIDTH; u++) {
            for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                if (!level.getBlockState(at(min, axis, u, v)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isRingComplete(Level level, BlockPos min, Direction.Axis axis) {
        for (int u = -1; u <= INTERIOR_WIDTH; u++) {
            for (int v = -1; v <= INTERIOR_HEIGHT; v++) {
                if (isRing(u, v) && !isRingMaterial(level.getBlockState(at(min, axis, u, v)))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A complete frame: candy-rock/frost-honey ring with air or candy-portal interior. */
    public static boolean isValid(Level level, BlockPos min, Direction.Axis axis) {
        if (!isRingComplete(level, min, axis)) {
            return false;
        }
        for (int u = 0; u < INTERIOR_WIDTH; u++) {
            for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                BlockState state = level.getBlockState(at(min, axis, u, v));
                if (!state.isAir() && !state.is(ModBlocks.CANDY_PORTAL.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A complete, still unlit frame (so it can be ignited). */
    public static boolean isIgnitable(Level level, BlockPos min, Direction.Axis axis) {
        return isRingComplete(level, min, axis) && isInteriorEmpty(level, min, axis);
    }

    /** A complete, lit frame: all six interior blocks are candy portal blocks of this axis. */
    public static boolean isLit(Level level, BlockPos min, Direction.Axis axis) {
        if (!isRingComplete(level, min, axis)) {
            return false;
        }
        for (int u = 0; u < INTERIOR_WIDTH; u++) {
            for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                BlockState state = level.getBlockState(at(min, axis, u, v));
                if (!state.is(ModBlocks.CANDY_PORTAL.get()) || state.getValue(CandyPortalBlock.AXIS) != axis) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Finds the frame whose interior contains {@code pos}, trying both possible orientations.
     */
    public static Optional<Frame> find(Level level, BlockPos pos) {
        for (Direction.Axis axis : HORIZONTAL_AXES) {
            Optional<Frame> frame = find(level, pos, axis);
            if (frame.isPresent()) {
                return frame;
            }
        }
        return Optional.empty();
    }

    /** Finds the frame with the given orientation whose interior contains {@code pos}. */
    public static Optional<Frame> find(Level level, BlockPos pos, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            return Optional.empty();
        }
        for (int u = 0; u < INTERIOR_WIDTH; u++) {
            for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                BlockPos min = offset(pos, axis, -u, -v);
                if (isValid(level, min, axis)) {
                    return Optional.of(new Frame(min, axis));
                }
            }
        }
        return Optional.empty();
    }

    /** Finds an unlit frame whose interior contains {@code pos}. */
    public static Optional<Frame> findIgnitable(Level level, BlockPos pos) {
        for (Direction.Axis axis : HORIZONTAL_AXES) {
            for (int u = 0; u < INTERIOR_WIDTH; u++) {
                for (int v = 0; v < INTERIOR_HEIGHT; v++) {
                    BlockPos min = offset(pos, axis, -u, -v);
                    if (isIgnitable(level, min, axis)) {
                        return Optional.of(new Frame(min, axis));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * True when {@code pos} is a ring block of at least one complete, lit frame.
     *
     * <p>Only the candidate frames that could own this ring block are tested: a ring block sits at a
     * perimeter offset ({@code u} in -1..2, {@code v} in -1..3) from the interior minimum, so the
     * candidate minimum is {@code pos - (u, v)} - at most 14 tries per orientation.</p>
     */
    public static boolean anyLitFrameAround(Level level, BlockPos pos) {
        for (Direction.Axis axis : HORIZONTAL_AXES) {
            for (int u = -1; u <= INTERIOR_WIDTH; u++) {
                for (int v = -1; v <= INTERIOR_HEIGHT; v++) {
                    if (!isRing(u, v)) {
                        continue;
                    }
                    BlockPos min = offset(pos, axis, -u, -v);
                    if (isLit(level, min, axis)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * True when {@code pos} belongs to a lit portal structure: a ring block of a lit frame, or a block
     * of the landing platform that the arrival builder puts under a floating doorway.
     *
     * <p>The platform sits up to two blocks outside the ring and up to two blocks in front of or
     * behind the portal plane, so unlike {@link #anyLitFrameAround} this search also steps along the
     * portal's normal. It is only used by {@code CandyRockBlock}, i.e. on block updates and random
     * ticks, so the handful of candidate frames per orientation is not a performance concern.</p>
     */
    public static boolean anyLitFrameNear(Level level, BlockPos pos) {
        if (anyLitFrameAround(level, pos)) {
            return true;
        }
        for (Direction.Axis axis : HORIZONTAL_AXES) {
            for (int u = -3; u <= INTERIOR_WIDTH + 2; u++) {
                for (int v = -2; v <= 1; v++) {
                    for (int normal = -2; normal <= 3; normal++) {
                        BlockPos min = offsetNormal(offset(pos, axis, -u, -v), axis, -normal);
                        if (isLit(level, min, axis)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Moves {@code pos} along the portal's normal axis (X for an x-facing plane, Z otherwise). */
    public static BlockPos offsetNormal(BlockPos pos, Direction.Axis axis, int distance) {
        return axis == Direction.Axis.X ? pos.offset(distance, 0, 0) : pos.offset(0, 0, distance);
    }

    // ------------------------------------------------------------------ build/edit guard

    private static boolean editing;

    /** Suppresses the frame integrity checks while a portal is being assembled block by block. */
    public static void beginEdit() {
        editing = true;
    }

    public static void endEdit() {
        editing = false;
    }

    public static boolean isEditing() {
        return editing;
    }

    // ------------------------------------------------------------------ ignition

    /**
     * Lights the frame: the six interior blocks become candy portal blocks and the ring becomes
     * unbreakable candy rock. The triggering candy syrup is consumed by the caller (the item).
     */
    public static void ignite(Level level, Frame frame, Entity trigger) {
        BlockState portal = ModBlocks.CANDY_PORTAL.get()
                .defaultBlockState()
                .setValue(CandyPortalBlock.AXIS, frame.axis());
        BlockState rock = ModBlocks.CANDY_ROCK.get().defaultBlockState();

        beginEdit();
        try {
            for (BlockPos pos : frame.interiorBlocks()) {
                level.setBlockAndUpdate(pos, portal);
            }
            for (BlockPos pos : frame.ringBlocks()) {
                level.setBlockAndUpdate(pos, rock);
            }
        } finally {
            endEdit();
        }

        level.playSound(null, frame.min(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Candy portal lit at {} (axis {})", frame.min(), frame.axis());
        }
    }

    private CandyPortalFrame() {
    }
}
