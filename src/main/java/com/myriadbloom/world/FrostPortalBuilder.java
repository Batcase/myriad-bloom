package com.myriadbloom.world;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.ModBlocks;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.block.FrostPortalBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Arrival-side bookkeeping for portal travel.
 *
 * <p>Coordinates are mirrored: the frost realm uses {@code coordinate_scale = 1}, so x/z map 1:1
 * between the overworld and the realm and a pool lines up with its counterpart. If the arrival
 * column has no pool yet, one is dug into the surface (frame at {@code surface - 1}) so the portal
 * sheet ends up flush with the ground.</p>
 */
public final class FrostPortalBuilder {

    /**
     * Post-transition action: play the portal sound, apply the vanilla travel cooldown and, when
     * {@code log_portal_events} is on, log the dimension the entity actually ended up in.
     * Running after the move is what makes this an authoritative arrival check.
     */
    private static DimensionTransition.PostDimensionTransition postTransition() {
        // The arrival must not be re-triggered: drop the (copied) portal process, start the vanilla
        // cooldown, and disarm the charge until the entity steps off the portal.
        DimensionTransition.PostDimensionTransition action = DimensionTransition.PLAY_PORTAL_SOUND
                .then(entity -> {
                    entity.setPortalCooldown();
                    entity.portalProcess = null;
                    PortalCharge.markArrived(entity);
                });
        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            return action.then(entity -> MyriadBloom.LOGGER.info(
                    "Frost portal arrival: {} is now in {} at {}",
                    entity.getName().getString(),
                    entity.level().dimension().location(),
                    entity.blockPosition()));
        }
        return action;
    }

    public static DimensionTransition buildDestination(Entity entity, ServerLevel destination, Direction.Axis axis) {
        BlockPos source = entity.blockPosition();
        // never arrive in one of the two special biomes: look for a normal snowy column instead
        BlockPos landing = findAllowedColumn(destination, source.getX(), source.getZ());
        int x = landing == null ? source.getX() : landing.getX();
        int z = landing == null ? source.getZ() : landing.getZ();
        FrostPortalFrame.Frame frame = ensurePortal(destination, x, z);
        Vec3 target = centerOf(frame);
        return new DimensionTransition(destination, target, entity.getDeltaMovement(),
                entity.getYRot(), entity.getXRot(), postTransition());
    }

    /**
     * Biomes a portal must not drop the player into: the frosted land and the congealed land.
     */
    private static final java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>
            FORBIDDEN = java.util.List.of(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.BIOME, MyriadBloomApi.id("frosted_land")),
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.BIOME, MyriadBloomApi.id("frost_congealed")));

    /** Clearance the arrival column must keep from the special biomes, in blocks. */
    private static int clearance() {
        return com.myriadbloom.FrostConfig.PORTAL_CLEARANCE.get();
    }

    /** True when the column itself sits in one of the forbidden biomes. */
    private static boolean isForbiddenAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(new BlockPos(x, y, z));
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> forbidden : FORBIDDEN) {
            if (biome.is(forbidden)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when a portal may open above this column: the column and everything within
     * {@value #CLEARANCE} blocks must be a normal biome. Arriving on the very edge would drop the
     * player straight into the frost of the congealed land, so a wide ring is checked too.
     */
    private static boolean portalAllowed(ServerLevel level, int x, int z) {
        if (!com.myriadbloom.FrostConfig.PORTAL_AVOID_SPECIAL_BIOMES.get()) {
            return true;
        }
        if (isForbiddenAt(level, x, z)) {
            return false;
        }
        int clearance = clearance();
        for (int ring = clearance / 2; ring <= clearance; ring += Math.max(1, clearance / 2)) {
            for (int step = 0; step < 8; step++) {
                double angle = step * (Math.PI / 4.0D);
                int px = x + (int) Math.round(Math.cos(angle) * ring);
                int pz = z + (int) Math.round(Math.sin(angle) * ring);
                if (isForbiddenAt(level, px, pz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The source column if it is allowed, otherwise the first allowed column on an outward spiral.
     * Returns {@code null} when nothing suitable was found, in which case the source column is used.
     */
    private static BlockPos findAllowedColumn(ServerLevel level, int x, int z) {
        if (portalAllowed(level, x, z)) {
            return new BlockPos(x, 0, z);
        }
        for (int radius = 24; radius <= 768; radius += 24) {
            for (int step = 0; step < 16; step++) {
                double angle = step * (Math.PI * 2.0D / 16.0D) + radius * 0.017D;
                int px = x + (int) Math.round(Math.cos(angle) * radius);
                int pz = z + (int) Math.round(Math.sin(angle) * radius);
                if (portalAllowed(level, px, pz)) {
                    MyriadBloom.LOGGER.info("Frost portal: moved arrival from {},{} to {},{} (special biome avoided)",
                            x, z, px, pz);
                    return new BlockPos(px, 0, pz);
                }
            }
        }
        return null;
    }

    /** Uses an existing pool at the column if there is one, otherwise builds a new flat frame. */
    public static FrostPortalFrame.Frame ensurePortal(ServerLevel level, int x, int z) {
        // Block writes silently no-op in unloaded chunks, so force the chunk in first.
        level.getChunk(x >> 4, z >> 4);

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        surface = Math.max(level.getMinBuildHeight() + 3, Math.min(surface, level.getMaxBuildHeight() - 4));

        for (int dy = -8; dy <= 8; dy++) {
            BlockPos candidate = new BlockPos(x, surface + dy, z);
            if (level.getBlockState(candidate).is(ModBlocks.FROST_PORTAL.get())) {
                Optional<FrostPortalFrame.Frame> existing = FrostPortalFrame.find(level, candidate);
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
        }

        // Embed the frame in the top layer of the ground so the portal sheet is flush with it.
        BlockPos min = new BlockPos(x, surface - 1, z);
        if (!FrostConfig.BUILD_DESTINATION_PORTAL.get()) {
            return new FrostPortalFrame.Frame(min.above(), Direction.Axis.Y);
        }
        return build(level, min, Direction.Axis.Y);
    }

    /** Builds a brand-new flat 4x4 snow pool with its 2x2 interior already lit. */
    public static FrostPortalFrame.Frame build(ServerLevel level, BlockPos min, Direction.Axis axis) {
        level.getChunkAt(min);

        BlockState frameBlock = ModBlocks.FROST_ROCK.get().defaultBlockState();
        BlockState portal = ModBlocks.FROST_PORTAL.get()
                .defaultBlockState()
                .setValue(FrostPortalBlock.AXIS, axis);

        // Frame first, then the surface: the blocks' own integrity checks run on every neighbour
        // update, so a half-built portal would immediately collapse itself.
        FrostPortalFrame.beginEdit();
        try {
            for (int u = -1; u <= FrostPortalFrame.INTERIOR_SIZE; u++) {
                for (int v = -1; v <= FrostPortalFrame.INTERIOR_SIZE; v++) {
                    boolean ring = u == -1 || u == FrostPortalFrame.INTERIOR_SIZE
                            || v == -1 || v == FrostPortalFrame.INTERIOR_SIZE;
                    if (ring) {
                        level.setBlockAndUpdate(FrostPortalFrame.at(min, axis, u, v), frameBlock);
                    }
                }
            }
            for (int u = 0; u < FrostPortalFrame.INTERIOR_SIZE; u++) {
                for (int v = 0; v < FrostPortalFrame.INTERIOR_SIZE; v++) {
                    level.setBlockAndUpdate(FrostPortalFrame.at(min, axis, u, v), portal);
                }
            }
        } finally {
            FrostPortalFrame.endEdit();
        }

        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Built frost portal at {} (axis {})", min, axis);
        }
        return new FrostPortalFrame.Frame(min, axis);
    }

    /** Centre of the 2x2 interior, used as the arrival position (feet level of the portal block). */
    private static Vec3 centerOf(FrostPortalFrame.Frame frame) {
        BlockPos min = frame.min();
        return switch (frame.axis()) {
            case Y -> new Vec3(min.getX() + 1.0D, min.getY() + 1.0D, min.getZ() + 1.0D);
            case X -> new Vec3(min.getX() + 1.0D, min.getY(), min.getZ() + 0.5D);
            case Z -> new Vec3(min.getX() + 0.5D, min.getY(), min.getZ() + 1.0D);
        };
    }

    private FrostPortalBuilder() {
    }
}
