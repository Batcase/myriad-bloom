package com.myriadbloom.world.block;

import com.myriadbloom.FrostConfig;
import com.myriadbloom.MyriadBloom;
import com.myriadbloom.api.MyriadBloomApi;
import com.myriadbloom.world.FrostPortalBuilder;
import com.myriadbloom.world.FrostPortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The portal surface of Frost Realm.
 *
 * <p>The portal lies <b>flat</b>, like the surface of the 2x2 water pool it replaces: the frame is a
 * 4x4 ring of snow blocks laid on the ground with water in the middle, so the portal is a sheet you
 * step into from above rather than a doorway you walk through.</p>
 *
 * <p>Once lit the surface never freezes: the four water blocks are gone (they <i>are</i> the portal
 * now), and the block resists both ice and snow. That is why it keeps collision - a block that does
 * not block motion is skipped by the {@code MOTION_BLOCKING} heightmap, and
 * {@code ServerLevel#tickPrecipitation} would then drop a snow layer straight into the portal block
 * and overwrite it. With collision, snow is placed one block above and immediately cleared again
 * ({@link #neighborChanged}, {@link #randomTick}), and the block is listed in
 * {@code minecraft:snow_layer_cannot_survive_on} so a layer cannot stay.</p>
 *
 * <p>Mechanics are borrowed from the nether portal: the block implements vanilla's {@link Portal}
 * interface and forwards entities to {@code Entity#setAsInsidePortal}. Vanilla then handles the
 * portal timer, the cross-dimension teleport (players <i>and</i> mobs), the travel sound and the
 * post-travel cooldown. All of it runs on the server.</p>
 */
public class FrostPortalBlock extends Block implements Portal {
    /** x / y / z; a flat pool uses {@link Direction.Axis#Y}. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    /** Vertical panels (unused by the flat design, kept for completeness). */
    private static final VoxelShape SHAPE_X = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    private static final VoxelShape SHAPE_Z = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    /** Flat pool: the sheet sits in the top of the block, so it is flush with the ground. */
    private static final VoxelShape SHAPE_Y = Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public FrostPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case Y -> SHAPE_Y;
        };
    }

    // ------------------------------------------------------------------ no ice, no snow

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        clearCover(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        // Fires the moment anything is placed above us, e.g. a snow layer from precipitation.
        if (neighborPos.equals(pos.above())) {
            clearCover(level, pos);
        }
        collapseIfBroken(level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        clearCover(level, pos);
        collapseIfBroken(level, pos);
    }

    /**
     * A portal surface only exists as part of a complete frame. If a sibling portal block (or the
     * frame) is gone, this block disappears as well, and the frost rock frame reverts to snow, so a
     * dismantled portal leaves nothing behind.
     */
    private static void collapseIfBroken(Level level, BlockPos pos) {
        if (level.isClientSide || FrostPortalFrame.isEditing()) {
            return;
        }
        if (FrostPortalFrame.find(level, pos).isEmpty()) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    /** Removes a snow layer sitting on the portal sheet; the sheet itself is never ice. */
    private static void clearCover(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(Blocks.SNOW) && cover.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) <= 1) {
            level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
        } else if (cover.is(Blocks.SNOW)) {
            level.setBlockAndUpdate(above, cover.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS,
                    cover.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) - 1));
        }
    }

    // ------------------------------------------------------------------ portal behaviour

    // Teleport triggering lives in FrostEvents#onEntityTick: edge based, so standing on the portal
    // after arriving does not bounce the player back.

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        // Charging happens in FrostEvents before this is ever called, so travel itself is instant.
        return 0;
    }

    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!FrostConfig.ENABLE_PORTAL.get()) {
            return null;
        }
        ServerLevel destination = MyriadBloomApi.otherSide(level.getServer(), level);
        if (destination == null) {
            MyriadBloom.LOGGER.warn("Frost portal at {} has no destination level ({} not loaded?)",
                    pos, MyriadBloomApi.otherSideKey(level));
            return null;
        }
        BlockState sourceState = level.getBlockState(pos);
        Direction.Axis axis = sourceState.hasProperty(AXIS) ? sourceState.getValue(AXIS) : Direction.Axis.Y;
        if (FrostConfig.LOG_PORTAL_EVENTS.get()) {
            MyriadBloom.LOGGER.info("Portal transfer: {} -> {} at {}",
                    entity.getName().getString(), destination.dimension().location(), pos);
        }
        return FrostPortalBuilder.buildDestination(entity, destination, axis);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        // Same on-screen distortion the nether portal uses.
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F,
                    random.nextFloat() * 0.4F + 0.8F, false);
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.95D + random.nextDouble() * 0.35D;
            double z = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() - 0.5D) * 0.06D;
            double vy = 0.010D + random.nextDouble() * 0.020D;
            double vz = (random.nextDouble() - 0.5D) * 0.06D;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, vy, vz);
        }
    }
}
