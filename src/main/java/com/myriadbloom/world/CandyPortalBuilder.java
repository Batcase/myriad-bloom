package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.world.block.CandyPortalBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Arrival-side bookkeeping for candy portal travel.
 *
 * <p>Coordinates are mirrored ({@code coordinate_scale = 1}), so the arrival column is the source
 * column. If the arrival side already has a candy portal it is reused; otherwise a fresh 4x5
 * standing frame is carved into the surface and lit, so the traveller always steps out of a working
 * portal. A doorway that ends up in the air (the heightmap can be a tree top or a water surface)
 * gets a 6x6 landing platform of the frame material, so nobody drops out of the world on arrival.
 * This is the vertical counterpart of {@link FrostPortalBuilder}, which builds the flat pool of the
 * frost realm.</p>
 */
public final class CandyPortalBuilder {

    /**
     * Post-transition action: play the portal sound, apply the vanilla travel cooldown and disarm the
     * charge until the entity steps off (otherwise it would be bounced straight back).
     */
    private static DimensionTransition.PostDimensionTransition postTransition() {
        DimensionTransition.PostDimensionTransition action = DimensionTransition.PLAY_PORTAL_SOUND
                .then(entity -> {
                    entity.setPortalCooldown();
                    entity.portalProcess = null;
                    PortalCharge.markArrived(entity);
                });
        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            return action.then(entity -> MyriadBloom.LOGGER.info(
                    "Candy portal arrival: {} is now in {} at {}",
                    entity.getName().getString(),
                    entity.level().dimension().location(),
                    entity.blockPosition()));
        }
        return action;
    }

    public static DimensionTransition buildDestination(Entity entity, ServerLevel destination, Direction.Axis axis) {
        BlockPos source = entity.blockPosition();
        CandyPortalFrame.Frame frame = ensurePortal(destination, source.getX(), source.getZ());
        Vec3 target = centerOf(frame);
        return new DimensionTransition(destination, target, entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), postTransition());
    }

    /** Uses an existing candy portal at the column if there is one, otherwise builds a new frame. */
    public static CandyPortalFrame.Frame ensurePortal(ServerLevel level, int x, int z) {
        // Block writes silently no-op in unloaded chunks, so force the chunk in first.
        level.getChunk(x >> 4, z >> 4);

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        surface = Mth.clamp(surface,
                level.getMinBuildHeight() + 4,
                level.getMaxBuildHeight() - CandyPortalFrame.INTERIOR_HEIGHT - 4);

        for (int dy = -10; dy <= 10; dy++) {
            BlockPos candidate = new BlockPos(x, surface + dy, z);
            BlockState state = level.getBlockState(candidate);
            if (state.is(ModBlocks.CANDY_PORTAL.get())) {
                Optional<CandyPortalFrame.Frame> existing =
                        CandyPortalFrame.find(level, candidate, state.getValue(CandyPortalBlock.AXIS));
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
        }

        // The interior minimum sits at the surface, so the bottom frame row is embedded in the
        // ground and the traveller arrives standing on top of it.
        BlockPos min = new BlockPos(x, surface, z);
        if (!FrostConfig.BUILD_DESTINATION_PORTAL.get()) {
            return new CandyPortalFrame.Frame(min, Direction.Axis.Z);
        }
        return build(level, min, Direction.Axis.Z);
    }

    /** Builds a brand-new 4x5 standing frame with its 2x3 interior already lit. */
    public static CandyPortalFrame.Frame build(ServerLevel level, BlockPos min, Direction.Axis axis) {
        level.getChunkAt(min);

        CandyPortalFrame.Frame frame = new CandyPortalFrame.Frame(min, axis);
        BlockState frameBlock = ModBlocks.CANDY_ROCK.get().defaultBlockState();
        BlockState portal = ModBlocks.CANDY_PORTAL.get()
                .defaultBlockState()
                .setValue(CandyPortalBlock.AXIS, axis);

        CandyPortalFrame.beginEdit();
        try {
            // Clear the whole footprint first: the arrival side may be rock, dirt or syrup, and a
            // half-built portal would collapse itself through the frame integrity checks.
            for (int u = -1; u <= CandyPortalFrame.INTERIOR_WIDTH; u++) {
                for (int v = -1; v <= CandyPortalFrame.INTERIOR_HEIGHT; v++) {
                    level.setBlockAndUpdate(CandyPortalFrame.at(min, axis, u, v), Blocks.AIR.defaultBlockState());
                }
            }
            for (BlockPos pos : frame.ringBlocks()) {
                level.setBlockAndUpdate(pos, frameBlock);
            }
            for (BlockPos pos : frame.interiorBlocks()) {
                level.setBlockAndUpdate(pos, portal);
            }
            ensureLandingPlatform(level, frame, frameBlock);
        } finally {
            CandyPortalFrame.endEdit();
        }

        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Built candy portal at {} (axis {})", min, axis);
        }
        return frame;
    }

    /**
     * Gives a floating doorway something to stand on.
     *
     * <p>The arrival column is picked from the heightmap, which can be the top of a tree, a water
     * surface or simply far above the ground - in that case the built portal hangs in the air and the
     * traveller drops out of it. Below the doorway, a {@code 6x6} patch of the frame material is
     * placed wherever there is nothing solid (air or fluid); on ground that is already there this
     * changes nothing.</p>
     */
    private static void ensureLandingPlatform(ServerLevel level, CandyPortalFrame.Frame frame,
                                              BlockState frameBlock) {
        BlockPos min = frame.min();
        int y = min.getY() - 1;
        boolean alongZ = frame.axis() == Direction.Axis.Z;
        int baseU = alongZ ? min.getX() : min.getZ();
        for (int u = -2; u <= CandyPortalFrame.INTERIOR_WIDTH + 1; u++) {
            for (int normal = -2; normal <= CandyPortalFrame.INTERIOR_WIDTH + 1; normal++) {
                BlockPos pos = alongZ
                        ? new BlockPos(baseU + u, y, min.getZ() + normal)
                        : new BlockPos(min.getX() + normal, y, baseU + u);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    level.setBlockAndUpdate(pos, frameBlock);
                }
            }
        }
    }

    /** Centre of the 2x3 interior, at the feet level of the bottom portal row. */
    private static Vec3 centerOf(CandyPortalFrame.Frame frame) {
        BlockPos min = frame.min();
        return switch (frame.axis()) {
            case X -> new Vec3(min.getX() + 0.5D, min.getY(), min.getZ() + 1.0D);
            case Z -> new Vec3(min.getX() + 1.0D, min.getY(), min.getZ() + 0.5D);
            default -> Vec3.atBottomCenterOf(min);
        };
    }

    private CandyPortalBuilder() {
    }
}
