package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.world.block.FrostPortalBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pool frame, seen from above:
 *
 * <pre>
 *   S S S S        S = snow block (12 of them, the ring)
 *   S W W S        W = water (2x2 interior, the pool)
 *   S W W S
 *   S S S S
 * </pre>
 *
 * It lies flat on the ground. Throwing a diamond into the pool water turns the four water blocks
 * into portal blocks, so the portal becomes a purple sheet flush with the ground that you step
 * into. Because the water is an ordinary water source, it will freeze into ice in cold biomes -
 * the diamond has to go in before that happens, otherwise the frame no longer validates.
 *
 * <p>Validation is purely server-side block-state inspection, so it works identically in
 * single-player and on a dedicated server and needs no block entity.</p>
 */
public final class FrostPortalFrame {
    /** Interior is 2x2; the frame ring adds one block on every side (4x4 total). */
    public static final int INTERIOR_SIZE = 2;

    /** A validated frame: {@code min} is the lowest/leftmost interior block. */
    public record Frame(BlockPos min, Direction.Axis axis) {
        /** Interior block at offset (u along the plane, v along the second plane axis). */
        public BlockPos interior(int u, int v) {
            return at(this.min, this.axis, u, v);
        }

        public List<BlockPos> interiorBlocks() {
            List<BlockPos> list = new ArrayList<>(4);
            for (int u = 0; u < INTERIOR_SIZE; u++) {
                for (int v = 0; v < INTERIOR_SIZE; v++) {
                    list.add(interior(u, v));
                }
            }
            return list;
        }
    }

    /**
     * Position inside the frame plane.
     *
     * <ul>
     *   <li>{@link Direction.Axis#Y} (flat pool, the normal case): u = x, v = z.</li>
     *   <li>{@link Direction.Axis#X}: u = x, v = y.</li>
     *   <li>{@link Direction.Axis#Z}: u = z, v = y.</li>
     * </ul>
     */
    public static BlockPos at(BlockPos min, Direction.Axis axis, int u, int v) {
        return switch (axis) {
            case Y -> new BlockPos(min.getX() + u, min.getY(), min.getZ() + v);
            case X -> new BlockPos(min.getX() + u, min.getY() + v, min.getZ());
            case Z -> new BlockPos(min.getX(), min.getY() + v, min.getZ() + u);
        };
    }

    /**
     * Looks for a flat pool frame that contains {@code waterPos} (which must be water or an
     * existing portal block). All four interior corners are tried.
     */
    public static Optional<Frame> find(Level level, BlockPos waterPos) {
        for (int du = 0; du >= -1; du--) {
            for (int dv = 0; dv >= -1; dv--) {
                BlockPos min = new BlockPos(waterPos.getX() + du, waterPos.getY(), waterPos.getZ() + dv);
                if (isValid(level, min, Direction.Axis.Y)) {
                    return Optional.of(new Frame(min, Direction.Axis.Y));
                }
            }
        }
        return Optional.empty();
    }

    /** A frame is built from snow; once lit the snow is replaced by unbreakable frost rock. */
    private static boolean isRingMaterial(BlockState state) {
        return state.is(Blocks.SNOW_BLOCK) || state.is(ModBlocks.FROST_ROCK.get());
    }

    /** Checks the 2x2 water interior plus the surrounding ring (snow or frost rock). */
    public static boolean isValid(Level level, BlockPos min, Direction.Axis axis) {
        for (int u = 0; u < INTERIOR_SIZE; u++) {
            for (int v = 0; v < INTERIOR_SIZE; v++) {
                BlockState state = level.getBlockState(at(min, axis, u, v));
                boolean ok = state.is(Blocks.WATER) || state.is(ModBlocks.FROST_PORTAL.get());
                if (!ok) {
                    return false;
                }
            }
        }
        for (int u = -1; u <= INTERIOR_SIZE; u++) {
            for (int v = -1; v <= INTERIOR_SIZE; v++) {
                boolean ring = u == -1 || u == INTERIOR_SIZE || v == -1 || v == INTERIOR_SIZE;
                if (!ring) {
                    continue;
                }
                if (!isRingMaterial(level.getBlockState(at(min, axis, u, v)))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** True when {@code min} is the corner of a complete, lit 2x2 portal. */
    public static boolean isLitFrame(Level level, BlockPos min) {
        for (int u = 0; u < INTERIOR_SIZE; u++) {
            for (int v = 0; v < INTERIOR_SIZE; v++) {
                BlockState state = level.getBlockState(at(min, Direction.Axis.Y, u, v));
                if (!state.is(ModBlocks.FROST_PORTAL.get())) {
                    return false;
                }
            }
        }
        return isValid(level, min, Direction.Axis.Y);
    }

    /** True when the block at {@code pos} belongs to at least one complete, lit portal frame. */
    public static boolean anyLitFrameAround(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (isLitFrame(level, pos.offset(dx, 0, dz))) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Lights the frame: pool water becomes portal blocks, the triggering diamond is consumed
     * (configurable) and the ignition sound plays.
     */
    public static void ignite(Level level, Frame frame, Entity trigger) {
        BlockState portal = ModBlocks.FROST_PORTAL.get()
                .defaultBlockState()
                .setValue(FrostPortalBlock.AXIS, frame.axis());

        beginEdit();
        try {
            for (BlockPos pos : frame.interiorBlocks()) {
                level.setBlockAndUpdate(pos, portal);
            }

            // The frame itself becomes unbreakable frost rock, so a lit portal can never be torn down.
            BlockState rock = ModBlocks.FROST_ROCK.get().defaultBlockState();
            for (int u = -1; u <= INTERIOR_SIZE; u++) {
                for (int v = -1; v <= INTERIOR_SIZE; v++) {
                    boolean ring = u == -1 || u == INTERIOR_SIZE || v == -1 || v == INTERIOR_SIZE;
                    if (ring) {
                        level.setBlockAndUpdate(at(frame.min(), frame.axis(), u, v), rock);
                    }
                }
            }
        } finally {
            endEdit();
        }

        if (trigger != null && FrostConfig.CONSUME_DIAMOND.get()) {
            if (trigger instanceof ItemEntity item) {
                ItemStack stack = item.getItem();
                stack.shrink(1);
                if (stack.isEmpty()) {
                    item.discard();
                }
            } else {
                trigger.discard();
            }
        }

        BlockPos soundPos = frame.min();
        level.playSound(null, soundPos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Frost portal lit at {} (axis {})", soundPos, frame.axis());
        }
    }

    /** {@code true} when the stack is the ignition item (a diamond). */
    public static boolean isIgnitionItem(ItemStack stack) {
        return stack.is(Items.DIAMOND);
    }

    private FrostPortalFrame() {
    }
}
