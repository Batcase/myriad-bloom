package com.myriadbloom.world.gen;

import com.myriadbloom.world.gen.SnowFortressFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.myriadbloom.world.RegionLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The one and only piece of the snow fortress: it does not build vanilla style piece by piece, it
 * simply calls {@link SnowFortressFeature#build} with its origin, which flattens the site and raises
 * the glacier ice igloo.
 */
public class SnowFortressPiece extends StructurePiece {
    /** Horizontal radius the piece claims, so chunk boundaries are respected. */
    private static final int RADIUS = 46;
    private static final int HEIGHT_BELOW = 8;
    private static final int HEIGHT_ABOVE = 34;
    private static final String ORIGIN_TAG = "Origin";

    private final BlockPos origin;
    private Boolean allowed;

    public SnowFortressPiece(BlockPos origin) {
        super(FrostStructures.SNOW_FORTRESS_PIECE.get(), 0, new BoundingBox(
                origin.getX() - RADIUS, origin.getY() - HEIGHT_BELOW, origin.getZ() - RADIUS,
                origin.getX() + RADIUS, origin.getY() + HEIGHT_ABOVE, origin.getZ() + RADIUS));
        this.origin = origin;
    }

    public static SnowFortressPiece fromTag(CompoundTag tag) {
        return new SnowFortressPiece(BlockPos.of(tag.getLong(ORIGIN_TAG)));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong(ORIGIN_TAG, this.origin.asLong());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!this.isAllowed(level)) {
            return;
        }
        // vanilla calls this once per chunk the piece touches; writes outside `box` would be dropped
        // with a "setBlock in a far chunk" error, so the builder clips itself to the box
        SnowFortressFeature.withClip(box, () -> SnowFortressFeature.build(level, random, this.origin));
    }

    /**
     * Region bookkeeping, evaluated once per structure: records the fortress for the future map and
     * enforces "at most nine fortresses per 16x16 chunk region".
     */
    private synchronized boolean isAllowed(WorldGenLevel level) {
        if (this.allowed == null) {
            if (level.getLevel() instanceof ServerLevel server) {
                RegionLedger ledger = RegionLedger.of(server);
                boolean fits = ledger.canPlaceFortress(this.origin);
                if (fits) {
                    ledger.record("fortress", this.origin);
                }
                this.allowed = fits;
            } else {
                this.allowed = true;
            }
        }
        return this.allowed;
    }
}
